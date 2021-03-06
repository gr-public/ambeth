package com.koch.ambeth.cache.util;

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

import java.lang.ref.WeakReference;

import com.koch.ambeth.service.metadata.Member;
import com.koch.ambeth.util.collections.ILinkedMap;
import com.koch.ambeth.util.collections.IdentityLinkedSet;

public class PrioMembersKey extends WeakReference<ILinkedMap<Class<?>, PrefetchPath[]>> {
	private final IdentityLinkedSet<Member> key1;

	public PrioMembersKey(ILinkedMap<Class<?>, PrefetchPath[]> referent,
			IdentityLinkedSet<Member> key1) {
		super(referent);
		this.key1 = key1;
	}

	public IdentityLinkedSet<Member> getKey1() {
		return key1;
	}
}
