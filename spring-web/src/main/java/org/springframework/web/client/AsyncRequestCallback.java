/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.client;

import java.io.IOException;

/**
 * 异步请求回调接口，用于操作 {@link org.springframework.http.client.AsyncClientHttpRequest}。
 * 允许操作请求头，并向请求体中写入内容。
 *
 * <p>内部由 {@link AsyncRestTemplate} 使用，但也对应用程序代码很有用。
 *
 * @author Arjen Poutsma
 * @see org.springframework.web.client.AsyncRestTemplate#execute
 * @since 4.0
 * @deprecated 自 Spring 5.0 起弃用，推荐使用 {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction}
 */
@FunctionalInterface
@Deprecated
public interface AsyncRequestCallback {

	/**
	 * 在 {@link AsyncRestTemplate#execute} 中被调用，传入一个打开的 {@code ClientHttpRequest}。
	 * 不需要关心关闭请求或处理错误：这些都将由 {@code RestTemplate} 处理。
	 *
	 * @param request 活动的 HTTP 请求
	 * @throws java.io.IOException 在 I/O 错误时抛出
	 */
	void doWithRequest(org.springframework.http.client.AsyncClientHttpRequest request) throws IOException;

}
