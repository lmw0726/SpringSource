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

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.eclipse.jetty.reactive.client.ReactiveRequest;
import org.eclipse.jetty.util.Callback;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.net.HttpCookie;
import java.net.URI;
import java.util.Collection;
import java.util.function.Function;

/**
 * Jetty 响应式流式 HTTP 客户端的 {@link ClientHttpRequest} 实现。
 *
 * @author Sebastien Deleuze
 * @see <a href="https://github.com/jetty-project/jetty-reactive-httpclient">Jetty ReactiveStreams HttpClient</a>
 * @since 5.1
 */
class JettyClientHttpRequest extends AbstractClientHttpRequest {
	/**
	 * Jetty 请求
	 */
	private final Request jettyRequest;

	/**
	 * 数据缓冲区工厂
	 */
	private final DataBufferFactory bufferFactory;

	/**
	 * 反应式请求构建器
	 */
	private final ReactiveRequest.Builder builder;


	public JettyClientHttpRequest(Request jettyRequest, DataBufferFactory bufferFactory) {
		this.jettyRequest = jettyRequest;
		this.bufferFactory = bufferFactory;
		this.builder = ReactiveRequest.newBuilder(this.jettyRequest).abortOnCancel(true);
	}


	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.jettyRequest.getMethod());
	}

	@Override
	public URI getURI() {
		return this.jettyRequest.getURI();
	}

	@Override
	public Mono<Void> setComplete() {
		return doCommit();
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return this.bufferFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getNativeRequest() {
		return (T) this.jettyRequest;
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		// 创建一个 Mono，其中包含了写入响应体的逻辑
		return Mono.<Void>create(sink -> {
					// 从响应体中创建 Flux，并将每个数据缓冲区转换为内容块，然后发送给 sink
					ReactiveRequest.Content content = Flux.from(body)
							.map(buffer -> toContentChunk(buffer, sink))
							.as(chunks -> ReactiveRequest.Content.fromPublisher(chunks, getContentType()));
					// 将内容设置到响应构建器中
					this.builder.content(content);
					// 完成 Mono，表示写入响应体的操作已完成
					sink.success();
				})
				// 执行提交操作并返回
				.then(doCommit());
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		// 使用 writeWith 方法将响应体内容写入响应中
		return writeWith(
				// 将响应体转换为 Flux，并扁平化处理，释放丢弃的 PooledDataBuffer
				Flux.from(body)
						.flatMap(Function.identity())
						.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release));
	}

	private String getContentType() {
		// 获取响应头中的内容类型
		MediaType contentType = getHeaders().getContentType();

		// 如果内容类型不为 null，则返回内容类型的字符串表示形式；否则返回默认的二进制流类型
		return contentType != null ? contentType.toString() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
	}

	private ContentChunk toContentChunk(DataBuffer buffer, MonoSink<Void> sink) {
		return new ContentChunk(buffer.asByteBuffer(), new Callback() {
			@Override
			public void succeeded() {
				DataBufferUtils.release(buffer);
			}

			@Override
			public void failed(Throwable t) {
				// 释放数据缓冲区，确保资源被正确清理
				DataBufferUtils.release(buffer);

				// 向 sink 发送错误信号，表示在处理数据时发生了异常
				sink.error(t);
			}
		});
	}

	@Override
	protected void applyCookies() {
		// 获取所有 Cookie，并将其转换为流，然后扁平化处理
		getCookies().values().stream().flatMap(Collection::stream)
				// 将 Servlet Cookie 转换为 Jetty 的 HttpCookie
				.map(cookie -> new HttpCookie(cookie.getName(), cookie.getValue()))
				// 将每个 HttpCookie 添加到 Jetty 请求中
				.forEach(this.jettyRequest::cookie);
	}

	@Override
	protected void applyHeaders() {
		// 获取响应头
		HttpHeaders headers = getHeaders();

		// 遍历响应头中的键值对，并将其添加到 Jetty 请求头中
		headers.forEach((key, value) -> value.forEach(v -> this.jettyRequest.header(key, v)));

		// 如果响应头中不包含 ACCEPT 键，则向 Jetty 请求头中添加 ACCEPT 键，并设置其值为 "*/*"
		if (!headers.containsKey(HttpHeaders.ACCEPT)) {
			this.jettyRequest.header(HttpHeaders.ACCEPT, "*/*");
		}
	}

	@Override
	protected HttpHeaders initReadOnlyHeaders() {
		return HttpHeaders.readOnlyHttpHeaders(new JettyHeadersAdapter(this.jettyRequest.getHeaders()));
	}

	public ReactiveRequest toReactiveRequest() {
		return this.builder.build();
	}

}
