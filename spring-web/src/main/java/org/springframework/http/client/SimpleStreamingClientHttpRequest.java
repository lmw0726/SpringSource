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
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 使用标准 JDK 功能执行流式请求的 {@link ClientHttpRequest} 实现。
 * 通过 {@link SimpleClientHttpRequestFactory} 创建。
 *
 * @author Arjen Poutsma
 * @see SimpleClientHttpRequestFactory#createRequest(java.net.URI, HttpMethod)
 * @see org.springframework.http.client.support.HttpAccessor
 * @see org.springframework.web.client.RestTemplate
 * @since 3.0
 */
final class SimpleStreamingClientHttpRequest extends AbstractClientHttpRequest {


	/**
	 * HTTP 连接实例，用于发送请求和接收响应。
	 */
	private final HttpURLConnection connection;

	/**
	 * 用于分块传输的块大小。
	 */
	private final int chunkSize;

	/**
	 * 请求的输出流。
	 */
	@Nullable
	private OutputStream body;

	/**
	 * 标识是否进行流式输出。
	 */
	private final boolean outputStreaming;


	SimpleStreamingClientHttpRequest(HttpURLConnection connection, int chunkSize, boolean outputStreaming) {
		this.connection = connection;
		this.chunkSize = chunkSize;
		this.outputStreaming = outputStreaming;
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
			// 如果发生 URISyntaxException 异常，抛出 IllegalStateException 异常，并包含详细的错误信息
			throw new IllegalStateException("Could not get HttpURLConnection URI: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		// 如果 请求体 为空
		if (this.body == null) {
			// 如果启用了输出流模式
			if (this.outputStreaming) {
				// 获取 内容长度
				long contentLength = headers.getContentLength();
				// 如果 内容长度 大于等于0，设置固定长度的流模式
				if (contentLength >= 0) {
					this.connection.setFixedLengthStreamingMode(contentLength);
				} else {
					// 否则，设置分块流模式，使用指定的块大小
					this.connection.setChunkedStreamingMode(this.chunkSize);
				}
			}

			// 添加 请求头 到 连接 对象中
			SimpleBufferingClientHttpRequest.addHeaders(this.connection, headers);

			// 建立连接
			this.connection.connect();

			// 获取 连接 的输出流，并赋值给 请求体
			this.body = this.connection.getOutputStream();
		}

		// 返回一个非关闭的输出流对象，包装当前的 请求体
		return StreamUtils.nonClosing(this.body);
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
		try {
			// 如果 请求体 不为空
			if (this.body != null) {
				// 关闭 请求体 输出流
				this.body.close();
			} else {
				// 否则，添加 请求头 到 连接 对象中
				SimpleBufferingClientHttpRequest.addHeaders(this.connection, headers);
				// 建立连接
				this.connection.connect();
				// 立即触发请求，即使在没有输出的情况下也执行
				this.connection.getResponseCode();
			}
		} catch (IOException ex) {
			// 捕获并忽略 IOException 异常
		}

		// 返回一个新的 SimpleClientHttpResponse 对象
		return new SimpleClientHttpResponse(this.connection);
	}

}
