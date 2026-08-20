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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.lang.Nullable;

/**
 * 由 {@link CacheInterceptor} 使用的接口。实现类知道如何获取
 * 缓存操作属性，无论是来自配置、源码级别的元数据属性，
 * 还是其他来源。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
public interface CacheOperationSource {

	/**
	 * 判断给定的类是否是该 {@code CacheOperationSource} 元数据格式下
	 * 缓存操作的候选目标。
	 * <p>如果此方法返回 {@code false}，则不会遍历给定类上的方法
	 * 以执行 {@link #getCacheOperations} 内省。
	 * 因此，为不受影响的类返回 {@code false} 是一种优化，
	 * 而返回 {@code true} 仅表示需要针对给定类上的每个方法
	 * 单独进行完整的内省。
	 * @param targetClass 要进行内省的类
	 * @return 如果已知该类在类级别或方法级别上没有任何缓存操作的
	 * 元数据则返回 {@code false}；否则返回 {@code true}。默认
	 * 实现返回 {@code true}，从而进行常规内省。
	 * @since 5.2
	 */
	default boolean isCandidateClass(Class<?> targetClass) {
		return true;
	}

	/**
	 * 返回此方法的缓存操作集合，
	 * 如果该方法不包含任何 <em>cacheable</em>（可缓存）注解则返回 {@code null}。
	 * @param method 要进行内省的方法
	 * @param targetClass 目标类（可能为 {@code null}，此时
	 * 必须使用该方法的声明类）
	 * @return 此方法的所有缓存操作，若未找到则返回 {@code null}
	 */
	@Nullable
	Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass);

}
