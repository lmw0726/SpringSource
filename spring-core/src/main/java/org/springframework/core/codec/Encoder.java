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
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 用于将类型为 {@code <T>} 的对象流编码为字节输出流的策略。
 *
 * @param <T> 输入流中元素的类型
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface Encoder<T> {

	/**
	 * 判断编码器是否支持给定的源元素类型和输出流的 MIME 类型。
	 *
	 * @param elementType 源流中元素的类型
	 * @param mimeType    输出流的 MIME 类型
	 * （如果未指定，可以为 {@code null}）
	 * @return 如果支持则返回 {@code true}，否则返回 {@code false}
	 */
	boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType);

	/**
	 * 将 {@code T} 类型的对象流编码为 {@link DataBuffer} 输出流。
	 *
	 * @param inputStream   要编码的对象的输入流。如果输入应编码为单个值而不是元素流，则应使用 {@link Mono} 的实例。
	 * @param bufferFactory 用于创建  {@link DataBuffer} 的输出流
	 * @param elementType   输入流中元素的预期类型;
	 *                      此类型必须先前已传递给 {@link #canEncode} 方法，并且必须返回 {@code true}。
	 * @param mimeType      输出内容的MIME类型 (可选)
	 * @param hints         有关如何编码的其他信息
	 * @return 输出流
	 */
	Flux<DataBuffer> encode(Publisher<? extends T> inputStream,
							DataBufferFactory bufferFactory,
							ResolvableType elementType,
							@Nullable MimeType mimeType,
							@Nullable Map<String, Object> hints);

	/**
	 * 将类型为 T 的对象编码为数据缓冲区。
	 * 这在需要对不同的消息（或事件）进行单独编码和处理，
	 * 且以完全聚合的形式呈现时非常有用。
	 * <p>默认情况下，此方法会抛出 {@link UnsupportedOperationException}，
	 * 因为预期某些编码器无法生成单个缓冲区，
	 * 或无法同步生成（例如编码 {@code Resource}）。
	 *
	 * @param value         要编码的值
	 * @param bufferFactory 用于创建输出 {@code DataBuffer} 的工厂
	 * @param valueType     被编码值的类型
	 * @param mimeType      输出内容的 MIME 类型（可选）
	 * @param hints         关于如何编码的附加信息
	 * @return 编码后的内容
	 * @since 5.2
	 */
	default DataBuffer encodeValue(T value, DataBufferFactory bufferFactory,
								   ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		// 可能无法同步生成单个 DataBuffer
		throw new UnsupportedOperationException();
	}

	/**
	 * 返回此编码器支持的 MIME 类型列表。
	 * 该列表可能并不适用于所有可能的目标元素类型，
	 * 因此调用此方法时通常应先通过
	 * {@link #canEncode(ResolvableType, MimeType) canEncode(elementType, null)}
	 * 进行检查。
	 * 列表中也可能不包含仅在特定元素类型下才支持的 MIME 类型。
	 * 或者，可以使用 {@link #getEncodableMimeTypes(ResolvableType)} 获取更精确的列表。
	 *
	 * @return 支持的 MIME 类型列表
	 */
	List<MimeType> getEncodableMimeTypes();

	/**
	 * 返回此编码器针对指定元素类型所支持的 MIME 类型列表。
	 * 如果编码器不支持该元素类型，或仅在某些 MIME 类型下支持，
	 * 则该列表可能与 {@link #getEncodableMimeTypes()} 不同。
	 *
	 * @param elementType 要检查编码支持的元素类型
	 * @return 针对指定元素类型所支持的 MIME 类型列表
	 * @since 5.3.4
	 */
	default List<MimeType> getEncodableMimeTypes(ResolvableType elementType) {
		return (canEncode(elementType, null) ? getEncodableMimeTypes() : Collections.emptyList());
	}

}
