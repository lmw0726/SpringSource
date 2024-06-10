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

package org.springframework.http.client.reactive;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClientRequest;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;

/**
 * 用于 Reactor-Netty HTTP 客户端的 {@link ClientHttpRequest} 实现。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see reactor.netty.http.client.HttpClient
 * @since 5.0
 */
class ReactorClientHttpRequest extends AbstractClientHttpRequest implements ZeroCopyHttpOutputMessage {

	/**
	 * HTTP请求方法
	 */
	private final HttpMethod httpMethod;

	/**
	 * 请求URI
	 */
	private final URI uri;

	/**
	 * Http客户端请求 实例
	 */
	private final HttpClientRequest request;

	/**
	 * Netty出站 实例
	 */
	private final NettyOutbound outbound;

	/**
	 * Netty数据缓冲区工厂
	 */
	private final NettyDataBufferFactory bufferFactory;


	/**
	 * 构造函数
	 *
	 * @param method   请求方法
	 * @param uri      请求URI
	 * @param request  HttpClientRequest实例
	 * @param outbound NettyOutbound实例
	 */
	public ReactorClientHttpRequest(HttpMethod method, URI uri, HttpClientRequest request, NettyOutbound outbound) {
		this.httpMethod = method;
		this.uri = uri;
		this.request = request;
		this.outbound = outbound;
		this.bufferFactory = new NettyDataBufferFactory(outbound.alloc());
	}


	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return this.bufferFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getNativeRequest() {
		return (T) this.request;
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return doCommit(() -> {
			// 如果body是Mono实例
			if (body instanceof Mono) {
				// 将body转换为Mono<ByteBuf>，并发送
				Mono<ByteBuf> byteBufMono = Mono.from(body).map(NettyDataBufferFactory::toByteBuf);
				return this.outbound.send(byteBufMono).then();
			} else {
				// 否则，将body转换为Flux<ByteBuf>，并发送
				Flux<ByteBuf> byteBufFlux = Flux.from(body).map(NettyDataBufferFactory::toByteBuf);
				return this.outbound.send(byteBufFlux).then();
			}
		});
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		// 将body转换为Publisher<Publisher<ByteBuf>>，每个元素代表一个ByteBuf流
		Publisher<Publisher<ByteBuf>> byteBufs = Flux.from(body).map(ReactorClientHttpRequest::toByteBufs);
		// 执行提交操作，发送字节缓冲组并返回结果
		return doCommit(() -> this.outbound.sendGroups(byteBufs).then());
	}

	private static Publisher<ByteBuf> toByteBufs(Publisher<? extends DataBuffer> dataBuffers) {
		return Flux.from(dataBuffers).map(NettyDataBufferFactory::toByteBuf);
	}

	@Override
	public Mono<Void> writeWith(Path file, long position, long count) {
		return doCommit(() -> this.outbound.sendFile(file, position, count).then());
	}

	@Override
	public Mono<Void> setComplete() {
		return doCommit(this.outbound::then);
	}

	@Override
	protected void applyHeaders() {
		getHeaders().forEach((key, value) -> this.request.requestHeaders().set(key, value));
	}

	@Override
	protected void applyCookies() {
		// 获取所有Cookie的值并扁平化为一个流
		getCookies().values().stream().flatMap(Collection::stream)
				// 将每个Cookie转换为 默认Cookie，并添加到请求中
				.map(cookie -> new DefaultCookie(cookie.getName(), cookie.getValue()))
				.forEach(this.request::addCookie);
	}

	@Override
	protected HttpHeaders initReadOnlyHeaders() {
		return HttpHeaders.readOnlyHttpHeaders(new NettyHeadersAdapter(this.request.requestHeaders()));
	}

}
