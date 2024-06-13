/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.client;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpRequest;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;

/**
 * 表示客户端异步HTTP请求。通过 {@link AsyncClientHttpRequestFactory} 的实现创建。
 *
 * <p>{@code AsyncHttpRequest} 可以被 {@linkplain #executeAsync() 执行}，
 * 获取一个 {@link ClientHttpResponse} 的 future，可以从中读取响应。
 *
 * @author Arjen Poutsma
 * @see AsyncClientHttpRequestFactory#createAsyncRequest
 * @since 4.0
 * @deprecated 自 Spring 5.0 起，推荐使用 {@link org.springframework.web.reactive.function.client.ClientRequest}
 */
@Deprecated
public interface AsyncClientHttpRequest extends HttpRequest, HttpOutputMessage {

	/**
	 * 异步执行此请求，返回一个 Future 处理器，可以用来获取 {@link ClientHttpResponse} 并读取响应。
	 *
	 * @return 执行结果的 future 响应
	 * @throws java.io.IOException 如果发生 I/O 错误
	 */
	ListenableFuture<ClientHttpResponse> executeAsync() throws IOException;

}
