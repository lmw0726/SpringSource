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

package org.springframework.http.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;

/**
 * 基于 Apache HttpComponents HttpClient 的 {@link ClientHttpRequest} 实现。
 *
 * <p>通过 {@link HttpComponentsClientHttpRequestFactory} 创建。
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see HttpComponentsClientHttpRequestFactory#createRequest(URI, HttpMethod)
 * @since 3.1
 */
final class HttpComponentsClientHttpRequest extends AbstractBufferingClientHttpRequest {

	/**
	 * 使用的 HttpClient 实例。
	 */
	private final HttpClient httpClient;

	/**
	 * HTTP 请求实例。
	 */
	private final HttpUriRequest httpRequest;

	/**
	 * HTTP 请求上下文。
	 */
	private final HttpContext httpContext;

	/**
	 * 构造一个 {@code HttpComponentsClientHttpRequest} 实例。
	 *
	 * @param client  HttpClient 实例
	 * @param request HTTP 请求实例
	 * @param context HTTP 请求上下文
	 */
	HttpComponentsClientHttpRequest(HttpClient client, HttpUriRequest request, HttpContext context) {
		this.httpClient = client;
		this.httpRequest = request;
		this.httpContext = context;
	}

	/**
	 * 获取 HTTP 请求的方法值。
	 *
	 * @return HTTP 请求的方法值
	 */
	@Override
	public String getMethodValue() {
		return this.httpRequest.getMethod();
	}

	/**
	 * 获取 HTTP 请求的 URI。
	 *
	 * @return HTTP 请求的 URI
	 */
	@Override
	public URI getURI() {
		return this.httpRequest.getURI();
	}

	/**
	 * 获取 HTTP 请求的上下文。
	 *
	 * @return HTTP 请求的上下文
	 */
	HttpContext getHttpContext() {
		return this.httpContext;
	}

	/**
	 * 执行 HTTP 请求的内部实现，将给定的头部和内容写入到请求中，并处理响应。
	 *
	 * @param headers        HTTP 请求头
	 * @param bufferedOutput 请求体的字节数组内容
	 * @return 执行请求后的响应对象
	 * @throws IOException 如果发生 I/O 错误
	 */
	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		// 添加请求头部信息
		addHeaders(this.httpRequest, headers);

		// 如果请求是包含实体的请求
		if (this.httpRequest instanceof HttpEntityEnclosingRequest) {
			// 将请求转换为包含实体的请求对象
			HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) this.httpRequest;
			// 创建一个请求实体，使用缓冲输出数据
			HttpEntity requestEntity = new ByteArrayEntity(bufferedOutput);
			// 设置请求实体到请求对象中
			entityEnclosingRequest.setEntity(requestEntity);
		}

		// 执行HTTP请求，并获取响应对象
		HttpResponse httpResponse = this.httpClient.execute(this.httpRequest, this.httpContext);

		// 返回一个基于HttpComponents的客户端响应对象
		return new HttpComponentsClientHttpResponse(httpResponse);
	}


	/**
	 * 将给定的 HTTP 头部添加到 HTTP 请求中。
	 *
	 * @param httpRequest HTTP 请求
	 * @param headers     要添加的头部
	 */
	static void addHeaders(HttpUriRequest httpRequest, HttpHeaders headers) {
		// 对请求头部信息进行迭代处理
		headers.forEach((headerName, headerValues) -> {
			// 如果是"Cookie"头部，按照RFC 6265规范处理
			if (HttpHeaders.COOKIE.equalsIgnoreCase(headerName)) {
				// 将多个Cookie值拼接成一个字符串，使用分号和空格分隔
				String headerValue = StringUtils.collectionToDelimitedString(headerValues, "; ");
				// 将处理后的Cookie头部值添加到HTTP请求中
				httpRequest.addHeader(headerName, headerValue);
			} else if (!HTTP.CONTENT_LEN.equalsIgnoreCase(headerName) &&
					!HTTP.TRANSFER_ENCODING.equalsIgnoreCase(headerName)) {
				// 对于非"Content-Length"和"Transfer-Encoding"的其他头部信息
				for (String headerValue : headerValues) {
					// 将每个头部值添加到HTTP请求中
					httpRequest.addHeader(headerName, headerValue);
				}
			}
		});
	}

}
