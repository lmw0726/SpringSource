/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.ui;

import org.springframework.core.Conventions;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用于构建用于UI工具的模型数据的 {@link java.util.Map} 实现。
 * 支持链式调用和模型属性名的生成。
 *
 * <p>该类作为Servlet MVC的通用模型持有者，但不局限于它。
 * 查看 {@link Model} 接口以获取接口变体。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see Conventions#getVariableName
 * @see org.springframework.web.servlet.ModelAndView
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ModelMap extends LinkedHashMap<String, Object> {

	/**
	 * 构造一个新的空的 {@code ModelMap}。
	 */
	public ModelMap() {
	}

	/**
	 * 构造一个包含提供的属性并以提供的名称命名的新的 {@code ModelMap}。
	 *
	 * @see #addAttribute(String, Object)
	 */
	public ModelMap(String attributeName, @Nullable Object attributeValue) {
		addAttribute(attributeName, attributeValue);
	}

	/**
	 * 构造一个包含提供的属性的新的 {@code ModelMap}。
	 * 使用属性名生成来生成提供的模型对象的键。
	 *
	 * @see #addAttribute(Object)
	 */
	public ModelMap(Object attributeValue) {
		addAttribute(attributeValue);
	}


	/**
	 * 将提供的属性添加到提供的名称下。
	 *
	 * @param attributeName  模型属性的名称 (不能为 {@code null})
	 * @param attributeValue 模型属性值 (可以为 {@code null})
	 */
	public ModelMap addAttribute(String attributeName, @Nullable Object attributeValue) {
		Assert.notNull(attributeName, "Model attribute name must not be null");
		put(attributeName, attributeValue);
		return this;
	}

	/**
	 * 使用 {@link org.springframework.core.Conventions#getVariableName 生成的名称} 将提供的属性添加到此 {@code Map}。
	 * <p><i>注意：使用此方法时，空的 {@link Collection 集合} 不会添加到模型中，
	 * 因为我们无法正确确定真实的约定名称。视图代码应检查 {@code null} 而不是空集合，
	 * 这已由 JSTL 标签完成。</i>
	 *
	 * @param attributeValue 模型属性值 (不能为 {@code null})
	 */
	public ModelMap addAttribute(Object attributeValue) {
		Assert.notNull(attributeValue, "Model object must not be null");
		if (attributeValue instanceof Collection && ((Collection<?>) attributeValue).isEmpty()) {
			return this;
		}
		return addAttribute(Conventions.getVariableName(attributeValue), attributeValue);
	}

	/**
	 * 使用属性名生成将提供的 {@code Collection} 中的所有属性复制到此 {@code Map} 中。
	 *
	 * @see #addAttribute(Object)
	 */
	public ModelMap addAllAttributes(@Nullable Collection<?> attributeValues) {
		if (attributeValues != null) {
			for (Object attributeValue : attributeValues) {
				addAttribute(attributeValue);
			}
		}
		return this;
	}

	/**
	 * 将提供的 {@code Map} 中的所有属性复制到此 {@code Map} 中。
	 *
	 * @see #addAttribute(String, Object)
	 */
	public ModelMap addAllAttributes(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			putAll(attributes);
		}
		return this;
	}

	/**
	 * 将提供的 {@code Map} 中的所有属性复制到此 {@code Map} 中，
	 * 具有相同名称的现有对象优先（即不会被替换）。
	 */
	public ModelMap mergeAttributes(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			attributes.forEach((key, value) -> {
				if (!containsKey(key)) {
					put(key, value);
				}
			});
		}
		return this;
	}

	/**
	 * 这个模型是否包含给定名称的属性？
	 *
	 * @param attributeName 模型属性的名称 (不能为 {@code null})
	 * @return 该模型是否包含对应的属性
	 */
	public boolean containsAttribute(String attributeName) {
		return containsKey(attributeName);
	}

	/**
	 * 返回给定名称的属性值（如果有）。
	 *
	 * @param attributeName 模型属性的名称 (不能为 {@code null})
	 * @return 对应的属性值，如果没有则返回 {@code null}
	 * @since 5.2
	 */
	@Nullable
	public Object getAttribute(String attributeName) {
		return get(attributeName);
	}

}
