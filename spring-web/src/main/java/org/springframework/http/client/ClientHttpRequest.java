/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpRequest;

import java.io.IOException;

/**
 * 表示客户端的 HTTP 请求。
 * 通过 {@link ClientHttpRequestFactory} 的实现创建。
 *
 * <p>{@code ClientHttpRequest} 可以被 {@linkplain #execute() 执行}，
 * 得到一个可以读取的 {@link ClientHttpResponse}。
 *
 * @author Arjen Poutsma
 * @see ClientHttpRequestFactory#createRequest(java.net.URI, HttpMethod)
 * @since 3.0
 */
public interface ClientHttpRequest extends HttpRequest, HttpOutputMessage {

	/**
	 * 执行此请求，得到一个可以读取的 {@link ClientHttpResponse}。
	 *
	 * @return 执行结果的响应
	 * @throws IOException 如果发生 I/O 错误
	 */
	ClientHttpResponse execute() throws IOException;

}
