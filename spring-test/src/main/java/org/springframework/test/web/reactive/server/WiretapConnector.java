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

package org.springframework.test.web.reactive.server;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * 用于拦截、捕获并公开传输到服务器和从服务器接收的实际请求和响应数据的 {@link ClientHttpConnector} 装饰器。
 *
 * <p>此装饰器通过拦截 {@link ClientHttpConnector} 的连接方法，包装请求和响应，以便记录和公开交换的详细信息。
 *
 * <p>它维护一个 {@link ConcurrentHashMap} 来存储每个请求的 {@link ClientExchangeInfo}，以便后续查询。
 *
 * <p>注意：此类是线程安全的，可以安全地在多线程环境中使用。
 *
 * @author Rossen Stoyanchev
 * @see HttpHandlerConnector
 * @since 5.0
 */
class WiretapConnector implements ClientHttpConnector {
	/**
	 * 客户端连接器
	 */
	private final ClientHttpConnector delegate;

	/**
	 * 请求编号 —— 客户端交换信息 映射
	 */
	private final Map<String, ClientExchangeInfo> exchanges = new ConcurrentHashMap<>();


	/**
	 * 构造一个 {@link WiretapConnector} 实例，装饰指定的 {@link ClientHttpConnector} 实例。
	 *
	 * @param delegate 要装饰的 {@link ClientHttpConnector} 实例
	 */
	WiretapConnector(ClientHttpConnector delegate) {
		this.delegate = delegate;
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
											Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		// 创建一个 AtomicReference 用于持有 窃听客户端Http请求 对象的引用
		AtomicReference<WiretapClientHttpRequest> requestRef = new AtomicReference<>();

		// 调用 代理者 的 connect 方法，并返回结果的流
		return this.delegate
				.connect(method, uri, request -> {
					// 包装请求为 窃听客户端Http请求
					WiretapClientHttpRequest wrapped = new WiretapClientHttpRequest(request);
					// 将 包装请求 设置到 请求引用 中
					requestRef.set(wrapped);
					// 调用 请求回调 应用方法，并返回结果
					return requestCallback.apply(wrapped);
				})
				.map(response -> {
					// 获取 请求引用 中的 窃听客户端Http请求
					WiretapClientHttpRequest wrappedRequest = requestRef.get();
					// 获取请求中的 请求编号
					String header = WebTestClient.WEBTESTCLIENT_REQUEST_ID;
					String requestId = wrappedRequest.getHeaders().getFirst(header);
					// 使用断言确保 请求编号 不为 null，否则抛出异常
					Assert.state(requestId != null, () -> "No \"" + header + "\" header");
					// 包装响应为 窃听客户端Http响应
					WiretapClientHttpResponse wrappedResponse = new WiretapClientHttpResponse(response);
					// 将请求 请求编号 和对应的 客户端交换信息 放入 交换信息映射 中
					this.exchanges.put(requestId, new ClientExchangeInfo(wrappedRequest, wrappedResponse));
					// 返回 包装响应 中
					return wrappedResponse;
				});
	}

	/**
	 * 根据给定的 "request-id" 头信息创建 {@link ExchangeResult}。
	 *
	 * @param requestId   请求的唯一标识符，通常是 "request-id" 头的值
	 * @param uriTemplate URI 模板信息，可为空
	 * @param timeout     获取交换结果的超时时间
	 * @return 根据请求标识符创建的 {@link ExchangeResult}
	 * @throws IllegalStateException 如果没有找到与请求标识符匹配的交换信息
	 */
	ExchangeResult getExchangeResult(String requestId, @Nullable String uriTemplate, Duration timeout) {
		// 从 交换映射 中移除对应 请求编号 的 客户端交换信息 对象，并获取 客户端交换信息
		ClientExchangeInfo clientInfo = this.exchanges.remove(requestId);
		// 使用断言确保 客户端交换信息 不为 null，否则抛出异常
		Assert.state(clientInfo != null, () -> {
			// 构造异常信息，指明找不到匹配的 请求编号
			String header = WebTestClient.WEBTESTCLIENT_REQUEST_ID;
			return "No match for " + header + "=" + requestId;
		});
		// 返回一个新的 ExchangeResult 对象，包含请求信息、响应信息、请求内容、响应内容、超时时间、URI模板和Mock服务器结果
		return new ExchangeResult(clientInfo.getRequest(), clientInfo.getResponse(),
				clientInfo.getRequest().getRecorder().getContent(),
				clientInfo.getResponse().getRecorder().getContent(),
				timeout, uriTemplate,
				clientInfo.getResponse().getMockServerResult());
	}


	/**
	 * {@link WiretapClientHttpRequest} 和 {@link WiretapClientHttpResponse} 的持有者。
	 */
	private static class ClientExchangeInfo {
		/**
		 * 窃听客户端Http请求
		 */
		private final WiretapClientHttpRequest request;

		/**
		 * 窃听客户端Http响应
		 */
		private final WiretapClientHttpResponse response;

		public ClientExchangeInfo(WiretapClientHttpRequest request, WiretapClientHttpResponse response) {
			this.request = request;
			this.response = response;
		}

		public WiretapClientHttpRequest getRequest() {
			return this.request;
		}

		public WiretapClientHttpResponse getResponse() {
			return this.response;
		}
	}


	/**
	 * 点击数据缓冲区的发布者以保存内容。
	 */
	final static class WiretapRecorder {
		/**
		 * 发布者
		 */
		@Nullable
		private final Flux<? extends DataBuffer> publisher;

		/**
		 * 嵌套的发布者
		 */
		@Nullable
		private final Flux<? extends Publisher<? extends DataBuffer>> publisherNested;

		/**
		 * 数据缓冲
		 */
		private final DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.allocateBuffer();

		/**
		 * 内容
		 * unsafe(): 我们正在拦截，已经序列化的发布者信号
		 */
		private final Sinks.One<byte[]> content = Sinks.unsafe().one();

		/**
		 * 是否有内容消费者
		 */
		private boolean hasContentConsumer;


		public WiretapRecorder(@Nullable Publisher<? extends DataBuffer> publisher,
							   @Nullable Publisher<? extends Publisher<? extends DataBuffer>> publisherNested) {

			if (publisher != null && publisherNested != null) {
				// 如果两个发布者者都不为空，则抛出异常
				throw new IllegalArgumentException("At most one publisher expected");
			}

			// 处理第一个发布者
			this.publisher = publisher != null ?
					Flux.from(publisher)
							// 订阅时标记已有内容消费者
							.doOnSubscribe(s -> this.hasContentConsumer = true)
							// 每次接收数据时写入缓冲区
							.doOnNext(this.buffer::write)
							// 处理错误时调用 handleOnError 方法
							.doOnError(this::handleOnError)
							// 取消订阅时调用 handleOnComplete 方法
							.doOnCancel(this::handleOnComplete)
							// 完成时调用 handleOnComplete 方法
							.doOnComplete(this::handleOnComplete) : null;

			// 处理嵌套发布者
			this.publisherNested = publisherNested != null ?
					Flux.from(publisherNested)
							// 订阅时标记已有内容消费者
							.doOnSubscribe(s -> this.hasContentConsumer = true)
							// 每次接收数据时写入缓冲区
							// 处理错误时调用 handleOnError 方法
							.map(p -> Flux.from(p).doOnNext(this.buffer::write).doOnError(this::handleOnError))
							// 处理错误时调用 handleOnError 方法
							.doOnError(this::handleOnError)
							// 取消订阅时调用 handleOnComplete 方法
							.doOnCancel(this::handleOnComplete)
							// 完成时调用 handleOnComplete 方法
							.doOnComplete(this::handleOnComplete) : null;

			// 如果两个发布者都为 null，则尝试发出空信号
			if (publisher == null && publisherNested == null) {
				this.content.tryEmitEmpty();
			}
		}


		/**
		 * 获取用于直接拦截 {@link Publisher} 的数据流。
		 *
		 * @return 用于直接拦截 {@link Publisher} 的数据流
		 * @throws IllegalArgumentException 如果没有使用 {@link Publisher} 拦截模式
		 */
		public Publisher<? extends DataBuffer> getPublisherToUse() {
			Assert.notNull(this.publisher, "Publisher not in use.");
			return this.publisher;
		}

		/**
		 * 获取用于拦截嵌套的 {@link Publisher} 的数据流。
		 *
		 * @return 用于拦截嵌套的 {@link Publisher} 的数据流
		 * @throws IllegalArgumentException 如果没有使用嵌套的 {@link Publisher} 拦截模式
		 */
		public Publisher<? extends Publisher<? extends DataBuffer>> getNestedPublisherToUse() {
			Assert.notNull(this.publisherNested, "Nested publisher not in use.");
			return this.publisherNested;
		}

		/**
		 * 获取一个 {@link Mono}，异步获取已保存的数据内容。
		 *
		 * @return 异步获取已保存的数据内容的 {@link Mono}
		 */
		public Mono<byte[]> getContent() {
			return Mono.defer(() -> {
				// 检查内容流是否已终止
				if (this.content.scan(Scannable.Attr.TERMINATED) == Boolean.TRUE) {
					// 如果已终止，直接返回内容流的 Mono
					return this.content.asMono();
				}

				// 如果没有消费者，并且出现了以下几种可能的情况：
				//  1. Mock 服务器在读取前发生了错误，导致请求体未被消费
				//  2. FluxExchangeResult: 在获取响应体之前调用了 getResponseBodyContent
				if (!this.hasContentConsumer) {
					// 忽略常量条件警告，根据情况选择合适的发布者进行错误映射，并订阅它
					(this.publisher != null ? this.publisher : this.publisherNested)
							.onErrorMap(ex -> new IllegalStateException(
									"Content has not been consumed, and " +
											"an error was raised while attempting to produce it.", ex))
							.subscribe();
				}

				// 返回内容流的 Mono
				return this.content.asMono();
			});
		}

		/**
		 * 处理发生错误时的回调。
		 *
		 * @param ex 发生的异常
		 */
		private void handleOnError(Throwable ex) {
			// 忽略结果: 信号无法竞争
			this.content.tryEmitError(ex);
		}

		/**
		 * 处理完成时的回调。
		 */
		private void handleOnComplete() {
			// 创建一个与缓冲区可读字节数相同大小的字节数组。
			byte[] bytes = new byte[this.buffer.readableByteCount()];

			// 从缓冲区中读取数据，并将其写入字节数组 。
			this.buffer.read(bytes);

			// 发出字节数组的值给订阅者，忽略结果。
			this.content.tryEmitValue(bytes);
		}
	}


	/**
	 * 拦截并保存请求体的 ClientHttpRequestDecorator。
	 */
	private static class WiretapClientHttpRequest extends ClientHttpRequestDecorator {
		/**
		 * 窃听记录器
		 */
		@Nullable
		private WiretapRecorder recorder;


		public WiretapClientHttpRequest(ClientHttpRequest delegate) {
			super(delegate);
		}

		public WiretapRecorder getRecorder() {
			Assert.notNull(this.recorder, "No WiretapRecorder: was the client request written?");
			return this.recorder;
		}

		@Override
		public Mono<Void> writeWith(Publisher<? extends DataBuffer> publisher) {
			// 创建一个 WiretapRecorder 实例来记录发布者的数据
			this.recorder = new WiretapRecorder(publisher, null);

			// 使用记录器中的发布者来写入响应
			return super.writeWith(this.recorder.getPublisherToUse());
		}

		@Override
		public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> publisher) {
			// 创建一个 WiretapRecorder 实例来记录发布者的数据
			this.recorder = new WiretapRecorder(null, publisher);

			// 使用记录器中的嵌套发布者来写入并刷新响应
			return super.writeAndFlushWith(this.recorder.getNestedPublisherToUse());
		}

		@Override
		public Mono<Void> setComplete() {
			// 创建一个 WiretapRecorder 实例，但不记录任何数据
			this.recorder = new WiretapRecorder(null, null);

			// 设置响应为已完成状态
			return super.setComplete();
		}
	}


	/**
	 * 拦截并保存响应体的 ClientHttpResponseDecorator。
	 */
	private static class WiretapClientHttpResponse extends ClientHttpResponseDecorator {
		/**
		 * 窃听记录器
		 */
		private final WiretapRecorder recorder;


		public WiretapClientHttpResponse(ClientHttpResponse delegate) {
			super(delegate);
			this.recorder = new WiretapRecorder(super.getBody(), null);
		}


		public WiretapRecorder getRecorder() {
			return this.recorder;
		}

		@Override
		@SuppressWarnings("ConstantConditions")
		public Flux<DataBuffer> getBody() {
			return Flux.from(this.recorder.getPublisherToUse());
		}

		@Nullable
		public Object getMockServerResult() {
			// 检查委托对象是否为 MockServerClientHttpResponse 类型，如果是，则返回其服务器结果；
			// 否则返回 null
			return (getDelegate() instanceof MockServerClientHttpResponse ?
					((MockServerClientHttpResponse) getDelegate()).getServerResult() : null);
		}
	}

}
