package com.koch.ambeth.persistence;

/*-
 * #%L
 * jambeth-persistence
 * %%
 * Copyright (C) 2017 Koch Softwaredevelopment
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 * #L%
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.koch.ambeth.ioc.IInitializingBean;
import com.koch.ambeth.ioc.IServiceContext;
import com.koch.ambeth.ioc.IStartingBean;
import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.log.ILogger;
import com.koch.ambeth.log.ILoggerHistory;
import com.koch.ambeth.log.LogInstance;
import com.koch.ambeth.persistence.api.IDatabaseMetaData;
import com.koch.ambeth.persistence.api.IDirectedLinkMetaData;
import com.koch.ambeth.persistence.api.IFieldMetaData;
import com.koch.ambeth.persistence.api.ILinkMetaData;
import com.koch.ambeth.persistence.api.IPermissionGroup;
import com.koch.ambeth.persistence.api.ITableMetaData;
import com.koch.ambeth.persistence.orm.XmlDatabaseMapper;
import com.koch.ambeth.service.merge.IEntityMetaDataProvider;
import com.koch.ambeth.util.ParamChecker;
import com.koch.ambeth.util.StringConversionHelper;
import com.koch.ambeth.util.collections.ArrayList;
import com.koch.ambeth.util.collections.HashMap;
import com.koch.ambeth.util.collections.HashSet;
import com.koch.ambeth.util.collections.Tuple2KeyHashMap;
import com.koch.ambeth.util.objectcollector.IThreadLocalObjectCollector;
import com.koch.ambeth.util.typeinfo.IRelationProvider;
import com.koch.ambeth.util.typeinfo.ITypeInfo;
import com.koch.ambeth.util.typeinfo.ITypeInfoItem;
import com.koch.ambeth.util.typeinfo.ITypeInfoProvider;

public class DatabaseMetaData
		implements
		IDatabaseMetaData,
		IConfigurableDatabaseMetaData,
		IInitializingBean,
		IStartingBean {
	@LogInstance
	private ILogger log;

	@Autowired
	protected IEntityMetaDataProvider entityMetaDataProvider;

	@Autowired
	protected ILoggerHistory loggerHistory;

	@Autowired
	protected IThreadLocalObjectCollector objectCollector;

	@Autowired
	protected IRelationProvider relationProvider;

	@Autowired
	protected IServiceContext serviceContext;

	@Autowired
	protected ITypeInfoProvider typeInfoProvider;

	protected final HashMap<String, ITableMetaData> nameToTableDict = new HashMap<>();

	protected final HashMap<Class<?>, ITableMetaData> typeToTableDict = new HashMap<>();

	protected final HashMap<Class<?>, ITableMetaData> typeToArchiveTableDict = new HashMap<>();

	protected final HashMap<String, IPermissionGroup> nameToPermissionGroupTableDict =
			new HashMap<>();

	protected final HashMap<String, ILinkMetaData> nameToLinkDict = new HashMap<>();

	protected final HashMap<String, ILinkMetaData> definingNameToLinkDict = new HashMap<>();

	protected final Tuple2KeyHashMap<ITableMetaData, ITableMetaData, ILinkMetaData[]> tablesToLinkDict =
			new Tuple2KeyHashMap<>();

	protected final HashMap<Class<?>, IEntityHandler> typeToEntityHandler = new HashMap<>();

	protected final ArrayList<Class<?>> handledEntities = new ArrayList<>();

	protected String name;
	protected final ArrayList<ITableMetaData> tables = new ArrayList<>();
	protected final List<ILinkMetaData> links = new ArrayList<>();

	private int maxNameLength;

	protected HashMap<String, HashSet<IDirectedLinkMetaData>> incompleteLinks = new HashMap<>();

	@Override
	public void addLinkByTables(ILinkMetaData link) {
		ITableMetaData fromTable = link.getFromTable();
		ITableMetaData toTable = link.getToTable();
		if (fromTable == null || toTable == null) {
			return;
		}

		ILinkMetaData[] links = tablesToLinkDict.get(fromTable, toTable);
		if (links == null) {
			links = new ILinkMetaData[] {link};
		}
		else {
			ILinkMetaData[] newLinks = new ILinkMetaData[links.length + 1];
			System.arraycopy(links, 0, newLinks, 0, links.length);
			newLinks[links.length] = link;
			links = newLinks;
		}
		tablesToLinkDict.put(fromTable, toTable, links);
	}

	@Override
	public void afterPropertiesSet() throws Throwable {
		// Intended blank
	}

	@Override
	public void afterStarted() {
		List<ITableMetaData> tables = getTables();
		for (int tableIndex = tables.size(); tableIndex-- > 0;) {
			ITableMetaData table = tables.get(tableIndex);
			handleTable(table);
		}
	}

	protected String buildFqName(String[] fqNameSplit) {
		if (fqNameSplit[0] == null) {
			return fqNameSplit[1];
		}
		return fqNameSplit[0] + "_" + fqNameSplit[1];
	}

	public String createForeignKeyLinkName(String fromTableName, String fromFieldName,
			String toTableName, String toFieldName) {
		fromTableName = buildFqName(XmlDatabaseMapper.splitSchemaAndName(fromTableName));
		fromFieldName = buildFqName(XmlDatabaseMapper.splitSchemaAndName(fromFieldName));
		toTableName = buildFqName(XmlDatabaseMapper.splitSchemaAndName(toTableName));
		toFieldName = buildFqName(XmlDatabaseMapper.splitSchemaAndName(toFieldName));
		return "LINK$" + fromTableName + "$" + fromFieldName + "$" + toTableName + "$"
				+ toFieldName;
	}

	@Override
	public ITableMetaData getArchiveTableByType(Class<?> entityType) {
		return typeToArchiveTableDict.get(entityType);
	}

	@Override
	public List<Class<?>> getHandledEntities() {
		return handledEntities;
	}

	@Override
	public ILinkMetaData getLinkByDefiningName(String definingName) {
		return definingNameToLinkDict.get(definingName);
	}

	@Override
	public ILinkMetaData getLinkByName(String linkName) {
		return nameToLinkDict.get(linkName);
	}

	@Override
	public List<ILinkMetaData> getLinks() {
		return links;
	}

	@Override
	public ILinkMetaData[] getLinksByTables(ITableMetaData table1, ITableMetaData table2) {
		return tablesToLinkDict.get(table1, table2);
	}

	@Override
	public int getMaxNameLength() {
		return maxNameLength;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IPermissionGroup getPermissionGroupOfTable(String tableName) {
		return nameToPermissionGroupTableDict.get(tableName);
	}

	@Override
	public String[] getSchemaNames() {
		throw new UnsupportedOperationException("Not implemented");
	}

	public IServiceContext getServiceProvider() {
		return serviceContext;
	}

	@Override
	public ITableMetaData getTableByName(String tableName) {
		return nameToTableDict.get(tableName);
	}

	@Override
	public ITableMetaData getTableByType(Class<?> entityType) {
		return getTableByType(entityType, false);
	}

	@Override
	public ITableMetaData getTableByType(Class<?> entityType, boolean tryOnly) {
		ParamChecker.assertParamNotNull(entityType, "entityType");
		ITableMetaData table = typeToTableDict.get(entityType);
		if (table == null) {
			table = typeToTableDict
					.get(entityMetaDataProvider.getMetaData(entityType).getEntityType());
			if (table == null) {
				if (!tryOnly) {
					throw new IllegalStateException(
							"No table found for entity type '" + entityType.getName() + "'");
				}
			}
		}
		return table;
	}

	@Override
	public List<ITableMetaData> getTables() {
		return tables;
	}

	public Map<Class<?>, IEntityHandler> getTypeToEntityHandler() {
		return typeToEntityHandler;
	}

	@Override
	public void handleTable(ITableMetaData table) {
		if (table.isPermissionGroup()) {
			return;
		}
		ArrayList<IFieldMetaData> alternateIdMembers = new ArrayList<>();
		Class<?> fromType = table.getEntityType();
		if (fromType == null) {
			if (log.isWarnEnabled()) {
				loggerHistory.warnOnce(log, this,
						"No entity mapped to table '" + table.getName() + "'");
			}
			return;
		}
		if (table.isArchive()) {
			typeToArchiveTableDict.put(fromType, table);
			return;
		}
		else {
			typeToTableDict.put(fromType, table);
		}

		List<IDirectedLinkMetaData> links = table.getLinks();
		handleTableLinks(table, links);
		HashSet<IDirectedLinkMetaData> incompleteLinkSet = incompleteLinks.remove(table.getName());
		if (incompleteLinkSet != null) {
			for (IDirectedLinkMetaData oneIncompleteLink : incompleteLinkSet) {
				handleTableLinks(oneIncompleteLink.getFromTable(),
						Arrays.asList(oneIncompleteLink));
			}
		}
		// TODO Reactivate this check with embedded-type case handling
		// for (ITypeInfoItem member : typeInfo.getChildMembers())
		// {
		// if (member.isXMLIgnore())
		// {
		// continue;
		// }
		// String memberName = member.getName();
		// if (table.getFieldByMemberName(memberName) == null &&
		// table.getLinkByMemberName(memberName)
		// == null)
		// {
		// throw new IllegalArgumentException("Member '" + fromType.getName() + "." + memberName
		// + " is neither mapped to a link or a field and it is not annotated with " +
		// Transient.class.getName());
		// }
		// }

		// Remove not-mapped (and so not usable) alternate id fields
		for (IFieldMetaData alternateIdMember : table.getAlternateIdFields()) {
			if (alternateIdMember.getMember() != null) {
				alternateIdMembers.add(alternateIdMember);
			}
		}
		((TableMetaData) table)
				.setAlternateIdFields(alternateIdMembers.toArray(IFieldMetaData.class));
	}

	protected void handleTableLinks(ITableMetaData table, List<IDirectedLinkMetaData> links) {
		Class<?> fromType = table.getEntityType();
		ITypeInfo typeInfo = typeInfoProvider.getTypeInfo(fromType);

		for (int linkIndex = links.size(); linkIndex-- > 0;) {
			IDirectedLinkMetaData link = links.get(linkIndex);
			ITableMetaData toTable = link.getToTable();

			Class<?> toType;
			if (toTable != null) {
				toType = toTable.getEntityType();
				if (toType == null) {
					HashSet<IDirectedLinkMetaData> incompleteLinkSet =
							incompleteLinks.get(toTable.getName());
					if (incompleteLinkSet == null) {
						incompleteLinkSet = new HashSet<>();
						incompleteLinks.put(toTable.getName(), incompleteLinkSet);
					}
					incompleteLinkSet.add(link);
					if (log.isWarnEnabled()) {
						log.warn("No entity mapped to table '" + toTable.getName()
								+ "'. Error occured while handling link '" + link.getName() + "'");
					}
					continue;
				}
			}
			else {
				toType = link.getToEntityType();
			}
			String memberName = table.getMemberNameByLinkName(link.getName());
			if (!(memberName == null || memberName.isEmpty())) {
				continue;
			}
			ITypeInfoItem matchingMember = null;
			String typeNamePluralLower = null;
			for (ITypeInfoItem member : typeInfo.getMembers()) {
				Class<?> elementType = member.getElementType();
				if (table.isIgnoredMember(member.getName())) {
					continue;
				}
				if (!relationProvider.isEntityType(member.getElementType())) {
					continue;
				}
				if (!elementType.isAssignableFrom(toType)) {
					if (Collection.class.isAssignableFrom(elementType)) {
						// No generic info at runtime, so we guess by name.
						if (typeNamePluralLower == null) {
							typeNamePluralLower = StringConversionHelper
									.entityNameToPlural(objectCollector,
											typeInfoProvider.getTypeInfo(toType).getSimpleName())
									.toLowerCase();
						}
						String memberNameLower = member.getName().toLowerCase();
						if (memberNameLower.equals(typeNamePluralLower)) {
							matchingMember = member;
						}
					}
					continue;
				}
				// Check if this member is already configured to another link
				if (table.getLinkByMemberName(member.getName()) != null) {
					continue;
				}
				if (matchingMember != null) {
					// ambiguous property-to-entity relationship so we do nothing automatically here
					throw new IllegalArgumentException(
							"Ambiguous property-to-entity relationship. Automatic mapping for link '"
									+ link.getName()
									+ "' not possible! Multiple properties with the same expected relation type found: "
									+ matchingMember.toString() + " vs. " + member.toString()
									+ " on entity '" + table.getEntityType().getName()
									+ "' both map to entity '" + member.getElementType().getName()
									+ "'");
				}
				matchingMember = member;
			}
			if (matchingMember == null) {
				if (!(memberName == null || memberName.isEmpty())) {
					throw new IllegalArgumentException(
							"Property-to-entity relationship which is explicit defined for member '"
									+ fromType.getName() + "." + memberName
									+ "' not possible. Member not found");
				}
				continue;
			}
			((TableMetaData) link.getFromTable()).mapLink(link.getName(), matchingMember.getName());
		}
	}

	@Override
	public boolean isFieldNullable(Connection connection, IFieldMetaData field)
			throws SQLException {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public boolean isLinkArchiveTable(String tableName) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public ITableMetaData mapArchiveTable(String tableName, Class<?> entityType) {
		ITableMetaData table = getTableByName(tableName);
		if (table == null) {
			throw new IllegalArgumentException("No table with name '" + tableName + "' found");
		}
		Class<?> mappedEntityType = table.getEntityType();
		if (mappedEntityType != null && !mappedEntityType.equals(entityType)) {
			throw new IllegalArgumentException("Table '" + tableName
					+ "' already mapped to entity '" + mappedEntityType.getSimpleName() + "'");
		}
		TableMetaData tableInst = (TableMetaData) table;
		tableInst.setEntityType(entityType);
		tableInst.setArchive(true);
		return table;
	}

	@Override
	public ILinkMetaData mapLink(ILinkMetaData link) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public void mapPermissionGroupTable(ITableMetaData permissionGroupTable,
			ITableMetaData targetTable) {
		TableMetaData tableInst = (TableMetaData) permissionGroupTable;
		tableInst.setPermissionGroup(true);

		IFieldMetaData userField = tableInst.getFieldByName(PermissionGroup.userIdName);
		IFieldMetaData permissionGroupField =
				tableInst.getFieldByName(PermissionGroup.permGroupIdName);
		IFieldMetaData readPermissionField =
				tableInst.getFieldByName(PermissionGroup.readPermColumName);
		IFieldMetaData updatePermissionField =
				tableInst.getFieldByName(PermissionGroup.updatePermColumName);
		IFieldMetaData deletePermissionField =
				tableInst.getFieldByName(PermissionGroup.deletePermColumName);
		IFieldMetaData permissionGroupFieldOnTarget =
				targetTable.getFieldByName(PermissionGroup.permGroupIdNameOfData);

		if (userField == null) {
			if (log.isWarnEnabled()) {
				loggerHistory.warnOnce(log, this,
						"No column found " + tableInst.getName() + "." + PermissionGroup.userIdName
								+ ": SQL based security is deactivated for table "
								+ targetTable.getName());
			}
			return;
		}
		if (permissionGroupField == null) {

			if (log.isWarnEnabled()) {
				loggerHistory.warnOnce(log, this, "No column found " + tableInst.getName() + "."
						+ PermissionGroup.permGroupIdName
						+ ": SQL based security is deactivated for table " + targetTable.getName());
			}
			return;
		}
		if (readPermissionField == null) {
			if (log.isWarnEnabled()) {
				loggerHistory.warnOnce(log, this, "No column found " + tableInst.getName() + "."
						+ PermissionGroup.readPermColumName
						+ ": SQL based security is deactivated for table " + targetTable.getName());
			}
			return;
		}
		if (updatePermissionField == null) {
			if (log.isWarnEnabled()) {
				loggerHistory.warnOnce(log, this, "No column found " + tableInst.getName() + "."
						+ PermissionGroup.updatePermColumName
						+ ": SQL based security is deactivated for table " + targetTable.getName());
			}
			return;
		}
		if (deletePermissionField == null) {
			if (log.isWarnEnabled()) {
				loggerHistory.warnOnce(log, this, "No column found " + tableInst.getName() + "."
						+ PermissionGroup.deletePermColumName
						+ ": SQL based security is deactivated for table " + targetTable.getName());
			}
			return;
		}
		if (permissionGroupFieldOnTarget == null) {
			if (log.isWarnEnabled()) {
				loggerHistory.warnOnce(log, this, "No column found " + targetTable.getName() + "."
						+ PermissionGroup.permGroupIdNameOfData
						+ ": SQL based security is deactivated for table " + targetTable.getName());
			}
			return;
		}
		PermissionGroup permissionGroup = serviceContext.registerBean(PermissionGroup.class)//
				.propertyValue("UserField", userField)//
				.propertyValue("PermissionGroupField", permissionGroupField)//
				.propertyValue("ReadPermissionField", readPermissionField)//
				.propertyValue("UpdatePermissionField", updatePermissionField)//
				.propertyValue("DeletePermissionField", deletePermissionField)//
				.propertyValue("PermissionGroupFieldOnTarget", permissionGroupFieldOnTarget)//
				.propertyValue("Table", permissionGroupTable)//
				.propertyValue("TargetTable", targetTable)//
				.finish();

		if (!nameToPermissionGroupTableDict.putIfNotExists(targetTable.getName(),
				permissionGroup)) {
			throw new IllegalStateException(
					"A permission group is already mapped to table '" + targetTable + "'");
		}
	}

	@Override
	public ITableMetaData mapTable(String tableName, Class<?> entityType) {
		ITableMetaData table = getTableByName(tableName);
		if (table == null) {
			throw new IllegalArgumentException("No table with name '" + tableName + "' found");
		}
		Class<?> mappedEntityType = table.getEntityType();
		if (mappedEntityType != null && !mappedEntityType.equals(entityType)) {
			throw new IllegalArgumentException(
					"Table '" + tableName + "' already mapped to entity '"
							+ typeInfoProvider.getTypeInfo(mappedEntityType).getSimpleName() + "'");
		}
		((TableMetaData) table).setEntityType(entityType);
		return table;
	}

	@Override
	public ITableMetaData registerNewTable(Connection connection, String fqTableName) {
		throw new UnsupportedOperationException("Not implemented");
	}

	public void setMaxNameLength(int maxNameLength) {
		this.maxNameLength = maxNameLength;
	}

	public void setName(String name) {
		this.name = name;
	}
}
