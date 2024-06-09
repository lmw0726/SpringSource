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

package org.springframework.web.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * {@link ClientHttpResponse} 的实现，不仅可以检查响应是否具有消息体，还可以通过实际读取输入流来检查其长度是否为 0（即空）。
 *
 * @author Brian Clozel
 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.3">RFC 7230 Section 3.3.3</a>
 * @since 4.1.5
 */
class MessageBodyClientHttpResponseWrapper implements ClientHttpResponse {
	/**
	 * 客户端Http响应
	 */
	private final ClientHttpResponse response;

	/**
	 * 回推输入流
	 */
	@Nullable
	private PushbackInputStream pushbackInputStream;


	public MessageBodyClientHttpResponseWrapper(ClientHttpResponse response) {
		this.response = response;
	}


	/**
	 * 指示响应是否具有消息体。
	 * <p>实现对以下情况返回 {@code false}：
	 * <ul>
	 * <li>响应状态为 {@code 1XX}、{@code 204} 或 {@code 304}</li>
	 * <li>{@code Content-Length} 标头为 {@code 0}</li>
	 * </ul>
	 *
	 * @return 如果响应具有消息体，则为 {@code true}，否则为 {@code false}
	 * @throws IOException 在 I/O 错误的情况下
	 */
	public boolean hasMessageBody() throws IOException {
		// 获取 HTTP 状态码并解析为 HttpStatus 对象
		HttpStatus status = HttpStatus.resolve(getRawStatusCode());
		if (status != null && (status.is1xxInformational() || status == HttpStatus.NO_CONTENT ||
				status == HttpStatus.NOT_MODIFIED)) {
			// 如果状态码不为空，且是信息性状态码（1xx），
			// 或者是 NO_CONTENT 或 NOT_MODIFIED，
			// 则返回 false
			return false;
		}
		// 如果响应头中的内容长度为 0，则返回 false
		if (getHeaders().getContentLength() == 0) {
			return false;
		}
		// 否则返回 true
		return true;
	}

	/**
	 * 指示响应是否具有空的消息体。
	 * <p>实现尝试读取响应流的前几个字节：
	 * <ul>
	 * <li>如果没有字节可用，则消息体为空</li>
	 * <li>否则它不是空的，流被重置到其起始位置以进行进一步读取</li>
	 * </ul>
	 *
	 * @return 如果响应具有零长度的消息体，则为 {@code true}，否则为 {@code false}
	 * @throws IOException 在 I/O 错误的情况下
	 */
	@SuppressWarnings("ConstantConditions")
	public boolean hasEmptyMessageBody() throws IOException {
		// 获取响应体的输入流
		InputStream body = this.response.getBody();
		// 根据约定，body 不应为 null，但是仍然进行检查..
		if (body == null) {
			return true;
		}
		// 如果输入流支持标记操作，则进行标记
		if (body.markSupported()) {
			body.mark(1);
			// 读取一个字节，如果为 -1，则表示流已结束
			if (body.read() == -1) {
				return true;
			} else {
				// 重置输入流
				body.reset();
				return false;
			}
		} else {
			// 创建可推回的输入流，并读取一个字节进行检查
			this.pushbackInputStream = new PushbackInputStream(body);
			int b = this.pushbackInputStream.read();
			// 如果为 -1，则表示流已结束
			if (b == -1) {
				return true;
			} else {
				// 推回读取的字节，继续使用
				this.pushbackInputStream.unread(b);
				return false;
			}
		}
	}


	@Override
	public HttpHeaders getHeaders() {
		return this.response.getHeaders();
	}

	@Override
	public InputStream getBody() throws IOException {
		return (this.pushbackInputStream != null ? this.pushbackInputStream : this.response.getBody());
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
	public void close() {
		this.response.close();
	}

}
