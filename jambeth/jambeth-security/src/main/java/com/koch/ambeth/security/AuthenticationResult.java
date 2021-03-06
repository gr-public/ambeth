package com.koch.ambeth.security;

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


public class AuthenticationResult implements IAuthenticationResult {
	private final String sid;

	private final boolean changePasswordRecommended, changePasswordRequired;

	private final boolean rehashPasswordRecommended, accountingActive;

	public AuthenticationResult(String sid, boolean changePasswordRecommended,
			boolean changePasswordRequired, boolean rehashPasswordRecommended, boolean accountingActive) {
		this.sid = sid;
		this.changePasswordRecommended = changePasswordRecommended;
		this.changePasswordRequired = changePasswordRequired;
		this.rehashPasswordRecommended = rehashPasswordRecommended;
		this.accountingActive = accountingActive;
	}

	@Override
	public String getSID() {
		return sid;
	}

	@Override
	public boolean isChangePasswordRecommended() {
		return changePasswordRecommended;
	}

	@Override
	public boolean isChangePasswordRequired() {
		return changePasswordRequired;
	}

	@Override
	public boolean isRehashPasswordRecommended() {
		return rehashPasswordRecommended;
	}

	@Override
	public boolean isAccountingActive() {
		return accountingActive;
	}
}
