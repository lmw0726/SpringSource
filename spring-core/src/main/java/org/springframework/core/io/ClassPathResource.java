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

package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * {@link Resource} implementation for class path resources. Uses either a
 * given {@link ClassLoader} or a given {@link Class} for loading resources.
 *
 * <p>Supports resolution as {@code java.io.File} if the class path
 * resource resides in the file system, but not for resources in a JAR.
 * Always supports resolution as URL.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see ClassLoader#getResourceAsStream(String)
 * @see Class#getResourceAsStream(String)
 * @since 28.12.2003
 */
public class ClassPathResource extends AbstractFileResolvingResource {
	//资源对应的路径
	private final String path;

	//类加载器，ClassPathResource可以通过它来加载资源
	@Nullable
	private ClassLoader classLoader;

	//指定的类，ClassPathResource也可以通过指定的类来加载资源
	@Nullable
	private Class<?> clazz;


	public ClassPathResource(String path) {
		this(path, (ClassLoader) null);
	}


	public ClassPathResource(String path, @Nullable ClassLoader classLoader) {
		Assert.notNull(path, "Path must not be null");
		String pathToUse = StringUtils.cleanPath(path);
		if (pathToUse.startsWith("/")) {
			pathToUse = pathToUse.substring(1);
		}
		this.path = pathToUse;
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
	}


	public ClassPathResource(String path, @Nullable Class<?> clazz) {
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);
		this.clazz = clazz;
	}


	@Deprecated
	protected ClassPathResource(String path, @Nullable ClassLoader classLoader, @Nullable Class<?> clazz) {
		this.path = StringUtils.cleanPath(path);
		this.classLoader = classLoader;
		this.clazz = clazz;
	}


	/**
	 * 返回此资源的路径(作为类路径中的资源路径)。
	 *
	 * @return 资源的路径
	 */
	public final String getPath() {
		return this.path;
	}


	/**
	 * 获取加载资源的类加载器
	 *
	 * @return 加载资源的类加载器
	 */
	@Nullable
	public final ClassLoader getClassLoader() {
		return (this.clazz == null ? this.classLoader : this.clazz.getClassLoader());
	}


	/**
	 * 资源是否为空，根据解析出来的URL是否为空来判断资源是否存在
	 *
	 * @return true表示资源为空
	 */
	@Override
	public boolean exists() {
		return (resolveURL() != null);
	}


	/**
	 * 资源是否可读
	 *
	 * @return true表示资源可读
	 */
	@Override
	public boolean isReadable() {
		//解析为URL，调用AbstractFileResolvingResource来检查是否可读
		URL url = resolveURL();
		return (url != null && checkReadable(url));
	}

	/**
	 * 解析为URL
	 *
	 * @return URL，资源统一定位符
	 */
	@Nullable
	protected URL resolveURL() {
		try {
			if (this.clazz != null) {
				//如果类不为空，则通过类获取路径对应的URL
				return this.clazz.getResource(this.path);
			} else if (this.classLoader != null) {
				//如果类加载器不为空，通过类加载器获取路径对应的URL
				return this.classLoader.getResource(this.path);
			} else {
				//否则通过类加载获取该路径对应的系统资源
				return ClassLoader.getSystemResource(this.path);
			}
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	/**
	 * 获取资源对应的输入流
	 *
	 * @return 输入流
	 * @throws IOException IO异常
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is;
		if (this.clazz != null) {
			//指定的类不为空，获取类获取该路径对应的输入流
			is = this.clazz.getResourceAsStream(this.path);
		} else if (this.classLoader != null) {
			//如果类加载器不为空，通过类加载器获取路径对应的输入流
			is = this.classLoader.getResourceAsStream(this.path);
		} else {
			//否则通过通过类加载器获取该路径对应的系统资源输入流
			is = ClassLoader.getSystemResourceAsStream(this.path);
		}
		//输入流为空，抛出文件未找到异常
		if (is == null) {
			throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
		}
		return is;
	}

	/**
	 * 获取URL，统一资源定位符
	 *
	 * @return 统一资源定位符
	 * @throws IOException IO异常
	 */
	@Override
	public URL getURL() throws IOException {
		URL url = resolveURL();
		if (url == null) {
			throw new FileNotFoundException(getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}


	/**
	 * 创建相关的资源
	 *
	 * @param relativePath 相对路径
	 * @return 相关的资源
	 */
	@Override
	public Resource createRelative(String relativePath) {
		//获取可用的路径
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		//如果指定类不为空，通过该类和可用路径创建ClassPathResource。
		return (this.clazz != null ? new ClassPathResource(pathToUse, this.clazz) :
				//否则通过类加载器和可用路径去创建ClassPathResource
				new ClassPathResource(pathToUse, this.classLoader));
	}


	/**
	 * 获取文件名，通过字符串截取路径获取文件名
	 *
	 * @return 资源对应的文件名
	 */
	@Override
	@Nullable
	public String getFilename() {
		return StringUtils.getFilename(this.path);
	}


	/**
	 * 获取资源描述，该资源描述和路径有关
	 *
	 * @return 资源描述
	 */
	@Override
	public String getDescription() {
		StringBuilder builder = new StringBuilder("class path resource [");
		String pathToUse = this.path;
		if (this.clazz != null && !pathToUse.startsWith("/")) {
			builder.append(ClassUtils.classPackageAsResourcePath(this.clazz));
			builder.append('/');
		}
		if (pathToUse.startsWith("/")) {
			pathToUse = pathToUse.substring(1);
		}
		builder.append(pathToUse);
		builder.append(']');
		return builder.toString();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ClassPathResource)) {
			return false;
		}
		ClassPathResource otherRes = (ClassPathResource) other;
		return (this.path.equals(otherRes.path) &&
				ObjectUtils.nullSafeEquals(this.classLoader, otherRes.classLoader) &&
				ObjectUtils.nullSafeEquals(this.clazz, otherRes.clazz));
	}


	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
