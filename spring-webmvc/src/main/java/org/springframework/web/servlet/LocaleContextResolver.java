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

import org.springframework.context.i18n.LocaleContext;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * {@link LocaleResolver} 的扩展，增加了对丰富的区域设置上下文（可能包括区域设置和时区信息）的支持。
 *
 * @author Juergen Hoeller
 * @see org.springframework.context.i18n.LocaleContext
 * @see org.springframework.context.i18n.TimeZoneAwareLocaleContext
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @see org.springframework.web.servlet.support.RequestContext#getTimeZone
 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
 * @since 4.0
 */
public interface LocaleContextResolver extends LocaleResolver {

	/**
	 * 通过给定的请求解析当前区域设置上下文。
	 * <p>这主要用于框架级处理；考虑使用 {@link org.springframework.web.servlet.support.RequestContextUtils} 或
	 * {@link org.springframework.web.servlet.support.RequestContext} 来访问当前区域设置和/或时区的应用级别。
	 * <p>返回的上下文可能是一个 {@link org.springframework.context.i18n.TimeZoneAwareLocaleContext}，
	 * 包含有关联的时区信息的区域设置。简单地应用 {@code instanceof} 检查并据此向下转换。
	 * <p>自定义解析器实现也可以在返回的上下文中返回额外的设置，这些设置可以通过向下转换来访问。
	 *
	 * @param request 要解析区域设置上下文的请求
	 * @return 当前区域设置上下文（永远不会为 {@code null}）
	 * @see #resolveLocale(HttpServletRequest)
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
	 */
	LocaleContext resolveLocaleContext(HttpServletRequest request);

	/**
	 * 将当前区域设置上下文设置为给定的区域设置上下文，
	 * 可能包括一个带有关联时区信息的区域设置。
	 *
	 * @param request       用于区域设置修改的请求
	 * @param response      用于区域设置修改的响应
	 * @param localeContext 新的区域设置上下文，或 {@code null} 以清除区域设置
	 * @throws UnsupportedOperationException 如果 LocaleResolver 实现不支持动态更改区域设置或时区
	 * @see #setLocale(HttpServletRequest, HttpServletResponse, Locale)
	 * @see org.springframework.context.i18n.SimpleLocaleContext
	 * @see org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext
	 */
	void setLocaleContext(HttpServletRequest request, @Nullable HttpServletResponse response,
						  @Nullable LocaleContext localeContext);

}
