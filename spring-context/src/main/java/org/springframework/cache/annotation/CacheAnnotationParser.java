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

package org.springframework.cache.annotation;

import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.lang.Nullable;

/**
 * 用于解析已知缓存注解类型的策略接口。
 * {@link AnnotationCacheOperationSource} 委托给此类解析器，
 * 以支持特定的注解类型，例如 Spring 自带的
 * {@link Cacheable}、{@link CachePut} 和 {@link CacheEvict}。
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 * @see AnnotationCacheOperationSource
 * @see SpringCacheAnnotationParser
 */
public interface CacheAnnotationParser {

	/**
	 * 判断给定的类是否可能是缓存操作的候选类，
	 * 其注解格式符合本 {@code CacheAnnotationParser} 所理解的格式。
	 * <p>如果此方法返回 {@code false}，则不会对给定类上的方法进行
	 * {@code #parseCacheAnnotations} 内省遍历。
	 * 因此，返回 {@code false} 是对不受影响的类的一种优化，
	 * 而返回 {@code true} 仅表示需要针对给定类上的每个方法
	 * 逐一进行完整的内省。
	 * @param targetClass 需要进行内省的类
	 * @return 如果已知该类在类级别或方法级别都没有缓存操作注解，
	 * 则返回 {@code false}；否则返回 {@code true}。默认实现
	 * 返回 {@code true}，从而进行常规的内省。
	 * @since 5.2
	 */
	default boolean isCandidateClass(Class<?> targetClass) {
		return true;
	}

	/**
	 * 根据此解析器所理解的注解类型，
	 * 为给定的类解析缓存定义。
	 * <p>这本质上是将已知的缓存注解解析为 Spring 的元数据
	 * 属性类。如果该类不可缓存，则返回 {@code null}。
	 * @param type 被注解的类
	 * @return 配置的缓存操作；如果没有找到，则返回 {@code null}
	 * @see AnnotationCacheOperationSource#findCacheOperations(Class)
	 */
	@Nullable
	Collection<CacheOperation> parseCacheAnnotations(Class<?> type);

	/**
	 * 根据此解析器所理解的注解类型，
	 * 为给定的方法解析缓存定义。
	 * <p>这本质上是将已知的缓存注解解析为 Spring 的元数据
	 * 属性类。如果该方法不可缓存，则返回 {@code null}。
	 * @param method 被注解的方法
	 * @return 配置的缓存操作；如果没有找到，则返回 {@code null}
	 * @see AnnotationCacheOperationSource#findCacheOperations(Method)
	 */
	@Nullable
	Collection<CacheOperation> parseCacheAnnotations(Method method);

}
