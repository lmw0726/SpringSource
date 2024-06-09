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

package org.springframework.web.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * 当找不到合适的{@link org.springframework.http.converter.HttpMessageConverter}来提取响应时抛出。
 *
 * @author Rossen Stoyanchev
 * @since 5.2.7
 */
public class UnknownContentTypeException extends RestClientException {

	private static final long serialVersionUID = 2759516676367274084L;

	/**
	 * 响应所期望的目标类型
	 */
	private final Type targetType;

	/**
	 * 响应的内容类型
	 */
	private final MediaType contentType;

	/**
	 * 原始HTTP状态码值
	 */
	private final int rawStatusCode;

	/**
	 * HTTP状态文本
	 */
	private final String statusText;

	/**
	 * 响应体
	 */
	private final byte[] responseBody;

	/**
	 * HTTP响应头
	 */
	private final HttpHeaders responseHeaders;


	/**
	 * 使用给定的响应数据构造一个新实例。
	 *
	 * @param targetType      期望的目标类型
	 * @param contentType     响应的内容类型
	 * @param statusCode      原始状态码值
	 * @param statusText      状态文本
	 * @param responseHeaders 响应头（可能为{@code null}）
	 * @param responseBody    响应体内容（可能为{@code null}）
	 */
	public UnknownContentTypeException(Type targetType, MediaType contentType,
									   int statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody) {

		super("Could not extract response: no suitable HttpMessageConverter found " +
				"for response type [" + targetType + "] and content type [" + contentType + "]");

		this.targetType = targetType;
		this.contentType = contentType;
		this.rawStatusCode = statusCode;
		this.statusText = statusText;
		this.responseHeaders = responseHeaders;
		this.responseBody = responseBody;
	}


	/**
	 * 返回响应所期望的目标类型。
	 */
	public Type getTargetType() {
		return this.targetType;
	}

	/**
	 * 返回响应的内容类型，或者返回“application/octet-stream”。
	 */
	public MediaType getContentType() {
		return this.contentType;
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
	public byte[] getResponseBody() {
		return this.responseBody;
	}

	/**
	 * 返回使用响应“Content-Type”的字符集或{@code "UTF-8"}以及默认字符集的字符串转换后的响应体。
	 */
	public String getResponseBodyAsString() {
		return new String(this.responseBody, this.contentType.getCharset() != null ?
				this.contentType.getCharset() : StandardCharsets.UTF_8);
	}

}
