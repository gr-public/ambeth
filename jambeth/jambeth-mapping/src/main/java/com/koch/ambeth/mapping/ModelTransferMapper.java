package com.koch.ambeth.mapping;

/*-
 * #%L
 * jambeth-mapping
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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.koch.ambeth.cache.ICacheIntern;
import com.koch.ambeth.cache.proxy.IValueHolderContainer;
import com.koch.ambeth.cache.rootcachevalue.RootCacheValue;
import com.koch.ambeth.filter.IFilterDescriptor;
import com.koch.ambeth.filter.ISortDescriptor;
import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.ioc.config.Property;
import com.koch.ambeth.log.ILogger;
import com.koch.ambeth.log.LogInstance;
import com.koch.ambeth.mapping.config.MappingConfigurationConstants;
import com.koch.ambeth.mapping.exception.MappingException;
import com.koch.ambeth.merge.IEntityFactory;
import com.koch.ambeth.merge.IObjRefHelper;
import com.koch.ambeth.merge.IObjRefProvider;
import com.koch.ambeth.merge.cache.AbstractCacheValue;
import com.koch.ambeth.merge.cache.CacheDirective;
import com.koch.ambeth.merge.cache.ICache;
import com.koch.ambeth.merge.cache.ICacheModification;
import com.koch.ambeth.merge.cache.IWritableCache;
import com.koch.ambeth.merge.cache.ValueHolderState;
import com.koch.ambeth.merge.proxy.IEntityMetaDataHolder;
import com.koch.ambeth.merge.proxy.IObjRefContainer;
import com.koch.ambeth.merge.transfer.ObjRef;
import com.koch.ambeth.merge.util.DirectValueHolderRef;
import com.koch.ambeth.merge.util.ICacheHelper;
import com.koch.ambeth.merge.util.IPrefetchHelper;
import com.koch.ambeth.merge.util.IPrefetchState;
import com.koch.ambeth.service.merge.IEntityMetaDataProvider;
import com.koch.ambeth.service.merge.IValueObjectConfig;
import com.koch.ambeth.service.merge.ValueObjectMemberType;
import com.koch.ambeth.service.merge.model.IEntityMetaData;
import com.koch.ambeth.service.merge.model.IObjRef;
import com.koch.ambeth.service.metadata.Member;
import com.koch.ambeth.service.metadata.PrimitiveMember;
import com.koch.ambeth.service.metadata.RelationMember;
import com.koch.ambeth.service.typeinfo.TypeInfoItem;
import com.koch.ambeth.util.IConversionHelper;
import com.koch.ambeth.util.IDisposable;
import com.koch.ambeth.util.ListUtil;
import com.koch.ambeth.util.collections.ArrayList;
import com.koch.ambeth.util.collections.EmptyList;
import com.koch.ambeth.util.collections.HashMap;
import com.koch.ambeth.util.collections.HashSet;
import com.koch.ambeth.util.collections.IList;
import com.koch.ambeth.util.collections.IMap;
import com.koch.ambeth.util.collections.ISet;
import com.koch.ambeth.util.collections.IdentityHashMap;
import com.koch.ambeth.util.collections.IdentityHashSet;
import com.koch.ambeth.util.exception.RuntimeExceptionUtil;
import com.koch.ambeth.util.model.IDataObject;
import com.koch.ambeth.util.objectcollector.IThreadLocalObjectCollector;
import com.koch.ambeth.util.typeinfo.IPropertyInfoProvider;
import com.koch.ambeth.util.typeinfo.ITypeInfoItem;
import com.koch.ambeth.util.typeinfo.ITypeInfoProvider;
import com.koch.ambeth.util.typeinfo.NullEquivalentValueUtil;

public class ModelTransferMapper implements IMapperService, IDisposable {
	protected static final Object NOT_YET_READY = new Object();

	@LogInstance
	private ILogger log;

	@Autowired
	protected ICacheHelper cacheHelper;

	@Autowired
	protected ICacheModification cacheModification;

	@Autowired
	protected IConversionHelper conversionHelper;

	@Autowired
	protected ICache cache;

	@Autowired
	protected IEntityFactory entityFactory;

	@Autowired
	protected IEntityMetaDataProvider entityMetaDataProvider;

	@Autowired
	protected IListTypeHelper listTypeHelper;

	@Autowired
	protected IDedicatedMapperRegistry mapperExtensionRegistry;

	@Autowired
	protected IPrefetchHelper prefetchHelper;

	@Autowired
	protected IThreadLocalObjectCollector objectCollector;

	@Autowired
	protected IObjRefHelper oriHelper;

	@Autowired
	protected IPropertyInfoProvider propertyInfoProvider;

	@Autowired
	protected ITypeInfoProvider typeInfoProvider;

	protected final HashMap<Class<?>, Map<String, ITypeInfoItem>> typeToTypeInfoMap = new HashMap<>();

	protected final HashMap<IObjRef, IObjRef> alreadyCreatedObjRefsMap = new HashMap<>();

	protected final IdentityHashMap<Object, IMap<Class<?>, Object>> boToSpecifiedVOMap = new IdentityHashMap<>();

	protected final IdentityHashMap<Object, Object> voToBoMap = new IdentityHashMap<>();

	protected final HashMap<CompositIdentityClassKey, Object> reverseRelationMap = new HashMap<>();

	protected final IdentityHashSet<Object> allBOsToKeepInCache = new IdentityHashSet<>();

	protected final IdentityHashSet<Object> bosToRemoveTempIdFrom = new IdentityHashSet<>();

	protected final IdentityHashSet<Object> vosToRemoveTempIdFrom = new IdentityHashSet<>();

	protected long nextTempId = -1;

	@Property(name = MappingConfigurationConstants.InitDirectRelationsInBusinessObjects, defaultValue = "true")
	protected boolean initDirectRelationsInBusinessObjects;

	@Override
	public void dispose() {
		conversionHelper = null;
		entityMetaDataProvider = null;
		objectCollector = null;
		oriHelper = null;
		cache = null;
	}

	@Override
	public void close() throws Exception {
		dispose();
	}

	@Override
	public <T> T mapToBusinessObject(Object valueObject) {
		if (valueObject == null) {
			return null;
		}
		List<Object> valueObjects = Arrays.asList(new Object[] { valueObject });
		List<T> results = mapToBusinessObjectList(valueObjects);
		return results.get(0);
	}

	@Override
	public <T> T mapToBusinessObjectListFromListType(Object listTypeObject) {
		try {
			@SuppressWarnings("unchecked")
			List<Object> valueObjectList = (List<Object>) listTypeHelper.unpackListType(listTypeObject);
			return mapToBusinessObjectList(valueObjectList);
		}
		catch (Exception e) {
			throw RuntimeExceptionUtil.mask(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T mapToBusinessObjectList(List<?> valueObjectList) {
		if (valueObjectList.isEmpty()) {
			return (T) Collections.emptyList();
		}
		ICacheIntern cache = (ICacheIntern) this.cache.getCurrentCache();
		IEntityMetaDataProvider entityMetaDataProvider = this.entityMetaDataProvider;
		IdentityHashMap<Object, Object> voToBoMap = this.voToBoMap;
		ArrayList<Object> allValueObjects = new ArrayList<>(valueObjectList.size());
		boolean acquiredHardRefs = cache.acquireHardRefTLIfNotAlready();
		boolean oldActive = cacheModification.isActive();
		cacheModification.setActive(true);
		try {
			resolveAllValueObjectsDirectly(valueObjectList, allValueObjects,
					IdentityHashSet.create(valueObjectList.size()), null);

			mapBosByVos(allValueObjects, cache);

			for (int i = allValueObjects.size(); i-- > 0;) {
				resolvePrimitiveProperties(allValueObjects.get(i), cache);
			}

			ArrayList<DirectValueHolderRef> boToPendingRelationsList = new ArrayList<>();
			HashSet<IObjRef> referencedBOsSet = new HashSet<>();
			HashMap<IObjRef, IObjRef> alreadyCreatedObjRefMap = new HashMap<>();
			try {
				for (int i = allValueObjects.size(); i-- > 0;) {
					collectReferencedBusinessObjects(allValueObjects.get(i), referencedBOsSet,
							boToPendingRelationsList, alreadyCreatedObjRefMap, cache);
				}
				IList<IObjRef> referencedBOsList = referencedBOsSet.toList();

				if (initDirectRelationsInBusinessObjects) {
					IPrefetchState prefetchState = prefetchHelper.prefetch(boToPendingRelationsList);
					// Store retrieved BOs to hard ref to suppress Weak GC handling of cache
					allBOsToKeepInCache.add(prefetchState);

					IList<Object> referencedBOs = cache.getObjects(referencedBOsList,
							CacheDirective.failEarlyAndReturnMisses());

					for (int a = referencedBOs.size(); a-- > 0;) {
						Object referencedBO = referencedBOs.get(a);
						if (referencedBO == null) {
							throw new MappingException(
									"At least one entity could not be found: " + referencedBOsList.get(a).toString());
						}
					}
					// // Allocate specific pending relations to their bo fields
					// for (int a = boToPendingRelationsList.size(); a-- > 0;)
					// {
					// PendingRelation pendingRelation = boToPendingRelationsList.get(a);
					// Object businessObject = pendingRelation.getBusinessObject();
					// IRelationInfoItem member = pendingRelation.getMember();
					// IList<IObjRef> pendingObjRefs = pendingRelation.getPendingObjRefs();
					//
					// // Everything which gets missed by now does not exist in the DB.
					// // FailEarly is important to suppress redundant tries of previously failed loadings
					// IList<Object> pendingObjects = childCache.getObjects(pendingObjRefs,
					// CacheDirective.failEarly());
					//
					// Object convertedPendingObjects = convertPrimitiveValue(pendingObjects,
					// member.getElementType(), member);
					// member.setValue(businessObject, convertedPendingObjects);
					// }
				}
			}
			finally {
				alreadyCreatedObjRefMap = null;
			}

			ArrayList<Object> allBusinessObjects = new ArrayList<>(allValueObjects.size());

			ArrayList<DirectValueHolderRef> objRefContainers = new ArrayList<>(allValueObjects.size());
			for (int i = allValueObjects.size(); i-- > 0;) {
				Object valueObject = allValueObjects.get(i);
				Object businessObject = voToBoMap.get(valueObject);

				IDedicatedMapper dedicatedMapper = mapperExtensionRegistry
						.getDedicatedMapper(businessObject.getClass());
				if (dedicatedMapper != null) {
					dedicatedMapper.applySpecialMapping(businessObject, valueObject, CopyDirection.VO_TO_BO);
				}

				allBusinessObjects.add(businessObject);
				if (!initDirectRelationsInBusinessObjects) {
					continue;
				}
				IEntityMetaData metaData = getMetaData(businessObject);
				RelationMember[] relationMembers = metaData.getRelationMembers();
				if (relationMembers.length == 0) {
					continue;
				}
				IValueHolderContainer vhc = (IValueHolderContainer) businessObject;
				for (int relationIndex = relationMembers.length; relationIndex-- > 0;) {
					if (ValueHolderState.INIT == vhc.get__State(relationIndex)) {
						continue;
					}
					objRefContainers.add(new DirectValueHolderRef(vhc, relationMembers[relationIndex]));
				}
			}
			if (!objRefContainers.isEmpty()) {
				prefetchHelper.prefetch(objRefContainers);
			}
			ArrayList<IObjRef> orisToGet = new ArrayList<>(valueObjectList.size());

			for (int i = 0, size = valueObjectList.size(); i < size; i++) {
				Object rootValueObject = valueObjectList.get(i);
				IValueObjectConfig config = getValueObjectConfig(rootValueObject.getClass());
				IEntityMetaData metaData = entityMetaDataProvider.getMetaData(config.getEntityType());
				Map<String, ITypeInfoItem> boNameToVoMember = getTypeInfoMapForVo(config);
				Object id = getIdFromValueObject(rootValueObject, metaData, boNameToVoMember, config);

				ObjRef objRef = new ObjRef(metaData.getEntityType(), ObjRef.PRIMARY_KEY_INDEX, id, null);
				orisToGet.add(objRef);
			}
			List<Object> businessObjectList = cache.getObjects(orisToGet,
					CacheDirective.failEarlyAndReturnMisses());
			clearObjectsWithTempIds((IWritableCache) cache);

			for (int a = allBusinessObjects.size(); a-- > 0;) {
				Object businessObject = allBusinessObjects.get(a);
				if (businessObject instanceof IDataObject) {
					((IDataObject) businessObject).setToBeUpdated(true);
				}
			}
			return (T) businessObjectList;
		}
		catch (Exception e) {
			throw RuntimeExceptionUtil.mask(e);
		}
		finally {
			cacheModification.setActive(oldActive);
			cache.clearHardRefs(acquiredHardRefs);
		}
	}

	@Override
	public <T> T mapToValueObject(Object businessObject, Class<T> valueObjectType) {
		if (businessObject == null) {
			return null;
		}
		List<Object> businessObjects = Arrays.asList(new Object[] { businessObject });
		List<T> results = mapToValueObjectList(businessObjects, valueObjectType);
		return results.get(0);
	}

	@Override
	public <L> L mapToValueObjectListType(List<?> businessObjectList, Class<?> valueObjectType,
			Class<L> listType) {
		try {
			List<Object> valueObjectList = mapToValueObjectList(businessObjectList, valueObjectType);
			return listTypeHelper.packInListType(valueObjectList, listType);
		}
		catch (Exception e) {
			throw RuntimeExceptionUtil.mask(e);
		}
	}

	@Override
	public <L> L mapToValueObjectRefListType(List<?> businessObjectList,
			Class<L> valueObjectRefListType) {
		try {
			List<Object> valueObjectList = mapToValueObjectRefList(businessObjectList);
			return listTypeHelper.packInListType(valueObjectList, valueObjectRefListType);
		}
		catch (Exception e) {
			throw RuntimeExceptionUtil.mask(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T mapToValueObjectList(List<?> businessObjectList, Class<?> valueObjectType) {
		if (businessObjectList.isEmpty()) {
			return (T) Collections.emptyList();
		}
		ICache cache = this.cache.getCurrentCache();
		// Checking for correct types
		Object bo = businessObjectList.get(0);
		IEntityMetaData boMetaData = getMetaData(bo);
		Class<?> businessObjectType = boMetaData.getEntityType();
		IValueObjectConfig config = getValueObjectConfig(valueObjectType);
		if (!config.getEntityType().equals(businessObjectType)) {
			throw new IllegalArgumentException("'" + businessObjectType.getName()
					+ "' cannot be mapped to '" + valueObjectType.getName() + "'");
		}

		ArrayList<Object> pendingValueHolders = new ArrayList<>();
		ArrayList<Runnable> runnables = new ArrayList<>();
		List<Object> valueObjectList = new java.util.ArrayList<>(businessObjectList.size());
		for (int i = 0; i < businessObjectList.size(); i++) {
			Object businessObject = businessObjectList.get(i);
			Object valueObject = subMapToCachedValueObject(businessObject, valueObjectType,
					pendingValueHolders, runnables);

			valueObjectList.add(valueObject);
		}
		while (!pendingValueHolders.isEmpty() || !runnables.isEmpty()) {
			if (!pendingValueHolders.isEmpty()) {
				prefetchHelper.prefetch(pendingValueHolders);
				pendingValueHolders.clear();
			}
			ArrayList<Runnable> runnablesClone = new ArrayList<>(runnables);

			// Reset ORIGINAL lists because they may have been referenced from within cascading runnables
			runnables.clear();

			for (int a = 0, size = runnablesClone.size(); a < size; a++) {
				runnablesClone.get(a).run();
			}
			// PendingValueHolders might be (re-)filled after the runnables. So we need a while loop
		}

		clearObjectsWithTempIds((IWritableCache) cache);

		return (T) valueObjectList;
	}

	@SuppressWarnings("unchecked")
	protected <T> List<T> mapToValueObjectRefList(List<?> businessObjectList) {
		if (businessObjectList.isEmpty()) {
			return (List<T>) Collections.emptyList();
		}
		// Checking for correct types
		ArrayList<T> refList = new ArrayList<>(businessObjectList.size());

		for (int a = 0, size = businessObjectList.size(); a < size; a++) {
			Object businessObject = businessObjectList.get(a);
			IEntityMetaData metaData = getMetaData(businessObject);

			Member idMember = selectIdMember(metaData);
			Object id = idMember.getValue(businessObject, false);
			if (id == null) {
				throw new IllegalArgumentException(
						"BusinessObject '" + businessObject + "' at index " + a + " does not have a valid ID");
			}
			refList.add((T) id);
		}
		return refList;
	}

	protected void resolveProperties(Object businessObject, final Object valueObject,
			final Collection<Object> pendingValueHolders, final Collection<Runnable> runnables) {
		IEntityMetaData businessObjectMetaData = getMetaData(businessObject);
		final IValueObjectConfig config = entityMetaDataProvider
				.getValueObjectConfig(valueObject.getClass());
		Map<String, ITypeInfoItem> boNameToVoMember = getTypeInfoMapForVo(config);

		copyPrimitives(businessObject, valueObject, config, CopyDirection.BO_TO_VO,
				businessObjectMetaData, boNameToVoMember);

		RelationMember[] relationMembers = businessObjectMetaData.getRelationMembers();
		if (relationMembers.length == 0) {
			return;
		}
		final IObjRefContainer vhc = (IObjRefContainer) businessObject;

		for (int relationIndex = relationMembers.length; relationIndex-- > 0;) {
			RelationMember boMember = relationMembers[relationIndex];
			String boMemberName = boMember.getName();
			String voMemberName = config.getValueObjectMemberName(boMemberName);
			final ITypeInfoItem voMember = boNameToVoMember.get(boMemberName);
			if (config.isIgnoredMember(voMemberName) || voMember == null) {
				continue;
			}
			Object voMemberValue = createVOMemberValue(vhc, relationIndex, boMember, config, voMember,
					pendingValueHolders, runnables);
			if (voMemberValue != NOT_YET_READY) {
				setPropertyValue(valueObject, voMember, voMemberValue);
			}
			else {
				final RelationMember fBoMember = boMember;
				final int fRelationIndex = relationIndex;
				runnables.add(new Runnable() {

					@Override
					public void run() {
						Object voMemberValue = createVOMemberValue(vhc, fRelationIndex, fBoMember, config,
								voMember, pendingValueHolders, runnables);
						if (voMemberValue == NOT_YET_READY) {
							throw new IllegalStateException("Must never happen");
						}
						setPropertyValue(valueObject, voMember, voMemberValue);
					}
				});
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void setPropertyValue(Object valueObject, ITypeInfoItem voMember,
			Object voMemberValue) {
		if (voMember.canWrite()) {
			voMember.setValue(valueObject, voMemberValue);
		}
		else if (voMember.canRead()) {
			Object currentValue = voMember.getValue(valueObject);
			if (currentValue == null) {
				String msg = "Property has only a getter and is null: " + valueObject.getClass().getName()
						+ "." + voMember.getName();
				throw new IllegalStateException(msg);
			}

			Class<?> realType = voMember.getRealType();
			if (Collection.class.isAssignableFrom(realType)) {
				Collection<Object> col = (Collection<Object>) currentValue;
				col.clear();
				col.addAll((Collection<? extends Object>) voMemberValue);
			}
			else {
				String msg = "Handling of getter-only property type " + realType.getName()
						+ " not yet implemented: " + valueObject.getClass().getName() + "."
						+ voMember.getName();
				throw new IllegalStateException(msg);
			}
		}
		else {
			String msg = "Property not accessible: " + valueObject.getClass().getName() + "."
					+ voMember.getName();
			throw new IllegalStateException(msg);
		}
	}

	protected void mapBosByVos(final List<Object> valueObjects, ICacheIntern cache) throws Exception {
		ArrayList<IObjRef> toLoad = new ArrayList<>();
		ArrayList<Object> waitingVOs = new ArrayList<>();
		IEntityMetaDataProvider entityMetaDataProvider = this.entityMetaDataProvider;
		IMap<Object, Object> voToBoMap = this.voToBoMap;
		for (int i = valueObjects.size(); i-- > 0;) {
			Object valueObject = valueObjects.get(i);
			if (valueObject == null || voToBoMap.containsKey(valueObject)) {
				continue;
			}
			IValueObjectConfig config = getValueObjectConfig(valueObject.getClass());
			IEntityMetaData boMetaData = entityMetaDataProvider.getMetaData(config.getEntityType());
			Map<String, ITypeInfoItem> boNameToVoMember = getTypeInfoMapForVo(config);

			Object businessObject = null;
			Object id = getIdFromValueObject(valueObject, boMetaData, boNameToVoMember, config);
			if (id != null) {
				if (initDirectRelationsInBusinessObjects) {
					IObjRef ori = getObjRef(config.getEntityType(), ObjRef.PRIMARY_KEY_INDEX, id,
							alreadyCreatedObjRefsMap);
					toLoad.add(ori);
					waitingVOs.add(valueObject);
				}
				else {
					businessObject = createBusinessObject(boMetaData, cache);
					voToBoMap.put(valueObject, businessObject);
				}
			}
			else {
				businessObject = createBusinessObject(boMetaData, cache);
				setTempIdToValueObject(valueObject, boMetaData, boNameToVoMember, config);
				bosToRemoveTempIdFrom.add(businessObject);
				id = getIdFromValueObject(valueObject, boMetaData, boNameToVoMember, config);
				voToBoMap.put(valueObject, businessObject);
			}
		}

		if (!toLoad.isEmpty()) {
			List<Object> businessObjects = cache.getObjects(toLoad, CacheDirective.returnMisses());
			for (int i = businessObjects.size(); i-- > 0;) {
				Object businessObject = businessObjects.get(i);
				Object valueObject = waitingVOs.get(i);
				if (businessObject == null) {
					IValueObjectConfig config = getValueObjectConfig(valueObject.getClass());
					IEntityMetaData boMetaData = entityMetaDataProvider.getMetaData(config.getEntityType());
					businessObject = createBusinessObject(boMetaData, cache);
				}
				voToBoMap.put(valueObject, businessObject);
			}
		}
	}

	protected Object createBusinessObject(IEntityMetaData boMetaData, ICacheIntern cache) {
		Object businessObject = entityFactory.createEntity(boMetaData);
		cache.assignEntityToCache(businessObject);
		return businessObject;
	}

	protected void resolvePrimitiveProperties(Object valueObject, ICacheIntern cache)
			throws Exception {
		IValueObjectConfig config = getValueObjectConfig(valueObject.getClass());

		IEntityMetaData boMetaData = entityMetaDataProvider.getMetaData(config.getEntityType());
		Map<String, ITypeInfoItem> boNameToVoMember = getTypeInfoMapForVo(config);

		Object businessObject = voToBoMap.get(valueObject);
		if (businessObject == null) {
			throw new IllegalStateException("Must never happen");
		}

		Object[] primitives = copyPrimitives(businessObject, valueObject, config,
				CopyDirection.VO_TO_BO, boMetaData, boNameToVoMember);

		Object id = getIdFromBusinessObject(businessObject, boMetaData);
		Object version = getVersionFromBusinessObject(businessObject, boMetaData);
		cache.addDirect(boMetaData, id, version, businessObject, primitives, null);// relationValues);
	}

	protected void collectReferencedBusinessObjects(Object valueObject,
			ISet<IObjRef> referencedBOsSet, List<DirectValueHolderRef> boToPendingRelationsList,
			Map<IObjRef, IObjRef> alreadyCreatedObjRefMap, ICacheIntern cache) {
		IValueObjectConfig config = getValueObjectConfig(valueObject.getClass());

		IEntityMetaDataProvider entityMetaDataProvider = this.entityMetaDataProvider;
		IEntityMetaData boMetaData = entityMetaDataProvider.getMetaData(config.getEntityType());
		Map<String, ITypeInfoItem> boNameToVoMember = getTypeInfoMapForVo(config);

		IdentityHashMap<Object, Object> voToBoMap = this.voToBoMap;

		RelationMember[] relationMembers = boMetaData.getRelationMembers();
		if (relationMembers.length == 0) {
			return;
		}
		IValueHolderContainer businessObject = (IValueHolderContainer) voToBoMap.get(valueObject);
		if (businessObject == null) {
			throw new IllegalStateException("Must never happen");
		}
		ICacheHelper cacheHelper = this.cacheHelper;
		IConversionHelper conversionHelper = this.conversionHelper;
		IListTypeHelper listTypeHelper = this.listTypeHelper;
		HashMap<CompositIdentityClassKey, Object> reverseRelationMap = this.reverseRelationMap;

		StringBuilder sb = new StringBuilder();

		for (int relationIndex = relationMembers.length; relationIndex-- > 0;) {
			RelationMember boMember = relationMembers[relationIndex];
			String boMemberName = boMember.getName();
			String voMemberName = config.getValueObjectMemberName(boMemberName);

			ITypeInfoItem voMember = boNameToVoMember.get(boMemberName);
			Object voValue = null;
			if (voMember != null) {
				if (config.isIgnoredMember(voMemberName)) {
					// Nothing to collect
					Object convertedEmptyRelation = convertPrimitiveValue(Collections.emptyList(),
							boMember.getElementType(), boMember.getRealType(), boMember.getElementType());
					boMember.setValue(businessObject, convertedEmptyRelation);
					continue;
				}
				sb.setLength(0);
				String voSpecifiedName = sb.append(boMemberName).append("Specified").toString();
				ITypeInfoItem voSpecifiedMember = boNameToVoMember.get(voSpecifiedName);
				if (voSpecifiedMember != null
						&& !Boolean.TRUE.equals(voSpecifiedMember.getValue(valueObject))) {
					continue;
				}
				voValue = voMember.getValue(valueObject);
			}
			else {
				Object boValue = null;
				// Workaround bis das Problem (TODO) behoben ist, um zumindest eindeutige Relationen
				// fehlerfrei aufzuloesen.
				CompositIdentityClassKey key = new CompositIdentityClassKey(valueObject,
						boMember.getElementType());
				voValue = reverseRelationMap.get(key);
				if (voValue != null) {
					boValue = voToBoMap.get(voValue);
					boMember.setValue(businessObject, boValue);
					continue;
				}
				Object id = boMetaData.getIdMember().getValue(businessObject, false);
				if (id != null) {
					// TODO value ueber die Rueckreferenz finden
					// Bis dahin wird es nach dem Mapping beim Speichern knallen, weil der LazyValueHolder bei
					// neuen Entitaeten nicht aufgeloest werden kann.
					if (ValueHolderState.INIT != businessObject.get__State(relationIndex)) {
						businessObject.set__Uninitialized(relationIndex, null);
					}
				}
				else if (boMember.getRealType().equals(boMember.getElementType())) {
					// To-one relation
					boValue = null;
					boMember.setValue(businessObject, boValue);
				}
				else {
					// To-many relation
					boValue = ListUtil.createCollectionOfType(boMember.getRealType(), 0);
					boMember.setValue(businessObject, boValue);
				}
				continue;
			}
			if (voValue == null) {
				// Nothing to collect
				Object convertedEmptyRelation = convertPrimitiveValue(Collections.emptyList(),
						boMember.getElementType(), boMember.getRealType(), boMember.getElementType());
				boMember.setValue(businessObject, convertedEmptyRelation);
				continue;
			}
			if (config.holdsListType(voMember.getName())) {
				voValue = listTypeHelper.unpackListType(voValue);
			}
			List<Object> voList = ListUtil.anyToList(voValue);
			if (voList.isEmpty()) {
				// Nothing to collect
				Object convertedEmptyRelation = convertPrimitiveValue(Collections.emptyList(),
						boMember.getElementType(), boMember.getRealType(), boMember.getElementType());
				boMember.setValue(businessObject, convertedEmptyRelation);
				continue;
			}
			IEntityMetaData boMetaDataOfItem = entityMetaDataProvider
					.getMetaData(boMember.getElementType());
			Member boIdMemberOfItem = selectIdMember(boMetaDataOfItem);
			byte idIndex = boMetaDataOfItem.getIdIndexByMemberName(boIdMemberOfItem.getName());

			ArrayList<IObjRef> pendingRelations = new ArrayList<>();

			ValueObjectMemberType memberType = config.getValueObjectMemberType(voMemberName);
			boolean mapAsBasic = memberType == ValueObjectMemberType.BASIC;

			if (!mapAsBasic) {
				for (int a = 0, size = voList.size(); a < size; a++) {
					Object voItem = voList.get(a);

					IValueObjectConfig configOfItem = entityMetaDataProvider
							.getValueObjectConfig(voItem.getClass());

					if (configOfItem == null) {
						// This is a simple id which we can use
						IObjRef objRef = getObjRef(boMetaDataOfItem.getEntityType(), idIndex, voItem,
								alreadyCreatedObjRefsMap);
						referencedBOsSet.add(objRef);
						pendingRelations.add(objRef);
						continue;
					}
					// voItem is a real VO handle
					Object boItem = voToBoMap.get(voItem);
					Object idOfItem = getIdFromBusinessObject(boItem, boMetaDataOfItem);
					if (idOfItem == null) {
						throw new IllegalStateException(
								"All BOs must have at least a temporary id at this point. " + boItem);
					}
					IObjRef objRef = getObjRef(boMetaDataOfItem.getEntityType(), ObjRef.PRIMARY_KEY_INDEX,
							idOfItem, alreadyCreatedObjRefsMap);
					referencedBOsSet.add(objRef);
					pendingRelations.add(objRef);
				}
			}

			if (mapAsBasic) {
				Class<?> targetType = boMember.getElementType();
				ArrayList<Object> boList = new ArrayList<>();
				for (int a = 0, size = voList.size(); a < size; a++) {
					Object voItem = voList.get(a);
					Object boItem = conversionHelper.convertValueToType(targetType, voItem);
					boList.add(boItem);
				}
				Object relationValue = cacheHelper.convertResultListToExpectedType(boList,
						boMember.getRealType(), boMember.getElementType());
				boMember.setValue(businessObject, relationValue);
			}
			else if (pendingRelations.isEmpty()) {
				Object relationValue = cacheHelper
						.createInstanceOfTargetExpectedType(boMember.getRealType(), boMember.getElementType());
				boMember.setValue(businessObject, relationValue);
			}
			else {
				IObjRef[] objRefs = !pendingRelations.isEmpty() ? pendingRelations.toArray(IObjRef.class)
						: ObjRef.EMPTY_ARRAY;
				businessObject.set__Uninitialized(relationIndex, objRefs);
				cache.assignEntityToCache(businessObject);
				referencedBOsSet.addAll(objRefs);
				boToPendingRelationsList.add(new DirectValueHolderRef(businessObject, boMember));
			}
		}
	}

	protected IObjRef getObjRef(Class<?> entityType, byte idIndex, Object id,
			Map<IObjRef, IObjRef> alreadyCreatedObjRefMap) {
		ObjRef objRef = new ObjRef(entityType, idIndex, id, null);
		IObjRef usingObjRef = alreadyCreatedObjRefMap.get(objRef);
		if (usingObjRef == null) {
			alreadyCreatedObjRefMap.put(objRef, objRef);
			usingObjRef = objRef;
		}
		return usingObjRef;
	}

	protected Object createVOMemberValue(IObjRefContainer businessObject, int relationIndex,
			RelationMember boMember, IValueObjectConfig config, ITypeInfoItem voMember,
			Collection<Object> pendingValueHolders, Collection<Runnable> runnables) {
		Object voMemberValue = null;
		Class<?> voMemberType = voMember.getRealType();
		boolean holdsListType = config.holdsListType(voMember.getName());
		boolean singularValue = !Collection.class.isAssignableFrom(voMemberType) && !holdsListType;

		if (!singularValue && !List.class.isAssignableFrom(voMemberType) && !holdsListType) {
			throw new IllegalArgumentException(
					"Unsupportet collection type '" + voMemberType.getName() + "'");
		}
		if (ValueHolderState.INIT != businessObject.get__State(relationIndex)) {
			pendingValueHolders.add(new DirectValueHolderRef(businessObject, boMember));
			return NOT_YET_READY;
		}
		Object boValue = businessObject instanceof RootCacheValue
				? ((RootCacheValue) businessObject).getRelation(relationIndex)
				: boMember.getValue(businessObject, false);

		List<Object> referencedBOs = ListUtil.anyToList(boValue);
		List<Object> referencedVOs = null;
		IConversionHelper conversionHelper = this.conversionHelper;
		IEntityMetaDataProvider entityMetaDataProvider = this.entityMetaDataProvider;

		if (!referencedBOs.isEmpty()) {
			referencedVOs = new java.util.ArrayList<>(referencedBOs.size());

			Class<?> voMemberElementType = voMember.getElementType();
			IValueObjectConfig refConfig = entityMetaDataProvider
					.getValueObjectConfig(voMemberElementType);
			boolean mapAsBasic = config
					.getValueObjectMemberType(voMember.getName()) == ValueObjectMemberType.BASIC;
			final IEntityMetaData referencedBOMetaData = entityMetaDataProvider
					.getMetaData(boMember.getElementType());
			final Member refBOBuidMember = selectIdMember(referencedBOMetaData);
			final Member refBOVersionMember = referencedBOMetaData.getVersionMember();
			final byte refBOBuidIndex = referencedBOMetaData
					.getIdIndexByMemberName(refBOBuidMember.getName());
			Class<?> expectedVOType = config.getMemberType(voMember.getName());

			IObjRefProvider buidOriProvider = new MappingObjRefProvider(refBOBuidMember,
					refBOVersionMember, refBOBuidIndex);

			for (int i = 0; i < referencedBOs.size(); i++) {
				Object refBO = referencedBOs.get(i);
				if (mapAsBasic) {
					Object refVO = conversionHelper.convertValueToType(expectedVOType, refBO);
					referencedVOs.add(refVO);
					continue;
				}
				if (refConfig == null) {
					IObjRef refOri = oriHelper.getCreateObjRef(refBO, buidOriProvider);
					if (refOri == null || refOri.getIdNameIndex() != refBOBuidIndex) {
						throw new IllegalArgumentException(
								"ORI of referenced BO is null or does not contain BUID: " + refOri);
					}

					if (refOri.getId() != null) {
						referencedVOs.add(refOri.getId());
					}
					else {
						throw new IllegalStateException("Relation ID is null:" + refBO);
					}
				}
				else {
					referencedVOs.add(subMapToCachedValueObject(refBO, voMemberElementType,
							pendingValueHolders, runnables));
				}
			}
		}

		if (!singularValue) {
			if (holdsListType) {
				voMemberValue = listTypeHelper.packInListType(referencedVOs, voMemberType);
			}
			else {
				if (referencedVOs != null && voMemberType.isAssignableFrom(referencedVOs.getClass())) {
					voMemberValue = referencedVOs;
				}
				else if (voMemberType.isArray()) {
					if (referencedVOs == null) {
						referencedVOs = EmptyList.<Object>getInstance();
					}
					voMemberValue = ListUtil.anyToArray(referencedVOs, voMemberType.getComponentType());
				}
				else {
					if (referencedVOs == null) {
						referencedVOs = EmptyList.<Object>getInstance();
					}
					voMemberValue = ListUtil.createCollectionOfType(voMemberType, referencedVOs.size());
					ListUtil.fillList(voMemberValue, referencedVOs);
				}
			}
		}
		else if (referencedVOs != null) {
			voMemberValue = referencedVOs.get(0);
		}

		return voMemberValue;
	}

	protected IValueObjectConfig getValueObjectConfig(Class<?> valueObjectType) {
		IValueObjectConfig config = entityMetaDataProvider.getValueObjectConfig(valueObjectType);
		if (config == null) {
			throw new IllegalStateException(
					"No config found for value object type '" + valueObjectType.getName() + "'");
		}
		return config;
	}

	protected void resolveAllValueObjectsDirectly(Object valueObject, List<Object> allDirectVOs,
			IdentityHashSet<Object> alreadyScannedSet, Object parent) {
		if (valueObject == null || !alreadyScannedSet.add(valueObject)) {
			return;
		}
		if (valueObject instanceof List) {
			List<?> list = (List<?>) valueObject;
			for (int a = list.size(); a-- > 0;) {
				Object item = list.get(a);
				resolveAllValueObjectsDirectly(item, allDirectVOs, alreadyScannedSet, parent);
			}
			return;
		}
		else if (valueObject instanceof Collection) {
			Iterator<?> iter = ((Collection<?>) valueObject).iterator();
			while (iter.hasNext()) {
				Object item = iter.next();
				resolveAllValueObjectsDirectly(item, allDirectVOs, alreadyScannedSet, parent);
			}
			return;
		}

		// filling map for resolving relations without back-link
		// null for root or non-unique cases
		Class<?> parentBoType = null;
		if (parent != null) {
			IValueObjectConfig parentConfig = entityMetaDataProvider
					.getValueObjectConfig(parent.getClass());
			parentBoType = parentConfig.getEntityType();
		}
		CompositIdentityClassKey key = new CompositIdentityClassKey(valueObject, parentBoType);
		if (!reverseRelationMap.containsKey(key)) {
			reverseRelationMap.put(key, parent);
		}
		else {
			reverseRelationMap.put(key, null);
		}

		IValueObjectConfig config = entityMetaDataProvider.getValueObjectConfig(valueObject.getClass());
		if (config == null) {
			return;
		}
		allDirectVOs.add(valueObject);

		if (handleNoEntities(valueObject, config)) {
			return;
		}
		Map<String, ITypeInfoItem> boNameToVoMember = getTypeInfoMapForVo(config);
		IEntityMetaData metaData = entityMetaDataProvider.getMetaData(config.getEntityType());
		for (Member boMember : metaData.getRelationMembers()) {
			String boMemberName = boMember.getName();
			String voMemberName = config.getValueObjectMemberName(boMemberName);
			ValueObjectMemberType valueObjectMemberType = config.getValueObjectMemberType(voMemberName);
			ITypeInfoItem voMember = boNameToVoMember.get(boMemberName);
			if (voMember == null || config.isIgnoredMember(voMemberName)
					|| valueObjectMemberType == ValueObjectMemberType.BASIC) {
				// ValueObjectMemberType.BASIC members of entityType VO are special case mappings via
				// conversionHelper
				continue;
			}
			Object item = voMember.getValue(valueObject, false);
			if (item == null) {
				// Nothing to resolve
				continue;
			}
			if (config.holdsListType(voMember.getName())) {
				item = listTypeHelper.unpackListType(item);
			}

			resolveAllValueObjectsDirectly(item, allDirectVOs, alreadyScannedSet, valueObject);
		}
	}

	protected boolean handleNoEntities(Object valueObject, IValueObjectConfig config) {
		Class<?> entityType = config.getEntityType();
		if (IFilterDescriptor.class.isAssignableFrom(entityType)) {
			return true;
		}
		else if (ISortDescriptor.class.isAssignableFrom(entityType)) {
			return true;
		}
		return false;
	}

	protected IEntityMetaData getMetaData(Object bo) {
		if (bo instanceof IEntityMetaDataHolder) {
			return ((IEntityMetaDataHolder) bo).get__EntityMetaData();
		}
		return entityMetaDataProvider.getMetaData(bo.getClass());
	}

	protected Object subMapToCachedValueObject(Object subBusinessObject, Class<?> valueObjectType,
			Collection<Object> pendingValueHolders, Collection<Runnable> runnables) {
		IMap<Class<?>, Object> boVOsMap = boToSpecifiedVOMap.get(subBusinessObject);

		if (boVOsMap == null) {
			boVOsMap = new IdentityHashMap<>();
			boToSpecifiedVOMap.put(subBusinessObject, boVOsMap);
		}
		IEntityMetaData metaData = getMetaData(subBusinessObject);
		Object subValueObject = boVOsMap.get(valueObjectType);
		if (subValueObject == null) {
			try {
				subValueObject = valueObjectType.newInstance();
			}
			catch (Exception e) {
				throw RuntimeExceptionUtil.mask(e);
			}
			boVOsMap.put(valueObjectType, subValueObject);

			Object id = getIdFromBusinessObject(subBusinessObject, metaData);
			if (id == null) {
				setTempIdToBusinessObject(subBusinessObject, metaData);
				vosToRemoveTempIdFrom.add(subValueObject);
			}
			resolveProperties(subBusinessObject, subValueObject, pendingValueHolders, runnables);
		}

		IDedicatedMapper dedicatedMapper = mapperExtensionRegistry
				.getDedicatedMapper(metaData.getEntityType());
		if (dedicatedMapper != null) {
			dedicatedMapper.applySpecialMapping(subBusinessObject, subValueObject,
					CopyDirection.BO_TO_VO);
		}
		return subValueObject;
	}

	@Override
	public Object getIdFromValueObject(Object valueObject) {
		IEntityMetaDataProvider entityMetaDataProvider = this.entityMetaDataProvider;
		IValueObjectConfig config = entityMetaDataProvider.getValueObjectConfig(valueObject.getClass());
		IEntityMetaData boMetaData = entityMetaDataProvider.getMetaData(config.getEntityType());
		Map<String, ITypeInfoItem> boNameToVoMember = getTypeInfoMapForVo(config);
		return getIdFromValueObject(valueObject, boMetaData, boNameToVoMember, config);
	}

	@Override
	public Object getVersionFromValueObject(Object valueObject) {
		IEntityMetaDataProvider entityMetaDataProvider = this.entityMetaDataProvider;
		IValueObjectConfig config = entityMetaDataProvider.getValueObjectConfig(valueObject.getClass());
		IEntityMetaData boMetaData = entityMetaDataProvider.getMetaData(config.getEntityType());
		Map<String, ITypeInfoItem> boNameToVoMember = getTypeInfoMapForVo(config);
		String boVersionMemberName = boMetaData.getVersionMember().getName();
		ITypeInfoItem voVersionMember = boNameToVoMember.get(boVersionMemberName);
		return voVersionMember.getValue(valueObject, false);
	}

	protected Object getIdFromValueObject(Object valueObject, IEntityMetaData boMetaData,
			Map<String, ITypeInfoItem> boNameToVoMember, IValueObjectConfig config) {
		ITypeInfoItem voIdMember = getVoIdMember(config, boMetaData, boNameToVoMember);
		return voIdMember.getValue(valueObject, false);
	}

	protected void setTempIdToValueObject(Object valueObject, IEntityMetaData boMetaData,
			Map<String, ITypeInfoItem> boNameToVoMember, IValueObjectConfig config) {
		ITypeInfoItem voIdMember = getVoIdMember(config, boMetaData, boNameToVoMember);
		Object tempId = getNextTempIdAs(voIdMember.getElementType());
		voIdMember.setValue(valueObject, tempId);
		vosToRemoveTempIdFrom.add(valueObject);
	}

	protected void removeTempIdFromValueObject(Object valueObject, IEntityMetaData boMetaData,
			Map<String, ITypeInfoItem> boNameToVoMember, IValueObjectConfig config) {
		ITypeInfoItem voIdMember = getVoIdMember(config, boMetaData, boNameToVoMember);
		Object nullEquivalentValue = NullEquivalentValueUtil
				.getNullEquivalentValue(voIdMember.getElementType());
		voIdMember.setValue(valueObject, nullEquivalentValue);
	}

	protected Object getIdFromBusinessObject(Object businessObject, IEntityMetaData metaData) {
		if (businessObject instanceof AbstractCacheValue) {
			return ((AbstractCacheValue) businessObject).getId();
		}
		return metaData.getIdMember().getValue(businessObject, false);
	}

	protected Object getVersionFromBusinessObject(Object businessObject, IEntityMetaData metaData) {
		if (businessObject instanceof AbstractCacheValue) {
			return ((AbstractCacheValue) businessObject).getVersion();
		}
		return metaData.getVersionMember().getValue(businessObject, false);
	}

	protected void setTempIdToBusinessObject(Object businessObject, IEntityMetaData metaData) {
		Member idMember = metaData.getIdMember();
		Object tempId = getNextTempIdAs(idMember.getElementType());
		idMember.setValue(businessObject, tempId);
		bosToRemoveTempIdFrom.add(businessObject);
	}

	protected void removeTempIdFromBusinessObject(Object businessObject, IEntityMetaData metaData,
			ObjRef tempObjRef, IWritableCache cache) {
		Member idMember = metaData.getIdMember();
		Object id = idMember.getValue(businessObject);
		tempObjRef.setRealType(metaData.getEntityType());
		tempObjRef.setIdNameIndex(ObjRef.PRIMARY_KEY_INDEX);
		tempObjRef.setId(id);
		cache.remove(tempObjRef);
		idMember.setValue(businessObject, null);
	}

	protected ITypeInfoItem getVoIdMember(IValueObjectConfig config, IEntityMetaData boMetaData,
			Map<String, ITypeInfoItem> boNameToVoMember) {
		String boIdMemberName = boMetaData.getIdMember().getName();
		return boNameToVoMember.get(boIdMemberName);
	}

	protected void clearObjectsWithTempIds(IWritableCache cache) {
		ISet<Object> bosToRemoveTempIdFrom = this.bosToRemoveTempIdFrom;
		IEntityMetaDataProvider entityMetaDataProvider = this.entityMetaDataProvider;
		ISet<Object> vosToRemoveTempIdFrom = this.vosToRemoveTempIdFrom;
		if (!vosToRemoveTempIdFrom.isEmpty()) {
			for (Object vo : vosToRemoveTempIdFrom) {
				IValueObjectConfig config = entityMetaDataProvider.getValueObjectConfig(vo.getClass());
				Map<String, ITypeInfoItem> boNameToVoMember = getTypeInfoMapForVo(config);
				IEntityMetaData boMetaData = entityMetaDataProvider.getMetaData(config.getEntityType());
				removeTempIdFromValueObject(vo, boMetaData, boNameToVoMember, config);
			}
			vosToRemoveTempIdFrom.clear();
		}
		if (!bosToRemoveTempIdFrom.isEmpty()) {
			ObjRef objRef = new ObjRef();
			for (Object bo : bosToRemoveTempIdFrom) {
				IEntityMetaData metaData = getMetaData(bo);
				removeTempIdFromBusinessObject(bo, metaData, objRef, cache);
			}
			bosToRemoveTempIdFrom.clear();
		}
	}

	protected <T> T getNextTempIdAs(Class<T> elementType) {
		if (nextTempId == Long.MIN_VALUE) {
			nextTempId = -1;
		}
		return conversionHelper.convertValueToType(elementType, nextTempId--);
	}

	protected Member selectIdMember(IEntityMetaData referencedBOMetaData) {
		if (referencedBOMetaData == null) {
			throw new IllegalArgumentException(
					"Business object contains reference to object without metadata");
		}
		Member idMember = referencedBOMetaData.getIdMember();
		if (referencedBOMetaData.getAlternateIdCount() == 1) {
			idMember = referencedBOMetaData.getAlternateIdMembers()[0];
		}
		else if (referencedBOMetaData.getAlternateIdCount() > 1) {
			// AgriLog specific solution for AlternateIdCount > 1
			for (Member alternateIdMember : referencedBOMetaData.getAlternateIdMembers()) {
				if ("Buid".equals(alternateIdMember.getName())) {
					idMember = alternateIdMember;
					break;
				}
			}
		}
		return idMember;
	}

	protected Map<String, ITypeInfoItem> getTypeInfoMapForVo(IValueObjectConfig config) {
		Map<String, ITypeInfoItem> typeInfoMap = typeToTypeInfoMap.get(config.getValueType());
		if (typeInfoMap == null) {
			IThreadLocalObjectCollector tlObjectCollector = objectCollector.getCurrent();
			StringBuilder sb = tlObjectCollector.create(StringBuilder.class);
			try {
				typeInfoMap = new HashMap<>();
				IEntityMetaData boMetaData = entityMetaDataProvider.getMetaData(config.getEntityType());
				addTypeInfoMapping(typeInfoMap, config, boMetaData.getIdMember().getName(), sb);
				if (boMetaData.getVersionMember() != null) {
					addTypeInfoMapping(typeInfoMap, config, boMetaData.getVersionMember().getName(), sb);
				}
				for (Member primitiveMember : boMetaData.getPrimitiveMembers()) {
					addTypeInfoMapping(typeInfoMap, config, primitiveMember.getName(), sb);
				}
				for (RelationMember relationMember : boMetaData.getRelationMembers()) {
					addTypeInfoMapping(typeInfoMap, config, relationMember.getName(), sb);
				}
				typeToTypeInfoMap.put(config.getValueType(), typeInfoMap);
			}
			finally {
				tlObjectCollector.dispose(sb);
			}
		}
		return typeInfoMap;
	}

	protected void addTypeInfoMapping(Map<String, ITypeInfoItem> typeInfoMap,
			IValueObjectConfig config, String boMemberName, StringBuilder sb) {
		String voMemberName = config.getValueObjectMemberName(boMemberName);
		ITypeInfoItem voMember = typeInfoProvider.getHierarchicMember(config.getValueType(),
				voMemberName);
		if (voMember == null) {
			return;
		}
		Class<?> elementType = config.getMemberType(voMemberName);
		if (elementType != null) {
			TypeInfoItem.setEntityType(elementType, voMember, null);
		}
		typeInfoMap.put(boMemberName, voMember);
		if (sb != null) {
			sb.setLength(0);
			String voSpecifiedName = sb.append(voMemberName).append("Specified").toString();
			ITypeInfoItem voSpecifiedMember = typeInfoProvider.getHierarchicMember(config.getValueType(),
					voSpecifiedName);
			if (voSpecifiedMember != null) {
				sb.setLength(0);
				String boSpecifiedName = sb.append(boMemberName).append("Specified").toString();
				typeInfoMap.put(boSpecifiedName, voSpecifiedMember);
			}
		}
	}

	protected Object[] copyPrimitives(Object businessObject, Object valueObject,
			IValueObjectConfig config, CopyDirection direction, IEntityMetaData boMetaData,
			Map<String, ITypeInfoItem> boNameToVoMember) {
		IThreadLocalObjectCollector objectCollector = this.objectCollector;
		PrimitiveMember[] primitiveMembers = allPrimitiveMembers(boMetaData);
		Object[] primitives = new Object[boMetaData.getPrimitiveMembers().length];
		StringBuilder sb = objectCollector.create(StringBuilder.class);
		try {
			for (int i = primitiveMembers.length; i-- > 0;) {
				PrimitiveMember boMember = primitiveMembers[i];
				String boMemberName = boMember.getName();
				String voMemberName = config.getValueObjectMemberName(boMemberName);
				sb.setLength(0);
				String boSpecifiedMemberName = sb.append(boMemberName).append("Specified").toString();
				ITypeInfoItem voMember = boNameToVoMember.get(boMemberName);
				ITypeInfoItem voSpecifiedMember = boNameToVoMember.get(boSpecifiedMemberName);
				boolean isSpecified = true;
				if (config.isIgnoredMember(voMemberName) || voMember == null) {
					continue;
				}
				switch (direction) {
					case VO_TO_BO: {
						// Copy primitive from value object to business object
						// TODO: Copy by value instead of copy by reference
						if (voSpecifiedMember != null) {
							isSpecified = (Boolean) voSpecifiedMember.getValue(valueObject);
						}
						if (!isSpecified) {
							continue;
						}
						Object value = voMember.getValue(valueObject, false);
						if (value != null && config.holdsListType(voMemberName)) {
							value = listTypeHelper.unpackListType(value);
						}
						value = convertPrimitiveValue(value, voMember.getElementType(), boMember.getRealType(),
								boMember.getElementType());
						// Do not 'kill' technical members except 'version' (for optimistic locking)
						if (boMember.isTechnicalMember() && !boMember.equals(boMetaData.getVersionMember())
								&& (value == null || value.equals(boMember.getNullEquivalentValue()))) {
							continue;
						}
						if (value == null) {
							value = boMember.getNullEquivalentValue();
						}
						boMember.setValue(businessObject, value);
						if (i < primitives.length) {
							primitives[i] = value;
						}
						break;
					}
					case BO_TO_VO: {
						// Copy primitive from business object to value object
						// TODO: Copy by value instead of copy by reference
						Object value = getPrimitiveFromBusinessObject(businessObject, boMember, i, boMetaData);
						isSpecified = value != null;
						if (voSpecifiedMember != null) {
							voSpecifiedMember.setValue(valueObject, isSpecified);
						}
						if (!isSpecified) {
							continue;
						}
						if (config.holdsListType(voMemberName)) {
							if (value instanceof Collection) {
								value = listTypeHelper.packInListType((Collection<?>) value,
										voMember.getRealType());
							}
						}
						value = convertPrimitiveValue(value, boMember.getElementType(), voMember.getRealType(),
								voMember.getElementType());
						if (voMember.isTechnicalMember()
								&& (value == null || value.equals(voMember.getNullEquivalentValue()))) {
							continue;
						}
						if (value == null) {
							value = boMember.getNullEquivalentValue();
						}
						setPropertyValue(valueObject, voMember, value);
						break;
					}
					default:
						throw RuntimeExceptionUtil.createEnumNotSupportedException(direction);
				}
			}
		}
		finally {
			objectCollector.dispose(sb);
		}
		return primitives;
	}

	private Object getPrimitiveFromBusinessObject(Object businessObject, PrimitiveMember boMember,
			int allPrimitiveIndex, IEntityMetaData metaData) {
		if (businessObject instanceof AbstractCacheValue) {
			PrimitiveMember[] primitiveMembers = metaData.getPrimitiveMembers();
			if (allPrimitiveIndex == primitiveMembers.length) {
				return ((AbstractCacheValue) businessObject).getId();
			}
			else if (allPrimitiveIndex == primitiveMembers.length + 1) {
				return ((AbstractCacheValue) businessObject).getVersion();
			}
			return ((AbstractCacheValue) businessObject).getPrimitive(allPrimitiveIndex);
		}
		return boMember.getValue(businessObject, true);
	}

	protected PrimitiveMember[] allPrimitiveMembers(IEntityMetaData businessObjectMetaData) {
		PrimitiveMember[] primitiveValueMembers = businessObjectMetaData.getPrimitiveMembers();
		int technicalMemberCount = 1;
		if (businessObjectMetaData.getVersionMember() != null) {
			technicalMemberCount++;
		}
		PrimitiveMember[] primitiveMembers = new PrimitiveMember[primitiveValueMembers.length
				+ technicalMemberCount];
		System.arraycopy(primitiveValueMembers, 0, primitiveMembers, 0, primitiveValueMembers.length);
		int insertIndex = primitiveMembers.length - technicalMemberCount;
		primitiveMembers[insertIndex++] = businessObjectMetaData.getIdMember();
		if (businessObjectMetaData.getVersionMember() != null) {
			primitiveMembers[insertIndex++] = businessObjectMetaData.getVersionMember();
		}
		return primitiveMembers;
	}

	protected Object convertPrimitiveValue(Object value, Class<?> sourceElementType,
			Class<?> targetRealType, Class<?> targetElementType) {
		if (value == null) {
			return null;
		}
		else if (value.getClass().isArray() && !String.class.equals(targetRealType)) // do not handle
																																									// byte[]
		// or char[] to
		// String here
		{
			return convertPrimitiveValue(ListUtil.anyToList(value), sourceElementType, targetRealType,
					targetElementType);
		}
		else if (!(value instanceof Collection)) {
			return conversionHelper.convertValueToType(targetRealType, value);
		}
		IConversionHelper conversionHelper = this.conversionHelper;

		Collection<?> coll = (Collection<?>) value;

		if (targetRealType.isArray()) {
			Object array = Array.newInstance(targetRealType.getComponentType(), coll.size());
			int index = 0;
			for (Object item : coll) {
				Object convertedItem = conversionHelper.convertValueToType(targetElementType, item);
				Array.set(array, index++, convertedItem);
			}
			return array;
		}
		else if (Set.class.isAssignableFrom(targetRealType)) {
			int size = coll.size();
			java.util.HashSet<Object> set = new java.util.HashSet<>((int) (size / 0.75f) + 1, 0.75f);
			for (Object item : coll) {
				Object convertedItem = conversionHelper.convertValueToType(targetElementType, item);
				set.add(convertedItem);
			}
			return set;
		}
		else if (Collection.class.isAssignableFrom(targetRealType)) {
			java.util.ArrayList<Object> list = new java.util.ArrayList<>(coll.size());
			for (Object item : coll) {
				Object convertedItem = conversionHelper.convertValueToType(targetElementType, item);
				list.add(convertedItem);
			}
			return list;
		}
		else if (coll.isEmpty()) {
			return null;
		}
		else if (coll.size() == 1) {
			if (coll instanceof List) {
				return ((List<?>) coll).get(0);
			}
			return coll.iterator().next();
		}
		throw new IllegalArgumentException("Cannot map '" + value.getClass() + "' of '"
				+ sourceElementType + "' to '" + targetRealType + "' of '" + targetElementType + "'");
	}

	@Override
	public Object getMappedBusinessObject(IObjRef objRef) {
		return cache.getObject(objRef, CacheDirective.failEarly());
	}

	@Override
	public IList<Object> getAllActiveBusinessObjects() {
		return voToBoMap.values();
	}
}
