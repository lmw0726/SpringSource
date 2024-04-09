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

package org.springframework.web.servlet.view.groovy;

import org.springframework.web.servlet.view.AbstractTemplateViewResolver;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

import java.util.Locale;

/**
 * 方便的 {@link AbstractTemplateViewResolver} 子类，支持 {@link GroovyMarkupView}
 * （即 Groovy XML/XHTML 标记模板）和其自定义子类。
 *
 * <p>通过 {@link #setViewClass(Class)} 可以指定此解析器创建的所有视图的视图类。
 *
 * <p><b>注意:</b> 当链接 ViewResolvers 时，此解析器将检查指定模板资源的存在，
 * 仅当实际找到模板时才返回非空的 {@code View} 对象。
 *
 * @author Brian Clozel
 * @since 4.1
 * @see GroovyMarkupConfigurer
 */
public class GroovyMarkupViewResolver extends AbstractTemplateViewResolver {

	/**
	 * 将默认的 {@link #setViewClass view 类} 设置为 {@link #requiredViewClass}：
	 * 默认为 {@link GroovyMarkupView}。
	 */
	public GroovyMarkupViewResolver() {
		setViewClass(requiredViewClass());
	}

	/**
	 * 一个方便的构造函数，允许指定 {@link #setPrefix 前缀} 和 {@link #setSuffix 后缀} 作为构造参数。
	 * @param prefix 构建 URL 时要添加到视图名称之前的前缀
	 * @param suffix 构建 URL 时要添加到视图名称之后的后缀
	 * @since 4.3
	 */
	public GroovyMarkupViewResolver(String prefix, String suffix) {
		this();
		setPrefix(prefix);
		setSuffix(suffix);
	}


	@Override
	protected Class<?> requiredViewClass() {
		return GroovyMarkupView.class;
	}

	@Override
	protected AbstractUrlBasedView instantiateView() {
		return (getViewClass() == GroovyMarkupView.class ? new GroovyMarkupView() : super.instantiateView());
	}

	/**
	 * 此解析器支持国际化，因此缓存键应包含区域设置信息。
	 */
	@Override
	protected Object getCacheKey(String viewName, Locale locale) {
		return viewName + '_' + locale;
	}

}
