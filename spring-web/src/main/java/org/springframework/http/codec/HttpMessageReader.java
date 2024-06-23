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

package org.springframework.http.codec;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 策略接口，用于从{@link ReactiveHttpInputMessage}读取并解码字节流到类型为{@code <T>}的对象流中。
 *
 * @param <T> 解码输出流中对象的类型
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface HttpMessageReader<T> {

	/**
	 * 返回此读取器支持的媒体类型列表。该列表可能不适用于每种可能的目标元素类型，
	 * 调用此方法应通常通过 {@link #canRead(ResolvableType, MediaType) canWrite(elementType, null)} 进行保护。
	 * 列表也可能不包括仅支持特定元素类型的媒体类型。或者，使用 {@link #getReadableMediaTypes(ResolvableType)} 获取更精确的列表。
	 *
	 * @return 支持的通用媒体类型列表
	 */
	List<MediaType> getReadableMediaTypes();

	/**
	 * 返回此读取器针对给定元素类型支持的媒体类型列表。
	 * 如果读取器不支持元素类型，或者仅支持部分媒体类型，则此列表可能与 {@link #getReadableMediaTypes()} 不同。
	 *
	 * @param elementType 要读取的元素类型
	 * @return 支持给定类的媒体类型列表
	 * @since 5.3.4
	 */
	default List<MediaType> getReadableMediaTypes(ResolvableType elementType) {
		return (canRead(elementType, null) ? getReadableMediaTypes() : Collections.emptyList());
	}

	/**
	 * 检查此读取器是否支持给定对象类型。
	 *
	 * @param elementType 要检查的对象类型
	 * @param mediaType   读取的媒体类型（可能为{@code null}）
	 * @return 如果可读则为{@code true}，否则为{@code false}
	 */
	boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType);

	/**
	 * 从输入消息中读取并解码为对象流。
	 *
	 * @param elementType 要解码的流中的对象类型，必须通过 {@link #canRead(ResolvableType, MediaType)} 进行检查
	 * @param message     要从中读取的消息
	 * @param hints       关于如何读取和解码输入的附加信息
	 * @return 解码的元素流
	 */
	Flux<T> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints);

	/**
	 * 从输入消息中读取并解码为单个对象。
	 *
	 * @param elementType 要解码的流中的对象类型，必须通过 {@link #canRead(ResolvableType, MediaType)} 进行检查
	 * @param message     要从中读取的消息
	 * @param hints       关于如何读取和解码输入的附加信息
	 * @return 解码的对象
	 */
	Mono<T> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints);

	/**
	 * 服务器端的替代方法，与 {@link #read(ResolvableType, ReactiveHttpInputMessage, Map)} 类似，提供额外的上下文信息。
	 *
	 * @param actualType  实际的目标方法参数类型；
	 *                    对于带注解的控制器，可以通过 {@link ResolvableType#getSource()} 访问 {@link MethodParameter}。
	 * @param elementType 输出流中对象的类型
	 * @param request     当前请求
	 * @param response    当前响应
	 * @param hints       关于如何读取请求体的附加信息
	 * @return 解码的元素流
	 */
	default Flux<T> read(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request,
						 ServerHttpResponse response, Map<String, Object> hints) {

		return read(elementType, request, hints);
	}

	/**
	 * 服务器端的替代方法，与 {@link #readMono(ResolvableType, ReactiveHttpInputMessage, Map)} 类似，提供额外的上下文信息。
	 *
	 * @param actualType  实际的目标方法参数类型；
	 *                    对于带注解的控制器，可以通过 {@link ResolvableType#getSource()} 访问 {@link MethodParameter}。
	 * @param elementType 输出流中对象的类型
	 * @param request     当前请求
	 * @param response    当前响应
	 * @param hints       关于如何读取请求体的附加信息
	 * @return 解码的元素流
	 */
	default Mono<T> readMono(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request,
							 ServerHttpResponse response, Map<String, Object> hints) {

		return readMono(elementType, request, hints);
	}

}
