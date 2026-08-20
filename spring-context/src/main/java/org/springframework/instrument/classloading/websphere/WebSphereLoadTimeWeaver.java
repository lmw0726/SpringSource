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

package org.springframework.instrument.classloading.websphere;

import java.lang.instrument.ClassFileTransformer;

import org.springframework.core.OverridingClassLoader;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 用于 WebSphere 可检测类加载器的 {@link LoadTimeWeaver} 实现。
 * 兼容 WebSphere 7 以及 8 和 9。
 *
 * @author Costin Leau
 * @since 3.1
 */
public class WebSphereLoadTimeWeaver implements LoadTimeWeaver {

	private final WebSphereClassLoaderAdapter classLoader;


	/**
	 * 使用默认的 {@link ClassLoader class loader} 创建 {@link WebSphereLoadTimeWeaver} 类的新实例。
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 */
	public WebSphereLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 使用提供的 {@link ClassLoader} 创建 {@link WebSphereLoadTimeWeaver} 类的新实例。
	 * @param classLoader 用于委托进行织入的 {@code ClassLoader}
	 */
	public WebSphereLoadTimeWeaver(@Nullable ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = new WebSphereClassLoaderAdapter(classLoader);
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		this.classLoader.addTransformer(transformer);
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader.getClassLoader();
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		return new OverridingClassLoader(this.classLoader.getClassLoader(),
				this.classLoader.getThrowawayClassLoader());
	}

}
