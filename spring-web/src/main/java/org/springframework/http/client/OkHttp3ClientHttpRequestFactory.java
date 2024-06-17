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

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * {@link ClientHttpRequestFactory}的实现，使用
 * <a href="https://square.github.io/okhttp/">OkHttp</a> 3.x创建请求。
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @author Roy Clarkson
 * @since 4.3
 */
@SuppressWarnings("deprecation")
public class OkHttp3ClientHttpRequestFactory
		implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory, DisposableBean {
	/**
	 * OK Http客户端
	 */
	private OkHttpClient client;

	/**
	 * 是否是默认客户端
	 */
	private final boolean defaultClient;


	/**
	 * 使用默认的{@link OkHttpClient}实例创建一个工厂。
	 */
	public OkHttp3ClientHttpRequestFactory() {
		this.client = new OkHttpClient();
		this.defaultClient = true;
	}

	/**
	 * 使用给定的{@link OkHttpClient}实例创建一个工厂。
	 *
	 * @param client 要使用的客户端
	 */
	public OkHttp3ClientHttpRequestFactory(OkHttpClient client) {
		Assert.notNull(client, "OkHttpClient must not be null");
		this.client = client;
		this.defaultClient = false;
	}


	/**
	 * 设置底层读取超时（以毫秒为单位）。
	 * 值为0表示无限超时。
	 */
	public void setReadTimeout(int readTimeout) {
		this.client = this.client.newBuilder()
				.readTimeout(readTimeout, TimeUnit.MILLISECONDS)
				.build();
	}

	/**
	 * 设置底层写入超时（以毫秒为单位）。
	 * 值为0表示无限超时。
	 */
	public void setWriteTimeout(int writeTimeout) {
		this.client = this.client.newBuilder()
				.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
				.build();
	}

	/**
	 * 设置底层连接超时（以毫秒为单位）。
	 * 值为0表示无限超时。
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.client = this.client.newBuilder()
				.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
				.build();
	}

	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
		return new OkHttp3ClientHttpRequest(this.client, uri, httpMethod);
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) {
		return new OkHttp3AsyncClientHttpRequest(this.client, uri, httpMethod);
	}

	@Override
	public void destroy() throws IOException {
		// 如果是默认的客户端
		if (this.defaultClient) {
			// 如果我们在构造函数中创建了客户端，则进行清理
			Cache cache = this.client.cache();
			if (cache != null) {
				// 如果缓存不为空，则关闭缓存
				cache.close();
			}
			// 关闭执行器
			this.client.dispatcher().executorService().shutdown();
			// 关闭所有的连接池
			this.client.connectionPool().evictAll();
		}
	}


	static Request buildRequest(HttpHeaders headers, byte[] content, URI uri, HttpMethod method)
			throws MalformedURLException {

		// 获取内容类型
		okhttp3.MediaType contentType = getContentType(headers);
		// 创建请求体，如果内容长度大于0或请求方法需要请求体，则创建 RequestBody 对象，否则为 null
		RequestBody body = (content.length > 0 ||
				okhttp3.internal.http.HttpMethod.requiresRequestBody(method.name()) ?
				RequestBody.create(contentType, content) : null);

		// 创建 请求构建器 对象，并设置 URL 和请求方法
		Request.Builder builder = new Request.Builder().url(uri.toURL()).method(method.name(), body);
		// 为请求添加头部信息
		headers.forEach((headerName, headerValues) -> {
			// 遍历所有的头部值
			for (String headerValue : headerValues) {
				// 将头部名称和头部值添加到请求构建器
				builder.addHeader(headerName, headerValue);
			}
		});
		// 构建并返回请求对象
		return builder.build();
	}

	@Nullable
	private static okhttp3.MediaType getContentType(HttpHeaders headers) {
		// 获取原始的内容类型字符串
		String rawContentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
		// 如果内容类型字符串不为空，且有文本内容，则解析成 okhttp3.MediaType 对象并返回，否则返回 null
		return (StringUtils.hasText(rawContentType) ? okhttp3.MediaType.parse(rawContentType) : null);
	}

}
