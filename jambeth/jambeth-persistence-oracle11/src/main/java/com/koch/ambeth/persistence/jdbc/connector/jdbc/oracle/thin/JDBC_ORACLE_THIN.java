package com.koch.ambeth.persistence.jdbc.connector.jdbc.oracle.thin;

/*-
 * #%L
 * jambeth-persistence-oracle11
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

import com.koch.ambeth.ioc.factory.IBeanContextFactory;
import com.koch.ambeth.log.config.Properties;
import com.koch.ambeth.persistence.jdbc.config.PersistenceJdbcConfigurationConstants;
import com.koch.ambeth.persistence.jdbc.connector.IConnector;
import com.koch.ambeth.persistence.oracle.Oracle10gConnectionModule;
import com.koch.ambeth.persistence.oracle.Oracle10gModule;
import com.koch.ambeth.persistence.oracle.Oracle11gModule;

public class JDBC_ORACLE_THIN implements IConnector {
	@Override
	public void handleProperties(Properties props, String databaseProtocol) {
		props.put(PersistenceJdbcConfigurationConstants.AdditionalConnectionModules,
				Oracle10gConnectionModule.class.getName());
	}

	@Override
	public void handleProd(IBeanContextFactory beanContextFactory, String databaseProtocol) {
		beanContextFactory.registerBean(Oracle10gModule.class);
		beanContextFactory.registerBean(Oracle11gModule.class);
	}
}
