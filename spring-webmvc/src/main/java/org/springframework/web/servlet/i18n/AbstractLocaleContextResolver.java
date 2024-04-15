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

package org.springframework.web.servlet.i18n;

import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.LocaleContextResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.TimeZone;

/**
 * {@link LocaleContextResolver} 实现的抽象基类。
 *
 * <p>提供对 {@linkplain #setDefaultLocale(Locale) 默认区域设置} 和 {@linkplain #setDefaultTimeZone(TimeZone) 默认时区} 的支持。
 *
 * <p>还提供了 {@link #resolveLocale} 和 {@link #setLocale} 的预实现版本，
 * 委托给 {@link #resolveLocaleContext} 和 {@link #setLocaleContext}。
 *
 * @author Juergen Hoeller
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 * @since 4.0
 */
public abstract class AbstractLocaleContextResolver extends AbstractLocaleResolver implements LocaleContextResolver {
	/**
	 * 默认时区
	 */
	@Nullable
	private TimeZone defaultTimeZone;


	/**
	 * 设置默认 {@link TimeZone}，如果没有找到其他时区，则此解析器将返回默认时区。
	 */
	public void setDefaultTimeZone(@Nullable TimeZone defaultTimeZone) {
		this.defaultTimeZone = defaultTimeZone;
	}

	/**
	 * 获取默认 {@link TimeZone}，如果有的话，此解析器应该回退到该时区。
	 */
	@Nullable
	public TimeZone getDefaultTimeZone() {
		return this.defaultTimeZone;
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		// 解析区域设置上下文，并获取区域设置
		Locale locale = resolveLocaleContext(request).getLocale();
		// 如果区域设置不为空，则使用区域设置，否则使用请求中的区域设置
		return (locale != null ? locale : request.getLocale());
	}

	@Override
	public void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale) {
		setLocaleContext(request, response, (locale != null ? new SimpleLocaleContext(locale) : null));
	}

}
