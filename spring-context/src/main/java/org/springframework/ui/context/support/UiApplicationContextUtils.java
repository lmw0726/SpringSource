/*
 * Copyright 2002-2012 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.ui.context.HierarchicalThemeSource;
import org.springframework.ui.context.ThemeSource;

/**
 * UI 应用上下文实现的工具类。
 * 提供对名为 "themeSource" 的特殊 bean 的支持，
 * 该 bean 类型为 {@link org.springframework.ui.context.ThemeSource}。
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @since 17.06.2003
 */
public abstract class UiApplicationContextUtils {

	/**
	 * 工厂中 ThemeSource bean 的名称。
	 * 如果未提供，则主题解析将委托给父级。
	 * @see org.springframework.ui.context.ThemeSource
	 */
	public static final String THEME_SOURCE_BEAN_NAME = "themeSource";


	private static final Log logger = LogFactory.getLog(UiApplicationContextUtils.class);


	/**
	 * 为给定的应用上下文初始化 ThemeSource，
	 * 自动检测名为 "themeSource" 的 bean。如果未找到该 bean，
	 * 将使用默认的（空的）ThemeSource。
	 * @param context 当前应用上下文
	 * @return 初始化后的主题源（永远不会为 {@code null}）
	 * @see #THEME_SOURCE_BEAN_NAME
	 */
	public static ThemeSource initThemeSource(ApplicationContext context) {
		if (context.containsLocalBean(THEME_SOURCE_BEAN_NAME)) {
			ThemeSource themeSource = context.getBean(THEME_SOURCE_BEAN_NAME, ThemeSource.class);
			// 让 ThemeSource 感知父 ThemeSource。
			if (context.getParent() instanceof ThemeSource && themeSource instanceof HierarchicalThemeSource) {
				HierarchicalThemeSource hts = (HierarchicalThemeSource) themeSource;
				if (hts.getParentThemeSource() == null) {
					// 仅在尚未注册父 ThemeSource 时，
					// 将父上下文设置为父 ThemeSource。
					hts.setParentThemeSource((ThemeSource) context.getParent());
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Using ThemeSource [" + themeSource + "]");
			}
			return themeSource;
		}
		else {
			// 使用默认的 ThemeSource 以能够接受 getTheme 调用，
			// 或委托给父上下文的默认值，或使用本地 ResourceBundleThemeSource。
			HierarchicalThemeSource themeSource = null;
			if (context.getParent() instanceof ThemeSource) {
				themeSource = new DelegatingThemeSource();
				themeSource.setParentThemeSource((ThemeSource) context.getParent());
			}
			else {
				themeSource = new ResourceBundleThemeSource();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate ThemeSource with name '" + THEME_SOURCE_BEAN_NAME +
						"': using default [" + themeSource + "]");
			}
			return themeSource;
		}
	}

}
