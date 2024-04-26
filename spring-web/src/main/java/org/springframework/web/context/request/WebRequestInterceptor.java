/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;

/**
 * 通用 Web 请求拦截的接口。通过构建在 {@link WebRequest} 抽象之上应用到 Servlet 请求。
 *
 * <p>该接口假定 MVC 风格的请求处理：执行处理程序，公开一组模型对象，然后基于该模型呈现视图。
 * 或者，处理程序也可以完全处理请求，不需要呈现视图。
 *
 * <p>在异步处理场景中，处理程序可能在单独的线程中执行，而主线程退出而不呈现或调用
 * {@code postHandle} 和 {@code afterCompletion} 回调。当并发处理程序执行完成时，
 * 请求被重新分派以继续呈现模型，并再次调用此合同的所有方法。有关更多选项和注释，
 * 请参见 {@code org.springframework.web.context.request.async.AsyncWebRequestInterceptor}
 *
 * <p>此接口有意保持最小化，以尽可能减少通用请求拦截器的依赖关系。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ServletWebRequest
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#setInterceptors
 * @see org.springframework.web.servlet.HandlerInterceptor
 */
public interface WebRequestInterceptor {

	/**
	 * 在调用请求处理程序 <i>之前</i> 拦截其执行。
	 * <p>允许准备上下文资源（例如 Hibernate 会话）并将其公开为请求属性或线程本地对象。
	 * @param request 当前的 Web 请求
	 * @throws Exception 如果发生错误
	 */
	void preHandle(WebRequest request) throws Exception;

	/**
	 * 在请求处理程序 <i>成功</i> 调用后，在视图渲染之前拦截执行。
	 * <p>允许在成功处理程序执行后修改上下文资源（例如，刷新 Hibernate 会话）。
	 * @param request 当前的 Web 请求
	 * @param model 将暴露给视图的模型对象的映射（可能为 {@code null}）。可用于分析暴露的模型和/或
	 * 添加进一步的模型属性（如果需要）。
	 * @throws Exception 如果发生错误
	 */
	void postHandle(WebRequest request, @Nullable ModelMap model) throws Exception;

	/**
	 * 在请求处理完成后的回调，即在渲染视图之后。将在处理程序执行的任何结果上调用，因此可用于正确的资源清理。
	 * <p>注意：仅在此拦截器的 {@code preHandle} 方法成功完成时才会调用！
	 * @param request 当前的 Web 请求
	 * @param ex 处理程序执行时抛出的异常（如果有）
	 * @throws Exception 如果发生错误
	 */
	void afterCompletion(WebRequest request, @Nullable Exception ex) throws Exception;

}
