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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * 基于 Apache HttpComponents HttpClient 的流式模式的 {@link ClientHttpRequest} 实现。
 *
 * <p>通过 {@link HttpComponentsClientHttpRequestFactory} 创建。
 *
 * @author Arjen Poutsma
 * @see HttpComponentsClientHttpRequestFactory#createRequest(java.net.URI, org.springframework.http.HttpMethod)
 * @since 4.0
 */
final class HttpComponentsStreamingClientHttpRequest extends AbstractClientHttpRequest
		implements StreamingHttpOutputMessage {
	/**
	 * Http客户端
	 */
	private final HttpClient httpClient;

	/**
	 * Http URI 请求
	 */
	private final HttpUriRequest httpRequest;

	/**
	 * Http上下文
	 */
	private final HttpContext httpContext;

	/**
	 * 请求体
	 */
	@Nullable
	private Body body;


	HttpComponentsStreamingClientHttpRequest(HttpClient client, HttpUriRequest request, HttpContext context) {
		this.httpClient = client;
		this.httpRequest = request;
		this.httpContext = context;
	}


	@Override
	public String getMethodValue() {
		return this.httpRequest.getMethod();
	}

	@Override
	public URI getURI() {
		return this.httpRequest.getURI();
	}

	@Override
	public void setBody(Body body) {
		assertNotExecuted();
		this.body = body;
	}

	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		throw new UnsupportedOperationException("getBody not supported");
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
		// 将请求头添加到 http请求 对象中
		HttpComponentsClientHttpRequest.addHeaders(this.httpRequest, headers);

		// 如果 http请求 是 Http实体封闭请求 的实例，并且请求体不为空
		if (this.httpRequest instanceof HttpEntityEnclosingRequest && this.body != null) {
			// 将 http请求 强制转换为 Http实体封闭请求 类型
			HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) this.httpRequest;

			// 创建一个 流式传输Http实体 对象
			HttpEntity requestEntity = new StreamingHttpEntity(getHeaders(), this.body);

			// 将 请求实体 设置为 实体封闭请求 的实体
			entityEnclosingRequest.setEntity(requestEntity);
		}

		// 执行 http客户端 发送请求，并返回响应
		HttpResponse httpResponse = this.httpClient.execute(this.httpRequest, this.httpContext);

		// 将 http响应 包装为 Http Components客户端Http Response 对象，并返回
		return new HttpComponentsClientHttpResponse(httpResponse);
	}


	private static class StreamingHttpEntity implements HttpEntity {
		/**
		 * Http请求头
		 */
		private final HttpHeaders headers;

		/**
		 * 请求体
		 */
		private final StreamingHttpOutputMessage.Body body;

		public StreamingHttpEntity(HttpHeaders headers, StreamingHttpOutputMessage.Body body) {
			this.headers = headers;
			this.body = body;
		}

		@Override
		public boolean isRepeatable() {
			return false;
		}

		@Override
		public boolean isChunked() {
			return false;
		}

		@Override
		public long getContentLength() {
			return this.headers.getContentLength();
		}

		@Override
		@Nullable
		public Header getContentType() {
			// 获取媒体类型
			MediaType contentType = this.headers.getContentType();
			// 如果媒体类型不为空，则转换为基础头部；否则返回null
			return (contentType != null ? new BasicHeader("Content-Type", contentType.toString()) : null);
		}

		@Override
		@Nullable
		public Header getContentEncoding() {
			// 获取内容编码
			String contentEncoding = this.headers.getFirst("Content-Encoding");
			// 如果内容编码不为空，则转换为基础头部；否则返回null
			return (contentEncoding != null ? new BasicHeader("Content-Encoding", contentEncoding) : null);

		}

		@Override
		public InputStream getContent() throws IOException, IllegalStateException {
			throw new IllegalStateException("No content available");
		}

		@Override
		public void writeTo(OutputStream outputStream) throws IOException {
			this.body.writeTo(outputStream);
		}

		@Override
		public boolean isStreaming() {
			return true;
		}

		@Override
		@Deprecated
		public void consumeContent() throws IOException {
			throw new UnsupportedOperationException();
		}
	}

}
