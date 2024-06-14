/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.mock.http.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link ClientHttpResponse} 的模拟实现。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MockClientHttpResponse extends MockHttpInputMessage implements ClientHttpResponse {
	/**
	 * 状态码
	 */
	private final int statusCode;


	/**
	 * 使用字节数组作为响应主体的构造函数。
	 */
	public MockClientHttpResponse(byte[] body, HttpStatus statusCode) {
		super(body);
		Assert.notNull(statusCode, "HttpStatus is required");
		this.statusCode = statusCode.value();
	}

	/**
	 * {@link #MockClientHttpResponse(byte[], HttpStatus)} 的变体，带有自定义的 HTTP 状态码。
	 *
	 * @since 5.3.17
	 */
	public MockClientHttpResponse(byte[] body, int statusCode) {
		super(body);
		this.statusCode = statusCode;
	}

	/**
	 * 使用 InputStream 作为响应主体的构造函数。
	 */
	public MockClientHttpResponse(InputStream body, HttpStatus statusCode) {
		super(body);
		Assert.notNull(statusCode, "HttpStatus is required");
		this.statusCode = statusCode.value();
	}

	/**
	 * {@link #MockClientHttpResponse(InputStream, HttpStatus)} 的变体，带有自定义的 HTTP 状态码。
	 *
	 * @since 5.3.17
	 */
	public MockClientHttpResponse(InputStream body, int statusCode) {
		super(body);
		this.statusCode = statusCode;
	}


	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(this.statusCode);
	}

	@Override
	public int getRawStatusCode() {
		return this.statusCode;
	}

	@Override
	public String getStatusText() {
		// 根据当前对象的状态码解析为HttpStatus对象
		HttpStatus status = HttpStatus.resolve(this.statusCode);

		// 如果解析出的HttpStatus对象不为空，则返回其对应的原因短语；
		// 否则返回空字符串
		return (status != null ? status.getReasonPhrase() : "");
	}

	@Override
	public void close() {
		try {
			getBody().close();
		} catch (IOException ex) {
			// 忽略
		}
	}

}
