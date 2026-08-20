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
import java.util.concurrent.Callable;

import org.springframework.core.annotation.AliasFor;

/**
 * 表示调用方法（或类中的所有方法）的结果可以被缓存的注解。
 *
 * <p>每次调用被通知（advised）的方法时，都会应用缓存行为，
 * 检查该方法是否已经针对给定的参数被调用过。
 * 一个合理的默认实现是直接使用方法参数来计算缓存键，但
 * 也可以通过 {@link #key} 属性提供 SpEL 表达式，或者使用自定义的
 * {@link org.springframework.cache.interceptor.KeyGenerator} 实现来
 * 替换默认实现（参见 {@link #keyGenerator}）。
 *
 * <p>如果根据计算出的缓存键在缓存中未找到任何值，则会调用目标方法，
 * 并将返回值存入关联的缓存中。
 * 注意，{@link java.util.Optional} 返回类型会被自动解包。
 * 如果 {@code Optional} 值为 {@linkplain java.util.Optional#isPresent()
 * present}（存在），则将其存入关联的缓存；如果 {@code Optional}
 * 值不存在，则将 {@code null} 存入关联的缓存。
 *
 * <p>该注解可以用作 <em>元注解（meta-annotation）</em>，通过属性覆盖来创建
 * 自定义的 <em>组合注解（composed annotations）</em>。
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
public @interface Cacheable {

	/**
	 * {@link #cacheNames} 的别名。
	 */
	@AliasFor("cacheNames")
	String[] value() default {};

	/**
	 * 存储方法调用结果的缓存名称。
	 * <p>这些名称可用于确定目标缓存（或缓存集合），与特定 bean 定义的
	 * 限定符值或 bean 名称相匹配。
	 * @since 4.2
	 * @see #value
	 * @see CacheConfig#cacheNames
	 */
	@AliasFor("value")
	String[] cacheNames() default {};

	/**
	 * 用于动态计算缓存键的 Spring 表达式语言（SpEL）表达式。
	 * <p>默认值为 {@code ""}，表示将所有方法参数视为缓存键，
	 * 除非配置了自定义的 {@link #keyGenerator}。
	 * <p>SpEL 表达式在一个专用上下文中求值，该上下文提供以下元数据：
	 * <ul>
	 * <li>{@code #root.method}、{@code #root.target} 和 {@code #root.caches}，
	 * 分别用于引用 {@link java.lang.reflect.Method 方法}、目标对象以及
	 * 受影响的缓存。</li>
	 * <li>还可以使用方法名称（{@code #root.methodName}）和目标类
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
	 * 自定义 {@link org.springframework.cache.CacheManager} 的 bean 名称，
	 * 当尚未设置 {@link org.springframework.cache.interceptor.CacheResolver} 时，
	 * 使用它来创建默认的 {@link org.springframework.cache.interceptor.CacheResolver}。
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
	 * 用于使方法缓存具有条件性的 Spring 表达式语言（SpEL）表达式。
	 * <p>默认值为 {@code ""}，表示方法结果始终被缓存。
	 * <p>SpEL 表达式在一个专用上下文中求值，该上下文提供以下元数据：
	 * <ul>
	 * <li>{@code #root.method}、{@code #root.target} 和 {@code #root.caches}，
	 * 分别用于引用 {@link java.lang.reflect.Method 方法}、目标对象以及
	 * 受影响的缓存。</li>
	 * <li>还可以使用方法名称（{@code #root.methodName}）和目标类
	 * （{@code #root.targetClass}）的快捷方式。
	 * <li>方法参数可以通过索引访问。例如，第二个参数可以通过
	 * {@code #root.args[1]}、{@code #p1} 或 {@code #a1} 访问。如果该信息可用，
	 * 参数也可以通过名称访问。</li>
	 * </ul>
	 */
	String condition() default "";

	/**
	 * 用于否决方法缓存的 Spring 表达式语言（SpEL）表达式。
	 * <p>与 {@link #condition} 不同，该表达式在方法被调用之后求值，
	 * 因此可以引用 {@code result}。
	 * <p>默认值为 {@code ""}，表示缓存永远不会被否决。
	 * <p>SpEL 表达式在一个专用上下文中求值，该上下文提供以下元数据：
	 * <ul>
	 * <li>{@code #result} 用于引用方法调用的结果。对于诸如 {@code Optional}
	 * 之类的受支持包装类型，{@code #result} 指向实际对象，而不是包装器。</li>
	 * <li>{@code #root.method}、{@code #root.target} 和 {@code #root.caches}，
	 * 分别用于引用 {@link java.lang.reflect.Method 方法}、目标对象以及
	 * 受影响的缓存。</li>
	 * <li>还可以使用方法名称（{@code #root.methodName}）和目标类
	 * （{@code #root.targetClass}）的快捷方式。
	 * <li>方法参数可以通过索引访问。例如，第二个参数可以通过
	 * {@code #root.args[1]}、{@code #p1} 或 {@code #a1} 访问。如果该信息可用，
	 * 参数也可以通过名称访问。</li>
	 * </ul>
	 * @since 3.2
	 */
	String unless() default "";

	/**
	 * 当多个线程尝试为同一个缓存键加载值时，同步底层方法的调用。
	 * 这种同步会带来一些限制：
	 * <ol>
	 * <li>不支持 {@link #unless()}</li>
	 * <li>只能指定一个缓存</li>
	 * <li>不能与其他缓存相关操作组合使用</li>
	 * </ol>
	 * 这实际上只是一个提示，你正在使用的实际缓存提供者可能不支持
	 * 以同步的方式进行该操作。有关实际语义的更多细节，
	 * 请查阅你的提供者文档。
	 * @since 4.3
	 * @see org.springframework.cache.Cache#get(Object, Callable)
	 */
	boolean sync() default false;

}
