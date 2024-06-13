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

import okhttp3.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.io.IOException;
import java.net.URI;

/**
 * 基于 OkHttp 3.x 的 {@link AsyncClientHttpRequest} 实现。
 *
 * <p>通过 {@link OkHttp3ClientHttpRequestFactory} 创建。
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @author Roy Clarkson
 * @since 4.3
 * @deprecated 自 Spring 5.0 起，没有直接的替代方案
 */
@Deprecated
class OkHttp3AsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

	/**
	 * OKHttp客户端
	 */
	private final OkHttpClient client;

	/**
	 * URI
	 */
	private final URI uri;

	/**
	 * Http方法
	 */
	private final HttpMethod method;


	/**
	 * 创建 {@link OkHttp3AsyncClientHttpRequest} 的新实例。
	 *
	 * @param client OkHttpClient 实例
	 * @param uri    请求的URI
	 * @param method HTTP 方法
	 */
	public OkHttp3AsyncClientHttpRequest(OkHttpClient client, URI uri, HttpMethod method) {
		this.client = client;
		this.uri = uri;
		this.method = method;
	}


	@Override
	public HttpMethod getMethod() {
		return this.method;
	}

	@Override
	public String getMethodValue() {
		return this.method.name();
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers, byte[] content)
			throws IOException {

		// 使用 OkHttp 3客户端Http请求工厂 构建请求对象
		Request request = OkHttp3ClientHttpRequestFactory.buildRequest(headers, content, this.uri, this.method);
		// 返回一个新的 OkHttp可监听的Future 对象
		return new OkHttpListenableFuture(this.client.newCall(request));
	}


	private static class OkHttpListenableFuture extends SettableListenableFuture<ClientHttpResponse> {

		private final Call call;

		public OkHttpListenableFuture(Call call) {
			this.call = call;
			this.call.enqueue(new Callback() {
				@Override
				public void onResponse(Call call, Response response) {
					set(new OkHttp3ClientHttpResponse(response));
				}

				@Override
				public void onFailure(Call call, IOException ex) {
					setException(ex);
				}
			});
		}

		@Override
		protected void interruptTask() {
			this.call.cancel();
		}
	}

}
