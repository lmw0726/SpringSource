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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * {@code DeferredResult}提供了一种替代{@link Callable}用于异步请求处理的方法。虽然{@code Callable}
 * 是代表应用程序并行执行的，但使用{@code DeferredResult}，应用程序可以从其选择的线程生成结果。
 *
 * <p>子类可以扩展此类以轻松地将其他数据或行为与{@link DeferredResult}关联起来。例如，可以扩展该类并添加一个额外的属性来关联创建{@link DeferredResult}的用户。
 * 通过这种方式，稍后可以轻松访问用户，而无需使用数据结构进行映射。
 *
 * <p>将附加行为关联到此类的示例可能通过扩展类来实现一个附加接口。例如，可能想要实现{@link Comparable}，
 * 以便将{@link DeferredResult}添加到{@link PriorityQueue}中时以正确的顺序处理它。
 *
 * @param <T> 结果类型
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Rob Winch
 * @since 3.2
 */
public class DeferredResult<T> {

	/**
	 * 空的结果值
	 */
	private static final Object RESULT_NONE = new Object();

	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(DeferredResult.class);

	/**
	 * 超时值，以毫秒为单位
	 */
	@Nullable
	private final Long timeoutValue;

	/**
	 * 超时时使用的结果提供者。
	 */
	private final Supplier<?> timeoutResult;

	/**
	 * 处理超时的回调。
	 */
	private Runnable timeoutCallback;

	/**
	 * 处理错误的回调。
	 */
	private Consumer<Throwable> errorCallback;

	/**
	 * 处理完成的回调。
	 */
	private Runnable completionCallback;

	/**
	 * 延迟结果处理器。
	 */
	private DeferredResultHandler resultHandler;

	/**
	 * 结果值。
	 */
	private volatile Object result = RESULT_NONE;

	/**
	 * 表示延迟结果是否已过期。
	 */
	private volatile boolean expired;


	/**
	 * 创建一个DeferredResult。
	 */
	public DeferredResult() {
		this(null, () -> RESULT_NONE);
	}

	/**
	 * 创建一个具有自定义超时值的DeferredResult。
	 * <p>默认情况下未设置，在这种情况下，默认配置在 MVC Java Config 或 MVC 命名空间中使用，
	 * 或者如果未设置，则取决于基础服务器的默认值。
	 *
	 * @param timeoutValue 超时值（毫秒）
	 */
	public DeferredResult(Long timeoutValue) {
		this(timeoutValue, () -> RESULT_NONE);
	}

	/**
	 * 创建一个带有超时值和默认结果的DeferredResult，以防超时。
	 *
	 * @param timeoutValue  超时值（毫秒）（如果为 {@code null} 则忽略）
	 * @param timeoutResult 要使用的结果
	 */
	public DeferredResult(@Nullable Long timeoutValue, Object timeoutResult) {
		this.timeoutValue = timeoutValue;
		this.timeoutResult = () -> timeoutResult;
	}

	/**
	 * {@link #DeferredResult(Long, Object)} 的变体，接受基于 {@link Supplier} 的动态回退值。
	 *
	 * @param timeoutValue  超时值（毫秒）（如果为 {@code null} 则忽略）
	 * @param timeoutResult 要使用的结果供应商
	 * @since 5.1.1
	 */
	public DeferredResult(@Nullable Long timeoutValue, Supplier<?> timeoutResult) {
		this.timeoutValue = timeoutValue;
		this.timeoutResult = timeoutResult;
	}


	/**
	 * 如果此DeferredResult不再可用，则返回true，要么是因为它以前已经设置，要么是因为底层请求已过期。
	 * <p>结果可能已经通过调用 {@link #setResult(Object)}、{@link #setErrorResult(Object)} 或作为超时的结果提供给构造函数而设置。
	 * 请求也可能由于超时或网络错误而过期。
	 */
	public final boolean isSetOrExpired() {
		return (this.result != RESULT_NONE || this.expired);
	}

	/**
	 * 如果 DeferredResult 已设置，则返回true。
	 *
	 * @since 4.0
	 */
	public boolean hasResult() {
		return (this.result != RESULT_NONE);
	}

	/**
	 * 返回结果，如果结果未设置，则返回null。由于结果也可能为null，
	 * 建议先使用{@link #hasResult()}检查是否存在结果，然后再调用此方法。
	 *
	 * @since 4.0
	 */
	@Nullable
	public Object getResult() {
		Object resultToCheck = this.result;
		// 如果结果值为空的结果值，则返回null。否则返回当前结果值
		return (resultToCheck != RESULT_NONE ? resultToCheck : null);
	}

	/**
	 * 返回配置的超时值（以毫秒为单位）。
	 */
	@Nullable
	final Long getTimeoutValue() {
		return this.timeoutValue;
	}

	/**
	 * 注册异步请求超时时要调用的代码。
	 * <p>当异步请求在{@code DeferredResult}填充之前超时时，从容器线程调用此方法。
	 * 它可以调用{@link DeferredResult#setResult setResult}或{@link DeferredResult#setErrorResult setErrorResult}来恢复处理。
	 */
	public void onTimeout(Runnable callback) {
		this.timeoutCallback = callback;
	}

	/**
	 * 注册在异步请求期间发生错误时要调用的代码。
	 * <p>当在填充{@code DeferredResult}之前异步处理请求时发生错误时，从容器线程调用此方法。
	 * 它可以调用{@link DeferredResult#setResult setResult}或{@link DeferredResult#setErrorResult setErrorResult}来恢复处理。
	 *
	 * @since 5.0
	 */
	public void onError(Consumer<Throwable> callback) {
		this.errorCallback = callback;
	}

	/**
	 * 注册在异步请求完成时要调用的代码。
	 * <p>当异步请求由于任何原因完成，包括超时和网络错误时，从容器线程调用此方法。
	 * 这对于检测到{@code DeferredResult}实例不再可用是有用的。
	 */
	public void onCompletion(Runnable callback) {
		this.completionCallback = callback;
	}

	/**
	 * 提供一个处理结果值的处理器。
	 *
	 * @param resultHandler 处理器
	 * @see DeferredResultProcessingInterceptor
	 */
	public final void setResultHandler(DeferredResultHandler resultHandler) {
		Assert.notNull(resultHandler, "DeferredResultHandler is required");
		// 在结果锁之外进行立即过期检查
		if (this.expired) {
			return;
		}
		Object resultToHandle;
		synchronized (this) {
			// 在此期间获得了锁：再次检查过期状态
			if (this.expired) {
				// 如果延迟结果过期，直接返回。
				return;
			}
			resultToHandle = this.result;
			if (resultToHandle == RESULT_NONE) {
				// 还没有结果：存储处理器，以便在结果可用时处理
				this.resultHandler = resultHandler;
				return;
			}
		}
		// 如果到达此处，我们需要立即处理现有的结果对象。
		// 决定在结果锁内；只在锁之外进行处理调用，避免与 Servlet 容器锁的任何死锁潜在风险。
		try {
			resultHandler.handleResult(resultToHandle);
		} catch (Throwable ex) {
			logger.debug("Failed to process async result", ex);
		}
	}

	/**
	 * 设置 DeferredResult 的值并处理它。
	 *
	 * @param result 要设置的值
	 * @return 如果结果已设置并传递给处理；如果结果已经设置或异步请求已过期，则为false
	 * @see #isSetOrExpired()
	 */
	public boolean setResult(T result) {
		return setResultInternal(result);
	}

	private boolean setResultInternal(Object result) {
		// 在结果锁之外进行立即过期检查
		if (isSetOrExpired()) {
			// 如果设置了结果或者是过期了，返回false。
			return false;
		}
		DeferredResultHandler resultHandlerToUse;
		synchronized (this) {
			// 在此期间获得了锁：再次检查过期状态
			if (isSetOrExpired()) {
				// 如果设置了结果或者是过期了，返回false。
				return false;
			}
			// 在此时，我们获得了一个要处理的新结果
			this.result = result;
			resultHandlerToUse = this.resultHandler;
			if (resultHandlerToUse == null) {
				// 尚未设置结果处理器 -> 让 setResultHandler 实现选择结果对象并调用其结果处理器。
				return true;
			}
			// 结果处理器可用 -> 让我们清除存储的引用，因为我们不再需要它。
			this.resultHandler = null;
		}
		// 如果到达此处，我们需要立即处理现有的结果对象。
		// 决定在结果锁内；只在锁之外进行处理调用，避免与 Servlet 容器锁的任何死锁潜在风险。
		resultHandlerToUse.handleResult(result);
		return true;
	}

	/**
	 * 设置 延迟结果 的错误值并处理它。
	 * 值可能是 {@link Exception} 或 {@link Throwable}，在这种情况下，它将被处理为处理程序引发的异常。
	 *
	 * @param result 错误结果值
	 * @return 如果结果已设置为错误值并传递给处理；如果结果已经设置或异步请求已过期，则为 false
	 * @see #isSetOrExpired()
	 */
	public boolean setErrorResult(Object result) {
		return setResultInternal(result);
	}


	final DeferredResultProcessingInterceptor getInterceptor() {
		return new DeferredResultProcessingInterceptor() {
			@Override
			public <S> boolean handleTimeout(NativeWebRequest request, DeferredResult<S> deferredResult) {
				// 设置是否继续处理的标志
				boolean continueProcessing = true;
				try {
					// 如果超时回调不为空，则运行超时回调
					if (timeoutCallback != null) {
						timeoutCallback.run();
					}
				} finally {
					// 获取超时结果
					Object value = timeoutResult.get();
					// 如果超时结果不是空的结果对象
					if (value != RESULT_NONE) {
						continueProcessing = false;
						try {
							// 设置结果内部值
							setResultInternal(value);
						} catch (Throwable ex) {
							// 处理超时结果失败
							logger.debug("Failed to handle timeout result", ex);
						}
					}
				}
				// 返回是否继续处理的标志
				return continueProcessing;
			}

			@Override
			public <S> boolean handleError(NativeWebRequest request, DeferredResult<S> deferredResult, Throwable t) {
				try {
					// 如果存在错误回调函数，则调用该函数处理异常
					if (errorCallback != null) {
						errorCallback.accept(t);
					}
				} finally {
					try {
						// 设置结果为异常对象
						setResultInternal(t);
					} catch (Throwable ex) {
						// 记录调试信息，表示处理错误结果时发生失败
						logger.debug("Failed to handle error result", ex);
					}
				}
				// 返回 false，表示任务执行失败
				return false;
			}

			@Override
			public <S> void afterCompletion(NativeWebRequest request, DeferredResult<S> deferredResult) {
				// 将任务设置为已过期
				expired = true;
				if (completionCallback != null) {
					// 如果存在完成回调函数，则运行该函数
					completionCallback.run();
				}
			}
		};
	}


	/**
	 * 当设置 延迟结果 值时处理的函数接口。
	 */
	@FunctionalInterface
	public interface DeferredResultHandler {


		void handleResult(Object result);
	}

}
