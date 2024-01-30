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

package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ResourceLoader}接口的默认实现。
 * 由{@link ResourceEditor}使用，并用作{@link org.springframework.context.support.AbstractApplicationContext}的基类。
 * 也可以独立使用。
 *
 * <p>如果位置值是URL，则将返回{@link UrlResource}，如果是非URL路径或"classpath:"伪URL，则返回{@link ClassPathResource}。
 *
 * @author Juergen Hoeller
 * @see FileSystemResourceLoader
 * @see org.springframework.context.support.ClassPathXmlApplicationContext
 * @since 10.03.2004
 */
public class DefaultResourceLoader implements ResourceLoader {

	/**
	 * 类加载器
	 */
	@Nullable
	private ClassLoader classLoader;

	/**
	 * 协议解析器列表
	 */
	private final Set<ProtocolResolver> protocolResolvers = new LinkedHashSet<>(4);

	/**
	 * 资源缓存
	 */
	private final Map<Class<?>, Map<Resource, ?>> resourceCaches = new ConcurrentHashMap<>(4);


	/**
	 * 创建一个新的DefaultResourceLoader。
	 * <p>类加载器访问将使用实际资源访问时的线程上下文类加载器（自5.3以来）。为了更多的控制，可以通过{@link #DefaultResourceLoader(ClassLoader)}传递一个特定的ClassLoader。
	 *
	 * @see java.lang.Thread#getContextClassLoader()
	 */
	public DefaultResourceLoader() {
	}

	/**
	 * 创建一个新的DefaultResourceLoader。
	 *
	 * @param classLoader 用于加载类路径资源的ClassLoader，或{@code null}表示在实际资源访问时使用线程上下文类加载器
	 */
	public DefaultResourceLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * 指定用于加载类路径资源的ClassLoader，或{@code null}表示在实际资源访问时使用线程上下文类加载器。
	 * <p>默认情况下，类加载器访问将使用实际资源访问时的线程上下文类加载器（自5.3以来）。
	 *
	 * @param classLoader 用于加载类路径资源的ClassLoader，或{@code null}表示在实际资源访问时使用线程上下文类加载器
	 */
	public void setClassLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * 返回用于加载类路径资源的ClassLoader。
	 * <p>将传递给此资源加载器创建的所有ClassPathResource对象的ClassPathResource构造函数。
	 *
	 * @return 用于加载类路径资源的ClassLoader
	 * @see ClassPathResource
	 */
	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return (this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 使用此资源加载器注册给定的解析器，允许处理附加协议。
	 * <p>任何此类解析器都将在此加载器的标准解析规则之前调用。因此，它也可能覆盖任何默认规则。
	 *
	 * @param resolver 要注册的协议解析器
	 * @see #getProtocolResolvers()
	 * @since 4.3
	 */
	public void addProtocolResolver(ProtocolResolver resolver) {
		Assert.notNull(resolver, "ProtocolResolver must not be null");
		this.protocolResolvers.add(resolver);
	}

	/**
	 * 返回当前注册的协议解析器的集合，允许内省以及修改。
	 *
	 * @return 当前注册的协议解析器的集合
	 * @since 4.3
	 */
	public Collection<ProtocolResolver> getProtocolResolvers() {
		return this.protocolResolvers;
	}

	/**
	 * 获取给定值类型的缓存，由{@link Resource}键控。
	 *
	 * @param valueType 值类型，例如ASM {@code MetadataReader}
	 * @return 缓存{@link Map}，在{@code ResourceLoader}级别共享
	 * @since 5.0
	 */
	@SuppressWarnings("unchecked")
	public <T> Map<Resource, T> getResourceCache(Class<T> valueType) {
		return (Map<Resource, T>) this.resourceCaches.computeIfAbsent(valueType, key -> new ConcurrentHashMap<>());
	}

	/**
	 * Clear all resource caches in this resource loader.
	 *
	 * @see #getResourceCache
	 * @since 5.0
	 */
	public void clearResourceCaches() {
		this.resourceCaches.clear();
	}


	@Override
	public Resource getResource(String location) {
		Assert.notNull(location, "Location must not be null");

		// 遍历所有ProtocolResolver，尝试通过协议解析器解析给定的位置。
		for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
			Resource resource = protocolResolver.resolve(location, this);
			// 如果通过协议解析器成功解析到资源，则返回该资源。
			if (resource != null) {
				return resource;
			}
		}

		// 如果位置以"/"开头，使用getResourceByPath方法获取资源。
		if (location.startsWith("/")) {
			return getResourceByPath(location);
		} else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			// 如果位置以"classpath:"开头，创建ClassPathResource对象。
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
		} else {
			try {
				// 尝试将位置解析为URL...
				URL url = new URL(location);
				// 如果是文件URL，则创建FileUrlResource对象；否则，创建UrlResource对象。
				return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
			} catch (MalformedURLException ex) {
				// 如果无法解析为URL，则将位置视为资源路径，使用getResourceByPath方法获取资源。
				return getResourceByPath(location);
			}
		}

	}

	/**
	 * 返回给定路径资源的Resource句柄。
	 * <p>默认实现支持类路径位置。这对于独立实现可能是合适的，但可以被覆盖，例如针对Servlet容器的实现。
	 *
	 * @param path 资源的路径
	 * @return 相应的Resource句柄
	 * @see ClassPathResource
	 * @see org.springframework.context.support.FileSystemXmlApplicationContext#getResourceByPath
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#getResourceByPath
	 */
	protected Resource getResourceByPath(String path) {
		return new ClassPathContextResource(path, getClassLoader());
	}


	/**
	 * 通过实现ContextResource接口明确表示上下文相对路径的ClassPathResource。
	 */
	protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

		public ClassPathContextResource(String path, @Nullable ClassLoader classLoader) {
			super(path, classLoader);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}

		@Override
		public Resource createRelative(String relativePath) {
			String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
			return new ClassPathContextResource(pathToUse, getClassLoader());
		}
	}

}
