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

package org.springframework.mock.http;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@link HttpInputMessage} 的模拟实现。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MockHttpInputMessage implements HttpInputMessage {
	/**
	 * Http头部
	 */
	private final HttpHeaders headers = new HttpHeaders();

	/**
	 * 主体输入流
	 */
	private final InputStream body;


	/**
	 * 使用字节数组内容创建 {@link MockHttpInputMessage}。
	 *
	 * @param content 字节数组内容，不能为空
	 */
	public MockHttpInputMessage(byte[] content) {
		Assert.notNull(content, "Byte array must not be null");
		this.body = new ByteArrayInputStream(content);
	}

	/**
	 * 使用输入流创建 {@link MockHttpInputMessage}。
	 *
	 * @param body 输入流，不能为空
	 */
	public MockHttpInputMessage(InputStream body) {
		Assert.notNull(body, "InputStream must not be null");
		this.body = body;
	}


	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.body;
	}

}
