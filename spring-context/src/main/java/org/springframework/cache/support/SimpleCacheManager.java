/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.cache.support;

import java.util.Collection;
import java.util.Collections;

import org.springframework.cache.Cache;

/**
 * 简单的缓存管理器，基于给定的缓存集合工作。
 * 适用于测试或简单的缓存声明。
 * <p>
 * 当直接使用此实现（即不通过常规 Bean 注册方式）时，
 * 一旦 {@linkplain #setCaches(Collection) 缓存已提供}，
 * 就应该调用 {@link #initializeCaches()} 来初始化其内部状态。
 *
 * @author Costin Leau
 * @since 3.1
 */
public class SimpleCacheManager extends AbstractCacheManager {

	private Collection<? extends Cache> caches = Collections.emptySet();


	/**
	 * 指定此 CacheManager 要使用的 Cache 实例集合。
	 * @see #initializeCaches()
	 */
	public void setCaches(Collection<? extends Cache> caches) {
		this.caches = caches;
	}

	@Override
	protected Collection<? extends Cache> loadCaches() {
		return this.caches;
	}

}
