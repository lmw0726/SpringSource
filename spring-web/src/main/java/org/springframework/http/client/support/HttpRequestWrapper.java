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

package org.springframework.http.client.support;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.net.URI;

/**
 * 提供了 {@link HttpRequest} 接口的方便实现，可以被覆盖以适应请求。
 *
 * <p>这些方法默认调用包装的请求对象。
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public class HttpRequestWrapper implements HttpRequest {
	/**
	 * Http请求
	 */
	private final HttpRequest request;


	/**
	 * 创建一个新的 {@code HttpRequestWrapper} 包装给定的请求对象。
	 *
	 * @param request 要包装的请求对象
	 */
	public HttpRequestWrapper(HttpRequest request) {
		Assert.notNull(request, "HttpRequest must not be null");
		this.request = request;
	}


	/**
	 * 返回包装的请求对象。
	 */
	public HttpRequest getRequest() {
		return this.request;
	}

	/**
	 * 返回包装请求的方法。
	 */
	@Override
	@Nullable
	public HttpMethod getMethod() {
		return this.request.getMethod();
	}

	/**
	 * 返回包装请求的方法值。
	 */
	@Override
	public String getMethodValue() {
		return this.request.getMethodValue();
	}

	/**
	 * 返回包装请求的URI。
	 */
	@Override
	public URI getURI() {
		return this.request.getURI();
	}

	/**
	 * 返回包装请求的头信息。
	 */
	@Override
	public HttpHeaders getHeaders() {
		return this.request.getHeaders();
	}

}
