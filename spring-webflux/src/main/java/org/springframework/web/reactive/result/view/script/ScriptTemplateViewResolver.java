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

package org.springframework.web.reactive.result.view.script;

import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.reactive.result.view.UrlBasedViewResolver;

/**
 * {@link UrlBasedViewResolver} 的便利子类，支持 {@link ScriptTemplateView} 及其自定义子类。
 *
 * <p>通过 {@link #setViewClass} 属性可以指定此解析器创建的所有视图的视图类。
 *
 * <p><b>注意：</b>在链接 ViewResolvers 时，此解析器将检查指定的模板资源的存在，
 * 只有在实际找到模板时才会返回非空的 View 对象。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see ScriptTemplateConfigurer
 */
public class ScriptTemplateViewResolver extends UrlBasedViewResolver {

	/**
	 * 将默认 {@link #setViewClass 视图类} 设置为 {@link #requiredViewClass}：
	 * 默认为 {@link ScriptTemplateView}。
	 */
	public ScriptTemplateViewResolver() {
		setViewClass(requiredViewClass());
	}

	/**
	 * 一个便利的构造函数，允许指定 {@link #setPrefix 前缀} 和 {@link #setSuffix 后缀} 作为构造函数参数。
	 *
	 * @param prefix 构建 URL 时附加到视图名称前面的前缀
	 * @param suffix 构建 URL 时附加到视图名称后面的后缀
	 */
	public ScriptTemplateViewResolver(String prefix, String suffix) {
		this();
		setPrefix(prefix);
		setSuffix(suffix);
	}

	@Override
	protected Class<?> requiredViewClass() {
		return ScriptTemplateView.class;
	}

	@Override
	protected AbstractUrlBasedView instantiateView() {
		return (getViewClass() == ScriptTemplateView.class ? new ScriptTemplateView() : super.instantiateView());
	}

}
