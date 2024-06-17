/*
 * Copyright 2002-2015 the original author or authors.
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
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;

/**
 * {@link ClientHttpRequestFactory} 的抽象基类，用于装饰另一个请求工厂。
 *
 * <p>子类必须实现 {@link #createRequest(URI, HttpMethod, ClientHttpRequestFactory)} 方法，
 * 通过传入的请求工厂创建一个新的 {@link ClientHttpRequest}。
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public abstract class AbstractClientHttpRequestFactoryWrapper implements ClientHttpRequestFactory {
	/**
	 * 请求工厂
	 */
	private final ClientHttpRequestFactory requestFactory;


	/**
	 * 创建一个 {@code AbstractClientHttpRequestFactoryWrapper}，装饰给定的请求工厂。
	 *
	 * @param requestFactory 要装饰的请求工厂，不能为空
	 */
	protected AbstractClientHttpRequestFactoryWrapper(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "ClientHttpRequestFactory must not be null");
		this.requestFactory = requestFactory;
	}


	/**
	 * 这个实现只是调用 {@link #createRequest(URI, HttpMethod, ClientHttpRequestFactory)}，
	 * 传入在 {@linkplain #AbstractClientHttpRequestFactoryWrapper(ClientHttpRequestFactory) 构造函数} 中提供的包装请求工厂。
	 *
	 * @param uri        要创建请求的 URI
	 * @param httpMethod 覦执行的 HTTP 方法
	 * @return 创建的请求
	 * @throws IOException 如果发生 I/O 错误
	 */
	@Override
	public final ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return createRequest(uri, httpMethod, this.requestFactory);
	}

	/**
	 * 创建一个新的 {@link ClientHttpRequest}，用于指定的 URI 和 HTTP 方法，
	 * 使用传入的请求工厂。
	 *
	 * <p>这个方法在 {@link #createRequest(URI, HttpMethod)} 中被调用。
	 *
	 * @param uri            要创建请求的 URI
	 * @param httpMethod     覦执行的 HTTP 方法
	 * @param requestFactory 被包装的请求工厂
	 * @return 创建的请求
	 * @throws IOException 如果发生 I/O 错误
	 */
	protected abstract ClientHttpRequest createRequest(
			URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) throws IOException;

}
