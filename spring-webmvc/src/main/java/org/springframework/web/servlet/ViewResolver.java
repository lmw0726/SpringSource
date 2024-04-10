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

package org.springframework.web.servlet;

import org.springframework.lang.Nullable;

import java.util.Locale;

/**
 * 由能够按名称解析视图的对象实现的接口。
 *
 * <p>在应用程序运行期间，视图状态不会发生变化，因此实现可以自由缓存视图。
 *
 * <p>鼓励实现支持国际化，即本地化视图解析。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.view.InternalResourceViewResolver
 * @see org.springframework.web.servlet.view.ContentNegotiatingViewResolver
 * @see org.springframework.web.servlet.view.BeanNameViewResolver
 */
public interface ViewResolver {

	/**
	 * 按名称解析给定的视图。
	 * <p>注意：为了允许视图解析器链接，如果未在其中定义具有给定名称的视图，则视图解析器应返回 {@code null}。
	 * 但是，这不是必需的：某些视图解析器将始终尝试使用给定名称构建视图对象，无法返回 {@code null}
	 * （在视图创建失败时抛出异常）。
	 * @param viewName 要解析的视图的名称
	 * @param locale 要解析视图的语言环境。支持国际化的 ViewResolvers 应该遵守此规定。
	 * @return 视图对象，如果未找到则为 {@code null}
	 * (可选，以允许视图解析器链接)
	 * @throws Exception 如果无法解析视图（通常在创建实际视图对象时出现问题）
	 */
	@Nullable
	View resolveViewName(String viewName, Locale locale) throws Exception;

}
