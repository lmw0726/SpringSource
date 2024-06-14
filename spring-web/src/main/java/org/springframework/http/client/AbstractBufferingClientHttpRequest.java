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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link ClientHttpRequest} 的基础实现，用于在发送到网络之前将输出缓冲到字节数组中。
 *
 * @author Arjen Poutsma
 * @since 3.0.6
 */
abstract class AbstractBufferingClientHttpRequest extends AbstractClientHttpRequest {

	/**
	 * 缓冲输出内容的字节数组流，初始容量为 1024 字节。
	 */
	private ByteArrayOutputStream bufferedOutput = new ByteArrayOutputStream(1024);

	/**
	 * 返回用于写入请求体的输出流。
	 *
	 * @param headers HTTP 请求头
	 * @return 请求体的输出流
	 * @throws IOException 如果发生 I/O 错误
	 */
	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		return this.bufferedOutput;
	}

	/**
	 * 执行 HTTP 请求的内部实现，将给定的头部和内容写入到请求中。
	 *
	 * @param headers HTTP 请求头
	 * @return 执行请求后的响应对象
	 * @throws IOException 如果发生 I/O 错误
	 */
	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
		// 将 缓冲输出流 转换为 byte 数组
		byte[] bytes = this.bufferedOutput.toByteArray();

		// 如果 头部 的内容长度小于0，设置内容长度为 bytes 的长度
		if (headers.getContentLength() < 0) {
			headers.setContentLength(bytes.length);
		}

		// 执行 内部请求，传入 头部 和 bytes，获取响应结果
		ClientHttpResponse result = executeInternal(headers, bytes);

		// 重新初始化 缓冲输出流，创建一个新的空的 ByteArrayOutputStream
		this.bufferedOutput = new ByteArrayOutputStream(0);

		// 返回响应结果
		return result;
	}

	/**
	 * 抽象模板方法，将给定的头部和内容写入 HTTP 请求中。
	 *
	 * @param headers        HTTP 请求头
	 * @param bufferedOutput 请求体的字节数组内容
	 * @return 执行请求后的响应对象
	 * @throws IOException 如果发生 I/O 错误
	 */
	protected abstract ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput)
			throws IOException;


}
