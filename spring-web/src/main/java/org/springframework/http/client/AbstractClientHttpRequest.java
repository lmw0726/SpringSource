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

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link ClientHttpRequest} 的抽象基类，确保头信息和主体不会被多次写入。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public abstract class AbstractClientHttpRequest implements ClientHttpRequest {
	/**
	 * 请求头
	 */
	private final HttpHeaders headers = new HttpHeaders();

	/**
	 * 是否已经执行
	 */
	private boolean executed = false;

	/**
	 * 是否只读请求头
	 */
	@Nullable
	private HttpHeaders readOnlyHeaders;


	@Override
	public final HttpHeaders getHeaders() {
		// 如果 是否只读请求头 不为 null，则直接返回 是否只读请求头
		if (this.readOnlyHeaders != null) {
			return this.readOnlyHeaders;
		} else if (this.executed) {
			// 否则，如果请求已经执行过，则创建一个只读的 HttpHeaders 对象，并返回它
			this.readOnlyHeaders = HttpHeaders.readOnlyHttpHeaders(this.headers);
			return this.readOnlyHeaders;
		} else {
			// 否则，返回可变的 请求头 对象
			return this.headers;
		}
	}

	@Override
	public final OutputStream getBody() throws IOException {
		// 确保请求尚未执行
		assertNotExecuted();

		// 返回基于 请求头 的内部请求体
		return getBodyInternal(this.headers);
	}

	@Override
	public final ClientHttpResponse execute() throws IOException {
		// 确保请求尚未执行
		assertNotExecuted();

		// 执行内部请求，传入 请求头，获取响应结果
		ClientHttpResponse result = executeInternal(this.headers);

		// 标记请求已执行
		this.executed = true;

		// 返回响应结果
		return result;
	}

	/**
	 * 断言此请求尚未被 {@linkplain #execute() 执行}。
	 *
	 * @throws IllegalStateException 如果此请求已被执行
	 */
	protected void assertNotExecuted() {
		Assert.state(!this.executed, "ClientHttpRequest already executed");
	}


	/**
	 * 返回主体的抽象模板方法。
	 *
	 * @param headers HTTP 头信息
	 * @return 主体输出流
	 */
	protected abstract OutputStream getBodyInternal(HttpHeaders headers) throws IOException;

	/**
	 * 将给定的头信息和内容写入 HTTP 请求的抽象模板方法。
	 *
	 * @param headers HTTP 头信息
	 * @return 执行请求后的响应对象
	 */
	protected abstract ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException;

}
