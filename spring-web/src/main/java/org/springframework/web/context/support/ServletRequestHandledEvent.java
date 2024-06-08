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

package org.springframework.web.context.support;

import org.springframework.lang.Nullable;

/**
 * RequestHandledEvent 的 Servlet 特定子类，添加了与 Servlet 相关的上下文信息。
 *
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.FrameworkServlet
 * @see org.springframework.context.ApplicationContext#publishEvent
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ServletRequestHandledEvent extends RequestHandledEvent {

	/**
	 * 触发请求的 URL。
	 */
	private final String requestUrl;

	/**
	 * 请求来源的 IP 地址。
	 */
	private final String clientAddress;

	/**
	 * 通常是 GET 或 POST。
	 */
	private final String method;

	/**
	 * 处理请求的 Servlet 的名称。
	 */
	private final String servletName;

	/**
	 * 响应的 HTTP 状态码。
	 */
	private final int statusCode;

	/**
	 * 创建一个新的 ServletRequestHandledEvent。
	 *
	 * @param source               发布事件的组件
	 * @param requestUrl           请求的 URL
	 * @param clientAddress        请求来源的 IP 地址
	 * @param method               请求的 HTTP 方法（通常是 GET 或 POST）
	 * @param servletName          处理请求的 Servlet 的名称
	 * @param sessionId            HTTP 会话的 ID（如果有）
	 * @param userName             与请求关联的用户的名称（如果有，通常是 UserPrincipal）
	 * @param processingTimeMillis 请求的处理时间（毫秒）
	 */
	public ServletRequestHandledEvent(Object source, String requestUrl,
									  String clientAddress, String method, String servletName,
									  @Nullable String sessionId, @Nullable String userName, long processingTimeMillis) {
		super(source, sessionId, userName, processingTimeMillis);
		this.requestUrl = requestUrl;
		this.clientAddress = clientAddress;
		this.method = method;
		this.servletName = servletName;
		this.statusCode = -1;
	}

	/**
	 * 创建一个新的 Servlet请求已处理事件。
	 *
	 * @param source               发布事件的组件
	 * @param requestUrl           请求的 URL
	 * @param clientAddress        请求来源的 IP 地址
	 * @param method               请求的 HTTP 方法（通常是 GET 或 POST）
	 * @param servletName          处理请求的 Servlet 的名称
	 * @param sessionId            HTTP 会话的 ID（如果有）
	 * @param userName             与请求关联的用户的名称（如果有，通常是 UserPrincipal）
	 * @param processingTimeMillis 请求的处理时间（毫秒）
	 * @param failureCause         失败的原因（如果有）
	 */
	public ServletRequestHandledEvent(Object source, String requestUrl,
									  String clientAddress, String method, String servletName, @Nullable String sessionId,
									  @Nullable String userName, long processingTimeMillis, @Nullable Throwable failureCause) {
		super(source, sessionId, userName, processingTimeMillis, failureCause);
		this.requestUrl = requestUrl;
		this.clientAddress = clientAddress;
		this.method = method;
		this.servletName = servletName;
		this.statusCode = -1;
	}

	/**
	 * 创建一个新的 Servlet请求已处理事件。
	 *
	 * @param source               发布事件的组件
	 * @param requestUrl           请求的 URL
	 * @param clientAddress        请求来源的 IP 地址
	 * @param method               请求的 HTTP 方法（通常是 GET 或 POST）
	 * @param servletName          处理请求的 Servlet 的名称
	 * @param sessionId            HTTP 会话的 ID（如果有）
	 * @param userName             与请求关联的用户的名称（如果有，通常是 UserPrincipal）
	 * @param processingTimeMillis 请求的处理时间（毫秒）
	 * @param failureCause         失败的原因（如果有）
	 * @param statusCode           响应的 HTTP 状态码
	 */
	public ServletRequestHandledEvent(Object source, String requestUrl,
									  String clientAddress, String method, String servletName, @Nullable String sessionId,
									  @Nullable String userName, long processingTimeMillis, @Nullable Throwable failureCause, int statusCode) {
		super(source, sessionId, userName, processingTimeMillis, failureCause);
		this.requestUrl = requestUrl;
		this.clientAddress = clientAddress;
		this.method = method;
		this.servletName = servletName;
		this.statusCode = statusCode;
	}

	/**
	 * 返回请求的 URL。
	 */
	public String getRequestUrl() {
		return this.requestUrl;
	}

	/**
	 * 返回请求来源的 IP 地址。
	 */
	public String getClientAddress() {
		return this.clientAddress;
	}

	/**
	 * 返回请求的 HTTP 方法（通常是 GET 或 POST）。
	 */
	public String getMethod() {
		return this.method;
	}

	/**
	 * 返回处理请求的 Servlet 的名称。
	 */
	public String getServletName() {
		return this.servletName;
	}

	/**
	 * 返回响应的 HTTP 状态码，如果状态码不可用则返回 -1。
	 *
	 * @since 4.1
	 */
	public int getStatusCode() {
		return this.statusCode;
	}

	@Override
	public String getShortDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("url=[").append(getRequestUrl()).append("]; ");
		sb.append("client=[").append(getClientAddress()).append("]; ");
		sb.append(super.getShortDescription());
		return sb.toString();
	}

	@Override
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("url=[").append(getRequestUrl()).append("]; ");
		sb.append("client=[").append(getClientAddress()).append("]; ");
		sb.append("method=[").append(getMethod()).append("]; ");
		sb.append("servlet=[").append(getServletName()).append("]; ");
		sb.append(super.getDescription());
		return sb.toString();
	}

	@Override
	public String toString() {
		return "ServletRequestHandledEvent: " + getDescription();
	}

}
