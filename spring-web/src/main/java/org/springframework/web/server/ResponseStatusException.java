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

package org.springframework.web.server;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Map;

/**
 * 与特定 HTTP 响应状态码相关的异常的基类。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
@SuppressWarnings("serial")
public class ResponseStatusException extends NestedRuntimeException {
	/**
	 * 状态码
	 */
	private final int status;

	/**
	 * 原因
	 */
	@Nullable
	private final String reason;


	/**
	 * 带有响应状态的构造函数。
	 *
	 * @param status HTTP 状态（必需）
	 */
	public ResponseStatusException(HttpStatus status) {
		this(status, null);
	}

	/**
	 * 带有响应状态和要添加到异常消息中的原因的构造函数。
	 *
	 * @param status 响应状态（必需）
	 * @param reason 相关原因（可选）
	 */
	public ResponseStatusException(HttpStatus status, @Nullable String reason) {
		super("");
		Assert.notNull(status, "HttpStatus is required");
		this.status = status.value();
		this.reason = reason;
	}

	/**
	 * 带有响应状态、要添加到异常消息中的原因以及嵌套异常的构造函数。
	 *
	 * @param status 响应状态（必需）
	 * @param reason 相关原因（可选）
	 * @param cause  嵌套异常（可选）
	 */
	public ResponseStatusException(HttpStatus status, @Nullable String reason, @Nullable Throwable cause) {
		super(null, cause);
		Assert.notNull(status, "HttpStatus is required");
		this.status = status.value();
		this.reason = reason;
	}

	/**
	 * 带有响应状态、要添加到异常消息中的原因以及嵌套异常的构造函数。
	 *
	 * @param rawStatusCode HTTP 状态码值
	 * @param reason        相关原因（可选）
	 * @param cause         嵌套异常（可选）
	 * @since 5.3
	 */
	public ResponseStatusException(int rawStatusCode, @Nullable String reason, @Nullable Throwable cause) {
		super(null, cause);
		this.status = rawStatusCode;
		this.reason = reason;
	}


	/**
	 * 返回与此异常关联的 HTTP 状态。
	 *
	 * @throws IllegalArgumentException 如果是未知的 HTTP 状态码
	 * @see HttpStatus#valueOf(int)
	 * @since #getRawStatusCode()
	 */
	public HttpStatus getStatus() {
		return HttpStatus.valueOf(this.status);
	}

	/**
	 * 将 HTTP 状态码（可能是非标准的，并且无法通过 HttpStatus 枚举解析）作为整数返回。
	 *
	 * @return 整数值的 HTTP 状态
	 * @see #getStatus()
	 * @see HttpStatus#resolve(int)
	 * @since 5.3
	 */
	public int getRawStatusCode() {
		return this.status;
	}

	/**
	 * 返回与应该添加到错误响应的异常关联的头，例如 "Allow"、"Accept" 等。
	 * <p>此类中的默认实现返回空映射。
	 *
	 * @since 5.1.11
	 * @deprecated 从 5.1.13 开始，使用 {@link #getResponseHeaders()} 代替
	 */
	@Deprecated
	public Map<String, String> getHeaders() {
		return Collections.emptyMap();
	}

	/**
	 * 返回与应该添加到错误响应的异常关联的头，例如 "Allow"、"Accept" 等。
	 * <p>此类中的默认实现返回空头。
	 *
	 * @since 5.1.13
	 */
	public HttpHeaders getResponseHeaders() {
		// 获取头部信息的映射
		Map<String, String> headers = getHeaders();
		// 如果头部信息为空
		if (headers.isEmpty()) {
			// 返回空的 HttpHeaders
			return HttpHeaders.EMPTY;
		}
		// 创建新的 HttpHeaders 对象
		HttpHeaders result = new HttpHeaders();
		// 将头部信息映射中的每个键值对添加到新的 HttpHeaders 对象中
		getHeaders().forEach(result::add);
		// 返回新的 HttpHeaders 对象
		return result;
	}

	/**
	 * 解释异常的原因（可能为 {@code null} 或空）。
	 */
	@Nullable
	public String getReason() {
		return this.reason;
	}


	@Override
	public String getMessage() {
		// 解析状态码
		HttpStatus code = HttpStatus.resolve(this.status);
		// 构建消息
		String msg = (code != null ? code : this.status) + (this.reason != null ? " \"" + this.reason + "\"" : "");
		// 使用嵌套异常工具构建消息
		return NestedExceptionUtils.buildMessage(msg, getCause());
	}

}
