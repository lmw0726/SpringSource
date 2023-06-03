/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.io.support;

import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * Utility class for determining whether a given URL is a resource
 * location that can be loaded via a {@link ResourcePatternResolver}.
 *
 * <p>Callers will usually assume that a location is a relative path
 * if the {@link #isUrl(String)} method returns {@code false}.
 *
 * @author Juergen Hoeller
 * @since 1.2.3
 */
public abstract class ResourcePatternUtils {

	/**
	 * 返回给定的资源位置是否是URL: 一个特殊的 “classpath” 或 “classpath” 伪URL或标准URL。
	 *
	 * @param resourceLocation 要检查的位置字符串
	 * @return 该位置是否符合URL的条件
	 * @see ResourcePatternResolver#CLASSPATH_ALL_URL_PREFIX
	 * @see org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX
	 * @see org.springframework.util.ResourceUtils#isUrl(String)
	 * @see java.net.URL
	 */
	public static boolean isUrl(@Nullable String resourceLocation) {
		//资源位置为空，则返回false
		if (resourceLocation == null) {
			return false;
		}
		//且该字符串以classpath*:开头，或者该字符串是一个url
		return resourceLocation.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX) ||
				ResourceUtils.isUrl(resourceLocation);
	}

	/**
	 * 返回给定 {@link ResourcePatternResolver} 的默认 {@link ResourcePatternResolver}。
	 * <p> 这可能是 {@code ResourceLoader} 本身，如果它实现了 {@code ResourcePatternResolver} 扩展名，
	 * 或者在给定的 {@code ResourceLoader} 上构建的默认 {@link PathMatchingResourcePatternResolver}。
	 *
	 * @param resourceLoader 用于构建模式解析器的ResourceLoader (可能为 {@code null} 表示默认ResourceLoader)
	 * @return 资源解析器
	 * @see PathMatchingResourcePatternResolver
	 */
	public static ResourcePatternResolver getResourcePatternResolver(@Nullable ResourceLoader resourceLoader) {
		if (resourceLoader instanceof ResourcePatternResolver) {
			return (ResourcePatternResolver) resourceLoader;
		}

		if (resourceLoader != null) {
			return new PathMatchingResourcePatternResolver(resourceLoader);
		}

		return new PathMatchingResourcePatternResolver();
	}

}
