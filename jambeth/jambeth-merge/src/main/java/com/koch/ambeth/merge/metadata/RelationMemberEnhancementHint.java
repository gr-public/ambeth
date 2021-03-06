package com.koch.ambeth.merge.metadata;

/*-
 * #%L
 * jambeth-merge
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

import org.objectweb.asm.Type;

import com.koch.ambeth.ioc.bytecode.IEnhancementHint;
import com.koch.ambeth.service.metadata.RelationMember;

public class RelationMemberEnhancementHint extends MemberEnhancementHint {
	public RelationMemberEnhancementHint(Class<?> entityType, String memberName) {
		super(entityType, memberName);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IEnhancementHint> T unwrap(Class<T> includedHintType) {
		T hint = super.unwrap(includedHintType);
		if (hint != null) {
			return hint;
		}
		if (RelationMemberEnhancementHint.class.equals(includedHintType)) {
			return (T) this;
		}
		return null;
	}

	@Override
	public String getTargetName(Class<?> typeToEnhance) {
		return Type.getInternalName(declaringType) + "$" + RelationMember.class.getSimpleName() + "$"
				+ memberName;
	}
}
