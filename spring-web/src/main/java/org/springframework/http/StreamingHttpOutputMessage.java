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
import java.io.OutputStream;

/**
 * 表示一个允许设置流式主体的 HTTP 输出消息。
 * 请注意，此类消息通常不支持 {@link #getBody()} 访问。
 *
 * @author Arjen Poutsma
 * @see #setBody
 * @since 4.0
 */
public interface StreamingHttpOutputMessage extends HttpOutputMessage {

	/**
	 * 设置此消息的流式主体回调。
	 *
	 * @param body 流式主体回调
	 */
	void setBody(Body body);


	/**
	 * 定义可直接写入 {@link OutputStream} 的主体的契约。
	 * 与提供通过回调机制间接访问 {@link OutputStream} 的 HTTP 客户端库一起使用时很有用。
	 */
	@FunctionalInterface
	interface Body {

		/**
		 * 将此主体写入给定的 {@link OutputStream}。
		 *
		 * @param outputStream 要写入的输出流
		 * @throws IOException 如果发生 I/O 错误
		 */
		void writeTo(OutputStream outputStream) throws IOException;
	}

}
