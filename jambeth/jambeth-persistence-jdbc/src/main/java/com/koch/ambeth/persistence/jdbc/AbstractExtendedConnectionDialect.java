package com.koch.ambeth.persistence.jdbc;

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

import java.sql.Blob;
import java.sql.Clob;

import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.persistence.IExtendedConnectionDialect;
import com.koch.ambeth.persistence.jdbc.lob.BlobInputSource;
import com.koch.ambeth.persistence.jdbc.lob.ClobInputSource;
import com.koch.ambeth.persistence.jdbc.lob.ILobInputSourceController;
import com.koch.ambeth.stream.binary.IBinaryInputSource;
import com.koch.ambeth.stream.chars.ICharacterInputSource;

public class AbstractExtendedConnectionDialect implements IExtendedConnectionDialect {
	@Autowired
	protected ILobInputSourceController lobInputSourceController;

	@Override
	public IBinaryInputSource createBinaryInputSource(Blob blob) {
		return new BlobInputSource(lobInputSourceController);
	}

	@Override
	public ICharacterInputSource createCharacterInputSource(Clob clob) {
		return new ClobInputSource(lobInputSourceController);
	}
}
