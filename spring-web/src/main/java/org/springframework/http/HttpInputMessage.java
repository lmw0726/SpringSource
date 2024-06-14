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

package org.springframework.http;

import java.io.IOException;
import java.io.InputStream;

/**
 * 表示一个HTTP输入消息，由 {@linkplain #getHeaders() headers} 和可读的 {@linkplain #getBody() body} 组成。
 *
 * <p>通常由服务器端的HTTP请求处理程序或客户端的HTTP响应处理程序实现。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface HttpInputMessage extends HttpMessage {

	/**
	 * 以输入流的形式返回消息体。
	 *
	 * @return 输入流形式的消息体（永不为 {@code null}）
	 * @throws IOException 在I/O错误的情况下
	 */
	InputStream getBody() throws IOException;

}
