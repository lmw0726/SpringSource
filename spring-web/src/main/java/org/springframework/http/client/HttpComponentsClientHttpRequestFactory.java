/*
 * Copyright 2002-2020 the original author or authors.
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
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.function.BiFunction;

/**
 * {@link org.springframework.http.client.ClientHttpRequestFactory} 的实现，使用
 * <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents
 * HttpClient</a> 创建请求。
 *
 * <p>允许使用预配置的 {@link HttpClient} 实例 - 可能包含认证、HTTP 连接池等功能。
 *
 * <p><b>注意:</b> 从 Spring 4.0 开始，需要使用 Apache HttpComponents 4.3 或更高版本。
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 */
public class HttpComponentsClientHttpRequestFactory implements ClientHttpRequestFactory, DisposableBean {
	/**
	 * Http客户端
	 */
	private HttpClient httpClient;

	/**
	 * 请求配置
	 */
	@Nullable
	private RequestConfig requestConfig;

	/**
	 * 是否缓冲区请求正文
	 */
	private boolean bufferRequestBody = true;

	/**
	 * http上下文工厂
	 */
	@Nullable
	private BiFunction<HttpMethod, URI, HttpContext> httpContextFactory;


	/**
	 * 使用基于系统属性的默认 {@link HttpClient} 创建 {@code HttpComponentsClientHttpRequestFactory} 的新实例。
	 */
	public HttpComponentsClientHttpRequestFactory() {
		this.httpClient = HttpClients.createSystem();
	}

	/**
	 * 使用给定的 {@link HttpClient} 实例创建 {@code HttpComponentsClientHttpRequestFactory} 的新实例。
	 *
	 * @param httpClient 用于此请求工厂的 HttpClient 实例
	 */
	public HttpComponentsClientHttpRequestFactory(HttpClient httpClient) {
		this.httpClient = httpClient;
	}


	/**
	 * 设置用于 {@linkplain #createRequest(URI, HttpMethod) 同步执行} 的 {@code HttpClient}。
	 *
	 * @param httpClient 要设置的 HttpClient 实例
	 */
	public void setHttpClient(HttpClient httpClient) {
		Assert.notNull(httpClient, "HttpClient must not be null");
		this.httpClient = httpClient;
	}

	/**
	 * 返回用于 {@linkplain #createRequest(URI, HttpMethod) 同步执行} 的 {@code HttpClient}。
	 *
	 * @return 当前的 HttpClient 实例
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * 设置底层 {@link RequestConfig} 的连接超时时间。
	 * 超时值为0表示无限超时。
	 * <p>可以通过在自定义 {@link HttpClient} 上指定 {@link RequestConfig} 实例来配置其他属性。
	 * <p>此选项不影响 SSL 握手或 CONNECT 请求的连接超时；为此，需要在 {@link HttpClient} 自身上使用 {@link org.apache.http.config.SocketConfig}。
	 *
	 * @param timeout 超时时间（毫秒）
	 * @see RequestConfig#getConnectTimeout()
	 * @see org.apache.http.config.SocketConfig#getSoTimeout
	 */
	public void setConnectTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.requestConfig = requestConfigBuilder().setConnectTimeout(timeout).build();
	}

	/**
	 * 设置从连接管理器请求连接时使用的超时时间（以毫秒为单位），使用底层 {@link RequestConfig}。
	 * 超时值为0表示无限超时。
	 * <p>可以通过在自定义 {@link HttpClient} 上指定 {@link RequestConfig} 实例来配置其他属性。
	 *
	 * @param connectionRequestTimeout 请求连接的超时时间（毫秒）
	 * @see RequestConfig#getConnectionRequestTimeout()
	 */
	public void setConnectionRequestTimeout(int connectionRequestTimeout) {
		this.requestConfig = requestConfigBuilder()
				.setConnectionRequestTimeout(connectionRequestTimeout).build();
	}

	/**
	 * 设置底层 {@link RequestConfig} 的套接字读取超时时间。
	 * 超时值为0表示无限超时。
	 * <p>可以通过在自定义 {@link HttpClient} 上指定 {@link RequestConfig} 实例来配置其他属性。
	 *
	 * @param timeout 超时时间（毫秒）
	 * @see RequestConfig#getSocketTimeout()
	 */
	public void setReadTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.requestConfig = requestConfigBuilder().setSocketTimeout(timeout).build();
	}

	/**
	 * 指示此请求工厂是否应在内部缓冲请求体。
	 * <p>默认值为 {@code true}。当通过 POST 或 PUT 发送大量数据时，
	 * 建议将此属性更改为 {@code false}，以避免内存耗尽。
	 *
	 * @since 4.0
	 */
	public void setBufferRequestBody(boolean bufferRequestBody) {
		this.bufferRequestBody = bufferRequestBody;
	}

	/**
	 * 配置工厂以预先创建每个请求的 {@link HttpContext}。
	 * <p>例如，在相互 TLS 认证中可能很有用，其中为每个客户端证书创建不同的 {@code RestTemplate}，
	 * 以便通过给定的 {@code RestTemplate} 实例发出的所有调用都与同一客户端身份关联。
	 * {@link HttpClientContext#setUserToken(Object)} 可用于指定所有请求的固定用户令牌。
	 *
	 * @param httpContextFactory 要使用的上下文工厂
	 * @since 5.2.7
	 */
	public void setHttpContextFactory(BiFunction<HttpMethod, URI, HttpContext> httpContextFactory) {
		this.httpContextFactory = httpContextFactory;
	}

	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		// 获取 Http客户端 对象
		HttpClient client = getHttpClient();

		// 创建 Http Uri请求 对象
		HttpUriRequest httpRequest = createHttpUriRequest(httpMethod, uri);
		// 对请求进行后处理
		postProcessHttpRequest(httpRequest);
		// 创建 Http上下文 对象
		HttpContext context = createHttpContext(httpMethod, uri);
		// 如果 Http上下文 为空
		if (context == null) {
			// 创建通过 Http客户端上下文 创建 Http上下文
			context = HttpClientContext.create();
		}

		// 如果未在上下文中设置请求配置
		if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
			// 当用户提供的请求可以配置时，使用用户提供的请求配置
			RequestConfig config = null;
			if (httpRequest instanceof Configurable) {
				// 如果Http请求 是 可配置的，获取配置
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

		if (this.bufferRequestBody) {
			// 如果缓冲请求体，则返回 HttpComponentsClientHttpRequest 对象
			return new HttpComponentsClientHttpRequest(client, httpRequest, context);
		} else {
			// 否则返回 HttpComponentsStreamingClientHttpRequest 对象
			return new HttpComponentsStreamingClientHttpRequest(client, httpRequest, context);
		}
	}


	/**
	 * 返回一个用于修改工厂级别 {@link RequestConfig} 的构建器。
	 *
	 * @since 4.2
	 */
	private RequestConfig.Builder requestConfigBuilder() {
		return (this.requestConfig != null ? RequestConfig.copy(this.requestConfig) : RequestConfig.custom());
	}

	/**
	 * 创建一个默认的 {@link RequestConfig}，用于给定的客户端。
	 * 可以返回 {@code null} 表示不应设置自定义请求配置，应使用 {@link HttpClient} 的默认配置。
	 * <p>默认实现尝试合并客户端的默认配置与本地工厂实例的自定义配置（如果有）。
	 *
	 * @param client 要检查的 {@link HttpClient}（或 {@code HttpAsyncClient}）
	 * @return 要使用的实际 RequestConfig（可能为 {@code null}）
	 * @see #mergeRequestConfig(RequestConfig)
	 * @since 4.2
	 */
	@Nullable
	protected RequestConfig createRequestConfig(Object client) {
		// 如果 Http客户端 实现了 Configurable 接口
		if (client instanceof Configurable) {
			// 获取 Http客户端 的请求配置
			RequestConfig clientRequestConfig = ((Configurable) client).getConfig();
			// 合并 Http客户端 的请求配置和当前实例的请求配置
			return mergeRequestConfig(clientRequestConfig);
		}
		// 否则返回当前实例的请求配置
		return this.requestConfig;
	}

	/**
	 * 合并给定的 {@link HttpClient} 级别的 {@link RequestConfig} 与工厂级别的 {@link RequestConfig}，如果有必要。
	 *
	 * @param clientConfig 当前客户端持有的配置
	 * @return 合并后的请求配置
	 * @since 4.2
	 */
	protected RequestConfig mergeRequestConfig(RequestConfig clientConfig) {
		// 如果当前实例的请求配置为空
		if (this.requestConfig == null) {
			// 没有要合并的内容，直接返回 Http客户端 的请求配置
			return clientConfig;
		}

		// 复制一份 Http客户端 的配置到 配置构建器 中
		RequestConfig.Builder builder = RequestConfig.copy(clientConfig);
		// 获取当前实例的连接超时时间
		int connectTimeout = this.requestConfig.getConnectTimeout();
		// 如果连接超时时间大于等于0，则设置到 配置构建器 中
		if (connectTimeout >= 0) {
			builder.setConnectTimeout(connectTimeout);
		}
		// 获取当前实例的连接请求超时时间
		int connectionRequestTimeout = this.requestConfig.getConnectionRequestTimeout();
		// 如果连接请求超时时间大于等于0，则设置到 配置构建器 中
		if (connectionRequestTimeout >= 0) {
			builder.setConnectionRequestTimeout(connectionRequestTimeout);
		}
		// 获取当前实例的套接字超时时间
		int socketTimeout = this.requestConfig.getSocketTimeout();
		// 如果套接字超时时间大于等于0，则设置到 配置构建器 中
		if (socketTimeout >= 0) {
			builder.setSocketTimeout(socketTimeout);
		}
		// 构建并返回合并后的 请求配置 对象
		return builder.build();
	}

	/**
	 * 根据给定的HTTP方法和URI规范创建一个Commons HttpMethodBase对象。
	 *
	 * @param httpMethod HTTP方法
	 * @param uri        URI
	 * @return Commons HttpMethodBase对象
	 */
	protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
		switch (httpMethod) {
			case GET:
				return new HttpGet(uri);
			case HEAD:
				return new HttpHead(uri);
			case POST:
				return new HttpPost(uri);
			case PUT:
				return new HttpPut(uri);
			case PATCH:
				return new HttpPatch(uri);
			case DELETE:
				return new HttpDelete(uri);
			case OPTIONS:
				return new HttpOptions(uri);
			case TRACE:
				return new HttpTrace(uri);
			default:
				throw new IllegalArgumentException("Invalid HTTP method: " + httpMethod);
		}
	}

	/**
	 * 模板方法，允许在将 {@link HttpUriRequest} 作为 {@link HttpComponentsClientHttpRequest} 的一部分返回之前对其进行操作。
	 * <p>默认实现为空。
	 *
	 * @param request 要处理的请求
	 */
	protected void postProcessHttpRequest(HttpUriRequest request) {
	}

	/**
	 * 模板方法，为给定的HTTP方法和URI创建一个 {@link HttpContext}。
	 * <p>默认实现返回 {@code null}。
	 *
	 * @param httpMethod HTTP方法
	 * @param uri        URI
	 * @return HTTP上下文
	 */
	@Nullable
	protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
		return (this.httpContextFactory != null ? this.httpContextFactory.apply(httpMethod, uri) : null);
	}


	/**
	 * 关闭挂钩，关闭底层
	 * {@link org.apache.http.conn.HttpClientConnectionManager ClientConnectionManager}
	 * 的连接池（如果有）。
	 */
	@Override
	public void destroy() throws Exception {
		// 获取Http客户端
		HttpClient httpClient = getHttpClient();
		if (httpClient instanceof Closeable) {
			// 如果Http客户端实现了 Closeable，则执行关闭方法
			((Closeable) httpClient).close();
		}
	}


	/**
	 * {@link org.apache.http.client.methods.HttpDelete}的替代实现，继承自
	 * {@link org.apache.http.client.methods.HttpEntityEnclosingRequestBase}，
	 * 而不是 {@link org.apache.http.client.methods.HttpRequestBase}，
	 * 因此允许带有请求体的 HTTP DELETE 请求。
	 * 用于与 RestTemplate 的 exchange 方法结合，允许 HTTP DELETE 与实体的组合使用。
	 *
	 * @since 4.1.2
	 */
	private static class HttpDelete extends HttpEntityEnclosingRequestBase {

		public HttpDelete(URI uri) {
			super();
			setURI(uri);
		}

		@Override
		public String getMethod() {
			return "DELETE";
		}
	}

}
