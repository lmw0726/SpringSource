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

package org.springframework.web.context.request.async;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.async.DeferredResult.DeferredResultHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * 管理异步请求处理的中心类，主要用作 SPI，一般不直接由应用程序类使用。
 *
 * <p>异步场景从常规的线程（T1）中的请求处理开始。可以通过调用
 * {@link #startCallableProcessing(Callable, Object...) startCallableProcessing} 或
 * {@link #startDeferredResultProcessing(DeferredResult, Object...) startDeferredResultProcessing}
 * 来启动并发请求处理，这两者都在单独的线程（T2）中生成结果。结果被保存，并将请求调度到容器，
 * 以便在第三个线程（T3）中使用保存的结果恢复处理。在调度的线程（T3）中，可以通过
 * {@link #getConcurrentResult()} 访问保存的结果，或通过 {@link #hasConcurrentResult()} 检测其存在。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see org.springframework.web.context.request.AsyncWebRequestInterceptor
 * @see org.springframework.web.servlet.AsyncHandlerInterceptor
 * @see org.springframework.web.filter.OncePerRequestFilter#shouldNotFilterAsyncDispatch
 * @see org.springframework.web.filter.OncePerRequestFilter#isAsyncDispatch
 * @since 3.2
 */
public final class WebAsyncManager {

	/**
	 * 表示没有结果的常量对象。
	 */
	private static final Object RESULT_NONE = new Object();

	/**
	 * 默认的任务执行器，使用类名创建一个简单的异步任务执行器。
	 */
	private static final AsyncTaskExecutor DEFAULT_TASK_EXECUTOR =
			new SimpleAsyncTaskExecutor(WebAsyncManager.class.getSimpleName());

	/**
	 * 用于记录日志的实例。
	 */
	private static final Log logger = LogFactory.getLog(WebAsyncManager.class);

	/**
	 * 用于超时的 Callable 拦截器。
	 */
	private static final CallableProcessingInterceptor timeoutCallableInterceptor =
			new TimeoutCallableProcessingInterceptor();

	/**
	 * 用于超时的 延迟结果 拦截器。
	 */
	private static final DeferredResultProcessingInterceptor timeoutDeferredResultInterceptor =
			new TimeoutDeferredResultProcessingInterceptor();

	/**
	 * 任务执行器的警告标志。
	 */
	private static Boolean taskExecutorWarning = true;

	/**
	 * 异步 Web 请求。
	 */
	private AsyncWebRequest asyncWebRequest;

	/**
	 * 任务执行器。
	 */
	private AsyncTaskExecutor taskExecutor = DEFAULT_TASK_EXECUTOR;

	/**
	 * 并发结果的值。
	 */
	private volatile Object concurrentResult = RESULT_NONE;

	/**
	 * 并发结果的上下文。
	 */
	private volatile Object[] concurrentResultContext;

	/*
	 * 并发结果是否是错误。如果此类错误未处理，一些 Servlet 容器将在结束时调用 AsyncListener#onError，
	 * 在 ASYNC 和/或 ERROR 调度之后（Boot 的情况），我们需要忽略这些。
	 */
	private volatile boolean errorHandlingInProgress;

	/**
	 * 回调拦截器
	 */
	private final Map<Object, CallableProcessingInterceptor> callableInterceptors = new LinkedHashMap<>();

	/**
	 * 延迟结果拦截器
	 */
	private final Map<Object, DeferredResultProcessingInterceptor> deferredResultInterceptors = new LinkedHashMap<>();


	/**
	 * 包私有构造函数。
	 *
	 * @see WebAsyncUtils#getAsyncManager(javax.servlet.ServletRequest)
	 * @see WebAsyncUtils#getAsyncManager(org.springframework.web.context.request.WebRequest)
	 */
	WebAsyncManager() {
	}


	/**
	 * 配置要使用的 {@link AsyncWebRequest}。在单个请求期间可以多次设置此属性，
	 * 以准确反映请求的当前状态（例如，在转发、请求/响应包装等之后）。
	 * 但是，在并发处理进行中时，即 {@link #isConcurrentHandlingStarted()} 为 {@code true} 时，
	 * 不应设置此属性。
	 *
	 * @param asyncWebRequest 要使用的异步 Web 请求
	 */
	public void setAsyncWebRequest(AsyncWebRequest asyncWebRequest) {
		Assert.notNull(asyncWebRequest, "AsyncWebRequest must not be null");
		// 设置异步网络请求
		this.asyncWebRequest = asyncWebRequest;
		// 添加完成处理程序，用于在异步请求完成后移除 Web异步管理器 属性
		this.asyncWebRequest.addCompletionHandler(() -> asyncWebRequest.removeAttribute(
				WebAsyncUtils.WEB_ASYNC_MANAGER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST));
	}

	/**
	 * 配置一个 AsyncTaskExecutor 以用于通过 {@link #startCallableProcessing(Callable, Object...)} 进行的并发处理。
	 * <p>默认情况下，使用 {@link SimpleAsyncTaskExecutor} 实例。
	 */
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 当前请求的选定处理程序是否选择以异步方式处理请求。返回值为 "true" 表示正在进行并发处理，
	 * 响应将保持打开状态。返回值为 "false" 表示并发处理要么没有启动，要么可能已经完成，
	 * 并且请求已被调度以进一步处理并发结果。
	 */
	public boolean isConcurrentHandlingStarted() {
		return (this.asyncWebRequest != null && this.asyncWebRequest.isAsyncStarted());
	}

	/**
	 * 是否存在作为并发处理结果的结果值。
	 */
	public boolean hasConcurrentResult() {
		return (this.concurrentResult != RESULT_NONE);
	}

	/**
	 * 提供对并发处理结果的访问。
	 *
	 * @return 一个对象，如果并发处理引发了一个 {@code Exception} 或 {@code Throwable}，则可能是一个 {@code Exception} 或 {@code Throwable}。
	 * @see #clearConcurrentResult()
	 */
	public Object getConcurrentResult() {
		return this.concurrentResult;
	}

	/**
	 * 提供对在并发处理开始时保存的附加处理上下文的访问。
	 *
	 * @see #clearConcurrentResult()
	 */
	public Object[] getConcurrentResultContext() {
		return this.concurrentResultContext;
	}

	/**
	 * 获取在给定键下注册的 {@link CallableProcessingInterceptor}。
	 *
	 * @param key 键
	 * @return 注册在该键下的拦截器，如果没有则返回 {@code null}
	 */
	@Nullable
	public CallableProcessingInterceptor getCallableInterceptor(Object key) {
		return this.callableInterceptors.get(key);
	}

	/**
	 * 获取在给定键下注册的 {@link DeferredResultProcessingInterceptor}。
	 *
	 * @param key 键
	 * @return 注册在该键下的拦截器，如果没有则返回 {@code null}
	 */
	@Nullable
	public DeferredResultProcessingInterceptor getDeferredResultInterceptor(Object key) {
		return this.deferredResultInterceptors.get(key);
	}

	/**
	 * 在给定的键下注册一个 {@link CallableProcessingInterceptor}。
	 *
	 * @param key         键
	 * @param interceptor 要注册的拦截器
	 */
	public void registerCallableInterceptor(Object key, CallableProcessingInterceptor interceptor) {
		Assert.notNull(key, "Key is required");
		Assert.notNull(interceptor, "CallableProcessingInterceptor is required");
		this.callableInterceptors.put(key, interceptor);
	}

	/**
	 * 注册一个没有键的 {@link CallableProcessingInterceptor}。
	 * 键是由类名和哈希码派生出来的。
	 *
	 * @param interceptors 一个或多个要注册的拦截器
	 */
	public void registerCallableInterceptors(CallableProcessingInterceptor... interceptors) {
		Assert.notNull(interceptors, "A CallableProcessingInterceptor is required");
		// 遍历可调用处理拦截器列表
		for (CallableProcessingInterceptor interceptor : interceptors) {
			// 获取拦截器的唯一标识符
			String key = interceptor.getClass().getName() + ":" + interceptor.hashCode();
			// 将拦截器添加到可调用拦截器映射中
			this.callableInterceptors.put(key, interceptor);
		}
	}

	/**
	 * 在给定的键下注册一个 {@link DeferredResultProcessingInterceptor}。
	 *
	 * @param key         键
	 * @param interceptor 要注册的拦截器
	 */
	public void registerDeferredResultInterceptor(Object key, DeferredResultProcessingInterceptor interceptor) {
		Assert.notNull(key, "Key is required");
		Assert.notNull(interceptor, "DeferredResultProcessingInterceptor is required");
		this.deferredResultInterceptors.put(key, interceptor);
	}

	/**
	 * 注册一个或多个没有指定键的 {@link DeferredResultProcessingInterceptor}。
	 * 默认键是由拦截器类名和哈希码派生出来的。
	 *
	 * @param interceptors 一个或多个要注册的拦截器
	 */
	public void registerDeferredResultInterceptors(DeferredResultProcessingInterceptor... interceptors) {
		Assert.notNull(interceptors, "A DeferredResultProcessingInterceptor is required");
		// 遍历延迟结果处理拦截器列表
		for (DeferredResultProcessingInterceptor interceptor : interceptors) {
			// 获取拦截器的唯一标识符
			String key = interceptor.getClass().getName() + ":" + interceptor.hashCode();
			// 将拦截器添加到延迟结果拦截器映射中
			this.deferredResultInterceptors.put(key, interceptor);
		}
	}

	/**
	 * 清除 {@linkplain #getConcurrentResult() 并发结果} 和
	 * {@linkplain #getConcurrentResultContext() 并发结果上下文}。
	 */
	public void clearConcurrentResult() {
		// 使用 Web异步管理器 对象作为同步锁
		synchronized (WebAsyncManager.this) {
			// 将并发结果设置为 空的结果
			this.concurrentResult = RESULT_NONE;
			// 清空并发结果上下文
			this.concurrentResultContext = null;
		}
	}

	/**
	 * 开始并发请求处理，并使用 {@link #setTaskExecutor(AsyncTaskExecutor) AsyncTaskExecutor} 执行给定的任务。
	 * 任务执行的结果会被保存，请求会被调度以便恢复结果的处理。如果任务抛出异常，那么保存的结果将是抛出的异常。
	 *
	 * @param callable          一个异步执行的工作单元
	 * @param processingContext 可以访问的附加上下文，通过 {@link #getConcurrentResultContext()} 访问
	 * @throws Exception 如果并发处理启动失败
	 * @see #getConcurrentResult()
	 * @see #getConcurrentResultContext()
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void startCallableProcessing(Callable<?> callable, Object... processingContext) throws Exception {
		Assert.notNull(callable, "Callable must not be null");
		// 开始回调处理
		startCallableProcessing(new WebAsyncTask(callable), processingContext);
	}

	/**
	 * 使用给定的 {@link WebAsyncTask} 配置任务执行器以及 {@code AsyncWebRequest} 的超时值，
	 * 然后委托给 {@link #startCallableProcessing(Callable, Object...)}。
	 *
	 * @param webAsyncTask      包含目标 {@code Callable} 的 WebAsyncTask
	 * @param processingContext 要保存的附加上下文，可以通过 {@link #getConcurrentResultContext()} 访问
	 * @throws Exception 如果并发处理启动失败
	 */
	public void startCallableProcessing(final WebAsyncTask<?> webAsyncTask, Object... processingContext)
			throws Exception {

		Assert.notNull(webAsyncTask, "WebAsyncTask must not be null");
		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");

		// 获取超时时间
		Long timeout = webAsyncTask.getTimeout();
		if (timeout != null) {
			// 如果超时时间不为空，设置异步Web请求的超时时间
			this.asyncWebRequest.setTimeout(timeout);
		}

		// 获取任务执行器
		AsyncTaskExecutor executor = webAsyncTask.getExecutor();
		if (executor != null) {
			// 如果任务执行器不为空，则设置为当前类的任务执行器
			this.taskExecutor = executor;
		} else {
			// 记录执行器警告
			logExecutorWarning();
		}

		// 构建拦截器链
		List<CallableProcessingInterceptor> interceptors = new ArrayList<>();
		// 添Web异步任务中的加拦截器
		interceptors.add(webAsyncTask.getInterceptor());
		// 添加所有回调拦截器
		interceptors.addAll(this.callableInterceptors.values());
		// 添加超时调用拦截器
		interceptors.add(timeoutCallableInterceptor);

		// 获取可调用对象和拦截器链
		final Callable<?> callable = webAsyncTask.getCallable();
		// 构建拦截器链
		final CallableInterceptorChain interceptorChain = new CallableInterceptorChain(interceptors);

		// 添加超时处理程序
		this.asyncWebRequest.addTimeoutHandler(() -> {
			// 如果日志级别为调试模式
			if (logger.isDebugEnabled()) {
				// 记录异步请求超时的调试信息，包括请求 URI
				logger.debug("Async request timeout for " + formatRequestUri());
			}

			// 触发超时后的拦截器链，并获取结果
			Object result = interceptorChain.triggerAfterTimeout(this.asyncWebRequest, callable);

			if (result != CallableProcessingInterceptor.RESULT_NONE) {
				// 如果结果不为空，则设置并发结果并进行分发
				setConcurrentResultAndDispatch(result);
			}
		});

		// 添加错误处理程序
		this.asyncWebRequest.addErrorHandler(ex -> {
			// 如果不是正在处理错误
			if (!this.errorHandlingInProgress) {
				// 如果日志级别为调试模式
				if (logger.isDebugEnabled()) {
					// 记录异步请求的错误信息，包括请求 URI 和异常信息
					logger.debug("Async request error for " + formatRequestUri() + ": " + ex);
				}

				// 触发错误后的拦截器链，并获取结果
				Object result = interceptorChain.triggerAfterError(this.asyncWebRequest, callable, ex);

				// 如果结果不为空，则设置并发结果并进行分发；否则将异常作为结果进行分发
				result = (result != CallableProcessingInterceptor.RESULT_NONE ? result : ex);
				// 设置并发结果并分发结果
				setConcurrentResultAndDispatch(result);
			}
		});

		// 添加完成处理程序
		this.asyncWebRequest.addCompletionHandler(() ->
				interceptorChain.triggerAfterCompletion(this.asyncWebRequest, callable));

		// 在并发处理之前应用拦截器链
		interceptorChain.applyBeforeConcurrentHandling(this.asyncWebRequest, callable);
		// 开始异步处理
		startAsyncProcessing(processingContext);

		try {
			// 提交任务到执行器
			Future<?> future = this.taskExecutor.submit(() -> {
				Object result = null;
				try {
					// 在调用前应用前处理拦截器链
					interceptorChain.applyPreProcess(this.asyncWebRequest, callable);
					// 调用可调用对象
					result = callable.call();
				} catch (Throwable ex) {
					// 处理异常
					result = ex;
				} finally {
					// 在调用后应用后处理拦截器链
					result = interceptorChain.applyPostProcess(this.asyncWebRequest, callable, result);
				}
				// 设置并发结果并调度
				setConcurrentResultAndDispatch(result);
			});
			// 设置任务的Future对象到拦截器链
			interceptorChain.setTaskFuture(future);
		} catch (RejectedExecutionException ex) {
			// 处理拒绝执行异常
			Object result = interceptorChain.applyPostProcess(this.asyncWebRequest, callable, ex);
			// 设置并发结果并分发结果
			setConcurrentResultAndDispatch(result);
			throw ex;
		}
	}

	private void logExecutorWarning() {
		// 如果 任务执行器警告为true 并且 日志记录器 的警告级别已启用
		if (taskExecutorWarning && logger.isWarnEnabled()) {
			// 同步锁定默认任务执行器
			synchronized (DEFAULT_TASK_EXECUTOR) {
				// 获取当前任务执行器
				AsyncTaskExecutor executor = this.taskExecutor;

				// 如果 任务执行器警告为true， 并且任务执行器是 简单的异步任务执行器 或 同步任务执行程序 的实例
				if (taskExecutorWarning &&
						(executor instanceof SimpleAsyncTaskExecutor || executor instanceof SyncTaskExecutor)) {

					// 获取执行器的简单类名
					String executorTypeName = executor.getClass().getSimpleName();

					// 记录警告信息，提示需要配置一个合适的任务执行器
					logger.warn("\n!!!\n" +
							"An Executor is required to handle java.util.concurrent.Callable return values.\n" +
							"Please, configure a TaskExecutor in the MVC config under \"async support\".\n" +
							"The " + executorTypeName + " currently in use is not suitable under load.\n" +
							"-------------------------------\n" +
							"Request URI: '" + formatRequestUri() + "'\n" +
							"!!!");

					// 将 任务执行器警告设置为false
					taskExecutorWarning = false;
				}
			}
		}
	}

	private String formatRequestUri() {
		// 从异步 Web 请求中获取原生的 HttpServlet请求 对象
		HttpServletRequest request = this.asyncWebRequest.getNativeRequest(HttpServletRequest.class);

		// 如果请求对象不为空，则返回请求的 URI，否则返回 "servlet container"
		return request != null ? request.getRequestURI() : "servlet container";
	}

	private void setConcurrentResultAndDispatch(Object result) {
		// 同步块，确保线程安全
		synchronized (WebAsyncManager.this) {
			// 如果并发结果不是空的结果，则返回
			if (this.concurrentResult != RESULT_NONE) {
				return;
			}
			// 设置并发结果
			this.concurrentResult = result;
			// 检查结果是否是异常类型
			this.errorHandlingInProgress = (result instanceof Throwable);
		}

		// 如果异步Web请求已经完成
		if (this.asyncWebRequest.isAsyncComplete()) {
			// 如果日志级别是调试，记录调试信息
			if (logger.isDebugEnabled()) {
				logger.debug("Async result set but request already complete: " + formatRequestUri());
			}
			return;
		}

		// 如果日志级别是调试，记录调试信息
		if (logger.isDebugEnabled()) {
			boolean isError = result instanceof Throwable;
			logger.debug("Async " + (isError ? "error" : "result set") + ", dispatch to " + formatRequestUri());
		}
		// 调度异步Web请求
		this.asyncWebRequest.dispatch();
	}

	/**
	 * 开始并发请求处理，并使用一个 {@link DeferredResultHandler} 初始化给定的 {@link DeferredResult}，
	 * 该处理程序保存结果并调度请求以继续处理该结果。 {@code AsyncWebRequest} 还会更新一个完成处理程序，
	 * 该处理程序会使 {@code DeferredResult} 过期，并假设 {@code DeferredResult} 有一个默认的超时结果，
	 * 还会更新一个超时处理程序。
	 *
	 * @param deferredResult    要初始化的 DeferredResult 实例
	 * @param processingContext 要保存的附加上下文，可以通过 {@link #getConcurrentResultContext()} 访问
	 * @throws Exception 如果并发处理启动失败
	 * @see #getConcurrentResult()
	 * @see #getConcurrentResultContext()
	 */
	public void startDeferredResultProcessing(
			final DeferredResult<?> deferredResult, Object... processingContext) throws Exception {

		Assert.notNull(deferredResult, "DeferredResult must not be null");
		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");

		// 获取 延迟结果 的超时值
		Long timeout = deferredResult.getTimeoutValue();
		// 如果超时值不为空，设置异步Web请求的超时值
		if (timeout != null) {
			this.asyncWebRequest.setTimeout(timeout);
		}

		// 创建拦截器列表
		List<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
		// 添加 延迟结果 的拦截器
		interceptors.add(deferredResult.getInterceptor());
		// 添加所有的 延迟结果拦截器
		interceptors.addAll(this.deferredResultInterceptors.values());
		// 添加超时拦截器
		interceptors.add(timeoutDeferredResultInterceptor);

		// 创建拦截器链
		final DeferredResultInterceptorChain interceptorChain = new DeferredResultInterceptorChain(interceptors);

		// 添加超时处理器
		this.asyncWebRequest.addTimeoutHandler(() -> {
			try {
				// 触发拦截器链的超时处理
				interceptorChain.triggerAfterTimeout(this.asyncWebRequest, deferredResult);
			} catch (Throwable ex) {
				// 设置并分派并发结果
				setConcurrentResultAndDispatch(ex);
			}
		});

		// 添加错误处理器
		this.asyncWebRequest.addErrorHandler(ex -> {
			if (!this.errorHandlingInProgress) {
				// 如果不是正在处理错误
				try {
					if (!interceptorChain.triggerAfterError(this.asyncWebRequest, deferredResult, ex)) {
						// 如果触发拦截器链的错误处理返回false，则直接结束
						return;
					}
					// 设置错误结果
					deferredResult.setErrorResult(ex);
				} catch (Throwable interceptorEx) {
					// 设置并分派并发结果
					setConcurrentResultAndDispatch(interceptorEx);
				}
			}
		});

		// 添加完成处理器
		this.asyncWebRequest.addCompletionHandler(()
				-> interceptorChain.triggerAfterCompletion(this.asyncWebRequest, deferredResult));

		// 应用并发处理前的拦截器
		interceptorChain.applyBeforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		// 开始异步处理
		startAsyncProcessing(processingContext);

		try {
			// 应用预处理拦截器
			interceptorChain.applyPreProcess(this.asyncWebRequest, deferredResult);
			// 设置结果处理器
			deferredResult.setResultHandler(result -> {
				// 应用后处理拦截器
				result = interceptorChain.applyPostProcess(this.asyncWebRequest, deferredResult, result);
				// 设置并分派并发结果
				setConcurrentResultAndDispatch(result);
			});
		} catch (Throwable ex) {
			// 设置并分派并发结果
			setConcurrentResultAndDispatch(ex);
		}
	}

	private void startAsyncProcessing(Object[] processingContext) {
		// 同步块，确保多线程环境下的安全
		synchronized (WebAsyncManager.this) {
			// 设置并发结果为空的结果
			this.concurrentResult = RESULT_NONE;
			// 设置并发结果上下文
			this.concurrentResultContext = processingContext;
			// 标记错误处理未进行中
			this.errorHandlingInProgress = false;
		}
		// 开始异步请求
		this.asyncWebRequest.startAsync();

		// 如果日志级别为调试模式，记录调试信息
		if (logger.isDebugEnabled()) {
			logger.debug("Started async request");
		}
	}

}
