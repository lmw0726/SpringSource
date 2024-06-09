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

/**
 * 协助调用{@link DeferredResultProcessingInterceptor}的类。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
class DeferredResultInterceptorChain {

	/**
	 * 日志记录器。
	 */
	private static final Log logger = LogFactory.getLog(DeferredResultInterceptorChain.class);

	/**
	 * 延迟结果处理拦截器列表。
	 */
	private final List<DeferredResultProcessingInterceptor> interceptors;

	/**
	 * 前一个处理过的拦截器索引。
	 */
	private int preProcessingIndex = -1;

	/**
	 * 构造一个DeferredResultInterceptorChain。
	 *
	 * @param interceptors DeferredResultProcessingInterceptor的拦截器列表
	 */
	public DeferredResultInterceptorChain(List<DeferredResultProcessingInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	/**
	 * 在并发处理之前应用拦截器。
	 *
	 * @param request        原生Web请求
	 * @param deferredResult 延迟结果
	 * @throws Exception 如果处理过程中出现异常
	 */
	public void applyBeforeConcurrentHandling(NativeWebRequest request, DeferredResult<?> deferredResult)
			throws Exception {

		// 遍历拦截器列表
		for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
			// 对每个拦截器调用 beforeConcurrentHandling 方法
			interceptor.beforeConcurrentHandling(request, deferredResult);
		}
	}

	/**
	 * 应用前处理。
	 *
	 * @param request        原生Web请求
	 * @param deferredResult 延迟结果
	 * @throws Exception 如果处理过程中出现异常
	 */
	public void applyPreProcess(NativeWebRequest request, DeferredResult<?> deferredResult) throws Exception {
		// 遍历拦截器列表
		for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
			// 对每个拦截器调用 preProcess 方法
			interceptor.preProcess(request, deferredResult);
			// 处理过的拦截器索引加一
			this.preProcessingIndex++;
		}

	}

	/**
	 * 应用后处理。
	 *
	 * @param request          原生Web请求
	 * @param deferredResult   延迟结果
	 * @param concurrentResult 并发结果
	 * @return 处理后的结果
	 */
	public Object applyPostProcess(NativeWebRequest request, DeferredResult<?> deferredResult,
								   Object concurrentResult) {
		try {
			// 依次调用拦截器的后处理方法
			for (int i = this.preProcessingIndex; i >= 0; i--) {
				// 对每个拦截器调用 postProcess 方法
				this.interceptors.get(i).postProcess(request, deferredResult, concurrentResult);
			}
		} catch (Throwable ex) {
			// 捕获异常并返回
			return ex;
		}
		// 返回并发结果
		return concurrentResult;
	}

	/**
	 * 触发超时后的处理。
	 *
	 * @param request        原生Web请求
	 * @param deferredResult 延迟结果
	 * @throws Exception 如果处理过程中出现异常
	 */
	public void triggerAfterTimeout(NativeWebRequest request, DeferredResult<?> deferredResult) throws Exception {
		// 遍历拦截器列表
		for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
			if (deferredResult.isSetOrExpired()) {
				// 如果 延迟结果 已经设置或者过期，则直接返回
				return;
			}
			// 调用拦截器的 handleTimeout 方法处理超时
			if (!interceptor.handleTimeout(request, deferredResult)) {
				// 如果拦截器返回 false，则跳出循环
				break;
			}
		}
	}

	/**
	 * 触发错误后的处理。
	 *
	 * @param request        原生Web请求
	 * @param deferredResult 延迟结果
	 * @param ex             异常对象
	 * @return 如果应继续处理错误，则返回true；否则返回false
	 * @throws Exception 如果处理过程中出现异常
	 */
	public boolean triggerAfterError(NativeWebRequest request, DeferredResult<?> deferredResult, Throwable ex)
			throws Exception {

		for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
			// 如果 延迟结果 已设置或已过期，则返回false
			if (deferredResult.isSetOrExpired()) {
				return false;
			}
			if (!interceptor.handleError(request, deferredResult, ex)) {
				// 调用拦截器的处理错误方法，如果返回false，则直接返回false
				return false;
			}
		}
		// 否则返回true
		return true;
	}

	/**
	 * 触发完成后的处理。
	 *
	 * @param request        原生Web请求
	 * @param deferredResult 延迟结果
	 */
	public void triggerAfterCompletion(NativeWebRequest request, DeferredResult<?> deferredResult) {
		// 当当前已处理过的拦截器开始，向前遍历拦截器
		for (int i = this.preProcessingIndex; i >= 0; i--) {
			try {
				// 调用拦截器的afterCompletion方法
				this.interceptors.get(i).afterCompletion(request, deferredResult);
			} catch (Throwable ex) {
				// 忽略拦截器的afterCompletion方法中的异常
				logger.trace("Ignoring failure in afterCompletion method", ex);
			}
		}
	}

}
