/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

/**
 * 使用 Spring 的 @{@link Cacheable} 注解的具体 AspectJ 缓存切面（aspect）。
 *
 * <p>使用此切面时，<i>必须</i>在实现类（以及/或者该类中的方法）上标注注解，
 * <i>而不要</i>标注在该类所实现的接口（如果有的话）上。AspectJ 遵循 Java 的规则，
 * 即接口上的注解是<i>不会</i>被继承的。
 *
 * <p>类上的 {@code @Cacheable} 注解为类中任何 <b>public</b> 操作的执行指定默认的
 * 缓存（cache）语义。
 *
 * <p>类中方法上的 {@code @Cacheable} 注解会覆盖类注解（如果存在）给出的默认缓存
 * 语义。任何方法都可以标注注解（无论其可见性如何）。直接标注非 public 方法是
 * 为这类操作的执行划定缓存边界的唯一途径。
 *
 * @author Costin Leau
 * @since 3.1
 */
public aspect AnnotationCacheAspect extends AbstractCacheAspect {

	public AnnotationCacheAspect() {
		super(new AnnotationCacheOperationSource(false));
	}

	/**
	 * 匹配带有 @{@link Cacheable} 注解的类型（或其任意子类型）中任何 public 方法的执行。
	 */
	private pointcut executionOfAnyPublicMethodInAtCacheableType() :
		execution(public * ((@Cacheable *)+).*(..)) && within(@Cacheable *);

	/**
	 * 匹配带有 @{@link CacheEvict} 注解的类型（或其任意子类型）中任何 public 方法的执行。
	 */
	private pointcut executionOfAnyPublicMethodInAtCacheEvictType() :
		execution(public * ((@CacheEvict *)+).*(..)) && within(@CacheEvict *);

	/**
	 * 匹配带有 @{@link CachePut} 注解的类型（或其任意子类型）中任何 public 方法的执行。
	 */
	private pointcut executionOfAnyPublicMethodInAtCachePutType() :
		execution(public * ((@CachePut *)+).*(..)) && within(@CachePut *);

	/**
	 * 匹配带有 @{@link Caching} 注解的类型（或其任意子类型）中任何 public 方法的执行。
	 */
	private pointcut executionOfAnyPublicMethodInAtCachingType() :
		execution(public * ((@Caching *)+).*(..)) && within(@Caching *);

	/**
	 * 匹配带有 @{@link Cacheable} 注解的任何方法的执行。
	 */
	private pointcut executionOfCacheableMethod() :
		execution(@Cacheable * *(..));

	/**
	 * 匹配带有 @{@link CacheEvict} 注解的任何方法的执行。
	 */
	private pointcut executionOfCacheEvictMethod() :
		execution(@CacheEvict * *(..));

	/**
	 * 匹配带有 @{@link CachePut} 注解的任何方法的执行。
	 */
	private pointcut executionOfCachePutMethod() :
		execution(@CachePut * *(..));

	/**
	 * 匹配带有 @{@link Caching} 注解的任何方法的执行。
	 */
	private pointcut executionOfCachingMethod() :
		execution(@Caching * *(..));

	/**
	 * 来自父切面（super aspect）的 pointcut（切点）定义——匹配到的连接点（join point）
	 * 将应用 Spring 缓存（cache）管理。
	 */
	protected pointcut cacheMethodExecution(Object cachedObject) :
		(executionOfAnyPublicMethodInAtCacheableType()
				|| executionOfAnyPublicMethodInAtCacheEvictType()
				|| executionOfAnyPublicMethodInAtCachePutType()
				|| executionOfAnyPublicMethodInAtCachingType()
				|| executionOfCacheableMethod()
				|| executionOfCacheEvictMethod()
				|| executionOfCachePutMethod()
				|| executionOfCachingMethod())
			&& this(cachedObject);
}