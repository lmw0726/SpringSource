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

package org.springframework.http.server.reactive;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;

/**
 * 用于 HTTP HEAD 请求的 {@link ServerHttpResponse} 装饰器。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HttpHeadResponseDecorator extends ServerHttpResponseDecorator {


	public HttpHeadResponseDecorator(ServerHttpResponse delegate) {
		super(delegate);
	}


	/**
	 * 消费并释放 body 而不写入。
	 * <p>如果 headers 中既不包含 Content-Length 也不包含 Transfer-Encoding，
	 * 并且 body 是 {@link Mono}，则计算字节数并设置 Content-Length。
	 */
	@Override
	@SuppressWarnings("unchecked")
	public final Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		// 如果应该设置内容长度并且响应体是 Mono 类型
		if (shouldSetContentLength() && body instanceof Mono) {
			return ((Mono<? extends DataBuffer>) body)
					.doOnSuccess(buffer -> {
						// 当缓冲区不为 null 时
						if (buffer != null) {
							// 设置响应头中的内容长度为缓冲区中可读字节数
							getHeaders().setContentLength(buffer.readableByteCount());
							// 释放缓冲区
							DataBufferUtils.release(buffer);
						} else {
							// 否则，设置响应头中的内容长度为 0
							getHeaders().setContentLength(0);
						}
					})
					.then();
		} else {
			// 否则，响应体是 Flux 类型
			return Flux.from(body)
					// 释放每个数据缓冲区
					.doOnNext(DataBufferUtils::release)
					// 返回一个空的 Mono 信号
					.then();
		}
	}

	private boolean shouldSetContentLength() {
		// 检查响应头中是否没有设置内容长度和传输编码
		return (getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH) == null &&
				getHeaders().getFirst(HttpHeaders.TRANSFER_ENCODING) == null);
	}

	/**
	 * 调用 {@link #setComplete()} 而不写入。
	 * <p>RFC 7302 允许不带 content-length 的 HTTP HEAD 响应，
	 * 这不是可以在流式响应上计算的内容。
	 */
	@Override
	public final Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		// 在可能的流式响应上不可行。
		// RFC 7302 允许 HEAD 响应不带 content-length。
		return setComplete();
	}

}
