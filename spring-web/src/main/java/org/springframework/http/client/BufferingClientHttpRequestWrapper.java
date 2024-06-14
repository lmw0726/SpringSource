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
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URI;

/**
 * {@link ClientHttpRequest} 的简单实现，包装另一个请求。
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
final class BufferingClientHttpRequestWrapper extends AbstractBufferingClientHttpRequest {

	/**
	 * 被包装的原始请求。
	 */
	private final ClientHttpRequest request;

	/**
	 * 构造一个 {@code BufferingClientHttpRequestWrapper} 实例。
	 *
	 * @param request 被包装的原始请求
	 */
	BufferingClientHttpRequestWrapper(ClientHttpRequest request) {
		this.request = request;
	}

	/**
	 * 获取 HTTP 请求的方法。
	 *
	 * @return HTTP 请求的方法，可能为 {@code null}
	 */
	@Nullable
	@Override
	public HttpMethod getMethod() {
		return this.request.getMethod();
	}

	/**
	 * 获取 HTTP 请求的方法值。
	 *
	 * @return HTTP 请求的方法值
	 */
	@Override
	public String getMethodValue() {
		return this.request.getMethodValue();
	}

	/**
	 * 获取 HTTP 请求的 URI。
	 *
	 * @return HTTP 请求的 URI
	 */
	@Override
	public URI getURI() {
		return this.request.getURI();
	}

	/**
	 * 执行 HTTP 请求的内部实现，将给定的头部和内容写入到请求中，并处理响应。
	 *
	 * @param headers        HTTP 请求头
	 * @param bufferedOutput 请求体的字节数组内容
	 * @return 执行请求后的响应对象
	 * @throws IOException 如果发生 I/O 错误
	 */
	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		// 将传入的 头部 添加到当前请求的 头部 中
		this.request.getHeaders().putAll(headers);

		// 将 缓冲输出流 中的数据复制到请求的 主体 中
		StreamUtils.copy(bufferedOutput, this.request.getBody());

		// 执行当前请求，并获取响应
		ClientHttpResponse response = this.request.execute();

		// 返回一个包装了响应的 BufferingClientHttpResponseWrapper 对象
		return new BufferingClientHttpResponseWrapper(response);
	}

}
