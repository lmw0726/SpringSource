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

package org.springframework.web.server.i18n;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@link LocaleContextResolver}实现，简单地使用HTTP请求的“Accept-Language”头中指定的主要区域设置
 * （即客户端浏览器发送的区域设置，通常是客户端操作系统的区域设置）。
 *
 * <p>注意：不支持{@link #setLocaleContext}，因为接受头只能通过更改客户端的区域设置来更改。
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @see HttpHeaders#getAcceptLanguageAsLocales()
 * @since 5.0
 */
public class AcceptHeaderLocaleContextResolver implements LocaleContextResolver {
	/**
	 * 支持的区域设置列表
	 */
	private final List<Locale> supportedLocales = new ArrayList<>(4);

	/**
	 * 默认的区域设置
	 */
	@Nullable
	private Locale defaultLocale;


	/**
	 * 配置要检查的支持的区域设置列表，以与通过{@link HttpHeaders#getAcceptLanguageAsLocales()}确定的请求的区域设置进行比较。
	 *
	 * @param locales 支持的区域设置
	 */
	public void setSupportedLocales(List<Locale> locales) {
		this.supportedLocales.clear();
		this.supportedLocales.addAll(locales);
	}

	/**
	 * 返回已配置的支持的区域设置列表。
	 */
	public List<Locale> getSupportedLocales() {
		return this.supportedLocales;
	}

	/**
	 * 配置一个固定的默认区域设置，以在请求没有“Accept-Language”头（默认情况下未设置）时回退。
	 *
	 * @param defaultLocale 要使用的默认区域设置
	 */
	public void setDefaultLocale(@Nullable Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * 配置的默认区域设置（如果有）。
	 * <p>此方法可以在子类中重写。
	 */
	@Nullable
	public Locale getDefaultLocale() {
		return this.defaultLocale;
	}


	@Override
	public LocaleContext resolveLocaleContext(ServerWebExchange exchange) {
		List<Locale> requestLocales = null;
		try {
			// 尝试获取请求中的语言偏好
			requestLocales = exchange.getRequest().getHeaders().getAcceptLanguageAsLocales();
		} catch (IllegalArgumentException ex) {
			// 如果Accept-Language头部无效：将其视为空以进行匹配
		}
		return new SimpleLocaleContext(resolveSupportedLocale(requestLocales));
	}

	@Nullable
	private Locale resolveSupportedLocale(@Nullable List<Locale> requestLocales) {
		if (CollectionUtils.isEmpty(requestLocales)) {
			// 如果请求的语言列表为空，则返回默认语言，可能为空
			return getDefaultLocale();
		}

		List<Locale> supportedLocales = getSupportedLocales();
		if (supportedLocales.isEmpty()) {
			// 如果支持的语言列表为空，则返回请求语言列表的第一个语言，永不为空
			return requestLocales.get(0);
		}

		Locale languageMatch = null;
		// 遍历请求的区域设置
		for (Locale locale : requestLocales) {
			// 如果支持的区域设置，包含当前的区域设置
			if (supportedLocales.contains(locale)) {
				if (languageMatch == null || languageMatch.getLanguage().equals(locale.getLanguage())) {
					// 完全匹配：语言 + 国家，可能是从之前的仅语言匹配中缩小了范围
					return locale;
				}
			} else if (languageMatch == null) {
				// 尝试找到一个仅语言匹配作为备用
				// 遍历支持的区域设置
				for (Locale candidate : supportedLocales) {
					if (!StringUtils.hasLength(candidate.getCountry()) &&
							candidate.getLanguage().equals(locale.getLanguage())) {
						// 如果当前的候选区域设置的国家为空，但语言与当前区域设置相同，则将其设置为匹配的候选者。
						languageMatch = candidate;
						break;
					}
				}
			}
		}
		if (languageMatch != null) {
			// 如果匹配语言的候选区域设置存在，则返回该区域设置。
			return languageMatch;
		}
		// 获取默认区域设置
		Locale defaultLocale = getDefaultLocale();
		// 返回默认语言，如果默认语言不为空，否则返回请求语言列表的第一个语言
		return (defaultLocale != null ? defaultLocale : requestLocales.get(0));
	}

	@Override
	public void setLocaleContext(ServerWebExchange exchange, @Nullable LocaleContext locale) {
		throw new UnsupportedOperationException(
				"Cannot change HTTP accept header - use a different locale context resolution strategy");
	}

}
