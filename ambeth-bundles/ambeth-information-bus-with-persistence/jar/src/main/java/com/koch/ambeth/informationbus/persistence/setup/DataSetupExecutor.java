package com.koch.ambeth.informationbus.persistence.setup;

/*-
 * #%L
 * jambeth-information-bus-with-persistence-test
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

import com.koch.ambeth.audit.server.IAuditInfoController;
import com.koch.ambeth.ioc.IStartingBean;
import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.log.ILogger;
import com.koch.ambeth.log.LogInstance;
import com.koch.ambeth.merge.ILightweightTransaction;
import com.koch.ambeth.merge.IMergeProcess;
import com.koch.ambeth.merge.security.ISecurityActivation;
import com.koch.ambeth.merge.util.setup.IDataSetup;
import com.koch.ambeth.merge.util.setup.IDataSetupWithAuthorization;
import com.koch.ambeth.security.persistence.IPermissionGroupUpdater;
import com.koch.ambeth.security.server.IPasswordUtil;
import com.koch.ambeth.util.exception.RuntimeExceptionUtil;
import com.koch.ambeth.util.state.IStateRollback;
import com.koch.ambeth.util.threading.IBackgroundWorkerDelegate;
import com.koch.ambeth.util.threading.IResultingBackgroundWorkerDelegate;

public class DataSetupExecutor implements IStartingBean {
	private static final ThreadLocal<Boolean> autoRebuildDataTL = new ThreadLocal<>();

	public static Boolean setAutoRebuildData(Boolean autoRebuildData) {
		Boolean oldValue = autoRebuildDataTL.get();
		if (autoRebuildData == null) {
			autoRebuildDataTL.remove();
		}
		else {
			autoRebuildDataTL.set(autoRebuildData);
		}
		return oldValue;
	}

	@LogInstance
	private ILogger log;

	@Autowired(optional = true)
	protected IAuditInfoController auditInfoController;

	@Autowired
	protected IPermissionGroupUpdater permissionGroupUpdater;

	@Autowired
	protected IDataSetup dataSetup;

	@Autowired
	protected IMergeProcess mergeProcess;

	@Autowired
	protected IPasswordUtil passwordUtil;

	@Autowired
	protected ISecurityActivation securityActivation;

	@Autowired
	protected ILightweightTransaction transaction;

	@Override
	public void afterStarted() throws Throwable {
		if (Boolean.TRUE.equals(autoRebuildDataTL.get())) {
			rebuildData();
		}
	}

	public void rebuildData() {
		if (auditInfoController != null) {
			auditInfoController.pushAuditReason("Data Rebuild!");
		}
		IStateRollback rollback = securityActivation
				.pushWithoutSecurity(IStateRollback.EMPTY_ROLLBACKS);
		try {
			rollback = passwordUtil.pushSuppressPasswordValidation(rollback);

			log.info("Processing test data setup...");
			final Collection<Object> dataSet = dataSetup.executeDatasetBuilders();
			log.info("Test data setup defined");

			IDataSetupWithAuthorization dataSetupWithAuthorization = dataSetup
					.resolveDataSetupWithAuthorization();
			if (dataSetupWithAuthorization != null) {
				rollback = dataSetupWithAuthorization.pushAuthorization(rollback);
			}
			transaction.runInTransaction(new IBackgroundWorkerDelegate() {
				@Override
				public void invoke() throws Exception {
					permissionGroupUpdater.executeWithoutPermissionGroupUpdate(
							new IResultingBackgroundWorkerDelegate<Object>() {
								@Override
								public Object invoke() throws Exception {
									if (!dataSet.isEmpty()) {
										log.info("Merging created test data");
										mergeProcess.process(dataSet);
										log.info("Merging of created test data finished");
									}
									return null;
								}
							});
					log.info("Filling potential permission group tables based in created test data");
					permissionGroupUpdater.fillEmptyPermissionGroups();
					log.info("Filling of potential permission group tables finished");
				}
			});
		}
		catch (Exception e) {
			throw RuntimeExceptionUtil.mask(e);
		}
		finally {
			rollback.rollback();
			if (auditInfoController != null) {
				auditInfoController.popAuditReason();
			}
		}
	}
}
