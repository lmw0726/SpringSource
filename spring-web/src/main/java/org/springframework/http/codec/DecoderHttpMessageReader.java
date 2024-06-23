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

import org.apache.commons.logging.Log;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Hints;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * {@code HttpMessageReader} 包装并委托给一个 {@link Decoder}。
 *
 * <p>同时也是一个 {@code HttpMessageReader}，从服务器端的额外信息（如请求或控制器方法参数注解）预先解析解码提示。
 *
 * @param <T> 解码后输出流中对象的类型
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DecoderHttpMessageReader<T> implements HttpMessageReader<T> {
	/**
	 * 解码器
	 */
	private final Decoder<T> decoder;

	/**
	 * 媒体类型列表
	 */
	private final List<MediaType> mediaTypes;


	/**
	 * 创建一个实例，包装给定的 {@link Decoder}。
	 *
	 * @param decoder 要包装的解码器
	 */
	public DecoderHttpMessageReader(Decoder<T> decoder) {
		Assert.notNull(decoder, "Decoder is required");
		initLogger(decoder);
		this.decoder = decoder;
		this.mediaTypes = MediaType.asMediaTypes(decoder.getDecodableMimeTypes());
	}

	private static void initLogger(Decoder<?> decoder) {
		// 如果 解码器 是 AbstractDecoder 的实例
		// 并且 解码器 的名称以 "org.springframework.core.codec" 开头
		if (decoder instanceof AbstractDecoder &&
				decoder.getClass().getName().startsWith("org.springframework.core.codec")) {
			// 获取 抽象解码器 的日志记录器，并创建对应的日志对象
			Log logger = HttpLogging.forLog(((AbstractDecoder<?>) decoder).getLogger());
			// 设置 解码器 的日志记录器
			((AbstractDecoder<?>) decoder).setLogger(logger);
		}
	}


	/**
	 * 返回该读取器的 {@link Decoder}。
	 */
	public Decoder<T> getDecoder() {
		return this.decoder;
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return this.mediaTypes;
	}

	@Override
	public List<MediaType> getReadableMediaTypes(ResolvableType elementType) {
		return MediaType.asMediaTypes(this.decoder.getDecodableMimeTypes(elementType));
	}

	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		return this.decoder.canDecode(elementType, mediaType);
	}

	@Override
	public Flux<T> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
		// 获取消息的内容类型
		MediaType contentType = getContentType(message);
		// 合并所有的提示信息
		Map<String, Object> allHints = Hints.merge(hints, getReadHints(elementType, message));
		// 使用解码器解码消息体
		return this.decoder.decode(message.getBody(), elementType, contentType, allHints);
	}

	@Override
	public Mono<T> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
		// 获取消息的内容类型
		MediaType contentType = getContentType(message);
		// 合并所有的提示信息
		Map<String, Object> allHints = Hints.merge(hints, getReadHints(elementType, message));
		// 使用解码器将消息体解码为 Mono
		return this.decoder.decodeToMono(message.getBody(), elementType, contentType, allHints);
	}

	/**
	 * 根据 "Content-Type" 头确定 HTTP 消息的内容类型，否则默认为 {@link MediaType#APPLICATION_OCTET_STREAM}。
	 *
	 * @param inputMessage HTTP 消息。
	 * @return 媒体类型，可能为 {@code null}。
	 */
	@Nullable
	protected MediaType getContentType(HttpMessage inputMessage) {
		// 获取输入消息的内容类型
		MediaType contentType = inputMessage.getHeaders().getContentType();
		// 如果内容类型不为空，则返回该类型；否则返回 application/octet-stream 内容类型
		return (contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);
	}

	/**
	 * 根据输入的 HTTP 消息获取解码的额外提示信息。
	 *
	 * @param elementType 要解码的元素类型。
	 * @param message     输入的 HTTP 消息。
	 * @return 解码的提示信息映射，始终返回空映射。
	 * @since 5.3
	 */
	protected Map<String, Object> getReadHints(ResolvableType elementType, ReactiveHttpInputMessage message) {
		return Hints.none();
	}


	// 仅服务器端...

	@Override
	public Flux<T> read(ResolvableType actualType, ResolvableType elementType,
						ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {
		// 合并所有的提示信息
		Map<String, Object> allHints = Hints.merge(hints,
				getReadHints(actualType, elementType, request, response));
		// 读取元素类型和所有提示信息
		return read(elementType, request, allHints);
	}

	@Override
	public Mono<T> readMono(ResolvableType actualType, ResolvableType elementType,
							ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {
		// 合并所有的提示信息
		Map<String, Object> allHints = Hints.merge(hints,
				getReadHints(actualType, elementType, request, response));

		// 读取元素类型和所有提示信息，并转为Mono
		return readMono(elementType, request, allHints);
	}

	/**
	 * 获取解码的额外提示信息，例如基于服务器请求或控制器方法参数的注解。默认情况下，如果解码器是 {@link HttpMessageDecoder} 的实例，则委托给解码器。
	 *
	 * @param actualType  目标方法参数的实际类型；对于带注解的控制器方法，可以通过 {@link ResolvableType#getSource()} 获取 {@link MethodParameter}。
	 * @param elementType 输出流中对象的类型。
	 * @param request     当前请求。
	 * @param response    当前响应。
	 * @return 解码的提示信息映射，如果解码器不是 {@link HttpMessageDecoder} 的实例，则返回空映射。
	 */
	protected Map<String, Object> getReadHints(ResolvableType actualType,
											   ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response) {

		// 如果解码器是 HttpMessageDecoder 的实例
		if (this.decoder instanceof HttpMessageDecoder) {
			// 将解码器转换为 HttpMessageDecoder 类型
			HttpMessageDecoder<?> decoder = (HttpMessageDecoder<?>) this.decoder;
			// 获取解码提示信息
			return decoder.getDecodeHints(actualType, elementType, request, response);
		}
		// 如果不是 HttpMessageDecoder 实例，则返回空的提示信息
		return Hints.none();
	}

}
