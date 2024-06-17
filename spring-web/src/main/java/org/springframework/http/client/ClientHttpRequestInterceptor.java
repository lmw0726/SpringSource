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

package org.springframework.http.client;

import org.springframework.http.HttpRequest;

import java.io.IOException;

/**
 * 拦截客户端 HTTP 请求。实现此接口的类可以注册到 {@link org.springframework.web.client.RestTemplate RestTemplate}，
 * 用于修改传出的 {@link ClientHttpRequest} 和/或传入的 {@link ClientHttpResponse}。
 *
 * <p>拦截器的主要入口点是 {@link #intercept(HttpRequest, byte[], ClientHttpRequestExecution)}。
 * <p>
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
@FunctionalInterface
public interface ClientHttpRequestInterceptor {

	/**
	 * 拦截给定的请求，并返回一个响应。给定的 {@link ClientHttpRequestExecution} 允许拦截器将请求和响应传递给链中的下一个实体。
	 * <p>此方法的典型实现会遵循以下模式：
	 * <ol>
	 * <li>检查 {@linkplain HttpRequest 请求} 和请求体。</li>
	 * <li>可选地 {@linkplain org.springframework.http.client.support.HttpRequestWrapper 包装} 请求以过滤 HTTP 属性。</li>
	 * <li>可选地修改请求体。</li>
	 * <ul>
	 * <li><strong>要么</strong>
	 * <li>使用 {@link ClientHttpRequestExecution#execute(org.springframework.http.HttpRequest, byte[])} 执行请求，</li>
	 * <li><strong>要么</strong></li>
	 * <li>不执行请求以完全阻止执行。</li>
	 * </ul>
	 * <li>可选地包装响应以过滤 HTTP 属性。</li>
	 * </ol>
	 *
	 * @param request   请求，包含方法、URI 和头部
	 * @param body      请求体
	 * @param execution 请求执行
	 * @return 响应
	 * @throws IOException 如果发生 I/O 错误
	 */
	ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException;

}
