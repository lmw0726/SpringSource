/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 表示某个方法（或某个类上的所有方法）会触发
 * {@link org.springframework.cache.Cache#evict(Object) 缓存逐出}操作的注解。
 *
 * <p>该注解可以用作<em>元注解</em>，通过属性覆盖来创建自定义的
 * <em>组合注解</em>。
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.1
 * @see CacheConfig
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CacheEvict {

	/**
	 * {@link #cacheNames} 的别名。
	 */
	@AliasFor("cacheNames")
	String[] value() default {};

	/**
	 * 用于缓存逐出操作的缓存名称。
	 * <p>这些名称可用于确定目标缓存（或缓存们），需与特定 bean 定义的限定符值
	 * 或 bean 名称相匹配。
	 * @since 4.2
	 * @see #value
	 * @see CacheConfig#cacheNames
	 */
	@AliasFor("value")
	String[] cacheNames() default {};

	/**
	 * 用于动态计算 key 的 Spring 表达式语言（SpEL）表达式。
	 * <p>默认值为 {@code ""}，表示所有方法参数都被视为 key，
	 * 除非已经设置了自定义的 {@link #keyGenerator}。
	 * <p>SpEL 表达式将针对一个专用上下文进行求值，该上下文提供以下元数据：
	 * <ul>
	 * <li>{@code #result} 表示对方法调用结果的引用，只有在
	 * {@link #beforeInvocation()} 为 {@code false} 时才可以使用。对于受支持的
	 * 包装类型（如 {@code Optional}），{@code #result} 引用的是实际对象，
	 * 而不是包装类型本身</li>
	 * <li>{@code #root.method}、{@code #root.target} 和 {@code #root.caches} 分别
	 * 引用 {@link java.lang.reflect.Method 方法}、目标对象以及
	 * 受影响的缓存。</li>
	 * <li>还提供了方法名（{@code #root.methodName}）和目标类
	 * （{@code #root.targetClass}）的快捷方式。
	 * <li>方法参数可以通过索引访问。例如，第二个参数可以通过
	 * {@code #root.args[1]}、{@code #p1} 或 {@code #a1} 访问。如果该信息可用，
	 * 参数也可以通过名称访问。</li>
	 * </ul>
	 */
	String key() default "";

	/**
	 * 要使用的自定义 {@link org.springframework.cache.interceptor.KeyGenerator}
	 * 的 bean 名称。
	 * <p>与 {@link #key} 属性互斥。
	 * @see CacheConfig#keyGenerator
	 */
	String keyGenerator() default "";

	/**
	 * 要使用的自定义 {@link org.springframework.cache.CacheManager} 的 bean 名称，
	 * 用于在尚未设置 {@link org.springframework.cache.interceptor.CacheResolver} 时
	 * 创建一个默认的缓存解析器。
	 * <p>与 {@link #cacheResolver} 属性互斥。
	 * @see org.springframework.cache.interceptor.SimpleCacheResolver
	 * @see CacheConfig#cacheManager
	 */
	String cacheManager() default "";

	/**
	 * 要使用的自定义 {@link org.springframework.cache.interceptor.CacheResolver}
	 * 的 bean 名称。
	 * @see CacheConfig#cacheResolver
	 */
	String cacheResolver() default "";

	/**
	 * 用于使缓存逐出操作满足条件的 Spring 表达式语言（SpEL）表达式。
	 * <p>默认值为 {@code ""}，表示始终执行缓存逐出。
	 * <p>SpEL 表达式将针对一个专用上下文进行求值，该上下文提供以下元数据：
	 * <ul>
	 * <li>{@code #root.method}、{@code #root.target} 和 {@code #root.caches} 分别
	 * 引用 {@link java.lang.reflect.Method 方法}、目标对象以及
	 * 受影响的缓存。</li>
	 * <li>还提供了方法名（{@code #root.methodName}）和目标类
	 * （{@code #root.targetClass}）的快捷方式。
	 * <li>方法参数可以通过索引访问。例如，第二个参数可以通过
	 * {@code #root.args[1]}、{@code #p1} 或 {@code #a1} 访问。如果该信息可用，
	 * 参数也可以通过名称访问。</li>
	 * </ul>
	 */
	String condition() default "";

	/**
	 * 是否移除缓存中的所有条目。
	 * <p>默认情况下，仅移除关联 key 对应的值。
	 * <p>请注意，不允许将此参数设置为 {@code true} 的同时指定
	 * {@link #key}。
	 */
	boolean allEntries() default false;

	/**
	 * 逐出操作是否应在方法被调用之前发生。
	 * <p>将此属性设置为 {@code true} 会导致无论方法的执行结果如何
	 * （即无论是否抛出异常），都会执行逐出操作。
	 * <p>默认为 {@code false}，表示缓存逐出操作将在被通知的方法成功调用
	 * <em>之后</em>发生（即仅当调用未抛出异常时）。
	 */
	boolean beforeInvocation() default false;

}
