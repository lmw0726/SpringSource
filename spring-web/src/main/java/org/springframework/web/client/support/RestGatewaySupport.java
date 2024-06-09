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

package org.springframework.web.client.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * 用于需要进行REST访问的应用程序类的便捷超类。
 *
 * <p>需要设置{@link ClientHttpRequestFactory}或{@link RestTemplate}的实例。
 *
 * @author Arjen Poutsma
 * @see #setRestTemplate
 * @see org.springframework.web.client.RestTemplate
 * @since 3.0
 */
public class RestGatewaySupport {

	/**
	 * 可用与子类的日志记录器。
	 */
	protected final Log logger = LogFactory.getLog(getClass());
	/**
	 * Rest请求模板
	 */
	private RestTemplate restTemplate;


	/**
	 * 构造一个新的{@link RestGatewaySupport}实例，使用默认参数。
	 */
	public RestGatewaySupport() {
		this.restTemplate = new RestTemplate();
	}

	/**
	 * 构造一个新的{@link RestGatewaySupport}实例，使用给定的{@link ClientHttpRequestFactory}。
	 *
	 * @param requestFactory 请求工厂
	 * @see RestTemplate#RestTemplate(ClientHttpRequestFactory)
	 */
	public RestGatewaySupport(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "'requestFactory' must not be null");
		this.restTemplate = new RestTemplate(requestFactory);
	}


	/**
	 * 设置网关的{@link RestTemplate}。
	 *
	 * @param restTemplate RestTemplate实例
	 */
	public void setRestTemplate(RestTemplate restTemplate) {
		Assert.notNull(restTemplate, "'restTemplate' must not be null");
		this.restTemplate = restTemplate;
	}

	/**
	 * 返回网关的{@link RestTemplate}。
	 *
	 * @return RestTemplate实例
	 */
	public RestTemplate getRestTemplate() {
		return this.restTemplate;
	}

}
