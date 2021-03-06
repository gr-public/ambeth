package com.koch.ambeth.xml.oriwrapper;

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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.koch.ambeth.informationbus.persistence.setup.SQLData;
import com.koch.ambeth.informationbus.persistence.setup.SQLDataList;
import com.koch.ambeth.informationbus.persistence.setup.SQLStructure;
import com.koch.ambeth.informationbus.persistence.setup.SQLStructureList;
import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.model.Material;
import com.koch.ambeth.model.MaterialGroup;
import com.koch.ambeth.service.ProcessServiceTestModule;
import com.koch.ambeth.service.config.ServiceConfigurationConstants;
import com.koch.ambeth.service.transfer.ServiceDescription;
import com.koch.ambeth.testutil.AbstractInformationBusWithPersistenceTest;
import com.koch.ambeth.testutil.TestModule;
import com.koch.ambeth.testutil.TestProperties;
import com.koch.ambeth.testutil.TestPropertiesList;
import com.koch.ambeth.xml.ICyclicXMLHandler;
import com.koch.ambeth.xml.ioc.BootstrapScannerModule;
import com.koch.ambeth.xml.ioc.XmlModule;
import com.koch.ambeth.xml.oriwrapper.OriWrapperTestBed.TestData;

@SQLStructureList({@SQLStructure("../../service/ProcessServiceTest_structure.sql"),
		@SQLStructure("OriWrapper_structure.sql")})
@SQLDataList({@SQLData("../../service/ProcessServiceTest_data.sql"),
		@SQLData("OriWrapper_data.sql")})
@TestPropertiesList({
		@TestProperties(file = "com/koch/ambeth/xml/oriwrapper/OriWrapperTestData.properties"),
		@TestProperties(name = ServiceConfigurationConstants.mappingFile,
				value = "com/koch/ambeth/service/orm.xml;com/koch/ambeth/xml/oriwrapper/orm.xml")})
@TestModule({BootstrapScannerModule.class, XmlModule.class, ProcessServiceTestModule.class,
		OriWrapperTestModule.class})
public class OriWrapperReadTest extends AbstractInformationBusWithPersistenceTest {
	@Autowired(XmlModule.CYCLIC_XML_HANDLER)
	protected ICyclicXMLHandler cyclicXmlHandler;

	@Autowired
	protected OriWrapperTestBed oriWrapperTestBed;

	@Override
	public void afterPropertiesSet() throws Throwable {
		super.afterPropertiesSet();

		oriWrapperTestBed.init();
	}

	@Test
	public void readSimpleEntity() {
		TestData testData = oriWrapperTestBed.getSimpleEntityTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		Assert.assertEquals(testData.obj, obj);
	}

	@Test
	public void readEntityWithRelation() {
		TestData testData = oriWrapperTestBed.getEntityWithRelationTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		Assert.assertEquals(testData.obj, obj);
	}

	@Test
	public void readMixedArray() {
		TestData testData = oriWrapperTestBed.getMixedArrayTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		Assert.assertArrayEquals((Object[]) testData.obj, (Object[]) obj);
	}

	@Test
	public void readMixedList() {
		TestData testData = oriWrapperTestBed.getMixedListTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		assertCollectionEquals((Collection<?>) testData.obj, (Collection<?>) obj);
	}

	@Test
	public void readMixedLinkedSet() {
		TestData testData = oriWrapperTestBed.getMixedLinkedSetTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		assertCollectionEquals((Collection<?>) testData.obj, (Collection<?>) obj, false);
	}

	@Test
	public void readServiceDescription() throws SecurityException, NoSuchMethodException {
		TestData testData = oriWrapperTestBed.getServiceDescriptionTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		com.koch.ambeth.transfer.Assert.assertEquals((ServiceDescription) testData.obj,
				(ServiceDescription) obj);
	}

	@Test
	public void readCreatedEntity() {
		TestData testData = oriWrapperTestBed.getCreatedEntityTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		Assert.assertTrue(obj instanceof Material);
		assertEquals((Material) testData.obj, (Material) obj);
	}

	@Test
	public void readCreatedEntityCSXML() {
		TestData testData = oriWrapperTestBed.getCreatedEntityTestData();
		Object obj = cyclicXmlHandler.read(testData.xmlCS);
		Assert.assertTrue(obj instanceof Material);
		assertEquals((Material) testData.obj, (Material) obj);
	}

	@Test
	public void readCreatedChildEntity() {
		TestData testData = oriWrapperTestBed.getCreatedChildEntityTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		Assert.assertTrue(obj instanceof Material);
		assertEquals((Material) testData.obj, (Material) obj);
	}

	@Test
	public void readCreatedChildEntity2() {
		TestData testData = oriWrapperTestBed.getCreatedChildEntityTestData2();
		Object obj = cyclicXmlHandler.read(testData.xml);
		Assert.assertTrue(obj instanceof Material);
		assertEquals((Material) testData.obj, (Material) obj);
	}

	@Test
	public void readCreatedChildEntityCSXML() {
		TestData testData = oriWrapperTestBed.getCreatedChildEntityTestData();
		Object obj = cyclicXmlHandler.read(testData.xmlCS);
		Assert.assertTrue(obj instanceof Material);
		assertEquals((Material) testData.obj, (Material) obj);
	}

	@Test
	public void readCreatedChildEntityCSXML2() {
		TestData testData = oriWrapperTestBed.getCreatedChildEntityTestData2();
		Object obj = cyclicXmlHandler.read(testData.xmlCS);
		Assert.assertTrue(obj instanceof Material);
		assertEquals((Material) testData.obj, (Material) obj);
	}

	@Test
	public void readCreatedParentAndChildEntities() {
		TestData testData = oriWrapperTestBed.getCreatedParentAndChildEntitiesTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		Assert.assertTrue(obj instanceof Material);
		assertEquals((Material) testData.obj, (Material) obj);
	}

	@Test
	public void readCreatedParentAndChildEntities2() {
		TestData testData = oriWrapperTestBed.getCreatedParentAndChildEntitiesTestData2();
		Object obj = cyclicXmlHandler.read(testData.xml);
		Assert.assertTrue(obj instanceof Material);
		assertEquals((Material) testData.obj, (Material) obj);
	}

	@Test
	public void readCreatedParentAndChildEntitiesCSXML() {
		TestData testData = oriWrapperTestBed.getCreatedParentAndChildEntitiesTestData();
		Object obj = cyclicXmlHandler.read(testData.xmlCS);
		Assert.assertTrue(obj instanceof Material);
		assertEquals((Material) testData.obj, (Material) obj);
	}

	@Test
	public void readCreatedParentAndChildEntitiesCSXML2() {
		TestData testData = oriWrapperTestBed.getCreatedParentAndChildEntitiesTestData2();
		Object obj = cyclicXmlHandler.read(testData.xmlCS);
		Assert.assertTrue(obj instanceof Material);
		assertEquals((Material) testData.obj, (Material) obj);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void readCreatedParentAndChildEntitiesInList() {
		TestData testData = oriWrapperTestBed.getCreatedParentAndChildEntitiesInListTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		Assert.assertTrue(obj instanceof List);
		List<Object> actuals = (List<Object>) obj;
		assertEquals((Material) ((List<Object>) testData.obj).get(0), (Material) actuals.get(0));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void readCreatedParentAndChildEntitiesInListCSXML() {
		TestData testData = oriWrapperTestBed.getCreatedParentAndChildEntitiesInListTestData();
		Object obj = cyclicXmlHandler.read(testData.xmlCS);
		Assert.assertTrue(obj instanceof List);
		List<Object> actuals = (List<Object>) obj;
		assertEquals((Material) ((List<Object>) testData.obj).get(0), (Material) actuals.get(0));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void readCreatedParentAndChildEntitiesInListCSXML2() {
		TestData testData = oriWrapperTestBed.getCreatedParentAndChildEntitiesInListTestData2();
		Object obj = cyclicXmlHandler.read(testData.xmlCS);
		Assert.assertTrue(obj instanceof List);
		List<Object> actuals = (List<Object>) obj;
		assertEquals((Material) ((List<Object>) testData.obj).get(0), (Material) actuals.get(0));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void readCreatedParentAndChildEntitiesInList2() {
		TestData testData = oriWrapperTestBed.getCreatedParentAndChildEntitiesInListTestData2();
		Object obj = cyclicXmlHandler.read(testData.xml);
		Assert.assertTrue(obj instanceof List);
		List<Object> actuals = (List<Object>) obj;
		assertEquals((Material) ((List<Object>) testData.obj).get(0), (Material) actuals.get(0));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void readMultipleCreatedEntities() {
		TestData testData = oriWrapperTestBed.getMultipleCreatedEntitiesTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		List<Material> expecteds = (List<Material>) testData.obj;
		List<Material> actuals = (List<Material>) obj;
		Assert.assertEquals(expecteds.size(), actuals.size());
		for (int i = 0; i < expecteds.size(); i++) {
			Material expected = expecteds.get(i);
			Material actual = actuals.get(i);
			assertEquals(expected, actual);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void readMultipleCreatedEntities2() {
		TestData testData = oriWrapperTestBed.getMultipleCreatedEntitiesTestData2();
		Object obj = cyclicXmlHandler.read(testData.xml);
		List<Material> expecteds = (List<Material>) testData.obj;
		List<Material> actuals = (List<Material>) obj;
		Assert.assertEquals(expecteds.size(), actuals.size());
		for (int i = 0; i < expecteds.size(); i++) {
			Material expected = expecteds.get(i);
			Material actual = actuals.get(i);
			assertEquals(expected, actual);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void readMultipleCreatedEntitiesCSXML() {
		TestData testData = oriWrapperTestBed.getMultipleCreatedEntitiesTestData();
		Object obj = cyclicXmlHandler.read(testData.xmlCS);
		List<Material> expecteds = (List<Material>) testData.obj;
		List<Material> actuals = (List<Material>) obj;
		Assert.assertEquals(expecteds.size(), actuals.size());
		for (int i = 0; i < expecteds.size(); i++) {
			Material expected = expecteds.get(i);
			Material actual = actuals.get(i);
			assertEquals(expected, actual);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void readMultipleCreatedEntitiesCSXML2() {
		TestData testData = oriWrapperTestBed.getMultipleCreatedEntitiesTestData2();
		Object obj = cyclicXmlHandler.read(testData.xmlCS);
		List<Material> expecteds = (List<Material>) testData.obj;
		List<Material> actuals = (List<Material>) obj;
		Assert.assertEquals(expecteds.size(), actuals.size());
		for (int i = 0; i < expecteds.size(); i++) {
			Material expected = expecteds.get(i);
			Material actual = actuals.get(i);
			assertEquals(expected, actual);
		}
	}

	@Test
	public void readUpdatedEntity() {
		TestData testData = oriWrapperTestBed.getUpdatedEntityTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		Assert.assertEquals(testData.obj, obj);
		assertEquals((Material) testData.obj, (Material) obj);
	}

	@Test
	public void readBuidUpdatedEntity() {
		TestData testData = oriWrapperTestBed.getBuidUpdatedEntityTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		Assert.assertEquals(testData.obj, obj);
		assertEquals((Material) testData.obj, (Material) obj);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void readCreatedAndUpdatedEntities() {
		TestData testData = oriWrapperTestBed.getCreatedAndUpdatedEntitiesTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		List<Material> expecteds = (List<Material>) testData.obj;
		List<Material> actuals = (List<Material>) obj;
		Assert.assertEquals(expecteds.size(), actuals.size());
		for (int i = 0; i < expecteds.size(); i++) {
			Material expected = expecteds.get(i);
			Material actual = actuals.get(i);
			assertEquals(expected, actual);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void readCreatedAndUpdatedEntities2() {
		TestData testData = oriWrapperTestBed.getCreatedAndUpdatedEntitiesTestData2();
		Object obj = cyclicXmlHandler.read(testData.xml);
		List<Material> expecteds = (List<Material>) testData.obj;
		List<Material> actuals = (List<Material>) obj;
		Assert.assertEquals(expecteds.size(), actuals.size());
		for (int i = 0; i < expecteds.size(); i++) {
			Material expected = expecteds.get(i);
			Material actual = actuals.get(i);
			assertEquals(expected, actual);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void readCreatedAndUpdatedEntitiesCSXML() {
		TestData testData = oriWrapperTestBed.getCreatedAndUpdatedEntitiesTestData();
		Object obj = cyclicXmlHandler.read(testData.xmlCS);
		List<Material> expecteds = (List<Material>) testData.obj;
		List<Material> actuals = (List<Material>) obj;
		Assert.assertEquals(expecteds.size(), actuals.size());
		for (int i = 0; i < expecteds.size(); i++) {
			Material expected = expecteds.get(i);
			Material actual = actuals.get(i);
			assertEquals(expected, actual);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void readCreatedAndUpdatedEntitiesCSXML2() {
		TestData testData = oriWrapperTestBed.getCreatedAndUpdatedEntitiesTestData2();
		Object obj = cyclicXmlHandler.read(testData.xmlCS);
		List<Material> expecteds = (List<Material>) testData.obj;
		List<Material> actuals = (List<Material>) obj;
		Assert.assertEquals(expecteds.size(), actuals.size());
		for (int i = 0; i < expecteds.size(); i++) {
			Material expected = expecteds.get(i);
			Material actual = actuals.get(i);
			assertEquals(expected, actual);
		}
	}

	@Test
	public void readCreatedAndExistingChildren() {
		TestData testData = oriWrapperTestBed.getCreatedAndExistingChildrenTestData();
		Object obj = cyclicXmlHandler.read(testData.xml);
		assertEquals((EntityA) testData.obj, (EntityA) obj);
	}

	@Test
	public void readCreatedAndExistingChildren2() {
		TestData testData = oriWrapperTestBed.getCreatedAndExistingChildrenTestData2();
		Object obj = cyclicXmlHandler.read(testData.xml);
		assertEquals((EntityA) testData.obj, (EntityA) obj);
	}

	@Test
	public void readCreatedAndExistingChildrenCSXML() {
		TestData testData = oriWrapperTestBed.getCreatedAndExistingChildrenTestData();
		Object obj = cyclicXmlHandler.read(testData.xmlCS);
		assertEquals((EntityA) testData.obj, (EntityA) obj);
	}

	@Test
	public void readCreatedAndExistingChildrenCSXML2() {
		TestData testData = oriWrapperTestBed.getCreatedAndExistingChildrenTestData2();
		Object obj = cyclicXmlHandler.read(testData.xmlCS);
		assertEquals((EntityA) testData.obj, (EntityA) obj);
	}

	protected void assertCollectionEquals(Collection<?> expected, Collection<?> actual) {
		assertCollectionEquals(expected, actual, true);
	}

	protected void assertCollectionEquals(Collection<?> expected, Collection<?> actual,
			boolean inOrder) {
		Assert.assertEquals(expected.size(), actual.size());
		Iterator<?> expectedIter = expected.iterator();
		Iterator<?> actualIter = actual.iterator();
		int index = 0;
		while (expectedIter.hasNext()) {
			Object expectedEntry = expectedIter.next();
			if (inOrder) {
				Object actualEntry = actualIter.next();
				Assert.assertEquals("collection first differend at element " + index++, expectedEntry,
						actualEntry);
			}
			else {
				Assert.assertTrue(actual.contains(expectedEntry));
			}
		}
	}

	private void assertEquals(Material expected, Material actual) {
		Assert.assertEquals(expected.getBuid(), actual.getBuid());
		Assert.assertEquals(expected.getCreatedBy(), actual.getCreatedBy());
		Assert.assertEquals(expected.getCreatedOn(), actual.getCreatedOn());
		Assert.assertEquals(expected.getId(), actual.getId());
		Assert.assertEquals(expected.getName(), actual.getName());
		Assert.assertEquals(expected.getUpdatedBy(), actual.getUpdatedBy());
		Assert.assertEquals(expected.getUpdatedOn(), actual.getUpdatedOn());
		Assert.assertEquals(expected.getVersion(), actual.getVersion());

		assertEquals(expected.getMaterialGroup(), actual.getMaterialGroup());
		Assert.assertEquals(expected.getUnit(), actual.getUnit());
	}

	private void assertEquals(MaterialGroup expected, MaterialGroup actual) {
		if (expected == null) {
			Assert.assertNull(actual);
			return;
		}

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected.getBuid(), actual.getBuid());
		Assert.assertEquals(expected.getId(), actual.getId());
		Assert.assertEquals(expected.getName(), actual.getName());
		Assert.assertEquals(expected.getVersion(), actual.getVersion());
	}

	private void assertEquals(EntityA expected, EntityA actual) {
		if (expected == null) {
			Assert.assertNull(actual);
			return;
		}

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected.getId(), actual.getId());
		Assert.assertEquals(expected.getVersion(), actual.getVersion());
		Assert.assertEquals(expected.getEntityBs().size(), actual.getEntityBs().size());
		for (int i = 0; i < expected.getEntityBs().size(); i++) {
			assertEquals(expected.getEntityBs().get(i), actual.getEntityBs().get(i));
		}
	}

	private void assertEquals(EntityB expected, EntityB actual) {
		if (expected == null) {
			Assert.assertNull(actual);
			return;
		}

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected.getId(), actual.getId());
		Assert.assertEquals(expected.getVersion(), actual.getVersion());
	}
}
