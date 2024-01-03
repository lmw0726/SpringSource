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

package org.springframework.web.reactive.function.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;

/**
 * 当接收到未知（或自定义）HTTP状态码时抛出的异常。
 *
 * @author Brian Clozel
 * @since 5.1
 */
public class UnknownHttpStatusCodeException extends WebClientResponseException {

	private static final long serialVersionUID = 2407169540168185007L;


	/**
	 * 使用给定的参数创建 {@code UnknownHttpStatusCodeException} 的新实例。
	 *
	 * @param statusCode      HTTP状态码
	 * @param headers         HTTP响应头
	 * @param responseBody    响应体字节数组
	 * @param responseCharset 响应字符集编码
	 */
	public UnknownHttpStatusCodeException(
			int statusCode, HttpHeaders headers, byte[] responseBody, Charset responseCharset) {

		super("Unknown status code [" + statusCode + "]", statusCode, "",
				headers, responseBody, responseCharset);
	}

	/**
	 * 使用给定的参数创建 {@code UnknownHttpStatusCodeException} 的新实例。
	 *
	 * @param statusCode      HTTP状态码
	 * @param headers         HTTP响应头
	 * @param responseBody    响应体字节数组
	 * @param responseCharset 响应字符集编码
	 * @param request         HTTP请求
	 * @since 5.1.4
	 */
	public UnknownHttpStatusCodeException(
			int statusCode, HttpHeaders headers, byte[] responseBody, @Nullable Charset responseCharset,
			@Nullable HttpRequest request) {

		super("Unknown status code [" + statusCode + "]", statusCode, "",
				headers, responseBody, responseCharset, request);
	}

}
