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
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.TimeZone;

/**
 * {@link org.springframework.web.servlet.LocaleResolver} 的实现，如果存在自定义设置，则使用用户会话中的区域设置属性，否则返回配置的默认区域设置、请求的 {@code Accept-Language} 标头或服务器的默认区域设置。
 *
 * <p>如果应用程序需要用户会话，即 {@code HttpSession} 不必仅用于存储用户的区域设置，则此方案最为适用。会话还可以选择包含一个关联的时区属性；或者，您可以指定默认时区。
 *
 * <p>自定义控制器可以通过调用 {@code #setLocale(Context)} 来覆盖用户的区域设置和时区，例如，响应区域设置更改请求。作为更方便的替代方案，请考虑使用 {@link org.springframework.web.servlet.support.RequestContext#changeLocale}。
 *
 * <p>与 {@link CookieLocaleResolver} 相比，此策略将本地选择的区域设置存储在 Servlet 容器的 {@code HttpSession} 中。因此，这些设置对于每个会话仅是临时的，因此在每个会话终止时丢失。
 *
 * <p>请注意，与外部会话管理机制（如“Spring Session”项目）之间没有直接关系。此 {@code LocaleResolver} 将简单地针对当前 {@code HttpServletRequest} 评估和修改相应的 {@code HttpSession} 属性。
 *
 * @author Juergen Hoeller
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 * @since 27.02.2003
 */
public class SessionLocaleResolver extends AbstractLocaleContextResolver {

	/**
	 * 默认的会话属性名称，用于保存区域设置。
	 * <p>仅在此实现内部使用。
	 * <p>在控制器或视图中使用 {@code RequestContext(Utils).getLocale()}
	 * 来检索当前的locale。
	 *
	 * @see org.springframework.web.servlet.support.RequestContext#getLocale
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 */
	public static final String LOCALE_SESSION_ATTRIBUTE_NAME = SessionLocaleResolver.class.getName() + ".LOCALE";

	/**
	 * 默认的会话属性名称，用于保存时区。
	 * <p>仅在此实现内部使用。
	 * <p>在控制器或视图中使用 {@code RequestContext(Utils).getTimeZone()}
	 * 来检索当前的时区。
	 *
	 * @see org.springframework.web.servlet.support.RequestContext#getTimeZone
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
	 */
	public static final String TIME_ZONE_SESSION_ATTRIBUTE_NAME = SessionLocaleResolver.class.getName() + ".TIME_ZONE";

	/**
	 * 区域设置属性名称
	 */
	private String localeAttributeName = LOCALE_SESSION_ATTRIBUTE_NAME;

	/**
	 * 时区属性名称
	 */
	private String timeZoneAttributeName = TIME_ZONE_SESSION_ATTRIBUTE_NAME;


	/**
	 * 指定保存当前{@link Locale}值的{@code HttpSession}中对应的属性名称。
	 * <p>默认是内部{@link #LOCALE_SESSION_ATTRIBUTE_NAME}。
	 *
	 * @since 4.3.8
	 */
	public void setLocaleAttributeName(String localeAttributeName) {
		this.localeAttributeName = localeAttributeName;
	}

	/**
	 * 指定保存当前{@link TimeZone}值的{@code HttpSession}中对应的属性名称。
	 * <p>默认是内部{@link #TIME_ZONE_SESSION_ATTRIBUTE_NAME}。
	 *
	 * @since 4.3.8
	 */
	public void setTimeZoneAttributeName(String timeZoneAttributeName) {
		this.timeZoneAttributeName = timeZoneAttributeName;
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		// 从 会话 中获取区域设置
		Locale locale = (Locale) WebUtils.getSessionAttribute(request, this.localeAttributeName);
		if (locale == null) {
			// 获取不到，则从默认区域设置中推断区域设置
			locale = determineDefaultLocale(request);
		}
		return locale;
	}

	@Override
	public LocaleContext resolveLocaleContext(final HttpServletRequest request) {
		return new TimeZoneAwareLocaleContext() {
			@Override
			public Locale getLocale() {
				// 从 会话 中获取区域设置
				Locale locale = (Locale) WebUtils.getSessionAttribute(request, localeAttributeName);
				if (locale == null) {
					// 获取不到，则从默认区域设置中推断区域设置
					locale = determineDefaultLocale(request);
				}
				return locale;
			}

			@Override
			@Nullable
			public TimeZone getTimeZone() {
				// 从 会话中 获取时区
				TimeZone timeZone = (TimeZone) WebUtils.getSessionAttribute(request, timeZoneAttributeName);
				if (timeZone == null) {
					// 获取不到，则从默认时区推断时区
					timeZone = determineDefaultTimeZone(request);
				}
				return timeZone;
			}
		};
	}

	@Override
	public void setLocaleContext(HttpServletRequest request, @Nullable HttpServletResponse response,
								 @Nullable LocaleContext localeContext) {

		Locale locale = null;
		TimeZone timeZone = null;
		if (localeContext != null) {
			// 如果存在区域设置上下文，则获取区域设置
			locale = localeContext.getLocale();
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				// 如果 区域设置上下文是 TimeZoneAwareLocaleContext类型，则获取时区
				timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			}
		}
		// 将区域设置和时区设置到会话中
		WebUtils.setSessionAttribute(request, this.localeAttributeName, locale);
		WebUtils.setSessionAttribute(request, this.timeZoneAttributeName, timeZone);
	}


	/**
	 * 确定给定请求的默认区域设置，如果没有找到{@link Locale}会话属性时调用。
	 * <p>默认实现返回配置的{@linkplain #setDefaultLocale(Locale)默认区域设置}（如果有），
	 * 否则回退到请求的{@code Accept-Language}头区域设置或服务器的默认区域设置。
	 *
	 * @param request 要解析区域设置的请求
	 * @return 默认区域设置（永不为{@code null}）
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
	 * 确定给定请求的默认时区，如果没有找到{@link TimeZone}会话属性时调用。
	 * <p>默认实现返回配置的默认时区（如果有），否则返回{@code null}。
	 *
	 * @param request 要解析时区的请求
	 * @return 默认时区（如果未定义，则为{@code null}）
	 * @see #setDefaultTimeZone
	 */
	@Nullable
	protected TimeZone determineDefaultTimeZone(HttpServletRequest request) {
		return getDefaultTimeZone();
	}

}
