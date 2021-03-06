package com.koch.ambeth.cache.rootcachevalue;

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

import com.koch.ambeth.merge.cache.AbstractCacheValue;
import com.koch.ambeth.merge.cache.ICache;
import com.koch.ambeth.merge.cache.ValueHolderState;
import com.koch.ambeth.merge.proxy.IObjRefContainer;
import com.koch.ambeth.service.merge.model.IEntityMetaData;
import com.koch.ambeth.service.merge.model.IObjRef;
import com.koch.ambeth.util.collections.IListElem;

public abstract class RootCacheValue extends AbstractCacheValue
		implements IListElem<RootCacheValue>, IObjRefContainer {
	protected Object listHandle;

	protected IListElem<RootCacheValue> next, prev;

	public RootCacheValue(IEntityMetaData metaData) {
		// Intended blank
	}

	@Override
	public ICache get__Cache() {
		return null;
	}

	public abstract void setPrimitives(Object[] primitives);

	public abstract IObjRef[][] getRelations();

	public abstract void setRelations(IObjRef[][] relations);

	public abstract IObjRef[] getRelation(int relationIndex);

	public abstract void setRelation(int relationIndex, IObjRef[] relationsOfMember);

	@Override
	public Object getListHandle() {
		return listHandle;
	}

	@Override
	public void setListHandle(Object listHandle) {
		if (this.listHandle != null && listHandle != null) {
			throw new UnsupportedOperationException();
		}
		this.listHandle = listHandle;
	}

	@Override
	public IListElem<RootCacheValue> getNext() {
		return next;
	}

	@Override
	public void setNext(IListElem<RootCacheValue> next) {
		this.next = next;
	}

	@Override
	public IListElem<RootCacheValue> getPrev() {
		return prev;
	}

	@Override
	public void setPrev(IListElem<RootCacheValue> prev) {
		this.prev = prev;
	}

	@Override
	public RootCacheValue getElemValue() {
		return this;
	}

	@Override
	public void setElemValue(RootCacheValue value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ValueHolderState get__State(int relationIndex) {
		return ValueHolderState.LAZY;
	}

	@Override
	public boolean is__Initialized(int relationIndex) {
		return false;
	}

	@Override
	public IObjRef[] get__ObjRefs(int relationIndex) {
		return getRelation(relationIndex);
	}

	@Override
	public void set__ObjRefs(int relationIndex, IObjRef[] objRefs) {
		setRelation(relationIndex, objRefs);
	}

	@Override
	public void detach() {
		// intended blank
	}
}
