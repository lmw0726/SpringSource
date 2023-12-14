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
 * Strategy for decoding a {@link DataBuffer} input stream into an output stream
 * of elements of type {@code <T>}.
 *
 * @param <T> the type of elements in the output stream
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface Decoder<T> {

	/**
	 * Whether the decoder supports the given target element type and the MIME
	 * type of the source stream.
	 *
	 * @param elementType the target element type for the output stream
	 * @param mimeType    the mime type associated with the stream to decode
	 *                    (can be {@code null} if not specified)
	 * @return {@code true} if supported, {@code false} otherwise
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
	 * Decode a data buffer to an Object of type T. This is useful for scenarios,
	 * that distinct messages (or events) are decoded and handled individually,
	 * in fully aggregated form.
	 *
	 * @param buffer     the {@code DataBuffer} to decode
	 * @param targetType the expected output type
	 * @param mimeType   the MIME type associated with the data
	 * @param hints      additional information about how to do decode
	 * @return the decoded value, possibly {@code null}
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
	 * Return the list of MIME types supported by this Decoder. The list may not
	 * apply to every possible target element type and calls to this method
	 * should typically be guarded via {@link #canDecode(ResolvableType, MimeType)
	 * canDecode(elementType, null)}. The list may also exclude MIME types
	 * supported only for a specific element type. Alternatively, use
	 * {@link #getDecodableMimeTypes(ResolvableType)} for a more precise list.
	 *
	 * @return the list of supported MIME types
	 */
	List<MimeType> getDecodableMimeTypes();

	/**
	 * Return the list of MIME types supported by this Decoder for the given type
	 * of element. This list may differ from {@link #getDecodableMimeTypes()}
	 * if the Decoder doesn't support the given element type or if it supports
	 * it only for a subset of MIME types.
	 *
	 * @param targetType the type of element to check for decoding
	 * @return the list of MIME types supported for the given target type
	 * @since 5.3.4
	 */
	default List<MimeType> getDecodableMimeTypes(ResolvableType targetType) {
		return (canDecode(targetType, null) ? getDecodableMimeTypes() : Collections.emptyList());
	}

}
