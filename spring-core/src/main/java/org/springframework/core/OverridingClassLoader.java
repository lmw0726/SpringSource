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

package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * 一个 {@code ClassLoader}，它不像普通类加载器那样总是委托给父加载器。
 * 这使得在覆盖类加载器中强制进行字节码增强成为可能，
 * 或者实现一种“临时”类加载行为，即选择性地在覆盖的 {@code ClassLoader} 中临时加载应用类以供分析，
 * 之后再在给定的父 {@code ClassLoader} 中加载该类的增强版本。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0.1
 */
public class OverridingClassLoader extends DecoratingClassLoader {

	/** 默认排除的包。 */
	public static final String[] DEFAULT_EXCLUDED_PACKAGES = new String[]
			{"java.", "javax.", "sun.", "oracle.", "javassist.", "org.aspectj.", "net.sf.cglib."};

	private static final String CLASS_FILE_SUFFIX = ".class";

	static {
		ClassLoader.registerAsParallelCapable();
	}


	@Nullable
	private final ClassLoader overrideDelegate;


	/**
	 * 为给定的 ClassLoader 创建一个新的 OverridingClassLoader。
	 * @param parent 要构建覆盖类加载器的 ClassLoader
	 */
	public OverridingClassLoader(@Nullable ClassLoader parent) {
		this(parent, null);
	}

	/**
	 * 为给定的 ClassLoader 创建一个新的 OverridingClassLoader。
	 * @param parent 要构建覆盖类加载器的 ClassLoader
	 * @param overrideDelegate 用于覆盖的委托 ClassLoader
	 * @since 4.3
	 */
	public OverridingClassLoader(@Nullable ClassLoader parent, @Nullable ClassLoader overrideDelegate) {
		super(parent);
		this.overrideDelegate = overrideDelegate;
		for (String packageName : DEFAULT_EXCLUDED_PACKAGES) {
			excludePackage(packageName);
		}
	}


	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (this.overrideDelegate != null && isEligibleForOverriding(name)) {
			return this.overrideDelegate.loadClass(name);
		}
		return super.loadClass(name);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (isEligibleForOverriding(name)) {
			Class<?> result = loadClassForOverriding(name);
			if (result != null) {
				if (resolve) {
					resolveClass(result);
				}
				return result;
			}
		}
		return super.loadClass(name, resolve);
	}

	/**
	 * 判断指定的类是否有资格被此类加载器覆盖。
	 * @param className 要检查的类名
	 * @return 指定类是否有资格被覆盖
	 * @see #isExcluded
	 */
	protected boolean isEligibleForOverriding(String className) {
		return !isExcluded(className);
	}

	/**
	 * 加载指定的类，用于在此 ClassLoader 中覆盖。
	 * <p>默认实现委托给 {@link #findLoadedClass}、
	 * {@link #loadBytesForClass} 和 {@link #defineClass}。
	 * @param name 类名
	 * @return Class 对象，或如果没有定义该名称的类则返回 {@code null}
	 * @throws ClassNotFoundException 如果无法加载给定名称的类
	 */
	@Nullable
	protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
		Class<?> result = findLoadedClass(name);
		if (result == null) {
			byte[] bytes = loadBytesForClass(name);
			if (bytes != null) {
				result = defineClass(name, bytes, 0, bytes.length);
			}
		}
		return result;
	}

	/**
	 * 加载给定类的定义字节，用于通过 {@link #defineClass} 调用转成 Class 对象。
	 * <p>默认实现委托给 {@link #openStreamForClass} 和 {@link #transformIfNecessary}。
	 * @param name 类名
	 * @return 字节内容（转换器已应用），
	 * 或如果没有定义该名称的类则返回 {@code null}
	 * @throws ClassNotFoundException 如果无法加载给定名称的类
	 */
	@Nullable
	protected byte[] loadBytesForClass(String name) throws ClassNotFoundException {
		InputStream is = openStreamForClass(name);
		if (is == null) {
			return null;
		}
		try {
			// 读取原始字节。
			byte[] bytes = FileCopyUtils.copyToByteArray(is);
			// 必要时转换并使用可能已转换的字节。
			return transformIfNecessary(name, bytes);
		}
		catch (IOException ex) {
			throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
		}
	}

	/**
	 * 为指定类打开 InputStream。
	 * <p>默认实现通过父 ClassLoader 的 {@code getResourceAsStream} 方法加载标准类文件。
	 * @param name 类名
	 * @return 包含指定类字节码的 InputStream
	 */
	@Nullable
	protected InputStream openStreamForClass(String name) {
		String internalName = name.replace('.', '/') + CLASS_FILE_SUFFIX;
		return getParent().getResourceAsStream(internalName);
	}


	/**
	 * 转换钩子，由子类实现。
	 * <p>默认实现直接返回传入的字节数组。
	 * @param name 被转换的类的全限定名
	 * @param bytes 类的原始字节
	 * @return 转换后的字节（永不为 {@code null}；
	 * 如果没有转换则与输入字节相同）
	 */
	protected byte[] transformIfNecessary(String name, byte[] bytes) {
		return bytes;
	}

}
