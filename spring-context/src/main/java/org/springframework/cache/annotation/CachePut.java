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

package org.springframework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示某个方法（或类上的所有方法）触发
 * {@link org.springframework.cache.Cache#put(Object, Object) 缓存写入（cache put）}操作的注解。
 *
 * <p>与 {@link Cacheable @Cacheable} 注解不同，本注解不会导致被通知（advised）的方法被跳过。
 * 相反，它总是会调用该方法，并且当 {@link #condition()} 和 {@link #unless()} 表达式匹配时，
 * 会将方法结果存储到关联的缓存中。请注意，Java 8 的 {@code Optional} 返回类型会被自动处理，
 * 如果其中包含内容，则会将其内容存储到缓存中。
 *
 * <p>该注解可以用作<em>元注解（meta-annotation）</em>，通过属性覆盖来创建自定义的
 * <em>组合注解（composed annotations）</em>。
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.1
 * @see CacheConfig
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CachePut {

	/**
	 * {@link #cacheNames} 的别名。
	 */
	@AliasFor("cacheNames")
	String[] value() default {};

	/**
	 * 用于缓存写入（cache put）操作的缓存名称。
	 * <p>这些名称可用于确定目标缓存（一个或多个），与特定 bean 定义的限定符值
	 * 或 bean 名称相匹配。
	 * @since 4.2
	 * @see #value
	 * @see CacheConfig#cacheNames
	 */
	@AliasFor("value")
	String[] cacheNames() default {};

	/**
	 * 用于动态计算 key 的 Spring 表达式语言（SpEL）表达式。
	 * <p>默认值为 {@code ""}，表示在未设置自定义 {@link #keyGenerator} 的情况下，
	 * 所有方法参数都将被视为 key。
	 * <p>SpEL 表达式会针对一个提供以下元数据的专用上下文进行求值：
	 * <ul>
	 * <li>{@code #result} 表示对方法调用结果的引用。对于受支持的包装器（如 {@code Optional}），
	 * {@code #result} 指向实际对象，而不是包装器</li>
	 * <li>{@code #root.method}、{@code #root.target} 和 {@code #root.caches}
	 * 分别表示对 {@link java.lang.reflect.Method 方法}、目标对象以及受影响的缓存
	 * （一个或多个）的引用。</li>
	 * <li>方法名称（{@code #root.methodName}）和目标类（{@code #root.targetClass}）的
	 * 快捷方式也可用。
	 * <li>方法参数可以按索引访问。例如，第二个参数可以通过 {@code #root.args[1]}、
	 * {@code #p1} 或 {@code #a1} 访问。如果该信息可用，参数也可以通过名称访问。</li>
	 * </ul>
	 */
	String key() default "";

	/**
	 * 要使用的自定义 {@link org.springframework.cache.interceptor.KeyGenerator} 的 bean 名称。
	 * <p>与 {@link #key} 属性互斥。
	 * @see CacheConfig#keyGenerator
	 */
	String keyGenerator() default "";

	/**
	 * 自定义 {@link org.springframework.cache.CacheManager} 的 bean 名称，用于在尚未设置
	 * {@link org.springframework.cache.interceptor.CacheResolver} 时创建默认的
	 * {@link org.springframework.cache.interceptor.CacheResolver}。
	 * <p>与 {@link #cacheResolver} 属性互斥。
	 * @see org.springframework.cache.interceptor.SimpleCacheResolver
	 * @see CacheConfig#cacheManager
	 */
	String cacheManager() default "";

	/**
	 * 要使用的自定义 {@link org.springframework.cache.interceptor.CacheResolver} 的 bean 名称。
	 * @see CacheConfig#cacheResolver
	 */
	String cacheResolver() default "";

	/**
	 * 用于使缓存写入（cache put）操作具备条件性的 Spring 表达式语言（SpEL）表达式。
	 * <p>由于写入（put）操作的特性，该表达式在方法被调用之后才进行求值，
	 * 因此可以引用 {@code result}。
	 * <p>默认值为 {@code ""}，表示方法结果始终会被缓存。
	 * <p>SpEL 表达式会针对一个提供以下元数据的专用上下文进行求值：
	 * <ul>
	 * <li>{@code #result} 表示对方法调用结果的引用。对于受支持的包装器（如 {@code Optional}），
	 * {@code #result} 指向实际对象，而不是包装器</li>
	 * <li>{@code #root.method}、{@code #root.target} 和 {@code #root.caches}
	 * 分别表示对 {@link java.lang.reflect.Method 方法}、目标对象以及受影响的缓存
	 * （一个或多个）的引用。</li>
	 * <li>方法名称（{@code #root.methodName}）和目标类（{@code #root.targetClass}）的
	 * 快捷方式也可用。
	 * <li>方法参数可以按索引访问。例如，第二个参数可以通过 {@code #root.args[1]}、
	 * {@code #p1} 或 {@code #a1} 访问。如果该信息可用，参数也可以通过名称访问。</li>
	 * </ul>
	 */
	String condition() default "";

	/**
	 * 用于否决（veto）缓存写入（cache put）操作的 Spring 表达式语言（SpEL）表达式。
	 * <p>默认值为 {@code ""}，表示缓存永远不会被否决。
	 * <p>SpEL 表达式会针对一个提供以下元数据的专用上下文进行求值：
	 * <ul>
	 * <li>{@code #result} 表示对方法调用结果的引用。对于受支持的包装器（如 {@code Optional}），
	 * {@code #result} 指向实际对象，而不是包装器</li>
	 * <li>{@code #root.method}、{@code #root.target} 和 {@code #root.caches}
	 * 分别表示对 {@link java.lang.reflect.Method 方法}、目标对象以及受影响的缓存
	 * （一个或多个）的引用。</li>
	 * <li>方法名称（{@code #root.methodName}）和目标类（{@code #root.targetClass}）的
	 * 快捷方式也可用。
	 * <li>方法参数可以按索引访问。例如，第二个参数可以通过 {@code #root.args[1]}、
	 * {@code #p1} 或 {@code #a1} 访问。如果该信息可用，参数也可以通过名称访问。</li>
	 * </ul>
	 * @since 3.2
	 */
	String unless() default "";

}
