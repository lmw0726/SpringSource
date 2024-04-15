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

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.CookieGenerator;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.TimeZone;

/**
 * {@link LocaleResolver} 实现，如果有自定义设置，
 * 则将 cookie 发送回用户，否则回退到配置的默认区域设置、请求的 {@code Accept-Language} 头，或服务器的默认区域设置。
 *
 * <p>这在没有用户会话的无状态应用程序中特别有用。cookie 可以选择性地包含关联的时区值；或者，您可以指定默认时区。
 *
 * <p>自定义控制器可以通过在解析器上调用 {@code #setLocale(Context)} 来覆盖用户的区域设置和时区，例如响应区域设置更改请求。
 * 作为更方便的替代方法，考虑使用 {@link org.springframework.web.servlet.support.RequestContext#changeLocale}。
 *
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 * @since 27.02.2003
 */
public class CookieLocaleResolver extends CookieGenerator implements LocaleContextResolver {

	/**
	 * 包含 {@code Locale} 的请求属性的名称。
	 * <p>仅在当前请求过程中更改了区域设置时用于覆盖 cookie 值！
	 * <p>使用 {@code RequestContext(Utils).getLocale()} 在控制器或视图中检索当前区域设置。
	 *
	 * @see org.springframework.web.servlet.support.RequestContext#getLocale
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 */
	public static final String LOCALE_REQUEST_ATTRIBUTE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";

	/**
	 * 包含 {@code TimeZone} 的请求属性的名称。
	 * <p>仅在当前请求过程中更改了区域设置时用于覆盖 cookie 值！
	 * <p>使用 {@code RequestContext(Utils).getTimeZone()} 在控制器或视图中检索当前时区。
	 *
	 * @see org.springframework.web.servlet.support.RequestContext#getTimeZone
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
	 */
	public static final String TIME_ZONE_REQUEST_ATTRIBUTE_NAME = CookieLocaleResolver.class.getName() + ".TIME_ZONE";

	/**
	 * 如果没有显式设置，默认使用的 cookie 名称。
	 */
	public static final String DEFAULT_COOKIE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";

	/**
	 * 是否符合语言标签
	 */
	private boolean languageTagCompliant = true;
	/**
	 * 是否拒绝无效的Cookie
	 */
	private boolean rejectInvalidCookies = true;
	/**
	 * 默认区域设置
	 */
	@Nullable
	private Locale defaultLocale;

	/**
	 * 默认时区
	 */
	@Nullable
	private TimeZone defaultTimeZone;


	/**
	 * 使用 {@linkplain #DEFAULT_COOKIE_NAME 默认 cookie 名称} 创建一个 {@link CookieLocaleResolver} 的新实例。
	 */
	public CookieLocaleResolver() {
		setCookieName(DEFAULT_COOKIE_NAME);
	}


	/**
	 * 指定此解析器的 cookie 是否应符合 BCP 47 语言标记，而不是 Java 的传统区域设置格式。
	 * <p>默认值为 {@code true}，从 5.1 版开始。将其切换为 {@code false} 以渲染 Java 的传统区域设置格式。
	 * 对于解析，此解析器会宽松接受传统的 {@link Locale#toString} 格式以及任何情况下的 BCP 47 语言标记。
	 *
	 * @see #parseLocaleValue(String)
	 * @see #toLocaleValue(Locale)
	 * @see Locale#forLanguageTag(String)
	 * @see Locale#toLanguageTag()
	 * @since 4.3
	 */
	public void setLanguageTagCompliant(boolean languageTagCompliant) {
		this.languageTagCompliant = languageTagCompliant;
	}

	/**
	 * 返回此解析器的 cookie 是否应符合 BCP 47 语言标记，而不是 Java 的传统区域设置格式。
	 *
	 * @since 4.3
	 */
	public boolean isLanguageTagCompliant() {
		return this.languageTagCompliant;
	}

	/**
	 * 指定是否拒绝带有无效内容的 cookie（例如，无效格式）。
	 * <p>默认值为 {@code true}。关闭此功能以宽松处理解析失败，这种情况下会回退到默认的区域设置和时区。
	 *
	 * @see #setDefaultLocale
	 * @see #setDefaultTimeZone
	 * @see #determineDefaultLocale
	 * @see #determineDefaultTimeZone
	 * @since 5.1.7
	 */
	public void setRejectInvalidCookies(boolean rejectInvalidCookies) {
		this.rejectInvalidCookies = rejectInvalidCookies;
	}

	/**
	 * 返回是否拒绝带有无效内容的 cookie（例如，无效格式）。
	 *
	 * @since 5.1.7
	 */
	public boolean isRejectInvalidCookies() {
		return this.rejectInvalidCookies;
	}

	/**
	 * 设置一个固定的区域设置，如果没有找到 cookie，则此解析器将返回该区域设置。
	 */
	public void setDefaultLocale(@Nullable Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * 返回如果没有找到 cookie，则此解析器将返回的固定区域设置（如果有）。
	 */
	@Nullable
	protected Locale getDefaultLocale() {
		return this.defaultLocale;
	}

	/**
	 * 设置一个固定的时区，如果没有找到 cookie，则此解析器将返回该时区。
	 *
	 * @since 4.0
	 */
	public void setDefaultTimeZone(@Nullable TimeZone defaultTimeZone) {
		this.defaultTimeZone = defaultTimeZone;
	}

	/**
	 * 返回如果没有找到 cookie，则此解析器将返回的固定时区（如果有）。
	 *
	 * @since 4.0
	 */
	@Nullable
	protected TimeZone getDefaultTimeZone() {
		return this.defaultTimeZone;
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		// 如有必要，从Cookie中解析Locale
		parseLocaleCookieIfNecessary(request);
		// 获取Locale值
		return (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
	}

	@Override
	public LocaleContext resolveLocaleContext(final HttpServletRequest request) {
		// 如有必要，从Cookie中解析Locale
		parseLocaleCookieIfNecessary(request);
		// 返回时区感知区域设置上下文
		return new TimeZoneAwareLocaleContext() {
			@Override
			@Nullable
			public Locale getLocale() {
				return (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
			}

			@Override
			@Nullable
			public TimeZone getTimeZone() {
				return (TimeZone) request.getAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
			}
		};
	}

	private void parseLocaleCookieIfNecessary(HttpServletRequest request) {
		if (request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME) == null) {
			// 如果获取的属性值为空
			Locale locale = null;
			TimeZone timeZone = null;

			// 获取并解析 cookie 值
			String cookieName = getCookieName();
			if (cookieName != null) {
				// Cookie名称存在，获取此Cookie
				Cookie cookie = WebUtils.getCookie(request, cookieName);
				if (cookie != null) {
					// 获取Cookie值
					String value = cookie.getValue();
					String localePart = value;
					String timeZonePart = null;
					// 查找分隔符位置
					int separatorIndex = localePart.indexOf('/');
					if (separatorIndex == -1) {
						// 容错地接受旧的使用空格分隔的 cookie...
						separatorIndex = localePart.indexOf(' ');
					}
					if (separatorIndex >= 0) {
						// 分割语言环境部分和时区部分
						localePart = value.substring(0, separatorIndex);
						timeZonePart = value.substring(separatorIndex + 1);
					}
					try {
						// 解析语言环境部分
						locale = (!"-".equals(localePart) ? parseLocaleValue(localePart) : null);
						if (timeZonePart != null) {
							// 解析时区部分
							timeZone = StringUtils.parseTimeZoneString(timeZonePart);
						}
					} catch (IllegalArgumentException ex) {
						// 如果是拒绝无效的 cookie 并且请求中没有错误异常属性，则抛出异常
						if (isRejectInvalidCookies() &&
								request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) == null) {
							throw new IllegalStateException("Encountered invalid locale cookie '" +
									cookieName + "': [" + value + "] due to: " + ex.getMessage());
						} else {
							// 宽容处理（例如，错误调度）：忽略语言环境/时区解析异常
							if (logger.isDebugEnabled()) {
								logger.debug("Ignoring invalid locale cookie '" + cookieName +
										"': [" + value + "] due to: " + ex.getMessage());
							}
						}
					}
					if (logger.isTraceEnabled()) {
						logger.trace("Parsed cookie value [" + cookie.getValue() + "] into locale '" + locale +
								"'" + (timeZone != null ? " and time zone '" + timeZone.getID() + "'" : ""));
					}
				}
			}

			// 设置请求属性：语言环境和时区
			request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME,
					(locale != null ? locale : determineDefaultLocale(request)));
			// 设置时区属性
			request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME,
					(timeZone != null ? timeZone : determineDefaultTimeZone(request)));
		}
	}

	@Override
	public void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale) {
		// 设置语言环境上下文
		setLocaleContext(request, response, (locale != null ? new SimpleLocaleContext(locale) : null));
	}

	@Override
	public void setLocaleContext(HttpServletRequest request, @Nullable HttpServletResponse response,
								 @Nullable LocaleContext localeContext) {

		Assert.notNull(response, "HttpServletResponse is required for CookieLocaleResolver");

		Locale locale = null;
		TimeZone timeZone = null;
		if (localeContext != null) {
			// 如果 localeContext存在
			// 获取语言环境
			locale = localeContext.getLocale();
			// 如果 localeContext 是 TimeZoneAwareLocaleContext 类型
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				// 获取时区
				timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			}
			//将语言环境和时区添加到 cookie中
			addCookie(response,
					(locale != null ? toLocaleValue(locale) : "-") + (timeZone != null ? '/' + timeZone.getID() : ""));
		} else {
			// 移除 cookie
			removeCookie(response);
		}
		// 设置请求属性：语言环境和时区
		request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME,
				(locale != null ? locale : determineDefaultLocale(request)));
		request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME,
				(timeZone != null ? timeZone : determineDefaultTimeZone(request)));
	}


	/**
	 * 解析来自传入 cookie 的区域设置值。
	 * <p>默认实现调用 {@link StringUtils#parseLocale(String)}，接受 {@link Locale#toString} 格式以及 BCP 47 语言标记。
	 *
	 * @param localeValue 要解析的区域设置值
	 * @return 相应的 {@code Locale} 实例
	 * @see StringUtils#parseLocale(String)
	 * @since 4.3
	 */
	@Nullable
	protected Locale parseLocaleValue(String localeValue) {
		return StringUtils.parseLocale(localeValue);
	}

	/**
	 * 将给定的区域设置渲染为包含在 cookie 中的文本值。
	 * <p>默认实现调用 {@link Locale#toString()} 或 {@link Locale#toLanguageTag()}，取决于 {@link #setLanguageTagCompliant "languageTagCompliant"} 配置属性。
	 *
	 * @param locale 要转换为字符串的区域设置
	 * @return 给定区域设置的字符串表示形式
	 * @see #isLanguageTagCompliant()
	 * @since 4.3
	 */
	protected String toLocaleValue(Locale locale) {
		return (isLanguageTagCompliant() ? locale.toLanguageTag() : locale.toString());
	}

	/**
	 * 确定给定请求的默认区域设置，在没有找到区域设置 cookie 的情况下调用。
	 * <p>默认实现返回配置的默认区域设置（如果有），否则返回请求的 {@code Accept-Language} 头区域设置或服务器的默认区域设置。
	 *
	 * @param request 要为其解析区域设置的请求
	 * @return 默认区域设置（永不为 {@code null}）
	 * @see #setDefaultLocale
	 * @see javax.servlet.http.HttpServletRequest#getLocale()
	 */
	protected Locale determineDefaultLocale(HttpServletRequest request) {
		Locale defaultLocale = getDefaultLocale();
		if (defaultLocale == null) {
			defaultLocale = request.getLocale();
		}
		return defaultLocale;
	}

	/**
	 * 确定给定请求的默认时区，在没有找到区域设置 cookie 的情况下调用。
	 * <p>默认实现返回配置的默认时区（如果有），否则返回 {@code null}。
	 *
	 * @param request 要为其解析时区的请求
	 * @return 默认时区（如果未定义则为 {@code null}）
	 * @see #setDefaultTimeZone
	 */
	@Nullable
	protected TimeZone determineDefaultTimeZone(HttpServletRequest request) {
		return getDefaultTimeZone();
	}

}
