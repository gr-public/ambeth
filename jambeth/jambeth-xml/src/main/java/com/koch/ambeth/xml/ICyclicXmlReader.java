package com.koch.ambeth.xml;

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

import java.io.InputStream;
import java.io.Reader;
import java.nio.channels.ReadableByteChannel;

import com.koch.ambeth.util.config.IProperties;

public interface ICyclicXmlReader {
	public static final String SKIP_CLASS_NOT_FOUND = "ambeth.xml.read.skip_class_not_found";

	Object read(String cyclicXmlContent);

	Object readFromStream(InputStream is);

	Object readFromStream(InputStream is, String encoding);

	Object readFromChannel(ReadableByteChannel byteChannel);

	Object readFromReader(Reader reader);

	ICyclicXmlReader createReaderWith(IProperties props);
}
