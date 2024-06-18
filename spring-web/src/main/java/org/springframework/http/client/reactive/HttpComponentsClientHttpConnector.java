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

package org.springframework.http.client.reactive;

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStreamResetException;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.reactive.ReactiveResponseConsumer;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * {@link ClientHttpConnector} 的 Apache HttpComponents HttpClient 5.x 实现。
 *
 * <p>通过 Apache HttpComponents 提供 HTTP 客户端连接器的实现。
 *
 * @author Martin Tarjányi
 * @author Arjen Poutsma
 * @see <a href="https://hc.apache.org/index.html">Apache HttpComponents</a>
 * @since 5.3
 */
public class HttpComponentsClientHttpConnector implements ClientHttpConnector, Closeable {
	/**
	 * 客户端
	 */
	private final CloseableHttpAsyncClient client;

	/**
	 * 上下文提供者
	 */
	private final BiFunction<HttpMethod, URI, ? extends HttpClientContext> contextProvider;

	/**
	 * 数据缓冲区工厂
	 */
	private DataBufferFactory dataBufferFactory = DefaultDataBufferFactory.sharedInstance;


	/**
	 * 默认构造函数，创建并启动一个新的 {@link CloseableHttpAsyncClient} 实例。
	 */
	public HttpComponentsClientHttpConnector() {
		this(HttpAsyncClients.createDefault());
	}

	/**
	 * 使用预配置的 {@link CloseableHttpAsyncClient} 实例的构造函数。
	 *
	 * @param client 要使用的客户端
	 */
	public HttpComponentsClientHttpConnector(CloseableHttpAsyncClient client) {
		this(client, (method, uri) -> HttpClientContext.create());
	}

	/**
	 * 使用预配置的 {@link CloseableHttpAsyncClient} 实例和每次请求前调用的 {@link HttpClientContext} 提供者的构造函数。
	 *
	 * @param client          要使用的客户端
	 * @param contextProvider 一个 {@link HttpClientContext} 提供者
	 */
	public HttpComponentsClientHttpConnector(CloseableHttpAsyncClient client,
											 BiFunction<HttpMethod, URI, ? extends HttpClientContext> contextProvider) {

		Assert.notNull(client, "Client must not be null");
		Assert.notNull(contextProvider, "ContextProvider must not be null");

		this.contextProvider = contextProvider;
		this.client = client;
		this.client.start();
	}


	/**
	 * 设置要使用的缓冲区工厂。
	 *
	 * @param bufferFactory 缓冲区工厂
	 */
	public void setBufferFactory(DataBufferFactory bufferFactory) {
		this.dataBufferFactory = bufferFactory;
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
											Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		// 从上下文提供者中获取 Http客户端上下文 对象
		HttpClientContext context = this.contextProvider.apply(method, uri);

		// 如果上下文中未设置 Cookie 存储
		if (context.getCookieStore() == null) {
			// 创建一个新的 BasicCookieStore 并设置到上下文中
			context.setCookieStore(new BasicCookieStore());
		}

		// 创建一个 HttpComponentsClientHttpRequest 对象，传入方法、URI、上下文和数据缓冲工厂
		HttpComponentsClientHttpRequest request = new HttpComponentsClientHttpRequest(method, uri, context, this.dataBufferFactory);

		// 应用请求回调，对请求进行配置，然后延迟执行请求，并返回一个 Mono 对象
		return requestCallback.apply(request).then(Mono.defer(() -> execute(request, context)));
	}

	private Mono<ClientHttpResponse> execute(HttpComponentsClientHttpRequest request, HttpClientContext context) {
		// 将请求对象转换为请求生产者
		AsyncRequestProducer requestProducer = request.toRequestProducer();

		return Mono.create(sink -> {
			// 创建一个 ReactiveResponseConsumer 对象，用于处理 HTTP 响应
			ReactiveResponseConsumer reactiveResponseConsumer =
					new ReactiveResponseConsumer(new MonoFutureCallbackAdapter(sink, this.dataBufferFactory, context));

			// 传入请求生产者、响应消费者、上下文和回调参数  执行 HTTP 请求
			this.client.execute(requestProducer, reactiveResponseConsumer, context, null);
		});
	}

	@Override
	public void close() throws IOException {
		this.client.close();
	}

	private static class MonoFutureCallbackAdapter
			implements FutureCallback<Message<HttpResponse, Publisher<ByteBuffer>>> {

		/**
		 * 用于处理响应的 {@link MonoSink} 实例。
		 */
		private final MonoSink<ClientHttpResponse> sink;

		/**
		 * 用于创建数据缓冲区的工厂。
		 */
		private final DataBufferFactory dataBufferFactory;

		/**
		 * 用于请求的上下文实例。
		 */
		private final HttpClientContext context;

		public MonoFutureCallbackAdapter(MonoSink<ClientHttpResponse> sink,
										 DataBufferFactory dataBufferFactory, HttpClientContext context) {
			this.sink = sink;
			this.dataBufferFactory = dataBufferFactory;
			this.context = context;
		}

		@Override
		public void completed(Message<HttpResponse, Publisher<ByteBuffer>> result) {
			// 传入数据缓冲工厂、结果和上下文，创建一个 HttpComponentsClientHttpResponse 对象
			HttpComponentsClientHttpResponse response =
					new HttpComponentsClientHttpResponse(this.dataBufferFactory, result, this.context);

			// 使用 sink 对象将响应对象传递出去，表示操作成功
			this.sink.success(response);
		}

		@Override
		public void failed(Exception ex) {
			Throwable t = ex;

			// 检查异常是否为 HttpStreamResetException 类型
			if (t instanceof HttpStreamResetException) {
				// 将异常强制转换为 HttpStreamResetException 类型
				HttpStreamResetException httpStreamResetException = (HttpStreamResetException) ex;
				// 获取 HttpStreamResetException 的原因
				t = httpStreamResetException.getCause();
			}

			// 使用 sink 对象将处理后的异常传递出去，表示操作失败
			this.sink.error(t);
		}

		@Override
		public void cancelled() {
		}
	}

}
