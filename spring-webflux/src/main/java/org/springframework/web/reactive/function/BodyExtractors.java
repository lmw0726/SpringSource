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

package org.springframework.web.reactive.function;

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * {@link BodyExtractor} 实现的静态工厂方法。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class BodyExtractors {

	/**
	 * 表单数据的 ResolvableType
	 */
	private static final ResolvableType FORM_DATA_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	/**
	 * 多部分数据的 ResolvableType
	 */
	private static final ResolvableType MULTIPART_DATA_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class);

	/**
	 * Part 类型的 ResolvableType
	 */
	private static final ResolvableType PART_TYPE = ResolvableType.forClass(Part.class);

	/**
	 * Void 类型的 ResolvableType
	 */
	private static final ResolvableType VOID_TYPE = ResolvableType.forClass(Void.class);


	/**
	 * 将输入内容解码为 {@code Mono<T>} 的提取器。
	 *
	 * @param elementClass 要解码到的元素类型的类
	 * @param <T>          要解码到的元素类型
	 * @return {@code Mono<T>} 的 {@code BodyExtractor}
	 */
	public static <T> BodyExtractor<Mono<T>, ReactiveHttpInputMessage> toMono(Class<? extends T> elementClass) {
		return toMono(ResolvableType.forClass(elementClass));
	}

	/**
	 * 用于带有泛型类型信息的 {@link #toMono(Class)} 变体。
	 *
	 * @param elementTypeRef 要解码到的类型的类型引用
	 * @param <T>            要解码到的元素类型
	 * @return {@code Mono<T>} 的 {@code BodyExtractor}
	 */
	public static <T> BodyExtractor<Mono<T>, ReactiveHttpInputMessage> toMono(ParameterizedTypeReference<T> elementTypeRef) {
		return toMono(ResolvableType.forType(elementTypeRef.getType()));
	}

	private static <T> BodyExtractor<Mono<T>, ReactiveHttpInputMessage> toMono(ResolvableType elementType) {
		// 返回一个 BiFunction，处理输入消息和上下文
		return (inputMessage, context) ->
				readWithMessageReaders(inputMessage, context, elementType,
						// 使用 HttpMessageReader 读取到 Mono
						(HttpMessageReader<T> reader) -> readToMono(inputMessage, context, elementType, reader),
						// 将异常转换为 Mono
						ex -> Mono.from(unsupportedErrorHandler(inputMessage, ex)),
						// 跳过消息体并转为 Mono
						skipBodyAsMono(inputMessage));
	}

	/**
	 * 将输入内容解码为 {@code Flux<T>} 的提取器。
	 *
	 * @param elementClass 要解码到的元素类型的类
	 * @param <T>          要解码到的元素类型
	 * @return {@code Flux<T>} 的 {@code BodyExtractor}
	 */
	public static <T> BodyExtractor<Flux<T>, ReactiveHttpInputMessage> toFlux(Class<? extends T> elementClass) {
		return toFlux(ResolvableType.forClass(elementClass));
	}

	/**
	 * 用于带有泛型类型信息的 {@link #toFlux(Class)} 变体。
	 *
	 * @param typeRef 要解码到的类型的类型引用
	 * @param <T>     要解码到的元素类型
	 * @return {@code Flux<T>} 的 {@code BodyExtractor}
	 */
	public static <T> BodyExtractor<Flux<T>, ReactiveHttpInputMessage> toFlux(ParameterizedTypeReference<T> typeRef) {
		return toFlux(ResolvableType.forType(typeRef.getType()));
	}

	@SuppressWarnings("unchecked")
	private static <T> BodyExtractor<Flux<T>, ReactiveHttpInputMessage> toFlux(ResolvableType elementType) {
		// 返回一个 BiFunction，处理输入消息和上下文
		return (inputMessage, context) ->
				readWithMessageReaders(inputMessage, context, elementType,
						// 使用 HttpMessageReader 读取到 Flux
						(HttpMessageReader<T> reader) -> readToFlux(inputMessage, context, elementType, reader),
						// 将异常传递给不支持的错误处理器
						ex -> unsupportedErrorHandler(inputMessage, ex),
						// 跳过消息体并转为 Flux
						skipBodyAsFlux(inputMessage));
	}


	// 特定内容的提取器...

	/**
	 * 提取器，将表单数据读取为 {@code MultiValueMap<String, String>}。
	 * <p>从 5.1 开始，此方法还可用于客户端，用于从服务器响应中读取表单数据（例如 OAuth）。
	 *
	 * @return 用于表单数据的 {@code BodyExtractor}
	 */
	public static BodyExtractor<Mono<MultiValueMap<String, String>>, ReactiveHttpInputMessage> toFormData() {
		return (message, context) -> {
			// 定义 elementType 和 mediaType
			ResolvableType elementType = FORM_DATA_TYPE;
			MediaType mediaType = MediaType.APPLICATION_FORM_URLENCODED;

			// 查找对应的 HttpMessageReader
			HttpMessageReader<MultiValueMap<String, String>> reader = findReader(elementType, mediaType, context);

			// 读取到 Mono
			return readToMono(message, context, elementType, reader);
		};
	}

	/**
	 * 提取器，将多部分数据读取为 {@code MultiValueMap<String, Part>}。
	 *
	 * @return 用于多部分数据的 {@code BodyExtractor}
	 */
	// 为了服务器端使用而参数化
	public static BodyExtractor<Mono<MultiValueMap<String, Part>>, ServerHttpRequest> toMultipartData() {
		return (serverRequest, context) -> {
			// 定义 elementType 和 mediaType
			ResolvableType elementType = MULTIPART_DATA_TYPE;
			MediaType mediaType = MediaType.MULTIPART_FORM_DATA;

			// 查找对应的 HttpMessageReader
			HttpMessageReader<MultiValueMap<String, Part>> reader = findReader(elementType, mediaType, context);

			// 读取到 Mono
			return readToMono(serverRequest, context, elementType, reader);
		};
	}

	/**
	 * 提取器，将多部分数据读取为 {@code Flux<Part>}。
	 *
	 * @return 用于多部分请求部分的 {@code BodyExtractor}
	 */
	// 为了服务器端使用而参数化
	public static BodyExtractor<Flux<Part>, ServerHttpRequest> toParts() {
		return (serverRequest, context) -> {
			// 定义 elementType 和 mediaType
			ResolvableType elementType = PART_TYPE;
			MediaType mediaType = MediaType.MULTIPART_FORM_DATA;

			// 查找对应的 HttpMessageReader
			HttpMessageReader<Part> reader = findReader(elementType, mediaType, context);

			// 读取到 Flux
			return readToFlux(serverRequest, context, elementType, reader);
		};
	}

	/**
	 * 提取器，返回原始的 {@link DataBuffer DataBuffers}。
	 * <p><strong>注意：</strong>使用后应当{@link org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer) 释放}数据缓冲区。
	 *
	 * @return 用于数据缓冲区的 {@code BodyExtractor}
	 */
	public static BodyExtractor<Flux<DataBuffer>, ReactiveHttpInputMessage> toDataBuffers() {
		return (inputMessage, context) -> inputMessage.getBody();
	}


	// 私有支持方法

	/**
	 * 使用消息读取器从输入消息中读取数据。
	 *
	 * @param <T>            要读取的数据类型
	 * @param <S>            数据发布者的类型
	 * @param message        反应式 HTTP 输入消息
	 * @param context        读取上下文配置
	 * @param elementType    要读取的元素类型
	 * @param readerFunction 读取数据的函数
	 * @param errorFunction  处理不支持的媒体类型异常的函数
	 * @param emptySupplier  空数据发布者的提供者
	 * @return 从消息读取器中读取的数据发布者
	 */
	private static <T, S extends Publisher<T>> S readWithMessageReaders(
			ReactiveHttpInputMessage message, BodyExtractor.Context context, ResolvableType elementType,
			Function<HttpMessageReader<T>, S> readerFunction,
			Function<UnsupportedMediaTypeException, S> errorFunction,
			Supplier<S> emptySupplier) {

		// 检查是否为 Void 类型
		if (VOID_TYPE.equals(elementType)) {
			return emptySupplier.get();
		}

		// 获取内容类型，默认为 APPLICATION_OCTET_STREAM
		MediaType contentType = Optional.ofNullable(message.getHeaders().getContentType())
				.orElse(MediaType.APPLICATION_OCTET_STREAM);

		// 查找可读取该类型和内容类型的消息读取器
		return context.messageReaders().stream()
				.filter(reader -> reader.canRead(elementType, contentType))
				.findFirst()
				.map(BodyExtractors::<T>cast)
				.map(readerFunction)
				.orElseGet(() -> {
					// 如果找不到匹配的读取器，则构建不支持的媒体类型异常并应用错误函数
					List<MediaType> mediaTypes = context.messageReaders().stream()
							.flatMap(reader -> reader.getReadableMediaTypes(elementType).stream())
							.collect(Collectors.toList());
					return errorFunction.apply(
							new UnsupportedMediaTypeException(contentType, mediaTypes, elementType));
				});
	}

	/**
	 * 从反应式 HTTP 输入消息中读取数据到 Mono。
	 *
	 * @param <T>      要读取的数据类型
	 * @param message  反应式 HTTP 输入消息
	 * @param context  读取上下文配置
	 * @param type     要读取的数据类型
	 * @param reader   HTTP 消息读取器
	 * @return 读取的 Mono 数据
	 */
	private static <T> Mono<T> readToMono(ReactiveHttpInputMessage message, BodyExtractor.Context context,
										  ResolvableType type, HttpMessageReader<T> reader) {

		return context.serverResponse()
				.map(response -> reader.readMono(type, type, (ServerHttpRequest) message, response, context.hints()))
				.orElseGet(() -> reader.readMono(type, message, context.hints()));
	}

	/**
	 * 从反应式 HTTP 输入消息中读取数据到 Flux。
	 *
	 * @param <T>      要读取的数据类型
	 * @param message  反应式 HTTP 输入消息
	 * @param context  读取上下文配置
	 * @param type     要读取的数据类型
	 * @param reader   HTTP 消息读取器
	 * @return 读取的 Flux 数据
	 */
	private static <T> Flux<T> readToFlux(ReactiveHttpInputMessage message, BodyExtractor.Context context,
										  ResolvableType type, HttpMessageReader<T> reader) {

		return context.serverResponse()
				.map(response -> reader.read(type, type, (ServerHttpRequest) message, response, context.hints()))
				.orElseGet(() -> reader.read(type, message, context.hints()));
	}

	/**
	 * 处理不支持的媒体类型异常的方法，返回 Flux 数据流。
	 *
	 * @param <T> 反应式流中的数据类型
	 * @param message 反应式 HTTP 输入消息
	 * @param ex 不支持的媒体类型异常
	 * @return 处理不支持的媒体类型异常后的 Flux 数据流
	 */
	private static <T> Flux<T> unsupportedErrorHandler(
			ReactiveHttpInputMessage message, UnsupportedMediaTypeException ex) {

		Flux<T> result;
		if (message.getHeaders().getContentType() == null) {
			// 可能没有内容类型是可以接受的，如果没有内容..
			result = message.getBody().map(buffer -> {
				// 释放数据缓存
				DataBufferUtils.release(buffer);
				throw ex;
			});
		} else {
			result = message instanceof ClientHttpResponse ?
					consumeAndCancel(message).thenMany(Flux.error(ex)) : Flux.error(ex);
		}
		return result;
	}

	/**
	 * 根据元素类型和媒体类型在给定的上下文中查找对应的 HttpMessageReader。
	 *
	 * @param <T>          要读取的数据类型
	 * @param elementType  元素类型
	 * @param mediaType    媒体类型
	 * @param context      BodyExtractor 上下文
	 * @return             匹配的 HttpMessageReader
	 * @throws IllegalStateException 如果找不到适合指定类型的 HttpMessageReader
	 */
	private static <T> HttpMessageReader<T> findReader(
			ResolvableType elementType, MediaType mediaType, BodyExtractor.Context context) {

		return context.messageReaders().stream()
				.filter(messageReader -> messageReader.canRead(elementType, mediaType))
				.findFirst()
				.map(BodyExtractors::<T>cast)
				.orElseThrow(() -> new IllegalStateException(
						"No HttpMessageReader for \"" + mediaType + "\" and \"" + elementType + "\""));
	}

	/**
	 * 将给定的 HttpMessageReader 转换为特定类型的 HttpMessageReader。
	 *
	 * @param <T>    要转换的目标类型
	 * @param reader 要转换的 HttpMessageReader
	 * @return       转换后的特定类型的 HttpMessageReader
	 */
	@SuppressWarnings("unchecked")
	private static <T> HttpMessageReader<T> cast(HttpMessageReader<?> reader) {
		return (HttpMessageReader<T>) reader;
	}

	/**
	 * 返回一个供应商，用于生成空的 Flux。如果消息是 ClientHttpResponse 类型，则会消耗并取消消息体。
	 *
	 * @param <T>     Flux 的元素类型
	 * @param message 待处理的 ReactiveHttpInputMessage
	 * @return        生成空 Flux 的供应商
	 */
	private static <T> Supplier<Flux<T>> skipBodyAsFlux(ReactiveHttpInputMessage message) {
		return message instanceof ClientHttpResponse ?
				() -> consumeAndCancel(message).thenMany(Mono.empty()) : Flux::empty;
	}

	/**
	 * 返回一个供应商，用于生成空的 Mono。如果消息是 ClientHttpResponse 类型，则会消耗并取消消息体。
	 *
	 * @param <T>     Mono 的元素类型
	 * @param message 待处理的 ReactiveHttpInputMessage
	 * @return        生成空 Mono 的供应商
	 */
	@SuppressWarnings("unchecked")
	private static <T> Supplier<Mono<T>> skipBodyAsMono(ReactiveHttpInputMessage message) {
		return message instanceof ClientHttpResponse ?
				() -> consumeAndCancel(message).then(Mono.empty()) : Mono::empty;
	}

	/**
	 * 消耗并取消消息体，并返回一个 Flux 以执行此操作。
	 *
	 * @param message 待处理的 ReactiveHttpInputMessage
	 * @return        执行消耗并取消操作的 Flux
	 */
	private static Flux<DataBuffer> consumeAndCancel(ReactiveHttpInputMessage message) {
		return message.getBody().takeWhile(buffer -> {
			DataBufferUtils.release(buffer);
			return false;
		});
	}

}
