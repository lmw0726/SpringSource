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

package org.springframework.core.io.support;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Locale;

/**
 * 用于加载本地化资源的辅助类，
 * 通过名称、扩展名和当前区域(locale)指定资源。
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 */
public class LocalizedResourceHelper {

	/** 文件名部分之间使用的默认分隔符：下划线。 */
	public static final String DEFAULT_SEPARATOR = "_";


	private final ResourceLoader resourceLoader;

	private String separator = DEFAULT_SEPARATOR;


	/**
	 * 使用 DefaultResourceLoader 创建一个新的 LocalizedResourceHelper。
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public LocalizedResourceHelper() {
		this.resourceLoader = new DefaultResourceLoader();
	}

	/**
	 * 使用指定的 ResourceLoader 创建一个新的 LocalizedResourceHelper。
	 * @param resourceLoader 要使用的 ResourceLoader
	 */
	public LocalizedResourceHelper(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 设置文件名部分之间使用的分隔符。
	 * 默认为下划线 ("_")。
	 */
	public void setSeparator(@Nullable String separator) {
		this.separator = (separator != null ? separator : DEFAULT_SEPARATOR);
	}


	/**
	 * 查找给定名称、扩展名和区域的最具体的本地化资源：
	 * <p>文件将按以下顺序查找位置，
	 * 类似于 {@code java.util.ResourceBundle} 的搜索顺序：
	 * <ul>
	 * <li>[name]_[language]_[country]_[variant][extension]
	 * <li>[name]_[language]_[country][extension]
	 * <li>[name]_[language][extension]
	 * <li>[name][extension]
	 * </ul>
	 * <p>如果没有找到具体文件，则返回默认位置的资源描述符。
	 * @param name 文件名，不含本地化部分和扩展名
	 * @param extension 文件扩展名（例如 ".xls"）
	 * @param locale 当前区域（可以为 {@code null}）
	 * @return 找到的最具体的本地化资源
	 * @see java.util.ResourceBundle
	 */
	public Resource findLocalizedResource(String name, String extension, @Nullable Locale locale) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(extension, "Extension must not be null");

		Resource resource = null;

		if (locale != null) {
			String lang = locale.getLanguage();
			String country = locale.getCountry();
			String variant = locale.getVariant();

			// 检查带语言、国家和变体的本地化文件。
			if (variant.length() > 0) {
				String location =
						name + this.separator + lang + this.separator + country + this.separator + variant + extension;
				resource = this.resourceLoader.getResource(location);
			}

			// 检查带语言和国家的本地化文件。
			if ((resource == null || !resource.exists()) && country.length() > 0) {
				String location = name + this.separator + lang + this.separator + country + extension;
				resource = this.resourceLoader.getResource(location);
			}

			// 检查带语言的本地化文件。
			if ((resource == null || !resource.exists()) && lang.length() > 0) {
				String location = name + this.separator + lang + extension;
				resource = this.resourceLoader.getResource(location);
			}
		}

		// 检查无本地化的文件。
		if (resource == null || !resource.exists()) {
			String location = name + extension;
			resource = this.resourceLoader.getResource(location);
		}

		return resource;
	}

}
