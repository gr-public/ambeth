package com.koch.ambeth.cache;

/*-
 * #%L
 * jambeth-cache
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.koch.ambeth.cache.collections.CacheHashMap;
import com.koch.ambeth.cache.collections.CacheMapEntry;
import com.koch.ambeth.cache.config.CacheConfigurationConstants;
import com.koch.ambeth.cache.proxy.IValueHolderContainer;
import com.koch.ambeth.cache.util.ICachePathHelper;
import com.koch.ambeth.event.IEventQueue;
import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.ioc.config.Property;
import com.koch.ambeth.ioc.garbageproxy.IGarbageProxyFactory;
import com.koch.ambeth.log.ILogger;
import com.koch.ambeth.log.LogInstance;
import com.koch.ambeth.merge.IEntityFactory;
import com.koch.ambeth.merge.cache.CacheDirective;
import com.koch.ambeth.merge.cache.HandleContentDelegate;
import com.koch.ambeth.merge.cache.ICacheModification;
import com.koch.ambeth.merge.cache.IDisposableCache;
import com.koch.ambeth.merge.cache.IWritableCache;
import com.koch.ambeth.merge.cache.ValueHolderState;
import com.koch.ambeth.merge.config.MergeConfigurationConstants;
import com.koch.ambeth.merge.model.IDirectObjRef;
import com.koch.ambeth.merge.proxy.IDefaultCollection;
import com.koch.ambeth.merge.proxy.IObjRefContainer;
import com.koch.ambeth.merge.security.ISecurityActivation;
import com.koch.ambeth.merge.transfer.ObjRef;
import com.koch.ambeth.merge.util.ICacheHelper;
import com.koch.ambeth.service.cache.model.ILoadContainer;
import com.koch.ambeth.service.cache.model.IObjRelation;
import com.koch.ambeth.service.cache.model.IObjRelationResult;
import com.koch.ambeth.service.merge.model.IEntityMetaData;
import com.koch.ambeth.service.merge.model.IObjRef;
import com.koch.ambeth.service.metadata.Member;
import com.koch.ambeth.service.metadata.RelationMember;
import com.koch.ambeth.util.IDisposable;
import com.koch.ambeth.util.IParamHolder;
import com.koch.ambeth.util.ListUtil;
import com.koch.ambeth.util.Lock;
import com.koch.ambeth.util.ParamHolder;
import com.koch.ambeth.util.annotation.CascadeLoadMode;
import com.koch.ambeth.util.collections.ArrayList;
import com.koch.ambeth.util.collections.EmptyList;
import com.koch.ambeth.util.collections.HashSet;
import com.koch.ambeth.util.collections.IList;
import com.koch.ambeth.util.exception.RuntimeExceptionUtil;
import com.koch.ambeth.util.model.IDataObject;
import com.koch.ambeth.util.state.IStateRollback;
import com.koch.ambeth.util.state.NoOpStateRollback;

public class ChildCache extends AbstractCache<Object>
		implements ICacheIntern, IWritableCache, IDisposableCache {
	private static final Class<?>[] ADDITIONAL_GC_PROXY_TYPES = new Class[] {IWritableCache.class};

	public static final String P_EVENT_QUEUE = "EventQueue";

	public static final String P_NAME = "Name";

	public static final String P_PARENT = "Parent";

	public static final String P_PRIVILEGED = "Privileged";

	@LogInstance
	private ILogger log;

	protected CacheHashMap keyToAlternateIdsMap;

	@Autowired
	protected ICacheModification cacheModification;

	@Autowired
	protected ICachePathHelper cachePathHelper;

	@Autowired
	protected IEntityFactory entityFactory;

	@Autowired(optional = true)
	protected IEventQueue eventQueue;

	@Autowired
	protected IFirstLevelCacheExtendable firstLevelCacheExtendable;

	@Autowired
	protected IGarbageProxyFactory garbageProxyFactory;

	@Autowired
	protected ICacheIntern parent;

	@Autowired(optional = true)
	protected ISecurityActivation securityActivation;

	@Property(mandatory = false)
	protected String name;

	protected int cacheId;

	protected ICacheIntern gcProxy;

	@Property(name = MergeConfigurationConstants.SecurityActive, defaultValue = "false")
	protected boolean securityActive;

	@Property(name = CacheConfigurationConstants.ValueholderOnEmptyToOne, defaultValue = "false")
	protected boolean valueholderOnEmptyToOne;

	@Property(name = CacheConfigurationConstants.OverwriteToManyRelationsInChildCache,
			defaultValue = "true")
	protected boolean overwriteToManyRelations;

	@Property
	protected boolean privileged;

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		keyToAlternateIdsMap = new CacheHashMap(cacheMapEntryTypeProvider);

		gcProxy = garbageProxyFactory.createGarbageProxy(this, (IDisposable) null, ICacheIntern.class,
				ADDITIONAL_GC_PROXY_TYPES);
	}

	@Property(name = CacheConfigurationConstants.FirstLevelCacheWeakActive, defaultValue = "true")
	@Override
	public void setWeakEntries(boolean weakEntries) {
		super.setWeakEntries(weakEntries);
	}

	@Override
	public boolean isPrivileged() {
		return privileged;
	}

	@Override
	public void setCacheId(int cacheId) {
		if (this.cacheId != 0 && cacheId != 0) {
			throw new UnsupportedOperationException();
		}
		this.cacheId = cacheId;
	}

	@Override
	public void dispose() {
		if (cacheId != 0) {
			firstLevelCacheExtendable.unregisterFirstLevelCache(this, null, false, name);
		}
		if (gcProxy != null) {
			((IDisposable) gcProxy).dispose(); // cuts the reference of all entities to this cache
																					// instance
			gcProxy = null;
		}
		cacheModification = null;
		cachePathHelper = null;
		entityFactory = null;
		firstLevelCacheExtendable = null;
		garbageProxyFactory = null;
		log = null;
		parent = null;
		keyToAlternateIdsMap = null;
		securityActivation = null;
		super.dispose();
	}

	public ICacheIntern getParent() {
		return parent;
	}

	@Override
	public int getCacheId() {
		return cacheId;
	}

	@Override
	protected void putIntern(ILoadContainer loadContainer) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void cacheValueHasBeenAdded(byte idIndex, Object id, IEntityMetaData metaData,
			Object[] primitives, IObjRef[][] relations, Object cacheValueR) {
		super.cacheValueHasBeenAdded(idIndex, id, metaData, primitives, relations, cacheValueR);

		Class<?> entityType = metaData.getEntityType();
		CacheKey[] oldAlternateCacheKeys = (CacheKey[]) keyToAlternateIdsMap.get(entityType, idIndex,
				id);
		if (oldAlternateCacheKeys != null) {
			for (int a = oldAlternateCacheKeys.length; a-- > 0;) {
				CacheKey alternateCacheKey = oldAlternateCacheKeys[a];
				if (alternateCacheKey != null) {
					removeKeyFromCache(alternateCacheKey);
				}
			}
		}
		CacheKey[] newAlternateCacheKeys = oldAlternateCacheKeys;
		if (newAlternateCacheKeys == null) {
			// Allocate new array to hold alternate ids
			newAlternateCacheKeys = extractAlternateCacheKeys(metaData, primitives);
			if (newAlternateCacheKeys.length > 0) {
				keyToAlternateIdsMap.put(entityType, idIndex, id, newAlternateCacheKeys);
			}
		}
		else {
			// reuse existing array for new alternate id-values
			extractAlternateCacheKeys(metaData, primitives, newAlternateCacheKeys);
		}
		putAlternateCacheKeysToCache(metaData, newAlternateCacheKeys, cacheValueR);
	}

	@Override
	protected void cacheValueHasBeenRemoved(IEntityMetaData metaData, byte idIndex, Object id,
			Object cacheValue) {
		((IValueHolderContainer) cacheValue).set__TargetCache(null);
		super.cacheValueHasBeenRemoved(metaData, idIndex, id, cacheValue);
	}

	@Override
	public Object createCacheValueInstance(IEntityMetaData metaData, Object obj) {
		if (obj != null) {
			return obj;
		}
		return entityFactory.createEntityNoEmptyInit(metaData);
	}

	@Override
	protected Object getIdOfCacheValue(IEntityMetaData metaData, Object cacheValue) {
		return metaData.getIdMember().getValue(cacheValue, false);
	}

	@Override
	protected void setIdOfCacheValue(IEntityMetaData metaData, Object cacheValue, Object id) {
		metaData.getIdMember().setValue(cacheValue, id);
	}

	@Override
	protected Object getVersionOfCacheValue(IEntityMetaData metaData, Object cacheValue) {
		Member versionMember = metaData.getVersionMember();
		if (versionMember == null) {
			return null;
		}
		return versionMember.getValue(cacheValue, false);
	}

	@Override
	protected void setVersionOfCacheValue(IEntityMetaData metaData, Object cacheValue,
			Object version) {
		Member versionMember = metaData.getVersionMember();
		if (versionMember == null) {
			return;
		}
		versionMember.setValue(cacheValue, version);
	}

	@Override
	public Object getObject(IObjRef oriToGet, ICacheIntern targetCache,
			Set<CacheDirective> cacheDirective) {
		checkNotDisposed();
		if (oriToGet == null) {
			return null;
		}
		ArrayList<IObjRef> orisToGet = new ArrayList<>(1);
		orisToGet.add(oriToGet);
		List<Object> objects = getObjects(orisToGet, targetCache, cacheDirective);
		if (objects.isEmpty()) {
			return null;
		}
		return objects.get(0);
	}

	@Override
	public IList<Object> getObjects(List<IObjRef> orisToGet, Set<CacheDirective> cacheDirective) {
		return getObjects(orisToGet, this, cacheDirective);
	}

	@Override
	public IList<Object> getObjects(List<IObjRef> orisToGet, ICacheIntern targetCache,
			Set<CacheDirective> cacheDirective) {
		checkNotDisposed();
		if (orisToGet == null || orisToGet.isEmpty()) {
			return EmptyList.getInstance();
		}
		if (cacheDirective == null) {
			cacheDirective = Collections.<CacheDirective>emptySet();
		}
		IEventQueue eventQueue = this.eventQueue;
		if (eventQueue != null) {
			eventQueue.pause(this);
		}
		try {
			ICacheModification cacheModification = this.cacheModification;
			boolean oldCacheModificationValue = cacheModification.isActive();
			boolean acquireSuccess = acquireHardRefTLIfNotAlready(orisToGet.size());
			cacheModification.setActive(true);
			try {
				if (cacheDirective.contains(CacheDirective.LoadContainerResult)
						|| cacheDirective.contains(CacheDirective.CacheValueResult)) {
					return parent.getObjects(orisToGet, this, cacheDirective);
				}
				ParamHolder<Boolean> doAnotherRetry = new ParamHolder<>();
				while (true) {
					doAnotherRetry.setValue(Boolean.FALSE);
					IList<Object> result = getObjectsRetry(orisToGet, cacheDirective, doAnotherRetry);
					if (!Boolean.TRUE.equals(doAnotherRetry.getValue())) {
						return result;
					}
				}
			}
			finally {
				cacheModification.setActive(oldCacheModificationValue);
				clearHardRefs(acquireSuccess);
			}
		}
		finally {
			if (eventQueue != null) {
				eventQueue.resume(this);
			}
		}
	}

	protected IList<Object> getObjectsRetry(List<IObjRef> orisToGet,
			Set<CacheDirective> cacheDirective, IParamHolder<Boolean> doAnotherRetry) {
		Lock readLock = getReadLock();
		if (cacheDirective.contains(CacheDirective.FailEarly)) {
			readLock.lock();
			try {
				return createResult(orisToGet, cacheDirective, true);
			}
			finally {
				readLock.unlock();
			}
		}
		ArrayList<IObjRef> orisToLoad = new ArrayList<>();
		int cacheVersionBeforeLongTimeAction = waitForConcurrentReadFinish(orisToGet, orisToLoad);
		if (orisToLoad.isEmpty()) {
			// Everything found in the cache. We STILL hold the readlock so we can immediately create the
			// result
			// We already even checked the version. So we do not bother version anymore here
			try {
				return createResult(orisToGet, cacheDirective, false);
			}
			finally {
				readLock.unlock();
			}
		}
		Set<CacheDirective> parentCacheDirective = CacheDirective.none();
		if (cacheDirective.contains(CacheDirective.FailInCacheHierarchy)) {
			parentCacheDirective = CacheDirective.failEarly();
		}
		parent.getObjects(orisToLoad, this, parentCacheDirective);
		// Objects do not have to be put, because their were already
		// added by the parent to this cache
		readLock.lock();
		try {
			int cacheVersionAfterLongTimeAction = changeVersion;
			if (cacheVersionAfterLongTimeAction != cacheVersionBeforeLongTimeAction) {
				// Another thread did some changes (possibly DataChange-Remove actions)
				// We have to ensure that our result-scope is still valid
				// We return null to allow a further full retry of getObjects()
				doAnotherRetry.setValue(Boolean.TRUE);
				return null;
			}
			return createResult(orisToGet, cacheDirective, false);
		}
		finally {
			readLock.unlock();
		}
	}

	protected int waitForConcurrentReadFinish(List<IObjRef> orisToGet, List<IObjRef> orisToLoad) {
		Lock readLock = getReadLock();
		boolean releaseReadLock = true;
		HashSet<IObjRef> objRefsAlreadyQueried = null;
		readLock.lock();
		try {
			for (int a = 0, size = orisToGet.size(); a < size; a++) {
				IObjRef oriToGet = orisToGet.get(a);
				if (oriToGet == null || oriToGet instanceof IDirectObjRef
						&& ((IDirectObjRef) oriToGet).getDirect() != null) {
					continue;
				}
				Object cacheValue = existsValue(oriToGet);
				if (cacheValue != null) {
					// Cache hit, but not relevant at this step, so we continue
					continue;
				}
				if (objRefsAlreadyQueried == null) {
					objRefsAlreadyQueried = HashSet.create(size - a);
				}
				if (!objRefsAlreadyQueried.add(oriToGet)) {
					// Object has been already queried from parent
					// It makes no sense to query it multiple times
					continue;
				}
				orisToLoad.add(oriToGet);
			}
			if (orisToLoad.isEmpty()) {
				releaseReadLock = false;
			}
			return changeVersion;
		}
		finally {
			if (releaseReadLock) {
				readLock.unlock();
			}
		}
	}

	protected IList<Object> createResult(List<IObjRef> orisToGet, Set<CacheDirective> cacheDirective,
			boolean checkVersion) {
		ArrayList<Object> result = new ArrayList<>(orisToGet.size());

		boolean returnMisses = cacheDirective.contains(CacheDirective.ReturnMisses);

		for (int a = 0, size = orisToGet.size(); a < size; a++) {
			IObjRef oriToGet = orisToGet.get(a);
			if (oriToGet == null) {
				if (returnMisses) {
					result.add(null);
				}
				continue;
			}
			if (oriToGet instanceof IDirectObjRef) {
				IDirectObjRef dori = (IDirectObjRef) oriToGet;
				Object entity = dori.getDirect();
				if (entity != null) {
					result.add(entity);
					continue;
				}
			}
			IEntityMetaData metaData = entityMetaDataProvider.getMetaData(oriToGet.getRealType());
			Object cacheValue = getCacheValue(metaData, oriToGet, checkVersion);
			if (cacheValue != null || returnMisses) {
				result.add(cacheValue);
			}
		}
		return result;
	}

	@Override
	public IList<IObjRelationResult> getObjRelations(List<IObjRelation> objRels,
			Set<CacheDirective> cacheDirective) {
		return getObjRelations(objRels, this, cacheDirective);
	}

	@Override
	public IList<IObjRelationResult> getObjRelations(List<IObjRelation> objRels,
			ICacheIntern targetCache, Set<CacheDirective> cacheDirective) {
		checkNotDisposed();
		IEventQueue eventQueue = this.eventQueue;
		if (eventQueue != null) {
			eventQueue.pause(this);
		}
		try {
			ICacheModification cacheModification = this.cacheModification;
			boolean oldCacheModificationValue = cacheModification.isActive();
			boolean acquireSuccess = acquireHardRefTLIfNotAlready(objRels.size());
			cacheModification.setActive(true);
			try {
				IStateRollback rollback = NoOpStateRollback.instance;
				if (securityActive && ((targetCache == null && isPrivileged())
						|| (targetCache != null && targetCache.isPrivileged())//
								&& securityActivation != null && securityActivation.isFilterActivated())) {
					rollback = securityActivation.pushWithoutFiltering(IStateRollback.EMPTY_ROLLBACKS);
				}
				try {
					return parent.getObjRelations(objRels, targetCache, cacheDirective);
				}
				finally {
					rollback.rollback();
				}
			}
			catch (Exception e) {
				throw RuntimeExceptionUtil.mask(e);
			}
			finally {
				cacheModification.setActive(oldCacheModificationValue);
				clearHardRefs(acquireSuccess);
			}
		}
		finally {
			if (eventQueue != null) {
				eventQueue.resume(this);
			}
		}
	}

	@Override
	public void addDirect(IEntityMetaData metaData, Object id, Object version,
			Object primitiveFilledObject, Object parentCacheValueOrArray, IObjRef[][] relations) {
		if (id == null) {
			throw new IllegalArgumentException("Key must be valid: " + primitiveFilledObject);
		}
		Class<?> entityType = metaData.getEntityType();
		byte idIndex = ObjRef.PRIMARY_KEY_INDEX;
		CacheKey[] oldAlternateCacheKeys = null;
		Object cacheValue;
		Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			Object cacheValueR = getCacheValueR(metaData, idIndex, id);
			cacheValue = getCacheValueFromReference(cacheValueR);

			oldAlternateCacheKeys = (CacheKey[]) keyToAlternateIdsMap.get(entityType, idIndex, id);
			if (oldAlternateCacheKeys != null) {
				for (int a = oldAlternateCacheKeys.length; a-- > 0;) {
					CacheKey alternateCacheKey = oldAlternateCacheKeys[a];
					if (alternateCacheKey != null) {
						removeKeyFromCache(alternateCacheKey);
					}
				}
			}

			if (cacheValue != null) {
				if (cacheValue != primitiveFilledObject) {
					throw new RuntimeException(
							"There is already another instance of the same entity in this cache. This is a fatal state");
				}
				// Object (same instance) already in cache. Nothing to do here
			}
			else {
				cacheValue = primitiveFilledObject;
				cacheValueR = createReference(cacheValue, entityType, idIndex, id);

				keyToCacheValueDict.put(entityType, idIndex, id, cacheValueR);
			}
			CacheKey[] newAlternateCacheKeys = oldAlternateCacheKeys;
			if (newAlternateCacheKeys == null) {
				// Allocate new array to hold alternate ids
				newAlternateCacheKeys = extractAlternateCacheKeys(metaData, parentCacheValueOrArray);
			}
			else {
				// reuse existing array for new alternate id-values
				extractAlternateCacheKeys(metaData, parentCacheValueOrArray, newAlternateCacheKeys);
			}
			if (newAlternateCacheKeys.length > 0) {
				keyToAlternateIdsMap.put(entityType, idIndex, id, newAlternateCacheKeys);
				putAlternateCacheKeysToCache(metaData, newAlternateCacheKeys, cacheValueR);
			}
		}
		finally {
			writeLock.unlock();
		}
		if (weakEntries) {
			addHardRefTL(cacheValue);
		}
		assignEntityToCache(primitiveFilledObject);
		if (relations != null && relations.length > 0) {
			handleValueHolderContainer((IValueHolderContainer) primitiveFilledObject,
					metaData.getRelationMembers(), relations);
		}
		if (primitiveFilledObject instanceof IDataObject) {
			((IDataObject) primitiveFilledObject).setToBeUpdated(false);
		}
	}

	@SuppressWarnings("unchecked")
	protected void handleValueHolderContainer(IValueHolderContainer vhc,
			RelationMember[] relationMembers, IObjRef[][] relations) {
		ICacheHelper cacheHelper = this.cacheHelper;
		ICacheIntern parent = this.parent;
		for (int relationIndex = relationMembers.length; relationIndex-- > 0;) {
			RelationMember relationMember = relationMembers[relationIndex];
			IObjRef[] relationsOfMember = relations[relationIndex];

			if (!CascadeLoadMode.EAGER.equals(relationMember.getCascadeLoadMode())) {
				if (!vhc.is__Initialized(relationIndex)) {
					// Update ObjRef information within the entity and do nothing else
					vhc.set__ObjRefs(relationIndex, relationsOfMember);
					continue;
				}
			}
			// We can safely access to relation if we want to
			if (relationsOfMember == null) {
				// Reset value holder state because we do not know the content currently
				if (vhc.is__Initialized(relationIndex)) {
					throw new IllegalStateException(
							"Relation already initialized but reverted state unknown: '"
									+ vhc.get__Self(relationIndex));
				}
				vhc.set__Uninitialized(relationIndex, null);
				continue;
			}
			Object relationValue = relationMember.getValue(vhc);
			if (relationsOfMember.length == 0) {
				if (!relationMember.isToMany()) {
					if (relationValue != null) {
						// Relation has to be flushed
						relationMember.setValue(vhc, null);
					}
				}
				else {
					if (relationValue != null) {
						// Reuse existing collection
						((Collection<?>) relationValue).clear();
					}
					else {
						// We have to create a new empty collection
						relationValue = cacheHelper.createInstanceOfTargetExpectedType(
								relationMember.getRealType(), relationMember.getElementType());
						relationMember.setValue(vhc, relationValue);
					}
				}
				continue;
			}
			// So we know the new content (which is not empty) and we know that the current content is
			// already initialized
			// Now we have to refresh the current content eagerly

			// load entities as if we were an "eager valueholder" here
			IList<Object> potentialNewItems = parent.getObjects(new ArrayList<IObjRef>(relationsOfMember),
					this, CacheDirective.none());
			if (overwriteToManyRelations) {
				Object newRelationValue = cacheHelper.convertResultListToExpectedType(potentialNewItems,
						relationMember.getRealType(), relationMember.getElementType());
				// Set new to-many-relation, even if there has not changed anything in its item content
				relationMember.setValue(vhc, newRelationValue);
				continue;
			}
			List<Object> relationItems = ListUtil.anyToList(relationValue);

			boolean diff = relationItems.size() != potentialNewItems.size();
			if (!diff) {
				for (int b = potentialNewItems.size(); b-- > 0;) {
					if (potentialNewItems.get(b) != relationItems.get(b)) {
						diff = true;
						break;
					}
				}
			}
			if (!diff) {
				// Nothing to do
				continue;
			}
			if (relationValue != null) {
				// Reuse existing collection
				Collection<Object> coll = (Collection<Object>) relationValue;
				coll.clear();
				coll.addAll(potentialNewItems);
			}
			else {
				// We have to create a new empty collection
				Object newRelationValue = cacheHelper.convertResultListToExpectedType(potentialNewItems,
						relationMember.getRealType(), relationMember.getElementType());
				relationMember.setValue(vhc, newRelationValue);
			}
		}
	}

	protected boolean isNotNullRelationValue(Object relationValue) {
		return relationValue != null && (!(relationValue instanceof IDefaultCollection)
				|| !((IDefaultCollection) relationValue).hasDefaultState());
	}

	@Override
	protected void clearIntern() {
		super.clearIntern();
		keyToAlternateIdsMap.clear();
	}

	@Override
	protected Object removeKeyFromCache(Class<?> entityType, byte idIndex, Object id) {
		if (entityType == null) {
			return null;
		}
		Object cacheValueR = super.removeKeyFromCache(entityType, idIndex, id);
		CacheKey[] alternateCacheKeys = (CacheKey[]) keyToAlternateIdsMap.remove(entityType, idIndex,
				id);
		if (alternateCacheKeys != null) {
			for (int a = alternateCacheKeys.length; a-- > 0;) {
				removeKeyFromCache(alternateCacheKeys[a]);
			}
		}
		return cacheValueR;
	}

	@Override
	protected CacheKey[] getAlternateCacheKeysFromCacheValue(IEntityMetaData metaData,
			Object cacheValue) {
		return emptyCacheKeyArray;
	}

	@Override
	public void getContent(final HandleContentDelegate handleContentDelegate) {
		checkNotDisposed();
		final CacheHashMap keyToInstanceMap = new CacheHashMap(cacheMapEntryTypeProvider);
		Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			for (CacheMapEntry entry : keyToCacheValueDict) {
				Object cacheValue = getCacheValueFromReference(entry.getValue());
				if (cacheValue == null) {
					continue;
				}
				keyToInstanceMap.put(entry.getEntityType(), entry.getIdIndex(), entry.getId(), cacheValue);
			}
			for (CacheMapEntry entry : keyToInstanceMap) {
				byte idIndex = entry.getIdIndex();
				if (idIndex == ObjRef.PRIMARY_KEY_INDEX) {
					handleContentDelegate.invoke(entry.getEntityType(), idIndex, entry.getId(),
							entry.getValue());
				}
			}
		}
		finally {
			writeLock.unlock();
		}
	}

	@Override
	protected void putInternObjRelation(Object cacheValue, IEntityMetaData metaData,
			IObjRelation objRelation, IObjRef[] relationsOfMember) {
		int relationIndex = metaData.getIndexByRelationName(objRelation.getMemberName());
		IObjRefContainer vhc = (IObjRefContainer) cacheValue;
		if (ValueHolderState.INIT == vhc.get__State(relationIndex)) {
			// It is not allowed to set ObjRefs for an already initialized relation
			return;
		}
		vhc.set__ObjRefs(relationIndex, relationsOfMember);
	}

	@Override
	protected void putInternPersistedEntity(Object entity) {
		ICacheIntern targetCache = ((IValueHolderContainer) entity).get__TargetCache();
		if (targetCache != null) {
			if (targetCache == gcProxy) {
				return;
			}
			throw new IllegalStateException(
					"Entity '" + entity + "' is owned by another cache: '" + targetCache + "'");
		}
		assignEntityToCache(entity);
		super.putInternPersistedEntity(entity);
	}

	@Override
	protected void putInternUnpersistedEntity(Object entity) {
		assignEntityToCache(entity);
		super.putInternUnpersistedEntity(entity);
	}

	@Override
	public void assignEntityToCache(Object entity) {
		((IValueHolderContainer) entity).set__TargetCache(gcProxy);
	}

	@Override
	public String toString() {
		if (name != null) {
			return name + " " + super.toString();
		}
		return super.toString();
	}
}
