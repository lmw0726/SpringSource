/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.URI;

/**
 * 包含实际 HTTP 请求数据的异常。
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
public class WebClientRequestException extends WebClientException {

	private static final long serialVersionUID = -5139991985321385005L;

	/**
	 * HTTP请求的方法。
	 */
	private final HttpMethod method;

	/**
	 * HTTP请求的URI。
	 */
	private final URI uri;

	/**
	 * HTTP请求的头信息。
	 */
	private final HttpHeaders headers;


	/**
	 * 使用异常、HTTP方法、URI和HTTP头构造一个新的 {@code WebClientRequestException} 实例。
	 *
	 * @param ex      异常
	 * @param method  HTTP方法
	 * @param uri     请求URI
	 * @param headers HTTP头信息
	 */
	public WebClientRequestException(Throwable ex, HttpMethod method, URI uri, HttpHeaders headers) {
		super(ex.getMessage(), ex);

		this.method = method;
		this.uri = uri;
		this.headers = headers;
	}

	/**
	 * 返回HTTP请求方法。
	 *
	 * @return HTTP请求方法
	 */
	public HttpMethod getMethod() {
		return this.method;
	}

	/**
	 * 返回请求URI。
	 *
	 * @return 请求URI
	 */
	public URI getUri() {
		return this.uri;
	}

	/**
	 * 返回HTTP请求头。
	 *
	 * @return HTTP请求头
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

}
