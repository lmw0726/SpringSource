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

package org.springframework.http.client;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.net.URI;

/**
 * 基于 OkHttp 3.x 的 {@link ClientHttpRequest} 实现。
 *
 * <p>通过 {@link OkHttp3ClientHttpRequestFactory} 创建。
 *
 * <p>该类封装了 OkHttpClient 来执行 HTTP 请求，支持缓冲请求内容。提供了一种简洁的方式来处理
 * HTTP 请求和响应，特别是在需要利用 OkHttp 特性的情况下。
 *
 * <p>该实现适用于希望使用 OkHttp 作为底层 HTTP 客户端的应用场景，提供了对 HTTP 头信息和请求体
 * 的灵活操作。
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @author Roy Clarkson
 * @since 4.3
 */
class OkHttp3ClientHttpRequest extends AbstractBufferingClientHttpRequest {

	/**
	 * 用于执行 HTTP 请求的 OkHttpClient 实例。
	 */
	private final OkHttpClient client;

	/**
	 * HTTP 请求的 URI。
	 */
	private final URI uri;

	/**
	 * HTTP 请求的方法。
	 */
	private final HttpMethod method;

	/**
	 * 构造一个 {@code OkHttp3ClientHttpRequest} 实例。
	 *
	 * @param client 用于执行请求的 OkHttpClient 实例
	 * @param uri    HTTP 请求的 URI
	 * @param method HTTP 请求的方法
	 */
	public OkHttp3ClientHttpRequest(OkHttpClient client, URI uri, HttpMethod method) {
		this.client = client;
		this.uri = uri;
		this.method = method;
	}

	/**
	 * 获取 HTTP 请求的方法。
	 *
	 * @return HTTP 请求的方法
	 */
	@Override
	public HttpMethod getMethod() {
		return this.method;
	}

	/**
	 * 获取 HTTP 请求的方法值。
	 *
	 * @return HTTP 请求的方法值
	 */
	@Override
	public String getMethodValue() {
		return this.method.name();
	}

	/**
	 * 获取 HTTP 请求的 URI。
	 *
	 * @return HTTP 请求的 URI
	 */
	@Override
	public URI getURI() {
		return this.uri;
	}

	/**
	 * 执行 HTTP 请求的内部实现，将给定的头部和内容写入到请求中，并处理响应。
	 *
	 * @param headers HTTP 请求头
	 * @param content 请求体的字节数组内容
	 * @return 执行请求后的响应对象
	 * @throws IOException 如果发生 I/O 错误
	 */
	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] content) throws IOException {
		// 构建一个请求对象，使用请求头部信息、内容、URI和方法
		Request request = OkHttp3ClientHttpRequestFactory.buildRequest(headers, content, this.uri, this.method);

		// 执行该请求并获取响应，将其封装在OkHttp3ClientHttpResponse对象中返回
		return new OkHttp3ClientHttpResponse(this.client.newCall(request).execute());
	}

}
