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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.HierarchicalThemeSource;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;

/**
 * {@link ThemeSource} 的实现，每个主题查找一个独立的
 * {@link java.util.ResourceBundle}。主题名称将被解释为
 * ResourceBundle 的基本名称，支持为所有主题设置一个共同的
 * 基本名称前缀。
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @see #setBasenamePrefix
 * @see java.util.ResourceBundle
 * @see org.springframework.context.support.ResourceBundleMessageSource
 */
public class ResourceBundleThemeSource implements HierarchicalThemeSource, BeanClassLoaderAware {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ThemeSource parentThemeSource;

	private String basenamePrefix = "";

	@Nullable
	private String defaultEncoding;

	@Nullable
	private Boolean fallbackToSystemLocale;

	@Nullable
	private ClassLoader beanClassLoader;

	/** 从主题名称到 Theme 实例的映射。 */
	private final Map<String, Theme> themeCache = new ConcurrentHashMap<>();


	@Override
	public void setParentThemeSource(@Nullable ThemeSource parent) {
		this.parentThemeSource = parent;

		// 更新已有的 Theme 对象。
		// 通常在调用此方法时不应存在任何已缓存的主题。
		synchronized (this.themeCache) {
			for (Theme theme : this.themeCache.values()) {
				initParent(theme);
			}
		}
	}

	@Override
	@Nullable
	public ThemeSource getParentThemeSource() {
		return this.parentThemeSource;
	}

	/**
	 * 设置应用于 ResourceBundle 基本名称的前缀，
	 * 即主题名称。
	 * 例如：basenamePrefix="test.", themeName="theme" &rarr; basename="test.theme"。
	 * <p>注意，ResourceBundle 名称实际上是类路径位置：因此，
	 * JDK 的标准 ResourceBundle 将点号视为包分隔符。
	 * 这意味着 "test.theme" 实际上等同于 "test/theme"，
	 * 就像在编程方式使用 {@code java.util.ResourceBundle} 时一样。
	 * @see java.util.ResourceBundle#getBundle(String)
	 */
	public void setBasenamePrefix(@Nullable String basenamePrefix) {
		this.basenamePrefix = (basenamePrefix != null ? basenamePrefix : "");
	}

	/**
	 * 设置用于解析资源束文件的默认字符集。
	 * <p>{@link ResourceBundleMessageSource} 的默认值是
	 * {@code java.util.ResourceBundle} 的默认编码：ISO-8859-1。
	 * @since 4.2
	 * @see ResourceBundleMessageSource#setDefaultEncoding
	 */
	public void setDefaultEncoding(@Nullable String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * 设置当找不到特定区域设置的文件时，是否回退到系统区域设置。
	 * <p>{@link ResourceBundleMessageSource} 的默认值为 "true"。
	 * @since 4.2
	 * @see ResourceBundleMessageSource#setFallbackToSystemLocale
	 */
	public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
		this.fallbackToSystemLocale = fallbackToSystemLocale;
	}

	@Override
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	/**
	 * 此实现返回一个 SimpleTheme 实例，持有一个基于
	 * ResourceBundle 的 MessageSource，其基本名称对应于
	 * 给定的主题名称（加上配置的 "basenamePrefix" 前缀）。
	 * <p>SimpleTheme 实例按主题名称缓存。如果需要主题反映
	 * 底层文件的更改，请使用可重新加载的 MessageSource。
	 * @see #setBasenamePrefix
	 * @see #createMessageSource
	 */
	@Override
	@Nullable
	public Theme getTheme(String themeName) {
		Theme theme = this.themeCache.get(themeName);
		if (theme == null) {
			synchronized (this.themeCache) {
				theme = this.themeCache.get(themeName);
				if (theme == null) {
					String basename = this.basenamePrefix + themeName;
					MessageSource messageSource = createMessageSource(basename);
					theme = new SimpleTheme(themeName, messageSource);
					initParent(theme);
					this.themeCache.put(themeName, theme);
					if (logger.isDebugEnabled()) {
						logger.debug("Theme created: name '" + themeName + "', basename [" + basename + "]");
					}
				}
			}
		}
		return theme;
	}

	/**
	 * 为给定的基本名称创建一个 MessageSource，
	 * 用作相应主题的 MessageSource。
	 * <p>默认实现为给定的基本名称创建一个 ResourceBundleMessageSource。
	 * 子类可以创建一个专门配置的 ReloadableResourceBundleMessageSource，
	 * 例如。
	 * @param basename 要为其创建 MessageSource 的基本名称
	 * @return MessageSource
	 * @see org.springframework.context.support.ResourceBundleMessageSource
	 * @see org.springframework.context.support.ReloadableResourceBundleMessageSource
	 */
	protected MessageSource createMessageSource(String basename) {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename(basename);
		if (this.defaultEncoding != null) {
			messageSource.setDefaultEncoding(this.defaultEncoding);
		}
		if (this.fallbackToSystemLocale != null) {
			messageSource.setFallbackToSystemLocale(this.fallbackToSystemLocale);
		}
		if (this.beanClassLoader != null) {
			messageSource.setBeanClassLoader(this.beanClassLoader);
		}
		return messageSource;
	}

	/**
	 * 使用此 ThemeSource 对应父级的 MessageSource
	 * 初始化给定主题的 MessageSource。
	 * @param theme 要（重新）初始化的 Theme
	 */
	protected void initParent(Theme theme) {
		if (theme.getMessageSource() instanceof HierarchicalMessageSource) {
			HierarchicalMessageSource messageSource = (HierarchicalMessageSource) theme.getMessageSource();
			if (getParentThemeSource() != null && messageSource.getParentMessageSource() == null) {
				Theme parentTheme = getParentThemeSource().getTheme(theme.getName());
				if (parentTheme != null) {
					messageSource.setParentMessageSource(parentTheme.getMessageSource());
				}
			}
		}
	}

}
