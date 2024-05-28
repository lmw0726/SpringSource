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

package org.springframework.web.server;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

/**
 * 用于表示适用于 Spring Web 应用程序的响应状态为 400（错误的请求）的错误的异常。该异常提供了附加字段（例如，如果与错误相关，则提供一个可选的 {@link MethodParameter}）。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class ServerWebInputException extends ResponseStatusException {
	/**
	 * 方法参数
	 */
	@Nullable
	private final MethodParameter parameter;


	/**
	 * 仅带有解释的构造函数。
	 */
	public ServerWebInputException(String reason) {
		this(reason, null, null);
	}

	/**
	 * 与特定 {@code MethodParameter} 关联的 400 错误的构造函数。
	 */
	public ServerWebInputException(String reason, @Nullable MethodParameter parameter) {
		this(reason, parameter, null);
	}

	/**
	 * 带有根原因的 400 错误的构造函数。
	 */
	public ServerWebInputException(String reason, @Nullable MethodParameter parameter, @Nullable Throwable cause) {
		super(HttpStatus.BAD_REQUEST, reason, cause);
		this.parameter = parameter;
	}


	/**
	 * 返回与此错误相关联的 {@code MethodParameter}，如果有的话。
	 */
	@Nullable
	public MethodParameter getMethodParameter() {
		return this.parameter;
	}

}
