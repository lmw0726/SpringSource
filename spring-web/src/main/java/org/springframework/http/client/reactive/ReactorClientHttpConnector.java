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

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * {@link ClientHttpConnector} 的 Reactor-Netty 实现。
 *
 * <p>通过 {@link reactor.netty.http.client.HttpClient} 实现 HTTP 客户端连接器。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see reactor.netty.http.client.HttpClient
 * @since 5.0
 */
public class ReactorClientHttpConnector implements ClientHttpConnector {
	/**
	 * 默认初始化器
	 */
	private final static Function<HttpClient, HttpClient> defaultInitializer = client -> client.compress(true);

	/**
	 * Http客户端
	 */
	private final HttpClient httpClient;


	/**
	 * 默认构造函数。通过以下方式初始化 {@link HttpClient}：
	 * <pre class="code">
	 * HttpClient.create().compress()
	 * </pre>
	 */
	public ReactorClientHttpConnector() {
		this.httpClient = defaultInitializer.apply(HttpClient.create());
	}

	/**
	 * 使用外部管理的 Reactor Netty 资源（包括事件循环线程的 {@link LoopResources} 和连接池的 {@link ConnectionProvider}）的构造函数。
	 * <p>仅当您不希望客户端参与 Reactor Netty 全局资源时才应使用此构造函数。
	 * 默认情况下，客户端参与 Reactor Netty 全局资源，这些资源由 {@link reactor.netty.http.HttpResources} 管理，
	 * 推荐使用固定的共享资源以提高事件循环并发性能。
	 * 考虑在 Spring ApplicationContext 中声明一个带有 {@code globalResources=true} 的 {@link ReactorResourceFactory} bean，
	 * 以确保在关闭 Spring ApplicationContext 时关闭 Reactor Netty 全局资源。
	 *
	 * @param factory 资源工厂用于获取资源
	 * @param mapper  用于进一步初始化创建的客户端的映射器
	 * @since 5.1
	 */
	public ReactorClientHttpConnector(ReactorResourceFactory factory, Function<HttpClient, HttpClient> mapper) {
		// 从工厂中获取连接提供者
		ConnectionProvider provider = factory.getConnectionProvider();

		Assert.notNull(provider, "No ConnectionProvider: is ReactorResourceFactory not initialized yet?");

		// 创建 Http客户端 实例，并通过函数组合的方式对其进行初始化
		this.httpClient = defaultInitializer
				.andThen(mapper)
				.andThen(applyLoopResources(factory))
				.apply(HttpClient.create(provider));
	}

	private static Function<HttpClient, HttpClient> applyLoopResources(ReactorResourceFactory factory) {
		return httpClient -> {
			// 从工厂获取循环资源
			LoopResources resources = factory.getLoopResources();

			// 断言循环资源不为 null，否则抛出异常
			Assert.notNull(resources, "No LoopResources: is ReactorResourceFactory not initialized yet?");

			// 使用获取到的循环资源来配置传入的 Http客户端，并返回配置后的 Http客户端
			return httpClient.runOn(resources);
		};
	}


	/**
	 * 使用预配置的 {@code HttpClient} 实例的构造函数。
	 *
	 * @param httpClient 要使用的客户端
	 * @since 5.1
	 */
	public ReactorClientHttpConnector(HttpClient httpClient) {
		Assert.notNull(httpClient, "HttpClient is required");
		this.httpClient = httpClient;
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
											Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		// 创建一个 AtomicReference，用于持有响应对象
		AtomicReference<ReactorClientHttpResponse> responseRef = new AtomicReference<>();

		// 使用 http客户端 发起请求，并链式配置请求和处理逻辑
		return this.httpClient
				// 指定 HTTP 方法
				.request(io.netty.handler.codec.http.HttpMethod.valueOf(method.name()))
				// 设置请求的 URI
				.uri(uri.toString())
				// 发送请求，并在发送前应用 请求回调 来修改请求
				.send((request, outbound) -> requestCallback.apply(adaptRequest(method, uri, request, outbound)))
				// 处理响应连接，将响应包装成 ReactorClientHttpResponse，并设置到 响应引用 中
				.responseConnection((response, connection) -> {
					responseRef.set(new ReactorClientHttpResponse(response, connection));
					// 返回一个 Mono，包含响应对象
					return Mono.just((ClientHttpResponse) responseRef.get());
				})
				// 继续处理下一个步骤
				.next()
				// 在取消时执行的操作
				.doOnCancel(() -> {
					ReactorClientHttpResponse response = responseRef.get();
					if (response != null) {
						// 如果存在响应对象，释放资源
						response.releaseAfterCancel(method);
					}
				});
	}

	private ReactorClientHttpRequest adaptRequest(HttpMethod method, URI uri, HttpClientRequest request,
												  NettyOutbound nettyOutbound) {

		return new ReactorClientHttpRequest(method, uri, request, nettyOutbound);
	}

}
