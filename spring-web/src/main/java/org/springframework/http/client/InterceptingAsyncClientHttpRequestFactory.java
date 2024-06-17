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
 * {@link AsyncClientHttpRequestFactory} 的包装器，支持 {@link AsyncClientHttpRequestInterceptor AsyncClientHttpRequestInterceptors}。
 *
 * @author Jakub Narloch
 * @see InterceptingAsyncClientHttpRequest
 * @since 4.3
 * @deprecated 从 Spring 5.0 起，没有直接的替代方案
 */
@Deprecated
public class InterceptingAsyncClientHttpRequestFactory implements AsyncClientHttpRequestFactory {
	/**
	 * 异步客户端Http请求工厂
	 */
	private AsyncClientHttpRequestFactory delegate;

	/**
	 * 异步客户端Http请求拦截器列表
	 */
	private List<AsyncClientHttpRequestInterceptor> interceptors;


	/**
	 * 使用委托的请求工厂和拦截器列表创建 {@link InterceptingAsyncClientHttpRequestFactory} 的新实例。
	 *
	 * @param delegate     要委托的请求工厂
	 * @param interceptors 要使用的拦截器列表
	 */
	public InterceptingAsyncClientHttpRequestFactory(AsyncClientHttpRequestFactory delegate,
													 @Nullable List<AsyncClientHttpRequestInterceptor> interceptors) {

		this.delegate = delegate;
		this.interceptors = (interceptors != null ? interceptors : Collections.emptyList());
	}


	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod method) {
		return new InterceptingAsyncClientHttpRequest(this.delegate, this.interceptors, uri, method);
	}

}
