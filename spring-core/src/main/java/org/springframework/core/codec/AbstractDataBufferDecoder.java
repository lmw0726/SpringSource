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
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 抽象基类，用于 {@code Decoder} 实现，可以直接将 {@code DataBuffer} 解码为目标元素类型。
 *
 * <p>子类必须实现 {@link #decodeDataBuffer} 以提供将 {@code DataBuffer} 转换为目标数据类型的方法。
 * 默认的 {@link #decode} 实现转换每个单独的数据缓冲区，而 {@link #decodeToMono} 应用“减少”并转换聚合的缓冲区。
 *
 * <p>子类可以覆盖 {@link #decode} 以便沿不同边界（例如，针对 {@code String} 的换行符）拆分输入流，
 * 或者始终减少到一个数据缓冲区（例如，{@code Resource}）。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> 元素类型
 */
@SuppressWarnings("deprecation")
public abstract class AbstractDataBufferDecoder<T> extends AbstractDecoder<T> {

	private int maxInMemorySize = 256 * 1024;


	protected AbstractDataBufferDecoder(MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
	}


	/**
	 * 配置在需要聚合输入流时可以缓冲的字节数限制。
	 * 这可能是解码为单个 {@code DataBuffer}、{@link java.nio.ByteBuffer ByteBuffer}、
	 * {@code byte[]}、{@link org.springframework.core.io.Resource Resource}、
	 * {@code String} 等的结果。
	 * 也可能发生在拆分输入流时，例如带分隔符的文本，在这种情况下，限制适用于分隔符之间缓冲的数据。
	 * <p>默认情况下，此值设置为 256K。
	 * @param byteCount 要缓冲的最大字节数，或 -1 表示无限制
	 * @since 5.1.11
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
	}

	/**
	 * 返回 {@link #setMaxInMemorySize 配置的} 字节计数限制。
	 * @since 5.1.11
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}


	@Override
	public Flux<T> decode(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.from(input).map(buffer -> decodeDataBuffer(buffer, elementType, mimeType, hints));
	}

	@Override
	public Mono<T> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return DataBufferUtils.join(input, this.maxInMemorySize)
				.map(buffer -> decodeDataBuffer(buffer, elementType, mimeType, hints));
	}

	/**
	 * 如何将 {@code DataBuffer} 解码为目标元素类型。
	 * @deprecated 自 5.2 版本起已废弃，请转而实现
	 * {@link #decode(DataBuffer, ResolvableType, MimeType, Map)}
	 */
	@Deprecated
	@Nullable
	protected T decodeDataBuffer(DataBuffer buffer, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return decode(buffer, elementType, mimeType, hints);
	}

}
