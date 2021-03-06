package com.koch.ambeth.service;

/*-
 * #%L
 * jambeth-test
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.koch.ambeth.informationbus.persistence.setup.SQLData;
import com.koch.ambeth.informationbus.persistence.setup.SQLStructure;
import com.koch.ambeth.model.Material;
import com.koch.ambeth.model.MaterialGroup;
import com.koch.ambeth.service.config.ServiceConfigurationConstants;
import com.koch.ambeth.service.transfer.ServiceDescription;
import com.koch.ambeth.testutil.AbstractInformationBusWithPersistenceTest;
import com.koch.ambeth.testutil.TestModule;
import com.koch.ambeth.testutil.TestProperties;
import com.koch.ambeth.transfer.ITestService;
import com.koch.ambeth.util.ParamChecker;

@SQLStructure("ProcessServiceTest_structure.sql")
@TestModule(ProcessServiceTestModule.class)
@TestProperties(name = ServiceConfigurationConstants.mappingFile,
		value = "com/koch/ambeth/service/orm.xml")
public class ProcessServiceTest extends AbstractInformationBusWithPersistenceTest {
	private static final Object[] NO_PARAMS = {};

	private IProcessService fixture;

	@Override
	public void afterPropertiesSet() throws Throwable {
		super.afterPropertiesSet();

		ParamChecker.assertNotNull(fixture, "fixture");
	}

	public void setProcessService(IProcessService processService) {
		fixture = processService;
	}

	@Test
	public void testInvokeService_primitiveReturn() throws Exception {
		Object actual = invokeServiceMethod("noParamPrimitiveReturn");
		assertNotNull(actual);
		assertEquals(1, actual);
	}

	@Test
	public void testInvokeService_primitiveArrayReturn() throws Exception {
		Object actual = invokeServiceMethod("noParamPrimitiveArrayReturn");
		assertNotNull(actual);
		int[] expected = {1, 2, 34};
		assertArrayEquals(expected, (int[]) actual);
	}

	@Test
	public void testInvokeService_primitiveListReturn() throws Exception {
		Object actual = invokeServiceMethod("noParamPrimitiveListReturn");
		assertNotNull(actual);
		@SuppressWarnings("unchecked")
		List<Integer> actualList = (List<Integer>) actual;
		Integer[] actualArray = actualList.toArray(new Integer[actualList.size()]);
		Integer[] expected = {12, 3, 4};
		assertArrayEquals(expected, actualArray);
	}

	@Test
	public void testInvokeService_dateReturn() throws Exception {
		Object actual = invokeServiceMethod("noParamDateReturn");
		assertNotNull(actual);
		long actualTime = ((Date) actual).getTime();
		long difference = System.currentTimeMillis() - actualTime;
		assertTrue(difference < 100);
	}

	@Test
	@SQLData("ProcessServiceTest_data.sql")
	public void testInvokeService_entityReturn() throws Exception {
		Object actual = invokeServiceMethod("noParamEntityReturn");
		assertNotNull(actual);
		assertTrue(actual instanceof MaterialGroup);
	}

	@Test
	@SQLData("ProcessServiceTest_data.sql")
	public void testInvokeService_entityWithRelationReturn() throws Exception {
		Object actual = invokeServiceMethod("noParamEntityWithRelationReturn");
		assertNotNull(actual);
		assertTrue(actual instanceof Material);
		Material material = (Material) actual;
		assertNotNull(material.getMaterialGroup());
	}

	private Object invokeServiceMethod(String methodName) throws NoSuchMethodException {
		Method serviceMethod = ITestService.class.getMethod(methodName);
		ServiceDescription serviceDescription = SyncToAsyncUtil
				.createServiceDescription(TestService.class.getSimpleName(), serviceMethod, NO_PARAMS);
		Object actual = fixture.invokeService(serviceDescription);
		return actual;
	}
}
