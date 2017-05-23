package com.koch.ambeth.util.appendable;

/*-
 * #%L
 * jambeth-util
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
import java.io.Writer;

import com.koch.ambeth.util.exception.RuntimeExceptionUtil;
import com.koch.ambeth.util.io.IntegerUtil;

public class WriterAppendable extends IntegerUtil implements IAppendable {
	protected final Writer target;

	public WriterAppendable(Writer target) {
		this.target = target;
	}

	@Override
	public IAppendable append(char value) {
		try {
			target.append(value);
		}
		catch (IOException e) {
			throw RuntimeExceptionUtil.mask(e);
		}
		return this;
	}

	@Override
	public IAppendable append(CharSequence value) {
		try {
			target.append(value);
		}
		catch (IOException e) {
			throw RuntimeExceptionUtil.mask(e);
		}
		return this;
	}

	@Override
	public IAppendable append(char[] value) {
		try {
			for (char oneChar : value) {
				target.append(oneChar);
			}
		}
		catch (IOException e) {
			throw RuntimeExceptionUtil.mask(e);
		}
		return this;
	}

	@Override
	public IAppendable append(char[] value, int offset, int length) {
		try {
			for (int a = offset, size = offset + length; a < size; a++) {
				target.append(value[a]);
			}
		}
		catch (IOException e) {
			throw RuntimeExceptionUtil.mask(e);
		}
		return this;
	}

	@Override
	public IAppendable appendInt(int intValue) {
		appendInt(intValue, this);
		return this;
	}
}
