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

package org.springframework.mock.http;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * {@link HttpOutputMessage} 的模拟实现。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MockHttpOutputMessage implements HttpOutputMessage {
	/**
	 * 默认字符集
	 */
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * 头部
	 */
	private final HttpHeaders headers = new HttpHeaders();

	/**
	 * 主体
	 */
	private final ByteArrayOutputStream body = new ByteArrayOutputStream(1024);


	/**
	 * 返回头部信息。
	 */
	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * 返回输出流，用于写入消息体内容。
	 */
	@Override
	public OutputStream getBody() throws IOException {
		return this.body;
	}

	/**
	 * 返回消息体内容的字节数组表示。
	 */
	public byte[] getBodyAsBytes() {
		return this.body.toByteArray();
	}

	/**
	 * 返回消息体内容的 UTF-8 编码字符串表示。
	 */
	public String getBodyAsString() {
		return getBodyAsString(DEFAULT_CHARSET);
	}

	/**
	 * 返回消息体内容的字符串表示。
	 * @param charset 用于将消息体内容转换为字符串的字符集
	 */
	public String getBodyAsString(Charset charset) {
		return StreamUtils.copyToString(this.body, charset);
	}

}
