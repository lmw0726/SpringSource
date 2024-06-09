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

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.concurrent.Callable;

/**
 * 拦截并发请求处理，其中并发结果是通过在应用程序代表下执行 {@link Callable} 的方式与 {@link AsyncTaskExecutor} 获得的。
 *
 * <p>{@code CallableProcessingInterceptor} 在异步线程中执行 {@code Callable} 任务之前和之后，以及在容器线程中超时/错误时或因任何原因完成处理之前都会被调用。
 *
 * <p>通常情况下，拦截器方法抛出的异常将导致异步处理恢复，通过返回到容器并使用异常实例作为并发结果。然后，这些异常将通过 {@code HandlerExceptionResolver} 机制进行处理。
 *
 * <p>{@link #handleTimeout(NativeWebRequest, Callable) handleTimeout} 方法可以选择一个值来恢复处理。
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 */
public interface CallableProcessingInterceptor {

	/**
	 * 指示此拦截器未确定结果的常量，给后续拦截器一个机会。
	 *
	 * @see #handleTimeout
	 * @see #handleError
	 */
	Object RESULT_NONE = new Object();

	/**
	 * 指示此拦截器处理了响应但没有结果，并且不会调用其他拦截器的常量。
	 *
	 * @see #handleTimeout
	 * @see #handleError
	 */
	Object RESPONSE_HANDLED = new Object();


	/**
	 * 在原始线程中提交 {@code Callable} 进行并发处理之前调用。
	 * <p>这对于在调用 {@link Callable} 之前捕获当前线程的状态很有用。一旦捕获状态，它就可以传递到新的 {@link Thread} 中，在 {@link #preProcess(NativeWebRequest, Callable)} 中使用。捕获 Spring Security 的 SecurityContextHolder 的状态并将其迁移到新的线程是一个具体的例子。
	 * <p>默认实现为空。
	 *
	 * @param request 当前请求
	 * @param task    当前异步请求的任务
	 * @throws Exception 发生错误时
	 */
	default <T> void beforeConcurrentHandling(NativeWebRequest request, Callable<T> task) throws Exception {
	}

	/**
	 * 在异步线程中执行 {@code Callable} 并在实际调用 {@code Callable} 之前调用。
	 * <p>默认实现为空。
	 *
	 * @param request 当前请求
	 * @param task    当前异步请求的任务
	 * @throws Exception 发生错误时
	 */
	default <T> void preProcess(NativeWebRequest request, Callable<T> task) throws Exception {
	}

	/**
	 * 在 {@code Callable} 在异步线程中产生结果后调用。此方法可能比 {@code afterTimeout} 或 {@code afterCompletion} 晚调用，具体取决于 {@code Callable} 何时完成处理。
	 * <p>默认实现为空。
	 *
	 * @param request          当前请求
	 * @param task             当前异步请求的任务
	 * @param concurrentResult 并发处理的结果，如果 {@code Callable} 引发了异常，则可能是 {@link Throwable}
	 * @throws Exception 发生错误时
	 */
	default <T> void postProcess(NativeWebRequest request, Callable<T> task,
								 Object concurrentResult) throws Exception {
	}

	/**
	 * 在异步请求处理完成之前 {@code Callable} 任务超时时，从容器线程调用。实现可以返回一个值，包括 {@link Exception}，以用于替代 {@link Callable} 未及时返回的值。
	 * <p>默认实现始终返回 {@link #RESULT_NONE}。
	 *
	 * @param request 当前请求
	 * @param task    当前异步请求的任务
	 * @return 并发结果值；如果值不是 {@link #RESULT_NONE} 或 {@link #RESPONSE_HANDLED}，则会恢复并发处理，不会调用后续拦截器
	 * @throws Exception 发生错误时
	 */
	default <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) throws Exception {
		return RESULT_NONE;
	}

	/**
	 * 在异步请求处理期间出现错误时从容器线程调用 {@code Callable} 任务。实现可以返回一个值，包括 {@link Exception}，以用于替代 {@link Callable} 未及时返回的值。
	 * <p>默认实现始终返回 {@link #RESULT_NONE}。
	 *
	 * @param request 当前请求
	 * @param task    当前异步请求的任务
	 * @param t       在处理请求时发生的错误
	 * @return 并发结果值；如果值不是 {@link #RESULT_NONE} 或 {@link #RESPONSE_HANDLED}，则会恢复并发处理，不会调用后续拦截器
	 * @throws Exception 发生错误时
	 * @since 5.0
	 */
	default <T> Object handleError(NativeWebRequest request, Callable<T> task, Throwable t) throws Exception {
		return RESULT_NONE;
	}

	/**
	 * 在异步处理完成时从容器线程调用，包括超时或网络错误。
	 * <p>默认实现为空。
	 *
	 * @param request 当前请求
	 * @param task    当前异步请求的任务
	 * @throws Exception 发生错误时
	 */
	default <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
	}

}
