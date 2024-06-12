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
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.reactive.AbstractClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/**
 * {@link ClientHttpRequest} 的模拟实现。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MockClientHttpRequest extends AbstractClientHttpRequest implements HttpRequest {
	/**
	 * Http方法
	 */
	private final HttpMethod httpMethod;

	/**
	 * URL
	 */
	private final URI url;

	/**
	 * 请求体
	 */
	private Flux<DataBuffer> body = Flux.error(
			new IllegalStateException("The body is not set. " +
					"Did handling complete with success? Is a custom \"writeHandler\" configured?"));

	/**
	 * 写入处理器
	 */
	private Function<Flux<DataBuffer>, Mono<Void>> writeHandler;


	public MockClientHttpRequest(HttpMethod httpMethod, String urlTemplate, Object... vars) {
		this(httpMethod, UriComponentsBuilder.fromUriString(urlTemplate).buildAndExpand(vars).encode().toUri());
	}

	public MockClientHttpRequest(HttpMethod httpMethod, URI url) {
		this.httpMethod = httpMethod;
		this.url = url;
		this.writeHandler = body -> {
			this.body = body.cache();
			return this.body.then();
		};
	}


	/**
	 * 配置用于写入请求体的自定义处理程序。
	 *
	 * <p>默认的写入处理程序消耗并缓存请求体，以便随后访问，例如在测试断言中。当请求体是无限流时使用此属性。
	 *
	 * @param writeHandler 当请求体已被 "写入"（即消耗）时返回 {@code Mono<Void>} 的写入处理程序
	 */
	public void setWriteHandler(Function<Flux<DataBuffer>, Mono<Void>> writeHandler) {
		Assert.notNull(writeHandler, "'writeHandler' is required");
		this.writeHandler = writeHandler;
	}


	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	@Override
	public String getMethodValue() {
		return this.httpMethod.name();
	}

	@Override
	public URI getURI() {
		return this.url;
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return DefaultDataBufferFactory.sharedInstance;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getNativeRequest() {
		return (T) this;
	}

	@Override
	protected void applyHeaders() {
	}

	@Override
	protected void applyCookies() {
		getCookies().values().stream().flatMap(Collection::stream)
				.forEach(cookie -> getHeaders().add(HttpHeaders.COOKIE, cookie.toString()));
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return doCommit(() -> Mono.defer(() -> this.writeHandler.apply(Flux.from(body))));
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return writeWith(Flux.from(body).flatMap(p -> p));
	}

	@Override
	public Mono<Void> setComplete() {
		return writeWith(Flux.empty());
	}


	/**
	 * 返回请求体，如果请求体从未设置过或配置了 {@link #setWriteHandler}，则返回错误流。
	 */
	public Flux<DataBuffer> getBody() {
		return this.body;
	}

	/**
	 * 聚合响应数据并使用 "Content-Type" 字符集或默认情况下的 "UTF-8" 转换为字符串。
	 */
	public Mono<String> getBodyAsString() {

		// 获取响应体的字符集，如果不存在则使用 UTF-8
		Charset charset = Optional.ofNullable(getHeaders().getContentType()).map(MimeType::getCharset)
				.orElse(StandardCharsets.UTF_8);

		// 将响应体中的数据缓冲区连接起来，并使用指定的字符集将其转换为字符串
		return DataBufferUtils.join(getBody())
				.map(buffer -> {
					// 将数据缓冲区转换为字符串
					String s = buffer.toString(charset);
					// 释放数据缓冲区，确保资源被正确清理
					DataBufferUtils.release(buffer);
					return s;
				})
				// 如果响应体为空，则返回空字符串
				.defaultIfEmpty("");
	}

}
