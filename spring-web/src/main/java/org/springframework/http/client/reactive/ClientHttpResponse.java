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

package org.springframework.http.client.reactive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * 表示客户端端的响应式 HTTP 响应。
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
public interface ClientHttpResponse extends ReactiveHttpInputMessage {

	/**
	 * 返回一个表示底层连接的 ID（如果可用），
	 * 或用于关联日志消息的请求。
	 *
	 * @since 5.3.5
	 */
	default String getId() {
		return ObjectUtils.getIdentityHexString(this);
	}

	/**
	 * 将 HTTP 状态码作为 {@link HttpStatus} 枚举值返回。
	 *
	 * @return HTTP 状态作为 HttpStatus 枚举值（永远不会是 {@code null}）
	 * @throws IllegalArgumentException 如果是未知的 HTTP 状态码
	 * @see HttpStatus#valueOf(int)
	 * @since #getRawStatusCode()
	 */
	HttpStatus getStatusCode();

	/**
	 * 将 HTTP 状态码（可能是非标准的，不能通过 {@link HttpStatus} 枚举解析）作为整数返回。
	 *
	 * @return HTTP 状态作为整数值
	 * @see #getStatusCode()
	 * @see HttpStatus#resolve(int)
	 * @since 5.0.6
	 */
	int getRawStatusCode();

	/**
	 * 返回从服务器接收到的响应 Cookie 的只读映射。
	 */
	MultiValueMap<String, ResponseCookie> getCookies();

}
