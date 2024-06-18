/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.client.reactive;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.netty.http.HttpResources;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 工厂类用于管理 Reactor Netty 资源，例如事件循环线程的 {@link LoopResources} 和连接池的 {@link ConnectionProvider}，
 * 在 Spring {@code ApplicationContext} 的生命周期内进行管理。
 *
 * <p>该工厂实现了 {@link InitializingBean} 和 {@link DisposableBean} 接口，
 * 通常被声明为一个由 Spring 管理的 bean。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.1
 */
public class ReactorResourceFactory implements InitializingBean, DisposableBean {
	/**
	 * 是否使用全局资源
	 */
	private boolean useGlobalResources = true;

	/**
	 * 全局资源消费者
	 */
	@Nullable
	private Consumer<HttpResources> globalResourcesConsumer;

	/**
	 * 连接提供商供应商
	 */
	private Supplier<ConnectionProvider> connectionProviderSupplier = () -> ConnectionProvider.create("webflux", 500);

	/**
	 * 连接提供者
	 */
	@Nullable
	private ConnectionProvider connectionProvider;

	/**
	 * 循环资源供应者
	 */
	private Supplier<LoopResources> loopResourcesSupplier = () -> LoopResources.create("webflux-http");

	/**
	 * 循环资源
	 */
	@Nullable
	private LoopResources loopResources;

	/**
	 * 管理连接提供者
	 */
	private boolean manageConnectionProvider = false;

	/**
	 * 是否需要管理循环资源
	 */
	private boolean manageLoopResources = false;

	/**
	 * 停机静默期
	 */
	private Duration shutdownQuietPeriod = Duration.ofSeconds(LoopResources.DEFAULT_SHUTDOWN_QUIET_PERIOD);

	/**
	 * 关机超时时间
	 */
	private Duration shutdownTimeout = Duration.ofSeconds(LoopResources.DEFAULT_SHUTDOWN_TIMEOUT);


	/**
	 * 设置是否使用全局 Reactor Netty 资源 {@link HttpResources}。
	 * <p>默认为 "true"，在这种情况下，该工厂在 Spring 的 {@code ApplicationContext} 生命周期内初始化和停止全局 Reactor Netty 资源。
	 * 如果设置为 "false"，则该工厂独立管理其资源，与全局资源无关。
	 *
	 * @param useGlobalResources 是否使用全局资源
	 * @see #addGlobalResourcesConsumer(Consumer)
	 */
	public void setUseGlobalResources(boolean useGlobalResources) {
		this.useGlobalResources = useGlobalResources;
	}

	/**
	 * 返回该工厂是否暴露全局的 {@link reactor.netty.http.HttpResources} 持有者。
	 */
	public boolean isUseGlobalResources() {
		return this.useGlobalResources;
	}

	/**
	 * 添加一个 Consumer 用于在启动时配置全局 Reactor Netty 资源。
	 * 使用此选项时，也会启用 {@link #setUseGlobalResources(boolean)}。
	 *
	 * @param consumer 要应用的 Consumer
	 * @see #setUseGlobalResources(boolean)
	 */
	public void addGlobalResourcesConsumer(Consumer<HttpResources> consumer) {
		// 设置使用全局资源标志为 true
		this.useGlobalResources = true;

		// 更新 全局资源消费者
		// 如果 全局资源消费者 不为 空，则将原先的 消费者 和新的 消费者 连接起来，形成一个链式调用
		// 否则，直接使用新的 消费者
		this.globalResourcesConsumer = (this.globalResourcesConsumer != null ?
				this.globalResourcesConsumer.andThen(consumer) : consumer);
	}

	/**
	 * 当不想参与全局资源并且想自定义托管的 {@code ConnectionProvider} 创建时使用此方法。
	 * <p>默认情况下，使用 {@code ConnectionProvider.elastic("http")}。
	 * <p>注意，如果 {@code userGlobalResources=false} 或者已设置 {@link #setConnectionProvider(ConnectionProvider)}，则此选项将被忽略。
	 *
	 * @param supplier 要使用的 Supplier
	 */
	public void setConnectionProviderSupplier(Supplier<ConnectionProvider> supplier) {
		this.connectionProviderSupplier = supplier;
	}

	/**
	 * 当您希望提供外部管理的 {@link ConnectionProvider} 实例时使用此方法。
	 *
	 * @param connectionProvider 要直接使用的连接提供者
	 */
	public void setConnectionProvider(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	/**
	 * 返回配置的 {@link ConnectionProvider}。
	 */
	public ConnectionProvider getConnectionProvider() {
		Assert.state(this.connectionProvider != null, "ConnectionProvider not initialized yet");
		return this.connectionProvider;
	}

	/**
	 * 当不想参与全局资源并且想自定义托管的 {@code LoopResources} 创建时使用此方法。
	 * <p>默认情况下，使用 {@code LoopResources.create("reactor-http")}。
	 * <p>注意，如果 {@code userGlobalResources=false} 或者已设置 {@link #setLoopResources(LoopResources)}，则此选项将被忽略。
	 *
	 * @param supplier 要使用的 Supplier
	 */
	public void setLoopResourcesSupplier(Supplier<LoopResources> supplier) {
		this.loopResourcesSupplier = supplier;
	}

	/**
	 * 当您希望提供外部管理的 {@link LoopResources} 实例时使用此选项。
	 *
	 * @param loopResources 要直接使用的 LoopResources
	 */
	public void setLoopResources(LoopResources loopResources) {
		this.loopResources = loopResources;
	}

	/**
	 * 返回配置的 {@link LoopResources}。
	 */
	public LoopResources getLoopResources() {
		Assert.state(this.loopResources != null, "LoopResources not initialized yet");
		return this.loopResources;
	}

	/**
	 * 配置在关闭资源之前等待的时间量。如果在 {@code shutdownQuietPeriod} 期间提交了任务，
	 * 则保证接受该任务，并重新启动 {@code shutdownQuietPeriod}。
	 * <p>默认情况下，此值设置为 {@link LoopResources#DEFAULT_SHUTDOWN_QUIET_PERIOD}，
	 * 即 2 秒，但也可以通过系统属性 {@link reactor.netty.ReactorNetty#SHUTDOWN_QUIET_PERIOD
	 * ReactorNetty.SHUTDOWN_QUIET_PERIOD} 进行覆盖。
	 *
	 * @param shutdownQuietPeriod 关闭静默期的持续时间
	 * @see #setShutdownTimeout(Duration)
	 * @since 5.2.4
	 */
	public void setShutdownQuietPeriod(Duration shutdownQuietPeriod) {
		Assert.notNull(shutdownQuietPeriod, "shutdownQuietPeriod should not be null");
		this.shutdownQuietPeriod = shutdownQuietPeriod;
	}

	/**
	 * 配置在放弃底层资源之前等待的最长时间量，无论在 {@code shutdownQuietPeriod} 期间是否提交了任务。
	 * <p>默认情况下，此值设置为 {@link LoopResources#DEFAULT_SHUTDOWN_TIMEOUT}，
	 * 即 15 秒，但也可以通过系统属性 {@link reactor.netty.ReactorNetty#SHUTDOWN_TIMEOUT
	 * ReactorNetty.SHUTDOWN_TIMEOUT} 进行覆盖。
	 *
	 * @param shutdownTimeout 关闭超时的持续时间
	 * @see #setShutdownQuietPeriod(Duration)
	 * @since 5.2.4
	 */
	public void setShutdownTimeout(Duration shutdownTimeout) {
		Assert.notNull(shutdownTimeout, "shutdownTimeout should not be null");
		this.shutdownTimeout = shutdownTimeout;
	}


	@Override
	public void afterPropertiesSet() {
		// 如果使用全局资源
		if (this.useGlobalResources) {
			Assert.isTrue(this.loopResources == null && this.connectionProvider == null,
					"'useGlobalResources' is mutually exclusive with explicitly configured resources");

			// 获取全局的 Http资源 实例
			HttpResources httpResources = HttpResources.get();

			// 如果设置了全局资源消费者，则接受全局资源
			if (this.globalResourcesConsumer != null) {
				this.globalResourcesConsumer.accept(httpResources);
			}

			// 将全局资源作为连接提供者
			this.connectionProvider = httpResources;
			// 设置循环资源
			this.loopResources = httpResources;
		} else {
			// 如果不使用全局资源

			// 如果循环资源未配置
			if (this.loopResources == null) {
				// 将管理循环资源标志设置为true。
				this.manageLoopResources = true;
				// 使用提供的 供应者 获取循环资源
				this.loopResources = this.loopResourcesSupplier.get();
			}

			// 如果连接提供者未配置
			if (this.connectionProvider == null) {
				// 管理连接提供者
				this.manageConnectionProvider = true;
				// 使用提供的 供应者 获取连接提供者
				this.connectionProvider = this.connectionProviderSupplier.get();
			}
		}
	}

	@Override
	public void destroy() {
		// 如果使用全局资源管理
		if (this.useGlobalResources) {
			// 延迟处理关闭循环和连接
			HttpResources.disposeLoopsAndConnectionsLater(this.shutdownQuietPeriod, this.shutdownTimeout).block();
		} else {
			// 否则，分别尝试释放连接提供者和循环资源
			try {
				ConnectionProvider provider = this.connectionProvider;
				// 如果连接提供者存在且需要管理连接提供者
				if (provider != null && this.manageConnectionProvider) {
					// 延迟释放连接提供者资源
					provider.disposeLater().block();
				}
			} catch (Throwable ex) {
				// 忽略异常
			}

			try {
				LoopResources resources = this.loopResources;
				// 如果循环资源存在且需要管理循环资源
				if (resources != null && this.manageLoopResources) {
					// 延迟释放循环资源
					resources.disposeLater(this.shutdownQuietPeriod, this.shutdownTimeout).block();
				}
			} catch (Throwable ex) {
				// 忽略异常
			}
		}
	}

}
