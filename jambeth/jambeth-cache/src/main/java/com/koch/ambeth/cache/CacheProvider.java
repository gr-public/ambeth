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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.koch.ambeth.ioc.IInitializingBean;
import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.ioc.config.Property;
import com.koch.ambeth.ioc.threadlocal.Forkable;
import com.koch.ambeth.ioc.threadlocal.IThreadLocalCleanupBean;
import com.koch.ambeth.merge.cache.CacheFactoryDirective;
import com.koch.ambeth.merge.cache.ICache;
import com.koch.ambeth.merge.cache.ICacheFactory;
import com.koch.ambeth.merge.cache.ICacheProvider;
import com.koch.ambeth.merge.cache.IDisposableCache;
import com.koch.ambeth.merge.config.MergeConfigurationConstants;
import com.koch.ambeth.merge.security.ISecurityActivation;
import com.koch.ambeth.util.threading.SensitiveThreadLocal;

public class CacheProvider implements IInitializingBean, IThreadLocalCleanupBean, ICacheProvider {
	@Autowired
	protected ICacheFactory cacheFactory;

	@Autowired
	protected IRootCache rootCache;

	@Autowired(optional = true)
	protected ISecurityActivation securityActivation;

	@Property(name = MergeConfigurationConstants.SecurityActive, defaultValue = "false")
	protected boolean securityActive;

	@Property
	protected CacheType cacheType;

	protected volatile ICache singletonCache;

	protected volatile ICache privilegedSingletonCache;

	@Forkable
	protected ThreadLocal<IDisposableCache> cacheTL;

	@Forkable
	protected ThreadLocal<IDisposableCache> privilegedCacheTL;

	protected final Lock lock = new ReentrantLock();

	@Override
	public void afterPropertiesSet() throws Throwable {
		switch (cacheType) {
			case PROTOTYPE: {
				break;
			}
			case SINGLETON: {
				break;
			}
			case THREAD_LOCAL: {
				cacheTL = new SensitiveThreadLocal<>();
				if (securityActivation != null) {
					privilegedCacheTL = new SensitiveThreadLocal<>();
				}
				break;
			}
			default:
				throw new IllegalStateException("Not supported type: " + cacheType);
		}
	}

	@Override
	public void cleanupThreadLocal() {
		if (cacheTL != null) {
			IDisposableCache cache = cacheTL.get();
			if (cache != null) {
				cacheTL.set(null);
				cache.dispose();
			}
		}
		if (privilegedCacheTL != null) {
			IDisposableCache cache = privilegedCacheTL.get();
			if (cache != null) {
				privilegedCacheTL.set(null);
				cache.dispose();
			}
		}
	}

	@Override
	public boolean isNewInstanceOnCall() {
		switch (cacheType) {
			case PROTOTYPE: {
				return true;
			}
			case SINGLETON:
			case THREAD_LOCAL: {
				return false;
			}
			default:
				throw new RuntimeException("Not supported type: " + cacheType);
		}
	}

	@Override
	public ICache getCurrentCache() {
		switch (cacheType) {
			case PROTOTYPE: {
				if (!securityActive || !securityActivation.isFilterActivated()) {
					return cacheFactory.createPrivileged(CacheFactoryDirective.SubscribeTransactionalDCE,
							false, null, "CacheProvider.PROTOTYPE.privileged");
				}
				return cacheFactory.create(CacheFactoryDirective.SubscribeTransactionalDCE, false, null,
						"CacheProvider.PROTOTYPE");
			}
			case SINGLETON: {
				if (!securityActive || !securityActivation.isFilterActivated()) {
					if (privilegedSingletonCache != null) {
						return privilegedSingletonCache;
					}
					lock.lock();
					try {
						if (privilegedSingletonCache != null) {
							// concurrent thread might have been faster
							return privilegedSingletonCache;
						}
						privilegedSingletonCache = cacheFactory.createPrivileged(
								CacheFactoryDirective.SubscribeTransactionalDCE, true, null,
								"CacheProvider.SINGLETON.privileged");
						return privilegedSingletonCache;
					}
					finally {
						lock.unlock();
					}
				}
				else if (singletonCache != null) {
					return singletonCache;
				}
				lock.lock();
				try {
					if (singletonCache != null) {
						// concurrent thread might have been faster
						return singletonCache;
					}
					singletonCache = cacheFactory.create(CacheFactoryDirective.SubscribeTransactionalDCE,
							true, null, "CacheProvider.SINGLETON");
					return singletonCache;
				}
				finally {
					lock.unlock();
				}
			}
			case THREAD_LOCAL: {
				if (!securityActive || !securityActivation.isFilterActivated()) {
					IDisposableCache cache = privilegedCacheTL.get();
					if (cache == null) {
						cache = cacheFactory.createPrivileged(CacheFactoryDirective.SubscribeTransactionalDCE,
								false, Boolean.FALSE, "CacheProvider.THREAD_LOCAL.privileged");
						privilegedCacheTL.set(cache);
					}
					return cache;
				}
				else {
					IDisposableCache cache = cacheTL.get();
					if (cache == null) {
						cache = cacheFactory.create(CacheFactoryDirective.SubscribeTransactionalDCE, false,
								Boolean.FALSE, "CacheProvider.THREAD_LOCAL");
						cacheTL.set(cache);
					}
					return cache;
				}
			}
			default:
				throw new IllegalStateException("Not supported type: " + cacheType);
		}

	}

	@Override
	public String toString() {
		return "CacheProvider" + cacheType + " " + super.toString();
	}
}
