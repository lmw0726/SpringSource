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

package org.springframework.web.reactive.result.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpEntity;
import org.springframework.http.RequestEntity;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 通过使用兼容的 {@code HttpMessageReader} 读取请求体，
 * 解析 {@link HttpEntity} 或 {@link RequestEntity} 类型的方法参数。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class HttpEntityMethodArgumentResolver extends AbstractMessageReaderArgumentResolver {

	public HttpEntityMethodArgumentResolver(List<HttpMessageReader<?>> readers, ReactiveAdapterRegistry registry) {
		super(readers, registry);
	}

	/**
	 * 判断是否支持解析指定类型的方法参数。
	 *
	 * @param parameter 要判断的方法参数
	 * @return 如果是 {@link HttpEntity} 或 {@link RequestEntity} 类型的参数则返回 true，否则返回 false
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		//参数类型不是Reactive包装类，并且参数类型是 HttpEntity 或者 RequestEntity
		return checkParameterTypeNoReactiveWrapper(parameter,
				type -> HttpEntity.class.equals(type) || RequestEntity.class.equals(type));
	}

	/**
	 * 解析参数并返回处理后的结果。
	 *
	 * @param parameter        方法参数
	 * @param bindingContext   绑定上下文
	 * @param exchange         当前的服务器Web交换
	 * @return 解析后的参数结果
	 */
	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {

		// 获取参数的类型
		Class<?> entityType = parameter.getParameterType();

		// 将方法参数的嵌套方法参数作为请求体，读取并映射为对应的实体对象，如果没有请求体则创建空的实体对象
		return readBody(parameter.nested(), parameter, false, bindingContext, exchange)
				.map(body -> createEntity(body, entityType, exchange.getRequest()))
				.defaultIfEmpty(createEntity(null, entityType, exchange.getRequest()));
	}

	/**
	 * 创建对应类型的实体对象。
	 *
	 * @param body        实体对象的主体内容
	 * @param entityType  实体对象的类型
	 * @param request     当前的服务器HTTP请求
	 * @return 创建的实体对象
	 */
	private Object createEntity(@Nullable Object body, Class<?> entityType, ServerHttpRequest request) {
		//如果实体类型是 RequestEntity，则获取请求头，请求方法，请求URI和请求体构建新的RequestEntity返回。
		return (RequestEntity.class.equals(entityType) ?
				new RequestEntity<>(body, request.getHeaders(), request.getMethod(), request.getURI()) :
				//否则返回 HttpEntity
				new HttpEntity<>(body, request.getHeaders()));
	}

}
