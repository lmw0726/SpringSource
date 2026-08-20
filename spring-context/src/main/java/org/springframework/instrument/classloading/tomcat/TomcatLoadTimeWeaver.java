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

package org.springframework.instrument.classloading.tomcat;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.core.OverridingClassLoader;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 针对 Tomcat 新版 {@code org.apache.tomcat.InstrumentableClassLoader} 的
 * {@link org.springframework.instrument.classloading.LoadTimeWeaver} 实现。
 * 也能处理遇到 Spring 的 TomcatInstrumentableClassLoader 的情况。
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class TomcatLoadTimeWeaver implements LoadTimeWeaver {

	private static final String INSTRUMENTABLE_LOADER_CLASS_NAME = "org.apache.tomcat.InstrumentableClassLoader";


	private final ClassLoader classLoader;

	private final Method addTransformerMethod;

	private final Method copyMethod;


	/**
	 * 使用默认的 {@link ClassLoader 类加载器}创建 {@link TomcatLoadTimeWeaver} 类的新实例。
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 */
	public TomcatLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 使用提供的 {@link ClassLoader}创建 {@link TomcatLoadTimeWeaver} 类的新实例。
	 * @param classLoader 用于委派进行织入的 {@code ClassLoader}
	 */
	public TomcatLoadTimeWeaver(@Nullable ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = classLoader;

		Class<?> instrumentableLoaderClass;
		try {
			instrumentableLoaderClass = classLoader.loadClass(INSTRUMENTABLE_LOADER_CLASS_NAME);
			if (!instrumentableLoaderClass.isInstance(classLoader)) {
				// 仍可能是一个符合约定的自定义 ClassLoader 变体
				instrumentableLoaderClass = classLoader.getClass();
			}
		}
		catch (ClassNotFoundException ex) {
			// 我们处于较早版本的 Tomcat 上，可能使用的是 Spring 的 TomcatInstrumentableClassLoader
			instrumentableLoaderClass = classLoader.getClass();
		}

		try {
			this.addTransformerMethod = instrumentableLoaderClass.getMethod("addTransformer", ClassFileTransformer.class);
			// 首先检查 Tomcat InstrumentableClassLoader 上的新 copyWithoutTransformers 方法
			Method copyMethod = ClassUtils.getMethodIfAvailable(instrumentableLoaderClass, "copyWithoutTransformers");
			if (copyMethod == null) {
				// 回退方案：期望 TomcatInstrumentableClassLoader 的 getThrowawayClassLoader 方法
				copyMethod = instrumentableLoaderClass.getMethod("getThrowawayClassLoader");
			}
			this.copyMethod = copyMethod;
		}
		catch (Throwable ex) {
			throw new IllegalStateException(
					"Could not initialize TomcatLoadTimeWeaver because Tomcat API classes are not available", ex);
		}
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		try {
			this.addTransformerMethod.invoke(this.classLoader, transformer);
		}
		catch (InvocationTargetException ex) {
			throw new IllegalStateException("Tomcat addTransformer method threw exception", ex.getCause());
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not invoke Tomcat addTransformer method", ex);
		}
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader;
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		try {
			return new OverridingClassLoader(this.classLoader, (ClassLoader) this.copyMethod.invoke(this.classLoader));
		}
		catch (InvocationTargetException ex) {
			throw new IllegalStateException("Tomcat copy method threw exception", ex.getCause());
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not invoke Tomcat copy method", ex);
		}
	}

}
