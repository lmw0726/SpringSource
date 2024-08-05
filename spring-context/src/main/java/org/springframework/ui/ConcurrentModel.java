/*
 * Copyright 2002-2021 the original author or authors.
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 {@link ConcurrentHashMap} 的 {@link Model} 接口实现，用于并发场景。
 *
 * <p>由 Spring WebFlux 暴露给处理方法，通常通过声明 {@link Model} 接口来使用。在用户代码中通常无需创建它。
 * 如果需要，处理方法可以返回一个常规的 {@code java.util.Map}，可能是一个 {@code java.util.ConcurrentMap}，用于预定模型。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class ConcurrentModel extends ConcurrentHashMap<String, Object> implements Model {

	/**
	 * 构造一个新的空 {@code ConcurrentModel}。
	 */
	public ConcurrentModel() {
	}

	/**
	 * 构造一个新的 {@code ModelMap}，包含指定名称的属性。
	 *
	 * @see #addAttribute(String, Object)
	 */
	public ConcurrentModel(String attributeName, Object attributeValue) {
		addAttribute(attributeName, attributeValue);
	}

	/**
	 * 构造一个新的 {@code ModelMap}，包含指定的属性。
	 * 使用属性名称生成来为提供的模型对象生成键。
	 *
	 * @see #addAttribute(Object)
	 */
	public ConcurrentModel(Object attributeValue) {
		addAttribute(attributeValue);
	}


	@Override
	@Nullable
	public Object put(String key, @Nullable Object value) {
		if (value != null) {
			return super.put(key, value);
		} else {
			return remove(key);
		}
	}

	@Override
	public void putAll(Map<? extends String, ?> map) {
		for (Map.Entry<? extends String, ?> entry : map.entrySet()) {
			// 遍历映射，将将其键值添加到当前的Map中。
			put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * 通过指定名称添加提供的属性。
	 *
	 * @param attributeName  模型属性的名称（绝不能为 {@code null}）
	 * @param attributeValue 模型属性的值（如果为 {@code null} 则忽略，仅删除任何现有条目）
	 */
	@Override
	public ConcurrentModel addAttribute(String attributeName, @Nullable Object attributeValue) {
		Assert.notNull(attributeName, "Model attribute name must not be null");
		put(attributeName, attributeValue);
		return this;
	}

	/**
	 * 使用 {@link org.springframework.core.Conventions#getVariableName 生成的名称} 将提供的属性添加到 {@code Map} 中。
	 * <p><i>注意：使用此方法时，空的 {@link Collection 集合} 不会添加到模型中，因为我们不能正确确定
	 * 真实的约定名称。视图代码应该检查 {@code null}，而不是空集合，正如 JSTL 标签已经做的那样。</i>
	 *
	 * @param attributeValue 模型属性的值（绝不能为 {@code null}）
	 */
	@Override
	public ConcurrentModel addAttribute(Object attributeValue) {
		Assert.notNull(attributeValue, "Model attribute value must not be null");
		if (attributeValue instanceof Collection && ((Collection<?>) attributeValue).isEmpty()) {
			// 如果属性值是集合类型，并且该集合为空，则返回当前的并发模型。
			return this;
		}
		// 获取属性名称后，将其和属性值添加进属性中。
		return addAttribute(Conventions.getVariableName(attributeValue), attributeValue);
	}

	/**
	 * 将提供的 {@code Collection} 中的所有属性复制到此 {@code Map} 中，为每个元素使用属性名称生成。
	 *
	 * @see #addAttribute(Object)
	 */
	@Override
	public ConcurrentModel addAllAttributes(@Nullable Collection<?> attributeValues) {
		if (attributeValues != null) {
			for (Object attributeValue : attributeValues) {
				// 循环添加属性
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
	@Override
	public ConcurrentModel addAllAttributes(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			putAll(attributes);
		}
		return this;
	}

	/**
	 * 将提供的 {@code Map} 中的所有属性复制到此 {@code Map} 中，
	 * 已存在的同名对象优先（即不会被替换）。
	 */
	@Override
	public ConcurrentModel mergeAttributes(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			attributes.forEach((key, value) -> {
				if (!containsKey(key)) {
					// 如果不含有该键，则将键值添加进当前的的 ConcurrentHashMap 中
					put(key, value);
				}
			});
		}
		return this;
	}

	/**
	 * 此模型是否包含具有给定名称的属性？
	 *
	 * @param attributeName 模型属性的名称（绝不能为 {@code null}）
	 * @return 此模型是否包含对应的属性
	 */
	@Override
	public boolean containsAttribute(String attributeName) {
		return containsKey(attributeName);
	}

	@Override
	@Nullable
	public Object getAttribute(String attributeName) {
		return get(attributeName);
	}

	@Override
	public Map<String, Object> asMap() {
		return this;
	}

}
