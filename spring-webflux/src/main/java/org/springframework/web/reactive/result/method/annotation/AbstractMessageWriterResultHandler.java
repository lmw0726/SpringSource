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

import org.reactivestreams.Publisher;
import org.springframework.core.*;
import org.springframework.core.codec.Hints;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.HandlerResultHandlerSupport;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 抽象基类，用于通过 {@link HttpMessageWriter} 将返回值写入响应。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class AbstractMessageWriterResultHandler extends HandlerResultHandlerSupport {

	/**
	 * 表示 Kotlin 协程流的类名。
	 */
	protected static final String COROUTINES_FLOW_CLASS_NAME = "kotlinx.coroutines.flow.Flow";

	/**
	 * 用于序列化对象到响应体流的 {@link HttpMessageWriter} 列表。
	 */
	private final List<HttpMessageWriter<?>> messageWriters;


	/**
	 * 构造函数，接受 {@link HttpMessageWriter HttpMessageWriter} 和 {@code RequestedContentTypeResolver}。
	 *
	 * @param messageWriters      用于将对象序列化到响应体流的消息写入器
	 * @param contentTypeResolver 用于解析请求的内容类型
	 */
	protected AbstractMessageWriterResultHandler(List<HttpMessageWriter<?>> messageWriters,
												 RequestedContentTypeResolver contentTypeResolver) {

		this(messageWriters, contentTypeResolver, ReactiveAdapterRegistry.getSharedInstance());
	}

	/**
	 * 构造函数，额外接收一个 {@link ReactiveAdapterRegistry}。
	 *
	 * @param messageWriters      用于将对象序列化到响应体流的消息写入器
	 * @param contentTypeResolver 用于解析请求的内容类型
	 * @param adapterRegistry     用于将其他响应式类型（例如 rx.Observable、rx.Single 等）适配到 Flux 或 Mono
	 */
	protected AbstractMessageWriterResultHandler(List<HttpMessageWriter<?>> messageWriters,
												 RequestedContentTypeResolver contentTypeResolver, ReactiveAdapterRegistry adapterRegistry) {

		super(contentTypeResolver, adapterRegistry);
		Assert.notEmpty(messageWriters, "At least one message writer is required");
		this.messageWriters = messageWriters;
	}


	/**
	 * 返回配置的消息转换器。
	 */
	public List<HttpMessageWriter<?>> getMessageWriters() {
		return this.messageWriters;
	}


	/**
	 * 使用 {@link HttpMessageWriter} 将给定的内容写入响应。
	 *
	 * @param body          要写入的对象
	 * @param bodyParameter 要写入的主体的 {@link MethodParameter}
	 * @param exchange      当前的交换对象
	 * @return 表示完成或错误的 Mono
	 * @see #writeBody(Object, MethodParameter, MethodParameter, ServerWebExchange)
	 */
	protected Mono<Void> writeBody(@Nullable Object body, MethodParameter bodyParameter, ServerWebExchange exchange) {
		return this.writeBody(body, bodyParameter, null, exchange);
	}

	/**
	 * 使用 {@link HttpMessageWriter} 将给定的内容写入响应。
	 *
	 * @param body          要写入的对象
	 * @param bodyParameter 要写入的主体的 {@link MethodParameter}
	 * @param actualParam   返回该值的方法的实际返回类型；当处理 {@code HttpEntity} 时可能与 {@code bodyParameter} 不同
	 * @param exchange      当前的交换对象
	 * @return 表示完成或错误的 Mono
	 * @since 5.0.2
	 */
	@SuppressWarnings({"unchecked", "rawtypes", "ConstantConditions"})
	protected Mono<Void> writeBody(@Nullable Object body, MethodParameter bodyParameter,
								   @Nullable MethodParameter actualParam, ServerWebExchange exchange) {
		// 从请求体参数中获取请求体类型
		ResolvableType bodyType = ResolvableType.forMethodParameter(bodyParameter);
		// 获取实际的返回类型
		ResolvableType actualType = (actualParam != null ? ResolvableType.forMethodParameter(actualParam) : bodyType);
		// 获取响应式适配器
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(bodyType.resolve(), body);

		Publisher<?> publisher;
		ResolvableType elementType;
		ResolvableType actualElementType;
		if (adapter != null) {
			// 如果存在响应式适配器，则使用其将对象转换为发布者
			publisher = adapter.toPublisher(body);
			// 是否包装类型
			boolean isUnwrapped = KotlinDetector.isSuspendingFunction(bodyParameter.getMethod()) &&
					!COROUTINES_FLOW_CLASS_NAME.equals(bodyType.toClass().getName()) &&
					!Flux.class.equals(bodyType.toClass());
			// 如果是包装类型，获取它的泛型类型
			ResolvableType genericType = isUnwrapped ? bodyType : bodyType.getGeneric();
			// 获取元素类型并将其作为实际返回类型
			elementType = getElementType(adapter, genericType);
			actualElementType = elementType;
		} else {
			// 否则，将对象包装在 Mono 中
			publisher = Mono.justOrEmpty(body);
			// 获取实际返回类型类型
			actualElementType = body != null ? ResolvableType.forInstance(body) : bodyType;
			// 如果请求体类型为Object，并且请求体不为空，使用请求体类型，否则使用请求体类型 作为元素类型。
			elementType = (bodyType.toClass() == Object.class && body != null ? actualElementType : bodyType);
		}

		// 如果返回类型为 void，则返回空 Mono
		if (elementType.resolve() == void.class || elementType.resolve() == Void.class) {
			return Mono.from((Publisher<Void>) publisher);
		}

		MediaType bestMediaType;
		try {
			// 选择最佳的媒体类型
			bestMediaType = selectMediaType(exchange, () -> getMediaTypesFor(elementType));
		} catch (NotAcceptableStatusException ex) {
			HttpStatus statusCode = exchange.getResponse().getStatusCode();
			if (statusCode != null && statusCode.isError()) {
				// 如果是错误状态，忽略错误响应内容
				if (logger.isDebugEnabled()) {
					logger.debug("忽略错误响应内容（如果有）。" + ex.getReason());
				}
				return Mono.empty();
			}
			throw ex;
		}

		// 如果存在最佳媒体类型，则使用相应的消息写入器写入响应
		if (bestMediaType != null) {
			String logPrefix = exchange.getLogPrefix();
			if (logger.isDebugEnabled()) {
				logger.debug(logPrefix +
						(publisher instanceof Mono ? "0..1" : "0..N") + " [" + elementType + "]");
			}
			for (HttpMessageWriter<?> writer : getMessageWriters()) {
				// 检查写入器是否能够写入给定元素类型和媒体类型
				if (writer.canWrite(actualElementType, bestMediaType)) {
					// 使用写入器将响应写入到 exchange 中
					return writer.write((Publisher) publisher, actualType, elementType,
							bestMediaType, exchange.getRequest(), exchange.getResponse(),
							Hints.from(Hints.LOG_PREFIX_HINT, logPrefix));
				}
			}

		}
		//获取内容类型
		MediaType contentType = exchange.getResponse().getHeaders().getContentType();
		//判断内容类型与最佳媒体类型是否相同
		boolean isPresentMediaType = (contentType != null && contentType.equals(bestMediaType));
		Set<MediaType> producibleTypes = exchange.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);

		// 如果存在媒体类型或可生产的媒体类型，则抛出不可写异常
		if (isPresentMediaType || !CollectionUtils.isEmpty(producibleTypes)) {
			return Mono.error(new HttpMessageNotWritableException(
					"No Encoder for [" + elementType + "] with preset Content-Type '" + contentType + "'"));
		}

		// 获取元素的可写媒体类型列表
		List<MediaType> mediaTypes = getMediaTypesFor(elementType);

		// 如果不存在最佳媒体类型且可写媒体类型列表为空，则抛出异常
		if (bestMediaType == null && mediaTypes.isEmpty()) {
			return Mono.error(new IllegalStateException("No HttpMessageWriter for " + elementType));
		}

		// 抛出不可接受的状态异常
		return Mono.error(new NotAcceptableStatusException(mediaTypes));
	}

	/**
	 * 获取响应式适配器的元素类型。
	 *
	 * @param adapter     响应式适配器
	 * @param genericType 泛型类型
	 * @return 元素类型
	 */
	private ResolvableType getElementType(ReactiveAdapter adapter, ResolvableType genericType) {
		// 如果适配器不包含值，则返回 Void 类型
		if (adapter.isNoValue()) {
			return ResolvableType.forClass(Void.class);
		}
		// 如果存在泛型类型，则返回泛型类型
		else if (genericType != ResolvableType.NONE) {
			return genericType;
		}
		// 否则返回 Object 类型
		else {
			return ResolvableType.forClass(Object.class);
		}
	}

	/**
	 * 获取给定元素类型的可写媒体类型列表。
	 *
	 * @param elementType 元素类型
	 * @return 可写媒体类型列表
	 */
	private List<MediaType> getMediaTypesFor(ResolvableType elementType) {
		// 用于存储可写媒体类型的列表
		List<MediaType> writableMediaTypes = new ArrayList<>();

		// 遍历所有的 HttpMessageWriter
		for (HttpMessageWriter<?> converter : getMessageWriters()) {
			// 如果当前的转换器可以写入给定的元素类型
			if (converter.canWrite(elementType, null)) {
				// 将当前转换器支持的可写媒体类型添加到列表中
				writableMediaTypes.addAll(converter.getWritableMediaTypes(elementType));
			}
		}

		// 返回可写媒体类型列表
		return writableMediaTypes;
	}

}
