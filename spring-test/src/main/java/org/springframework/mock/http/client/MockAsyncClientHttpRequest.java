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

package org.springframework.mock.http.client;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.io.IOException;
import java.net.URI;

/**
 * {@link MockClientHttpRequest} 的扩展，同时实现 {@link org.springframework.http.client.AsyncClientHttpRequest}，
 * 通过将响应包装在 {@link SettableListenableFuture} 中实现异步请求。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 * @deprecated 自 Spring 5.0 起，没有直接的替代方案
 */
@Deprecated
public class MockAsyncClientHttpRequest extends MockClientHttpRequest implements org.springframework.http.client.AsyncClientHttpRequest {

	public MockAsyncClientHttpRequest() {
	}

	public MockAsyncClientHttpRequest(HttpMethod httpMethod, URI uri) {
		super(httpMethod, uri);
	}


	@Override
	public ListenableFuture<ClientHttpResponse> executeAsync() throws IOException {
		// 创建一个可设置的可监听的Future对象，用于持有ClientHttpResponse
		SettableListenableFuture<ClientHttpResponse> future = new SettableListenableFuture<>();
		// 设置Future的结果为执行execute()方法的返回值
		future.set(execute());
		// 返回设置好结果的Future对象
		return future;
	}

}
