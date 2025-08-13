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

package org.springframework.core.codec;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 将 {@link DataBuffer} 输入流解码为类型为 {@code <T>} 的元素输出流的策略。
 *
 * @param <T> 输出流中元素的类型
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface Decoder<T> {

	/**
	 * 判断解码器是否支持给定的目标元素类型和源流的 MIME 类型。
	 *
	 * @param elementType 输出流的目标元素类型
	 * @param mimeType    与要解码的流关联的 MIME 类型
	 * （如果未指定，可以为 {@code null}）
	 * @return 如果支持则返回 {@code true}，否则返回 {@code false}
	 */
	boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType);

	/**
	 * 将 {@link DataBuffer} 输入流解码为 {@code T} 的Flux。
	 *
	 * @param inputStream 要解码的 {@code DataBuffer} 输入流
	 * @param elementType 输出流中元素的预期类型;
	 *                    此类型必须先前已传递给 {@link #canDecode} 方法，并且必须返回 {@code true}。
	 * @param mimeType    与输入流关联的MIME类型 (可选)
	 * @param hints       有关如何进行解码的其他信息
	 * @return 具有已解码元素的输出流
	 */
	Flux<T> decode(Publisher<DataBuffer> inputStream,
				   ResolvableType elementType,
				   @Nullable MimeType mimeType,
				   @Nullable Map<String, Object> hints);

	/**
	 * 将 {@link DataBuffer} 输入流解码为 {@code T} 的Mono。
	 *
	 * @param inputStream 要解码的 {@code DataBuffer} 输入流
	 * @param elementType t输出流中元素的预期类型;
	 * 	 *                    此类型必须先前已传递给 {@link #canDecode} 方法，并且必须返回 {@code true}。
	 * @param mimeType    与输入流关联的MIME类型 (可选)
	 * @param hints      有关如何进行解码的其他信息
	 * @return 具有已解码元素的输出流
	 */
	Mono<T> decodeToMono(Publisher<DataBuffer> inputStream,
						 ResolvableType elementType,
						 @Nullable MimeType mimeType,
						 @Nullable Map<String, Object> hints);

	/**
	 * 将数据缓冲区解码为类型为 T 的对象。这对于以下场景非常有用：
	 * 独立的（或事件）消息以完全聚合的形式被解码和单独处理。
	 *
	 * @param buffer     要解码的 {@code DataBuffer}
	 * @param targetType 预期的输出类型
	 * @param mimeType   与数据关联的 MIME 类型
	 * @param hints      有关如何解码的附加信息
	 * @return 解码后的值，可能为 {@code null}
	 * @since 5.2
	 */
	@Nullable
	default T decode(DataBuffer buffer, ResolvableType targetType,
					 @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {

		CompletableFuture<T> future = decodeToMono(Mono.just(buffer), targetType, mimeType, hints).toFuture();
		Assert.state(future.isDone(), "DataBuffer decoding should have completed.");

		Throwable failure;
		try {
			return future.get();
		} catch (ExecutionException ex) {
			failure = ex.getCause();
		} catch (InterruptedException ex) {
			failure = ex;
		}
		throw (failure instanceof CodecException ? (CodecException) failure :
				new DecodingException("Failed to decode: " + failure.getMessage(), failure));
	}

	/**
	 * 返回此解码器支持的 MIME 类型列表。此列表可能不适用于
	 * 所有可能的目​​标元素类型，并且对该方法的调用通常应通过
	 * {@link #canDecode(ResolvableType, MimeType) canDecode(elementType, null)}
	 * 进行保护。该列表也可能排除仅针对特定元素类型支持的 MIME 类型。
	 * 或者，使用 {@link #getDecodableMimeTypes(ResolvableType)} 获取更精确的列表。
	 *
	 * @return 支持的 MIME 类型列表
	 */
	List<MimeType> getDecodableMimeTypes();

	/**
	 * 返回此解码器针对给定元素类型支持的 MIME 类型列表。
	 * 如果解码器不支持给定元素类型，或者仅支持其 MIME 类型的一个子集，
	 * 则此列表可能与 {@link #getDecodableMimeTypes()} 不同。
	 *
	 * @param targetType 要检查解码的元素类型
	 * @return 给定目标类型支持的 MIME 类型列表
	 * @since 5.3.4
	 */
	default List<MimeType> getDecodableMimeTypes(ResolvableType targetType) {
		return (canDecode(targetType, null) ? getDecodableMimeTypes() : Collections.emptyList());
	}

}
