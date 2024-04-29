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

package org.springframework.web.servlet.view.script;

import org.springframework.context.ApplicationContext;

import java.util.Locale;
import java.util.function.Function;

/**
 * 渲染函数传递给 {@link ScriptTemplateView} 的上下文，以便在脚本端提供应用程序上下文、区域设置、模板加载器和URL。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class RenderingContext {

	/**
	 * 应用程序上下文
	 */
	private final ApplicationContext applicationContext;

	/**
	 * 区域设置
	 */
	private final Locale locale;

	/**
	 * 模板加载器
	 */
	private final Function<String, String> templateLoader;

	/**
	 * URL
	 */
	private final String url;


	/**
	 * 创建一个新的 {@code RenderingContext}。
	 *
	 * @param applicationContext 应用程序上下文
	 * @param locale 渲染模板的区域设置
	 * @param templateLoader 一个函数，接受模板路径作为输入，并将模板内容作为字符串返回
	 * @param url 渲染模板的URL
	 */
	public RenderingContext(ApplicationContext applicationContext, Locale locale,
							Function<String, String> templateLoader, String url) {

		this.applicationContext = applicationContext;
		this.locale = locale;
		this.templateLoader = templateLoader;
		this.url = url;
	}


	/**
	 * 返回应用程序上下文。
	 */
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * 返回渲染模板的区域设置。
	 */
	public Locale getLocale() {
		return this.locale;
	}

	/**
	 * 返回一个函数，接受模板路径作为输入，并将模板内容作为字符串返回。
	 */
	public Function<String, String> getTemplateLoader() {
		return this.templateLoader;
	}

	/**
	 * 返回渲染模板的URL。
	 */
	public String getUrl() {
		return this.url;
	}

}
