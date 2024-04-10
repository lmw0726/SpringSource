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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * 用于基于Web的主题解析策略的接口，允许通过请求进行主题解析和通过请求和响应进行主题修改。
 *
 * <p>该接口允许基于会话、cookie等实现。默认实现是{@link org.springframework.web.servlet.theme.FixedThemeResolver}，简单地使用配置的默认主题。
 * <p>请注意，此解析器仅负责确定当前主题名称。通过相应的ThemeSource（即当前的WebApplicationContext）通过DispatcherServlet查找已解析的主题名称的Theme实例。
 * <p>在控制器或视图中使用{@link org.springframework.web.servlet.support.RequestContext#getTheme()}来检索当前主题，与实际解析策略无关。
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @see org.springframework.ui.context.Theme
 * @see org.springframework.ui.context.ThemeSource
 * @since 17.06.2003
 */
public interface ThemeResolver {

	/**
	 * 通过给定的请求解析当前主题名称。在任何情况下都应返回默认主题作为后备。
	 *
	 * @param request 用于解析的请求
	 * @return 当前主题名称
	 */
	String resolveThemeName(HttpServletRequest request);

	/**
	 * 将当前主题名称设置为给定的名称。
	 *
	 * @param request   用于主题名称修改的请求
	 * @param response  用于主题名称修改的响应
	 * @param themeName 新的主题名称（{@code null}或空表示重置）
	 * @throws UnsupportedOperationException 如果ThemeResolver实现不支持动态更改主题
	 */
	void setThemeName(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable String themeName);

}
