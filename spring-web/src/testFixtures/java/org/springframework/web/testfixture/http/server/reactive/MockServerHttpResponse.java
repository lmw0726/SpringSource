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

package org.springframework.web.testfixture.http.server.reactive;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.AbstractServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * {@link AbstractServerHttpResponse} 的 Mock 扩展，用于在没有实际服务器的测试中使用。
 *
 * <p>默认情况下，响应内容在写入时会被完全消耗并缓存以供后续访问，但也可以设置自定义的 {@link #setWriteHandler(Function) writeHandler}。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MockServerHttpResponse extends AbstractServerHttpResponse {
	/**
	 * 响应体数据
	 */
	private Flux<DataBuffer> body = Flux.error(new IllegalStateException(
			"No content was written nor was setComplete() called on this response."));

	/**
	 * 写入处理器
	 */
	private Function<Flux<DataBuffer>, Mono<Void>> writeHandler;


	public MockServerHttpResponse() {
		this(DefaultDataBufferFactory.sharedInstance);
	}

	public MockServerHttpResponse(DataBufferFactory dataBufferFactory) {
		super(dataBufferFactory);
		this.writeHandler = body -> {
			// 避免使用 .then() 导致数据缓冲区被丢弃和释放
			Sinks.Empty<Void> completion = Sinks.unsafe().empty();
			// 缓存响应体
			this.body = body.cache();
			// 信号被串行化
			this.body.subscribe(aVoid -> {
			}, completion::tryEmitError, completion::tryEmitEmpty);
			return completion.asMono();
		};
	}


	/**
	 * 配置自定义处理程序以消耗响应体。
	 * <p>默认情况下，在测试中，响应体内容将被完全消耗并缓存以供后续访问。
	 * 使用此选项控制响应体的消耗方式。
	 *
	 * @param writeHandler 使用返回{@code Mono<Void>}的写入处理程序，在响应体被“写入”（即消耗）后
	 */
	public void setWriteHandler(Function<Flux<DataBuffer>, Mono<Void>> writeHandler) {
		Assert.notNull(writeHandler, "'writeHandler' is required");
		this.body = Flux.error(new IllegalStateException("Not available with custom write handler."));
		this.writeHandler = writeHandler;
	}

	@Override
	public <T> T getNativeResponse() {
		throw new IllegalStateException("This is a mock. No running server, no native response.");
	}


	@Override
	protected void applyStatusCode() {
	}

	@Override
	protected void applyHeaders() {
	}

	@Override
	protected void applyCookies() {
		// 遍历 Cookie 集合中的每个键对应的值（每个值是一个 Cookie 列表）
		for (List<ResponseCookie> cookies : getCookies().values()) {
			// 遍历 Cookie 列表中的每个 Cookie 对象
			for (ResponseCookie cookie : cookies) {
				// 将每个 Cookie 对象转换为字符串并添加到响应头的 Set-Cookie 中
				getHeaders().add(HttpHeaders.SET_COOKIE, cookie.toString());
			}
		}
	}

	@Override
	protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
		return this.writeHandler.apply(Flux.from(body));
	}

	@Override
	protected Mono<Void> writeAndFlushWithInternal(
			Publisher<? extends Publisher<? extends DataBuffer>> body) {

		return this.writeHandler.apply(Flux.from(body).concatMap(Flux::from));
	}

	@Override
	public Mono<Void> setComplete() {
		return doCommit(() -> Mono.defer(() -> this.writeHandler.apply(Flux.empty())));
	}

	/**
	 * 返回响应体或错误流（如果未设置响应体）。
	 */
	public Flux<DataBuffer> getBody() {
		return this.body;
	}

	/**
	 * 聚合响应数据并使用“Content-Type”字符集或默认情况下的“UTF-8”转换为字符串。
	 */
	public Mono<String> getBodyAsString() {

		// 获取响应头中的字符集，如果未设置字符集，则默认为 UTF-8
		Charset charset = Optional.ofNullable(getHeaders().getContentType()).map(MimeType::getCharset)
				.orElse(StandardCharsets.UTF_8);

		// 将响应体中的数据缓冲区合并成一个数据缓冲区
		return DataBufferUtils.join(getBody())
				// 将数据缓冲区转换为字符串
				.map(buffer -> {
					String s = buffer.toString(charset);
					// 释放数据缓冲区
					DataBufferUtils.release(buffer);
					return s;
				})
				// 如果数据缓冲区为空，则返回一个空字符串
				.defaultIfEmpty("");
	}

}
