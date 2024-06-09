/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;

/**
 * 在接收到未知（或自定义）HTTP状态代码时抛出的异常。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class UnknownHttpStatusCodeException extends RestClientResponseException {

	private static final long serialVersionUID = 7103980251635005491L;


	/**
	 * 基于{@link HttpStatus}、状态文本和响应体内容构造{@code HttpStatusCodeException}的新实例。
	 *
	 * @param rawStatusCode   原始状态码值
	 * @param statusText      状态文本
	 * @param responseHeaders 响应头（可能为{@code null}）
	 * @param responseBody    响应体内容（可能为{@code null}）
	 * @param responseCharset 响应体字符集（可能为{@code null}）
	 */
	public UnknownHttpStatusCodeException(int rawStatusCode, String statusText, @Nullable HttpHeaders responseHeaders,
										  @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		this("Unknown status code [" + rawStatusCode + "]" + " " + statusText,
				rawStatusCode, statusText, responseHeaders, responseBody, responseCharset);
	}

	/**
	 * 基于{@link HttpStatus}、状态文本和响应体内容构造{@code HttpStatusCodeException}的新实例。
	 *
	 * @param rawStatusCode   原始状态码值
	 * @param statusText      状态文本
	 * @param responseHeaders 响应头（可能为{@code null}）
	 * @param responseBody    响应体内容（可能为{@code null}）
	 * @param responseCharset 响应体字符集（可能为{@code null}）
	 * @since 5.2.2
	 */
	public UnknownHttpStatusCodeException(String message, int rawStatusCode, String statusText,
										  @Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		super(message, rawStatusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
