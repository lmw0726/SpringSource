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

import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.net.URI;

/**
 * 一个{@link ClientHttpRequestFactory}的包装类，
 * 用于在内存中缓冲所有传出的和传入的流。
 *
 * <p>使用这个包装类可以多次读取
 * {@linkplain ClientHttpResponse#getBody() 响应体}。
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public class BufferingClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

	/**
	 * 为给定的{@link ClientHttpRequestFactory}创建一个缓冲包装器。
	 *
	 * @param requestFactory 要包装的目标请求工厂
	 */
	public BufferingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory) {
		super(requestFactory);
	}


	@Override
	protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory)
			throws IOException {

		// 使用工厂创建 客户端Http请求 对象
		ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
		// 如果应该缓冲请求
		if (shouldBuffer(uri, httpMethod)) {
			// 返回缓冲包装的 客户端Http请求 对象
			return new BufferingClientHttpRequestWrapper(request);
		} else {
			// 否则返回原始的请求对象
			return request;
		}
	}

	/**
	 * 指示给定URI和方法的请求/响应交换是否应缓存在内存中。
	 * <p>默认实现对所有URI和方法返回{@code true}。
	 * 子类可以重写此方法以改变此行为。
	 *
	 * @param uri        URI
	 * @param httpMethod 方法
	 * @return 如果交换应缓冲，则返回{@code true}；否则返回{@code false}
	 */
	protected boolean shouldBuffer(URI uri, HttpMethod httpMethod) {
		return true;
	}

}
