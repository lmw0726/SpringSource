/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * {@code @RequestPart} 参数的解析器，其中命名的部分类似于 {@code @RequestBody} 参数，
 * 但是基于单个部分的内容进行解码。参数可以包装在响应式类型中以获取单个值（例如 Reactor 的 {@code Mono}，RxJava 的 {@code Single}）。
 *
 * <p>此解析器还支持 {@link Part} 类型的参数，可以将其包装在响应式类型中以获取单个值或多个值。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestPartMethodArgumentResolver extends AbstractMessageReaderArgumentResolver {

	public RequestPartMethodArgumentResolver(List<HttpMessageReader<?>> readers, ReactiveAdapterRegistry registry) {
		super(readers, registry);
	}

	/**
	 * 判断该方法参数是否支持解析。如果参数上标注了 {@code @RequestPart} 注解，
	 * 或者参数类型是 {@link Part} 或其子类，则返回 true。
	 *
	 * @param parameter 要判断的方法参数
	 * @return 如果支持解析则返回 true，否则返回 false
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		//方法参数上标注@RequestPart注解或者类型是Part类型子类
		return (parameter.hasParameterAnnotation(RequestPart.class) ||
				checkParameterType(parameter, Part.class::isAssignableFrom));
	}

	/**
	 * 解析参数并返回处理后的结果。
	 *
	 * @param parameter      方法参数
	 * @param bindingContext 绑定上下文
	 * @param exchange       当前的服务器Web交换
	 * @return 解析后的参数结果
	 */
	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {

		// 获取参数上的 RequestPart 注解
		RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
		// 如果@RequestPart注解不存在，或者@RequestPart注解的必填属性为true，则是必须的。
		boolean isRequired = (requestPart == null || requestPart.required());
		//获取参数类型
		Class<?> paramType = parameter.getParameterType();
		//获取多部分值Flux流
		Flux<Part> partValues = getPartValues(parameter, requestPart, isRequired, exchange);

		// 如果参数类型是 Part 或其子类，则返回第一个 Part
		if (Part.class.isAssignableFrom(paramType)) {
			return partValues.next().cast(Object.class);
		}

		// 如果参数类型是 Collection 或 List
		if (Collection.class.isAssignableFrom(paramType) || List.class.isAssignableFrom(paramType)) {
			//获取嵌套的方法参数
			MethodParameter elementType = parameter.nested();
			if (Part.class.isAssignableFrom(elementType.getNestedParameterType())) {
				// 如果集合元素类型是 Part 或其子类，收集所有 Part
				return partValues.collectList().cast(Object.class);
			} else {
				// 否则解码第一个 Part 并返回结果，如果没有则返回空列表
				return partValues.next()
						.flatMap(part -> decode(part, parameter, bindingContext, exchange, isRequired))
						.defaultIfEmpty(Collections.emptyList());
			}
		}

		// 如果需要适配器，则获取适配器进行转换
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(paramType);
		if (adapter == null) {
			return partValues.next().flatMap(part ->
					decode(part, parameter, bindingContext, exchange, isRequired));
		}

		// 获取元素类型并根据适配器将 Part 转换为对应类型
		MethodParameter elementType = parameter.nested();
		if (Part.class.isAssignableFrom(elementType.getNestedParameterType())) {
			// 如果集合元素类型是 Part 或其子类，则将多部分值转为Mono返回
			return Mono.just(adapter.fromPublisher(partValues));
		}

		// 将 Part 流解码为指定类型的流，并使用适配器进行转换
		Flux<?> flux = partValues.flatMap(part -> decode(part, elementType, bindingContext, exchange, isRequired));
		return Mono.just(adapter.fromPublisher(flux));
	}

	/**
	 * 获取请求中指定名称的部分值流。
	 *
	 * @param parameter   方法参数
	 * @param requestPart 请求部分注解
	 * @param isRequired  是否需要此部分数据，如果是必需的，则为 true；否则为 false
	 * @param exchange    当前的服务器Web交换
	 * @return 请求中指定名称的部分值流
	 * @throws ServerWebInputException 如果必需的请求部分不存在，则抛出异常
	 */
	public Flux<Part> getPartValues(
			MethodParameter parameter, @Nullable RequestPart requestPart, boolean isRequired,
			ServerWebExchange exchange) {

		// 获取请求部分的名称
		String name = getPartName(parameter, requestPart);

		// 获取请求中的多部分数据
		return exchange.getMultipartData()
				.flatMapIterable(map -> {
					// 根据部分的名称获取部分列表
					List<Part> list = map.get(name);

					// 如果部分列表为空
					if (CollectionUtils.isEmpty(list)) {
						// 如果该部分是必需的，则抛出异常，指明该请求部分不存在
						if (isRequired) {
							String reason = "Required request part '" + name + "' is not present";
							throw new ServerWebInputException(reason, parameter);
						}

						// 如果非必需，返回空列表
						return Collections.emptyList();
					}

					// 返回部分列表
					return list;
				});
	}


	/**
	 * 获取请求部分的名称。
	 *
	 * @param methodParam 方法参数
	 * @param requestPart 请求部分注解
	 * @return 请求部分的名称
	 * @throws IllegalArgumentException 如果未指定参数类型的请求部分名称，
	 *                                  并且在类文件中找不到参数名称信息，则抛出异常
	 */
	private String getPartName(MethodParameter methodParam, @Nullable RequestPart requestPart) {
		// 初始化部分的名称为 null
		String name = null;

		// 如果存在 RequestPart 注解，使用注解中的名称
		if (requestPart != null) {
			name = requestPart.name();
		}

		// 如果没有指定名称且方法参数具有参数名称，则使用方法参数的名称
		if (!StringUtils.hasLength(name)) {
			name = methodParam.getParameterName();
		}

		// 如果最终部分名称仍然为空，则抛出异常，指明参数类型的请求部分名称未指定
		if (!StringUtils.hasLength(name)) {
			throw new IllegalArgumentException("Request part name for argument type [" +
					methodParam.getNestedParameterType().getName() +
					"] not specified, and parameter name information not found in class file either.");
		}

		// 返回最终确定的请求部分名称
		return name;
	}

	/**
	 * 解码部分数据流为指定类型的对象。
	 *
	 * @param part           要解码的部分
	 * @param elementType    要解码到的目标类型
	 * @param bindingContext 绑定上下文
	 * @param exchange       当前的服务器Web交换
	 * @param isRequired     是否需要此部分数据，如果是必需的，则为 true；否则为 false
	 * @param <T>            解码的目标类型
	 * @return 解码后的对象的 {@code Mono} 包装
	 */
	@SuppressWarnings("unchecked")
	private <T> Mono<T> decode(
			Part part, MethodParameter elementType, BindingContext bindingContext,
			ServerWebExchange exchange, boolean isRequired) {

		// 创建针对部分的新请求和交换
		ServerHttpRequest partRequest = new PartServerHttpRequest(exchange.getRequest(), part);
		ServerWebExchange partExchange = exchange.mutate().request(partRequest).build();

		// 如果日志级别为 DEBUG，则记录解码部分的信息
		if (logger.isDebugEnabled()) {
			logger.debug(exchange.getLogPrefix() + "Decoding part '" + part.name() + "'");
		}

		// 调用 readBody 方法以解码部分内容并返回解码后的对象的 Mono
		return (Mono<T>) readBody(elementType, isRequired, bindingContext, partExchange);

	}


	/**
	 * 内部静态类，扩展自 {@code ServerHttpRequestDecorator}，
	 * 用于处理部分 {@code ServerHttpRequest} 的实现。
	 */
	private static class PartServerHttpRequest extends ServerHttpRequestDecorator {

		private final Part part;

		/**
		 * 构造方法，传入原始的 {@code ServerHttpRequest} 和处理的部分。
		 *
		 * @param delegate 原始的 {@code ServerHttpRequest}
		 * @param part     处理的部分
		 */
		public PartServerHttpRequest(ServerHttpRequest delegate, Part part) {
			super(delegate);
			this.part = part;
		}

		/**
		 * 获取部分的头信息。
		 *
		 * @return 部分的头信息
		 */
		@Override
		public HttpHeaders getHeaders() {
			return this.part.headers();
		}

		/**
		 * 获取部分的数据流。
		 *
		 * @return 部分的数据流
		 */
		@Override
		public Flux<DataBuffer> getBody() {
			return this.part.content();
		}
	}

}
