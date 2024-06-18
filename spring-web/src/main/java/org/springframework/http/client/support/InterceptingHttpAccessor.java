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

package org.springframework.http.client.support;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link org.springframework.web.client.RestTemplate}和其他HTTP访问网关助手的基类，
 * 在{@link HttpAccessor}的通用属性中添加了拦截器相关的属性。
 *
 * <p>不打算直接使用。
 * 参见{@link org.springframework.web.client.RestTemplate}以了解入口点。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see ClientHttpRequestInterceptor
 * @see InterceptingClientHttpRequestFactory
 * @see org.springframework.web.client.RestTemplate
 * @since 3.0
 */
public abstract class InterceptingHttpAccessor extends HttpAccessor {
	/**
	 * 客户端Http请求拦截器列表。
	 */
	private final List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

	/**
	 * 拦截请求工厂
	 */
	@Nullable
	private volatile ClientHttpRequestFactory interceptingRequestFactory;


	/**
	 * 设置此访问器应使用的请求拦截器。
	 * <p>拦截器将立即按照其{@linkplain AnnotationAwareOrderComparator#sort(List)顺序}进行排序。
	 *
	 * @param interceptors 要使用的拦截器列表
	 * @see #getRequestFactory()
	 * @see AnnotationAwareOrderComparator
	 */
	public void setInterceptors(List<ClientHttpRequestInterceptor> interceptors) {
		Assert.noNullElements(interceptors, "'interceptors' must not contain null elements");
		// 传入时直接采用getInterceptors()列表
		// 如果当前的拦截器列表与传入的拦截器列表不同
		if (this.interceptors != interceptors) {
			// 清空当前的拦截器列表
			this.interceptors.clear();
			// 将传入的拦截器列表全部添加到当前的拦截器列表中
			this.interceptors.addAll(interceptors);
			// 对当前的拦截器列表进行排序
			AnnotationAwareOrderComparator.sort(this.interceptors);
		}
	}

	/**
	 * 获取此访问器使用的请求拦截器。
	 * <p>返回的{@link List}是活动的，可以进行修改。但是，请注意，在构建
	 * {@link ClientHttpRequestFactory}之前，拦截器不会按照其{@linkplain AnnotationAwareOrderComparator#sort(List)顺序}重新排序。
	 *
	 * @return 请求拦截器列表
	 */
	public List<ClientHttpRequestInterceptor> getInterceptors() {
		return this.interceptors;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		super.setRequestFactory(requestFactory);
		this.interceptingRequestFactory = null;
	}

	/**
	 * 如果需要，重写以暴露{@link InterceptingClientHttpRequestFactory}。
	 *
	 * @see #getInterceptors()
	 */
	@Override
	public ClientHttpRequestFactory getRequestFactory() {
		// 获取拦截器列表
		List<ClientHttpRequestInterceptor> interceptors = getInterceptors();
		// 如果拦截器列表不为空
		if (!CollectionUtils.isEmpty(interceptors)) {
			// 获取当前的拦截请求工厂
			ClientHttpRequestFactory factory = this.interceptingRequestFactory;
			// 如果拦截请求工厂为空
			if (factory == null) {
				// 创建新的 拦截客户端Http请求工厂
				factory = new InterceptingClientHttpRequestFactory(super.getRequestFactory(), interceptors);
				this.interceptingRequestFactory = factory;
			}
			// 返回拦截请求工厂
			return factory;
		} else {
			// 如果拦截器列表为空，返回父类的请求工厂
			return super.getRequestFactory();
		}
	}

}
