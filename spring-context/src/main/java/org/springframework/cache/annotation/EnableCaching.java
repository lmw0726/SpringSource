/*
 * Copyright 2002-2018 the original author or authors.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * 启用 Spring 基于注解驱动的缓存管理功能，类似于 Spring 的 {@code <cache:*>} XML 命名空间
 * 所提供的支持。需要与 @{@link org.springframework.context.annotation.Configuration Configuration}
 * 类配合使用，用法如下：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableCaching
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         // 配置并返回一个包含 &#064;Cacheable 方法的类
 *         return new MyService();
 *     }
 *
 *     &#064;Bean
 *     public CacheManager cacheManager() {
 *         // 配置并返回 Spring 的 CacheManager SPI 的一个实现
 *         SimpleCacheManager cacheManager = new SimpleCacheManager();
 *         cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
 *         return cacheManager;
 *     }
 * }</pre>
 *
 * <p>作为参考，上面的示例可以与下面的 Spring XML 配置进行对比：
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;cache:annotation-driven/&gt;
 *
 *     &lt;bean id="myService" class="com.foo.MyService"/&gt;
 *
 *     &lt;bean id="cacheManager" class="org.springframework.cache.support.SimpleCacheManager"&gt;
 *         &lt;property name="caches"&gt;
 *             &lt;set&gt;
 *                 &lt;bean class="org.springframework.cache.concurrent.ConcurrentMapCacheFactoryBean"&gt;
 *                     &lt;property name="name" value="default"/&gt;
 *                 &lt;/bean&gt;
 *             &lt;/set&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * 在上述两种场景中，{@code @EnableCaching} 和 {@code
 * <cache:annotation-driven/>} 负责注册驱动注解式缓存管理所需的必要 Spring
 * 组件，例如
 * {@link org.springframework.cache.interceptor.CacheInterceptor CacheInterceptor}，以及
 * 在调用 {@link org.springframework.cache.annotation.Cacheable @Cacheable} 方法时
 * 将拦截器织入调用栈的基于代理或 AspectJ 的通知。
 *
 * <p>如果 JSR-107 API 和 Spring 的 JCache 实现都存在，则还会注册用于管理标准缓存注解的
 * 必要组件。这会创建在调用带有 {@code CacheResult}、{@code CachePut}、{@code CacheRemove} 或
 * {@code CacheRemoveAll} 注解的方法时，将拦截器织入调用栈的基于代理或 AspectJ 的通知。
 *
 * <p><strong>必须注册一个类型为 {@link org.springframework.cache.CacheManager CacheManager}
 * 的 bean</strong>，因为框架没有合理的默认值可作为约定使用。而且，{@code <cache:annotation-driven>} 元素
 * 假定存在一个<em>名为</em> "cacheManager" 的 bean，而 {@code @EnableCaching} 则<em>按类型</em>
 * 查找缓存管理器 bean。因此，缓存管理器 bean 方法的命名并不重要。
 *
 * <p>对于那些希望在 {@code @EnableCaching} 与所使用的确切缓存管理器 bean 之间建立更直接关系的使用者，
 * 可以实现 {@link CachingConfigurer} 回调接口。
 * 请注意下面带有 {@code @Override} 注解的方法：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableCaching
 * public class AppConfig extends CachingConfigurerSupport {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         // 配置并返回一个包含 &#064;Cacheable 方法的类
 *         return new MyService();
 *     }
 *
 *     &#064;Bean
 *     &#064;Override
 *     public CacheManager cacheManager() {
 *         // 配置并返回 Spring 的 CacheManager SPI 的一个实现
 *         SimpleCacheManager cacheManager = new SimpleCacheManager();
 *         cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
 *         return cacheManager;
 *     }
 *
 *     &#064;Bean
 *     &#064;Override
 *     public KeyGenerator keyGenerator() {
 *         // 配置并返回 Spring 的 KeyGenerator SPI 的一个实现
 *         return new MyKeyGenerator();
 *     }
 * }</pre>
 *
 * 采用这种方式可能仅仅是因为它更加明确，也可能是在同一容器中存在两个 {@code CacheManager} bean 时，
 * 有必要用来加以区分。
 *
 * <p>还请留意上面示例中的 {@code keyGenerator} 方法。它允许根据 Spring 的 {@link
 * org.springframework.cache.interceptor.KeyGenerator KeyGenerator} SPI 定制缓存键的生成策略。通常情况下，
 * {@code @EnableCaching} 会为此目的配置 Spring 的
 * {@link org.springframework.cache.interceptor.SimpleKeyGenerator SimpleKeyGenerator}，
 * 但在实现 {@code CachingConfigurer} 时，必须显式提供一个键生成器。
 * 如果无需任何定制，请从该方法返回 {@code null} 或 {@code new SimpleKeyGenerator()}。
 *
 * <p>{@link CachingConfigurer} 还提供了其他定制选项：建议继承自
 * {@link org.springframework.cache.annotation.CachingConfigurerSupport
 * CachingConfigurerSupport}，它为所有方法提供了默认实现，
 * 如果你不需要对一切进行定制，这会非常有用。更多细节请参阅 {@link CachingConfigurer}
 * 的 Javadoc。
 *
 * <p>{@link #mode} 属性控制通知的施加方式：如果模式是
 * {@link AdviceMode#PROXY}（默认值），那么其他属性控制代理的行为。
 * 请注意，代理模式只允许拦截通过代理的调用；同一类内部的本地调用无法以这种方式被拦截。
 *
 * <p>请注意，如果 {@linkplain #mode} 设置为 {@link AdviceMode#ASPECTJ}，那么
 * {@link #proxyTargetClass} 属性的值将被忽略。同样需要注意的是，在这种情况下，
 * classpath 中必须存在 {@code spring-aspects} 模块的 JAR，
 * 并通过编译期织入或加载期织入将切面应用到受影响的类上。
 * 在这种场景下不涉及代理；本地调用同样会被拦截。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see CachingConfigurer
 * @see CachingConfigurationSelector
 * @see ProxyCachingConfiguration
 * @see org.springframework.cache.aspectj.AspectJCachingConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CachingConfigurationSelector.class)
public @interface EnableCaching {

	/**
	 * 指示是创建基于子类（CGLIB）的代理，还是创建标准的基于 Java 接口的代理。
	 * 默认值为 {@code false}。<strong>
	 * 仅当 {@link #mode()} 设置为 {@link AdviceMode#PROXY} 时才适用</strong>。
	 * <p>请注意，将此属性设置为 {@code true} 将影响<em>所有</em>
	 * 需要代理的 Spring 管理的 bean，而不仅仅是那些带有 {@code @Cacheable} 注解的 bean。
	 * 例如，其他带有 Spring 的 {@code @Transactional} 注解的 bean
	 * 也将同时升级为子类代理。这种做法在实践中没有负面影响，
	 * 除非有人明确期望使用某一种代理类型而不是另一种，
	 * 例如在测试中。
	 */
	boolean proxyTargetClass() default false;

	/**
	 * 指示缓存通知应如何施加。
	 * <p><b>默认值为 {@link AdviceMode#PROXY}。</b>
	 * 请注意，代理模式只允许拦截通过代理的调用。
	 * 同一类内部的本地调用无法以这种方式被拦截；
	 * 本地调用中此类方法上的缓存注解将被忽略，
	 * 因为在这种运行时场景下，Spring 的拦截器甚至不会生效。
	 * 如需更高级的拦截模式，请考虑将其切换到
	 * {@link AdviceMode#ASPECTJ}。
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * 指示在特定连接点上施加多个通知时，
	 * 缓存通知器（caching advisor）的执行顺序。
	 * <p>默认值为 {@link Ordered#LOWEST_PRECEDENCE}。
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
