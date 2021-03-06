package com.koch.ambeth.cache.imc;

/*-
 * #%L
 * jambeth-cache
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

import com.koch.ambeth.service.merge.model.IObjRef;

public interface IInMemoryConfig {
	IInMemoryConfig primitive(String memberName, Object value);

	IInMemoryConfig relation(String memberName, IObjRef... objRefs);

	IInMemoryConfig addRelation(String memberName, IObjRef... objRefs);

	IInMemoryConfig relation(String memberName, IInMemoryConfig... inMemoryConfigs);

	IInMemoryConfig addRelation(String memberName, IInMemoryConfig... inMemoryConfigs);
}
