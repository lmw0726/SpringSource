/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.web.servlet.ThemeResolver;

/**
 * {@link ThemeResolver} 实现的抽象基类。提供对默认主题名称的支持。
 *
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @since 17.06.2003
 */
public abstract class AbstractThemeResolver implements ThemeResolver {

	/**
	 * 默认主题名称的开箱即用值: "theme"。
	 */
	public static final String ORIGINAL_DEFAULT_THEME_NAME = "theme";

	/**
	 * 默认主题名称
	 */
	private String defaultThemeName = ORIGINAL_DEFAULT_THEME_NAME;


	/**
	 * 设置默认主题的名称。
	 * 开箱即用的值为 "theme"。
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

}
