/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.index.processor;

import java.util.HashSet;
import java.util.Set;

/**
 * 表示索引中的一个条目。类型定义了目标候选者的标识（通常是全限定名），
 * 而定型（stereotypes）是可用于检索候选者的"标记"。
 * 一个典型的用例是候选者上存在给定注解。
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
class ItemMetadata {

	private final String type;

	private final Set<String> stereotypes;


	public ItemMetadata(String type, Set<String> stereotypes) {
		this.type = type;
		this.stereotypes = new HashSet<>(stereotypes);
	}


	public String getType() {
		return this.type;
	}

	public Set<String> getStereotypes() {
		return this.stereotypes;
	}

}
