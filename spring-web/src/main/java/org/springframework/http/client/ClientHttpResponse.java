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

package org.springframework.http.client;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;

import java.io.Closeable;
import java.io.IOException;

/**
 * 表示客户端 HTTP 响应。
 *
 * <p>通过调用 {@link ClientHttpRequest#execute()} 获得。
 *
 * <p>{@code ClientHttpResponse} 必须被 {@linkplain #close() 关闭}，
 * 通常在 {@code finally} 块中进行。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface ClientHttpResponse extends HttpInputMessage, Closeable {

	/**
	 * 获取 HTTP 状态码作为 {@link HttpStatus} 枚举值。
	 * <p>对于 {@code HttpStatus} 不支持的状态码，请使用
	 * {@link #getRawStatusCode()}。
	 *
	 * @return 作为 HttpStatus 枚举值的 HTTP 状态码（永不为 {@code null}）
	 * @throws IOException              如果发生 I/O 错误
	 * @throws IllegalArgumentException 如果是未知的 HTTP 状态码
	 * @see HttpStatus#valueOf(int)
	 */
	HttpStatus getStatusCode() throws IOException;

	/**
	 * 以整数形式获取 HTTP 状态码（可能是非标准且不能通过 {@link HttpStatus} 枚举解析）。
	 *
	 * @return 整数形式的 HTTP 状态码
	 * @throws IOException 如果发生 I/O 错误
	 * @see #getStatusCode()
	 * @see HttpStatus#resolve(int)
	 * @since 3.1.1
	 */
	int getRawStatusCode() throws IOException;

	/**
	 * 获取响应的 HTTP 状态文本。
	 *
	 * @return HTTP 状态文本
	 * @throws IOException 如果发生 I/O 错误
	 */
	String getStatusText() throws IOException;

	/**
	 * 关闭此响应，释放所有创建的资源。
	 */
	@Override
	void close();

}
