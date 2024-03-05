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

package org.springframework.context;

/**
 * 定义用于启动/停止生命周期控制的方法的通用接口。
 * 这个接口的典型用例是控制异步处理。
 * <b>注意：这个接口不暗示特定的自动启动语义。
 * 考虑实现 {@link SmartLifecycle} 来实现这个目的。</b>
 *
 * <p>可以由组件（通常是在 Spring 上下文中定义的 Spring bean）和容器（通常是 Spring {@link ApplicationContext} 本身）来实现。
 * 容器将在每个容器中传播启动/停止信号给所有适用的组件，例如在运行时进行停止/重启场景。
 *
 * <p>可以用于直接调用或通过 JMX 进行管理操作。
 * 在后一种情况下，通常会使用 {@link org.springframework.jmx.export.MBeanExporter}
 * 定义一个 {@link org.springframework.jmx.export.assembler.InterfaceBasedMBeanInfoAssembler}，
 * 将受控制的活动组件的可见性限制为 Lifecycle 接口。
 *
 * <p>请注意，当前的 {@code Lifecycle} 接口仅在顶级单例 bean 上受支持。
 * 在任何其他组件上，{@code Lifecycle} 接口将保持未被检测到，因此被忽略。
 * 另外，请注意，扩展的 {@link SmartLifecycle} 接口与应用程序上下文的启动和关闭阶段进行了复杂的集成。
 *
 * @author Juergen Hoeller
 * @see SmartLifecycle
 * @see ConfigurableApplicationContext
 * @see org.springframework.jms.listener.AbstractMessageListenerContainer
 * @see org.springframework.scheduling.quartz.SchedulerFactoryBean
 * @since 2.0
 */
public interface Lifecycle {

	/**
	 * 启动该组件。
	 * <p>如果组件已经运行，不应该抛出异常。
	 * <p>对于容器而言，这将向所有适用的组件传播启动信号。
	 *
	 * @see SmartLifecycle#isAutoStartup()
	 */
	void start();

	/**
	 * 停止该组件，通常是同步方式，以便在该方法返回时组件完全停止。
	 * 当需要异步停止行为时，考虑实现 {@link SmartLifecycle} 及其 {@code stop(Runnable)} 变体。
	 * <p>请注意，此停止通知不能保证在销毁之前发生：
	 * 在正常关闭时，{@code Lifecycle} bean将在传播一般销毁回调之前首先接收到停止通知；
	 * 但是，在上下文的生命周期内进行的热刷新或在刷新尝试中的中止时，将在事先考虑停止信号的情况下调用给定bean的destroy方法。
	 * <p>如果组件未运行（尚未启动），不应抛出异常。
	 * <p>对于容器而言，这将向所有适用的组件传播停止信号。
	 *
	 * @see SmartLifecycle#stop(Runnable)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	void stop();

	/**
	 * 检查此组件当前是否正在运行。
	 * 对于容器而言，只有当所有适用的组件当前都在运行时，才会返回 {@code true}。
	 *
	 * @return 组件当前是否正在运行
	 */
	boolean isRunning();
}
