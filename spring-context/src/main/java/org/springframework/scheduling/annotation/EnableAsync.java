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

package org.springframework.scheduling.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * 启用 Spring 的异步方法执行能力，类似于 Spring 的 {@code <task:*>} XML 命名空间中的功能。
 *
 * <p>与 @{@link Configuration Configuration} 类一起使用如下，
 * 为整个 Spring 应用上下文启用注解驱动的异步处理：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAsync
 * public class AppConfig {
 *
 * }</pre>
 *
 * {@code MyAsyncBean} 是一个用户定义的类型，其一个或多个方法使用了 Spring 的
 * {@code @Async} 注解、EJB 3.1 的 {@code @javax.ejb.Asynchronous} 注解，
 * 或通过 {@link #annotation} 属性指定的任何自定义注解。
 * 切面会透明地添加到任何已注册的 Bean 上，例如通过以下配置：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AnotherAppConfig {
 *
 *     &#064;Bean
 *     public MyAsyncBean asyncBean() {
 *         return new MyAsyncBean();
 *     }
 * }</pre>
 *
 * <p>默认情况下，Spring 将搜索关联的线程池定义：
 * 要么是上下文中唯一的 {@link org.springframework.core.task.TaskExecutor} Bean，
 * 要么是名为 "taskExecutor" 的 {@link java.util.concurrent.Executor} Bean。
 * 如果这两个都无法解析，则将使用
 * {@link org.springframework.core.task.SimpleAsyncTaskExecutor} 来处理异步方法调用。
 * 此外，返回类型为 {@code void} 的注解方法无法将任何异常传递回调用者。
 * 默认情况下，这些未捕获的异常仅被记录日志。
 *
 * <p>要自定义以上所有配置，请实现 {@link AsyncConfigurer} 并提供：
 * <ul>
 * <li>通过 {@link AsyncConfigurer#getAsyncExecutor getAsyncExecutor()} 方法提供
 * 您自己的 {@link java.util.concurrent.Executor Executor}，以及</li>
 * <li>通过 {@link AsyncConfigurer#getAsyncUncaughtExceptionHandler
 * getAsyncUncaughtExceptionHandler()} 方法提供
 * 您自己的 {@link org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
 * AsyncUncaughtExceptionHandler}。</li>
 * </ul>
 *
 * <p><b>注意：{@link AsyncConfigurer} 配置类在应用上下文引导过程中较早初始化。
 * 如果您在那里需要依赖其他 Bean，请确保尽可能将它们声明为"延迟加载"，
 * 以便它们也能经过其他后处理器的处理。</b>
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAsync
 * public class AppConfig implements AsyncConfigurer {
 *
 *     &#064;Override
 *     public Executor getAsyncExecutor() {
 *         ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 *         executor.setCorePoolSize(7);
 *         executor.setMaxPoolSize(42);
 *         executor.setQueueCapacity(11);
 *         executor.setThreadNamePrefix("MyExecutor-");
 *         executor.initialize();
 *         return executor;
 *     }
 *
 *     &#064;Override
 *     public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
 *         return new MyAsyncUncaughtExceptionHandler();
 *     }
 * }</pre>
 *
 * <p>如果只需要自定义其中一项，可以返回 {@code null} 以保留默认设置。
 * 尽可能考虑也继承 {@link AsyncConfigurerSupport}。
 *
 * <p>注意：在上面的示例中，{@code ThreadPoolTaskExecutor} 不是一个完全受管理的
 * Spring Bean。如果您想要一个完全受管理的 Bean，请在 {@code getAsyncExecutor()} 方法上
 * 添加 {@code @Bean} 注解。在这种情况下，不再需要手动调用
 * {@code executor.initialize()} 方法，因为该方法会在 Bean 初始化时自动调用。
 *
 * <p>作为参考，上述示例可以与以下 Spring XML 配置进行比较：
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;task:annotation-driven executor="myExecutor" exception-handler="exceptionHandler"/&gt;
 *
 *     &lt;task:executor id="myExecutor" pool-size="7-42" queue-capacity="11"/&gt;
 *
 *     &lt;bean id="asyncBean" class="com.foo.MyAsyncBean"/&gt;
 *
 *     &lt;bean id="exceptionHandler" class="com.foo.MyAsyncUncaughtExceptionHandler"/&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * 上述基于 XML 和基于 JavaConfig 的示例是等价的，唯一的区别在于
 * {@code Executor} 的<em>线程名称前缀</em>设置；这是因为
 * {@code <task:executor>} 元素没有暴露这样的属性。
 * 这演示了基于 JavaConfig 的方法如何通过直接访问实际组件来实现最大的可配置性。
 *
 * <p>{@link #mode} 属性控制通知的应用方式：如果模式是
 * {@link AdviceMode#PROXY}（默认值），则其他属性控制代理的行为。
 * 请注意，代理模式仅允许拦截通过代理的调用；
 * 同一类中的本地调用无法以这种方式被拦截。
 *
 * <p>请注意，如果 {@linkplain #mode} 设置为 {@link AdviceMode#ASPECTJ}，
 * 则 {@link #proxyTargetClass} 属性的值将被忽略。另请注意，在这种情况下，
 * {@code spring-aspects} 模块 JAR 必须在类路径上，并且需要编译时织入或加载时织入
 * 将切面应用于受影响的类。在这种场景中不涉及代理；本地调用也将被拦截。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.1
 * @see Async
 * @see AsyncConfigurer
 * @see AsyncConfigurationSelector
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AsyncConfigurationSelector.class)
public @interface EnableAsync {

	/**
	 * 指定在类级别或方法级别检测的"异步"注解类型。
	 * <p>默认情况下，将同时检测 Spring 的 @{@link Async} 注解和
	 * EJB 3.1 的 {@code @javax.ejb.Asynchronous} 注解。
	 * <p>此属性的存在是为了让开发者可以提供自己的自定义注解类型，
	 * 以指示某个方法（或给定类的所有方法）应被异步调用。
	 */
	Class<? extends Annotation> annotation() default Annotation.class;

	/**
	 * 指定是否创建基于子类（CGLIB）的代理，而不是标准的基于 Java 接口的代理。
	 * <p><strong>仅当 {@link #mode} 设置为 {@link AdviceMode#PROXY} 时适用。</strong>
	 * <p>默认值为 {@code false}。
	 * <p>请注意，将此属性设置为 {@code true} 将影响<em>所有</em>需要代理的
	 * Spring 管理 Bean，而不仅仅是标记了 {@code @Async} 的 Bean。
	 * 例如，其他标记了 Spring 的 {@code @Transactional} 注解的 Bean
	 * 也将同时升级为子类代理。这种方法在实践中没有负面影响，除非有人明确期望
	 * 某种类型的代理 &mdash; 例如在测试中。
	 */
	boolean proxyTargetClass() default false;

	/**
	 * 指定异步通知的应用方式。
	 * <p><b>默认值为 {@link AdviceMode#PROXY}。</b>
	 * 请注意，代理模式仅允许拦截通过代理的调用。
	 * 同一类中的本地调用无法以这种方式被拦截；在本地调用中
	 * 方法上的 {@link Async} 注解将被忽略，因为 Spring 的拦截器在这种运行时场景下根本不会启动。
	 * 要使用更高级的拦截模式，请考虑将其切换为
	 * {@link AdviceMode#ASPECTJ}。
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * 指定 {@link AsyncAnnotationBeanPostProcessor} 的应用顺序。
	 * <p>默认值为 {@link Ordered#LOWEST_PRECEDENCE}，以便在所有其他后处理器之后运行，
	 * 这样它可以向现有代理添加通知器，而不是进行双重代理。
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
