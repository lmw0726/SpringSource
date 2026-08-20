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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * ShadowingClassLoader 的子类，用于覆盖对某些文件的定位尝试。
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0
 */
public class ResourceOverridingShadowingClassLoader extends ShadowingClassLoader {

	private static final Enumeration<URL> EMPTY_URL_ENUMERATION = new Enumeration<URL>() {
		@Override
		public boolean hasMoreElements() {
			return false;
		}
		@Override
		public URL nextElement() {
			throw new UnsupportedOperationException("Should not be called. I am empty.");
		}
	};


	/**
	 * 键为请求的值，值为实际的值。
	 */
	private Map<String, String> overrides = new HashMap<>();


	/**
	 * 创建一个新的 ResourceOverridingShadowingClassLoader，
	 * 装饰给定的 ClassLoader。
	 * @param enclosingClassLoader 要装饰的 ClassLoader
	 */
	public ResourceOverridingShadowingClassLoader(ClassLoader enclosingClassLoader) {
		super(enclosingClassLoader);
	}


	/**
	 * 当尝试定位旧路径上的资源时，
	 * 返回新路径上的资源（如果有的话）。
	 * @param oldPath 请求的路径
	 * @param newPath 实际要查找的路径
	 */
	public void override(String oldPath, String newPath) {
		this.overrides.put(oldPath, newPath);
	}

	/**
	 * 确保具有给定路径的资源不会被找到。
	 * @param oldPath 要隐藏的资源路径，即使它存在于父 ClassLoader 中
	 */
	public void suppress(String oldPath) {
		this.overrides.put(oldPath, null);
	}

	/**
	 * 复制给定 ClassLoader 中的所有覆盖配置。
	 * @param other 要复制的另一个 ClassLoader
	 */
	public void copyOverrides(ResourceOverridingShadowingClassLoader other) {
		Assert.notNull(other, "Other ClassLoader must not be null");
		this.overrides.putAll(other.overrides);
	}


	@Override
	public URL getResource(String requestedPath) {
		if (this.overrides.containsKey(requestedPath)) {
			String overriddenPath = this.overrides.get(requestedPath);
			return (overriddenPath != null ? super.getResource(overriddenPath) : null);
		}
		else {
			return super.getResource(requestedPath);
		}
	}

	@Override
	@Nullable
	public InputStream getResourceAsStream(String requestedPath) {
		if (this.overrides.containsKey(requestedPath)) {
			String overriddenPath = this.overrides.get(requestedPath);
			return (overriddenPath != null ? super.getResourceAsStream(overriddenPath) : null);
		}
		else {
			return super.getResourceAsStream(requestedPath);
		}
	}

	@Override
	public Enumeration<URL> getResources(String requestedPath) throws IOException {
		if (this.overrides.containsKey(requestedPath)) {
			String overriddenLocation = this.overrides.get(requestedPath);
			return (overriddenLocation != null ?
					super.getResources(overriddenLocation) : EMPTY_URL_ENUMERATION);
		}
		else {
			return super.getResources(requestedPath);
		}
	}

}
