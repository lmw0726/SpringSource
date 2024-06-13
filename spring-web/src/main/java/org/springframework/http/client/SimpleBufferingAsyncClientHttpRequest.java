/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.http.HttpMethod;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 使用标准 JDK 设施执行缓冲请求的 {@link org.springframework.http.client.ClientHttpRequest} 实现。
 * 通过 {@link org.springframework.http.client.SimpleClientHttpRequestFactory} 创建。
 *
 * @author Arjen Poutsma
 * @see org.springframework.http.client.SimpleClientHttpRequestFactory#createRequest
 * @since 3.0
 * @deprecated 自 Spring 5.0 起，没有直接的替代方案
 */
@Deprecated
final class SimpleBufferingAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

	/**
	 * HTTP 连接对象，用于执行 HTTP 请求。
	 */
	private final HttpURLConnection connection;

	/**
	 * 指示是否启用输出流模式。
	 */
	private final boolean outputStreaming;

	/**
	 * 异步可监听的任务执行器，用于提交请求任务。
	 */
	private final AsyncListenableTaskExecutor taskExecutor;


	SimpleBufferingAsyncClientHttpRequest(HttpURLConnection connection,
										  boolean outputStreaming, AsyncListenableTaskExecutor taskExecutor) {

		this.connection = connection;
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
	protected ListenableFuture<ClientHttpResponse> executeInternal(
			HttpHeaders headers, byte[] bufferedOutput) throws IOException {

		return this.taskExecutor.submitListenable(() -> {
			// 添加请求头到 连接 对象中
			SimpleBufferingClientHttpRequest.addHeaders(this.connection, headers);

			// 对于 HTTP DELETE 方法且 缓冲输出 长度为0的情况，设置不使用输出流
			if (getMethod() == HttpMethod.DELETE && bufferedOutput.length == 0) {
				this.connection.setDoOutput(false);
			}

			// 如果 连接 设置了输出流且启用了输出流模式
			if (this.connection.getDoOutput() && this.outputStreaming) {
				// 设置固定长度的流模式，长度为 缓冲输出 的长度
				this.connection.setFixedLengthStreamingMode(bufferedOutput.length);
			}

			// 建立连接
			this.connection.connect();

			// 如果 连接 设置了输出流
			if (this.connection.getDoOutput()) {
				// 将 缓冲输出 的内容复制到 连接 的输出流中
				FileCopyUtils.copy(bufferedOutput, this.connection.getOutputStream());
			} else {
				// 在没有输出的情况下立即触发请求
				this.connection.getResponseCode();
			}

			// 返回一个新的 SimpleClientHttpResponse 对象
			return new SimpleClientHttpResponse(this.connection);
		});
	}

}
