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

package org.springframework.mock.http.client;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * {@link ClientHttpRequest} 的模拟实现。
 *
 * <p>该类用于模拟客户端的 HTTP 请求，并提供设置和获取请求方法、URI、响应等功能。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class MockClientHttpRequest extends MockHttpOutputMessage implements ClientHttpRequest {

	/**
	 * HTTP 请求的方法。
	 */
	private HttpMethod httpMethod;

	/**
	 * HTTP 请求的 URI。
	 */
	private URI uri;

	/**
	 * 客户端响应的 {@link ClientHttpResponse} 实例。
	 */
	@Nullable
	private ClientHttpResponse clientHttpResponse;

	/**
	 * 表示请求是否已经执行的标志。
	 */
	private boolean executed = false;


	/**
	 * 默认构造方法，使用 GET 方法和根路径 URI 创建实例。
	 */
	public MockClientHttpRequest() {
		// 设置HTTP方法为GET
		this.httpMethod = HttpMethod.GET;

		try {
			// 尝试创建URI对象，路径为"/"
			this.uri = new URI("/");
		} catch (URISyntaxException ex) {
			// 如果发生URI语法异常，抛出非法状态异常，并附带异常信息
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * 使用给定的 HttpMethod 和 URI 创建实例。
	 *
	 * @param httpMethod HTTP 请求方法
	 * @param uri        HTTP 请求 URI
	 */
	public MockClientHttpRequest(HttpMethod httpMethod, URI uri) {
		this.httpMethod = httpMethod;
		this.uri = uri;
	}


	/**
	 * 设置 HTTP 请求的方法。
	 *
	 * @param httpMethod HTTP 请求方法
	 */
	public void setMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	/**
	 * 获取 HTTP 请求的方法。
	 *
	 * @return HTTP 请求方法
	 */
	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	/**
	 * 获取 HTTP 请求的方法值。
	 *
	 * @return HTTP 请求方法值
	 */
	@Override
	public String getMethodValue() {
		return this.httpMethod.name();
	}

	/**
	 * 设置 HTTP 请求的 URI。
	 *
	 * @param uri HTTP 请求 URI
	 */
	public void setURI(URI uri) {
		this.uri = uri;
	}

	/**
	 * 获取 HTTP 请求的 URI。
	 *
	 * @return HTTP 请求 URI
	 */
	@Override
	public URI getURI() {
		return this.uri;
	}

	/**
	 * 设置客户端响应 {@link ClientHttpResponse}。
	 *
	 * @param clientHttpResponse 客户端响应对象
	 */
	public void setResponse(ClientHttpResponse clientHttpResponse) {
		this.clientHttpResponse = clientHttpResponse;
	}

	/**
	 * 检查请求是否已经执行。
	 *
	 * @return 如果请求已经执行返回 true，否则返回 false
	 */
	public boolean isExecuted() {
		return this.executed;
	}

	/**
	 * 将 {@link #isExecuted() executed} 标志设置为 true，并返回配置的 {@link #setResponse(ClientHttpResponse) 响应}。
	 *
	 * @return 执行请求后的响应对象
	 * @throws IOException 如果发生 I/O 错误
	 * @see #executeInternal()
	 */
	@Override
	public final ClientHttpResponse execute() throws IOException {
		this.executed = true;
		return executeInternal();
	}

	/**
	 * 默认实现返回配置的 {@link #setResponse(ClientHttpResponse) 响应}。
	 * <p>覆盖此方法以执行请求并返回一个响应，可能与配置的响应不同。
	 *
	 * @return 执行请求后的响应对象
	 * @throws IOException 如果发生 I/O 错误
	 */
	protected ClientHttpResponse executeInternal() throws IOException {
		Assert.state(this.clientHttpResponse != null, "No ClientHttpResponse");
		return this.clientHttpResponse;
	}

	/**
	 * 返回对象的字符串表示形式，包括 HTTP 方法、URI 和头部信息（如果有）。
	 *
	 * @return 对象的字符串表示形式
	 */
	@Override
	public String toString() {
		// 创建一个StringBuilder对象
		StringBuilder sb = new StringBuilder();

		// 将HTTP方法追加到字符串构建器中
		sb.append(this.httpMethod);
		// 追加空格和URI到字符串构建器中
		sb.append(' ').append(this.uri);

		// 如果请求头部不为空
		if (!getHeaders().isEmpty()) {
			// 追加头部信息到字符串构建器中
			sb.append(", headers: ").append(getHeaders());
		}

		// 如果字符串构建器长度为0（表示未初始化）
		if (sb.length() == 0) {
			// 添加未初始化的提示信息到字符串构建器中
			sb.append("Not yet initialized");
		}

		// 返回字符串构建器的字符串表示形式
		return sb.toString();
	}

}
