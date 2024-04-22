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

package org.springframework.web.servlet;

import org.springframework.http.HttpHeaders;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 默认情况下，当 DispatcherServlet 找不到请求的处理程序时，它会发送一个 404 响应。
 * 但是，如果它的属性 "throwExceptionIfNoHandlerFound" 设置为 {@code true}，则会引发此异常，
 * 并可以使用配置的 HandlerExceptionResolver 进行处理。
 *
 * @author Brian Clozel
 * @see DispatcherServlet#setThrowExceptionIfNoHandlerFound(boolean)
 * @see DispatcherServlet#noHandlerFound(HttpServletRequest, HttpServletResponse)
 * @since 4.0
 */
@SuppressWarnings("serial")
public class NoHandlerFoundException extends ServletException {

	/**
	 * HTTP方法
	 */
	private final String httpMethod;

	/**
	 * 请求URL
	 */
	private final String requestURL;

	/**
	 * 请求头
	 */
	private final HttpHeaders headers;


	/**
	 * NoHandlerFoundException 的构造函数。
	 *
	 * @param httpMethod HTTP 方法
	 * @param requestURL HTTP 请求的 URL
	 * @param headers    HTTP 请求头
	 */
	public NoHandlerFoundException(String httpMethod, String requestURL, HttpHeaders headers) {
		super("No handler found for " + httpMethod + " " + requestURL);
		this.httpMethod = httpMethod;
		this.requestURL = requestURL;
		this.headers = headers;
	}


	public String getHttpMethod() {
		return this.httpMethod;
	}

	public String getRequestURL() {
		return this.requestURL;
	}

	public HttpHeaders getHeaders() {
		return this.headers;
	}

}
