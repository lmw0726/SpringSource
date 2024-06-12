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

package org.springframework.http.server.reactive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * 表示反应式服务器端HTTP响应。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ServerHttpResponse extends ReactiveHttpOutputMessage {

	/**
	 * 设置响应的HTTP状态码。
	 *
	 * @param status 作为{@link HttpStatus}枚举值的HTTP状态
	 * @return 如果状态码更改未处理，因为HTTP响应已提交，则返回{@code false}，
	 * 成功设置则返回{@code true}。
	 */
	boolean setStatusCode(@Nullable HttpStatus status);

	/**
	 * 返回已设置的状态码，或者如果没有设置则返回来自底层服务器的响应状态。
	 * 如果状态码值超出{@link HttpStatus}枚举范围，或底层服务器没有默认值，
	 * 则返回值可能为{@code null}。
	 *
	 * @return 当前设置的状态码，可能为{@code null}
	 */
	@Nullable
	HttpStatus getStatusCode();

	/**
	 * 将HTTP状态码设置为给定的值（可能是非标准的，无法通过{@link HttpStatus}枚举解析），以整数表示。
	 *
	 * @param value 状态码值
	 * @return 如果状态码更改未处理，因为HTTP响应已提交，则返回{@code false}，
	 * 成功设置则返回{@code true}。
	 * @since 5.2.4
	 */
	default boolean setRawStatusCode(@Nullable Integer value) {
		// 如果传入的值为 null
		if (value == null) {
			// 将状态码设置为 null，并返回结果
			return setStatusCode(null);
		} else {
			// 尝试解析传入的值为 HttpStatus 对象
			HttpStatus httpStatus = HttpStatus.resolve(value);

			// 如果无法解析传入的值为有效的 HttpStatus 对象
			if (httpStatus == null) {
				// 抛出 IllegalStateException 异常
				throw new IllegalStateException(
						"Unresolvable HttpStatus for general ServerHttpResponse: " + value);
			}

			// 将状态码设置为解析后的 HttpStatus 对象，并返回结果
			return setStatusCode(httpStatus);
		}
	}

	/**
	 * 返回已设置的状态码，或者如果没有设置则返回来自底层服务器的响应状态。
	 * 如果底层服务器没有默认值，则返回值可能为{@code null}。
	 *
	 * @return 当前设置的原始状态码，可能为{@code null}
	 * @since 5.2.4
	 */
	@Nullable
	default Integer getRawStatusCode() {
		// 获取当前的 HttpStatus 状态码
		HttpStatus httpStatus = getStatusCode();

		// 如果 HttpStatus 不为空，返回其整数值；否则，返回 null
		return (httpStatus != null ? httpStatus.value() : null);
	}

	/**
	 * 返回一个可变的映射，包含要发送到服务器的cookies。
	 *
	 * @return 包含cookies的可变映射
	 */
	MultiValueMap<String, ResponseCookie> getCookies();

	/**
	 * 添加给定的{@code ResponseCookie}。
	 *
	 * @param cookie 要添加的cookie
	 * @throws IllegalStateException 如果响应已经提交
	 */
	void addCookie(ResponseCookie cookie);

}
