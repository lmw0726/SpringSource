/*
 * Copyright 2002-2022 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * 用于基于 Web 的区域设置解析策略的接口，允许通过请求解析区域设置以及通过请求和响应修改区域设置。
 *
 * <p>此接口允许基于请求、会话、Cookie 等的实现。默认实现是
 * {@link org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver}，
 * 简单地使用相应 HTTP 标头提供的请求区域设置。
 *
 * <p>使用 {@link org.springframework.web.servlet.support.RequestContext#getLocale()}，
 * 在控制器或视图中检索当前区域设置，独立于实际的解析策略。
 *
 * <p>注意：从 Spring 4.0 开始，还有一个扩展策略接口称为 {@link LocaleContextResolver}，
 * 允许解析 {@link org.springframework.context.i18n.LocaleContext} 对象，可能包含关联的时区信息。
 * Spring 提供的解析器实现在适当的地方实现了扩展的 {@link LocaleContextResolver} 接口。
 *
 * @author Juergen Hoeller
 * @see LocaleContextResolver
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @see org.springframework.web.servlet.support.RequestContext#getLocale
 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
 * @since 27.02.2003
 */
public interface LocaleResolver {

	/**
	 * 通过给定的请求解析当前区域设置。
	 * <p>在任何情况下都可以返回默认区域设置作为后备。
	 *
	 * @param request 要解析区域设置的请求
	 * @return 当前区域设置（永远不会为 {@code null}）
	 */
	Locale resolveLocale(HttpServletRequest request);

	/**
	 * 将当前区域设置设置为给定的区域设置。
	 *
	 * @param request  用于区域设置修改的请求
	 * @param response 用于区域设置修改的响应
	 * @param locale   新的区域设置，或 {@code null} 以清除区域设置
	 * @throws UnsupportedOperationException 如果 LocaleResolver 实现不支持动态更改区域设置
	 */
	void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale);

}
