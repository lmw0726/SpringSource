/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.server.reactive;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * 包装另一个 {@link ServerHttpResponse} 并将所有方法委托给它。
 * 子类可以有选择地重写特定方法。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ServerHttpResponseDecorator implements ServerHttpResponse {
	/**
	 * 代理的ServerHttp响应
	 */
	private final ServerHttpResponse delegate;


	public ServerHttpResponseDecorator(ServerHttpResponse delegate) {
		Assert.notNull(delegate, "Delegate is required");
		this.delegate = delegate;
	}


	public ServerHttpResponse getDelegate() {
		return this.delegate;
	}


	// ServerHttpResponse 委托方法...

	@Override
	public boolean setStatusCode(@Nullable HttpStatus status) {
		return getDelegate().setStatusCode(status);
	}

	@Override
	public HttpStatus getStatusCode() {
		return getDelegate().getStatusCode();
	}

	@Override
	public boolean setRawStatusCode(@Nullable Integer value) {
		return getDelegate().setRawStatusCode(value);
	}

	@Override
	public Integer getRawStatusCode() {
		return getDelegate().getRawStatusCode();
	}

	@Override
	public HttpHeaders getHeaders() {
		return getDelegate().getHeaders();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return getDelegate().getCookies();
	}

	@Override
	public void addCookie(ResponseCookie cookie) {
		getDelegate().addCookie(cookie);
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return getDelegate().bufferFactory();
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		getDelegate().beforeCommit(action);
	}

	@Override
	public boolean isCommitted() {
		return getDelegate().isCommitted();
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return getDelegate().writeWith(body);
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return getDelegate().writeAndFlushWith(body);
	}

	@Override
	public Mono<Void> setComplete() {
		return getDelegate().setComplete();
	}


	/**
	 * 返回底层服务器 API 的原生响应，如果可能的话，
	 * 还会解包 {@link ServerHttpResponseDecorator}。
	 *
	 * @param response 要检查的响应
	 * @param <T>      期望的原生响应类型
	 * @throws IllegalArgumentException 如果无法获取原生响应
	 * @since 5.3.3
	 */
	public static <T> T getNativeResponse(ServerHttpResponse response) {
		// 如果响应对象是 AbstractServerHttpResponse 类型，则返回其内部的本机响应对象
		if (response instanceof AbstractServerHttpResponse) {
			return ((AbstractServerHttpResponse) response).getNativeResponse();
		} else if (response instanceof ServerHttpResponseDecorator) {
			// 如果响应对象是 ServerHttpResponseDecorator 类型，则递归获取其委托对象，并再次调用该方法
			return getNativeResponse(((ServerHttpResponseDecorator) response).getDelegate());
		} else {
			// 如果响应对象类型不是上述两种类型，则抛出异常
			throw new IllegalArgumentException(
					"Can't find native response in " + response.getClass().getName());
		}
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
	}

}
