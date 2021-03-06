package com.koch.ambeth.query.jdbc.sql;

/*-
 * #%L
 * jambeth-query-jdbc
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

import java.util.Map;

import com.koch.ambeth.query.IMultiValueOperand;
import com.koch.ambeth.query.IOperand;
import com.koch.ambeth.query.IValueOperand;
import com.koch.ambeth.util.appendable.IAppendable;
import com.koch.ambeth.util.collections.EmptyList;
import com.koch.ambeth.util.collections.IList;
import com.koch.ambeth.util.collections.IMap;

public class NullValueOperand implements IOperand, IValueOperand, IMultiValueOperand {
	public static final NullValueOperand INSTANCE = new NullValueOperand();

	@Override
	public boolean isNull(Map<Object, Object> nameToValueMap) {
		return true;
	}

	@Override
	public boolean isNullOrEmpty(Map<Object, Object> nameToValueMap) {
		return true;
	}

	@Override
	public Object getValue(Map<Object, Object> nameToValueMap) {
		return null;
	}

	@Override
	public IList<Object> getMultiValue(Map<Object, Object> nameToValueMap) {
		return EmptyList.createTypedEmptyList(Object.class);
	}

	@Override
	public void expandQuery(IAppendable querySB, IMap<Object, Object> nameToValueMap,
			boolean joinQuery, IList<Object> parameters) {
		querySB.append("NULL");
	}
}
