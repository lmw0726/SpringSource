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

package org.springframework.web.servlet.view.script;

import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * 支持 {@link ScriptTemplateView}（即脚本模板视图）及其自定义子类的 {@link UrlBasedViewResolver} 的便利子类。
 *
 * <p>通过 {@link #setViewClass(Class)} 属性可以指定此解析器创建的所有视图的视图类。
 *
 * <p><b>注意：</b>当链接视图解析器时，此解析器将检查指定的模板资源是否存在，只有在实际找到模板时才会返回非空的视图对象。
 *
 * @author Sebastien Deleuze
 * @see ScriptTemplateConfigurer
 * @since 4.2
 */
public class ScriptTemplateViewResolver extends UrlBasedViewResolver {

	/**
	 * 将默认的 {@link #setViewClass 视图类} 设置为 {@link #requiredViewClass}：
	 * 默认为 {@link ScriptTemplateView}。
	 */
	public ScriptTemplateViewResolver() {
		setViewClass(requiredViewClass());
	}

	/**
	 * 一个方便的构造函数，允许将 {@link #setPrefix 前缀} 和 {@link #setSuffix 后缀} 作为构造函数参数进行指定。
	 *
	 * @param prefix 构建 URL 时要添加到视图名称前面的前缀
	 * @param suffix 构建 URL 时要添加到视图名称后面的后缀
	 * @since 4.3
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
