/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 装饰 ClassLoader 的基类，如 {@link OverridingClassLoader}
 * 和 {@link org.springframework.instrument.classloading.ShadowingClassLoader}，
 * 提供对排除包和类的通用处理。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.5.2
 */
public abstract class DecoratingClassLoader extends ClassLoader {

	static {
		ClassLoader.registerAsParallelCapable();
	}


	private final Set<String> excludedPackages = Collections.newSetFromMap(new ConcurrentHashMap<>(8));

	private final Set<String> excludedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(8));


	/**
	 * 创建一个无父 ClassLoader 的 DecoratingClassLoader。
	 */
	public DecoratingClassLoader() {
	}

	/**
	 * 使用给定的父 ClassLoader 创建一个 DecoratingClassLoader 以进行委托。
	 */
	public DecoratingClassLoader(@Nullable ClassLoader parent) {
		super(parent);
	}


	/**
	 * 添加要排除装饰（例如重写）的包名。
	 * <p>任何全限定名以此注册名开头的类，
	 * 都将由父 ClassLoader 以通常方式处理。
	 *
	 * @param packageName 要排除的包名
	 */
	public void excludePackage(String packageName) {
		Assert.notNull(packageName, "Package name must not be null");
		this.excludedPackages.add(packageName);
	}

	/**
	 * 添加一个类名排除在装饰之外 (例如覆盖)。
	 * <p> 在此处注册的任何类名都将由父类加载器以通常的方式处理。
	 *
	 * @param className 要排除的类名
	 */
	public void excludeClass(String className) {
		Assert.notNull(className, "Class name must not be null");
		this.excludedClasses.add(className);
	}

	/**
	 * 判断指定的类是否被该类加载器排除装饰。
	 * <p>默认实现检查排除的包和类列表。
	 *
	 * @param className 需要检查的类名
	 * @return 指定类是否被排除
	 * @see #excludePackage
	 * @see #excludeClass
	 */
	protected boolean isExcluded(String className) {
		if (this.excludedClasses.contains(className)) {
			return true;
		}
		for (String packageName : this.excludedPackages) {
			if (className.startsWith(packageName)) {
				return true;
			}
		}
		return false;
	}

}
