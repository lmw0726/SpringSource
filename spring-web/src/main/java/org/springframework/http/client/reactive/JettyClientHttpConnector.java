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

package org.springframework.http.client.reactive;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Jetty Reactive Streams HttpClient 的 {@link ClientHttpConnector} 实现。
 *
 * <p>此类用于连接 Jetty Reactive Streams HttpClient 到原始服务器，
 * 并提供发送 {@link ClientHttpRequest} 和接收 {@link ClientHttpResponse} 所需的基础设施。
 *
 * @author Sebastien Deleuze
 * @see <a href="https://github.com/jetty-project/jetty-reactive-httpclient">Jetty ReactiveStreams HttpClient</a>
 * @since 5.1
 */
public class JettyClientHttpConnector implements ClientHttpConnector {
	/**
	 * Http客户端
	 */
	private final HttpClient httpClient;

	/**
	 * 数据缓冲区工厂
	 */
	private DataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;


	/**
	 * 默认构造函数，创建一个新的 {@link HttpClient} 实例。
	 */
	public JettyClientHttpConnector() {
		this(new HttpClient());
	}

	/**
	 * 使用初始化的 {@link HttpClient} 构造函数。
	 *
	 * @param httpClient 要使用的 {@link HttpClient}
	 */
	public JettyClientHttpConnector(HttpClient httpClient) {
		this(httpClient, null);
	}

	/**
	 * 使用初始化的 {@link HttpClient} 构造函数，并配置给定的 {@link JettyResourceFactory}。
	 *
	 * @param httpClient      要使用的 {@link HttpClient}
	 * @param resourceFactory 要使用的 {@link JettyResourceFactory}
	 * @since 5.2
	 */
	public JettyClientHttpConnector(HttpClient httpClient, @Nullable JettyResourceFactory resourceFactory) {
		Assert.notNull(httpClient, "HttpClient is required");
		// 如果 资源工厂 不为空
		if (resourceFactory != null) {
			// 从中获取并设置 执行器
			httpClient.setExecutor(resourceFactory.getExecutor());

			// 从 资源工厂 中获取并设置字节缓冲池，
			httpClient.setByteBufferPool(resourceFactory.getByteBufferPool());

			// 从 资源工厂 中获取并设置调度器
			httpClient.setScheduler(resourceFactory.getScheduler());
		}

		// 设置配置好的 http客户端
		this.httpClient = httpClient;
	}

	/**
	 * 使用将管理共享资源的 {@link JettyResourceFactory} 构造函数。
	 *
	 * @param resourceFactory 要使用的 {@link JettyResourceFactory}
	 * @param customizer      用于自定义 {@link HttpClient} 的 Lambda 表达式
	 * @deprecated 自 5.2 起弃用，请使用 {@link JettyClientHttpConnector#JettyClientHttpConnector(HttpClient, JettyResourceFactory)}
	 */
	@Deprecated
	public JettyClientHttpConnector(JettyResourceFactory resourceFactory, @Nullable Consumer<HttpClient> customizer) {
		this(new HttpClient(), resourceFactory);
		if (customizer != null) {
			// 设置自定义的 http 客户端
			customizer.accept(this.httpClient);
		}
	}


	/**
	 * 设置要使用的缓冲区工厂。
	 *
	 * @param bufferFactory 要使用的缓冲区工厂
	 */
	public void setBufferFactory(DataBufferFactory bufferFactory) {
		this.bufferFactory = bufferFactory;
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
											Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {
		// 如果URI不是绝对路径
		if (!uri.isAbsolute()) {
			// 则返回一个带有错误信息的Mono
			return Mono.error(new IllegalArgumentException("URI is not absolute: " + uri));
		}

		// 如果httpClient尚未启动
		if (!this.httpClient.isStarted()) {
			try {
				// 启动Http客户端
				this.httpClient.start();
			} catch (Exception ex) {
				// 启动过程中发生异常，则返回一个带有异常的Mono
				return Mono.error(ex);
			}
		}

		// 设置请求方法和URI并创建一个Jetty请求对象
		Request jettyRequest = this.httpClient.newRequest(uri).method(method.toString());

		// 使用 封装Jetty的请求对象和数据缓冲工厂， 创建 Jetty客户端Http请求 对象
		JettyClientHttpRequest request = new JettyClientHttpRequest(jettyRequest, this.bufferFactory);

		// 应用请求回调对请求进行进一步的处理，然后执行请求并返回执行结果的Mono
		return requestCallback.apply(request).then(execute(request));
	}

	private Mono<ClientHttpResponse> execute(JettyClientHttpRequest request) {
		return Mono.fromDirect(request.toReactiveRequest()
				.response((reactiveResponse, chunkPublisher) -> {
					// 将 分块发布者 转换为Flux<DataBuffer>，每个分块被映射为一个DataBuffer
					Flux<DataBuffer> content = Flux.from(chunkPublisher).map(this::toDataBuffer);
					// 返回一个包含 Jetty客户端Http响应 的 Mono
					return Mono.just(new JettyClientHttpResponse(reactiveResponse, content));
				}));
	}

	private DataBuffer toDataBuffer(ContentChunk chunk) {

		// 原始的拷贝操作，由于以下问题导致：
		// https://github.com/eclipse/jetty.project/issues/2429
		// 现在问题已经标记为已修复，我们需要用一个PooledDataBuffer来替换下面的操作，
		// 它将"release()"适配为"succeeded()"，并且评估这里的相关问题是否得到了解决。

		// 使用工厂方法 缓冲工厂 创建一个新的数据缓冲区
		DataBuffer buffer = this.bufferFactory.allocateBuffer(chunk.buffer.capacity());

		// 将 分块缓冲 中的数据写入新分配的缓冲区。
		buffer.write(chunk.buffer);

		// 执行回调，表示操作成功完成。
		chunk.callback.succeeded();

		// 返回填充了数据的缓冲区对象。
		return buffer;
	}

}
