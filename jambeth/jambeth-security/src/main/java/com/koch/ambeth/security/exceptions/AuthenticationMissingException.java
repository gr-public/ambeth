package com.koch.ambeth.security.exceptions;

/*-
 * #%L
 * jambeth-security-server
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

import java.lang.reflect.Method;

public class AuthenticationMissingException extends SecurityException {
	private static final long serialVersionUID = -1163535828252169857L;

	public AuthenticationMissingException() {
		super("No authentication handle found");
	}

	public AuthenticationMissingException(Method method) {
		super(
				"No authentication handle found - Requested to access '" + method.toGenericString() + "'");
	}
}