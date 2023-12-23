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

import org.springframework.core.*;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * 通过使用 {@link HttpMessageReader} 读取请求体来解析方法参数的参数解析器的抽象基类。
 *
 * <p>如果方法参数带有任何 {@linkplain org.springframework.validation.annotation.ValidationAnnotationUtils#determineValidationHints 触发验证的注解}，则应用验证。
 * 验证失败会导致 {@link ServerWebInputException}。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class AbstractMessageReaderArgumentResolver extends HandlerMethodArgumentResolverSupport {

	/**
	 * 支持的Http方法有POST、PUT、PATCH
	 */
	private static final Set<HttpMethod> SUPPORTED_METHODS =
			EnumSet.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);
	/**
	 * Http消息阅读器
	 */
	private final List<HttpMessageReader<?>> messageReaders;

	/**
	 * 使用 {@link HttpMessageReader} 和 {@link Validator} 的构造函数。
	 *
	 * @param readers 要从请求体转换的读取器
	 */
	protected AbstractMessageReaderArgumentResolver(List<HttpMessageReader<?>> readers) {
		this(readers, ReactiveAdapterRegistry.getSharedInstance());
	}

	/**
	 * 还接受 {@link ReactiveAdapterRegistry} 的构造函数。
	 *
	 * @param messageReaders  要从请求体转换的读取器
	 * @param adapterRegistry 用于从 Flux 和 Mono 适配到其他响应式类型的适配器注册表
	 */
	protected AbstractMessageReaderArgumentResolver(
			List<HttpMessageReader<?>> messageReaders, ReactiveAdapterRegistry adapterRegistry) {

		super(adapterRegistry);
		Assert.notEmpty(messageReaders, "At least one HttpMessageReader is required");
		Assert.notNull(adapterRegistry, "ReactiveAdapterRegistry is required");
		this.messageReaders = messageReaders;
	}

	/**
	 * 返回配置的消息转换器。
	 */
	public List<HttpMessageReader<?>> getMessageReaders() {
		return this.messageReaders;
	}

	/**
	 * 使用 {@link HttpMessageReader} 从方法参数读取请求体。
	 *
	 * @param bodyParameter  要读取的 {@link MethodParameter}
	 * @param isBodyRequired 如果需要请求体，则为 true
	 * @param bindingContext 要使用的绑定上下文
	 * @param exchange       当前交换
	 * @return 请求体
	 * @see #readBody(MethodParameter, MethodParameter, boolean, BindingContext, ServerWebExchange)
	 */
	protected Mono<Object> readBody(MethodParameter bodyParameter, boolean isBodyRequired,
									BindingContext bindingContext, ServerWebExchange exchange) {

		return this.readBody(bodyParameter, null, isBodyRequired, bindingContext, exchange);
	}

	/**
	 * 使用 {@link HttpMessageReader} 从方法参数读取请求体。
	 *
	 * @param bodyParam      表示请求体元素类型的参数
	 * @param actualParam    实际的方法参数类型；可能与 {@code bodyParam} 不同，例如对于 {@code HttpEntity} 参数
	 * @param isBodyRequired 如果需要请求体，则为 true
	 * @param bindingContext 要使用的绑定上下文
	 * @param exchange       当前交换
	 * @return 一个 Mono，其中包含用于方法参数的值
	 * @since 5.0.2
	 */
	protected Mono<Object> readBody(MethodParameter bodyParam, @Nullable MethodParameter actualParam,
									boolean isBodyRequired, BindingContext bindingContext, ServerWebExchange exchange) {
		// 创建 ResolvableType 对象，表示请求体类型
		ResolvableType bodyType = ResolvableType.forMethodParameter(bodyParam);

		// 创建 ResolvableType 对象，表示实际类型，如果存在实际参数则使用其类型，否则使用请求体类型
		ResolvableType actualType = (actualParam != null ? ResolvableType.forMethodParameter(actualParam) : bodyType);

		// 解析请求体类型的 Class 对象
		Class<?> resolvedType = bodyType.resolve();

		// 获取适配器以读取请求体，如果适配器存在，则获取其对应的 ReactiveAdapter
		ReactiveAdapter adapter = (resolvedType != null ? getAdapterRegistry().getAdapter(resolvedType) : null);

		// 确定请求体元素的 ResolvableType
		ResolvableType elementType = (adapter != null ? bodyType.getGeneric() : bodyType);

		// 检查是否需要请求体，若适配器存在且不支持空值，则请求体必须存在
		isBodyRequired = isBodyRequired || (adapter != null && !adapter.supportsEmpty());

		// 获取请求和响应对象
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();

		// 获取请求的内容类型和媒体类型
		MediaType contentType = request.getHeaders().getContentType();
		MediaType mediaType = (contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);

		// 提取验证提示
		Object[] hints = extractValidationHints(bodyParam);

		// 如果媒体类型与 APPLICATION_FORM_URLENCODED 兼容，则返回 UNSUPPORTED_MEDIA_TYPE 异常
		if (mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Form data is accessed via ServerWebExchange.getFormData() in WebFlux.");
			}
			return Mono.error(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
		}

		// 调试日志记录
		if (logger.isDebugEnabled()) {
			logger.debug(exchange.getLogPrefix() + (contentType != null ?
					"Content-Type:" + contentType :
					"No Content-Type, using " + MediaType.APPLICATION_OCTET_STREAM));
		}

		// 遍历消息读取器，寻找能够读取请求体的读取器
		for (HttpMessageReader<?> reader : getMessageReaders()) {
			if (reader.canRead(elementType, mediaType)) {
				// 找到能够读取请求体的读取器
				Map<String, Object> readHints = Hints.from(Hints.LOG_PREFIX_HINT, exchange.getLogPrefix());
				if (adapter != null && adapter.isMultiValue()) {
					// 如果适配器支持多值
					if (logger.isDebugEnabled()) {
						logger.debug(exchange.getLogPrefix() + "0..N [" + elementType + "]");
					}
					// 使用消息读取器读取请求体生成 Flux
					Flux<?> flux = reader.read(actualType, elementType, request, response, readHints);

					// 在错误情况下将其转换为 Flux 错误信号
					flux = flux.onErrorResume(ex -> Flux.error(handleReadError(bodyParam, ex)));

					// 如果需要请求体且 Flux 为空，则转换为对应的错误信号
					if (isBodyRequired) {
						flux = flux.switchIfEmpty(Flux.error(() -> handleMissingBody(bodyParam)));
					}

					// 如果存在验证提示，则在每个元素上执行验证
					if (hints != null) {
						flux = flux.doOnNext(target ->
								validate(target, hints, bodyParam, bindingContext, exchange));
					}

					// 将 Flux 转换为适配器支持的 Mono
					return Mono.just(adapter.fromPublisher(flux));

				} else {
					// 单值（带或不带响应式类型包装器）
					if (logger.isDebugEnabled()) {
						logger.debug(exchange.getLogPrefix() + "0..1 [" + elementType + "]");
					}
					// 使用消息读取器读取请求体生成 Mono
					Mono<?> mono = reader.readMono(actualType, elementType, request, response, readHints);

					// 在错误情况下将其转换为 Mono 错误信号
					mono = mono.onErrorResume(ex -> Mono.error(handleReadError(bodyParam, ex)));

					// 如果需要请求体且 Mono 为空，则转换为对应的错误信号
					if (isBodyRequired) {
						mono = mono.switchIfEmpty(Mono.error(() -> handleMissingBody(bodyParam)));
					}

					// 如果存在验证提示，则在每个元素上执行验证
					if (hints != null) {
						mono = mono.doOnNext(target ->
								validate(target, hints, bodyParam, bindingContext, exchange));
					}

					// 将 Mono 转换为适配器支持的 Mono
					return (adapter != null ? Mono.just(adapter.fromPublisher(mono)) : Mono.from(mono));
				}
			}
		}

		// 没有兼容的读取器，但请求体可能为空。
		// 获取请求方法
		HttpMethod method = request.getMethod();

		// 如果内容类型为空，且请求方法存在，且为支持的方法之一
		if (contentType == null && method != null && SUPPORTED_METHODS.contains(method)) {
			// 获取请求体并在每个数据缓冲区上执行操作
			Flux<DataBuffer> body = request.getBody().doOnNext(buffer -> {
				// 释放数据缓冲区
				DataBufferUtils.release(buffer);
				// 请求体不为空，返回 415..
				throw new UnsupportedMediaTypeStatusException(
						mediaType, getSupportedMediaTypes(elementType), elementType);
			});

			// 如果需要请求体且 Flux 为空，则转换为对应的错误信号
			if (isBodyRequired) {
				body = body.switchIfEmpty(Mono.error(() -> handleMissingBody(bodyParam)));
			}

			// 将 Flux 转换为适配器支持的 Mono
			return (adapter != null ? Mono.just(adapter.fromPublisher(body)) : Mono.from(body));
		}


		// 返回不支持的媒体类型异常
		return Mono.error(new UnsupportedMediaTypeStatusException(
				mediaType, getSupportedMediaTypes(elementType), elementType));

	}

	/**
	 * 处理读取错误的私有方法。
	 *
	 * @param parameter 方法参数
	 * @param ex        异常对象
	 * @return 异常对象（如果不是 DecodingException，则直接返回原始异常对象）
	 */
	private Throwable handleReadError(MethodParameter parameter, Throwable ex) {
		// 如果异常是 DecodingException，则创建服务器网络输入异常并返回
		return (ex instanceof DecodingException ?
				new ServerWebInputException("Failed to read HTTP message", parameter, ex) : ex);
	}


	/**
	 * 处理缺少请求体的私有方法。
	 *
	 * @param parameter 方法参数
	 * @return 服务器网络输入异常
	 */
	private ServerWebInputException handleMissingBody(MethodParameter parameter) {
		// 获取参数的信息（方法的泛型字符串表示）
		String paramInfo = parameter.getExecutable().toGenericString();
		// 创建服务器网络输入异常并返回
		return new ServerWebInputException("Request body is missing: " + paramInfo, parameter);
	}

	/**
	 * 检查给定的 MethodParameter 是否需要验证，如果需要，则返回验证提示的对象数组（可能为空）。
	 * 返回 {@code null} 表示不需要验证。
	 *
	 * @param parameter 方法参数
	 * @return 验证提示
	 */
	@Nullable
	private Object[] extractValidationHints(MethodParameter parameter) {
		//获取方法参数上所有注解
		Annotation[] annotations = parameter.getParameterAnnotations();

		// 遍历参数的所有注解
		for (Annotation ann : annotations) {
			// 确定注解对应的验证提示
			Object[] hints = ValidationAnnotationUtils.determineValidationHints(ann);

			// 如果验证提示不为null，则返回这些提示
			if (hints != null) {
				return hints;
			}
		}
		// 如果没有找到任何验证提示，则返回null
		return null;
	}

	/**
	 * 验证方法，用于对目标对象进行验证
	 *
	 * @param target          目标对象
	 * @param validationHints 验证提示
	 * @param param           方法参数
	 * @param binding         绑定上下文
	 * @param exchange        服务器网络交换
	 */
	private void validate(Object target, Object[] validationHints, MethodParameter param,
						  BindingContext binding, ServerWebExchange exchange) {
		// 获取参数的名称
		String name = Conventions.getVariableNameForParameter(param);

		// 创建一个WebExchangeDataBinder，用于绑定数据
		WebExchangeDataBinder binder = binding.createDataBinder(exchange, target, name);

		// 对数据进行验证
		binder.validate(validationHints);

		// 检查是否有验证错误，若有则抛出WebExchangeBindException
		if (binder.getBindingResult().hasErrors()) {
			throw new WebExchangeBindException(param, binder.getBindingResult());
		}
	}


	/**
	 * 获取支持的媒体类型列表。
	 *
	 * @param elementType 元素类型的 ResolvableType
	 * @return 支持的媒体类型列表
	 */
	private List<MediaType> getSupportedMediaTypes(ResolvableType elementType) {
		// 创建一个列表以存储媒体类型
		List<MediaType> mediaTypes = new ArrayList<>();

		// 遍历消息阅读器列表
		for (HttpMessageReader<?> reader : this.messageReaders) {
			// 获取每个阅读器可读的媒体类型并添加到媒体类型列表中
			mediaTypes.addAll(reader.getReadableMediaTypes(elementType));
		}

		// 返回媒体类型列表
		return mediaTypes;
	}

}
