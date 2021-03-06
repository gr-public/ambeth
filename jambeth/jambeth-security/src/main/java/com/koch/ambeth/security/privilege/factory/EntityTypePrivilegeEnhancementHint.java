package com.koch.ambeth.security.privilege.factory;

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

import java.io.Serializable;

import org.objectweb.asm.Type;

import com.koch.ambeth.ioc.bytecode.IEnhancementHint;
import com.koch.ambeth.ioc.bytecode.ITargetNameEnhancementHint;
import com.koch.ambeth.security.privilege.model.impl.AbstractPrivilege;
import com.koch.ambeth.security.privilege.model.impl.AbstractTypePrivilege;

public class EntityTypePrivilegeEnhancementHint
		implements IEnhancementHint, ITargetNameEnhancementHint, Serializable {
	private static final long serialVersionUID = -6647489741758504317L;

	protected final Class<?> entityType;

	protected final Boolean create, read, update, delete, execute;

	public EntityTypePrivilegeEnhancementHint(Class<?> entityType, Boolean create, Boolean read,
			Boolean update, Boolean delete, Boolean execute) {
		this.entityType = entityType;
		this.create = create;
		this.read = read;
		this.update = update;
		this.delete = delete;
		this.execute = execute;
	}

	public Class<?> getEntityType() {
		return entityType;
	}

	public Boolean isCreate() {
		return create;
	}

	public Boolean isRead() {
		return read;
	}

	public Boolean isUpdate() {
		return update;
	}

	public Boolean isDelete() {
		return delete;
	}

	public Boolean isExecute() {
		return execute;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof EntityTypePrivilegeEnhancementHint)) {
			return false;
		}
		EntityTypePrivilegeEnhancementHint other = (EntityTypePrivilegeEnhancementHint) obj;
		return getEntityType().equals(other.getEntityType()) && isCreate() == other.isCreate()
				&& isRead() == other.isRead() && isUpdate() == other.isUpdate()
				&& isDelete() == other.isDelete() && isExecute() == other.isExecute();
	}

	@Override
	public int hashCode() {
		return getClass().hashCode() ^ getEntityType().hashCode()
				^ AbstractTypePrivilege.calcIndex(create, read, update, delete, execute);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IEnhancementHint> T unwrap(Class<T> includedHintType) {
		if (EntityTypePrivilegeEnhancementHint.class.isAssignableFrom(includedHintType)) {
			return (T) this;
		}
		return null;
	}

	@Override
	public String getTargetName(Class<?> typeToEnhance) {
		return Type.getInternalName(entityType) + "$" + AbstractTypePrivilege.class.getSimpleName()
				+ "_" + AbstractPrivilege.upperOrLower(create, 'c')
				+ AbstractPrivilege.upperOrLower(read, 'r') + AbstractPrivilege.upperOrLower(update, 'u')
				+ AbstractPrivilege.upperOrLower(delete, 'd')
				+ AbstractPrivilege.upperOrLower(execute, 'e');
	}
}
