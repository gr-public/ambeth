package com.koch.ambeth.security.privilege.transfer;

/*-
 * #%L
 * jambeth-security
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
import com.koch.ambeth.service.model.ISecurityScope;
import com.koch.ambeth.util.annotation.XmlType;
import com.koch.ambeth.util.transfer.IDTOType;

@XmlType
public interface IPrivilegeOfService extends IDTOType {
	IObjRef getReference();

	ISecurityScope getSecurityScope();

	boolean isCreateAllowed();

	boolean isReadAllowed();

	boolean isUpdateAllowed();

	boolean isDeleteAllowed();

	boolean isExecuteAllowed();

	String[] getPropertyPrivilegeNames();

	IPropertyPrivilegeOfService[] getPropertyPrivileges();
}
