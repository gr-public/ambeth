package com.koch.ambeth.persistence.jdbc.connector;

/*-
 * #%L
 * jambeth-persistence-jdbc
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

import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.koch.ambeth.ioc.IInitializingModule;
import com.koch.ambeth.ioc.IPropertyLoadingBean;
import com.koch.ambeth.ioc.annotation.FrameworkModule;
import com.koch.ambeth.ioc.config.Property;
import com.koch.ambeth.ioc.factory.IBeanContextFactory;
import com.koch.ambeth.log.config.Properties;
import com.koch.ambeth.persistence.jdbc.config.PersistenceJdbcConfigurationConstants;
import com.koch.ambeth.util.ParamChecker;
import com.koch.ambeth.util.exception.RuntimeExceptionUtil;

@FrameworkModule
public class DialectSelectorModule implements IInitializingModule, IPropertyLoadingBean {
	public static void fillProperties(Properties props) {
		String databaseProtocol = props
				.getString(PersistenceJdbcConfigurationConstants.DatabaseProtocol);
		if (databaseProtocol == null) {
			return;
		}
		IConnector connector = loadConnector(databaseProtocol);
		connector.handleProperties(props, databaseProtocol);
	}

	protected static IConnector loadConnector(String databaseProtocol) {
		String connectorName = databaseProtocol.toLowerCase().replace(':', '.') + '.'
				+ databaseProtocol.toUpperCase().replace(':', '_');
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String fqConnectorName = DialectSelectorModule.class.getPackage().getName() + "."
				+ connectorName;
		try {
			Class<?> connectorType = classLoader.loadClass(fqConnectorName);
			return (IConnector) connectorType.newInstance();
		}
		catch (Throwable e) {
			try {
				Class<?> connectorType = DialectSelectorModule.class.getClassLoader()
						.loadClass(fqConnectorName);
				return (IConnector) connectorType.newInstance();
			}
			catch (Throwable e2) {
				throw new IllegalStateException("Protocol not supported: '" + databaseProtocol + "'", e);
			}
		}
	}

	@Property(name = PersistenceJdbcConfigurationConstants.DatabaseProtocol, mandatory = false)
	protected String databaseProtocol;

	@Property(name = PersistenceJdbcConfigurationConstants.DataSourceName, mandatory = false)
	protected String dataSourceName;

	@Property(name = PersistenceJdbcConfigurationConstants.DataSourceInstance, mandatory = false)
	protected DataSource dataSource;

	@Override
	public void afterPropertiesSet(IBeanContextFactory beanContextFactory) throws Throwable {
		if (databaseProtocol == null) {
			// At this point databaseProtocol MUST be initialized
			ParamChecker.assertNotNull(databaseProtocol, "databaseProtocol");
		}
		IConnector connector = loadConnector(databaseProtocol);
		connector.handleProd(beanContextFactory, databaseProtocol);
	}

	@Override
	public void applyProperties(Properties contextProperties) {
		if (contextProperties.get(PersistenceJdbcConfigurationConstants.DatabaseProtocol) != null) {
			return;
		}

		// If we don't use the integrated connection factory,
		try {
			DataSource dataSource = this.dataSource;
			if (dataSource == null) {
				InitialContext ic = new InitialContext();
				dataSource = (DataSource) ic.lookup(dataSourceName);
			}
			Connection connection = dataSource.getConnection();
			try {
				String connectionUrl = connection.getMetaData().getURL();
				if (contextProperties
						.get(PersistenceJdbcConfigurationConstants.DatabaseConnection) == null) {
					contextProperties.putString(PersistenceJdbcConfigurationConstants.DatabaseConnection,
							connectionUrl);
				}
				Matcher urlMatcher;
				if (connectionUrl.contains(":@")) {
					// Oracle
					// jdbc:oracle:driver:username/password@host:port:database
					urlMatcher = Pattern.compile("^(jdbc:[^:]+:[^:]+)(?::[^:/]+/[^:]+)?:@.*")
							.matcher(connectionUrl);
					// Ignore ([^:]+)(?::(\\d++))?(?::([^:]+))?$ => host:post/database?params
				}
				else {
					// Use everything from jdbc to the second :
					// Postgresql, MySql, SqlServer
					// jdbc:driver://host:port/database?user=...
					// jdbc:h2:tcp://localhost/~/test;AUTO_RECONNECT=TRUE
					// Derby, DB2, Sybase, H2 non-urls
					// jdbc:driver:...
					urlMatcher = Pattern.compile("^(jdbc:[^:]+)(:.*)?").matcher(connectionUrl);
				}
				if (urlMatcher.matches()) {
					String protocol = urlMatcher.group(1);
					contextProperties.putString(PersistenceJdbcConfigurationConstants.DatabaseProtocol,
							protocol);
					databaseProtocol = protocol;
				}
			}
			finally {
				connection.close();
			}
		}
		catch (Throwable e) {
			throw RuntimeExceptionUtil.mask(e, "The " + getClass().getSimpleName()
					+ " was not able to get the database protocol from the dataSource");
			// Do nothing and hope that the connection is configured elsewhere
		}
	}
}
