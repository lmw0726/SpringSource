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

package org.springframework.ui.context.support;

import org.springframework.lang.Nullable;
import org.springframework.ui.context.HierarchicalThemeSource;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;

/**
 * 空的 ThemeSource 实现，将所有调用委托给父 ThemeSource。
 * 如果没有父 ThemeSource 可用，则不会解析任何主题。
 *
 * <p>由 UiApplicationContextUtils 用作占位符，当上下文未定义自己的 ThemeSource 时使用。
 * 不打算在应用程序中直接使用。
 *
 * @author Juergen Hoeller
 * @since 1.2.4
 * @see UiApplicationContextUtils
 */
public class DelegatingThemeSource implements HierarchicalThemeSource {

	@Nullable
	private ThemeSource parentThemeSource;


	@Override
	public void setParentThemeSource(@Nullable ThemeSource parentThemeSource) {
		this.parentThemeSource = parentThemeSource;
	}

	@Override
	@Nullable
	public ThemeSource getParentThemeSource() {
		return this.parentThemeSource;
	}


	@Override
	@Nullable
	public Theme getTheme(String themeName) {
		if (this.parentThemeSource != null) {
			return this.parentThemeSource.getTheme(themeName);
		}
		else {
			return null;
		}
	}

}
