/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.TimeZone;

/**
 * {@link org.springframework.web.servlet.LocaleResolver} 的实现，始终返回固定的默认区域设置和可选的时区。
 * 默认情况下为当前 JVM 的默认区域设置。
 *
 * <p>注意：不支持 {@code setLocale(Context)}，因为无法更改固定的区域设置和时区。
 *
 * @author Juergen Hoeller
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 * @since 1.1
 */
public class FixedLocaleResolver extends AbstractLocaleContextResolver {

	/**
	 * 创建一个默认的 FixedLocaleResolver，暴露一个配置的默认区域设置（或 JVM 的默认区域设置作为后备）。
	 *
	 * @see #setDefaultLocale
	 * @see #setDefaultTimeZone
	 */
	public FixedLocaleResolver() {
		setDefaultLocale(Locale.getDefault());
	}

	/**
	 * 创建一个 FixedLocaleResolver，暴露给定的区域设置。
	 *
	 * @param locale 要暴露的区域设置
	 */
	public FixedLocaleResolver(Locale locale) {
		// 设置默认区域设置
		setDefaultLocale(locale);
	}

	/**
	 * 创建一个 FixedLocaleResolver，暴露给定的区域设置和时区。
	 *
	 * @param locale   要暴露的区域设置
	 * @param timeZone 要暴露的时区
	 */
	public FixedLocaleResolver(Locale locale, TimeZone timeZone) {
		// 设置默认区域设置
		setDefaultLocale(locale);
		// 设置默认时区
		setDefaultTimeZone(timeZone);
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		// 获取默认区域设置
		Locale locale = getDefaultLocale();
		if (locale == null) {
			// 如果不存在，则从系统中获取当前设置的区域设置
			locale = Locale.getDefault();
		}
		return locale;
	}

	@Override
	public LocaleContext resolveLocaleContext(HttpServletRequest request) {
		return new TimeZoneAwareLocaleContext() {
			@Override
			@Nullable
			public Locale getLocale() {
				return getDefaultLocale();
			}

			@Override
			public TimeZone getTimeZone() {
				return getDefaultTimeZone();
			}
		};
	}

	@Override
	public void setLocaleContext(HttpServletRequest request, @Nullable HttpServletResponse response,
								 @Nullable LocaleContext localeContext) {

		throw new UnsupportedOperationException("Cannot change fixed locale - use a different locale resolution strategy");
	}

}
