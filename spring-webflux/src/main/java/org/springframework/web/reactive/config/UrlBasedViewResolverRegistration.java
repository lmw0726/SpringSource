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

package org.springframework.web.reactive.config;

import org.springframework.util.Assert;
import org.springframework.web.reactive.result.view.UrlBasedViewResolver;

/**
 * 辅助配置 {@link UrlBasedViewResolver} 的属性。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class UrlBasedViewResolverRegistration {
	/**
	 * URL视图解析器
	 */
	private final UrlBasedViewResolver viewResolver;

	public UrlBasedViewResolverRegistration(UrlBasedViewResolver viewResolver) {
		Assert.notNull(viewResolver, "ViewResolver must not be null");
		this.viewResolver = viewResolver;
	}

	/**
	 * 设置构建 URL 时添加到视图名称前面的前缀。
	 *
	 * @see UrlBasedViewResolver#setPrefix
	 */
	public UrlBasedViewResolverRegistration prefix(String prefix) {
		this.viewResolver.setPrefix(prefix);
		return this;
	}

	/**
	 * 设置构建 URL 时添加到视图名称后面的后缀。
	 *
	 * @see UrlBasedViewResolver#setSuffix
	 */
	public UrlBasedViewResolverRegistration suffix(String suffix) {
		this.viewResolver.setSuffix(suffix);
		return this;
	}

	/**
	 * 设置用于创建视图的视图类。
	 *
	 * @see UrlBasedViewResolver#setViewClass
	 */
	public UrlBasedViewResolverRegistration viewClass(Class<?> viewClass) {
		this.viewResolver.setViewClass(viewClass);
		return this;
	}

	/**
	 * 设置可以由此视图解析器处理的视图名称（或名称模式）。视图名称可以包含简单通配符，例如 'my*'、'*Report' 和 '*Repo*'，它们都匹配视图名称 'myReport'。
	 *
	 * @see UrlBasedViewResolver#setViewNames
	 */
	public UrlBasedViewResolverRegistration viewNames(String... viewNames) {
		this.viewResolver.setViewNames(viewNames);
		return this;
	}

	/**
	 * 获取视图解析器
	 *
	 * @return 视图解析器
	 */
	protected UrlBasedViewResolver getViewResolver() {
		return this.viewResolver;
	}
}
