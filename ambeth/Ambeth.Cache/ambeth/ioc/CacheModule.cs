﻿using System;
using De.Osthus.Ambeth.Cache;
using De.Osthus.Ambeth.Config;
using De.Osthus.Ambeth.Merge;
using De.Osthus.Ambeth.Util;
using De.Osthus.Ambeth.Ioc.Factory;
using De.Osthus.Ambeth.Proxy;
using De.Osthus.Ambeth.Service;
using De.Osthus.Ambeth.Cache.Config;
using De.Osthus.Ambeth.Remote;
#if SILVERLIGHT
using Castle.Core.Interceptor;
#else
using Castle.DynamicProxy;
#endif
using De.Osthus.Ambeth.Cache.Interceptor;
using De.Osthus.Ambeth.Ioc.Threadlocal;
using De.Osthus.Ambeth.Filter.Model;
using De.Osthus.Ambeth.Event;
using De.Osthus.Ambeth.Ioc.Annotation;
using De.Osthus.Ambeth.Merge.Config;
using De.Osthus.Ambeth.Cache.Collections;
using De.Osthus.Ambeth.Cache.Rootcachevalue;
using De.Osthus.Ambeth.Ioc.Config;
using De.Osthus.Ambeth.Walker;
using De.Osthus.Ambeth.Mixin;
using De.Osthus.Ambeth.Databinding;

namespace De.Osthus.Ambeth.Ioc
{
    [FrameworkModule]
    public class CacheModule : IInitializingModule
    {
        public const String CACHE_DATA_CHANGE_LISTENER = "cache.dcl";

        public const String COMMITTED_ROOT_CACHE = "committedRootCache";

        public const String INTERNAL_CACHE_SERVICE = "cacheService.internal";

        public const String EXTERNAL_CACHE_SERVICE = "cacheService.external";

        public const String DEFAULT_CACHE_RETRIEVER = "cacheRetriever.default";

        public const String ROOT_CACHE_RETRIEVER = "cacheRetriever.rootCache";

        public const String ROOT_CACHE = "rootCache";

        public IProxyFactory ProxyFactory { get; set; }

        [Property(ServiceConfigurationConstants.NetworkClientMode, DefaultValue = "false")]
        public bool IsNetworkClientMode { get; set; }

        [Property(CacheConfigurationConstants.CacheServiceBeanActive, DefaultValue = "true")]
        public bool IsCacheServiceBeanActive { get; set; }

        [Property(CacheConfigurationConstants.SecondLevelCacheActive, DefaultValue = "true")]
        public bool IsSecondLevelCacheActive { get; set; }

        [Property(CacheConfigurationConstants.FirstLevelCacheType, Mandatory = false)]
        public CacheType DefaultCacheType { get; set; }

        public void AfterPropertiesSet(IBeanContextFactory beanContextFactory)
        {
            ParamChecker.AssertNotNull(ProxyFactory, "ProxyFactory");

            IBeanConfiguration serviceResultcache = beanContextFactory.RegisterBean<ServiceResultCache>().Autowireable<IServiceResultCache>();
            beanContextFactory.Link(serviceResultcache, "HandleClearAllCaches").To<IEventListenerExtendable>().With(typeof(ClearAllCachesEvent));

            beanContextFactory.RegisterBean<ValueHolderIEC>().Autowireable(typeof(ValueHolderIEC), typeof(IProxyHelper));

            beanContextFactory.RegisterBean<CacheHelper>().Autowireable(typeof(ICacheHelper), typeof(ICachePathHelper), typeof(IPrefetchHelper));

			IBeanConfiguration prioMembersProvider = beanContextFactory.RegisterBean<PrioMembersProvider>().Autowireable<IPrioMembersProvider>();
			beanContextFactory.Link(prioMembersProvider, PrioMembersProvider.handleMetaDataAddedEvent).To<IEventListenerExtendable>()
				.With(typeof(IEntityMetaDataEvent));

            beanContextFactory.RegisterBean<CacheWalker>().Autowireable<ICacheWalker>();

            beanContextFactory.RegisterAutowireableBean<ICacheMapEntryTypeProvider, CacheMapEntryTypeProvider>();

            beanContextFactory.RegisterAutowireableBean<IRootCacheValueFactory, RootCacheValueFactory>();

            //IBeanConfiguration rootCache = beanContextFactory.registerBean<RootCache>("rootCache").autowireable(typeof(RootCache), typeof(IWritableCache));
            //if (IsUseSingleChildCache)
            //{
            //    beanContextFactory.registerBean<SingletonCacheFactory>("cacheFactory")
            //        .propertyRefs("singletonChildCache")
            //        .autowireable<ICacheFactory>();

            //    IWritableCache childCache = (IWritableCache)beanContextFactory.registerBean<ChildCache>("singletonChildCache")
            //        .propertyRefs("rootCache")
            //        .autowireable(typeof(ICache)).GetInstance();

            //    ((RootCache)rootCache.GetInstance()).AddChildCache(childCache);
            //}
            //else
            //{
            //    rootCache.autowireable(typeof(ICache), typeof(IWritableCache), typeof(ICacheFactory));
            //}

            //beanContextFactory.registerBean<RootCache>(ROOT_CACHE).autowireable<RootCache>();
            //beanContextFactory.Link("rootCache").To<IOfflineListenerExtendable>();

            beanContextFactory.RegisterBean<CacheRetrieverRegistry>(ROOT_CACHE_RETRIEVER).Autowireable(typeof(ICacheServiceByNameExtendable), typeof(ICacheRetrieverExtendable));

            beanContextFactory.RegisterBean<FirstLevelCacheManager>("firstLevelCacheManager").Autowireable(typeof(IFirstLevelCacheExtendable), typeof(IFirstLevelCacheManager));

            String rootCacheBridge = "rootCacheBridge";

            beanContextFactory.RegisterBean<RootCacheBridge>(rootCacheBridge).PropertyRefs(COMMITTED_ROOT_CACHE, ROOT_CACHE_RETRIEVER);

            TransactionalRootCacheInterceptor txRcInterceptor = new TransactionalRootCacheInterceptor();

            beanContextFactory.RegisterWithLifecycle("txRootCacheInterceptor", txRcInterceptor).PropertyRefs(COMMITTED_ROOT_CACHE, rootCacheBridge)
                    .Autowireable(typeof(ITransactionalRootCache), typeof(ISecondLevelCacheManager));

            Object txRcProxy = ProxyFactory.CreateProxy(new Type[] { typeof(IRootCache), typeof(ICacheIntern), typeof(IOfflineListener) }, txRcInterceptor);

            beanContextFactory.RegisterExternalBean(ROOT_CACHE, txRcProxy).Autowireable(typeof(IRootCache), typeof(ICacheIntern));

            if (IsSecondLevelCacheActive)
            {
                // One single root cache instance for whole context
                beanContextFactory.RegisterBean<RootCache>(COMMITTED_ROOT_CACHE).PropertyRef("CacheRetriever", ROOT_CACHE_RETRIEVER)
                        .PropertyValue("Privileged", true);
                beanContextFactory.Link(CacheModule.COMMITTED_ROOT_CACHE).To<IOfflineListenerExtendable>();
            }
            else
            {
                // One root cache instance per thread sequence. Most often used in server environment where the "deactivated"
                // second level cache means that each thread hold his own, isolated root cache (which gets cleared with each service
                // request. Effectively this means that the root cache itself only lives per-request and does not hold a longer state
                IInterceptor threadLocalRootCacheInterceptor = (IInterceptor)beanContextFactory
                        .RegisterBean<ThreadLocalRootCacheInterceptor>("threadLocalRootCacheInterceptor")
                        .PropertyRef("StoredCacheRetriever", CacheModule.ROOT_CACHE_RETRIEVER).PropertyValue("Privileged", true).GetInstance();

                RootCache rootCacheProxy = ProxyFactory.CreateProxy<RootCache>(threadLocalRootCacheInterceptor);

                beanContextFactory.RegisterExternalBean(CacheModule.COMMITTED_ROOT_CACHE, rootCacheProxy).Autowireable<RootCache>();
            }
            beanContextFactory.RegisterBean<CacheEventTargetExtractor>("cacheEventTargetExtractor");
            beanContextFactory.Link("cacheEventTargetExtractor").To<IEventTargetExtractorExtendable>().With(typeof(ICache));

            beanContextFactory.RegisterBean<CacheFactory>().Autowireable<ICacheFactory>();

            IInterceptor cacheProviderInterceptor = (IInterceptor)beanContextFactory
                    .RegisterBean<CacheProviderInterceptor>("cacheProviderInterceptor")
                    .Autowireable(typeof(ICacheProviderExtendable), typeof(ICacheProvider), typeof(ICacheContext)).GetInstance();

            ICache cacheProxy = ProxyFactory.CreateProxy<ICache>(new Type[] { typeof(ICacheProvider), typeof(IWritableCache) }, cacheProviderInterceptor);
            beanContextFactory.RegisterExternalBean("cache", cacheProxy).Autowireable<ICache>();

            beanContextFactory.RegisterBean<PagingQueryServiceResultProcessor>("pagingQuerySRP");
            beanContextFactory.Link("pagingQuerySRP").To<IServiceResultProcessorExtendable>().With(typeof(IPagingResponse));

            beanContextFactory.RegisterBean<CacheProvider>(CacheNamedBeans.CacheProviderSingleton).PropertyValue("CacheType", CacheType.SINGLETON);

            beanContextFactory.RegisterBean<CacheProvider>(CacheNamedBeans.CacheProviderThreadLocal).PropertyValue("CacheType", CacheType.THREAD_LOCAL);

            beanContextFactory.RegisterBean<CacheProvider>(CacheNamedBeans.CacheProviderPrototype).PropertyValue("CacheType", CacheType.PROTOTYPE);

            String defaultCacheProviderBeanName;
            switch (DefaultCacheType)
            {
                case CacheType.PROTOTYPE:
                    {
                        defaultCacheProviderBeanName = CacheNamedBeans.CacheProviderPrototype;
                        break;
                    }
                case CacheType.SINGLETON:
                    {
                        defaultCacheProviderBeanName = CacheNamedBeans.CacheProviderSingleton;
                        break;
                    }
                case CacheType.THREAD_LOCAL:
                    {
                        defaultCacheProviderBeanName = CacheNamedBeans.CacheProviderThreadLocal;
                        break;
                    }
                case CacheType.DEFAULT:
                    {
                        defaultCacheProviderBeanName = CacheNamedBeans.CacheProviderThreadLocal;
                        break;
                    }
                default:
                    throw new Exception("Not supported type: " + DefaultCacheType);
            }
            beanContextFactory.Link(defaultCacheProviderBeanName).To<ICacheProviderExtendable>();

            // CacheContextPostProcessor must be registered AFTER CachePostProcessor...
            Object cachePostProcessor = beanContextFactory.RegisterBean<CachePostProcessor>().GetInstance();
            beanContextFactory.RegisterBean<CacheContextPostProcessor>().PropertyValue("CachePostProcessor", cachePostProcessor);

            if (IsNetworkClientMode && IsCacheServiceBeanActive)
            {
                IBeanConfiguration remoteCacheService = beanContextFactory.RegisterBean<ClientServiceBean>(CacheModule.EXTERNAL_CACHE_SERVICE)
                    .PropertyValue("Interface", typeof(ICacheService))
                    .PropertyValue("SyncRemoteInterface", typeof(ICacheServiceWCF))
                    .PropertyValue("AsyncRemoteInterface", typeof(ICacheClient)).Autowireable<ICacheService>();

                beanContextFactory.RegisterAlias(CacheModule.DEFAULT_CACHE_RETRIEVER, CacheModule.EXTERNAL_CACHE_SERVICE);

                // register to all entities in a "most-weak" manner
                beanContextFactory.Link(remoteCacheService).To<ICacheRetrieverExtendable>().With(typeof(Object));
                //beanContextFactory.RegisterAlias(CacheModule.ROOT_CACHE_RETRIEVER, CacheModule.EXTERNAL_CACHE_SERVICE);
                //beanContextFactory.registerBean<CacheServiceDelegate>("cacheService").autowireable<ICacheService>();
            }
            beanContextFactory.RegisterBean<DataObjectMixin>().Autowireable<DataObjectMixin>();
            beanContextFactory.RegisterBean<EntityEqualsMixin>().Autowireable<EntityEqualsMixin>();
            beanContextFactory.RegisterBean<EmbeddedTypeMixin>().Autowireable<EmbeddedTypeMixin>();
            beanContextFactory.RegisterBean<PropertyChangeMixin>().Autowireable(typeof(PropertyChangeMixin),
                typeof(IPropertyChangeExtensionExtendable), typeof(ICollectionChangeExtensionExtendable));
            beanContextFactory.RegisterBean<ValueHolderContainerMixin>().Autowireable<ValueHolderContainerMixin>();
        }
    }
}