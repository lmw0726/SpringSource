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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

/**
 * {@link AsyncClientHttpRequest} 的包装器，通过调用注册的拦截器来增强实际请求执行过程。
 *
 * @author Jakub Narloch
 * @author Rossen Stoyanchev
 * @see InterceptingAsyncClientHttpRequestFactory
 * @deprecated 自 Spring 5.0 起，没有直接的替代方案
 */
@Deprecated
class InterceptingAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

	/**
	 * 异步请求工厂，用于创建 {@link AsyncClientHttpRequest} 的实例。
	 */
	private AsyncClientHttpRequestFactory requestFactory;

	/**
	 * 异步客户端请求拦截器列表，用于拦截和处理请求。
	 */
	private List<AsyncClientHttpRequestInterceptor> interceptors;

	/**
	 * 请求的统一资源标识符 (URI)。
	 */
	private URI uri;

	/**
	 * HTTP 请求方法。
	 */
	private HttpMethod httpMethod;


	/**
	 * 创建 {@link InterceptingAsyncClientHttpRequest} 的新实例。
	 *
	 * @param requestFactory 异步请求工厂
	 * @param interceptors   拦截器列表
	 * @param uri            请求的URI
	 * @param httpMethod     HTTP 方法
	 */
	public InterceptingAsyncClientHttpRequest(AsyncClientHttpRequestFactory requestFactory,
											  List<AsyncClientHttpRequestInterceptor> interceptors, URI uri, HttpMethod httpMethod) {

		this.requestFactory = requestFactory;
		this.interceptors = interceptors;
		this.uri = uri;
		this.httpMethod = httpMethod;
	}


	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers, byte[] body)
			throws IOException {

		return new AsyncRequestExecution().executeAsync(this, body);
	}

	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	@Override
	public String getMethodValue() {
		return this.httpMethod.name();
	}

	@Override
	public URI getURI() {
		return this.uri;
	}


	private class AsyncRequestExecution implements AsyncClientHttpRequestExecution {
		/**
		 * 异步客户端Http请求拦截器 迭代器
		 */
		private Iterator<AsyncClientHttpRequestInterceptor> iterator;

		public AsyncRequestExecution() {
			this.iterator = interceptors.iterator();
		}

		@Override
		public ListenableFuture<ClientHttpResponse> executeAsync(HttpRequest request, byte[] body)
				throws IOException {

			// 如果仍有下一个拦截器
			if (this.iterator.hasNext()) {
				// 获取下一个拦截器并调用其拦截方法
				AsyncClientHttpRequestInterceptor interceptor = this.iterator.next();
				return interceptor.intercept(request, body, this);
			} else {
				// 否则，获取请求的URI、方法和头部
				URI uri = request.getURI();
				HttpMethod method = request.getMethod();
				HttpHeaders headers = request.getHeaders();

				// 确保HTTP方法不为空
				Assert.state(method != null, "No standard HTTP method");

				// 使用请求工厂创建异步请求
				AsyncClientHttpRequest delegate = requestFactory.createAsyncRequest(uri, method);
				// 将请求的头部复制到委托请求的头部
				delegate.getHeaders().putAll(headers);
				// 如果请求体长度大于0，则复制请求体到委托请求的输出流中
				if (body.length > 0) {
					StreamUtils.copy(body, delegate.getBody());
				}

				// 执行委托请求并返回结果
				return delegate.executeAsync();
			}
		}
	}

}
