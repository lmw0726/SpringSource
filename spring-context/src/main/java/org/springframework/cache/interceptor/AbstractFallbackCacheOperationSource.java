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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.MethodClassKey;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link CacheOperation} 的抽象实现，会缓存方法的属性（attributes）并实现回退（fallback）策略：
 * 1. 具体的目标方法；2. 目标类；3. 声明方法；4. 声明类/接口。
 *
 * <p>默认使用目标类的缓存属性（如果目标方法没有关联任何缓存属性）。
 * 与目标方法关联的任何缓存属性都会完全覆盖类的缓存属性。
 * 如果在目标类上未找到，则会检查被调用方法所经由的接口（在 JDK 代理的情况下）。
 *
 * <p>本实现会在方法第一次被使用后按方法缓存其属性。
 * 如果将来希望允许动态修改可缓存属性（这种情况非常少见），
 * 可以将缓存行为改为可配置的。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractFallbackCacheOperationSource implements CacheOperationSource {

	/**
	 * 缓存中保存的规范值，用于表示未为此方法找到缓存属性，
	 * 并且我们无需再次查找。
	 */
	private static final Collection<CacheOperation> NULL_CACHING_ATTRIBUTE = Collections.emptyList();


	/**
	 * 可供子类使用的 Logger。
	 * <p>由于此基类未标记为 Serializable，序列化后 Logger 会被重新创建——
	 * 前提是具体的子类是可序列化的。
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * CacheOperation 的缓存，以特定目标类上的方法为键。
	 * <p>由于此基类未标记为 Serializable，序列化后缓存会被重新创建——
	 * 前提是具体的子类是可序列化的。
	 */
	private final Map<Object, Collection<CacheOperation>> attributeCache = new ConcurrentHashMap<>(1024);


	/**
	 * 确定此方法调用的缓存属性。
	 * <p>如果未找到方法属性，则默认使用类的缓存属性。
	 * @param method 当前调用的方法（绝不会是 {@code null}）
	 * @param targetClass 此调用的目标类（可能为 {@code null}）
	 * @return 此方法的 {@link CacheOperation}；如果该方法不可缓存则返回 {@code null}
	 */
	@Override
	@Nullable
	public Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
		if (method.getDeclaringClass() == Object.class) {
			return null;
		}

		Object cacheKey = getCacheKey(method, targetClass);
		Collection<CacheOperation> cached = this.attributeCache.get(cacheKey);

		if (cached != null) {
			return (cached != NULL_CACHING_ATTRIBUTE ? cached : null);
		}
		else {
			Collection<CacheOperation> cacheOps = computeCacheOperations(method, targetClass);
			if (cacheOps != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Adding cacheable method '" + method.getName() + "' with attribute: " + cacheOps);
				}
				this.attributeCache.put(cacheKey, cacheOps);
			}
			else {
				this.attributeCache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
			}
			return cacheOps;
		}
	}

	/**
	 * 为给定的方法和目标类确定缓存键。
	 * <p>不得为重载方法生成相同的键。
	 * 必须为同一方法的不同实例生成相同的键。
	 * @param method 方法（绝不会是 {@code null}）
	 * @param targetClass 目标类（可能为 {@code null}）
	 * @return 缓存键（绝不会是 {@code null}）
	 */
	protected Object getCacheKey(Method method, @Nullable Class<?> targetClass) {
		return new MethodClassKey(method, targetClass);
	}

	@Nullable
	private Collection<CacheOperation> computeCacheOperations(Method method, @Nullable Class<?> targetClass) {
		// 按配置不允许非 public 方法。
		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
			return null;
		}

		// 该方法可能位于接口上，但我们需要的是目标类上的属性。
		// 如果目标类为 null，则方法保持不变。
		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

		// 第一次尝试：目标类中的方法。
		Collection<CacheOperation> opDef = findCacheOperations(specificMethod);
		if (opDef != null) {
			return opDef;
		}

		// 第二次尝试：目标类上的缓存操作。
		opDef = findCacheOperations(specificMethod.getDeclaringClass());
		if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
			return opDef;
		}

		if (specificMethod != method) {
			// 回退：查看原始方法。
			opDef = findCacheOperations(method);
			if (opDef != null) {
				return opDef;
			}
			// 最后的回退：原始方法所属的类。
			opDef = findCacheOperations(method.getDeclaringClass());
			if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
				return opDef;
			}
		}

		return null;
	}


	/**
	 * 子类需要实现此方法，以返回给定类的缓存属性（如果有）。
	 * @param clazz 要检索属性的类
	 * @return 与此类关联的所有缓存属性；如果没有则返回 {@code null}
	 */
	@Nullable
	protected abstract Collection<CacheOperation> findCacheOperations(Class<?> clazz);

	/**
	 * 子类需要实现此方法，以返回给定方法的缓存属性（如果有）。
	 * @param method 要检索属性的方法
	 * @return 与此方法关联的所有缓存属性；如果没有则返回 {@code null}
	 */
	@Nullable
	protected abstract Collection<CacheOperation> findCacheOperations(Method method);

	/**
	 * 是否只允许 public 方法具有缓存语义？
	 * <p>默认实现返回 {@code false}。
	 */
	protected boolean allowPublicMethodsOnly() {
		return false;
	}

}
