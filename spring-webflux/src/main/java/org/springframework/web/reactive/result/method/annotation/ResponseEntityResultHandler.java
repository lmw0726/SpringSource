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

import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.*;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 处理 {@link HttpEntity} 和 {@link ResponseEntity} 返回值的结果处理器。
 *
 * <p>默认情况下，此结果处理器的顺序设置为0。通常可以将其放在顺序的最前面，因为它查找一个具体的返回类型。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResponseEntityResultHandler extends AbstractMessageWriterResultHandler implements HandlerResultHandler {

	private static final Set<HttpMethod> SAFE_METHODS = EnumSet.of(HttpMethod.GET, HttpMethod.HEAD);

	/**
	 * 使用默认的 {@link ReactiveAdapterRegistry} 构造函数。
	 *
	 * @param writers  用于将响应体序列化的写入器
	 * @param resolver 用于确定请求的内容类型
	 */
	public ResponseEntityResultHandler(List<HttpMessageWriter<?>> writers, RequestedContentTypeResolver resolver) {
		this(writers, resolver, ReactiveAdapterRegistry.getSharedInstance());
	}

	/**
	 * 使用 {@link ReactiveAdapterRegistry} 实例的构造函数。
	 *
	 * @param writers  用于将响应体序列化的写入器
	 * @param resolver 用于确定请求的内容类型
	 * @param registry 用于适应到响应类型的反应式类型
	 */
	public ResponseEntityResultHandler(List<HttpMessageWriter<?>> writers,
									   RequestedContentTypeResolver resolver, ReactiveAdapterRegistry registry) {
		super(writers, resolver, registry);
		setOrder(0);
	}

	/**
	 * 判断是否支持处理给定的处理结果。
	 *
	 * @param result 处理结果
	 * @return 如果支持处理，则为 true；否则为 false
	 */
	@Override
	public boolean supports(HandlerResult result) {
		// 解析返回值类型
		Class<?> valueType = resolveReturnValueType(result);

		// 检查值类型是否被支持
		if (isSupportedType(valueType)) {
			// 如果支持，返回 true
			return true;
		}

		// 获取适配器
		ReactiveAdapter adapter = getAdapter(result);

		// 检查适配器是否存在且不是无值状态，并检查返回类型是否被支持
		return adapter != null && !adapter.isNoValue() &&
				isSupportedType(result.getReturnType().getGeneric().toClass());

	}

	/**
	 * 解析返回值类型。
	 *
	 * @param result 处理结果
	 * @return 返回值类型
	 */
	@Nullable
	private static Class<?> resolveReturnValueType(HandlerResult result) {
		// 获取返回值类型
		Class<?> valueType = result.getReturnType().toClass();

		// 获取返回值
		Object value = result.getReturnValue();

		// 如果返回值类型是 Object.class 并且值不为 null
		if (valueType == Object.class && value != null) {
			// 更新值类型为实际值的类型
			valueType = value.getClass();
		}

		// 返回值类型
		return valueType;
	}

	/**
	 * 检查是否为支持的类型。
	 *
	 * @param clazz 要检查的类型
	 * @return 如果是支持的类型，则为 true；否则为 false
	 */
	private boolean isSupportedType(@Nullable Class<?> clazz) {
		// 类不为 null
		// 该类是 HttpEntity 的子类，并且不是 RequestEntity 的子类；或者该类是 HttpHeaders 的子类。
		// 如果满足这些条件，返回 true，否则返回 false。
		return (clazz != null && ((HttpEntity.class.isAssignableFrom(clazz) &&
				!RequestEntity.class.isAssignableFrom(clazz)) ||
				HttpHeaders.class.isAssignableFrom(clazz)));
	}

	/**
	 * 处理给定的处理结果。
	 *
	 * @param exchange 当前的服务器WebExchange
	 * @param result   处理结果
	 * @return 表示完成或错误的 Mono
	 */
	@Override
	@SuppressWarnings("ConstantConditions")
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {

		Mono<?> returnValueMono;
		MethodParameter bodyParameter;
		ReactiveAdapter adapter = getAdapter(result);
		// 获取返回值类型的实际参数
		MethodParameter actualParameter = result.getReturnTypeSource();

		// 判断适配器是否存在
		if (adapter != null) {
			// 如果适配器存在，确保不支持多值
			Assert.isTrue(!adapter.isMultiValue(), "Only a single ResponseEntity supported");

			// 从适配器获取返回值的 Mono
			returnValueMono = Mono.from(adapter.toPublisher(result.getReturnValue()));

			// 检查是否是 Kotlin 的挂起函数
			boolean isContinuation = (KotlinDetector.isSuspendingFunction(actualParameter.getMethod()) &&
					!COROUTINES_FLOW_CLASS_NAME.equals(actualParameter.getParameterType().getName()));

			// 根据是否是 Kotlin 的挂起函数设置 bodyParameter
			bodyParameter = (isContinuation ? actualParameter.nested() : actualParameter.nested().nested());
		} else {
			// 如果适配器不存在，直接将返回值包装成 Mono
			returnValueMono = Mono.justOrEmpty(result.getReturnValue());
			bodyParameter = actualParameter.nested();
		}

		// 处理返回值 Mono
		return returnValueMono.flatMap(returnValue -> {
			HttpEntity<?> httpEntity;

			// 判断返回值类型
			if (returnValue instanceof HttpEntity) {
				httpEntity = (HttpEntity<?>) returnValue;
			} else if (returnValue instanceof HttpHeaders) {
				httpEntity = new ResponseEntity<>((HttpHeaders) returnValue, HttpStatus.OK);
			} else {
				throw new IllegalArgumentException(
						"HttpEntity or HttpHeaders expected but got: " + returnValue.getClass());
			}

			// 处理 ResponseEntity 的状态码
			if (httpEntity instanceof ResponseEntity) {
				exchange.getResponse().setRawStatusCode(
						((ResponseEntity<?>) httpEntity).getStatusCodeValue());
			}

			// 处理实体头部信息
			HttpHeaders entityHeaders = httpEntity.getHeaders();
			HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
			if (!entityHeaders.isEmpty()) {
				entityHeaders.entrySet().stream()
						.forEach(entry -> responseHeaders.put(entry.getKey(), entry.getValue()));
			}

			// 处理空 body 的情况
			if (httpEntity.getBody() == null || returnValue instanceof HttpHeaders) {
				//如果响应体为空或者返回值是HttpHeaders
				return exchange.getResponse().setComplete();
			}

			// 处理缓存和写入响应体
			String etag = entityHeaders.getETag();
			// 将实体头部的最后修改时间转换为 Instant 类型
			Instant lastModified = Instant.ofEpochMilli(entityHeaders.getLastModified());

			// 获取请求的 HTTP 方法
			HttpMethod httpMethod = exchange.getRequest().getMethod();

			// 检查请求方法是否在安全方法集合中，并且检查是否未修改过（ETag 和最后修改时间）
			if (SAFE_METHODS.contains(httpMethod) && exchange.checkNotModified(etag, lastModified)) {
				return exchange.getResponse().setComplete();
			}

			return writeBody(httpEntity.getBody(), bodyParameter, actualParameter, exchange);
		});
	}

}
