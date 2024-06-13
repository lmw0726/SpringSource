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

package org.springframework.http.client;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 使用标准Java设施执行流式请求的{@link org.springframework.http.client.ClientHttpRequest}实现。
 * 通过{@link org.springframework.http.client.SimpleClientHttpRequestFactory}创建。
 *
 * <p>此类通过标准Java设施执行流式请求，对于较大的请求体特别有效。
 *
 * <p>已废弃，自Spring 5.0起，没有直接的替代方案。
 *
 * @author Arjen Poutsma
 * @see org.springframework.http.client.SimpleClientHttpRequestFactory#createRequest
 * @see org.springframework.http.client.support.AsyncHttpAccessor
 * @see org.springframework.web.client.AsyncRestTemplate
 * @since 3.0
 * @deprecated 自Spring 5.0起，没有直接的替代方案
 */
@Deprecated
final class SimpleStreamingAsyncClientHttpRequest extends AbstractAsyncClientHttpRequest {

	/**
	 * 使用的HTTP连接。
	 */
	private final HttpURLConnection connection;

	/**
	 * 块大小，用于分块流传输。
	 */
	private final int chunkSize;

	/**
	 * 请求体的输出流，如果输出流式传输可用。
	 */
	@Nullable
	private OutputStream body;

	/**
	 * 是否输出流式传输。
	 */
	private final boolean outputStreaming;

	/**
	 * 异步任务执行器。
	 */
	private final AsyncListenableTaskExecutor taskExecutor;


	SimpleStreamingAsyncClientHttpRequest(HttpURLConnection connection, int chunkSize,
										  boolean outputStreaming, AsyncListenableTaskExecutor taskExecutor) {

		this.connection = connection;
		this.chunkSize = chunkSize;
		this.outputStreaming = outputStreaming;
		this.taskExecutor = taskExecutor;
	}


	@Override
	public String getMethodValue() {
		return this.connection.getRequestMethod();
	}

	@Override
	public URI getURI() {
		try {
			// 获取 HttpURLConnection 的 URL，并将其转换为 URI 返回
			return this.connection.getURL().toURI();
		} catch (URISyntaxException ex) {
			// 如果发生 URISyntaxException 异常，抛出 IllegalStateException 异常
			throw new IllegalStateException("Could not get HttpURLConnection URI: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		// 如果 请求体的输出流 为 null
		if (this.body == null) {
			// 如果启用了输出流模式
			if (this.outputStreaming) {
				// 获取内容长度
				long contentLength = headers.getContentLength();
				// 如果内容长度大于等于0，设置固定长度的流模式
				if (contentLength >= 0) {
					this.connection.setFixedLengthStreamingMode(contentLength);
				} else {
					// 否则，设置分块流模式，使用指定的块大小
					this.connection.setChunkedStreamingMode(this.chunkSize);
				}
			}

			// 添加请求头到 HTTP连接 对象中
			SimpleBufferingClientHttpRequest.addHeaders(this.connection, headers);

			// 建立连接
			this.connection.connect();

			// 获取 HTTP连接 的输出流，并赋值给 请求体的输出流
			this.body = this.connection.getOutputStream();
		}

		// 返回一个非关闭的输出流对象
		return StreamUtils.nonClosing(this.body);
	}

	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers) throws IOException {
		return this.taskExecutor.submitListenable(() -> {
			try {
				// 如果 请求体的输出流 不为 null，关闭 请求体的输出流 输出流
				if (this.body != null) {
					this.body.close();
				} else {
					// 否则，添加请求头到 HTTP连接 对象中
					SimpleBufferingClientHttpRequest.addHeaders(this.connection, headers);
					// 建立连接
					this.connection.connect();
					// 立即触发请求，即使没有输出的情况下也执行
					this.connection.getResponseCode();
				}
			} catch (IOException ex) {
				// 发生 IOException 时，忽略异常
			}
			// 返回一个新的 SimpleClientHttpResponse 对象
			return new SimpleClientHttpResponse(this.connection);
		});

	}

}
