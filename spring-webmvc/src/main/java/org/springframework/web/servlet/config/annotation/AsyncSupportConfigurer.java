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

package org.springframework.web.servlet.config.annotation;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 帮助配置异步请求处理选项的类。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class AsyncSupportConfigurer {
	/**
	 * 异步任务执行器
	 */
	@Nullable
	private AsyncTaskExecutor taskExecutor;

	/**
	 * 超时时间，单位毫秒
	 */
	@Nullable
	private Long timeout;

	/**
	 * 回调处理拦截器列表
	 */
	private final List<CallableProcessingInterceptor> callableInterceptors = new ArrayList<>();

	/**
	 * 延迟结果处理拦截器列表
	 */
	private final List<DeferredResultProcessingInterceptor> deferredResultInterceptors = new ArrayList<>();


	/**
	 * 配置任务执行器:
	 * <ol>
	 * <li>处理 {@link Callable} 控制器方法的返回值。
	 * <li>通过响应进行阻塞写操作时使用反应式（例如 Reactor, RxJava）控制器方法的返回值。
	 * </ol>
	 * <p>默认情况下仅使用 {@link SimpleAsyncTaskExecutor}。但是当使用上述两种用例时，
	 * 建议配置一个由线程池支持的执行器，例如 {@link ThreadPoolTaskExecutor}。
	 *
	 * @param taskExecutor 要使用的任务执行器实例
	 * @return 当前的 AsyncSupportConfigurer 实例
	 */
	public AsyncSupportConfigurer setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
		return this;
	}

	/**
	 * 指定异步请求处理超时时间（以毫秒为单位）。
	 * 在 Servlet 3 中，超时从主请求处理线程退出后开始，结束于再次调度请求以进一步处理并发生成的结果时。
	 * <p>如果未设置此值，则使用底层实现的默认超时时间。
	 *
	 * @param timeout 超时时间（毫秒）
	 * @return 当前的 AsyncSupportConfigurer 实例
	 */
	public AsyncSupportConfigurer setDefaultTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * 配置生命周期拦截器，在控制器返回 {@link java.util.concurrent.Callable} 时，
	 * 对并发请求执行进行回调。
	 *
	 * @param interceptors 要注册的拦截器
	 * @return 当前的 AsyncSupportConfigurer 实例
	 */
	public AsyncSupportConfigurer registerCallableInterceptors(CallableProcessingInterceptor... interceptors) {
		this.callableInterceptors.addAll(Arrays.asList(interceptors));
		return this;
	}

	/**
	 * 配置生命周期拦截器，在控制器返回 {@link DeferredResult} 时，
	 * 对并发请求执行进行回调。
	 *
	 * @param interceptors 要注册的拦截器
	 * @return 当前的 AsyncSupportConfigurer 实例
	 */
	public AsyncSupportConfigurer registerDeferredResultInterceptors(
			DeferredResultProcessingInterceptor... interceptors) {

		this.deferredResultInterceptors.addAll(Arrays.asList(interceptors));
		return this;
	}


	@Nullable
	protected AsyncTaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	@Nullable
	protected Long getTimeout() {
		return this.timeout;
	}

	protected List<CallableProcessingInterceptor> getCallableInterceptors() {
		return this.callableInterceptors;
	}

	protected List<DeferredResultProcessingInterceptor> getDeferredResultInterceptors() {
		return this.deferredResultInterceptors;
	}

}
