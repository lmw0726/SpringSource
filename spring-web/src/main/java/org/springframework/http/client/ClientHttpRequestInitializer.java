/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.http.client.support.HttpAccessor;

/**
 * 用于在使用之前初始化 {@link ClientHttpRequest} 的回调接口。
 *
 * <p>通常与 {@link HttpAccessor} 及其子类（例如 {@link org.springframework.web.client.RestTemplate RestTemplate}）
 * 一起使用，以便对每个请求应用一致的设置或头部信息。
 *
 * <p>与 {@link ClientHttpRequestInterceptor} 不同，此接口可以在不需要将整个请求体读入内存的情况下应用定制设置。
 *
 * @author Phillip Webb
 * @see HttpAccessor#getClientHttpRequestInitializers()
 * @since 5.2
 */
@FunctionalInterface
public interface ClientHttpRequestInitializer {

	/**
	 * 初始化给定的客户端 HTTP 请求。
	 *
	 * @param request 要配置的请求对象
	 */
	void initialize(ClientHttpRequest request);

}
