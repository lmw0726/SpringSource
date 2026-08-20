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

package org.springframework.cache.interceptor;

import java.util.Collection;

import org.springframework.cache.Cache;

/**
 * 确定被拦截的方法调用所要使用的 {@link Cache} 实例。
 *
 * <p>实现必须是线程安全的。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@FunctionalInterface
public interface CacheResolver {

	/**
	 * 返回指定调用所要使用的缓存。
	 * @param context 特定调用的上下文
	 * @return 要使用的缓存（绝不会为 {@code null}）
	 * @throws IllegalStateException 如果缓存解析失败
	 */
	Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context);

}
