/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertiesPersister;
import org.springframework.lang.Nullable;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于指定基本名称访问资源包的Spring特定{@link org.springframework.context.MessageSource}实现，
 * 参与Spring {@link org.springframework.context.ApplicationContext}的资源加载。
 *
 * <p>与基于JDK的{@link ResourceBundleMessageSource}相比，此类使用{@link java.util.Properties}实例作为其消息的自定义数据结构，
 * 通过Spring {@link Resource}句柄从{@link org.springframework.util.PropertiesPersister}策略加载它们。
 * 此策略不仅能够根据时间戳更改重新加载文件，还能够加载具有特定字符编码的属性文件。它还将检测XML属性文件。
 *
 * <p>请注意，设置为{@link #setBasenames "basenames"}属性的基本名称与{@link ResourceBundleMessageSource}的“basenames”属性稍有不同。
 * 它遵循基本的ResourceBundle规则，不指定文件扩展名或语言代码，但可以引用任何Spring资源位置（而不限于类路径资源）。
 * 使用“classpath:”前缀，仍然可以从类路径加载资源，但在这种情况下，“cacheSeconds”值不是“-1”（永久缓存）可能不可靠。
 *
 * <p>对于典型的Web应用程序，消息文件可以放置在{@code WEB-INF}中：
 * 例如，“WEB-INF/messages”基本名称将找到“WEB-INF/messages.properties”，
 * “WEB-INF/messages_en.properties”等安排，以及“WEB-INF/messages.xml”，“WEB-INF/messages_en.xml”等。
 * 请注意，由于顺序查找，先前资源包中的消息定义将覆盖后续资源包中的消息定义。
 *
 * <p>此MessageSource可以轻松地在{@link org.springframework.context.ApplicationContext}之外使用：
 * 它将使用{@link org.springframework.core.io.DefaultResourceLoader}作为默认值，如果在上下文中运行，
 * 则简单地被ApplicationContext的资源加载器覆盖。
 * 它没有任何其他特定的依赖项。
 *
 * <p>感谢Thomas Achleitner提供此消息源的初始实现！
 *
 * @author Juergen Hoeller
 * @see #setCacheSeconds
 * @see #setBasenames
 * @see #setDefaultEncoding
 * @see #setFileEncodings
 * @see #setPropertiesPersister
 * @see #setResourceLoader
 * @see ResourcePropertiesPersister
 * @see org.springframework.core.io.DefaultResourceLoader
 * @see ResourceBundleMessageSource
 * @see java.util.ResourceBundle
 */
public class ReloadableResourceBundleMessageSource extends AbstractResourceBasedMessageSource
		implements ResourceLoaderAware {

	/**
	 * properties后缀
	 */
	private static final String PROPERTIES_SUFFIX = ".properties";

	/**
	 * XML前缀
	 */
	private static final String XML_SUFFIX = ".xml";

	/**
	 * 文件编码
	 */
	@Nullable
	private Properties fileEncodings;
	/**
	 * 是否允许并发刷新行为，默认为true
	 */
	private boolean concurrentRefresh = true;

	/**
	 * 属性持久化器
	 */
	private PropertiesPersister propertiesPersister = ResourcePropertiesPersister.INSTANCE;

	/**
	 * 资源加载器
	 */
	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	/**
	 * 缓存每个区域设置的文件名列表
	 */
	private final ConcurrentMap<String, Map<Locale, List<String>>> cachedFilenames = new ConcurrentHashMap<>();

	/**
	 * 缓存每个文件名已加载的属性
	 */
	private final ConcurrentMap<String, PropertiesHolder> cachedProperties = new ConcurrentHashMap<>();

	/**
	 * 缓存每个区域设置已加载的属性
	 */
	private final ConcurrentMap<Locale, PropertiesHolder> cachedMergedProperties = new ConcurrentHashMap<>();


	/**
	 * 设置每个文件的字符集，用于解析属性文件。
	 * <p>仅适用于经典属性文件，不适用于XML文件。
	 *
	 * @param fileEncodings 一个带有文件名作为键和字符集名称作为值的Properties。
	 *                      文件名必须与基本名称语法匹配，包括可选的特定于语言环境的组件：例如“WEB-INF/messages”或“WEB-INF/messages_en”。
	 * @see #setBasenames
	 * @see org.springframework.util.PropertiesPersister#load
	 */
	public void setFileEncodings(Properties fileEncodings) {
		this.fileEncodings = fileEncodings;
	}

	/**
	 * 指定是否允许并发刷新行为，即一个线程锁定在特定缓存的属性文件的刷新尝试中，而其他线程在此期间继续返回旧属性，直到刷新尝试完成。
	 * <p>默认为“true”：此行为是从Spring Framework 4.1开始的新行为，最大限度地减少了线程之间的争用。
	 * 如果您更喜欢旧行为，即完全阻止刷新，请将此标志切换为“false”。
	 *
	 * @see #setCacheSeconds
	 * @since 4.1
	 */
	public void setConcurrentRefresh(boolean concurrentRefresh) {
		this.concurrentRefresh = concurrentRefresh;
	}

	/**
	 * 设置用于解析属性文件的PropertiesPersister。
	 * <p>默认为ResourcePropertiesPersister。
	 *
	 * @see ResourcePropertiesPersister#INSTANCE
	 */
	public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
		this.propertiesPersister =
				(propertiesPersister != null ? propertiesPersister : ResourcePropertiesPersister.INSTANCE);
	}

	/**
	 * 设置用于加载捆绑包属性文件的ResourceLoader。
	 * <p>默认为DefaultResourceLoader。如果在上下文中运行，则会被ApplicationContext覆盖，因为它实现了ResourceLoaderAware接口。
	 * 在运行时如果不在ApplicationContext中，可以手动覆盖。
	 *
	 * @see org.springframework.core.io.DefaultResourceLoader
	 * @see org.springframework.context.ResourceLoaderAware
	 */
	@Override
	public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
		// 如果资源加载器存在，则使用它；否则使用默认的资源加载器
		this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
	}


	/**
	 * 将给定的消息代码作为键在检索到的捆绑包文件中解析，
	 * 返回在捆绑包中找到的值（无需MessageFormat解析）。
	 */
	@Override
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		// 如果缓存时间小于0
		if (getCacheMillis() < 0) {
			// 获取合并后的属性持有者
			PropertiesHolder propHolder = getMergedProperties(locale);
			// 获取属性持有者中消息代码的属性值
			String result = propHolder.getProperty(code);
			if (result != null) {
				// 如果属性值存在，则返回属性值
				return result;
			}
		} else {
			// 遍历每个基本名称
			for (String basename : getBasenameSet()) {
				// 计算指定基本名称和区域设置的所有文件名
				List<String> filenames = calculateAllFilenames(basename, locale);
				// 遍历每个文件名
				for (String filename : filenames) {
					// 获取指定文件名的属性持有者
					PropertiesHolder propHolder = getProperties(filename);
					// 获取属性持有者中消息代码的属性值
					String result = propHolder.getProperty(code);
					// 如果属性值不为null，则返回属性值
					if (result != null) {
						return result;
					}
				}
			}
		}
		// 如果没有找到消息代码的属性值，则返回null
		return null;
	}

	/**
	 * 将给定的消息代码作为键在检索到的捆绑包文件中解析，
	 * 使用每个消息代码的缓存的MessageFormat实例。
	 */
	@Override
	@Nullable
	protected MessageFormat resolveCode(String code, Locale locale) {
		if (getCacheMillis() < 0) {
			// 如果不使用缓存
			// 获取合并属性持有者
			PropertiesHolder propHolder = getMergedProperties(locale);
			//从该属性持有者中获取消息格式化器
			MessageFormat result = propHolder.getMessageFormat(code, locale);
			if (result != null) {
				// 消息格式化器存在，则返回该格式化器
				return result;
			}
		} else {
			// 使用缓存，遍历所有基本名对应的属性文件，获取消息格式
			for (String basename : getBasenameSet()) {
				// 根据基本名称和区域设置计算所有的文件名
				List<String> filenames = calculateAllFilenames(basename, locale);
				for (String filename : filenames) {
					// 根据文件名获取属性值持有者
					PropertiesHolder propHolder = getProperties(filename);
					// //从该属性持有者中获取消息格式化器
					MessageFormat result = propHolder.getMessageFormat(code, locale);
					if (result != null) {
						// 消息格式化器存在，则返回该格式化器
						return result;
					}
				}
			}
		}
		// 如果找不到消息格式，则返回null
		return null;
	}


	/**
	 * 获取包含实际可见属性的PropertiesHolder，用于区域设置，合并所有指定的资源包。
	 * 要么从缓存中获取持有者，要么新加载它。
	 * <p>仅在永久缓存资源包内容时使用，即cacheSeconds &lt;0。
	 * 因此，合并的属性始终永久缓存。
	 */
	protected PropertiesHolder getMergedProperties(Locale locale) {
		// 获取缓存的合并属性持有者
		PropertiesHolder mergedHolder = this.cachedMergedProperties.get(locale);
		if (mergedHolder != null) {
			return mergedHolder;
		}

		// 合并所有基本名对应的属性文件
		Properties mergedProps = newProperties();
		long latestTimestamp = -1;
		String[] basenames = StringUtils.toStringArray(getBasenameSet());
		for (int i = basenames.length - 1; i >= 0; i--) {
			// 计算所有的文件名列表
			List<String> filenames = calculateAllFilenames(basenames[i], locale);
			for (int j = filenames.size() - 1; j >= 0; j--) {
				// 挨个遍历文件名
				String filename = filenames.get(j);
				// 获取该文件名的属性值
				PropertiesHolder propHolder = getProperties(filename);
				if (propHolder.getProperties() != null) {
					// 如果属性值存在，则添加到合并属性值中。
					mergedProps.putAll(propHolder.getProperties());
					if (propHolder.getFileTimestamp() > latestTimestamp) {
						// 如果当前更新时间大于最新更新时间，则设置为最新更新时间
						latestTimestamp = propHolder.getFileTimestamp();
					}
				}
			}
		}

		// 创建合并属性持有者
		mergedHolder = new PropertiesHolder(mergedProps, latestTimestamp);
		// 将合并属性持有者放入缓存
		PropertiesHolder existing = this.cachedMergedProperties.putIfAbsent(locale, mergedHolder);
		if (existing != null) {
			// 当前区域已经存在该属性瞅着，则返回原来的属性持有者
			mergedHolder = existing;
		}
		return mergedHolder;
	}

	/**
	 * 计算给定的捆绑包基本名称和区域设置的所有文件名。
	 * 将为给定的区域设置、系统区域设置（如果适用）和默认文件计算文件名。
	 *
	 * @param basename 捆绑包的基本名称
	 * @param locale   区域设置
	 * @return 要检查的文件名列表
	 * @see #setFallbackToSystemLocale
	 * @see #calculateFilenamesForLocale
	 */
	protected List<String> calculateAllFilenames(String basename, Locale locale) {
		// 获取缓存的文件名列表
		Map<Locale, List<String>> localeMap = this.cachedFilenames.get(basename);
		if (localeMap != null) {
			// 如果区域设置映射存在，获取文件名列表
			List<String> filenames = localeMap.get(locale);
			if (filenames != null) {
				// 文件名列表存在，直接返回
				return filenames;
			}
		}

		// 给定区域设置的文件名列表
		List<String> filenames = new ArrayList<>(7);
		// 添加从区域设置的获取到文件名列表
		filenames.addAll(calculateFilenamesForLocale(basename, locale));

		// 如果存在默认区域设置，则添加默认区域设置的文件名到列表中
		Locale defaultLocale = getDefaultLocale();
		if (defaultLocale != null && !defaultLocale.equals(locale)) {
			// 如果默认区域设置存在，并且当前不是默认区域设置
			// 从默认的区域设置获取文件名列表
			List<String> fallbackFilenames = calculateFilenamesForLocale(basename, defaultLocale);
			for (String fallbackFilename : fallbackFilenames) {
				if (!filenames.contains(fallbackFilename)) {
					// 如果不在文件名列表中，则添加该回退的文件名
					filenames.add(fallbackFilename);
				}
			}
		}

		// 添加默认捆绑包文件的文件名到列表中
		filenames.add(basename);

		// 将文件名列表与对应的区域设置缓存起来
		if (localeMap == null) {
			localeMap = new ConcurrentHashMap<>();
			Map<Locale, List<String>> existing = this.cachedFilenames.putIfAbsent(basename, localeMap);
			if (existing != null) {
				localeMap = existing;
			}
		}
		localeMap.put(locale, filenames);
		return filenames;
	}

	/**
	 * 计算给定的捆绑包基本名称和区域设置的文件名，
	 * 添加语言代码、国家代码和变体代码。
	 * <p>例如，基本名称为“messages”，区域设置为“de_AT_oo” →“messages_de_AT_OO”，
	 * “messages_de_AT”，“messages_de”。
	 * <p>遵循{@link java.util.Locale#toString()}定义的规则。
	 *
	 * @param basename 捆绑包的基本名称
	 * @param locale   区域设置
	 * @return 要检查的文件名列表
	 */
	protected List<String> calculateFilenamesForLocale(String basename, Locale locale) {
		// 创建一个容量为3的ArrayList来存储结果
		List<String> result = new ArrayList<>(3);
		// 获取区域设置的语言、国家和变体
		String language = locale.getLanguage();
		String country = locale.getCountry();
		String variant = locale.getVariant();
		// 创建一个StringBuilder来构建基本名称
		StringBuilder temp = new StringBuilder(basename);

		// 添加语言部分
		temp.append('_');
		if (language.length() > 0) {
			temp.append(language);
			result.add(0, temp.toString());
		}

		// 添加国家部分
		temp.append('_');
		if (country.length() > 0) {
			temp.append(country);
			result.add(0, temp.toString());
		}

		// 如果变体不为空且语言或国家不为空，则添加变体部分
		if (variant.length() > 0 && (language.length() > 0 || country.length() > 0)) {
			temp.append('_').append(variant);
			result.add(0, temp.toString());
		}

		// 返回结果列表
		return result;
	}


	/**
	 * 获取给定文件名的PropertiesHolder，从缓存中获取或刷新加载。
	 *
	 * @param filename 捆绑包文件名（基本名称 + 区域设置）
	 * @return 捆绑包的当前PropertiesHolder
	 */
	protected PropertiesHolder getProperties(String filename) {
		// 从缓存中获取属性持有者
		PropertiesHolder propHolder = this.cachedProperties.get(filename);
		// 初始化原始时间戳为-2
		long originalTimestamp = -2;

		// 如果属性持有者不为空
		if (propHolder != null) {
			// 获取属性持有者的刷新时间戳
			originalTimestamp = propHolder.getRefreshTimestamp();
			if (originalTimestamp == -1 || originalTimestamp > System.currentTimeMillis() - getCacheMillis()) {
				// 如果刷新时间戳为-1（永远不过期）或者刷新时间间隔没到，则直接返回属性持有者
				return propHolder;
			}
		} else {
			// 如果属性持有者为空，则创建一个新的空属性持有者
			propHolder = new PropertiesHolder();
			// 放入到缓存中
			PropertiesHolder existingHolder = this.cachedProperties.putIfAbsent(filename, propHolder);
			if (existingHolder != null) {
				propHolder = existingHolder;
			}
		}

		// 到达此点，需要刷新...
		// 如果允许并发刷新且属性持有者已经填充且过时，则尝试获取刷新锁
		if (this.concurrentRefresh && propHolder.getRefreshTimestamp() >= 0) {
			// 一个已填充但陈旧的持有者 -> 可能继续使用它。
			if (!propHolder.refreshLock.tryLock()) {
				// 已被其他线程刷新 -> 暂时返回现有属性。
				return propHolder;
			}
		} else {
			// 否则，获取刷新锁
			propHolder.refreshLock.lock();
		}
		try {
			// 再次检查缓存中的属性持有者是否已被其他线程刷新
			PropertiesHolder existingHolder = this.cachedProperties.get(filename);
			if (existingHolder != null && existingHolder.getRefreshTimestamp() > originalTimestamp) {
				// 如果缓存中已经存在该持有者，并且到了还没到刷新时间，则返回该持有者
				return existingHolder;
			}
			// 刷新属性文件并返回更新后的属性持有者
			return refreshProperties(filename, propHolder);
		} finally {
			// 释放刷新锁
			propHolder.refreshLock.unlock();
		}
	}

	/**
	 * 刷新给定捆绑包文件名的PropertiesHolder。
	 * 如果之前未缓存，holder可以为null，或者是超时的缓存条目
	 * （可能会根据当前的最后修改时间戳进行重新验证）。
	 *
	 * @param filename   捆绑包文件名（基本名称 + 区域设置）
	 * @param propHolder 捆绑包的当前PropertiesHolder
	 */
	protected PropertiesHolder refreshProperties(String filename, @Nullable PropertiesHolder propHolder) {
		// 计算刷新时间戳，如果无缓存，则设置为-1
		long refreshTimestamp = (getCacheMillis() < 0 ? -1 : System.currentTimeMillis());

		// 获取属性文件资源
		Resource resource = this.resourceLoader.getResource(filename + PROPERTIES_SUFFIX);
		if (!resource.exists()) {
			// 如果属性文件不存在，则尝试查找XML文件
			resource = this.resourceLoader.getResource(filename + XML_SUFFIX);
		}

		// 如果找到资源文件
		if (resource.exists()) {
			long fileTimestamp = -1;
			// 如果设置了缓存时间，则检查文件的最后修改时间戳
			if (getCacheMillis() >= 0) {
				// 如果缓存超时，读取文件的最后修改时间戳。
				try {
					fileTimestamp = resource.lastModified();
					// 如果属性文件的最后修改时间戳与之前的缓存一致，则直接返回缓存的PropertiesHolder
					if (propHolder != null && propHolder.getFileTimestamp() == fileTimestamp) {
						if (logger.isDebugEnabled()) {
							logger.debug("Re-caching properties for filename [" + filename + "] - file hasn't been modified");
						}
						// 设置刷新时间戳
						propHolder.setRefreshTimestamp(refreshTimestamp);
						return propHolder;
					}
				} catch (IOException ex) {
					// 可能是类路径资源：永久缓存。
					if (logger.isDebugEnabled()) {
						logger.debug(resource + " could not be resolved in the file system - assuming that it hasn't changed", ex);
					}
					fileTimestamp = -1;
				}
			}
			try {
				// 加载属性文件，并创建一个新的PropertiesHolder
				Properties props = loadProperties(resource, filename);
				propHolder = new PropertiesHolder(props, fileTimestamp);
			} catch (IOException ex) {
				// 加载属性文件失败，记录警告信息，并创建一个空的PropertiesHolder
				if (logger.isWarnEnabled()) {
					logger.warn("Could not parse properties file [" + resource.getFilename() + "]", ex);
				}
				// 表示“无效”的空持有者。
				propHolder = new PropertiesHolder();
			}
		} else {
			// 如果资源文件不存在，记录调试信息，并创建一个空的PropertiesHolder
			if (logger.isDebugEnabled()) {
				logger.debug("No properties file found for [" + filename + "] - neither plain properties nor XML");
			}
			// 表示“未找到”的空持有者。
			propHolder = new PropertiesHolder();
		}

		// 设置刷新时间戳
		propHolder.setRefreshTimestamp(refreshTimestamp);
		// 将文件名和缓存持有者缓存起来
		this.cachedProperties.put(filename, propHolder);
		return propHolder;
	}

	/**
	 * 从给定资源加载属性。
	 *
	 * @param resource 要加载的资源
	 * @param filename 原始捆绑包文件名（基本名称 + 区域设置）
	 * @return 填充的Properties实例
	 * @throws IOException 如果加载属性失败
	 */
	protected Properties loadProperties(Resource resource, String filename) throws IOException {
		// 创建一个新的Properties对象
		Properties props = newProperties();
		// 读取资源文件的内容
		try (InputStream is = resource.getInputStream()) {
			// 获取资源文件的文件名
			String resourceFilename = resource.getFilename();
			// 如果资源文件名以 .xml 文件结尾
			if (resourceFilename != null && resourceFilename.endsWith(XML_SUFFIX)) {
				// 如果日志级别为DEBUG，则记录加载XML文件信息
				if (logger.isDebugEnabled()) {
					logger.debug("Loading properties [" + resource.getFilename() + "]");
				}
				// 从XML格式加载属性
				this.propertiesPersister.loadFromXml(props, is);
			} else {
				// 否则，尝试根据编码加载属性
				String encoding = null;
				// 如果文件编码集不为空，尝试从中获取文件编码
				if (this.fileEncodings != null) {
					encoding = this.fileEncodings.getProperty(filename);
				}
				if (encoding == null) {
					// 如果文件编码为null，则使用默认编码
					encoding = getDefaultEncoding();
				}
				// 如果获取到了编码，则使用指定编码加载属性
				if (encoding != null) {
					// 如果日志级别为DEBUG，则记录加载带编码的属性文件信息
					if (logger.isDebugEnabled()) {
						logger.debug("Loading properties [" + resource.getFilename() + "] with encoding '" + encoding + "'");
					}
					// 从带有指定编码的Reader中加载属性
					this.propertiesPersister.load(props, new InputStreamReader(is, encoding));
				} else {
					// 否则，使用默认方式加载属性文件
					if (logger.isDebugEnabled()) {
						logger.debug("Loading properties [" + resource.getFilename() + "]");
					}
					// 从InputStream中加载属性
					this.propertiesPersister.load(props, is);
				}
			}
			// 返回加载后的Properties对象
			return props;
		}
	}

	/**
	 * 创建一个普通的新{@link Properties}实例的模板方法。
	 * 默认实现只调用{@link Properties#Properties()}。
	 * <p>允许在子类中返回自定义的{@link Properties}扩展。
	 * 覆盖方法应该只实例化一个自定义的{@link Properties}子类，
	 * 在此时不应进行其他初始化或填充。
	 *
	 * @return 一个普通的Properties实例
	 * @since 4.2
	 */
	protected Properties newProperties() {
		return new Properties();
	}


	/**
	 * 清除资源捆绑包缓存。
	 * 后续的解析调用将导致重新加载属性文件。
	 */
	public void clearCache() {
		logger.debug("Clearing entire resource bundle cache");
		// 清除缓存属性
		this.cachedProperties.clear();
		// 清除合并属性
		this.cachedMergedProperties.clear();
	}

	/**
	 * 清除此消息源及其所有祖先的资源捆绑包缓存。
	 *
	 * @see #clearCache
	 */
	public void clearCacheIncludingAncestors() {
		// 清除缓存
		clearCache();
		// 如果父消息源是可重新加载的资源包消息源
		if (getParentMessageSource() instanceof ReloadableResourceBundleMessageSource) {
			// 清除包括祖先在内的所有缓存
			((ReloadableResourceBundleMessageSource) getParentMessageSource()).clearCacheIncludingAncestors();
		}
	}


	@Override
	public String toString() {
		return getClass().getName() + ": basenames=" + getBasenameSet();
	}


	/**
	 * 用于缓存的PropertiesHolder。
	 * 存储源文件的上次修改时间戳，以进行高效的更改检测，
	 * 以及上次刷新尝试的时间戳（每次缓存条目被重新验证时更新）。
	 */
	protected class PropertiesHolder {

		/**
		 * 属性值
		 */
		@Nullable
		private final Properties properties;

		/**
		 * 文件时间戳
		 */
		private final long fileTimestamp;

		/**
		 * 刷新时间戳
		 */
		private volatile long refreshTimestamp = -2;

		/**
		 * 刷新锁
		 */
		private final ReentrantLock refreshLock = new ReentrantLock();

		/**
		 * 用于缓存的ConcurrentMap，存储每个消息代码的已生成的MessageFormats。
		 */
		private final ConcurrentMap<String, Map<Locale, MessageFormat>> cachedMessageFormats =
				new ConcurrentHashMap<>();

		public PropertiesHolder() {
			this.properties = null;
			this.fileTimestamp = -1;
		}

		public PropertiesHolder(Properties properties, long fileTimestamp) {
			this.properties = properties;
			this.fileTimestamp = fileTimestamp;
		}

		@Nullable
		public Properties getProperties() {
			return this.properties;
		}

		public long getFileTimestamp() {
			return this.fileTimestamp;
		}

		public void setRefreshTimestamp(long refreshTimestamp) {
			this.refreshTimestamp = refreshTimestamp;
		}

		public long getRefreshTimestamp() {
			return this.refreshTimestamp;
		}

		@Nullable
		public String getProperty(String code) {
			if (this.properties == null) {
				return null;
			}
			return this.properties.getProperty(code);
		}

		@Nullable
		public MessageFormat getMessageFormat(String code, Locale locale) {
			// 如果属性文件为空，则返回null
			if (this.properties == null) {
				return null;
			}
			// 获取缓存的区域设置-消息格式映射
			Map<Locale, MessageFormat> localeMap = this.cachedMessageFormats.get(code);
			if (localeMap != null) {
				// 如果映射存在
				// 从映射中获取指定区域设置的消息格式
				MessageFormat result = localeMap.get(locale);
				if (result != null) {
					// 如果消息格式不为null，则返回消息格式
					return result;
				}
			}
			// 从属性文件中获取消息代码的消息
			String msg = this.properties.getProperty(code);
			// 如果消息存在
			if (msg != null) {
				// 如果消息格式映射为null
				if (localeMap == null) {
					// 创建一个新的消息格式映射并放入缓存中
					localeMap = new ConcurrentHashMap<>();
					// 添加进缓存中
					Map<Locale, MessageFormat> existing = this.cachedMessageFormats.putIfAbsent(code, localeMap);
					if (existing != null) {
						localeMap = existing;
					}
				}
				// 创建消息格式并放入映射中
				MessageFormat result = createMessageFormat(msg, locale);
				localeMap.put(locale, result);
				return result;
			}
			// 如果未找到指定代码的消息，则返回null
			return null;
		}
	}

}
