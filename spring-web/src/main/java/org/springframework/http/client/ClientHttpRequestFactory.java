/*
 * Copyright 2002-2016 the original author or authors.
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
 * {@link ClientHttpRequest} 对象的工厂接口。
 * 请求通过 {@link #createRequest(URI, HttpMethod)} 方法创建。
 *
 * <p>该工厂接口允许根据给定的 URI 和 HTTP 方法创建新的 {@link ClientHttpRequest} 对象，
 * 返回的请求可以进行写入操作，并通过调用 {@link ClientHttpRequest#execute()} 方法执行。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
@FunctionalInterface
public interface ClientHttpRequestFactory {

	/**
	 * 根据指定的 URI 和 HTTP 方法创建一个新的 {@link ClientHttpRequest} 对象。
	 *
	 * @param uri        要创建请求的 URI
	 * @param httpMethod 要执行的 HTTP 方法
	 * @return 创建的请求对象
	 * @throws IOException 如果发生I/O错误
	 */
	ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException;

}
