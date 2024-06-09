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

import org.springframework.core.NestedRuntimeException;

/**
 * {@link RestTemplate}由于服务器错误响应（通过{@link ResponseErrorHandler＃hasError(ClientHttpResponse)}确定），
 * 无法解码响应或低级I / O错误而导致请求失败而抛出的异常的基类。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class RestClientException extends NestedRuntimeException {

	private static final long serialVersionUID = -4084444984163796577L;


	/**
	 * 使用给定的消息构造一个新的{@code RestClientException}实例。
	 *
	 * @param msg 消息
	 */
	public RestClientException(String msg) {
		super(msg);
	}

	/**
	 * 使用给定的消息和异常构造一个新的{@code RestClientException}实例。
	 *
	 * @param msg 消息
	 * @param ex  异常
	 */
	public RestClientException(String msg, Throwable ex) {
		super(msg, ex);
	}

}
