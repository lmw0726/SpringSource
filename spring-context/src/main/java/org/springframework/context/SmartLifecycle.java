/*
 * Copyright 2002-2019 the original author or authors.
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
 * {@link Lifecycle} 接口的扩展，适用于那些需要在 {@code ApplicationContext}
 * 刷新和/或关闭时以特定顺序启动的对象。
 *
 * <p>{@link #isAutoStartup()} 的返回值指示此对象是否应在上下文刷新时
 * 自动启动。接受回调参数的 {@link #stop(Runnable)} 方法适用于具有异步
 * 关闭过程的对象。任何此接口的实现都 <i>必须</i> 在关闭完成后调用
 * 回调的 {@code run()} 方法，以避免整体 {@code ApplicationContext} 关闭
 * 过程中不必要的延迟。
 *
 * <p>此接口扩展了 {@link Phased}，{@link #getPhase()} 方法的返回值
 * 指示此 {@code Lifecycle} 组件应在哪个阶段启动和停止。启动过程从
 * <i>最低</i> 阶段值开始，到 <i>最高</i> 阶段值结束（{@code Integer.MIN_VALUE}
 * 是可能的最低值，{@code Integer.MAX_VALUE} 是可能的最高值）。关闭过程
 * 将按相反顺序执行。具有相同值的组件在同一阶段内的顺序是任意的。
 *
 * <p>示例：如果组件 B 依赖于组件 A 已经启动，则组件 A 应具有比组件 B
 * 更低的阶段值。在关闭过程中，组件 B 将在组件 A 之前被停止。
 *
 * <p>任何显式的 "depends-on" 关系将优先于阶段顺序，使得被依赖的 bean
 * 总是在其依赖项之后启动，并且总是在其依赖项之前停止。
 *
 * <p>上下文中任何未实现 {@code SmartLifecycle} 的 {@code Lifecycle} 组件
 * 将被视为具有阶段值 {@code 0}。这允许 {@code SmartLifecycle} 组件在
 * 其阶段值为负数时，在这些 {@code Lifecycle} 组件之前启动；或者在
 * {@code SmartLifecycle} 组件的阶段值为正数时，在这些 {@code Lifecycle}
 * 组件之后启动。
 *
 * <p>请注意，由于 {@code SmartLifecycle} 的自动启动支持，{@code SmartLifecycle}
 * bean 实例通常会在应用程序上下文启动时被初始化。因此，bean 定义的
 * lazy-init 标志对 {@code SmartLifecycle} bean 的实际效果非常有限。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see LifecycleProcessor
 * @see ConfigurableApplicationContext
 */
public interface SmartLifecycle extends Lifecycle, Phased {

	/**
	 * {@code SmartLifecycle} 的默认阶段：{@code Integer.MAX_VALUE}。
	 * <p>这与普通 {@link Lifecycle} 实现关联的常见阶段 {@code 0} 不同，
	 * 将通常自动启动的 {@code SmartLifecycle} bean 放入较晚的启动阶段
	 * 和较早的关闭阶段。
	 * @since 5.1
	 * @see #getPhase()
	 * @see org.springframework.context.support.DefaultLifecycleProcessor#getPhase(Lifecycle)
	 */
	int DEFAULT_PHASE = Integer.MAX_VALUE;


	/**
	 * 如果此 {@code Lifecycle} 组件应在容器刷新包含它的 {@link ApplicationContext}
	 * 时自动启动，则返回 {@code true}。
	 * <p>值为 {@code false} 表示该组件应通过显式调用 {@link #start()} 来启动，
	 * 类似于普通的 {@link Lifecycle} 实现。
	 * <p>默认实现返回 {@code true}。
	 * @see #start()
	 * @see #getPhase()
	 * @see LifecycleProcessor#onRefresh()
	 * @see ConfigurableApplicationContext#refresh()
	 */
	default boolean isAutoStartup() {
		return true;
	}

	/**
	 * 表示如果 Lifecycle 组件当前正在运行，则必须停止。
	 * <p>提供的回调用于 {@link LifecycleProcessor}，以支持按顺序（可能并发地）
	 * 关闭所有具有相同关闭顺序值的组件。回调 <b>必须</b> 在
	 * {@code SmartLifecycle} 组件确实停止后执行。
	 * <p>{@link LifecycleProcessor} 将 <i>仅</i> 调用此变体的
	 * {@code stop} 方法；即除非在此方法的实现中显式委托，否则不会为
	 * {@code SmartLifecycle} 实现调用 {@link Lifecycle#stop()}。
	 * <p>默认实现委托给 {@link #stop()} 并立即在调用线程中触发给定的
	 * 回调。请注意，两者之间没有同步，因此自定义实现可能至少需要
	 * 将相同的步骤放在其公共生命周期监视器中（如果有的话）。
	 * @see #stop()
	 * @see #getPhase()
	 */
	default void stop(Runnable callback) {
		stop();
		callback.run();
	}

	/**
	 * 返回此生命周期对象应在其内运行的阶段。
	 * <p>默认实现返回 {@link #DEFAULT_PHASE}，以便让 {@code stop()} 回调
	 * 在普通 {@code Lifecycle} 实现之后执行。
	 * @see #isAutoStartup()
	 * @see #start()
	 * @see #stop(Runnable)
	 * @see org.springframework.context.support.DefaultLifecycleProcessor#getPhase(Lifecycle)
	 */
	@Override
	default int getPhase() {
		return DEFAULT_PHASE;
	}

}
