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

package org.springframework.web.context.request;

/**
 * 通过在异步请求处理期间调用回调方法扩展 {@code WebRequestInterceptor}。
 *
 * <p>当处理程序开始异步请求处理时，DispatcherServlet 将退出而不调用 {@code postHandle}
 * 和 {@code afterCompletion}，因为它通常不会，因为请求处理的结果（例如 ModelAndView）
 * 在当前线程中不可用，并且处理尚未完成。
 * 在这种情况下，将调用 {@link #afterConcurrentHandlingStarted(WebRequest)}
 * 方法，允许实现执行诸如清理线程绑定属性等任务。
 *
 * <p>当异步处理完成时，请求将被分派到容器进行进一步处理。在此阶段，DispatcherServlet
 * 会像往常一样调用 {@code preHandle}、{@code postHandle} 和 {@code afterCompletion}。
 *
 * @author Rossen Stoyanchev
 * @see org.springframework.web.context.request.async.WebAsyncManager
 * @since 3.2
 */
public interface AsyncWebRequestInterceptor extends WebRequestInterceptor {

	/**
	 * 在处理程序开始并发处理请求时，调用此方法而不是 {@code postHandle} 和 {@code afterCompletion}。
	 *
	 * @param request 当前请求
	 */
	void afterConcurrentHandlingStarted(WebRequest request);

}
