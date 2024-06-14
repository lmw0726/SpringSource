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

package org.springframework.http.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

/**
 * {@link ClientHttpRequest} 的包装器，支持 {@link ClientHttpRequestInterceptor} 拦截器。
 *
 * <p>该类提供了在执行 HTTP 请求之前或之后进行自定义处理的能力，通过一系列拦截器对请求和响应进行处理。
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
class InterceptingClientHttpRequest extends AbstractBufferingClientHttpRequest {

	/**
	 * 用于创建 HTTP 请求的工厂。
	 */
	private final ClientHttpRequestFactory requestFactory;

	/**
	 * 请求拦截器列表。
	 */
	private final List<ClientHttpRequestInterceptor> interceptors;

	/**
	 * HTTP 请求的方法。
	 */
	private HttpMethod method;

	/**
	 * HTTP 请求的 URI。
	 */
	private URI uri;

	/**
	 * 构造一个 {@code InterceptingClientHttpRequest} 实例。
	 *
	 * @param requestFactory 用于创建请求的工厂
	 * @param interceptors   请求拦截器列表
	 * @param uri            HTTP 请求的 URI
	 * @param method         HTTP 请求的方法
	 */
	protected InterceptingClientHttpRequest(ClientHttpRequestFactory requestFactory,
											List<ClientHttpRequestInterceptor> interceptors, URI uri, HttpMethod method) {
		this.requestFactory = requestFactory;
		this.interceptors = interceptors;
		this.method = method;
		this.uri = uri;
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
	 * 执行 HTTP 请求的内部实现，通过拦截器链处理请求和响应。
	 *
	 * @param headers        HTTP 请求头
	 * @param bufferedOutput 请求体的字节数组内容
	 * @return 执行请求后的响应对象
	 * @throws IOException 如果发生 I/O 错误
	 */
	@Override
	protected final ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		// 创建一个拦截请求执行对象
		InterceptingRequestExecution requestExecution = new InterceptingRequestExecution();

		// 执行请求并返回结果，传递当前对象和缓冲输出数据
		return requestExecution.execute(this, bufferedOutput);
	}

	/**
	 * 请求执行的内部类，处理拦截器链。
	 */
	private class InterceptingRequestExecution implements ClientHttpRequestExecution {

		/**
		 * 拦截器的迭代器。
		 */
		private final Iterator<ClientHttpRequestInterceptor> iterator;

		/**
		 * 构造一个 {@code InterceptingRequestExecution} 实例。
		 */
		public InterceptingRequestExecution() {
			this.iterator = interceptors.iterator();
		}

		/**
		 * 执行请求，处理拦截器链。
		 *
		 * @param request 请求对象
		 * @param body    请求体
		 * @return 请求的响应对象
		 * @throws IOException 如果发生 I/O 错误
		 */
		@Override
		public ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException {
			// 如果还有下一个拦截器
			if (this.iterator.hasNext()) {
				// 获取下一个拦截器
				ClientHttpRequestInterceptor nextInterceptor = this.iterator.next();
				// 调用下一个拦截器的拦截方法，并传递请求、请求体和当前对象
				return nextInterceptor.intercept(request, body, this);
			} else {
				// 获取请求的HTTP方法
				HttpMethod method = request.getMethod();
				// 断言确保HTTP方法不为空
				Assert.state(method != null, "No standard HTTP method");
				// 创建一个客户端请求对象
				ClientHttpRequest delegate = requestFactory.createRequest(request.getURI(), method);
				// 将请求的所有头部信息添加到代理请求对象中
				request.getHeaders().forEach((key, value) -> delegate.getHeaders().addAll(key, value));

				// 如果请求体长度大于0
				if (body.length > 0) {
					// 如果代理请求对象是StreamingHttpOutputMessage的实例
					if (delegate instanceof StreamingHttpOutputMessage) {
						// 将请求体数据设置到流式输出消息中
						StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) delegate;
						streamingOutputMessage.setBody(outputStream -> StreamUtils.copy(body, outputStream));
					} else {
						// 将请求体数据直接拷贝到代理请求对象的输出流中
						StreamUtils.copy(body, delegate.getBody());
					}
				}

				// 执行代理请求并返回响应
				return delegate.execute();
			}
		}
	}

}
