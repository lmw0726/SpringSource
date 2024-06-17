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

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

/**
 * {@link HttpComponentsClientHttpRequestFactory} 的异步扩展版本。使用
 * <a href="https://hc.apache.org/httpcomponents-asyncclient-dev/">Apache HttpComponents
 * HttpAsyncClient 4.0</a> 创建请求。
 *
 * <p><b>注意:</b> 从 Spring 5.0 起，已弃用，推荐使用
 * {@link org.springframework.http.client.reactive.HttpComponentsClientHttpConnector}。
 *
 * @author Arjen Poutsma
 * @author Stephane Nicoll
 * @see HttpAsyncClient
 * @since 4.0
 * @deprecated 从 Spring 5.0 起，推荐使用 {@link org.springframework.http.client.reactive.HttpComponentsClientHttpConnector}
 */
@Deprecated
public class HttpComponentsAsyncClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory
		implements AsyncClientHttpRequestFactory, InitializingBean {

	/**
	 * Http异步客户端
	 */
	private HttpAsyncClient asyncClient;


	/**
	 * 使用默认的 {@link HttpAsyncClient} 和 {@link HttpClient} 创建
	 * {@code HttpComponentsAsyncClientHttpRequestFactory} 的新实例。
	 */
	public HttpComponentsAsyncClientHttpRequestFactory() {
		super();
		this.asyncClient = HttpAsyncClients.createSystem();
	}

	/**
	 * 使用给定的 {@link HttpAsyncClient} 实例和默认的 {@link HttpClient} 创建
	 * {@code HttpComponentsAsyncClientHttpRequestFactory} 的新实例。
	 *
	 * @param asyncClient 要用于此请求工厂的 HttpAsyncClient 实例
	 * @since 4.3.10
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(HttpAsyncClient asyncClient) {
		super();
		this.asyncClient = asyncClient;
	}

	/**
	 * 使用给定的 {@link CloseableHttpAsyncClient} 实例和默认的 {@link HttpClient} 创建
	 * {@code HttpComponentsAsyncClientHttpRequestFactory} 的新实例。
	 *
	 * @param asyncClient 要用于此请求工厂的 CloseableHttpAsyncClient 实例
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(CloseableHttpAsyncClient asyncClient) {
		super();
		this.asyncClient = asyncClient;
	}

	/**
	 * 使用给定的 {@link HttpClient} 和 {@link HttpAsyncClient} 实例创建
	 * {@code HttpComponentsAsyncClientHttpRequestFactory} 的新实例。
	 *
	 * @param httpClient  用于此请求工厂的 HttpClient 实例
	 * @param asyncClient 用于此请求工厂的 HttpAsyncClient 实例
	 * @since 4.3.10
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(HttpClient httpClient, HttpAsyncClient asyncClient) {
		super(httpClient);
		this.asyncClient = asyncClient;
	}

	/**
	 * 使用给定的 {@link CloseableHttpClient} 和 {@link CloseableHttpAsyncClient} 实例创建
	 * {@code HttpComponentsAsyncClientHttpRequestFactory} 的新实例。
	 *
	 * @param httpClient  用于此请求工厂的 CloseableHttpClient 实例
	 * @param asyncClient 用于此请求工厂的 CloseableHttpAsyncClient 实例
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(
			CloseableHttpClient httpClient, CloseableHttpAsyncClient asyncClient) {

		super(httpClient);
		this.asyncClient = asyncClient;
	}


	/**
	 * 设置用于 {@linkplain #createAsyncRequest(URI, HttpMethod) 异步执行} 的 {@code HttpAsyncClient}。
	 *
	 * @param asyncClient 要设置的 HttpAsyncClient 实例
	 * @see #setHttpClient(HttpClient)
	 * @since 4.3.10
	 */
	public void setAsyncClient(HttpAsyncClient asyncClient) {
		Assert.notNull(asyncClient, "HttpAsyncClient must not be null");
		this.asyncClient = asyncClient;
	}

	/**
	 * 返回用于 {@linkplain #createAsyncRequest(URI, HttpMethod) 异步执行} 的 {@code HttpAsyncClient}。
	 *
	 * @return 当前的 HttpAsyncClient 实例
	 * @see #getHttpClient()
	 * @since 4.3.10
	 */
	public HttpAsyncClient getAsyncClient() {
		return this.asyncClient;
	}

	/**
	 * 设置用于 {@linkplain #createAsyncRequest(URI, HttpMethod) 异步执行} 的 {@code CloseableHttpAsyncClient}。
	 *
	 * @param asyncClient 要设置的 CloseableHttpAsyncClient 实例
	 * @deprecated 自 4.3.10 起，推荐使用 {@link #setAsyncClient(HttpAsyncClient)}
	 */
	@Deprecated
	public void setHttpAsyncClient(CloseableHttpAsyncClient asyncClient) {
		this.asyncClient = asyncClient;
	}

	/**
	 * 返回用于 {@linkplain #createAsyncRequest(URI, HttpMethod) 异步执行} 的 {@code CloseableHttpAsyncClient}。
	 *
	 * @return 当前的 CloseableHttpAsyncClient 实例
	 * @deprecated 自 4.3.10 起，推荐使用 {@link #getAsyncClient()}
	 */
	@Deprecated
	public CloseableHttpAsyncClient getHttpAsyncClient() {
		Assert.state(this.asyncClient instanceof CloseableHttpAsyncClient,
				"No CloseableHttpAsyncClient - use getAsyncClient() instead");
		return (CloseableHttpAsyncClient) this.asyncClient;
	}


	@Override
	public void afterPropertiesSet() {
		startAsyncClient();
	}

	private HttpAsyncClient startAsyncClient() {
		// 获取异步 Http 客户端对象
		HttpAsyncClient client = getAsyncClient();
		// 如果客户端是 CloseableHttpAsyncClient 的实例
		if (client instanceof CloseableHttpAsyncClient) {
			// 强制类型转换为 CloseableHttpAsyncClient
			@SuppressWarnings("resource")
			CloseableHttpAsyncClient closeableAsyncClient = (CloseableHttpAsyncClient) client;
			// 如果客户端未运行，则启动客户端
			if (!closeableAsyncClient.isRunning()) {
				closeableAsyncClient.start();
			}
		}
		// 返回客户端对象
		return client;
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		// 获取异步客户端对象并启动
		HttpAsyncClient client = startAsyncClient();

		// 创建 Http Uri请求 请求对象
		HttpUriRequest httpRequest = createHttpUriRequest(httpMethod, uri);
		// 对请求进行后处理
		postProcessHttpRequest(httpRequest);
		// 创建 Http上下文 对象
		HttpContext context = createHttpContext(httpMethod, uri);
		// 如果 Http上下文 为空
		if (context == null) {
			// 通过 Http客户端上下文 创建一个新的 Http上下文
			context = HttpClientContext.create();
		}

		// 如果未在上下文中设置请求配置
		if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
			// 当用户提供的请求可以配置时，使用用户提供的请求配置
			RequestConfig config = null;
			if (httpRequest instanceof Configurable) {
				// 如果Http请求实现了Configurable接口，获取配置
				config = ((Configurable) httpRequest).getConfig();
			}
			// 如果配置为空，则创建默认的请求配置
			if (config == null) {
				config = createRequestConfig(client);
			}
			// 如果配置不为空，则设置到上下文中
			if (config != null) {
				context.setAttribute(HttpClientContext.REQUEST_CONFIG, config);
			}
		}

		// 返回 Http组件异步客户端Http请求 对象
		return new HttpComponentsAsyncClientHttpRequest(client, httpRequest, context);
	}

	@Override
	public void destroy() throws Exception {
		try {
			// 调用父类的 destroy 方法
			super.destroy();
		} finally {
			// 获取异步客户端对象
			HttpAsyncClient asyncClient = getAsyncClient();
			// 如果异步客户端是 Closeable 的实例
			if (asyncClient instanceof Closeable) {
				// 关闭异步客户端
				((Closeable) asyncClient).close();
			}
		}
	}

}
