/*
 * Copyright 2002-2020 the original author or authors.
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
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 抽象基类，用于只能处理单个值的 {@link org.springframework.core.codec.Encoder} 类。
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @param <T> 元素类型
 */
public abstract class AbstractSingleValueEncoder<T> extends AbstractEncoder<T> {


	public AbstractSingleValueEncoder(MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
	}


	@Override
	public final Flux<DataBuffer> encode(Publisher<? extends T> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.from(inputStream)
				.take(1)
				.concatMap(value -> encode(value, bufferFactory, elementType, mimeType, hints))
				.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
	}

	/**
	 * 将 {@code T} 编码为输出 {@link DataBuffer} 流。
	 * @param t 要处理的值
	 * @param dataBufferFactory 用于创建输出的缓冲区工厂
	 * @param type 要处理的流元素类型
	 * @param mimeType 要处理的 MIME 类型
	 * @param hints 关于如何解码的附加信息，可选
	 * @return 输出流
	 */
	protected abstract Flux<DataBuffer> encode(T t, DataBufferFactory dataBufferFactory,
			ResolvableType type, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

}
