/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * {@link DataBuffer DataBuffers} 的简单直通解码器。
 *
 * <p><strong>注意：</strong>数据缓冲区在使用后应通过
 * {@link org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer)}
 * 进行释放。此外，如果使用 {@code Flux} 或 {@code Mono} 运算符，例如 flatMap、reduce
 * 以及其他内部预取、缓存、跳过或过滤数据项的运算符，请将
 * {@code doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release)} 添加到组合链中，
 * 以确保在发生错误或取消信号之前释放缓存的数据缓冲区。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DataBufferDecoder extends AbstractDataBufferDecoder<DataBuffer> {

	public DataBufferDecoder() {
		super(MimeTypeUtils.ALL);
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return (DataBuffer.class.isAssignableFrom(elementType.toClass()) &&
				super.canDecode(elementType, mimeType));
	}

	@Override
	public Flux<DataBuffer> decode(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.from(input);
	}

	@Override
	public DataBuffer decode(DataBuffer buffer, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (logger.isDebugEnabled()) {
			logger.debug(Hints.getLogPrefix(hints) + "Read " + buffer.readableByteCount() + " bytes");
		}
		return buffer;
	}

}
