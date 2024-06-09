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
import org.springframework.lang.Nullable;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 包含实际HTTP响应数据的异常的通用基类。
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public class RestClientResponseException extends RestClientException {

	private static final long serialVersionUID = -8803556342728481792L;

	/**
	 * 默认字符集
	 */
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * 原始状态码值
	 */
	private final int rawStatusCode;

	/**
	 * 状态码文本
	 */
	private final String statusText;

	/**
	 * 响应体字节数组
	 */
	private final byte[] responseBody;

	/**
	 * 响应头
	 */
	@Nullable
	private final HttpHeaders responseHeaders;

	/**
	 * 响应字符集
	 */
	@Nullable
	private final String responseCharset;


	/**
	 * 使用给定的响应数据构造一个新实例。
	 *
	 * @param statusCode      原始状态码值
	 * @param statusText      状态文本
	 * @param responseHeaders 响应头（可能为{@code null}）
	 * @param responseBody    响应体内容（可能为{@code null}）
	 * @param responseCharset 响应体字符集（可能为{@code null}）
	 */
	public RestClientResponseException(String message, int statusCode, String statusText,
									   @Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		super(message);
		this.rawStatusCode = statusCode;
		this.statusText = statusText;
		this.responseHeaders = responseHeaders;
		this.responseBody = (responseBody != null ? responseBody : new byte[0]);
		this.responseCharset = (responseCharset != null ? responseCharset.name() : null);
	}


	/**
	 * 返回原始HTTP状态码值。
	 */
	public int getRawStatusCode() {
		return this.rawStatusCode;
	}

	/**
	 * 返回HTTP状态文本。
	 */
	public String getStatusText() {
		return this.statusText;
	}

	/**
	 * 返回HTTP响应头。
	 */
	@Nullable
	public HttpHeaders getResponseHeaders() {
		return this.responseHeaders;
	}

	/**
	 * 返回响应体作为字节数组。
	 */
	public byte[] getResponseBodyAsByteArray() {
		return this.responseBody;
	}

	/**
	 * 返回响应体转换为字符串。所使用的字符集为响应“Content-Type”的字符集，否则为{@code "UTF-8"}。
	 */
	public String getResponseBodyAsString() {
		return getResponseBodyAsString(DEFAULT_CHARSET);
	}

	/**
	 * 返回响应体转换为字符串。所使用的字符集为响应“Content-Type”的字符集，否则为给定的字符集。
	 *
	 * @param fallbackCharset 如果响应未指定字符集，则使用的字符集。
	 * @since 5.1.11
	 */
	public String getResponseBodyAsString(Charset fallbackCharset) {
		if (this.responseCharset == null) {
			// 如果响应字符集为空，则使用回退字符集和响应体字节数组构建字符串
			return new String(this.responseBody, fallbackCharset);
		}
		try {
			// 否则使用响应字符集构建响应字符串
			return new String(this.responseBody, this.responseCharset);
		} catch (UnsupportedEncodingException ex) {
			// 不应该发生
			throw new IllegalStateException(ex);
		}
	}

}
