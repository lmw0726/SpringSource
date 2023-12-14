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
 * Strategy to encode a stream of Objects of type {@code <T>} into an output
 * stream of bytes.
 *
 * @param <T> the type of elements in the input stream
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface Encoder<T> {

	/**
	 * Whether the encoder supports the given source element type and the MIME
	 * type for the output stream.
	 *
	 * @param elementType the type of elements in the source stream
	 * @param mimeType    the MIME type for the output stream
	 *                    (can be {@code null} if not specified)
	 * @return {@code true} if supported, {@code false} otherwise
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
	 * Encode an Object of type T to a data buffer. This is useful for scenarios,
	 * that distinct messages (or events) are encoded and handled individually,
	 * in fully aggregated form.
	 * <p>By default this method raises {@link UnsupportedOperationException}
	 * and it is expected that some encoders cannot produce a single buffer or
	 * cannot do so synchronously (e.g. encoding a {@code Resource}).
	 *
	 * @param value         the value to be encoded
	 * @param bufferFactory for creating the output {@code DataBuffer}
	 * @param valueType     the type for the value being encoded
	 * @param mimeType      the MIME type for the output content (optional)
	 * @param hints         additional information about how to encode
	 * @return the encoded content
	 * @since 5.2
	 */
	default DataBuffer encodeValue(T value, DataBufferFactory bufferFactory,
								   ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		// It may not be possible to produce a single DataBuffer synchronously
		throw new UnsupportedOperationException();
	}

	/**
	 * Return the list of MIME types supported by this Encoder. The list may not
	 * apply to every possible target element type and calls to this method should
	 * typically be guarded via {@link #canEncode(ResolvableType, MimeType)
	 * canEncode(elementType, null)}. The list may also exclude MIME types
	 * supported only for a specific element type. Alternatively, use
	 * {@link #getEncodableMimeTypes(ResolvableType)} for a more precise list.
	 *
	 * @return the list of supported MIME types
	 */
	List<MimeType> getEncodableMimeTypes();

	/**
	 * Return the list of MIME types supported by this Encoder for the given type
	 * of element. This list may differ from the {@link #getEncodableMimeTypes()}
	 * if the Encoder doesn't support the element type or if it supports it only
	 * for a subset of MIME types.
	 *
	 * @param elementType the type of element to check for encoding
	 * @return the list of MIME types supported for the given element type
	 * @since 5.3.4
	 */
	default List<MimeType> getEncodableMimeTypes(ResolvableType elementType) {
		return (canEncode(elementType, null) ? getEncodableMimeTypes() : Collections.emptyList());
	}

}
