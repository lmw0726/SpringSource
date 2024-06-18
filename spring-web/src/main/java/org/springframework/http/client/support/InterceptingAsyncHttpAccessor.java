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

package org.springframework.http.client.support;

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 扩展基础类{@link AsyncHttpAccessor}，增加请求拦截功能的HTTP访问器。
 *
 * @author Jakub Narloch
 * @author Rossen Stoyanchev
 * @since 4.3
 * @deprecated 从Spring 5.0开始，不再推荐使用，没有直接的替代品
 */
@Deprecated
public abstract class InterceptingAsyncHttpAccessor extends AsyncHttpAccessor {
	/**
	 * 异步客户端Http请求拦截器列表
	 */
	private List<org.springframework.http.client.AsyncClientHttpRequestInterceptor> interceptors =
			new ArrayList<>();


	/**
	 * 设置此访问器应该使用的请求拦截器。
	 *
	 * @param interceptors 拦截器列表
	 */
	public void setInterceptors(List<org.springframework.http.client.AsyncClientHttpRequestInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	/**
	 * 返回此访问器使用的请求拦截器。
	 *
	 * @return 拦截器列表
	 */
	public List<org.springframework.http.client.AsyncClientHttpRequestInterceptor> getInterceptors() {
		return this.interceptors;
	}


	@Override
	public org.springframework.http.client.AsyncClientHttpRequestFactory getAsyncRequestFactory() {
		// 获取父类的异步请求工厂
		org.springframework.http.client.AsyncClientHttpRequestFactory delegate = super.getAsyncRequestFactory();
		// 如果拦截器列表不为空
		if (!CollectionUtils.isEmpty(getInterceptors())) {
			// 使用拦截器创建 拦截异步客户端Http请求工厂 并返回
			return new org.springframework.http.client.InterceptingAsyncClientHttpRequestFactory(delegate, getInterceptors());
		} else {
			// 否则直接返回父类的异步请求工厂
			return delegate;
		}
	}

}
