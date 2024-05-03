/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.PageContext;

/**
 * 用于为 JSP 标签生成 '{@code id}' 属性值的实用工具类。给定标签的名称（在大多数情况下是数据绑定路径），
 * 返回当前 {@link PageContext} 中该名称的唯一 ID。对于给定名称的每个 ID 请求，将会在名称本身后附加一个不断增加的计数器。
 * 例如，给定名称 '{@code person.name}'，第一次请求将给出 '{@code person.name1}'，第二次请求将给出 '{@code person.name2}'。
 * 这支持一个常见的用例，即为同一数据字段生成一组单选按钮或复选按钮，每个按钮都是一个独特的标签实例。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
abstract class TagIdGenerator {

	/**
	 * 此标签创建的所有 {@link PageContext} 属性的前缀。
	 */
	private static final String PAGE_CONTEXT_ATTRIBUTE_PREFIX = TagIdGenerator.class.getName() + ".";

	/**
	 * 获取所提供名称的下一个唯一 ID（在给定的 {@link PageContext} 中）。
	 */
	public static String nextId(String name, PageContext pageContext) {
		// 构建属性名称，使用页面上下文属性前缀和给定名称。
		String attributeName = PAGE_CONTEXT_ATTRIBUTE_PREFIX + name;

		// 获取当前计数，如果存在则增加 1，否则设置为 1。
		Integer currentCount = (Integer) pageContext.getAttribute(attributeName);
		currentCount = (currentCount != null ? currentCount + 1 : 1);

		// 将更新后的计数值保存到页面上下文中。
		pageContext.setAttribute(attributeName, currentCount);

		// 返回带有计数的名称。
		return (name + currentCount);
	}

}
