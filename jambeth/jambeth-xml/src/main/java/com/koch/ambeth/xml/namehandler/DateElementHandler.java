package com.koch.ambeth.xml.namehandler;

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

import java.util.Date;

import com.koch.ambeth.xml.INameBasedHandler;
import com.koch.ambeth.xml.IReader;
import com.koch.ambeth.xml.IWriter;
import com.koch.ambeth.xml.typehandler.AbstractHandler;

public class DateElementHandler extends AbstractHandler implements INameBasedHandler {
	@Override
	public boolean writesCustom(Object obj, Class<?> type, IWriter writer) {
		if (!Date.class.isAssignableFrom(type)) {
			return false;
		}
		int id = writer.acquireIdForObject(obj);
		long time = ((Date) obj).getTime();
		writer.writeStartElement("d");
		writer.writeAttribute(xmlDictionary.getIdAttribute(), Integer.toString(id));
		writer.writeAttribute(xmlDictionary.getValueAttribute(), Long.toString(time));
		writer.writeEndElement();
		return true;
	}

	@Override
	public Object readObject(Class<?> returnType, String elementName, int id, IReader reader) {
		if (!"d".equals(elementName)) {
			throw new IllegalStateException("Element '" + elementName + "' not supported");
		}
		String timeString = reader.getAttributeValue(xmlDictionary.getValueAttribute());
		return new Date(Long.parseLong(timeString));
	}
}
