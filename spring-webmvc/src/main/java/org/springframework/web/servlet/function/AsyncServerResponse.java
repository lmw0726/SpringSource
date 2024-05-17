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

package org.springframework.web.servlet.function;

import org.reactivestreams.Publisher;
import org.springframework.core.ReactiveAdapterRegistry;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * {@link ServerResponse} 的异步子类型，公开了 future 响应。
 *
 * @author Arjen Poutsma
 * @see ServerResponse#async(Object)
 * @since 5.3.2
 */
public interface AsyncServerResponse extends ServerResponse {

	/**
	 * 阻塞直到获得 future 响应。
	 */
	ServerResponse block();


	// 静态创建方法

	/**
	 * 使用给定的异步响应创建一个 {@code AsyncServerResponse}。
	 * 参数 {@code asyncResponse} 可以是
	 * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;} 或
	 * {@link Publisher Publisher&lt;ServerResponse&gt;}（或者任何
	 * 异步生成单个 {@code ServerResponse} 的生产者，可以通过 {@link ReactiveAdapterRegistry} 适配）。
	 *
	 * @param asyncResponse {@code CompletableFuture<ServerResponse>} 或 {@code Publisher<ServerResponse>}
	 * @return 异步响应
	 */
	static AsyncServerResponse create(Object asyncResponse) {
		return DefaultAsyncServerResponse.create(asyncResponse, null);
	}

	/**
	 * 使用给定的异步响应创建一个（已构建的）响应。
	 * 参数 {@code asyncResponse} 可以是
	 * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;} 或
	 * {@link Publisher Publisher&lt;ServerResponse&gt;}（或者任何
	 * 异步生成单个 {@code ServerResponse} 的生产者，可以通过 {@link ReactiveAdapterRegistry} 适配）。
	 *
	 * @param asyncResponse {@code CompletableFuture<ServerResponse>} 或 {@code Publisher<ServerResponse>}
	 * @param timeout       最长等待超时时间
	 * @return 异步响应
	 */
	static AsyncServerResponse create(Object asyncResponse, Duration timeout) {
		return DefaultAsyncServerResponse.create(asyncResponse, timeout);
	}

}

