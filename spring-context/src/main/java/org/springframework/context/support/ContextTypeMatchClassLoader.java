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

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.OverridingClassLoader;
import org.springframework.core.SmartClassLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * OverridingClassLoader 的特殊变体，用于 {@link AbstractApplicationContext} 中的临时类型匹配。
 * 对于每次 {@code loadClass} 调用，都会从缓存的字节数组重新定义类，
 * 以便获取父 ClassLoader 中最近加载的类型。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see AbstractApplicationContext
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setTempClassLoader
 */
class ContextTypeMatchClassLoader extends DecoratingClassLoader implements SmartClassLoader {

	static {
		ClassLoader.registerAsParallelCapable();
	}


	private static Method findLoadedClassMethod;

	static {
		try {
			findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Invalid [java.lang.ClassLoader] class: no 'findLoadedClass' method defined!");
		}
	}


	/** 按类名缓存的字节数组。 */
	private final Map<String, byte[]> bytesCache = new ConcurrentHashMap<>(256);


	public ContextTypeMatchClassLoader(@Nullable ClassLoader parent) {
		super(parent);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return new ContextOverridingClassLoader(getParent()).loadClass(name);
	}

	@Override
	public boolean isClassReloadable(Class<?> clazz) {
		return (clazz.getClassLoader() instanceof ContextOverridingClassLoader);
	}

	@Override
	public Class<?> publicDefineClass(String name, byte[] b, @Nullable ProtectionDomain protectionDomain) {
		return defineClass(name, b, 0, b.length, protectionDomain);
	}


	/**
	 * 为每个加载的类创建的 ClassLoader。
	 * 缓存类文件内容，但每次调用时重新定义类。
	 */
	private class ContextOverridingClassLoader extends OverridingClassLoader {

		public ContextOverridingClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected boolean isEligibleForOverriding(String className) {
			if (isExcluded(className) || ContextTypeMatchClassLoader.this.isExcluded(className)) {
				return false;
			}
			ReflectionUtils.makeAccessible(findLoadedClassMethod);
			ClassLoader parent = getParent();
			while (parent != null) {
				if (ReflectionUtils.invokeMethod(findLoadedClassMethod, parent, className) != null) {
					return false;
				}
				parent = parent.getParent();
			}
			return true;
		}

		@Override
		protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
			byte[] bytes = bytesCache.get(name);
			if (bytes == null) {
				bytes = loadBytesForClass(name);
				if (bytes != null) {
					bytesCache.put(name, bytes);
				}
				else {
					return null;
				}
			}
			return defineClass(name, bytes, 0, bytes.length);
		}
	}

}
