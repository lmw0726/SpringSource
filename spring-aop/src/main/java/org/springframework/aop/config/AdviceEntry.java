/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.aop.config;

import org.springframework.beans.factory.parsing.ParseState;

/**
 * 表示通知元素的 {@link ParseState} 条目。
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class AdviceEntry implements ParseState.Entry {

	private final String kind;


	/**
	 * 创建一个新的 {@code AdviceEntry} 实例。
	 * @param kind 此条目表示的通知类型（before、after、around）
	 */
	public AdviceEntry(String kind) {
		this.kind = kind;
	}


	@Override
	public String toString() {
		return "Advice (" + this.kind + ")";
	}

}
