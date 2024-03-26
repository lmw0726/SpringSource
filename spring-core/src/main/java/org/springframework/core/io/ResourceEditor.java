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

package org.springframework.core.io;

import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

/**
 * {@link java.beans.PropertyEditor Editor} 用于{@link Resource}描述符，以自动将{@code String}位置
 * (例如{@code file:C:/myfile.txt}或{@code classpath:myfile.txt})转换为{@code Resource}属性，
 * 而不是使用{@code String}位置属性。
 *
 * <p>路径可能包含{@code ${...}}占位符，要作为{@link org.springframework.core.env.Environment}属性解析：
 * 例如 {@code ${user.dir}}。 默认情况下，无法解析的占位符将被忽略。
 *
 * <p>默认情况下，委托给{@link ResourceLoader}来执行繁重的工作，使用的是{@link DefaultResourceLoader}。
 *
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Chris Beams
 * @see Resource
 * @see ResourceLoader
 * @see DefaultResourceLoader
 * @see PropertyResolver#resolvePlaceholders
 * @since 28.12.2003
 */
public class ResourceEditor extends PropertyEditorSupport {

	/**
	 * 资源加载器
	 */
	private final ResourceLoader resourceLoader;
	/**
	 * 属性解析器
	 */
	@Nullable
	private PropertyResolver propertyResolver;

	/**
	 * 是否忽略无法解析的占位符
	 */
	private final boolean ignoreUnresolvablePlaceholders;


	/**
	 * 使用{@link DefaultResourceLoader}和{@link StandardEnvironment}创建{@link ResourceEditor}类的新实例。
	 */
	public ResourceEditor() {
		this(new DefaultResourceLoader(), null);
	}

	/**
	 * 使用给定的{@link ResourceLoader}和{@link PropertyResolver}创建{@link ResourceEditor}类的新实例。
	 *
	 * @param resourceLoader   要使用的{@code ResourceLoader}
	 * @param propertyResolver 要使用的{@code PropertyResolver}
	 */
	public ResourceEditor(ResourceLoader resourceLoader, @Nullable PropertyResolver propertyResolver) {
		this(resourceLoader, propertyResolver, true);
	}

	/**
	 * 使用给定的{@link ResourceLoader}创建{@link ResourceEditor}类的新实例。
	 *
	 * @param resourceLoader                 要使用的{@code ResourceLoader}
	 * @param propertyResolver               要使用的{@code PropertyResolver}
	 * @param ignoreUnresolvablePlaceholders 是否忽略无法解析的占位符，如果在给定的{@code propertyResolver}中找不到对应的属性
	 */
	public ResourceEditor(ResourceLoader resourceLoader, @Nullable PropertyResolver propertyResolver,
						  boolean ignoreUnresolvablePlaceholders) {

		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
		this.propertyResolver = propertyResolver;
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}


	@Override
	public void setAsText(String text) {
		// 如果文本不为空
		if (StringUtils.hasText(text)) {
			// 解析路径并去除首尾空白字符
			String locationToUse = resolvePath(text).trim();
			// 设置属性值为解析后的资源
			setValue(this.resourceLoader.getResource(locationToUse));
		} else {
			// 如果文本为空，则将属性值设为 null
			setValue(null);
		}
	}

	/**
	 * 解析给定的路径，必要时使用{@code environment}中的对应属性值替换占位符。
	 *
	 * @param path 原始文件路径
	 * @return 解析后的文件路径
	 * @see PropertyResolver#resolvePlaceholders
	 * @see PropertyResolver#resolveRequiredPlaceholders
	 */
	protected String resolvePath(String path) {
		if (this.propertyResolver == null) {
			// 如果属性解析器为空，则创建一个标准环境的属性解析器
			this.propertyResolver = new StandardEnvironment();
		}
		// 返回解析后的属性值，根据是否忽略未解析的占位符来决定使用 resolvePlaceholders
		// 还是 resolveRequiredPlaceholders 方法
		return (this.ignoreUnresolvablePlaceholders ? this.propertyResolver.resolvePlaceholders(path) :
				this.propertyResolver.resolveRequiredPlaceholders(path));
	}


	@Override
	@Nullable
	public String getAsText() {
		// 获取属性值并尝试获取资源的 URL 地址
		Resource value = (Resource) getValue();
		try {
			// 尝试获取资源的 URL 地址并转换为外部字符串形式
			return (value != null ? value.getURL().toExternalForm() : "");
		} catch (IOException ex) {
			// 如果无法确定资源的 URL 地址，则返回 null 表示没有适当的文本表示
			return null;
		}
	}

}
