package com.koch.ambeth.filter;

/*-
 * #%L
 * jambeth-persistence-test
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

import com.koch.ambeth.ioc.IInitializingModule;
import com.koch.ambeth.ioc.factory.IBeanContextFactory;
import com.koch.ambeth.merge.DefaultProxyHelper;
import com.koch.ambeth.merge.IProxyHelper;
import com.koch.ambeth.merge.util.XmlConfigUtil;
import com.koch.ambeth.persistence.filter.FilterToQueryBuilder;
import com.koch.ambeth.query.IQueryBuilderFactory;
import com.koch.ambeth.util.xml.IXmlConfigUtil;

public class FilterToQueryBuilderTestModule implements IInitializingModule {
	@Override
	public void afterPropertiesSet(IBeanContextFactory beanContextFactory) throws Throwable {
		beanContextFactory.registerBean(FilterToQueryBuilder.class)
				.autowireable(FilterToQueryBuilder.class);
		beanContextFactory.registerBean("queryBuilderDummy", QueryBuilderFactoryDummy.class)
				.autowireable(IQueryBuilderFactory.class);
		beanContextFactory.registerBean("xmlConfigUtil", XmlConfigUtil.class)
				.autowireable(IXmlConfigUtil.class);
		beanContextFactory.registerBean("proxyHelper", DefaultProxyHelper.class)
				.autowireable(IProxyHelper.class);
	}
}
