package com.koch.ambeth.stream.binary;

/*-
 * #%L
 * jambeth-stream
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

import java.io.ByteArrayInputStream;

public class ReusableByteArrayInputStream extends ByteArrayInputStream {
	public ReusableByteArrayInputStream(byte[] buf) {
		super(buf);
	}

	public ReusableByteArrayInputStream(byte[] buf, int offset, int length) {
		super(buf, offset, length);
	}

	public void reset(byte[] buf) {
		mark = 0;
		pos = 0;
		this.buf = buf;
		count = buf.length;
	}
}
