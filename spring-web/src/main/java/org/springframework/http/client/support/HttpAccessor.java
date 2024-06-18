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

package org.springframework.http.client.support;

import org.apache.commons.logging.Log;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link org.springframework.web.client.RestTemplate}和其他HTTP访问网关助手的基类，定义了通用属性，
 * 如操作所需的{@link ClientHttpRequestFactory}。
 *
 * <p>不打算直接使用。
 *
 * <p>参见{@link org.springframework.web.client.RestTemplate}以了解入口点。
 *
 * <p><b>注意：</b> 标准的JDK HTTP库不支持HTTP PATCH方法。
 * 配置Apache HttpComponents或OkHttp请求工厂以启用PATCH。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @see ClientHttpRequestFactory
 * @see org.springframework.web.client.RestTemplate
 * @since 3.0
 */
public abstract class HttpAccessor {

	/**
	 * 供子类使用的日志记录器。
	 */
	protected final Log logger = HttpLogging.forLogName(getClass());

	/**
	 * 客户端Http请求工厂
	 */
	private ClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

	/**
	 * 客户端Http请求初始化器列表
	 */
	private final List<ClientHttpRequestInitializer> clientHttpRequestInitializers = new ArrayList<>();


	/**
	 * 设置此访问器用于获取客户端请求句柄的请求工厂。
	 * <p>默认是基于JDK自带HTTP库（{@link java.net.HttpURLConnection}）的{@link SimpleClientHttpRequestFactory}。
	 * <p><b>注意：标准的JDK HTTP库不支持HTTP PATCH方法。
	 * 配置Apache HttpComponents或OkHttp请求工厂以启用PATCH。</b>
	 *
	 * @param requestFactory 用于获取客户端请求句柄的请求工厂
	 * @see #createRequest(URI, HttpMethod)
	 * @see SimpleClientHttpRequestFactory
	 * @see org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory
	 * @see org.springframework.http.client.OkHttp3ClientHttpRequestFactory
	 */
	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "ClientHttpRequestFactory must not be null");
		this.requestFactory = requestFactory;
	}

	/**
	 * 返回此访问器用于获取客户端请求句柄的请求工厂。
	 */
	public ClientHttpRequestFactory getRequestFactory() {
		return this.requestFactory;
	}


	/**
	 * 设置此访问器应使用的请求初始化器。
	 * <p>初始化器将立即按照其{@linkplain AnnotationAwareOrderComparator#sort(List)顺序}进行排序。
	 *
	 * @param clientHttpRequestInitializers 要使用的初始化器列表
	 * @since 5.2
	 */
	public void setClientHttpRequestInitializers(List<ClientHttpRequestInitializer> clientHttpRequestInitializers) {
		// 如果当前的 客户端Http请求初始化器列表 与传入的 初始化器列表 不同
		if (this.clientHttpRequestInitializers != clientHttpRequestInitializers) {
			// 清空当前的 客户端Http请求初始化器列表
			this.clientHttpRequestInitializers.clear();
			// 将 初始化器列表 全部添加到当前的 客户端Http请求初始化器列表 中
			this.clientHttpRequestInitializers.addAll(clientHttpRequestInitializers);
			// 对当前的 客户端Http请求初始化器列表 进行排序
			AnnotationAwareOrderComparator.sort(this.clientHttpRequestInitializers);
		}
	}

	/**
	 * 获取此访问器使用的请求初始化器。
	 * <p>返回的{@link List}是活动的，可以进行修改。但是，请注意，在{@link ClientHttpRequest}初始化之前，
	 * 初始化器不会按照其{@linkplain AnnotationAwareOrderComparator#sort(List)顺序}进行重新排序。
	 *
	 * @see #setClientHttpRequestInitializers(List)
	 * @since 5.2
	 */
	public List<ClientHttpRequestInitializer> getClientHttpRequestInitializers() {
		return this.clientHttpRequestInitializers;
	}

	/**
	 * 通过此模板的{@link ClientHttpRequestFactory}创建一个新的{@link ClientHttpRequest}。
	 *
	 * @param url    要连接的URL
	 * @param method 要执行的HTTP方法（GET、POST等）
	 * @return 创建的请求
	 * @throws IOException 如果发生I/O错误
	 * @see #getRequestFactory()
	 * @see ClientHttpRequestFactory#createRequest(URI, HttpMethod)
	 */
	protected ClientHttpRequest createRequest(URI url, HttpMethod method) throws IOException {
		// 使用请求工厂创建 客户端Http请求 对象
		ClientHttpRequest request = getRequestFactory().createRequest(url, method);
		// 初始化请求
		initialize(request);
		// 如果日志记录器处于调试级别
		if (logger.isDebugEnabled()) {
			// 记录调试信息，包括 HTTP 方法和 URL
			logger.debug("HTTP " + method.name() + " " + url);
		}
		// 返回创建的请求对象
		return request;
	}

	private void initialize(ClientHttpRequest request) {
		this.clientHttpRequestInitializers.forEach(initializer -> initializer.initialize(request));
	}

}
