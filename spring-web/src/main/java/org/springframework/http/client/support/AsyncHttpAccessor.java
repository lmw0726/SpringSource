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

package org.springframework.http.client.support;

import org.apache.commons.logging.Log;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;

/**
 * {@link org.springframework.web.client.AsyncRestTemplate}和其他HTTP访问网关助手的基类，
 * 定义了操作所需的通用属性，如{@link org.springframework.http.client.AsyncClientHttpRequestFactory}。
 *
 * <p>不打算直接使用。参见{@link org.springframework.web.client.AsyncRestTemplate}。
 *
 * @author Arjen Poutsma
 * @see org.springframework.web.client.AsyncRestTemplate
 * @since 4.0
 * @deprecated 从Spring 5.0开始，不再推荐使用，没有直接的替代品
 */
@Deprecated
public class AsyncHttpAccessor {

	/**
	 * 可供子类使用的日志记录器。
	 */
	protected final Log logger = HttpLogging.forLogName(getClass());

	/**
	 * 异步请求工厂
	 */
	@Nullable
	private org.springframework.http.client.AsyncClientHttpRequestFactory asyncRequestFactory;


	/**
	 * 设置此访问器用于获取{@link org.springframework.http.client.ClientHttpRequest HttpRequests}的请求工厂。
	 *
	 * @param asyncRequestFactory 要设置的请求工厂
	 */
	public void setAsyncRequestFactory(
			org.springframework.http.client.AsyncClientHttpRequestFactory asyncRequestFactory) {

		Assert.notNull(asyncRequestFactory, "AsyncClientHttpRequestFactory must not be null");
		this.asyncRequestFactory = asyncRequestFactory;
	}

	/**
	 * 返回此访问器用于获取{@link org.springframework.http.client.ClientHttpRequest HttpRequests}的请求工厂。
	 *
	 * @return 请求工厂
	 */
	public org.springframework.http.client.AsyncClientHttpRequestFactory getAsyncRequestFactory() {
		Assert.state(this.asyncRequestFactory != null, "No AsyncClientHttpRequestFactory set");
		return this.asyncRequestFactory;
	}

	/**
	 * 通过此模板的{@link org.springframework.http.client.AsyncClientHttpRequestFactory}创建一个新的{@link org.springframework.http.client.AsyncClientHttpRequest}。
	 *
	 * @param url    要连接的URL
	 * @param method 要执行的HTTP方法（GET, POST等）
	 * @return 创建的请求
	 * @throws IOException 在I/O错误的情况下抛出
	 */
	protected org.springframework.http.client.AsyncClientHttpRequest createAsyncRequest(URI url, HttpMethod method)
			throws IOException {

		// 使用异步请求工厂创建 异步客户端Http请求 对象
		org.springframework.http.client.AsyncClientHttpRequest request =
				getAsyncRequestFactory().createAsyncRequest(url, method);
		// 如果日志记录器处于调试级别
		if (logger.isDebugEnabled()) {
			// 记录调试信息，包含请求方法和 URL
			logger.debug("Created asynchronous " + method.name() + " request for \"" + url + "\"");
		}
		// 返回创建的异步请求对象
		return request;
	}

}
