/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.web.context.request.NativeWebRequest;

/**
 * 拦截并发请求处理，其中并发结果通过等待应用程序选择的线程（例如响应某些外部事件）设置的
 * {@link DeferredResult} 获得。
 *
 * <p>在开始异步处理之前、在 {@code DeferredResult} 设置之后以及在超时/错误后或
 * 因任何原因（包括超时或网络错误）完成后，都会调用 {@code DeferredResultProcessingInterceptor}。
 *
 * <p>通常，拦截器方法引发的异常将导致通过调度回容器并使用异常实例作为并发结果来恢复异步处理。
 * 这些异常将通过 {@code HandlerExceptionResolver} 机制进行处理。
 *
 * <p>{@link #handleTimeout(NativeWebRequest, DeferredResult) handleTimeout}
 * 方法可以设置 {@code DeferredResult} 以恢复处理。
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 */
public interface DeferredResultProcessingInterceptor {

	/**
	 * 在并发处理开始之前立即调用，与启动它的线程相同。此方法可用于在
	 * 使用给定的 {@code DeferredResult} 开始并发处理之前捕获状态。
	 *
	 * @param request        当前请求
	 * @param deferredResult 当前请求的 DeferredResult
	 * @throws Exception 如果出现错误
	 */
	default <T> void beforeConcurrentHandling(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {
	}

	/**
	 * 在并发处理开始后立即调用，与启动它的线程相同。此方法可用于检测
	 * 使用给定的 {@code DeferredResult} 开始并发处理。
	 * <p>{@code DeferredResult} 可能已经被设置，例如在创建时或由其他线程设置。
	 *
	 * @param request        当前请求
	 * @param deferredResult 当前请求的 DeferredResult
	 * @throws Exception 如果出现错误
	 */
	default <T> void preProcess(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {
	}

	/**
	 * 在 {@code DeferredResult} 通过 {@link DeferredResult#setResult(Object)}
	 * 或 {@link DeferredResult#setErrorResult(Object)} 设置后调用，并且也已准备好
	 * 处理并发结果。
	 * <p>在超时后，如果 {@code DeferredResult} 是使用接受默认超时结果的构造函数创建的，
	 * 也可能会调用此方法。
	 *
	 * @param request          当前请求
	 * @param deferredResult   当前请求的 DeferredResult
	 * @param concurrentResult {@code DeferredResult} 的结果
	 * @throws Exception 如果出现错误
	 */
	default <T> void postProcess(NativeWebRequest request, DeferredResult<T> deferredResult,
								 Object concurrentResult) throws Exception {
	}

	/**
	 * 当异步请求在 {@code DeferredResult} 设置之前超时时，从容器线程调用。
	 * 实现可以调用 {@link DeferredResult#setResult(Object)} 或
	 * {@link DeferredResult#setErrorResult(Object)} 以恢复处理。
	 *
	 * @param request        当前请求
	 * @param deferredResult 当前请求的 DeferredResult；如果 {@code DeferredResult} 已设置，
	 *                       则恢复并发处理，不再调用后续拦截器
	 * @return 如果处理应继续，则返回 {@code true}；如果不应调用其他拦截器，则返回 {@code false}
	 * @throws Exception 如果出现错误
	 */
	default <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {

		return true;
	}

	/**
	 * 当在处理异步请求时发生错误，并且在 {@code DeferredResult} 设置之前，从容器线程调用。
	 * 实现可以调用 {@link DeferredResult#setResult(Object)} 或
	 * {@link DeferredResult#setErrorResult(Object)} 以恢复处理。
	 *
	 * @param request        当前请求
	 * @param deferredResult 当前请求的 DeferredResult；如果 {@code DeferredResult} 已设置，
	 *                       则恢复并发处理，不再调用后续拦截器
	 * @param t              处理请求时发生的错误
	 * @return 如果错误处理应继续，则返回 {@code true}；如果应绕过其他拦截器并且不应调用，则返回 {@code false}
	 * @throws Exception 如果出现错误
	 */
	default <T> boolean handleError(NativeWebRequest request, DeferredResult<T> deferredResult,
									Throwable t) throws Exception {

		return true;
	}

	/**
	 * 当由于任何原因（包括超时和网络错误）完成异步请求时，从容器线程调用。
	 * 此方法可用于检测 {@code DeferredResult} 实例不再可用。
	 *
	 * @param request        当前请求
	 * @param deferredResult 当前请求的 DeferredResult
	 * @throws Exception 如果出现错误
	 */
	default <T> void afterCompletion(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {
	}

}
