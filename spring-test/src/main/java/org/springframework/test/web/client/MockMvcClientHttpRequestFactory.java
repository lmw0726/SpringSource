/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.web.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;


/**
 * 用于通过 {@link MockMvc} 执行请求的 {@link ClientHttpRequestFactory}。
 *
 * <p>从5.0版本开始，该类还实现了 {@link org.springframework.http.client.AsyncClientHttpRequestFactory
 * AsyncClientHttpRequestFactory} 接口。然而请注意，{@link org.springframework.web.client.AsyncRestTemplate}
 * 及相关类在同一时间被标记为过时。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
@SuppressWarnings("deprecation")
public class MockMvcClientHttpRequestFactory
		implements ClientHttpRequestFactory, org.springframework.http.client.AsyncClientHttpRequestFactory {
	/**
	 * 模拟MVC
	 */
	private final MockMvc mockMvc;

	/**
	 * 使用给定的 {@link MockMvc} 实例创建一个新的工厂。
	 *
	 * @param mockMvc 要使用的 MockMvc 实例，不能为空
	 */
	public MockMvcClientHttpRequestFactory(MockMvc mockMvc) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		this.mockMvc = mockMvc;
	}

	/**
	 * 创建一个 {@link ClientHttpRequest} 对象，用于执行指定 URI 和 HTTP 方法的请求。
	 *
	 * @param uri        要创建请求的 URI
	 * @param httpMethod 要执行的 HTTP 方法
	 * @return 创建的请求对象
	 */
	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
		return new MockClientHttpRequest(httpMethod, uri) {
			@Override
			public ClientHttpResponse executeInternal() throws IOException {
				return getClientHttpResponse(httpMethod, uri, getHeaders(), getBodyAsBytes());
			}
		};
	}

	/**
	 * 创建一个 {@link org.springframework.http.client.AsyncClientHttpRequest} 对象，用于执行指定 URI 和 HTTP 方法的异步请求。
	 *
	 * @param uri    要创建请求的 URI
	 * @param method 要执行的 HTTP 方法
	 * @return 创建的异步请求对象
	 */
	@Override
	public org.springframework.http.client.AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod method) {
		return new org.springframework.mock.http.client.MockAsyncClientHttpRequest(method, uri) {
			@Override
			protected ClientHttpResponse executeInternal() throws IOException {
				return getClientHttpResponse(method, uri, getHeaders(), getBodyAsBytes());
			}
		};
	}

	private ClientHttpResponse getClientHttpResponse(
			HttpMethod httpMethod, URI uri, HttpHeaders requestHeaders, byte[] requestBody) {

		try {
			// 使用 模拟Mvc 执行请求，并获取 模拟HttpServlet响应 对象
			MockHttpServletResponse servletResponse = this.mockMvc
					.perform(request(httpMethod, uri).content(requestBody).headers(requestHeaders))
					.andReturn()
					.getResponse();

			// 获取响应状态码
			HttpStatus status = HttpStatus.valueOf(servletResponse.getStatus());
			// 获取响应体内容
			byte[] body = servletResponse.getContentAsByteArray();
			// 创建 模拟客户端Http响应 对象
			MockClientHttpResponse clientResponse = new MockClientHttpResponse(body, status);
			// 将 Servlet响应 的响应头复制到 客户响应 的 响应头 中
			clientResponse.getHeaders().putAll(getResponseHeaders(servletResponse));
			// 返回 客户响应
			return clientResponse;
		} catch (Exception ex) {
			// 如果捕获到异常，将异常信息转换为字节数组
			byte[] body = ex.toString().getBytes(StandardCharsets.UTF_8);
			// 返回一个带有异常信息和状态码 内部服务器错误 的 模拟客户端Http响应 对象
			return new MockClientHttpResponse(body, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private HttpHeaders getResponseHeaders(MockHttpServletResponse response) {
		// 创建新的 Http标头 对象
		HttpHeaders headers = new HttpHeaders();
		// 遍历响应中的所有头部名称
		for (String name : response.getHeaderNames()) {
			// 获取当前头部名称对应的所有值
			List<String> values = response.getHeaders(name);
			// 将每个值添加到 Http标头 对象中
			for (String value : values) {
				headers.add(name, value);
			}
		}
		// 返回填充了响应头部信息的 Http标头 对象
		return headers;
	}

}
