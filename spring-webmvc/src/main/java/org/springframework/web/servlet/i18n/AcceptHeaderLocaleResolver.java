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

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * {@link LocaleResolver} 的实现，简单地使用 HTTP 请求的 {@code Accept-Language} 头中指定的主要区域设置
 * （即客户端浏览器发送的区域设置，通常是客户端操作系统的区域设置）。
 *
 * <p>注意：不支持 {@link #setLocale}，因为 {@code Accept-Language} 头只能通过更改客户端的区域设置来更改。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see javax.servlet.http.HttpServletRequest#getLocale()
 * @since 27.02.2003
 */
public class AcceptHeaderLocaleResolver implements LocaleResolver {

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
	 * 配置要与通过 {@link HttpServletRequest#getLocales()} 确定的请求区域设置进行检查的支持区域设置。
	 * 如果未配置此项，则使用 {@link HttpServletRequest#getLocale()}。
	 *
	 * @param locales 支持的区域设置
	 * @since 4.3
	 */
	public void setSupportedLocales(List<Locale> locales) {
		this.supportedLocales.clear();
		this.supportedLocales.addAll(locales);
	}

	/**
	 * 获取已配置的支持区域设置列表。
	 *
	 * @since 4.3
	 */
	public List<Locale> getSupportedLocales() {
		return this.supportedLocales;
	}

	/**
	 * 配置固定的默认区域设置，以在请求没有 "Accept-Language" 头时回退。
	 * <p>默认情况下，不设置此项，在没有 "Accept-Language" 头时，将使用服务器的默认区域设置，如 {@link HttpServletRequest#getLocale()} 中定义的那样。
	 *
	 * @param defaultLocale 要使用的默认区域设置
	 * @since 4.3
	 */
	public void setDefaultLocale(@Nullable Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * 配置的默认区域设置（如果有）。
	 * <p>可以在子类中重写此方法。
	 *
	 * @since 4.3
	 */
	@Nullable
	public Locale getDefaultLocale() {
		return this.defaultLocale;
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		// 获取默认语言环境
		Locale defaultLocale = getDefaultLocale();
		if (defaultLocale != null && request.getHeader("Accept-Language") == null) {
			// 如果存在默认语言环境且请求头中没有 Accept-Language 字段，则返回默认语言环境
			return defaultLocale;
		}
		// 获取请求的语言环境
		Locale requestLocale = request.getLocale();
		// 获取支持的语言环境列表
		List<Locale> supportedLocales = getSupportedLocales();
		// 如果支持的语言环境列表为空或包含请求的语言环境，则返回请求的语言环境
		if (supportedLocales.isEmpty() || supportedLocales.contains(requestLocale)) {
			return requestLocale;
		}
		// 查找支持的语言环境中最匹配的语言环境
		Locale supportedLocale = findSupportedLocale(request, supportedLocales);
		if (supportedLocale != null) {
			// 如果受支持的语言环境存在，则返回该语言环境
			return supportedLocale;
		}
		// 如果没有找到最匹配的支持的语言环境，则返回默认语言环境（如果存在），否则返回请求的语言环境
		return (defaultLocale != null ? defaultLocale : requestLocale);
	}

	@Nullable
	private Locale findSupportedLocale(HttpServletRequest request, List<Locale> supportedLocales) {
		// 获取请求中的语言环境枚举
		Enumeration<Locale> requestLocales = request.getLocales();
		Locale languageMatch = null;
		// 遍历请求中的语言环境
		while (requestLocales.hasMoreElements()) {
			// 获取下一个语言环境
			Locale locale = requestLocales.nextElement();
			// 如果支持的语言环境列表中包含该语言环境
			if (supportedLocales.contains(locale)) {
				// 如果语言匹配为空或者语言匹配的语言与当前语言相同
				if (languageMatch == null || languageMatch.getLanguage().equals(locale.getLanguage())) {
					// 完全匹配：语言 + 国家，可能是之前只匹配语言的匹配结果
					return locale;
				}
			} else if (languageMatch == null) {
				// 如果语言匹配为空，尝试寻找只匹配语言的备选方案
				for (Locale candidate : supportedLocales) {
					if (!StringUtils.hasLength(candidate.getCountry()) &&
							candidate.getLanguage().equals(locale.getLanguage())) {
						// 如果候选语言环境的的国籍信息存在，并且与当前的吃吃的语言环境的语言相同，
						// 则说明找到了匹配的语言环境，跳出循环
						languageMatch = candidate;
						break;
					}
				}
			}
		}
		// 返回语言匹配结果
		return languageMatch;
	}

	@Override
	public void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale) {
		throw new UnsupportedOperationException(
				"Cannot change HTTP Accept-Language header - use a different locale resolution strategy");
	}

}
