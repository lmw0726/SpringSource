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

package org.springframework.http.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 简单的 {@link ClientHttpResponse} 实现，将响应的主体读取到内存中，
 * 从而允许多次调用 {@link #getBody()}。
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
final class BufferingClientHttpResponseWrapper implements ClientHttpResponse {
	/**
	 * 客户端Http响应
	 */
	private final ClientHttpResponse response;

	/**
	 * 响应主体字节数组
	 */
	@Nullable
	private byte[] body;


	BufferingClientHttpResponseWrapper(ClientHttpResponse response) {
		this.response = response;
	}


	@Override
	public HttpStatus getStatusCode() throws IOException {
		return this.response.getStatusCode();
	}

	@Override
	public int getRawStatusCode() throws IOException {
		return this.response.getRawStatusCode();
	}

	@Override
	public String getStatusText() throws IOException {
		return this.response.getStatusText();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.response.getHeaders();
	}

	@Override
	public InputStream getBody() throws IOException {
		// 如果当前对象的body属性为空
		if (this.body == null) {
			// 将响应体的内容复制为字节数组，并赋值给body属性
			this.body = StreamUtils.copyToByteArray(this.response.getBody());
		}

		// 返回一个包含body字节数组的新ByteArrayInputStream对象
		return new ByteArrayInputStream(this.body);
	}

	@Override
	public void close() {
		this.response.close();
	}

}
