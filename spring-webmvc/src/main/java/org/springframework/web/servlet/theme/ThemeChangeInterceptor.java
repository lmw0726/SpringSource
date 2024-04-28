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

package org.springframework.web.servlet.theme;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 拦截器，允许通过可配置的请求参数（默认参数名："theme"）在每个请求上更改当前主题。
 *
 * @author Juergen Hoeller
 * @since 20.06.2003
 * @see org.springframework.web.servlet.ThemeResolver
 */
public class ThemeChangeInterceptor implements HandlerInterceptor {

	/**
	 * 主题规范参数的默认名称："theme"。
	 */
	public static final String DEFAULT_PARAM_NAME = "theme";
	/**
	 * 参数名称
	 */
	private String paramName = DEFAULT_PARAM_NAME;


	/**
	 * 设置包含主题规范的参数的名称。默认为"theme"。
	 */
	public void setParamName(String paramName) {
		this.paramName = paramName;
	}

	/**
	 * 返回包含主题规范的参数的名称。
	 */
	public String getParamName() {
		return this.paramName;
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {

		// 从请求中获取参数值作为新的主题
		String newTheme = request.getParameter(this.paramName);

		// 如果新主题不为空
		if (newTheme != null) {
			// 获取主题解析器
			ThemeResolver themeResolver = RequestContextUtils.getThemeResolver(request);
			// 如果主题解析器为空，则抛出异常
			if (themeResolver == null) {
				throw new IllegalStateException("No ThemeResolver found: not in a DispatcherServlet request?");
			}
			// 设置新的主题
			themeResolver.setThemeName(request, response, newTheme);
		}

		// 无论如何都继续处理
		return true;
	}

}
