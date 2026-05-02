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
import org.springframework.util.StringUtils;

/**
 * 表示切面的 {@link ParseState} 条目。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.0
 */
public class AspectEntry implements ParseState.Entry {

	private final String id;

	private final String ref;


	/**
	 * 创建一个新的 {@code AspectEntry} 实例。
	 * @param id 切面元素的 id
	 * @param ref 此切面元素引用的 bean 名称
	 */
	public AspectEntry(String id, String ref) {
		this.id = id;
		this.ref = ref;
	}


	@Override
	public String toString() {
		return "Aspect: " + (StringUtils.hasLength(this.id) ? "id='" + this.id + "'" : "ref='" + this.ref + "'");
	}

}
