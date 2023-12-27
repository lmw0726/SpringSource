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

package org.springframework.web.reactive.result.view.script;

import org.springframework.context.ApplicationContext;

import java.util.Locale;
import java.util.function.Function;

/**
 * 传递给 {@link ScriptTemplateView} 渲染函数的上下文，以便在脚本端提供应用程序上下文、区域设置、模板加载器和 URL。
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
	 * 语言环境
	 */
	private final Locale locale;

	/**
	 * 模板加载器函数
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
	 * @param locale             渲染模板的区域设置
	 * @param templateLoader     一个接受模板路径作为输入并返回模板内容的函数（字符串形式）
	 * @param url                渲染模板的 URL
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
	 * 返回一个函数，该函数接受模板路径作为输入，并将模板内容作为字符串返回。
	 */
	public Function<String, String> getTemplateLoader() {
		return this.templateLoader;
	}

	/**
	 * 返回渲染模板的 URL。
	 */
	public String getUrl() {
		return this.url;
	}

}
