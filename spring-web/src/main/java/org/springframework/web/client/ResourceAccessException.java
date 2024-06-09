/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.IOException;

/**
 * 当发生I/O错误时抛出的异常。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class ResourceAccessException extends RestClientException {

	private static final long serialVersionUID = -8513182514355844870L;


	/**
	 * 使用给定的消息构造一个新的{@code ResourceAccessException}实例。
	 *
	 * @param msg 消息
	 */
	public ResourceAccessException(String msg) {
		super(msg);
	}

	/**
	 * 使用给定的消息和{@link IOException}构造一个新的{@code ResourceAccessException}实例。
	 *
	 * @param msg 消息
	 * @param ex  {@code IOException}
	 */
	public ResourceAccessException(String msg, IOException ex) {
		super(msg, ex);
	}

}
