/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * 定义模型属性持有者的接口。
 *
 * <p>主要用于向模型中添加属性。
 *
 * <p>允许将整体模型作为 {@code java.util.Map} 进行访问。
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 */
public interface Model {

	/**
	 * 通过指定名称添加提供的属性。
	 *
	 * @param attributeName  模型属性的名称（绝不能为 {@code null}）
	 * @param attributeValue 模型属性的值（可以为 {@code null}）
	 */
	Model addAttribute(String attributeName, @Nullable Object attributeValue);

	/**
	 * 使用 {@link org.springframework.core.Conventions#getVariableName 生成的名称} 将提供的属性添加到 {@code Map} 中。
	 * <p><i>注意：使用此方法时，空的 {@link java.util.Collection 集合} 不会添加到模型中，因为我们不能正确确定
	 * 真实的约定名称。视图代码应该检查 {@code null}，而不是空集合，正如 JSTL 标签已经做的那样。</i>
	 *
	 * @param attributeValue 模型属性的值（绝不能为 {@code null}）
	 */
	Model addAttribute(Object attributeValue);

	/**
	 * 将提供的 {@code Collection} 中的所有属性复制到此 {@code Map} 中，为每个元素使用属性名称生成。
	 *
	 * @see #addAttribute(Object)
	 */
	Model addAllAttributes(Collection<?> attributeValues);

	/**
	 * 将提供的 {@code Map} 中的所有属性复制到此 {@code Map} 中。
	 *
	 * @see #addAttribute(String, Object)
	 */
	Model addAllAttributes(Map<String, ?> attributes);

	/**
	 * 将提供的 {@code Map} 中的所有属性复制到此 {@code Map} 中，
	 * 已存在的同名对象优先（即不会被替换）。
	 */
	Model mergeAttributes(Map<String, ?> attributes);

	/**
	 * 此模型是否包含具有给定名称的属性？
	 *
	 * @param attributeName 模型属性的名称（绝不能为 {@code null}）
	 * @return 此模型是否包含对应的属性
	 */
	boolean containsAttribute(String attributeName);

	/**
	 * 返回具有给定名称的属性值（如果有的话）。
	 *
	 * @param attributeName 模型属性的名称（绝不能为 {@code null}）
	 * @return 对应的属性值，如果没有则返回 {@code null}
	 * @since 5.2
	 */
	@Nullable
	Object getAttribute(String attributeName);

	/**
	 * 将当前的模型属性集作为 Map 返回。
	 */
	Map<String, Object> asMap();

}
