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

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link org.springframework.context.MessageSource}实现，使用指定的基名访问资源包。
 * 该类依赖于底层的JDK的{@link java.util.ResourceBundle}实现，以及由{@link java.text.MessageFormat}提供的JDK的标准消息解析。
 *
 * <p>该MessageSource缓存了访问的ResourceBundle实例和每个消息生成的MessageFormats。
 * 它还实现了AbstractMessageSource基类支持的不带参数的消息的渲染，这种缓存比{@code java.util.ResourceBundle}类内置的缓存快得多。
 *
 * <p>基名遵循{@link java.util.ResourceBundle}约定：基本上是完全限定的类路径位置。
 * 如果它不包含包限定符（例如{@code org.mypackage}），则将从类路径根目录解析。
 * 请注意，JDK的标准ResourceBundle将点视为包分隔符：这意味着"test.theme"实际上等效于"test/theme"。
 *
 * <p>在类路径上，资源包资源将使用本地配置的{@link #setDefaultEncoding encoding}读取：默认情况下是ISO-8859-1；
 * 考虑切换到UTF-8，或者切换到平台默认编码{@code null}。
 * 在JDK 9+模块路径上，本地提供的{@code ResourceBundle.Control}处理程序不受支持，
 * 该MessageSource始终回退到使用平台默认编码的{@link ResourceBundle#getBundle}检索：JDK 9+上的UTF-8（可通过"java.util.PropertyResourceBundle.encoding"系统属性配置），
 * 并在JDK 9+上回退到ISO-8859-1。请注意，在这种情况下，也不会调用{@link #loadBundle(Reader)}/{@link #loadBundle(InputStream)}，
 * 从而有效地忽略了子类中的重写。考虑改为实现JDK 9 {@code java.util.spi.ResourceBundleProvider}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Qimiao Chen
 * @see #setBasenames
 * @see ReloadableResourceBundleMessageSource
 * @see java.util.ResourceBundle
 * @see java.text.MessageFormat
 */
public class ResourceBundleMessageSource extends AbstractResourceBasedMessageSource implements BeanClassLoaderAware {
	/**
	 * 包类加载
	 */
	@Nullable
	private ClassLoader bundleClassLoader;

	/**
	 * bean类加载器
	 */
	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/**
	 * 用于保存加载的ResourceBundles的缓存。
	 * 此Map的键是bundle基本名称，其中包含一个键为Locale的Map，进而包含ResourceBundle实例。
	 * 这允许非常有效的哈希查找，比ResourceBundle类本身的缓存快得多。
	 */
	private final Map<String, Map<Locale, ResourceBundle>> cachedResourceBundles =
			new ConcurrentHashMap<>();

	/**
	 * 用于保存已生成的MessageFormats的缓存。
	 * 此Map的键是ResourceBundle，其中包含一个键为消息代码的Map，
	 * 再包含一个键为Locale的Map，保存MessageFormat值。这允许非常高效的哈希查找，而无需串联键。
	 *
	 * @see #getMessageFormat
	 */
	private final Map<ResourceBundle, Map<String, Map<Locale, MessageFormat>>> cachedBundleMessageFormats =
			new ConcurrentHashMap<>();

	/**
	 * 消息控制
	 */
	@Nullable
	private volatile MessageSourceControl control = new MessageSourceControl();


	public ResourceBundleMessageSource() {
		setDefaultEncoding("ISO-8859-1");
	}


	/**
	 * 设置用于加载资源包的ClassLoader。
	 * <p>默认值是包含BeanFactory的
	 * {@link org.springframework.beans.factory.BeanClassLoaderAware bean ClassLoader}，
	 * 或者如果不在BeanFactory中运行，则是由
	 * {@link org.springframework.util.ClassUtils#getDefaultClassLoader()}确定的默认ClassLoader。
	 */
	public void setBundleClassLoader(ClassLoader classLoader) {
		this.bundleClassLoader = classLoader;
	}

	/**
	 * 返回用于加载资源包的ClassLoader。
	 * <p>默认值是包含BeanFactory的bean ClassLoader。
	 *
	 * @see #setBundleClassLoader
	 */
	@Nullable
	protected ClassLoader getBundleClassLoader() {
		return (this.bundleClassLoader != null ? this.bundleClassLoader : this.beanClassLoader);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	/**
	 * 将给定的消息代码解析为已注册资源包中的键，
	 * 返回在包中找到的值（不进行MessageFormat解析）。
	 */
	@Override
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		// 获取基本名称集合
		Set<String> basenames = getBasenameSet();
		for (String basename : basenames) {
			// 遍历基本名称集合，获取资源包
			ResourceBundle bundle = getResourceBundle(basename, locale);
			if (bundle != null) {
				// 如果资源包不为空，则从资源包中获取指定代码的字符串
				String result = getStringOrNull(bundle, code);
				if (result != null) {
					// 如果找到了字符串，则返回结果
					return result;
				}
			}
		}
		// 如果在所有基本名称中都未找到指定代码的字符串，则返回 null
		return null;
	}

	/**
	 * 将给定的消息代码解析为已注册资源包中的键，
	 * 使用每个消息代码的缓存的MessageFormat实例。
	 */
	@Override
	@Nullable
	protected MessageFormat resolveCode(String code, Locale locale) {
		// 获取基本名称集合
		Set<String> basenames = getBasenameSet();
		for (String basename : basenames) {
			// 遍历基本名称集合，获取资源包
			ResourceBundle bundle = getResourceBundle(basename, locale);
			if (bundle != null) {
				// 如果资源包不为空，则从资源包中获取指定代码的消息格式
				MessageFormat messageFormat = getMessageFormat(bundle, code, locale);
				if (messageFormat != null) {
					// 如果找到了消息格式，则返回结果
					return messageFormat;
				}
			}
		}
		// 如果在所有基本名称中都未找到指定代码的消息格式，则返回 null
		return null;
	}


	/**
	 * 为给定的basename和Locale返回ResourceBundle，
	 * 从缓存中获取已生成的ResourceBundle。
	 *
	 * @param basename ResourceBundle的basename
	 * @param locale   要查找ResourceBundle的Locale
	 * @return 结果ResourceBundle，如果未找到给定basename和Locale的ResourceBundle，则为{@code null}
	 */
	@Nullable
	protected ResourceBundle getResourceBundle(String basename, Locale locale) {
		if (getCacheMillis() >= 0) {
			// 使用全新的ResourceBundle.getBundle调用，以便让ResourceBundle执行其本机缓存，但代价是更多的查找步骤。
			return doGetBundle(basename, locale);
		} else {
			// 永久缓存：优先使用locale缓存，而不是重复的getBundle调用。
			Map<Locale, ResourceBundle> localeMap = this.cachedResourceBundles.get(basename);
			if (localeMap != null) {
				// 获取缓存中的资源包
				ResourceBundle bundle = localeMap.get(locale);
				if (bundle != null) {
					// 如果该资源包存在，则直接返回它
					return bundle;
				}
			}
			try {
				// 根据基本名称和区域设置获取资源包
				ResourceBundle bundle = doGetBundle(basename, locale);
				// 如果成功获取到资源包，则将其存储在缓存中
				if (localeMap == null) {
					localeMap = this.cachedResourceBundles.computeIfAbsent(basename, bn -> new ConcurrentHashMap<>());
				}
				localeMap.put(locale, bundle);
				// 返回获取到的资源包
				return bundle;
			} catch (MissingResourceException ex) {
				// 如果资源包未找到，则记录警告信息并返回 null
				if (logger.isWarnEnabled()) {
					logger.warn("ResourceBundle [" + basename + "] not found for MessageSource: " + ex.getMessage());
				}
				// 假设bundle未找到-> 不要抛出异常以允许检查父消息源。
				return null;
			}
		}
	}

	/**
	 * 获取给定basename和Locale的资源包。
	 *
	 * @param basename 要查找的basename
	 * @param locale   要查找的Locale
	 * @return 对应的ResourceBundle
	 * @throws MissingResourceException 如果找不到匹配的bundle
	 * @see java.util.ResourceBundle#getBundle(String, Locale, ClassLoader)
	 * @see #getBundleClassLoader()
	 */
	protected ResourceBundle doGetBundle(String basename, Locale locale) throws MissingResourceException {
		// 获取资源包类加载器
		ClassLoader classLoader = getBundleClassLoader();
		Assert.state(classLoader != null, "No bundle ClassLoader set");

		MessageSourceControl control = this.control;
		if (control != null) {
			try {
				// 如果资源控制存在，根据资源控制、基本名称、区域设置、类加载器获取资源包
				return ResourceBundle.getBundle(basename, locale, classLoader, control);
			} catch (UnsupportedOperationException ex) {
				// 可能在JDK 9+的Jigsaw环境中
				this.control = null;
				String encoding = getDefaultEncoding();
				if (encoding != null && logger.isInfoEnabled()) {
					logger.info("ResourceBundleMessageSource is configured to read resources with encoding '" +
							encoding + "' but ResourceBundle.Control not supported in current system environment: " +
							ex.getMessage() + " - falling back to plain ResourceBundle.getBundle retrieval with the " +
							"platform default encoding. Consider setting the 'defaultEncoding' property to 'null' " +
							"for participating in the platform default and therefore avoiding this log message.");
				}
			}
		}

		// 回退：没有Control句柄的普通getBundle查找
		return ResourceBundle.getBundle(basename, locale, classLoader);
	}

	/**
	 * 从给定的Reader中加载基于属性的资源包。
	 * <p>这将在{@link #setDefaultEncoding "defaultEncoding"}的情况下调用，
	 * 包括{@link ResourceBundleMessageSource}的默认ISO-8859-1编码。
	 * 请注意，此方法只能使用{@code ResourceBundle.Control}调用：
	 * 当在JDK 9+模块路径上运行时，此类控制句柄不受支持，
	 * 自定义子类中的任何覆盖实际上将被忽略。
	 * <p>默认实现返回一个{@link PropertyResourceBundle}。
	 *
	 * @param reader 目标资源的Reader
	 * @return 完全加载的bundle
	 * @throws IOException 如果I/O失败
	 * @see #loadBundle(InputStream)
	 * @see PropertyResourceBundle#PropertyResourceBundle(Reader)
	 * @since 4.2
	 */
	protected ResourceBundle loadBundle(Reader reader) throws IOException {
		return new PropertyResourceBundle(reader);
	}

	/**
	 * 从给定的输入流中加载基于属性的资源包，获取JDK 9+上的默认属性编码。
	 * <p>这将仅在将{@link #setDefaultEncoding "defaultEncoding"}设置为{@code null}时调用，
	 * 明确强制执行平台默认编码（JDK 9+上的UTF-8并具有ISO-8859-1回退，但可以通过“java.util.PropertyResourceBundle.encoding”系统属性配置）。
	 * 请注意，此方法只能使用{@code ResourceBundle.Control}调用：
	 * 当在JDK 9+模块路径上运行时，此类控制句柄不受支持，
	 * 自定义子类中的任何覆盖实际上将被忽略。
	 * <p>默认实现返回一个{@link PropertyResourceBundle}。
	 *
	 * @param inputStream 目标资源的输入流
	 * @return 完全加载的bundle
	 * @throws IOException 如果I/O失败
	 * @see #loadBundle(Reader)
	 * @see PropertyResourceBundle#PropertyResourceBundle(InputStream)
	 * @since 5.1
	 */
	protected ResourceBundle loadBundle(InputStream inputStream) throws IOException {
		return new PropertyResourceBundle(inputStream);
	}

	/**
	 * 为给定的bundle和code返回一个MessageFormat，
	 * 从缓存中获取已生成的MessageFormats。
	 *
	 * @param bundle 要处理的ResourceBundle
	 * @param code   要检索的消息代码
	 * @param locale 用于构建MessageFormat的Locale
	 * @return 结果MessageFormat，如果给定代码没有定义消息，则为{@code null}
	 * @throws MissingResourceException 如果由ResourceBundle抛出
	 */
	@Nullable
	protected MessageFormat getMessageFormat(ResourceBundle bundle, String code, Locale locale)
			throws MissingResourceException {

		Map<String, Map<Locale, MessageFormat>> codeMap = this.cachedBundleMessageFormats.get(bundle);
		Map<Locale, MessageFormat> localeMap = null;
		if (codeMap != null) {
			// 检查缓存中是否有基于指定资源包的代码映射
			localeMap = codeMap.get(code);
			if (localeMap != null) {
				// 如果找到了对应的区域设置映射，则返回其中的消息格式
				MessageFormat result = localeMap.get(locale);
				if (result != null) {
					return result;
				}
			}
		}

		// 如果没有在缓存中找到对应的消息格式，则从资源包中获取消息
		String msg = getStringOrNull(bundle, code);
		if (msg != null) {
			// 如果成功获取到消息，则将其格式化为消息格式，并将其存储在缓存中
			if (codeMap == null) {
				codeMap = this.cachedBundleMessageFormats.computeIfAbsent(bundle, b -> new ConcurrentHashMap<>());
			}
			if (localeMap == null) {
				localeMap = codeMap.computeIfAbsent(code, c -> new ConcurrentHashMap<>());
			}
			// 创建消息格式化器
			MessageFormat result = createMessageFormat(msg, locale);
			// 加入缓存中
			localeMap.put(locale, result);
			return result;
		}

		// 如果无法获取消息，则返回 null
		return null;
	}

	/**
	 * 高效地检索指定键的字符串值，如果未找到则返回{@code null}。
	 * <p>从4.2版本开始，默认实现在尝试调用{@code getString}之前会检查{@code containsKey}
	 * （这需要捕获{@code MissingResourceException}以处理找不到的键）。
	 * <p>可在子类中重写。
	 *
	 * @param bundle 要执行查找的ResourceBundle
	 * @param key    要查找的键
	 * @return 关联的值，如果没有则为{@code null}
	 * @see ResourceBundle#getString(String)
	 * @see ResourceBundle#containsKey(String)
	 * @since 4.2
	 */
	@Nullable
	protected String getStringOrNull(ResourceBundle bundle, String key) {
		if (bundle.containsKey(key)) {
			// 如果资源包中含有该键
			try {
				//获取该键对应的字符串值
				return bundle.getString(key);
			} catch (MissingResourceException ex) {
				// 假定由于其他原因找不到键
				// -> 不要抛出异常以允许检查父消息源。
			}
		}
		// 不包含，则返回空
		return null;
	}

	/**
	 * 显示此消息源的配置。
	 */
	@Override
	public String toString() {
		return getClass().getName() + ": basenames=" + getBasenameSet();
	}


	/**
	 * {@code ResourceBundle.Control}的自定义实现，添加对自定义文件编码的支持，
	 * 停用对系统区域设置的回退，并激活ResourceBundle的本机缓存（如果需要）。
	 */
	private class MessageSourceControl extends ResourceBundle.Control {

		@Override
		@Nullable
		public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
				throws IllegalAccessException, InstantiationException, IOException {

			// 对于特殊的 java.properties 格式，需要特殊处理
			if (format.equals("java.properties")) {
				// 将基础名称和区域设置转换为资源包名称
				String bundleName = toBundleName(baseName, locale);
				// 将资源包名称转换为资源名称
				final String resourceName = toResourceName(bundleName, "properties");
				final ClassLoader classLoader = loader;
				final boolean reloadFlag = reload;
				InputStream inputStream;
				try {
					// 使用特权操作访问资源
					inputStream = AccessController.doPrivileged((PrivilegedExceptionAction<InputStream>) () -> {
						InputStream is = null;
						if (reloadFlag) {
							// 如果需要重新加载，获取资源名对应的URL
							URL url = classLoader.getResource(resourceName);
							if (url != null) {
								// 打开URL连接
								URLConnection connection = url.openConnection();
								if (connection != null) {
									connection.setUseCaches(false);
									// 获取输入流
									is = connection.getInputStream();
								}
							}
						} else {
							// 如果不需要重新加载，则直接获取输入流
							is = classLoader.getResourceAsStream(resourceName);
						}
						return is;
					});
				} catch (PrivilegedActionException ex) {
					// 如果出现异常，则抛出 IOException
					throw (IOException) ex.getException();
				}
				if (inputStream != null) {
					// 如果获取到的输入流
					// 获取默认的编码格式
					String encoding = getDefaultEncoding();
					if (encoding != null) {
						// 如果编码格式存在，则根据编码格式和输入流加载资源包
						try (InputStreamReader bundleReader = new InputStreamReader(inputStream, encoding)) {
							return loadBundle(bundleReader);
						}
					} else {
						// 如果默认编码为 null，则直接加载资源包
						try (InputStream bundleStream = inputStream) {
							return loadBundle(bundleStream);
						}
					}
				} else {
					// 如果无法获取到输入流，则返回 null
					return null;
				}
			} else {
				// 对于其他格式（例如 java.class），委托给标准的 Control 处理
				return super.newBundle(baseName, locale, format, loader, reload);
			}
		}

		@Override
		@Nullable
		public Locale getFallbackLocale(String baseName, Locale locale) {
			// 获取默认的区域设置
			Locale defaultLocale = getDefaultLocale();
			// 如果默认的区域设置不为空且不等于指定的区域设置，则返回默认的区域设置，否则返回 null
			return (defaultLocale != null && !defaultLocale.equals(locale) ? defaultLocale : null);
		}

		@Override
		public long getTimeToLive(String baseName, Locale locale) {
			// 获取缓存的时间（以毫秒为单位）
			long cacheMillis = getCacheMillis();
			// 如果缓存时间大于等于0，则返回缓存时间；否则，委托给父类的 getTimeToLive 方法
			return (cacheMillis >= 0 ? cacheMillis : super.getTimeToLive(baseName, locale));
		}

		@Override
		public boolean needsReload(
				String baseName, Locale locale, String format, ClassLoader loader, ResourceBundle bundle, long loadTime) {

			// 检查是否需要重新加载资源束
			if (super.needsReload(baseName, locale, format, loader, bundle, loadTime)) {
				// 如果需要重新加载，则从缓存中移除已缓存的消息格式
				cachedBundleMessageFormats.remove(bundle);
				// 返回 true 表示需要重新加载
				return true;
			} else {
				// 如果不需要重新加载，则返回 false
				return false;
			}
		}
	}

}
