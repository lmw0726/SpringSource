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

package org.springframework.context.support;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 基于资源包约定的{@code MessageSource}实现的抽象基类，例如{@link ResourceBundleMessageSource}
 * 和{@link ReloadableResourceBundleMessageSource}。提供通用的配置方法和相应的语义定义。
 *
 * @author Juergen Hoeller
 * @see ResourceBundleMessageSource
 * @see ReloadableResourceBundleMessageSource
 * @since 4.3
 */
public abstract class AbstractResourceBasedMessageSource extends AbstractMessageSource {

	/**
	 * 处理好的基本名称集合
	 */
	private final Set<String> basenameSet = new LinkedHashSet<>(4);
	/**
	 * 默认编码格式
	 */
	@Nullable
	private String defaultEncoding;

	/**
	 * 是否回退到系统区域设置
	 */
	private boolean fallbackToSystemLocale = true;

	/**
	 * 默认的区域设置
	 */
	@Nullable
	private Locale defaultLocale;

	/**
	 * 加载的属性文件的缓存时间（毫秒），默认不缓存
	 */
	private long cacheMillis = -1;


	/**
	 * 设置单个基本名称，遵循不指定文件扩展名或语言代码的基本ResourceBundle约定。
	 * 资源位置格式由特定的{@code MessageSource}实现确定。
	 * <p>支持常规和XML属性文件：例如，“messages”将找到“messages.properties”，
	 * “messages_en.properties”等安排以及“messages.xml”，“messages_en.xml”等。
	 *
	 * @param basename 单个基本名称
	 * @see #setBasenames
	 * @see org.springframework.core.io.ResourceEditor
	 * @see java.util.ResourceBundle
	 */
	public void setBasename(String basename) {
		setBasenames(basename);
	}

	/**
	 * 设置一个基本名称数组，每个名称都遵循不指定文件扩展名或语言代码的基本ResourceBundle约定。
	 * 资源位置格式由特定的{@code MessageSource}实现确定。
	 * <p>支持常规和XML属性文件：例如，“messages”将找到“messages.properties”，
	 * “messages_en.properties”等安排以及“messages.xml”，“messages_en.xml”等。
	 * <p>在解析消息代码时，将顺序检查相关联的资源包。注意，资源包中的消息定义会覆盖较后的资源包中的消息定义，因为它们是顺序查找的。
	 * <p>注意：与{@link #addBasenames}相比，此方法会替换现有名称的条目，并且因此也可用于重置配置。
	 *
	 * @param basenames 基本名称数组
	 * @see #setBasename
	 * @see java.util.ResourceBundle
	 */
	public void setBasenames(String... basenames) {
		this.basenameSet.clear();
		addBasenames(basenames);
	}

	/**
	 * 向现有基本名称配置添加指定的基本名称。
	 * <p>注意：如果给定的基本名称已存在，则其条目的位置将保持与原始集合中的位置相同。
	 * 新条目将添加到列表的末尾，以便在现有基本名称后进行搜索。
	 *
	 * @param basenames 要添加的基本名称
	 * @see #setBasenames
	 * @see java.util.ResourceBundle
	 * @since 4.3
	 */
	public void addBasenames(String... basenames) {
		// 如果 基本名称集合 不为空
		if (!ObjectUtils.isEmpty(basenames)) {
			// 遍历 基本名称集合
			for (String basename : basenames) {
				// 检查 基本名称 是否为空
				Assert.hasText(basename, "Basename must not be empty");
				// 将去除首尾空格后的 基本名称 添加到 处理好的基本名称集合 中
				this.basenameSet.add(basename.trim());
			}
		}
	}

	/**
	 * 返回此{@code MessageSource}的基本名称集合，按注册顺序排序。
	 * 调用代码可以检查此集合，以及添加或删除条目。
	 *
	 * @see #addBasenames
	 * @since 4.3
	 */
	public Set<String> getBasenameSet() {
		return this.basenameSet;
	}

	/**
	 * 设置用于解析属性文件的默认字符集。
	 * 如果没有为文件指定特定的字符集，则使用该字符集。
	 * <p>有效的默认值是{@code java.util.Properties}的默认编码：ISO-8859-1。
	 * {@code null}值表示平台默认编码。
	 * <p>仅适用于经典属性文件，不适用于XML文件。
	 *
	 * @param defaultEncoding 默认字符集
	 */
	public void setDefaultEncoding(@Nullable String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * 返回用于解析属性文件的默认字符集，如果有的话。
	 *
	 * @since 4.3
	 */
	@Nullable
	protected String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * 设置是否在没有找到特定区域设置的文件时返回到系统区域设置。
	 * 默认为“true”；如果关闭此选项，唯一的回退将是默认文件（例如，对于基本名称“messages”，是“messages.properties”）。
	 * <p>回退到系统区域设置是{@code java.util.ResourceBundle}的默认行为。
	 * 但是，在应用程序服务器环境中通常不希望这样做，因为系统区域设置与应用程序无关：在这种情况下，将此标志设置为“false”。
	 *
	 * @see #setDefaultLocale
	 */
	public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
		this.fallbackToSystemLocale = fallbackToSystemLocale;
	}

	/**
	 * 返回是否在没有找到特定区域设置的文件时返回到系统区域设置。
	 *
	 * @since 4.3
	 * @deprecated 从5.2.2开始，改用{@link #getDefaultLocale()}
	 */
	@Deprecated
	protected boolean isFallbackToSystemLocale() {
		return this.fallbackToSystemLocale;
	}

	/**
	 * 指定要返回到的默认区域设置，作为返回到系统区域设置的替代方案。
	 * 默认情况下返回系统区域设置。
	 * 您可以在此处用本地指定的默认区域设置覆盖此设置，也可以通过禁用“fallbackToSystemLocale”来强制不返回到回退区域设置。
	 *
	 * @see #setFallbackToSystemLocale
	 * @see #getDefaultLocale()
	 * @since 5.2.2
	 */
	public void setDefaultLocale(@Nullable Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * 确定要返回到的默认区域设置：可以是本地指定的默认区域设置，也可以是系统区域设置，也可以是{@code null}以根本不返回到回退区域设置。
	 *
	 * @see #setDefaultLocale
	 * @see #setFallbackToSystemLocale
	 * @see Locale#getDefault()
	 * @since 5.2.2
	 */
	@Nullable
	protected Locale getDefaultLocale() {
		if (this.defaultLocale != null) {
			// 如果默认的区域设置存在，则返回该区域设置
			return this.defaultLocale;
		}
		if (this.fallbackToSystemLocale) {
			// 如果要回退到系统区域设置，则返回系统默认的区域设置
			return Locale.getDefault();
		}
		return null;
	}

	/**
	 * 设置加载的属性文件的缓存时间（以秒为单位）。
	 * <ul>
	 * <li>默认值为“-1”，表示永久缓存（与{@code java.util.ResourceBundle}的默认行为相匹配）。
	 * 请注意，此常量遵循Spring约定，而不是{@link java.util.ResourceBundle.Control#getTimeToLive}。
	 * <li>正数将加载的属性文件缓存给定秒数。这基本上是刷新检查之间的间隔。
	 * 请注意，刷新尝试首先检查文件的最后修改时间戳，然后才会重新加载它；因此，如果文件不更改，则此间隔可以设置得相当低，因为刷新尝试实际上不会重新加载。
	 * <li>值为“0”将在每次消息访问时检查文件的最后修改时间戳。<b>不要在生产环境中使用此选项！</b>
	 * </ul>
	 * <p><b>请注意，根据您的ClassLoader，到期可能无法可靠工作，
	 * 因为ClassLoader可能会保留对包文件的缓存版本。</b>在这种情况下，请优先使用{@link ReloadableResourceBundleMessageSource}而不是
	 * {@link ResourceBundleMessageSource}，结合使用非类路径位置。
	 *
	 * @param cacheSeconds 缓存时间（秒）
	 */
	public void setCacheSeconds(int cacheSeconds) {
		this.cacheMillis = cacheSeconds * 1000L;
	}

	/**
	 * 设置加载的属性文件的缓存时间（以毫秒为单位）。
	 * 请注意，通常设置秒而不是毫秒：{@link #setCacheSeconds}。
	 * <ul>
	 * <li>默认值为“-1”，表示永久缓存（与{@code java.util.ResourceBundle}的默认行为相匹配）。
	 * 请注意，此常量遵循Spring约定，而不是{@link java.util.ResourceBundle.Control#getTimeToLive}。
	 * <li>正数将加载的属性文件缓存给定毫秒数。这基本上是刷新检查之间的间隔。
	 * 请注意，刷新尝试首先检查文件的最后修改时间戳，然后才会重新加载它；因此，如果文件不更改，则此间隔可以设置得相当低，因为刷新尝试实际上不会重新加载。
	 * <li>值为“0”将在每次消息访问时检查文件的最后修改时间戳。<b>不要在生产环境中使用此选项！</b>
	 * </ul>
	 *
	 * @see #setCacheSeconds
	 * @since 4.3
	 */
	public void setCacheMillis(long cacheMillis) {
		this.cacheMillis = cacheMillis;
	}

	/**
	 * 返回加载的属性文件的缓存时间（毫秒）。
	 *
	 * @since 4.3
	 */
	protected long getCacheMillis() {
		return this.cacheMillis;
	}

}
