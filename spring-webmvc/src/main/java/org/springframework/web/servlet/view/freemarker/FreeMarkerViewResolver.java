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

package org.springframework.web.servlet.view.freemarker;

import org.springframework.web.servlet.view.AbstractTemplateViewResolver;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * {@link org.springframework.web.servlet.view.UrlBasedViewResolver} 的便利子类，
 * 支持 {@link FreeMarkerView}（即 FreeMarker 模板）和其自定义子类。
 *
 * <p>通过 "viewClass" 属性可以指定此解析器生成的所有视图的视图类。有关详细信息，请参阅 UrlBasedViewResolver 的 javadoc。
 *
 * <p><b>注意:</b> 在链接 ViewResolver 时，FreeMarkerViewResolver 将检查指定模板资源的存在，
 * 只有在实际找到模板时才会返回非空的 View 对象。
 *
 * @author Juergen Hoeller
 * @see #setViewClass
 * @see #setPrefix
 * @see #setSuffix
 * @see #setRequestContextAttribute
 * @see #setExposeSpringMacroHelpers
 * @see FreeMarkerView
 * @since 1.1
 */
public class FreeMarkerViewResolver extends AbstractTemplateViewResolver {

	/**
	 * 将默认 {@link #setViewClass 视图类} 设置为 {@link #requiredViewClass}：
	 * 默认为 {@link FreeMarkerView}。
	 */
	public FreeMarkerViewResolver() {
		setViewClass(requiredViewClass());
	}

	/**
	 * 一个方便的构造函数，允许指定 {@link #setPrefix 前缀} 和 {@link #setSuffix 后缀} 作为构造参数。
	 *
	 * @param prefix 构建 URL 时要添加到视图名称前面的前缀
	 * @param suffix 构建 URL 时要添加到视图名称后面的后缀
	 * @since 4.3
	 */
	public FreeMarkerViewResolver(String prefix, String suffix) {
		this();
		setPrefix(prefix);
		setSuffix(suffix);
	}


	/**
	 * 需要 {@link FreeMarkerView}。
	 */
	@Override
	protected Class<?> requiredViewClass() {
		return FreeMarkerView.class;
	}

	@Override
	protected AbstractUrlBasedView instantiateView() {
		return (getViewClass() == FreeMarkerView.class ? new FreeMarkerView() : super.instantiateView());
	}

}
