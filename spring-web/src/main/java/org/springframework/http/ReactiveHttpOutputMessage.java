/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * 一个“反应式”HTTP输出消息，接受作为{@link Publisher}的输出。
 *
 * <p>通常由客户端的HTTP请求或服务器端的HTTP响应实现。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ReactiveHttpOutputMessage extends HttpMessage {

	/**
	 * 返回一个可以用来创建消息体的{@link DataBufferFactory}。
	 *
	 * @return 一个缓冲区工厂
	 * @see #writeWith(Publisher)
	 */
	DataBufferFactory bufferFactory();

	/**
	 * 注册一个操作，在HttpOutputMessage提交之前应用。
	 * <p><strong>注意：</strong>提供的操作必须正确延迟，例如通过{@link Mono#defer}或{@link Mono#fromRunnable}，
	 * 以确保它按正确的顺序执行，相对于其他操作。
	 *
	 * @param action 要应用的操作
	 */
	void beforeCommit(Supplier<? extends Mono<Void>> action);

	/**
	 * 判断HttpOutputMessage是否已提交。
	 *
	 * @return 是否已提交
	 */
	boolean isCommitted();

	/**
	 * 使用给定的{@link Publisher}将消息体写入底层HTTP层。
	 *
	 * @param body 消息体内容的发布者
	 * @return 一个指示完成或错误的{@link Mono}
	 */
	Mono<Void> writeWith(Publisher<? extends DataBuffer> body);

	/**
	 * 使用给定的{@link Publisher}的{@code Publishers}将HttpOutputMessage的消息体写入底层HTTP层，
	 * 在每个{@code Publisher<DataBuffer>}之后刷新。
	 *
	 * @param body 消息体内容的发布者
	 * @return 一个指示完成或错误的{@link Mono}
	 */
	Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body);

	/**
	 * 表示消息处理已完成，允许执行任何清理或处理结束任务，例如将通过{@link #getHeaders()}进行的头更改应用到底层HTTP消息（如果尚未应用）。
	 * <p>此方法应在消息处理结束时自动调用，因此通常应用程序不需要调用它。
	 * 如果多次调用它，不应有任何副作用。
	 *
	 * @return 一个指示完成或错误的{@link Mono}
	 */
	Mono<Void> setComplete();

}
