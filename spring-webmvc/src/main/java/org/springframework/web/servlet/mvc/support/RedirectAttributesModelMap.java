/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.mvc.support;

import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;
import org.springframework.validation.DataBinder;

import java.util.Collection;
import java.util.Map;

/**
 * {@link ModelMap} 的 {@link RedirectAttributes} 实现，使用 {@link DataBinder} 将值格式化为字符串。
 * 同时提供了一个存储 Flash 属性的位置，以便它们可以在重定向时幸存，而不需要嵌入重定向 URL。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@SuppressWarnings("serial")
public class RedirectAttributesModelMap extends ModelMap implements RedirectAttributes {

	/**
	 * 数据绑定
	 */
	@Nullable
	private final DataBinder dataBinder;

	/**
	 * 闪存属性映射
	 */
	private final ModelMap flashAttributes = new ModelMap();


	/**
	 * 默认构造函数，不带 DataBinder。
	 * 属性值通过 {@link #toString()} 转换为字符串。
	 */
	public RedirectAttributesModelMap() {
		this(null);
	}

	/**
	 * 构造函数，带 DataBinder。
	 *
	 * @param dataBinder 用于将属性值格式化为字符串
	 */
	public RedirectAttributesModelMap(@Nullable DataBinder dataBinder) {
		this.dataBinder = dataBinder;
	}


	/**
	 * 返回用于 Flash 存储的属性候选项或空 Map。
	 */
	@Override
	public Map<String, ?> getFlashAttributes() {
		return this.flashAttributes;
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加之前将属性值格式化为字符串。
	 */
	@Override
	public RedirectAttributesModelMap addAttribute(String attributeName, @Nullable Object attributeValue) {
		super.addAttribute(attributeName, formatValue(attributeValue));
		return this;
	}

	@Nullable
	private String formatValue(@Nullable Object value) {
		// 如果值为 null，则直接返回 null
		if (value == null) {
			return null;
		}
		// 如果数据绑定器不为 null，则使用数据绑定器将值转换为字符串；
		// 否则直接调用值的 toString 方法
		return (this.dataBinder != null ? this.dataBinder.convertIfNecessary(value, String.class) : value.toString());
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加之前将属性值格式化为字符串。
	 */
	@Override
	public RedirectAttributesModelMap addAttribute(Object attributeValue) {
		super.addAttribute(attributeValue);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加之前将每个属性值格式化为字符串。
	 */
	@Override
	public RedirectAttributesModelMap addAllAttributes(@Nullable Collection<?> attributeValues) {
		super.addAllAttributes(attributeValues);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加之前将每个属性值格式化为字符串。
	 */
	@Override
	public RedirectAttributesModelMap addAllAttributes(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			// 循环调用添加属性的方法
			attributes.forEach(this::addAttribute);
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>在合并之前将每个属性值格式化为字符串。
	 */
	@Override
	public RedirectAttributesModelMap mergeAttributes(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			// 如果属性不为空
			// 遍历每对映射
			attributes.forEach((key, attribute) -> {
				if (!containsKey(key)) {
					// 如果不包含key，则添加键和属性
					addAttribute(key, attribute);
				}
			});
		}
		return this;
	}

	@Override
	public Map<String, Object> asMap() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加之前将属性值格式化为字符串。
	 */
	@Override
	public Object put(String key, @Nullable Object value) {
		return super.put(key, formatValue(value));
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加之前将每个属性值格式化为字符串。
	 */
	@Override
	public void putAll(@Nullable Map<? extends String, ? extends Object> map) {
		if (map != null) {
			map.forEach((key, value) -> put(key, formatValue(value)));
		}
	}

	@Override
	public RedirectAttributes addFlashAttribute(String attributeName, @Nullable Object attributeValue) {
		this.flashAttributes.addAttribute(attributeName, attributeValue);
		return this;
	}

	@Override
	public RedirectAttributes addFlashAttribute(Object attributeValue) {
		this.flashAttributes.addAttribute(attributeValue);
		return this;
	}

}
