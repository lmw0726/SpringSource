/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import org.springframework.web.servlet.view.UrlBasedViewResolver;

import java.util.Map;

/**
 * 辅助配置 {@link org.springframework.web.servlet.view.UrlBasedViewResolver}。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class UrlBasedViewResolverRegistration {

	/**
	 * URL基础视图解析器
	 */
	protected final UrlBasedViewResolver viewResolver;


	public UrlBasedViewResolverRegistration(UrlBasedViewResolver viewResolver) {
		this.viewResolver = viewResolver;
	}


	protected UrlBasedViewResolver getViewResolver() {
		return this.viewResolver;
	}

	/**
	 * 设置在构建 URL 时添加到视图名称前的前缀。
	 *
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setPrefix
	 */
	public UrlBasedViewResolverRegistration prefix(String prefix) {
		this.viewResolver.setPrefix(prefix);
		return this;
	}

	/**
	 * 设置在构建 URL 时添加到视图名称后的后缀。
	 *
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setSuffix
	 */
	public UrlBasedViewResolverRegistration suffix(String suffix) {
		this.viewResolver.setSuffix(suffix);
		return this;
	}

	/**
	 * 设置用于创建视图的视图类。
	 *
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setViewClass
	 */
	public UrlBasedViewResolverRegistration viewClass(Class<?> viewClass) {
		this.viewResolver.setViewClass(viewClass);
		return this;
	}

	/**
	 * 设置此视图解析器可以处理的视图名称（或名称模式）。
	 * 视图名称可以包含简单的通配符，例如 'my*'、'*Report' 和 '*Repo*' 都将匹配视图名称 'myReport'。
	 *
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setViewNames
	 */
	public UrlBasedViewResolverRegistration viewNames(String... viewNames) {
		this.viewResolver.setViewNames(viewNames);
		return this;
	}

	/**
	 * 设置要添加到此视图解析器解析的所有请求的模型中的静态属性。
	 * 这允许设置任何类型的属性值，例如 bean 引用。
	 *
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setAttributesMap
	 */
	public UrlBasedViewResolverRegistration attributes(Map<String, ?> attributes) {
		this.viewResolver.setAttributesMap(attributes);
		return this;
	}

	/**
	 * 指定视图缓存的最大条目数。
	 * 默认值是 1024。
	 *
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setCacheLimit
	 */
	public UrlBasedViewResolverRegistration cacheLimit(int cacheLimit) {
		this.viewResolver.setCacheLimit(cacheLimit);
		return this;
	}

	/**
	 * 启用或禁用缓存。
	 * <p>这相当于将 {@link #cacheLimit "cacheLimit"} 属性分别设置为默认限制（1024）或 0。
	 * <p>默认值是 "true"：启用缓存。
	 * 仅在调试和开发时禁用此功能。
	 *
	 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setCache
	 */
	public UrlBasedViewResolverRegistration cache(boolean cache) {
		this.viewResolver.setCache(cache);
		return this;
	}

}


