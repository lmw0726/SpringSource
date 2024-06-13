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

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpHeaders;
import org.springframework.util.concurrent.*;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;


/**
 * 基于 Apache HttpComponents HttpAsyncClient 的 {@link ClientHttpRequest} 实现。
 *
 * <p>通过 {@link HttpComponentsClientHttpRequestFactory} 创建。
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @see HttpComponentsClientHttpRequestFactory#createRequest
 * @since 4.0
 * @deprecated 自 Spring 5.0 起，推荐使用 {@link org.springframework.http.client.reactive.HttpComponentsClientHttpConnector}
 */
@Deprecated
final class HttpComponentsAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

	/**
	 * Http异步客户端
	 */
	private final HttpAsyncClient httpClient;

	/**
	 * Http URI 请求
	 */
	private final HttpUriRequest httpRequest;

	/**
	 * Http上下文
	 */
	private final HttpContext httpContext;


	HttpComponentsAsyncClientHttpRequest(HttpAsyncClient client, HttpUriRequest request, HttpContext context) {
		this.httpClient = client;
		this.httpRequest = request;
		this.httpContext = context;
	}


	@Override
	public String getMethodValue() {
		return this.httpRequest.getMethod();
	}

	@Override
	public URI getURI() {
		return this.httpRequest.getURI();
	}

	/**
	 * 获取 HTTP 上下文。
	 *
	 * @return HTTP 上下文
	 */
	HttpContext getHttpContext() {
		return this.httpContext;
	}

	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers, byte[] bufferedOutput)
			throws IOException {

		// 添加请求头到 httpRequest 对象中
		HttpComponentsClientHttpRequest.addHeaders(this.httpRequest, headers);

		// 如果 httpRequest 是 HttpEntityEnclosingRequest 的实例
		if (this.httpRequest instanceof HttpEntityEnclosingRequest) {
			// 将 httpRequest 转换为 HttpEntityEnclosingRequest
			HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) this.httpRequest;
			// 创建一个新的请求实体，使用 bufferedOutput 中的数据
			HttpEntity requestEntity = new NByteArrayEntity(bufferedOutput);
			// 设置请求实体到 entityEnclosingRequest 中
			entityEnclosingRequest.setEntity(requestEntity);
		}

		// 创建一个新的 HttpResponseFutureCallback 对象
		HttpResponseFutureCallback callback = new HttpResponseFutureCallback(this.httpRequest);
		// 使用 httpClient 执行 httpRequest
		Future<HttpResponse> futureResponse = this.httpClient.execute(this.httpRequest, this.httpContext, callback);

		// 返回一个新的 ClientHttpResponseFuture
		return new ClientHttpResponseFuture(futureResponse, callback);
	}


	private static class HttpResponseFutureCallback implements FutureCallback<HttpResponse> {
		/**
		 * HttpURI请求
		 */
		private final HttpUriRequest request;

		/**
		 * 异步监听器回调注册表
		 */
		private final ListenableFutureCallbackRegistry<ClientHttpResponse> callbacks =
				new ListenableFutureCallbackRegistry<>();

		public HttpResponseFutureCallback(HttpUriRequest request) {
			this.request = request;
		}

		public void addCallback(ListenableFutureCallback<? super ClientHttpResponse> callback) {
			this.callbacks.addCallback(callback);
		}

		public void addSuccessCallback(SuccessCallback<? super ClientHttpResponse> callback) {
			this.callbacks.addSuccessCallback(callback);
		}

		public void addFailureCallback(FailureCallback callback) {
			this.callbacks.addFailureCallback(callback);
		}

		@Override
		public void completed(HttpResponse result) {
			this.callbacks.success(new HttpComponentsAsyncClientHttpResponse(result));
		}

		@Override
		public void failed(Exception ex) {
			this.callbacks.failure(ex);
		}

		@Override
		public void cancelled() {
			this.request.abort();
		}
	}


	private static class ClientHttpResponseFuture extends FutureAdapter<ClientHttpResponse, HttpResponse>
			implements ListenableFuture<ClientHttpResponse> {

		/**
		 * Http响应异步回调
		 */
		private final HttpResponseFutureCallback callback;

		public ClientHttpResponseFuture(Future<HttpResponse> response, HttpResponseFutureCallback callback) {
			super(response);
			this.callback = callback;
		}

		@Override
		protected ClientHttpResponse adapt(HttpResponse response) {
			return new HttpComponentsAsyncClientHttpResponse(response);
		}

		@Override
		public void addCallback(ListenableFutureCallback<? super ClientHttpResponse> callback) {
			this.callback.addCallback(callback);
		}

		@Override
		public void addCallback(SuccessCallback<? super ClientHttpResponse> successCallback,
								FailureCallback failureCallback) {

			// 将 成功回调 添加到 callback 的成功回调列表中
			this.callback.addSuccessCallback(successCallback);
			// 将 失败回调 添加到 callback 的失败回调列表中
			this.callback.addFailureCallback(failureCallback);
		}
	}

}
