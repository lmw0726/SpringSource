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
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;

import org.aspectj.lang.annotation.RequiredTypes;
import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.jcache.interceptor.JCacheAspectSupport;

/**
 * 使用 JSR-107 标准注解的具体 AspectJ 缓存切面（aspect）。
 *
 * <p>使用此切面时，<i>必须</i>在实现类（和/或该类中的方法）上标注注解，<i>而不要</i>
 * 在类所实现的接口（如果有）上标注。AspectJ 遵循 Java 的规则：接口上的注解<i>不会</i>
 * 被继承。
 *
 * <p>任何方法都可以标注注解（无论可见性如何）。直接为非 public 方法标注注解，
 * 是为这类操作的执行提供缓存边界（caching demarcation）的唯一方式。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@RequiredTypes({"org.springframework.cache.jcache.interceptor.JCacheAspectSupport", "javax.cache.annotation.CacheResult"})
public aspect JCacheCacheAspect extends JCacheAspectSupport {

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
			return null; // 永远不会执行到这里
		}
	}

	/**
	* 切点（pointcut）定义：匹配的连接点（join point）将应用 JSR-107
	* 缓存管理。
	*/
	protected pointcut cacheMethodExecution(Object cachedObject) :
			(executionOfCacheResultMethod()
				|| executionOfCachePutMethod()
				|| executionOfCacheRemoveMethod()
				|| executionOfCacheRemoveAllMethod())
			&& this(cachedObject);

	/**
	 * 匹配带有 @{@link CacheResult} 注解的任意方法的执行。
	 */
	private pointcut executionOfCacheResultMethod() :
		execution(@CacheResult * *(..));

	/**
	 * 匹配带有 @{@link CachePut} 注解的任意方法的执行。
	 */
	private pointcut executionOfCachePutMethod() :
		execution(@CachePut * *(..));

	/**
	 * 匹配带有 @{@link CacheRemove} 注解的任意方法的执行。
	 */
	private pointcut executionOfCacheRemoveMethod() :
		execution(@CacheRemove * *(..));

	/**
	 * 匹配带有 @{@link CacheRemoveAll} 注解的任意方法的执行。
	 */
	private pointcut executionOfCacheRemoveAllMethod() :
		execution(@CacheRemoveAll * *(..));


}
