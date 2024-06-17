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
import org.springframework.lang.Nullable;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * 支持 {@link ClientHttpRequestInterceptor ClientHttpRequestInterceptors} 的 {@link ClientHttpRequestFactory} 包装器。
 *
 * @author Arjen Poutsma
 * @see ClientHttpRequestFactory
 * @see ClientHttpRequestInterceptor
 * @since 3.1
 */
public class InterceptingClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {
	/**
	 * 拦截器列表
	 */
	private final List<ClientHttpRequestInterceptor> interceptors;


	/**
	 * 创建一个 {@code InterceptingClientHttpRequestFactory} 的新实例，并指定要包装的请求工厂和拦截器。
	 *
	 * @param requestFactory 要包装的请求工厂
	 * @param interceptors   要应用的拦截器列表（可以为 {@code null}）
	 */
	public InterceptingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory,
												@Nullable List<ClientHttpRequestInterceptor> interceptors) {

		super(requestFactory);
		this.interceptors = (interceptors != null ? interceptors : Collections.emptyList());
	}

	/**
	 * 创建一个新的 {@link ClientHttpRequest}，并应用配置的拦截器。
	 *
	 * @param uri            请求的 URI
	 * @param httpMethod     请求的方法
	 * @param requestFactory 用于创建请求的工厂
	 * @return 创建的请求对象
	 */
	@Override
	protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) {
		return new InterceptingClientHttpRequest(requestFactory, this.interceptors, uri, httpMethod);
	}

}
