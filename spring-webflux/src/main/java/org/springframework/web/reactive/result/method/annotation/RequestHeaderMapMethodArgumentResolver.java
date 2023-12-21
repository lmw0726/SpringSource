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

package org.springframework.web.reactive.result.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;

/**
 * 解析带有 {@code @RequestHeader} 注解的 {@link Map} 方法参数。
 * 对于使用 {@code @RequestHeader} 注解的单个头部值，请参阅 {@link RequestHeaderMethodArgumentResolver}。
 *
 * <p>创建的 {@link Map} 包含所有请求头名称/值对。
 * 方法参数类型可以是 {@link MultiValueMap}，以接收头部的所有值，而不仅仅是第一个值。
 *
 * @author Rossen Stoyanchev
 * @see RequestHeaderMethodArgumentResolver
 * @since 5.0
 */
public class RequestHeaderMapMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public RequestHeaderMapMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		//方法参数类型及其嵌套类型上是否含有@RequestHeader注解，并且
		return checkAnnotatedParamNoReactiveWrapper(param, RequestHeader.class, this::allParams);
	}

	/**
	 * 检查是否所有参数都符合请求头注解要求
	 *
	 * @param annotation 请求头注解
	 * @param type       参数类型
	 * @return true表示断言符合条件
	 */
	private boolean allParams(RequestHeader annotation, Class<?> type) {
		// 检查参数类型是否为 Map 类型
		return Map.class.isAssignableFrom(type);
	}

	/**
	 * 解析方法参数的值。
	 *
	 * @param methodParameter 方法参数
	 * @param context         要使用的绑定上下文
	 * @param exchange        当前交换
	 * @return 参数值
	 */
	@Override
	public Object resolveArgumentValue(
			MethodParameter methodParameter, BindingContext context, ServerWebExchange exchange) {

		// 检查参数类型是否为 MultiValueMap
		boolean isMultiValueMap = MultiValueMap.class.isAssignableFrom(methodParameter.getParameterType());

		// 获取请求的头部信息
		HttpHeaders headers = exchange.getRequest().getHeaders();

		// 如果参数类型为 MultiValueMap，则直接返回头部信息
		// 否则，将头部信息转换为单值 Map 返回
		return (isMultiValueMap ? headers : headers.toSingleValueMap());
	}

}
