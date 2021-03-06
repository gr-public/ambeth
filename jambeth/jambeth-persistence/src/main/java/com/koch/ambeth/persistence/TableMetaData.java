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

import java.util.HashMap;
import java.util.List;

import com.koch.ambeth.ioc.IInitializingBean;
import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.ioc.config.Property;
import com.koch.ambeth.log.ILogger;
import com.koch.ambeth.log.LogInstance;
import com.koch.ambeth.merge.metadata.IMemberTypeProvider;
import com.koch.ambeth.merge.transfer.ObjRef;
import com.koch.ambeth.persistence.api.IDirectedLinkMetaData;
import com.koch.ambeth.persistence.api.IFieldMetaData;
import com.koch.ambeth.persistence.api.ITableMetaData;
import com.koch.ambeth.service.merge.IEntityMetaDataProvider;
import com.koch.ambeth.service.merge.model.IEntityMetaData;
import com.koch.ambeth.service.metadata.Member;
import com.koch.ambeth.service.metadata.RelationMember;
import com.koch.ambeth.util.ParamChecker;
import com.koch.ambeth.util.collections.ArrayList;
import com.koch.ambeth.util.collections.HashSet;
import com.koch.ambeth.util.objectcollector.IThreadLocalObjectCollector;

public class TableMetaData implements ITableMetaData, IInitializingBean {
	public static final String P_NAME = "Name";

	public static final String P_INITIAL_VERSION = "InitialVersion";

	public static final String P_FULL_QUALIFIED_ESCAPED_NAME = "FullqualifiedEscapedName";

	public static final String P_VIEW_BASED = "ViewBased";

	public static final short[] EMPTY_SHORT_ARRAY = new short[0];

	public static final IFieldMetaData[] EMPTY_FIELD_ARRAY = new IFieldMetaData[0];

	public static final IFieldMetaData[][] EMPTY_FIELD_ARRAY_ARRAY = new IFieldMetaData[0][0];

	@LogInstance
	private ILogger log;

	@Autowired(optional = true)
	protected IConnectionDialect connectionDialect;

	@Autowired
	protected IEntityMetaDataProvider entityMetaDataProvider;

	@Autowired
	protected IMemberTypeProvider memberTypeProvider;

	@Autowired
	protected IThreadLocalObjectCollector objectCollector;

	@Property
	protected String name;

	@Property
	protected boolean viewBased;

	protected final ArrayList<IFieldMetaData> primitiveFields;

	protected final ArrayList<IFieldMetaData> fulltextFields;

	protected final ArrayList<IFieldMetaData> allFields;

	protected final ArrayList<IDirectedLinkMetaData> links;

	protected final HashMap<String, Integer> fieldNameToFieldIndexDict = new HashMap<>();

	protected final HashMap<String, IFieldMetaData> fieldNameToFieldDict = new HashMap<>();

	protected final HashSet<String> fieldNameToIgnoreDict = new HashSet<>();

	protected final HashMap<String, IFieldMetaData> memberNameToFieldDict = new HashMap<>();

	protected final HashSet<String> memberNameToIgnoreDict = new HashSet<>();

	protected final HashMap<String, IDirectedLinkMetaData> fieldNameToLinkDict = new HashMap<>();

	protected final HashMap<String, IDirectedLinkMetaData> linkNameToLinkDict = new HashMap<>();

	protected final HashMap<String, IDirectedLinkMetaData> memberNameToLinkDict = new HashMap<>();

	protected final HashMap<String, String> linkNameToMemberNameDict = new HashMap<>();

	protected Class<?> entityType;

	protected boolean archive = false, permissionGroup = false;

	protected IFieldMetaData[] idFields;

	protected IFieldMetaData versionField;

	protected IFieldMetaData descriminatorField;

	protected IFieldMetaData createdByField;

	protected IFieldMetaData createdOnField;

	protected IFieldMetaData updatedByField;

	protected IFieldMetaData updatedOnField;

	protected String sequenceName;

	protected IFieldMetaData[] alternateIdFields = EMPTY_FIELD_ARRAY;

	protected short[] alternateIdFieldIndices = EMPTY_SHORT_ARRAY;

	private Object initialVersion;

	public TableMetaData() {
		primitiveFields = new ArrayList<>();
		fulltextFields = new ArrayList<>();
		allFields = new ArrayList<>();
		links = new ArrayList<>();
	}

	@Override
	public void afterPropertiesSet() throws Throwable {
		ParamChecker.assertNotNull(name, "name");
	}

	@Override
	public List<IFieldMetaData> getAllFields() {
		return allFields;
	}

	@Override
	public int getAlternateIdCount() {
		return alternateIdFields.length;
	}

	@Override
	public short[] getAlternateIdFieldIndicesInFields() {
		return alternateIdFieldIndices;
	}

	@Override
	public IFieldMetaData[] getAlternateIdFields() {
		return alternateIdFields;
	}

	@Override
	public IFieldMetaData getCreatedByField() {
		return createdByField;
	}

	@Override
	public IFieldMetaData getCreatedOnField() {
		return createdOnField;
	}

	@Override
	public IFieldMetaData getDescriminatorField() {
		return descriminatorField;
	}

	@Override
	public Class<?> getEntityType() {
		return entityType;
	}

	@Override
	public IFieldMetaData getFieldByMemberName(String memberName) {
		return memberNameToFieldDict.get(memberName);
	}

	@Override
	public IFieldMetaData getFieldByName(String fieldName) {
		if (fieldName == null || fieldName.isEmpty()) {
			return null;
		}
		if (connectionDialect != null) {
			fieldName = connectionDialect.toDefaultCase(fieldName);
		}
		if (idFields != null) {
			for (IFieldMetaData idField : idFields) {
				if (fieldName.equals(idField.getName())) {
					return idField;
				}
			}
		}
		if (versionField != null && fieldName.equals(versionField.getName())) {
			return versionField;
		}
		return fieldNameToFieldDict.get(fieldName);
	}

	@Override
	public IFieldMetaData getFieldByPropertyName(String propertyName) {
		IFieldMetaData field = getFieldByMemberName(propertyName);
		if (field == null) {
			IDirectedLinkMetaData link = getLinkByMemberName(propertyName);
			if (link != null) {
				field = link.getFromField();
			}
		}
		if (field == null) {
			if (getIdField().getMember().getName().equals(propertyName)) {
				field = getIdField();
			} else if (getVersionField().getMember().getName().equals(propertyName)) {
				field = getVersionField();
			}
		}

		return field;
	}

	@Override
	public int getFieldIndexByName(String fieldName) {
		Integer index = fieldNameToFieldIndexDict.get(fieldName);
		if (index == null) {
			return -1;
		}
		return index.intValue();
	}

	@Override
	public String getFullqualifiedEscapedName() {
		return getName();
	}

	@Override
	public List<IFieldMetaData> getFulltextFields() {
		return fulltextFields;
	}

	@Override
	public IFieldMetaData getIdField() {
		return idFields != null ? idFields[0] : null;
	}

	@Override
	public IFieldMetaData getIdFieldByAlternateIdIndex(int idIndex) {
		if (idIndex == ObjRef.PRIMARY_KEY_INDEX) {
			return getIdField();
		}
		return getAlternateIdFields()[idIndex];
	}

	@Override
	public IFieldMetaData[] getIdFields() {
		return idFields;
	}

	@Override
	public Object getInitialVersion() {
		return initialVersion;
	}

	@Override
	public IDirectedLinkMetaData getLinkByFieldName(String fieldName) {
		return fieldNameToLinkDict.get(fieldName);
	}

	@Override
	public IDirectedLinkMetaData getLinkByMemberName(String memberName) {
		return memberNameToLinkDict.get(memberName);
	}

	@Override
	public IDirectedLinkMetaData getLinkByName(String linkName) {
		return linkNameToLinkDict.get(linkName);
	}

	@Override
	public List<IDirectedLinkMetaData> getLinks() {
		return links;
	}

	@Override
	public String getMemberNameByLinkName(String linkName) {
		return linkNameToMemberNameDict.get(linkName);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<IFieldMetaData> getPrimitiveFields() {
		return primitiveFields;
	}

	@Override
	public String getSequenceName() {
		return sequenceName;
	}

	@Override
	public IFieldMetaData getUpdatedByField() {
		return updatedByField;
	}

	@Override
	public IFieldMetaData getUpdatedOnField() {
		return updatedOnField;
	}

	@Override
	public IFieldMetaData getVersionField() {
		return versionField;
	}

	@Override
	public boolean isArchive() {
		return archive;
	}

	@Override
	public boolean isIgnoredField(String fieldName) {
		return fieldNameToIgnoreDict.contains(fieldName);
	}

	@Override
	public boolean isIgnoredMember(String memberName) {
		return memberNameToIgnoreDict.contains(memberName);
	}

	@Override
	public boolean isPermissionGroup() {
		return permissionGroup;
	}

	@Override
	public boolean isViewBased() {
		return viewBased;
	}

	public void mapField(FieldMetaData field) {
		String fieldName = field.getName();
		if (getFieldByName(fieldName) != null) {
			throw new RuntimeException(
					"Field '" + getName() + "." + fieldName + "' already configured");
		}
		primitiveFields.add(field);
		allFields.add(field);
		Integer index = Integer.valueOf(fieldNameToFieldIndexDict.size());
		field.setIndexOnTable(index.intValue());
		fieldNameToFieldIndexDict.put(fieldName, index);
		fieldNameToFieldDict.put(fieldName, field);
		fieldNameToFieldDict.put(fieldName.toUpperCase(), field);
		fieldNameToFieldDict.put(fieldName.toLowerCase(), field);
	}

	@Override
	public IFieldMetaData mapField(String fieldName, String memberName) {
		IEntityMetaData metaData = entityMetaDataProvider.getMetaData(entityType);
		Member member = metaData.getMemberByName(memberName);

		if (member == null) {
			throw new NullPointerException("Member '" + getEntityType().getName() + "." + memberName
					+ "' not found to map to field '" + getName() + "." + fieldName + "'");
		}
		IFieldMetaData field = getFieldByName(fieldName);
		if (field == null) {
			throw new NullPointerException(
					"Field '" + getName() + "." + fieldName + "' not found to map to member '"
							+ getEntityType().getName() + "." + memberName + "'");
		}
		FieldMetaData fieldInstance = (FieldMetaData) field;
		fieldInstance.setMember(member);

		boolean isIdOrVersionField = false;
		if (idFields != null) {
			for (IFieldMetaData idField : idFields) {
				if (idField == field) {
					isIdOrVersionField = true;
					break;
				}
			}
		}
		if (field == versionField) {
			isIdOrVersionField = true;
		}
		if (!isIdOrVersionField) {
			memberNameToFieldDict.put(memberName, field);
		}
		return field;
	}

	@Override
	public void mapIgnore(String fieldName, String memberName) {
		if (fieldName != null) {
			fieldNameToIgnoreDict.add(fieldName);
			fieldNameToIgnoreDict.add(fieldName.toUpperCase());
			fieldNameToIgnoreDict.add(fieldName.toLowerCase());
		}
		if (memberName != null) {
			memberNameToIgnoreDict.add(memberName);
		}
	}

	public void mapLink(IDirectedLinkMetaData link) {
		String linkName = link.getName();
		if (getLinkByName(linkName) != null) {
			throw new RuntimeException("Link '" + getName() + "." + link + "' already configured");
		}
		links.add(link);
		linkNameToLinkDict.put(linkName, link);
		linkNameToLinkDict.put(linkName.toUpperCase(), link);
		linkNameToLinkDict.put(linkName.toLowerCase(), link);
		String fieldName = link.getFromField().getName();
		fieldNameToLinkDict.put(fieldName, link);
		fieldNameToLinkDict.put(fieldName.toUpperCase(), link);
		fieldNameToLinkDict.put(fieldName.toLowerCase(), link);
	}

	@Override
	public IDirectedLinkMetaData mapLink(String linkName, String memberName) {
		RelationMember member = memberTypeProvider.getRelationMember(getEntityType(), memberName);
		if (member == null) {
			throw new NullPointerException("Member '" + getEntityType().getName() + "." + memberName
					+ "' not found to map to link '" + linkName + "'");
		}
		IDirectedLinkMetaData link = getLinkByName(linkName);
		if (link == null) {
			throw new NullPointerException("Link '" + linkName + "' not found to map to member '"
					+ getEntityType().getName() + "." + memberName + "'");
		}
		DirectedLinkMetaData linkInstance = (DirectedLinkMetaData) link;
		linkInstance.setMember(member);
		// TODO: REGRESSION - This is related to the current RelationAutomappingTest
		// Comment can be completely removed after the test is solved
		// if (link.isPersistingLink())
		// {
		memberNameToLinkDict.put(memberName, link);
		// }
		linkNameToMemberNameDict.put(linkName, memberName);
		linkNameToMemberNameDict.put(linkName.toUpperCase(), memberName);
		linkNameToMemberNameDict.put(linkName.toLowerCase(), memberName);
		return link;
	}

	protected void refreshAlternateIdFieldIndices() {
		if (alternateIdFields == null || alternateIdFields.length == 0 || allFields == null
				|| allFields.isEmpty()) {
			alternateIdFieldIndices = EMPTY_SHORT_ARRAY;
			return;
		}
		alternateIdFieldIndices = new short[alternateIdFields.length];
		for (int a = alternateIdFields.length; a-- > 0;) {
			short index = -1;
			String alternateIdFieldName = alternateIdFields[a].getName();
			for (int b = allFields.size(); b-- > 0;) {
				if (alternateIdFieldName.equals(allFields.get(b).getName())) {
					index = (short) b;
					break;
				}
			}
			if (index == -1) {
				throw new IllegalArgumentException("Alternate id member '" + alternateIdFieldName
						+ "' has to be a valid primitive member");
			}
			alternateIdFieldIndices[a] = index;
		}
	}

	public void setAlternateIdFields(IFieldMetaData[] alternateIdFields) {
		this.alternateIdFields = alternateIdFields;
		refreshAlternateIdFieldIndices();
	}

	public void setArchive(boolean archive) {
		this.archive = archive;
	}

	public void setCreatedByField(IFieldMetaData createdByField) {
		this.createdByField = createdByField;
	}

	public void setCreatedOnField(IFieldMetaData createdOnField) {
		this.createdOnField = createdOnField;
	}

	public void setDescriminatorField(IFieldMetaData descriminatorField) {
		this.descriminatorField = descriminatorField;
		fieldNameToFieldDict.put(descriminatorField.getName(), descriminatorField);
		fieldNameToFieldDict.put(descriminatorField.getName().toUpperCase(), descriminatorField);
		fieldNameToFieldDict.put(descriminatorField.getName().toLowerCase(), descriminatorField);
	}

	public void setEntityType(Class<?> entityType) {
		this.entityType = entityType;

		// formerly in afterPropertiesSet, but now only active if an explicit entity mapping is done
		if (!viewBased) {
			ParamChecker.assertNotNull(idFields, "idField (in " + name + ")");
		}
		if (log.isWarnEnabled() && versionField == null) {
			log.warn("No version field (in " + name + ")");
		}
	}

	public void setFulltextFieldsByNames(List<String> names) {
		for (int i = names.size(); i-- > 0;) {
			IFieldMetaData field = getFieldByName(names.get(i));
			if (field != null) {
				fulltextFields.add(field);
			}
		}
	}

	public void setIdFields(IFieldMetaData[] idFields) {
		this.idFields = idFields;
		for (IFieldMetaData field : idFields) {
			fieldNameToFieldDict.put(field.getName(), field);
			fieldNameToFieldDict.put(field.getName().toUpperCase(), field);
			fieldNameToFieldDict.put(field.getName().toLowerCase(), field);
		}
	}

	public void setInitialVersion(Object initialVersion) {
		this.initialVersion = initialVersion;
	}

	public void setPermissionGroup(boolean permissionGroup) {
		this.permissionGroup = permissionGroup;
	}

	public void setSequenceName(String sequenceName) {
		this.sequenceName = sequenceName;
	}

	public void setUpdatedByField(IFieldMetaData updatedByField) {
		this.updatedByField = updatedByField;
	}

	public void setUpdatedOnField(IFieldMetaData updatedOnField) {
		this.updatedOnField = updatedOnField;
	}

	public void setVersionField(IFieldMetaData versionField) {
		this.versionField = versionField;
		fieldNameToFieldDict.put(versionField.getName(), versionField);
		fieldNameToFieldDict.put(versionField.getName().toUpperCase(), versionField);
		fieldNameToFieldDict.put(versionField.getName().toLowerCase(), versionField);
	}

	@Override
	public String toString() {
		return "Table: " + getName();
	}
}
