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

package org.springframework.web.servlet.theme;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link org.springframework.web.servlet.ThemeResolver} 实现，如果有自定义设置，则使用用户会话中的主题属性，否则回退到默认主题。如果应用程序需要用户会话，这是最合适的。
 *
 * <p>自定义控制器可以通过调用 {@code setThemeName} 来覆盖用户的主题，例如响应主题更改请求。
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @see #setThemeName
 * @since 17.06.2003
 */
public class SessionThemeResolver extends AbstractThemeResolver {

	/**
	 * 保存主题名称的会话属性的名称。只在此实现中内部使用。
	 * 使用 {@code RequestContext(Utils).getTheme()} 在控制器或视图中检索当前主题。
	 *
	 * @see org.springframework.web.servlet.support.RequestContext#getTheme
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTheme
	 */
	public static final String THEME_SESSION_ATTRIBUTE_NAME = SessionThemeResolver.class.getName() + ".THEME";


	@Override
	public String resolveThemeName(HttpServletRequest request) {
		String themeName = (String) WebUtils.getSessionAttribute(request, THEME_SESSION_ATTRIBUTE_NAME);
		// 指定了特定的主题，还是需要回退到默认主题？
		return (themeName != null ? themeName : getDefaultThemeName());
	}

	@Override
	public void setThemeName(
			HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable String themeName) {
		// 设置 Session 中的主题名称
		WebUtils.setSessionAttribute(request, THEME_SESSION_ATTRIBUTE_NAME,
				(StringUtils.hasText(themeName) ? themeName : null));
	}

}
