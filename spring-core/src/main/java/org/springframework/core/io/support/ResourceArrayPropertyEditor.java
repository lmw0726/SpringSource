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

package org.springframework.core.io.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.util.*;

/**
 * 用于 {@link org.springframework.core.io.Resource} 数组的编辑器，
 * 自动将 {@code String} 位置模式（例如 {@code "file:C:/my*.txt"} 或 {@code "classpath*:myfile.txt"}）
 * 转换为 {@code Resource} 数组属性。也可以将一组位置模式的集合或数组
 * 转换为合并后的 Resource 数组。
 *
 * <p>路径中可以包含 {@code ${...}} 占位符，
 * 会被解析为 {@link org.springframework.core.env.Environment} 属性，例如 {@code ${user.dir}}。
 * 默认情况下，无法解析的占位符会被忽略。
 *
 * <p>委托给 {@link ResourcePatternResolver} 进行解析，
 * 默认使用 {@link PathMatchingResourcePatternResolver}。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 1.1.2
 * @see org.springframework.core.io.Resource
 * @see ResourcePatternResolver
 * @see PathMatchingResourcePatternResolver
 */
public class ResourceArrayPropertyEditor extends PropertyEditorSupport {

	private static final Log logger = LogFactory.getLog(ResourceArrayPropertyEditor.class);

	private final ResourcePatternResolver resourcePatternResolver;

	@Nullable
	private PropertyResolver propertyResolver;

	private final boolean ignoreUnresolvablePlaceholders;


	/**
	 * 使用默认的 {@link PathMatchingResourcePatternResolver} 和 {@link StandardEnvironment}
	 * 创建一个新的 ResourceArrayPropertyEditor。
	 * @see PathMatchingResourcePatternResolver
	 * @see Environment
	 */
	public ResourceArrayPropertyEditor() {
		this(new PathMatchingResourcePatternResolver(), null, true);
	}

	/**
	 * 使用给定的 {@link ResourcePatternResolver} 和 {@link PropertyResolver}（通常是 {@link Environment}）
	 * 创建一个新的 ResourceArrayPropertyEditor。
	 * @param resourcePatternResolver 要使用的 ResourcePatternResolver
	 * @param propertyResolver 要使用的 PropertyResolver
	 */
	public ResourceArrayPropertyEditor(
			ResourcePatternResolver resourcePatternResolver, @Nullable PropertyResolver propertyResolver) {

		this(resourcePatternResolver, propertyResolver, true);
	}

	/**
	 * 使用给定的 {@link ResourcePatternResolver} 和 {@link PropertyResolver}（通常是 {@link Environment}）
	 * 创建一个新的 ResourceArrayPropertyEditor。
	 * @param resourcePatternResolver 要使用的 ResourcePatternResolver
	 * @param propertyResolver 要使用的 PropertyResolver
	 * @param ignoreUnresolvablePlaceholders 是否忽略无法解析的占位符（当找不到对应的系统属性时）
	 */
	public ResourceArrayPropertyEditor(ResourcePatternResolver resourcePatternResolver,
			@Nullable PropertyResolver propertyResolver, boolean ignoreUnresolvablePlaceholders) {

		Assert.notNull(resourcePatternResolver, "ResourcePatternResolver must not be null");
		this.resourcePatternResolver = resourcePatternResolver;
		this.propertyResolver = propertyResolver;
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}


	/**
	 * 将给定文本视为位置模式并转换为 Resource 数组。
	 */
	@Override
	public void setAsText(String text) {
		String pattern = resolvePath(text).trim();
		try {
			setValue(this.resourcePatternResolver.getResources(pattern));
		}
		catch (IOException ex) {
			throw new IllegalArgumentException(
					"Could not resolve resource location pattern [" + pattern + "]: " + ex.getMessage());
		}
	}

	/**
	 * 将给定值视为集合或数组并转换为 Resource 数组。
	 * 字符串元素被视为位置模式，Resource 元素则直接使用。
	 */
	@Override
	public void setValue(Object value) throws IllegalArgumentException {
		if (value instanceof Collection || (value instanceof Object[] && !(value instanceof Resource[]))) {
			Collection<?> input = (value instanceof Collection ? (Collection<?>) value : Arrays.asList((Object[]) value));
			Set<Resource> merged = new LinkedHashSet<>();
			for (Object element : input) {
				if (element instanceof String) {
					// 位置模式：解析为 Resource 数组。
					// 可能指向单个资源或多个资源。
					String pattern = resolvePath((String) element).trim();
					try {
						Resource[] resources = this.resourcePatternResolver.getResources(pattern);
						Collections.addAll(merged, resources);
					}
					catch (IOException ex) {
						// 忽略 - 可能是未解析的占位符或不存在的基础目录
						if (logger.isDebugEnabled()) {
							logger.debug("Could not retrieve resources for pattern '" + pattern + "'", ex);
						}
					}
				}
				else if (element instanceof Resource) {
					// Resource 对象：添加到结果中。
					merged.add((Resource) element);
				}
				else {
					throw new IllegalArgumentException("Cannot convert element [" + element + "] to [" +
							Resource.class.getName() + "]: only location String and Resource object supported");
				}
			}
			super.setValue(merged.toArray(new Resource[0]));
		}

		else {
			// 任意值：可能是字符串或 Resource 数组。
			// 字符串会调用 setAsText；Resource 数组则原样使用。
			super.setValue(value);
		}
	}

	/**
	 * 解析给定路径，必要时替换占位符为对应的系统属性值。
	 * @param path 原始文件路径
	 * @return 解析后的文件路径
	 * @see PropertyResolver#resolvePlaceholders
	 * @see PropertyResolver#resolveRequiredPlaceholders(String)
	 */
	protected String resolvePath(String path) {
		if (this.propertyResolver == null) {
			this.propertyResolver = new StandardEnvironment();
		}
		return (this.ignoreUnresolvablePlaceholders ? this.propertyResolver.resolvePlaceholders(path) :
				this.propertyResolver.resolveRequiredPlaceholders(path));
	}

}
