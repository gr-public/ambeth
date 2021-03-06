package com.koch.ambeth.merge.cache;

/*-
 * #%L
 * jambeth-merge
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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Allows to customize the behavior a cache processes requests.<br>
 * <br>
 * A valid behavior considers the following rules:<br>
 * <br>
 * Rule 1) Either <code>FailEarly</code> or <code>FailInCacheHierarchy</code> or none of them for
 * default behavior in that aspect<br>
 * Rule 2) Either <codoe>NoResult</code>, <code>LoadContainerResult</code>,
 * <code>CacheValueResult</code> or none of them for default behavior in that aspect<br>
 * Rule 3) <code>ReturnMisses</code> or nothing for default behavior in that aspect
 */
public enum CacheDirective {
	/**
	 * Default cache request behavior. Which can be separated in 3 specific aspects:<br>
	 * <br>
	 * 1) Always returns entity instances even if you call this on an instance of RootCache<br>
	 * 2) Does transparently try to resolve cache-misses by cascading through the cache hierarchy and
	 * calling remote CacheRetrievers<br>
	 * 3) Does return only cache-hits. So if at least a single miss happened the result does NOT
	 * correlate by index with the requested ObjRefs
	 */
	None, //

	/**
	 * Customizes the default behavior in a specific aspect:<br>
	 * 2) Does NOT try to resolve cache-misses. It does NOT cascade through the cache hierarchy<br>
	 */
	FailEarly, //

	/**
	 * Customizes the default behavior in a specific aspect:<br>
	 * 2) Does transparently try to resolve cache-misses by cascading through the cache hierarchy but
	 * NOT calling remote CacheRetrievers<br>
	 */
	FailInCacheHierarchy, //

	/**
	 * Customizes the default behavior in a specific aspect:<br>
	 * 3) Does return cache-hits as well as null entries for cache-misses. So the result does always
	 * correlate by index with the requested ObjRefs
	 */
	ReturnMisses, //

	/**
	 * Customizes the default behavior in a specific aspect:<br>
	 * 3) Does not return any result<br>
	 * <br>
	 * This may be useful to "ensure" that a cache contains an entry after the request. But keep in
	 * mind that this makes no sense if the cache refers weakly to its entries because there is no
	 * guarantee how long the cache is able to hold the target instance due to GC
	 */
	NoResult, //

	/**
	 * Customizes the default behavior in a specific aspect:<br>
	 * 3) Always returns <code>ILoadContainer</code> instances. If you call this on a
	 * <code>ChildCache</code> the request is directly passed through the parent
	 * <code>RootCache</code> and executed there to build the response<br>
	 */
	LoadContainerResult, //

	/**
	 * Customizes the default behavior in a specific aspect:<br>
	 * 3) Always returns the real internal cache entries. If you call this on a
	 * <code>ChildCache</code> the request is directly passed through the parent
	 * <code>RootCache</code> and executed there to build the response. A <code>RootCache</code>
	 * returns the direct reference to the internal cache entry.<br>
	 * <br>
	 * As long as you hold a hard reference to the result it can be ensured that even for weakly
	 * referencing <code>ChildCache</code> or <code>RootCache</code> instances the internal entries
	 * are NOT lost due to GC.<br>
	 * <br>
	 * CAUTION: Any read or write access to these exposed cache instances in the result are not
	 * thread-safe and the resulting behavior is undefined.
	 */
	CacheValueResult;

	private static Set<CacheDirective> cacheValueResultSet = EnumSet.of(CacheValueResult);

	private static Set<CacheDirective> loadContainerResultSet = EnumSet.of(LoadContainerResult);

	private static Set<CacheDirective> noResultSet = EnumSet.of(NoResult);

	private static Set<CacheDirective> returnMissesSet = EnumSet.of(ReturnMisses);

	private static Set<CacheDirective> failEarlySet = EnumSet.of(FailEarly);

	private static Set<CacheDirective> failEarlyAndReturnMissesSet =
			EnumSet.of(FailEarly, ReturnMisses);

	private static Set<CacheDirective> failInCacheHierarchySet = EnumSet.of(FailInCacheHierarchy);

	public static Set<CacheDirective> cacheValueResult() {
		return cacheValueResultSet;
	}

	public static Set<CacheDirective> failEarly() {
		return failEarlySet;
	}

	public static Set<CacheDirective> failEarlyAndReturnMisses() {
		return failEarlyAndReturnMissesSet;
	}

	public static Set<CacheDirective> failInCacheHierarchy() {
		return failInCacheHierarchySet;
	}

	public static Set<CacheDirective> loadContainerResult() {
		return loadContainerResultSet;
	}

	public static Set<CacheDirective> noResult() {
		return noResultSet;
	}

	public static Set<CacheDirective> returnMisses() {
		return returnMissesSet;
	}

	public static Set<CacheDirective> none() {
		return Collections.<CacheDirective>emptySet();
	}
}
