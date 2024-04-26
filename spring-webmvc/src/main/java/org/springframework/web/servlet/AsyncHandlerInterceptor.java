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

package org.springframework.web.servlet;

import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 扩展了 {@code HandlerInterceptor}，具有在异步请求处理开始后调用的回调方法。
 *
 * <p>当处理程序启动异步请求时，{@link DispatcherServlet} 不会像对同步请求一样调用 {@code postHandle} 和 {@code afterCompletion}，因为请求处理的结果（例如 ModelAndView）可能尚未准备好，将会从另一个线程并发生成。在这种情况下，将调用 {@link #afterConcurrentHandlingStarted}，允许实现执行诸如清理线程绑定属性等任务，然后将线程释放给 Servlet 容器。
 *
 * <p>当异步处理完成时，请求将被分派到容器进行进一步处理。在这个阶段，{@code DispatcherServlet} 调用 {@code preHandle}、{@code postHandle} 和 {@code afterCompletion}。为了区分初始请求和异步处理完成后的后续分派，拦截器可以检查 {@link javax.servlet.ServletRequest} 的 {@code javax.servlet.DispatcherType} 是否为 {@code "REQUEST"} 或 {@code "ASYNC"}。
 *
 * <p>请注意，{@code HandlerInterceptor} 实现可能需要在异步请求超时或完成时执行工作。对于这种情况，Servlet 容器不会进行分派，因此不会调用 {@code postHandle} 和 {@code afterCompletion} 方法。相反，拦截器可以通过 {@link org.springframework.web.context.request.async.WebAsyncManager WebAsyncManager} 的 {@code registerCallbackInterceptor} 和 {@code registerDeferredResultInterceptor} 方法注册以跟踪异步请求。这可以在每个请求的 {@code preHandle} 中主动完成，无论异步请求处理是否开始。
 *
 * @author Rossen Stoyanchev
 * @see org.springframework.web.context.request.async.WebAsyncManager
 * @see org.springframework.web.context.request.async.CallableProcessingInterceptor
 * @see org.springframework.web.context.request.async.DeferredResultProcessingInterceptor
 * @since 3.2
 */
public interface AsyncHandlerInterceptor extends HandlerInterceptor {

	/**
	 * 在处理程序并发执行时调用，而不是在处理程序执行完成后调用 {@code postHandle} 和 {@code afterCompletion}。
	 * <p>实现可以使用提供的请求和响应，但应避免以与处理程序的并发执行冲突的方式修改它们。
	 * 此方法的典型用途是清理线程本地变量。
	 *
	 * @param request  当前请求
	 * @param response 当前响应
	 * @param handler  启动异步执行的处理程序（或 {@link HandlerMethod}），用于类型和/或实例检查
	 * @throws Exception 发生错误时
	 */
	default void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response,
												Object handler) throws Exception {
	}

}
