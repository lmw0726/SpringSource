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

package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.OverridingClassLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 使用反射将加载时织入委托给底层 ClassLoader 的 {@link LoadTimeWeaver} 实现，
 * 底层 ClassLoader 需要提供已知的转换钩子方法。底层 ClassLoader 应当支持以下织入方法
 * （如 {@link LoadTimeWeaver} 接口所定义）：
 * <ul>
 * <li>{@code public void addTransformer(java.lang.instrument.ClassFileTransformer)}：
 * 在此 ClassLoader 上注册给定的 ClassFileTransformer
 * <li>{@code public ClassLoader getThrowawayClassLoader()}：
 * 获取此 ClassLoader 的临时类加载器（可选；
 * 如果该方法不可用，ReflectiveLoadTimeWeaver 将回退使用 SimpleThrowawayClassLoader）
 * </ul>
 *
 * <p>请注意，上述方法<i>必须</i>存在于一个可公开访问的类中，
 * 但该类本身不必对应用程序的类加载器可见。
 *
 * <p>此 LoadTimeWeaver 的反射特性在底层 ClassLoader 实现本身由不同的类加载器
 * 加载时尤为有用（例如应用程序服务器的类加载器对 Web 应用不可见）。
 * 此 LoadTimeWeaver 适配器与底层 ClassLoader 之间没有直接的 API 依赖，
 * 仅存在一个'松散'的方法契约。
 *
 * <p>例如，Resin 应用服务器 3.1+ 版本应使用此 LoadTimeWeaver。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0
 * @see #addTransformer(java.lang.instrument.ClassFileTransformer)
 * @see #getThrowawayClassLoader()
 * @see SimpleThrowawayClassLoader
 */
public class ReflectiveLoadTimeWeaver implements LoadTimeWeaver {

	private static final String ADD_TRANSFORMER_METHOD_NAME = "addTransformer";

	private static final String GET_THROWAWAY_CLASS_LOADER_METHOD_NAME = "getThrowawayClassLoader";

	private static final Log logger = LogFactory.getLog(ReflectiveLoadTimeWeaver.class);


	private final ClassLoader classLoader;

	private final Method addTransformerMethod;

	@Nullable
	private final Method getThrowawayClassLoaderMethod;


	/**
	 * 为当前上下文类加载器创建一个新的 ReflectiveLoadTimeWeaver，
	 * <i>该类加载器需要支持所需的织入方法</i>。
	 */
	public ReflectiveLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 为给定的类加载器创建一个新的 SimpleLoadTimeWeaver。
	 * @param classLoader 用于委托织入操作的 {@code ClassLoader}
	 * （<i>必须</i>支持所需的织入方法）。
	 * @throws IllegalStateException 如果提供的 {@code ClassLoader}
	 * 不支持所需的织入方法
	 */
	public ReflectiveLoadTimeWeaver(@Nullable ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = classLoader;

		Method addTransformerMethod = ClassUtils.getMethodIfAvailable(
				this.classLoader.getClass(), ADD_TRANSFORMER_METHOD_NAME, ClassFileTransformer.class);
		if (addTransformerMethod == null) {
			throw new IllegalStateException(
					"ClassLoader [" + classLoader.getClass().getName() + "] does NOT provide an " +
					"'addTransformer(ClassFileTransformer)' method.");
		}
		this.addTransformerMethod = addTransformerMethod;

		Method getThrowawayClassLoaderMethod = ClassUtils.getMethodIfAvailable(
				this.classLoader.getClass(), GET_THROWAWAY_CLASS_LOADER_METHOD_NAME);
		// getThrowawayClassLoader 方法是可选的
		if (getThrowawayClassLoaderMethod == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("The ClassLoader [" + classLoader.getClass().getName() + "] does NOT provide a " +
						"'getThrowawayClassLoader()' method; SimpleThrowawayClassLoader will be used instead.");
			}
		}
		this.getThrowawayClassLoaderMethod = getThrowawayClassLoaderMethod;
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		Assert.notNull(transformer, "Transformer must not be null");
		ReflectionUtils.invokeMethod(this.addTransformerMethod, this.classLoader, transformer);
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader;
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		if (this.getThrowawayClassLoaderMethod != null) {
			ClassLoader target = (ClassLoader)
					ReflectionUtils.invokeMethod(this.getThrowawayClassLoaderMethod, this.classLoader);
			return (target instanceof DecoratingClassLoader ? target :
					new OverridingClassLoader(this.classLoader, target));
		}
		else {
			return new SimpleThrowawayClassLoader(this.classLoader);
		}
	}

}
