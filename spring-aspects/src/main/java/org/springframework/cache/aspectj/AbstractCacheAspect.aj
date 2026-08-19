/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cache.aspectj;

import java.lang.reflect.Method;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheOperationSource;

/**
 * AspectJ 缓存切面（aspect）的抽象超切面。具体子切面将使用诸如 Java 5 注解之类的策略来实现
 * {@link #cacheMethodExecution} 切点（pointcut）。
 *
 * <p>既可在 Spring IoC 容器内部使用，也可在容器外部使用。请适当设置
 * {@link #setCacheManager cacheManager} 属性，以便能够使用 Spring 支持的任何缓存实现。
 *
 * <p><b>注意：</b> 如果某个方法实现了本身带有缓存注解的接口，则相应的
 * Spring 缓存定义将<i>不会</i>被解析。
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @since 3.1
 */
public abstract aspect AbstractCacheAspect extends CacheAspectSupport implements DisposableBean {

	protected AbstractCacheAspect() {
	}

	/**
	 * 使用给定的缓存元数据检索策略构造对象。
	 * @param cos {@link CacheOperationSource} 实现，用于为每个连接点（joinpoint）检索 Spring 缓存元数据。
	 */
	protected AbstractCacheAspect(CacheOperationSource... cos) {
		setCacheOperationSources(cos);
	}

	@Override
	public void destroy() {
		clearMetadataCache(); // 切面（aspect）本质上是一个单例（singleton）
	}

	@SuppressAjWarnings("adviceDidNotMatch")
	Object around(final Object cachedObject) : cacheMethodExecution(cachedObject) {
		MethodSignature methodSignature = (MethodSignature) thisJoinPoint.getSignature();
		Method method = methodSignature.getMethod();

		CacheOperationInvoker aspectJInvoker = new CacheOperationInvoker() {
			public Object invoke() {
				try {
					return proceed(cachedObject);
				}
				catch (Throwable ex) {
					throw new ThrowableWrapper(ex);
				}
			}
		};

		try {
			return execute(aspectJInvoker, thisJoinPoint.getTarget(), method, thisJoinPoint.getArgs());
		}
		catch (CacheOperationInvoker.ThrowableWrapper th) {
			AnyThrow.throwUnchecked(th.getOriginal());
			return null; // 永远不会执行到此处
		}
	}

	/**
	 * 具体子切面必须实现此切点（pointcut），用于识别被缓存的方法。
	 */
	protected abstract pointcut cacheMethodExecution(Object cachedObject);

}
