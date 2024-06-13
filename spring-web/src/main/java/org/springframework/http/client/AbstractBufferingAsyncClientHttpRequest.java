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

import org.springframework.http.HttpHeaders;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 异步客户端请求的基础实现，通过在发送请求之前将输出缓冲到字节数组中。
 *
 * <p>继承自 {@link AbstractAsyncClientHttpRequest}。
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @deprecated 自 Spring 5.0 起，没有直接的替代方案
 */
@Deprecated
abstract class AbstractBufferingAsyncClientHttpRequest extends AbstractAsyncClientHttpRequest {

	/**
	 * 缓冲输出内容的字节数组输出流。初始大小为 1024 字节。
	 */
	private ByteArrayOutputStream bufferedOutput = new ByteArrayOutputStream(1024);

	/**
	 * 获取请求体的输出流，内部使用字节数组输出流。
	 *
	 * @param headers HTTP 头信息
	 * @return 输出流，用于写入请求体内容
	 * @throws IOException 如果发生 I/O 错误
	 */
	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		return this.bufferedOutput;
	}

	/**
	 * 执行 HTTP 请求的内部实现，将缓冲的输出内容发送到服务器。
	 *
	 * @param headers HTTP 头信息
	 * @return 执行请求后的响应对象
	 * @throws IOException 如果发生 I/O 错误
	 */
	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers) throws IOException {
		// 将 缓冲输出 转换为 byte 数组
		byte[] bytes = this.bufferedOutput.toByteArray();

		// 如果 请求头 的内容长度小于0，设置内容长度为 bytes 的长度
		if (headers.getContentLength() < 0) {
			headers.setContentLength(bytes.length);
		}

		// 调用 执行内部 方法执行请求
		ListenableFuture<ClientHttpResponse> result = executeInternal(headers, bytes);

		// 重新初始化 缓冲输出，创建一个新的空的 ByteArrayOutputStream
		this.bufferedOutput = new ByteArrayOutputStream(0);

		// 返回执行结果的 ListenableFuture<ClientHttpResponse>
		return result;
	}

	/**
	 * 抽象模板方法，用于将给定的头信息和内容写入 HTTP 请求。
	 *
	 * @param headers        HTTP 头信息
	 * @param bufferedOutput 请求体内容的字节数组
	 * @return 执行请求后的响应对象
	 * @throws IOException 如果发生 I/O 错误
	 */
	protected abstract ListenableFuture<ClientHttpResponse> executeInternal(
			HttpHeaders headers, byte[] bufferedOutput) throws IOException;

}
