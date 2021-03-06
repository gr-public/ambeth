package com.koch.ambeth.xml.simple;

/*-
 * #%L
 * jambeth-xml
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

import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.ioc.extendable.ClassExtendableContainer;
import com.koch.ambeth.ioc.extendable.MapExtendableContainer;
import com.koch.ambeth.merge.IProxyHelper;
import com.koch.ambeth.xml.ICyclicXmlController;
import com.koch.ambeth.xml.INameBasedHandler;
import com.koch.ambeth.xml.INameBasedHandlerExtendable;
import com.koch.ambeth.xml.IReader;
import com.koch.ambeth.xml.ITypeBasedHandler;
import com.koch.ambeth.xml.ITypeBasedHandlerExtendable;
import com.koch.ambeth.xml.IWriter;

public class SimpleXmlController
		implements ICyclicXmlController, ITypeBasedHandlerExtendable, INameBasedHandlerExtendable {
	protected final ClassExtendableContainer<ITypeBasedHandler> typeToElementHandlers =
			new ClassExtendableContainer<>("elementHandler", "type");

	protected final MapExtendableContainer<String, INameBasedHandler> nameToElementHandlers =
			new MapExtendableContainer<>("elementHandler", "name");

	@Autowired
	protected IProxyHelper proxyHelper;

	@Override
	public Object readObject(IReader reader) {
		return readObject(Object.class, reader);
	}

	@Override
	public Object readObject(Class<?> returnType, IReader reader) {
		String elementName = reader.getElementName();
		INameBasedHandler nameBasedHandler = nameToElementHandlers.getExtension(elementName);
		if (nameBasedHandler == null) {
			throw new IllegalArgumentException("Could not read object: " + elementName);
		}
		return nameBasedHandler.readObject(returnType, elementName, 0, reader);
	}

	@Override
	public void writeObject(Object obj, IWriter writer) {
		Class<?> type = proxyHelper.getRealType(obj.getClass());
		ITypeBasedHandler extension = typeToElementHandlers.getExtension(type);
		if (extension == null) {
			throw new IllegalArgumentException("Could not write object: " + obj);
		}
		extension.writeObject(obj, type, writer);
	}

	@Override
	public void registerElementHandler(ITypeBasedHandler elementHandler, Class<?> type) {
		typeToElementHandlers.register(elementHandler, type);
	}

	@Override
	public void unregisterElementHandler(ITypeBasedHandler elementHandler, Class<?> type) {
		typeToElementHandlers.unregister(elementHandler, type);
	}

	@Override
	public void registerNameBasedElementHandler(INameBasedHandler nameBasedElementHandler,
			String elementName) {
		nameToElementHandlers.register(nameBasedElementHandler, elementName);
	}

	@Override
	public void unregisterNameBasedElementHandler(INameBasedHandler nameBasedElementHandler,
			String elementName) {
		nameToElementHandlers.unregister(nameBasedElementHandler, elementName);
	}
}
