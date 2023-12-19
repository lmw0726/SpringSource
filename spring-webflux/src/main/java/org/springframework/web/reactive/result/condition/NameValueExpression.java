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

package org.springframework.web.reactive.result.condition;

import org.springframework.lang.Nullable;
/**
 * 用于指定在 {@code @RequestMapping} 中的请求参数和请求头条件的 {@code "name!=value"} 样式表达式的约定。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> 值的类型
 */
public interface NameValueExpression<T> {
	/**
	 * 获取表达式中的名称。
	 *
	 * @return 表达式的名称
	 */
	String getName();

	/**
	 * 获取表达式中的值，如果没有值则返回 {@code null}。
	 *
	 * @return 表达式的值，可能为 {@code null}
	 */
	@Nullable
	T getValue();

	/**
	 * 检查表达式是否是否定的。
	 *
	 * @return 如果表达式是否定，则为 true；否则为 false
	 */
	boolean isNegated();

}
