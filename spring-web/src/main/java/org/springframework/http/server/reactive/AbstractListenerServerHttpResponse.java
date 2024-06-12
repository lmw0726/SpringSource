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

package org.springframework.http.server.reactive;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用于基于监听器的服务器响应的抽象基类，例如 Servlet 3.1 和 Undertow。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class AbstractListenerServerHttpResponse extends AbstractServerHttpResponse {
	/**
	 * 写入方法是否已经被调用过了
	 */
	private final AtomicBoolean writeCalled = new AtomicBoolean();


	public AbstractListenerServerHttpResponse(DataBufferFactory bufferFactory) {
		super(bufferFactory);
	}

	public AbstractListenerServerHttpResponse(DataBufferFactory bufferFactory, HttpHeaders headers) {
		super(bufferFactory, headers);
	}


	@Override
	protected final Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
		return writeAndFlushWithInternal(Mono.just(body));
	}

	@Override
	protected final Mono<Void> writeAndFlushWithInternal(
			Publisher<? extends Publisher<? extends DataBuffer>> body) {

		// 如果 writeWith() 或 writeAndFlushWith() 方法已经被调用过
		if (!this.writeCalled.compareAndSet(false, true)) {
			// 返回一个错误的 Mono，抛出 IllegalStateException 异常
			return Mono.error(new IllegalStateException(
					"writeWith() or writeAndFlushWith() has already been called"));
		}

		// 创建用于处理请求体的处理器
		Processor<? super Publisher<? extends DataBuffer>, Void> processor = createBodyFlushProcessor();

		// 创建一个新的 Mono，并在订阅时执行操作
		return Mono.from(subscriber -> {
			// 订阅请求体，将数据发送给处理器
			body.subscribe(processor);
			// 处理器订阅订阅者，将数据传递给订阅者
			processor.subscribe(subscriber);
		});
	}

	/**
	 * 抽象模板方法，用于创建一个 {@code Processor<Publisher<DataBuffer>, Void>}，
	 * 它将使用刷新将响应体写入底层输出。从 {@link #writeAndFlushWithInternal(Publisher)} 调用。
	 */
	protected abstract Processor<? super Publisher<? extends DataBuffer>, Void> createBodyFlushProcessor();

}
