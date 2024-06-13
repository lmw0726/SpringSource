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
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link AsyncClientHttpRequest} 的抽象基类，确保头部和请求体不会被多次写入。
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @deprecated 自 Spring 5.0 起，推荐使用 {@link org.springframework.http.client.reactive.AbstractClientHttpRequest}
 */
@Deprecated
abstract class AbstractAsyncClientHttpRequest implements AsyncClientHttpRequest {
	/**
	 * 请求头
	 */
	private final HttpHeaders headers = new HttpHeaders();

	/**
	 * 是否已经执行
	 */
	private boolean executed = false;


	@Override
	public final HttpHeaders getHeaders() {
		return (this.executed ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	@Override
	public final OutputStream getBody() throws IOException {
		// 断言未执行过
		assertNotExecuted();
		// 返回使用当前请求头获取的内部请求体
		return getBodyInternal(this.headers);
	}

	@Override
	public ListenableFuture<ClientHttpResponse> executeAsync() throws IOException {
		// 断言未执行过
		assertNotExecuted();
		// 执行内部请求，并获取返回的可监听的Future对象
		ListenableFuture<ClientHttpResponse> result = executeInternal(this.headers);
		// 设置执行标志为true
		this.executed = true;
		// 返回执行结果
		return result;
	}

	/**
	 * 断言此请求尚未 {@linkplain #executeAsync() 执行}。
	 *
	 * @throws IllegalStateException 如果此请求已执行
	 */
	protected void assertNotExecuted() {
		Assert.state(!this.executed, "ClientHttpRequest already executed");
	}


	/**
	 * 抽象模板方法，返回请求体输出流。
	 *
	 * @param headers HTTP 头部信息
	 * @return 请求体输出流
	 */
	protected abstract OutputStream getBodyInternal(HttpHeaders headers) throws IOException;

	/**
	 * 抽象模板方法，将给定的头部和内容写入 HTTP 请求。
	 *
	 * @param headers HTTP 头部信息
	 * @return 执行请求的响应对象
	 */
	protected abstract ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers)
			throws IOException;

}
