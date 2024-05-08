/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;

/**
 * 支持上次修改的 HTTP 请求以便实现内容缓存。
 * 与 Servlet API 的 {@code getLastModified} 方法具有相同的合同。
 *
 * <p>由 {@link org.springframework.web.servlet.HandlerAdapter#getLastModified} 实现委托。
 * 默认情况下，Spring 默认框架中的任何 Controller 或 HttpRequestHandler 都可以实现此接口以启用上次修改的检查。
 *
 * <p><b>注意:</b> 替代的处理程序实现方法具有不同的上次修改处理风格。例如，Spring 2.5 的注解控制器方法（使用 {@code @RequestMapping}）
 * 通过 {@link org.springframework.web.context.request.WebRequest#checkNotModified} 方法提供了上次修改的支持，
 * 允许在主处理程序方法中进行上次修改的检查。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see javax.servlet.http.HttpServlet#getLastModified
 * @see Controller
 * @see SimpleControllerHandlerAdapter
 * @see org.springframework.web.HttpRequestHandler
 * @see HttpRequestHandlerAdapter
 * @deprecated 自 5.3.9 起，建议使用 {@link org.springframework.web.context.request.WebRequest} 中的 {@code checkNotModified} 方法，
 * 或者从注解控制器方法返回设置了 "ETag" 和/或 "Last-Modified" 标头的 {@link org.springframework.http.ResponseEntity}。
 */
@Deprecated
public interface LastModified {

	/**
	 * 与 HttpServlet 的 {@code getLastModified} 方法具有相同的合同。
	 * 在请求处理之前调用。
	 * <p>返回值将作为 Last-Modified 标头发送给 HTTP 客户端，并与客户端发送回来的 If-Modified-Since 标头进行比较。
	 * 只有在发生修改时内容才会重新生成。
	 * @param request 当前 HTTP 请求
	 * @return 底层资源上次修改的时间，或 -1 表示内容必须始终重新生成
	 * @see org.springframework.web.servlet.HandlerAdapter#getLastModified
	 * @see javax.servlet.http.HttpServlet#getLastModified
	 */
	long getLastModified(HttpServletRequest request);

}
