/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.cache.jcache.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.MethodClassKey;
import org.springframework.lang.Nullable;

/**
 * {@link JCacheOperationSource} 的抽象实现，缓存方法的属性
 * 并实现回退策略：1. 特定目标方法；2. 声明方法。
 *
 * <p>此实现在首次使用后按方法缓存属性。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.cache.interceptor.AbstractFallbackCacheOperationSource
 */
public abstract class AbstractFallbackJCacheOperationSource implements JCacheOperationSource {

	/**
	 * 缓存中保存的规范值，表示未找到此方法的缓存属性，
	 * 无需再次查找。
	 */
	private static final Object NULL_CACHING_ATTRIBUTE = new Object();


	protected final Log logger = LogFactory.getLog(getClass());

	private final Map<MethodClassKey, Object> cache = new ConcurrentHashMap<>(1024);


	@Override
	public JCacheOperation<?> getCacheOperation(Method method, @Nullable Class<?> targetClass) {
		MethodClassKey cacheKey = new MethodClassKey(method, targetClass);
		Object cached = this.cache.get(cacheKey);

		if (cached != null) {
			return (cached != NULL_CACHING_ATTRIBUTE ? (JCacheOperation<?>) cached : null);
		}
		else {
			JCacheOperation<?> operation = computeCacheOperation(method, targetClass);
			if (operation != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Adding cacheable method '" + method.getName() + "' with operation: " + operation);
				}
				this.cache.put(cacheKey, operation);
			}
			else {
				this.cache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
			}
			return operation;
		}
	}

	@Nullable
	private JCacheOperation<?> computeCacheOperation(Method method, @Nullable Class<?> targetClass) {
		// 不允许非公开方法，按配置执行。
		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
			return null;
		}

		// 该方法可能在接口上，但我们需要从目标类获取属性。
		// 如果目标类为null，则方法保持不变。
		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

		// 首先尝试目标类中的方法。
		JCacheOperation<?> operation = findCacheOperation(specificMethod, targetClass);
		if (operation != null) {
			return operation;
		}
		if (specificMethod != method) {
			// 回退到查看原始方法。
			operation = findCacheOperation(method, targetClass);
			if (operation != null) {
				return operation;
			}
		}
		return null;
	}


	/**
	 * 子类需要实现此方法以返回给定方法的缓存操作（如果有的话）。
	 * @param method 要检索操作的方法
	 * @param targetType 目标类
	 * @return 与此方法关联的缓存操作（如果没有则返回 {@code null}）
	 */
	@Nullable
	protected abstract JCacheOperation<?> findCacheOperation(Method method, @Nullable Class<?> targetType);

	/**
	 * 是否只允许公共方法具有缓存语义？
	 * <p>默认实现返回 {@code false}。
	 */
	protected boolean allowPublicMethodsOnly() {
		return false;
	}

}
