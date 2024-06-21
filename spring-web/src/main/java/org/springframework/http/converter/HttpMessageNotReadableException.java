/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.converter;

import org.springframework.http.HttpInputMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 当 {@link HttpMessageConverter} 实现的 {@link HttpMessageConverter#read} 方法失败时抛出。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("serial")
public class HttpMessageNotReadableException extends HttpMessageConversionException {
	/**
	 * Http输入消息
	 */
	@Nullable
	private final HttpInputMessage httpInputMessage;


	/**
	 * 创建一个新的 HttpMessageNotReadableException。
	 *
	 * @param msg 详细信息
	 * @deprecated 自 5.1 起，建议使用 {@link #HttpMessageNotReadableException(String, HttpInputMessage)}
	 */
	@Deprecated
	public HttpMessageNotReadableException(String msg) {
		super(msg);
		this.httpInputMessage = null;
	}

	/**
	 * 创建一个新的 HttpMessageNotReadableException。
	 *
	 * @param msg   详细信息
	 * @param cause 根本原因（如果有）
	 * @deprecated 自 5.1 起，建议使用 {@link #HttpMessageNotReadableException(String, Throwable, HttpInputMessage)}
	 */
	@Deprecated
	public HttpMessageNotReadableException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
		this.httpInputMessage = null;
	}

	/**
	 * 创建一个新的 HttpMessageNotReadableException。
	 *
	 * @param msg              详细信息
	 * @param httpInputMessage 原始 HTTP 消息
	 * @since 5.1
	 */
	public HttpMessageNotReadableException(String msg, HttpInputMessage httpInputMessage) {
		super(msg);
		this.httpInputMessage = httpInputMessage;
	}

	/**
	 * 创建一个新的 HttpMessageNotReadableException。
	 *
	 * @param msg              详细信息
	 * @param cause            根本原因（如果有）
	 * @param httpInputMessage 原始 HTTP 消息
	 * @since 5.1
	 */
	public HttpMessageNotReadableException(String msg, @Nullable Throwable cause, HttpInputMessage httpInputMessage) {
		super(msg, cause);
		this.httpInputMessage = httpInputMessage;
	}


	/**
	 * 返回原始 HTTP 消息。
	 *
	 * @since 5.1
	 */
	public HttpInputMessage getHttpInputMessage() {
		Assert.state(this.httpInputMessage != null, "No HttpInputMessage available - use non-deprecated constructors");
		return this.httpInputMessage;
	}

}
