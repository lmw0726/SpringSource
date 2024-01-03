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

package org.springframework.web.reactive.function.client;

import org.springframework.core.NestedRuntimeException;

/**
 * {@link WebClient} 在发生错误时发布的异常的抽象基类。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class WebClientException extends NestedRuntimeException {

	private static final long serialVersionUID = 472776714118912855L;

	/**
	 * 使用给定的消息构造一个新的 {@code WebClientException} 实例。
	 *
	 * @param msg 消息
	 */
	public WebClientException(String msg) {
		super(msg);
	}

	/**
	 * 使用给定的消息和异常构造一个新的 {@code WebClientException} 实例。
	 *
	 * @param msg 消息
	 * @param ex  异常
	 */
	public WebClientException(String msg, Throwable ex) {
		super(msg, ex);
	}

}
