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

package org.springframework.web.servlet.theme;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.util.CookieGenerator;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link ThemeResolver} 实现，它在存在自定义设置时使用发送回用户的 cookie，否则使用默认主题。
 * 这对于没有用户会话的无状态应用程序特别有用。
 *
 * <p>自定义控制器可以通过调用 {@code setThemeName} 来覆盖用户的主题，
 * 例如，响应某个主题更改请求。
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @see #setThemeName
 * @since 17.06.2003
 */
public class CookieThemeResolver extends CookieGenerator implements ThemeResolver {

	/**
	 * 如果没有提供备选方案，则使用的默认主题名称。
	 */
	public static final String ORIGINAL_DEFAULT_THEME_NAME = "theme";

	/**
	 * 存储主题名称的请求属性的名称。
	 * 仅在当前请求过程中更改主题时用于覆盖 cookie 值！
	 * 使用 RequestContext.getTheme() 在控制器或视图中检索当前主题。
	 *
	 * @see org.springframework.web.servlet.support.RequestContext#getTheme
	 */
	public static final String THEME_REQUEST_ATTRIBUTE_NAME = CookieThemeResolver.class.getName() + ".THEME";

	/**
	 * 持有主题名称的 cookie 的默认名称。
	 */
	public static final String DEFAULT_COOKIE_NAME = CookieThemeResolver.class.getName() + ".THEME";

	/**
	 * 默认主题名称
	 */
	private String defaultThemeName = ORIGINAL_DEFAULT_THEME_NAME;


	public CookieThemeResolver() {
		setCookieName(DEFAULT_COOKIE_NAME);
	}


	/**
	 * 设置默认主题的名称。
	 */
	public void setDefaultThemeName(String defaultThemeName) {
		this.defaultThemeName = defaultThemeName;
	}

	/**
	 * 返回默认主题的名称。
	 */
	public String getDefaultThemeName() {
		return this.defaultThemeName;
	}


	@Override
	public String resolveThemeName(HttpServletRequest request) {
		// 检查请求以获取预解析或预设置的主题。
		String themeName = (String) request.getAttribute(THEME_REQUEST_ATTRIBUTE_NAME);
		if (themeName != null) {
			return themeName;
		}

		// 从请求中检索 cookie 值。
		String cookieName = getCookieName();
		if (cookieName != null) {
			// 获取 cookie 值
			Cookie cookie = WebUtils.getCookie(request, cookieName);
			if (cookie != null) {
				// 获取值
				String value = cookie.getValue();
				if (StringUtils.hasText(value)) {
					// 如果值不为空，将其设置为主题名称
					themeName = value;
				}
			}
		}

		// 回退到默认主题。
		if (themeName == null) {
			// 如果主题名称为空，则设置为默认主题名称
			themeName = getDefaultThemeName();
		}
		request.setAttribute(THEME_REQUEST_ATTRIBUTE_NAME, themeName);
		return themeName;
	}

	@Override
	public void setThemeName(
			HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable String themeName) {

		Assert.notNull(response, "HttpServletResponse is required for CookieThemeResolver");

		if (StringUtils.hasText(themeName)) {
			// 设置请求属性并添加 cookie。
			request.setAttribute(THEME_REQUEST_ATTRIBUTE_NAME, themeName);
			addCookie(response, themeName);
		} else {
			// 设置请求属性为回退主题并删除 cookie。
			request.setAttribute(THEME_REQUEST_ATTRIBUTE_NAME, getDefaultThemeName());
			removeCookie(response);
		}
	}

}
