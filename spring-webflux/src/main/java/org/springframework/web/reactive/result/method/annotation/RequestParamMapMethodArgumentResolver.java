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
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;

/**
 * {@link RequestParam @RequestParam} 注解标记的未指定请求参数名称的 {@link Map} 方法参数的解析器。
 * 用于解析具有请求参数名称的 {@link Map} 方法参数，请参阅 {@link RequestParamMethodArgumentResolver}。
 *
 * <p>创建的 {@link Map} 包含所有请求参数名-值对。
 * 如果方法参数类型是 {@link MultiValueMap}，则创建的映射包含所有请求参数及其值，对于请求参数有多个值的情况。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @see RequestParamMethodArgumentResolver
 * @since 5.0
 */
public class RequestParamMapMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public RequestParamMapMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}

	/**
	 * 检查参数解析器是否支持方法参数。
	 *
	 * @param param 方法参数
	 * @return 是否支持该参数
	 */
	@Override
	public boolean supportsParameter(MethodParameter param) {
		// 检查带有 @RequestParam 注解的参数，不允许使用反应式包装器
		return checkAnnotatedParamNoReactiveWrapper(param, RequestParam.class, this::allParams);
	}

	/**
	 * 检查所有参数是否满足条件。
	 *
	 * @param requestParam 请求参数
	 * @param type         类型
	 * @return 是否满足所有参数的条件
	 */
	private boolean allParams(RequestParam requestParam, Class<?> type) {
		//类型是Map类型，并且@RequestParam名称有值
		return (Map.class.isAssignableFrom(type) && !StringUtils.hasText(requestParam.name()));
	}


	/**
	 * 解析参数值并返回对象。
	 *
	 * @param methodParameter 方法参数
	 * @param context         绑定上下文
	 * @param exchange        ServerWebExchange 对象
	 * @return 解析后的对象
	 */
	@Override
	public Object resolveArgumentValue(
			MethodParameter methodParameter, BindingContext context, ServerWebExchange exchange) {
		//方法参数类型是 MultiValueMap 类型
		boolean isMultiValueMap = MultiValueMap.class.isAssignableFrom(methodParameter.getParameterType());
		//获取查询参数
		MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
		//如果是MultiValueMap 类型，则返回该查询参数，否则返回单个值的Map
		return (isMultiValueMap ? queryParams : queryParams.toSingleValueMap());
	}

}
