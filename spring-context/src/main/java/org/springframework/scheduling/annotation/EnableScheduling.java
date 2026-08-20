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

package org.springframework.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Executor;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 启用 Spring 的计划任务执行功能，类似于
 * Spring 的 {@code <task:*>} XML 命名空间中提供的功能。用于
 * {@link Configuration @Configuration} 类，如下所示：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 *
 *     // 各种 &#064;Bean 定义
 * }</pre>
 *
 * <p>这将启用对容器中任何
 * Spring 管理的 Bean 上 {@link Scheduled @Scheduled} 注解的检测。例如，给定一个类 {@code MyTask}：
 *
 * <pre class="code">
 * package com.myco.tasks;
 *
 * public class MyTask {
 *
 *     &#064;Scheduled(fixedRate=1000)
 *     public void work() {
 *         // 任务执行逻辑
 *     }
 * }</pre>
 *
 * <p>以下配置将确保 {@code MyTask.work()} 每 1000 毫秒被调用一次：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyTask task() {
 *         return new MyTask();
 *     }
 * }</pre>
 *
 * <p>或者，如果 {@code MyTask} 被 {@code @Component} 注解标注，则
 * 以下配置将确保其 {@code @Scheduled} 方法
 * 以期望的间隔被调用：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * &#064;ComponentScan(basePackages="com.myco.tasks")
 * public class AppConfig {
 * }</pre>
 *
 * <p>用 {@code @Scheduled} 注解的方法甚至可以直接在
 * {@code @Configuration} 类中声明：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 *
 *     &#064;Scheduled(fixedRate=1000)
 *     public void work() {
 *         // 任务执行逻辑
 *     }
 * }</pre>
 *
 * <p>默认情况下，Spring 将查找关联的调度器定义：要么是上下文中唯一的 {@link org.springframework.scheduling.TaskScheduler} Bean，
 * 要么是名为 "taskScheduler" 的 {@code TaskScheduler} Bean；对于 {@link java.util.concurrent.ScheduledExecutorService}
 * Bean 也将执行相同的查找。如果两者都无法解析，则将在注册器中创建并使用本地单线程默认调度器。
 *
 * <p>当需要更多控制时，{@code @Configuration} 类可以实现
 * {@link SchedulingConfigurer}。这允许访问底层的
 * {@link ScheduledTaskRegistrar} 实例。例如，以下示例
 * 演示了如何自定义用于执行计划任务的 {@link Executor}：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig implements SchedulingConfigurer {
 *
 *     &#064;Override
 *     public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
 *         taskRegistrar.setScheduler(taskExecutor());
 *     }
 *
 *     &#064;Bean(destroyMethod="shutdown")
 *     public Executor taskExecutor() {
 *         return Executors.newScheduledThreadPool(100);
 *     }
 * }</pre>
 *
 * <p>注意上面示例中 {@code @Bean(destroyMethod="shutdown")} 的使用。
 * 这确保了当 Spring 应用程序上下文本身关闭时，任务执行器会被正确关闭。
 *
 * <p>实现 {@code SchedulingConfigurer} 还允许通过 {@code ScheduledTaskRegistrar}
 * 对任务注册进行细粒度控制。
 * 例如，以下配置根据自定义 {@code Trigger} 实现来执行特定 Bean 方法：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig implements SchedulingConfigurer {
 *
 *     &#064;Override
 *     public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
 *         taskRegistrar.setScheduler(taskScheduler());
 *         taskRegistrar.addTriggerTask(
 *             () -&gt; myTask().work(),
 *             new CustomTrigger()
 *         );
 *     }
 *
 *     &#064;Bean(destroyMethod="shutdown")
 *     public Executor taskScheduler() {
 *         return Executors.newScheduledThreadPool(42);
 *     }
 *
 *     &#064;Bean
 *     public MyTask myTask() {
 *         return new MyTask();
 *     }
 * }</pre>
 *
 * <p>作为参考，以上示例可以与以下 Spring XML
 * 配置进行比较：
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;task:annotation-driven scheduler="taskScheduler"/&gt;
 *
 *     &lt;task:scheduler id="taskScheduler" pool-size="42"/&gt;
 *
 *     &lt;task:scheduled-tasks scheduler="taskScheduler"&gt;
 *         &lt;task:scheduled ref="myTask" method="work" fixed-rate="1000"/&gt;
 *     &lt;/task:scheduled-tasks&gt;
 *
 *     &lt;bean id="myTask" class="com.foo.MyTask"/&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * <p>以上示例是等效的，区别仅在于 XML 中使用了 <em>fixed-rate</em> 周期
 * 而不是自定义的 <em>{@code Trigger}</em> 实现；这是因为
 * {@code task:} 命名空间的 {@code scheduled} 无法轻松地暴露此类支持。这仅仅是
 * 演示了基于代码的方式如何通过直接访问实际组件来实现最大的可配置性。
 *
 * <p><b>注意：{@code @EnableScheduling} 仅适用于其本地应用程序上下文，
 * 允许在不同级别对 Bean 进行选择性调度。</b> 如果需要在多个级别应用其行为，
 * 请在每个单独的上下文中重新声明 {@code @EnableScheduling}，例如公共的根 Web
 * 应用程序上下文和任何单独的 {@code DispatcherServlet} 应用程序上下文。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see Scheduled
 * @see SchedulingConfiguration
 * @see SchedulingConfigurer
 * @see ScheduledTaskRegistrar
 * @see Trigger
 * @see ScheduledAnnotationBeanPostProcessor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SchedulingConfiguration.class)
@Documented
public @interface EnableScheduling {

}
