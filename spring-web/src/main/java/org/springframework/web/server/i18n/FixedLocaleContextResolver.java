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

package org.springframework.web.server.i18n;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

import java.util.Locale;
import java.util.TimeZone;

/**
 * 始终返回固定区域设置和可选时区的{@link LocaleContextResolver}实现。默认为当前JVM的默认区域设置。
 *
 * <p>注意：不支持{@link #setLocaleContext}，因为固定的区域设置和时区不能更改。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class FixedLocaleContextResolver implements LocaleContextResolver {
	/**
	 * 区域设置
	 */
	private final Locale locale;

	/**
	 * 时区
	 */
	@Nullable
	private final TimeZone timeZone;


	/**
	 * 创建一个默认的FixedLocaleResolver，公开配置的默认区域设置（或者作为回退的JVM的默认区域设置）。
	 */
	public FixedLocaleContextResolver() {
		this(Locale.getDefault());
	}

	/**
	 * 创建一个FixedLocaleResolver，公开给定的区域设置。
	 *
	 * @param locale 要公开的区域设置
	 */
	public FixedLocaleContextResolver(Locale locale) {
		this(locale, null);
	}

	/**
	 * 创建一个FixedLocaleResolver，公开给定的区域设置和时区。
	 *
	 * @param locale   要公开的区域设置
	 * @param timeZone 要公开的时区
	 */
	public FixedLocaleContextResolver(Locale locale, @Nullable TimeZone timeZone) {
		Assert.notNull(locale, "Locale must not be null");
		this.locale = locale;
		this.timeZone = timeZone;
	}

	@Override
	public LocaleContext resolveLocaleContext(ServerWebExchange exchange) {
		return new TimeZoneAwareLocaleContext() {
			@Override
			public Locale getLocale() {
				return locale;
			}

			@Override
			@Nullable
			public TimeZone getTimeZone() {
				return timeZone;
			}
		};
	}

	@Override
	public void setLocaleContext(ServerWebExchange exchange, @Nullable LocaleContext localeContext) {
		throw new UnsupportedOperationException(
				"Cannot change fixed locale - use a different locale context resolution strategy");
	}

}
