package com.koch.ambeth.persistence.jdbc.lob;

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

import java.io.IOException;

import com.koch.ambeth.stream.IInputStream;
import com.koch.ambeth.stream.binary.IBinaryInputStream;

public class EmptyBlobInputStream implements IBinaryInputStream, IInputStream {
	@Override
	public int readByte() {
		return -1;
	}

	@Override
	public void close() throws IOException {
		// Intended blank
	}
}
