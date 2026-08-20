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

package org.springframework.instrument.classloading;

import org.springframework.core.OverridingClassLoader;
import org.springframework.lang.Nullable;

/**
 * 一个可以用来加载类的 ClassLoader，而不会将这些类带入父加载器。
 * 旨在支持 JPA 的"临时类加载器"需求，但并不局限于 JPA。
 *
 * @author Rod Johnson
 * @since 2.0
 */
public class SimpleThrowawayClassLoader extends OverridingClassLoader {

	static {
		ClassLoader.registerAsParallelCapable();
	}


	/**
	 * 为给定的 ClassLoader 创建一个新的 SimpleThrowawayClassLoader。
	 * @param parent 要为其构建临时 ClassLoader 的 ClassLoader
	 */
	public SimpleThrowawayClassLoader(@Nullable ClassLoader parent) {
		super(parent);
	}

}
