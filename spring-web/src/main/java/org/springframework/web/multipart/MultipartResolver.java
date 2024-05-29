/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.multipart;

import javax.servlet.http.HttpServletRequest;

/**
 * 与 <a href="https://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a> 符合的多部分文件上传解析策略的策略接口。
 * 实现通常可在应用程序上下文和独立环境中使用。
 *
 * <p>截至 Spring 3.1，Spring 中包含两个具体的实现：
 * <ul>
 * <li>{@link org.springframework.web.multipart.commons.CommonsMultipartResolver} 用于 Apache Commons FileUpload
 * <li>{@link org.springframework.web.multipart.support.StandardServletMultipartResolver} 用于 Servlet 3.0+ Part API
 * </ul>
 *
 * <p>对于 Spring 的 {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}，没有默认的解析器实现，
 * 因为应用程序可能选择自行解析其多部分请求。要定义一个实现，请在 {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
 * 的应用程序上下文中创建一个 ID 为 "multipartResolver" 的 bean。此类解析器将应用于由该 {@link org.springframework.web.servlet.DispatcherServlet}
 * 处理的所有请求。
 *
 * <p>如果 {@link org.springframework.web.servlet.DispatcherServlet} 检测到多部分请求，它将通过配置的 {@link MultipartResolver} 解析该请求，
 * 并传递一个包装后的 {@link javax.servlet.http.HttpServletRequest}。控制器随后可以将给定的请求转换为 {@link MultipartHttpServletRequest}
 * 接口，该接口允许访问任何 {@link MultipartFile MultipartFiles}。请注意，此转换仅在实际为多部分请求时受支持。
 *
 * <pre class="code">
 * public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
 *   MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
 *   MultipartFile multipartFile = multipartRequest.getFile("image");
 *   ...
 * }</pre>
 * <p>
 * 与直接访问相比，命令或表单控制器可以在其数据绑定器中注册一个 {@link org.springframework.web.multipart.support.ByteArrayMultipartFileEditor}
 * 或 {@link org.springframework.web.multipart.support.StringMultipartFileEditor}，以自动将多部分内容应用于表单 bean 属性。
 *
 * <p>作为使用 {@link org.springframework.web.servlet.DispatcherServlet} 的 {@link MultipartResolver} 的替代方法，
 * 可以在 {@code web.xml} 中注册一个 {@link org.springframework.web.multipart.support.MultipartFilter}。它将委托给根应用程序上下文中的
 * 相应 {@link MultipartResolver} bean。这主要适用于不使用 Spring 自己的 Web MVC 框架的应用程序。
 *
 * <p>注意：几乎不需要从应用程序代码中访问 {@link MultipartResolver} 本身。它将在后台执行其工作，使 {@link MultipartHttpServletRequest MultipartHttpServletRequests}
 * 可用于控制器。
 *
 * @author Juergen Hoeller
 * @author Trevor D. Cook
 * @see MultipartHttpServletRequest
 * @see MultipartFile
 * @see org.springframework.web.multipart.commons.CommonsMultipartResolver
 * @see org.springframework.web.multipart.support.ByteArrayMultipartFileEditor
 * @see org.springframework.web.multipart.support.StringMultipartFileEditor
 * @see org.springframework.web.servlet.DispatcherServlet
 * @since 29.09.2003
 */
public interface MultipartResolver {

	/**
	 * 确定给定的请求是否包含多部分内容。
	 * <p>通常将检查内容类型为 "multipart/form-data"，但实际接受的请求可能取决于解析器实现的功能。
	 *
	 * @param request 要评估的 Servlet 请求
	 * @return 请求是否包含多部分内容
	 */
	boolean isMultipart(HttpServletRequest request);

	/**
	 * 解析给定的 HTTP 请求为多部分文件和参数，并将请求包装在
	 * {@link org.springframework.web.multipart.MultipartHttpServletRequest} 对象中，该对象提供对文件描述符的访问，
	 * 并使包含的参数可通过标准的 ServletRequest 方法访问。
	 *
	 * @param request 要包装的 Servlet 请求（必须是多部分内容类型）
	 * @return 包装后的 Servlet 请求
	 * @throws MultipartException 如果 Servlet 请求不是多部分的，或者遇到实现特定的问题（如超过文件大小限制）
	 * @see MultipartHttpServletRequest#getFile
	 * @see MultipartHttpServletRequest#getFileNames
	 * @see MultipartHttpServletRequest#getFileMap
	 * @see javax.servlet.http.HttpServletRequest#getParameter
	 * @see javax.servlet.http.HttpServletRequest#getParameterNames
	 * @see javax.servlet.http.HttpServletRequest#getParameterMap
	 */
	MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException;

	/**
	 * 清理用于多部分处理的任何资源，例如上传文件的存储。
	 *
	 * @param request 要清理资源的请求
	 */
	void cleanupMultipart(MultipartHttpServletRequest request);

}
