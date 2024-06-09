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

package org.springframework.web.context.request.async;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * 协助调用 {@link CallableProcessingInterceptor} 的类。
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 */
class CallableInterceptorChain {

	/**
	 * 日志记录器。
	 */
	private static final Log logger = LogFactory.getLog(CallableInterceptorChain.class);

	/**
	 * 可调用处理拦截器 拦截器列表。
	 */
	private final List<CallableProcessingInterceptor> interceptors;

	/**
	 * 当前已执行的预处理拦截器的索引。
	 */
	private int preProcessIndex = -1;

	/**
	 * 异步任务 对象。
	 */
	private volatile Future<?> taskFuture;


	public CallableInterceptorChain(List<CallableProcessingInterceptor> interceptors) {
		this.interceptors = interceptors;
	}


	public void setTaskFuture(Future<?> taskFuture) {
		this.taskFuture = taskFuture;
	}


	public void applyBeforeConcurrentHandling(NativeWebRequest request, Callable<?> task) throws Exception {
		// 遍历拦截器列表
		for (CallableProcessingInterceptor interceptor : this.interceptors) {
			// 依次调用每个拦截器的 beforeConcurrentHandling 方法
			interceptor.beforeConcurrentHandling(request, task);
		}
	}

	public void applyPreProcess(NativeWebRequest request, Callable<?> task) throws Exception {
		// 遍历拦截器列表
		for (CallableProcessingInterceptor interceptor : this.interceptors) {
			// 依次调用每个拦截器的 preProcess 方法
			interceptor.preProcess(request, task);
			// 预处理拦截器的索引 加一
			this.preProcessIndex++;
		}
	}

	public Object applyPostProcess(NativeWebRequest request, Callable<?> task, Object concurrentResult) {
		Throwable exceptionResult = null;
		// 从当前拦截器索引开始向前遍历拦截器列表
		for (int i = this.preProcessIndex; i >= 0; i--) {
			try {
				// 调用拦截器的postProcess方法
				this.interceptors.get(i).postProcess(request, task, concurrentResult);
			} catch (Throwable ex) {
				if (exceptionResult != null) {
					// 如果异常结果不为null，则记录异常日志
					if (logger.isTraceEnabled()) {
						logger.trace("Ignoring failure in postProcess method", ex);
					}
				} else {
					// 如果异常结果为null，则保存第一个异常
					exceptionResult = ex;
				}
			}
		}
		// 返回异常结果，如果为null，则返回并发结果
		return (exceptionResult != null) ? exceptionResult : concurrentResult;
	}

	public Object triggerAfterTimeout(NativeWebRequest request, Callable<?> task) {
		// 取消任务
		cancelTask();
		// 遍历拦截器列表
		for (CallableProcessingInterceptor interceptor : this.interceptors) {
			try {
				// 调用拦截器的handleTimeout方法
				Object result = interceptor.handleTimeout(request, task);
				// 如果结果为已处理响应，则中断循环
				if (result == CallableProcessingInterceptor.RESPONSE_HANDLED) {
					break;
				} else if (result != CallableProcessingInterceptor.RESULT_NONE) {
					// 如果结果不是空的结果，则直接返回结果
					return result;
				}
			} catch (Throwable ex) {
				// 如果出现异常，则直接返回异常
				return ex;
			}
		}
		// 如果没有返回结果，则返回空的结果
		return CallableProcessingInterceptor.RESULT_NONE;
	}

	private void cancelTask() {
		// 获取任务的 Future 对象
		Future<?> future = this.taskFuture;
		if (future != null) {
			// 如果 Future 对象不为空，则尝试取消任务
			try {
				future.cancel(true);
			} catch (Throwable ex) {
				// 忽略异常
			}
		}
	}

	public Object triggerAfterError(NativeWebRequest request, Callable<?> task, Throwable throwable) {
		// 取消任务
		cancelTask();
		// 遍历拦截器列表
		for (CallableProcessingInterceptor interceptor : this.interceptors) {
			try {
				// 调用拦截器的handleError方法
				Object result = interceptor.handleError(request, task, throwable);
				// 如果结果为已处理响应，则中断循环
				if (result == CallableProcessingInterceptor.RESPONSE_HANDLED) {
					break;
				} else if (result != CallableProcessingInterceptor.RESULT_NONE) {
					// 如果结果不是空的结果，则直接返回结果
					return result;
				}
			} catch (Throwable ex) {
				// 如果出现异常，则直接返回异常
				return ex;
			}
		}
		// 如果没有返回结果，则返回空的结果
		return CallableProcessingInterceptor.RESULT_NONE;
	}

	public void triggerAfterCompletion(NativeWebRequest request, Callable<?> task) {
		// 从拦截器列表末尾开始循环
		for (int i = this.interceptors.size() - 1; i >= 0; i--) {
			try {
				// 调用每个拦截器的 afterCompletion 方法
				this.interceptors.get(i).afterCompletion(request, task);
			} catch (Throwable ex) {
				// 如果发生异常，则记录日志并继续循环
				if (logger.isTraceEnabled()) {
					logger.trace("Ignoring failure in afterCompletion method", ex);
				}
			}
		}
	}

}
