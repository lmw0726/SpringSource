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

import org.springframework.http.HttpRequest;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;

/**
 * 拦截客户端 HTTP 请求。实现该接口的类可以注册到 {@link org.springframework.web.client.AsyncRestTemplate}，
 * 以修改传出的 {@link HttpRequest}，并且可以注册以修改传入的 {@link ClientHttpResponse}，
 * 使用 {@link org.springframework.util.concurrent.ListenableFutureAdapter} 进行适配。
 *
 * <p>拦截器的主要入口点是 {@link #intercept} 方法。
 *
 * @author Jakub Narloch
 * @author Rossen Stoyanchev
 * @see org.springframework.web.client.AsyncRestTemplate
 * @see org.springframework.http.client.support.InterceptingAsyncHttpAccessor
 * @since 4.3
 * @deprecated 自 Spring 5.0，推荐使用 {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction}
 */
@Deprecated
public interface AsyncClientHttpRequestInterceptor {

	/**
	 * 拦截给定的请求，并返回一个响应的 Future。给定的 {@link AsyncClientHttpRequestExecution} 允许拦截器
	 * 将请求传递给链中的下一个实体。
	 *
	 * <p>实现可能会遵循以下模式：
	 * <ol>
	 * <li>检查 {@linkplain HttpRequest 请求} 和主体</li>
	 * <li>可选择 {@linkplain org.springframework.http.client.support.HttpRequestWrapper
	 * 包装} 请求以过滤 HTTP 属性。</li>
	 * <li>可选择修改请求的主体。</li>
	 * <li>以下操作之一：
	 * <ul>
	 * <li>通过 {@link ClientHttpRequestExecution} 执行请求</li>
	 * <li>不执行请求以完全阻塞执行</li>
	 * </ul>
	 * <li>可选择使用 {@link org.springframework.util.concurrent.ListenableFutureAdapter
	 * ListenableFutureAdapter} 适配器来过滤响应的 HTTP 属性。</li>
	 * </ol>
	 *
	 * @param request   请求，包含方法、URI 和头部信息
	 * @param body      请求的主体
	 * @param execution 请求执行
	 * @return 响应的 Future
	 * @throws IOException 如果发生 I/O 错误
	 */
	ListenableFuture<ClientHttpResponse> intercept(HttpRequest request, byte[] body,
												   AsyncClientHttpRequestExecution execution) throws IOException;

}
