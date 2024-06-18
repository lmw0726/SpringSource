/*
 * Copyright 2002-2018 the original author or authors.
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
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.Function;

/**
 * HTTP 客户端连接器的抽象，驱动底层的 HTTP 客户端连接到原始服务器，并提供发送 {@link ClientHttpRequest} 和接收 {@link ClientHttpResponse} 所需的所有基础设施。
 *
 * @author Brian Clozel
 * @since 5.0
 */
public interface ClientHttpConnector {

	/**
	 * 使用给定的 {@code HttpMethod} 和 {@code URI} 连接到原始服务器，并在底层 API 的 HTTP 请求可以初始化并写入时应用给定的 {@code requestCallback}。
	 *
	 * @param method          HTTP 请求方法
	 * @param uri             HTTP 请求 URI
	 * @param requestCallback 准备并写入请求的函数，返回一个发布者，表示写入完成的信号。
	 *                        实现可以通过调用 {@link ClientHttpRequest#writeWith} 或 {@link ClientHttpRequest#setComplete} 返回 {@code Mono<Void>}。
	 * @return {@link ClientHttpResponse} 的发布者
	 */
	Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
									 Function<? super ClientHttpRequest, Mono<Void>> requestCallback);

}
