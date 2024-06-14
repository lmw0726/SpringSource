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

package org.springframework.http.converter.json;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * 可以最终存储用于反序列化消息的 Jackson 视图的 {@link HttpInputMessage} 实现。
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public class MappingJacksonInputMessage implements HttpInputMessage {
	/**
	 * 请求体输入流
	 */
	private final InputStream body;

	/**
	 * 请求头
	 */
	private final HttpHeaders headers;

	/**
	 * 反序列化视图类
	 */
	@Nullable
	private Class<?> deserializationView;


	public MappingJacksonInputMessage(InputStream body, HttpHeaders headers) {
		this.body = body;
		this.headers = headers;
	}

	public MappingJacksonInputMessage(InputStream body, HttpHeaders headers, Class<?> deserializationView) {
		this(body, headers);
		this.deserializationView = deserializationView;
	}


	@Override
	public InputStream getBody() throws IOException {
		return this.body;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	public void setDeserializationView(@Nullable Class<?> deserializationView) {
		this.deserializationView = deserializationView;
	}

	@Nullable
	public Class<?> getDeserializationView() {
		return this.deserializationView;
	}

}
