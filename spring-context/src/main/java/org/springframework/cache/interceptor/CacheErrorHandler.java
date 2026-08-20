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

package org.springframework.cache.interceptor;

import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

/**
 * 处理缓存相关错误的策略。在大多数情况下，缓存提供者抛出的任何异常
 * 都应直接抛回给客户端，但在某些情况下，基础设施可能需要以不同的
 * 方式处理缓存提供者抛出的异常。
 *
 * <p>通常，使用给定 id 从缓存中获取对象失败时，可以不抛出该异常，
 * 而是将其透明地当作缓存未命中（cache miss）来处理。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface CacheErrorHandler {

	/**
	 * 处理缓存提供者在按指定 {@code key} 获取条目时抛出的给定运行时异常，
	 * 可能会将其作为致命异常重新抛出。
	 * @param exception 缓存提供者抛出的异常
	 * @param cache 缓存
	 * @param key 用于获取条目的键
	 * @see Cache#get(Object)
	 */
	void handleCacheGetError(RuntimeException exception, Cache cache, Object key);

	/**
	 * 处理缓存提供者在按指定 {@code key} 和 {@code value} 更新条目时抛出的
	 * 给定运行时异常，可能会将其作为致命异常重新抛出。
	 * @param exception 缓存提供者抛出的异常
	 * @param cache 缓存
	 * @param key 用于更新条目的键
	 * @param value 要与该键关联的值
	 * @see Cache#put(Object, Object)
	 */
	void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value);

	/**
	 * 处理缓存提供者在按指定 {@code key} 清除条目时抛出的给定运行时异常，
	 * 可能会将其作为致命异常重新抛出。
	 * @param exception 缓存提供者抛出的异常
	 * @param cache 缓存
	 * @param key 用于清除条目的键
	 */
	void handleCacheEvictError(RuntimeException exception, Cache cache, Object key);

	/**
	 * 处理缓存提供者在清除指定 {@link Cache} 时抛出的给定运行时异常，
	 * 可能会将其作为致命异常重新抛出。
	 * @param exception 缓存提供者抛出的异常
	 * @param cache 要清除的缓存
	 */
	void handleCacheClearError(RuntimeException exception, Cache cache);

}
