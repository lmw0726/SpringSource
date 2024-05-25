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

package org.springframework.web;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 处理 HTTP 请求的简单处理器接口，类似于 Servlet。仅声明 {@link javax.servlet.ServletException}
 * 和 {@link java.io.IOException}，以便在任何 {@link javax.servlet.http.HttpServlet} 中使用。
 * 此接口本质上是 HttpServlet 的直接等价物，简化为一个中央的处理方法。
 *
 * <p>以 Spring 风格公开 HttpRequestHandler bean 最简单的方法是在 Spring 的根 Web 应用程序上下文中定义它，
 * 并在 {@code web.xml} 中定义一个 {@link org.springframework.web.context.support.HttpRequestHandlerServlet}，
 * 通过其 {@code servlet-name} 指向目标 HttpRequestHandler bean，该名称需要与目标 bean 名称匹配。
 *
 * <p>在 Spring 的 {@link org.springframework.web.servlet.DispatcherServlet} 中作为处理器类型支持，
 * 能够与调度程序的高级映射和拦截功能进行交互。这是推荐的公开 HttpRequestHandler 的方式，
 * 同时保持处理器实现没有对 DispatcherServlet 环境的直接依赖。
 *
 * <p>通常实现为直接生成二进制响应，没有涉及单独的视图资源。这使它区别于 Spring Web MVC 框架中的
 * {@link org.springframework.web.servlet.mvc.Controller}。缺少 {@link org.springframework.web.servlet.ModelAndView}
 * 返回值为调度程序以外的调用者提供了更清晰的签名，表明将永远不会有视图需要渲染。
 *
 * <p>从 Spring 2.0 开始，Spring 基于 HTTP 的远程导出器，例如
 * {@link org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter}
 * 和 {@link org.springframework.remoting.caucho.HessianServiceExporter}，
 * 实现了此接口而不是更广泛的 Controller 接口，以尽量减少对 Spring 特定 Web 基础设施的依赖。
 *
 * <p>请注意，HttpRequestHandlers 可以选择性地实现 {@link org.springframework.web.servlet.mvc.LastModified} 接口，
 * 就像 Controllers 一样，<i>前提是它们运行在 Spring 的 DispatcherServlet 中</i>。
 * 然而，这通常不是必须的，因为 HttpRequestHandlers 通常只支持 POST 请求。
 * 或者，处理器可以在其 {@code handle} 方法中手动实现 "If-Modified-Since" HTTP 头处理。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.web.context.support.HttpRequestHandlerServlet
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.servlet.ModelAndView
 * @see org.springframework.web.servlet.mvc.Controller
 * @see org.springframework.web.servlet.mvc.LastModified
 * @see org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter
 * @see org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter
 * @see org.springframework.remoting.caucho.HessianServiceExporter
 */
@FunctionalInterface
public interface HttpRequestHandler {

	/**
	 * 处理给定的请求，生成响应。
	 *
	 * @param request  当前的 HTTP 请求
	 * @param response 当前的 HTTP 响应
	 * @throws ServletException 在发生一般错误时
	 * @throws IOException      在发生 I/O 错误时
	 */
	void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException;

}
