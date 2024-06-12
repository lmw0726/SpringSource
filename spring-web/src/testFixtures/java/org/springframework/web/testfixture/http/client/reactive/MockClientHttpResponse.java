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

package org.springframework.web.testfixture.http.client.reactive;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * {@link ClientHttpResponse} 的模拟实现。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MockClientHttpResponse implements ClientHttpResponse {

	/**
	 * 表示模拟响应的 HTTP 状态码。
	 */
	private final int status;

	/**
	 * 表示模拟响应的 HTTP 头部。
	 */
	private final HttpHeaders headers = new HttpHeaders();

	/**
	 * 表示模拟响应的 HTTP Cookie。
	 */
	private final MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

	/**
	 * 表示模拟响应的 HTTP 主体。
	 */
	private Flux<DataBuffer> body = Flux.empty();


	public MockClientHttpResponse(HttpStatus status) {
		Assert.notNull(status, "HttpStatus is required");
		this.status = status.value();
	}

	public MockClientHttpResponse(int status) {
		Assert.isTrue(status > 99 && status < 1000, "Status must be between 100 and 999");
		this.status = status;
	}


	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(this.status);
	}

	@Override
	public int getRawStatusCode() {
		return this.status;
	}

	@Override
	public HttpHeaders getHeaders() {
		// 如果响应中包含 Cookie 且响应头中没有设置 SET_COOKIE
		if (!getCookies().isEmpty() && this.headers.get(HttpHeaders.SET_COOKIE) == null) {
			// 将响应中的所有 Cookie 添加到 SET_COOKIE 响应头中
			getCookies().values().stream().flatMap(Collection::stream)
					.forEach(cookie -> this.headers.add(HttpHeaders.SET_COOKIE, cookie.toString()));
		}

		// 返回修改后的响应头
		return this.headers;
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return this.cookies;
	}

	public void setBody(Publisher<DataBuffer> body) {
		this.body = Flux.from(body);
	}

	public void setBody(String body) {
		setBody(body, StandardCharsets.UTF_8);
	}

	public void setBody(String body, Charset charset) {
		// 将给定的主体对象转换为数据缓冲区，并使用指定的字符集
		DataBuffer buffer = toDataBuffer(body, charset);

		// 将数据缓冲区包装成 Flux 流
		this.body = Flux.just(buffer);
	}

	private DataBuffer toDataBuffer(String body, Charset charset) {
		// 使用指定的字符集将主体对象转换为字节数组
		byte[] bytes = body.getBytes(charset);

		// 将字节数组包装成 ByteBuffer
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

		// 使用默认的数据缓冲区工厂将 ByteBuffer 包装成数据缓冲区
		return DefaultDataBufferFactory.sharedInstance.wrap(byteBuffer);
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.body;
	}

	/**
	 * 将响应体聚合并转换为使用 Content-Type 响应的字符集或默认为 "UTF-8" 的字符串。
	 */
	public Mono<String> getBodyAsString() {
		// 将响应体中的数据缓冲区连接起来，并将其转换为字符串
		return DataBufferUtils.join(getBody())
				.map(buffer -> {
					// 将数据缓冲区转换为字符串
					String s = buffer.toString(getCharset());
					// 释放数据缓冲区，确保资源被正确清理
					DataBufferUtils.release(buffer);
					return s;
				})
				// 如果响应体为空，则返回空字符串
				.defaultIfEmpty("");
	}

	private Charset getCharset() {
		// 初始化字符集为 null
		Charset charset = null;

		// 获取响应头中的内容类型
		MediaType contentType = getHeaders().getContentType();

		// 如果内容类型不为 null，则获取其字符集
		if (contentType != null) {
			charset = contentType.getCharset();
		}

		// 如果字符集不为 null，则返回字符集，否则返回 UTF-8 字符集
		return (charset != null ? charset : StandardCharsets.UTF_8);
	}


	@Override
	public String toString() {
		// 解析 HTTP 状态码
		HttpStatus code = HttpStatus.resolve(this.status);

		// 如果状态码不为 null，则返回状态码的名称和值，否则返回状态码的值以及对应的响应头
		return (code != null ? code.name() + "(" + this.status + ")" : "Status (" + this.status + ")") + this.headers;
	}
}
