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

import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.net.URI;

/**
 * 用于创建 {@link AsyncClientHttpRequest} 对象的工厂。
 * 请求通过 {@link #createAsyncRequest(URI, HttpMethod)} 方法创建。
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @deprecated 从 Spring 5.0 起，推荐使用 {@link org.springframework.http.client.reactive.ClientHttpConnector}
 */
@Deprecated
public interface AsyncClientHttpRequestFactory {

	/**
	 * 为指定的 URI 和 HTTP 方法创建一个新的异步 {@link AsyncClientHttpRequest}。
	 * <p>返回的请求可以写入数据，然后通过调用 {@link AsyncClientHttpRequest#executeAsync()} 执行。
	 *
	 * @param uri        要创建请求的 URI
	 * @param httpMethod 要执行的 HTTP 方法
	 * @return 创建的请求
	 * @throws IOException 如果发生 I/O 错误
	 */
	AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException;

}
