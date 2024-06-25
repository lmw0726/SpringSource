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

package org.springframework.http.codec.support;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.*;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@link ClientCodecConfigurer.ClientDefaultCodecs}的默认实现。
 *
 * @author Rossen Stoyanchev
 */
class ClientDefaultCodecsImpl extends BaseDefaultCodecs implements ClientCodecConfigurer.ClientDefaultCodecs {
	/**
	 * 多部分编解码器
	 */
	@Nullable
	private DefaultMultipartCodecs multipartCodecs;

	/**
	 * SSE解码器
	 */
	@Nullable
	private Decoder<?> sseDecoder;

	/**
	 * 部分写入器提供者
	 */
	@Nullable
	private Supplier<List<HttpMessageWriter<?>>> partWritersSupplier;

	/**
	 * 构造一个新的 {@code ClientDefaultCodecsImpl} 实例。
	 */
	ClientDefaultCodecsImpl() {
	}

	/**
	 * 根据给定的 {@code ClientDefaultCodecsImpl} 实例创建一个新的实例。
	 *
	 * @param other 要复制的 {@code ClientDefaultCodecsImpl} 实例
	 */
	ClientDefaultCodecsImpl(ClientDefaultCodecsImpl other) {
		super(other);
		this.multipartCodecs = (other.multipartCodecs != null ?
				new DefaultMultipartCodecs(other.multipartCodecs) : null);
		this.sseDecoder = other.sseDecoder;
	}


	/**
	 * 设置一个供应商用于在未明确配置 {@link #multipartCodecs()} 时使用的部分写入器。
	 * 这与通用写入器相同，除了multipart写入器本身。
	 *
	 * @param supplier 部分写入器的供应商
	 */
	void setPartWritersSupplier(Supplier<List<HttpMessageWriter<?>>> supplier) {
		this.partWritersSupplier = supplier;
		// 初始化类型写入器
		initTypedWriters();
	}

	/**
	 * 返回多部分编解码器配置。
	 *
	 * @return 多部分编解码器配置
	 */
	@Override
	public ClientCodecConfigurer.MultipartCodecs multipartCodecs() {
		if (this.multipartCodecs == null) {
			// 如果 多部分编解码器 为空，创建一个默认的多部分编解码器
			this.multipartCodecs = new DefaultMultipartCodecs();
		}
		return this.multipartCodecs;
	}

	/**
	 * 设置Server-Sent Events解码器。
	 *
	 * @param decoder Server-Sent Events解码器
	 */
	@Override
	public void serverSentEventDecoder(Decoder<?> decoder) {
		this.sseDecoder = decoder;
		// 初始化对象读取器
		initObjectReaders();
	}

	/**
	 * 扩展特定于客户端的对象读取器。
	 *
	 * @param objectReaders 对象读取器列表
	 */
	@Override
	protected void extendObjectReaders(List<HttpMessageReader<?>> objectReaders) {

		// 创建一个Decoder对象，并根据条件选择不同的解码器
		Decoder<?> decoder = (this.sseDecoder != null ? this.sseDecoder :
				jackson2Present ? getJackson2JsonDecoder() :
						kotlinSerializationJsonPresent ? getKotlinSerializationJsonDecoder() :
								null);

		// 使用选定的解码器创建ServerSentEventHttpMessageReader对象，并添加到 对象读取器列表 中
		addCodec(objectReaders, new ServerSentEventHttpMessageReader(decoder));
	}

	/**
	 * 扩展特定于客户端的类型写入器。
	 *
	 * @param typedWriters 类型写入器列表
	 */
	@Override
	protected void extendTypedWriters(List<HttpMessageWriter<?>> typedWriters) {
		addCodec(typedWriters, new MultipartHttpMessageWriter(getPartWriters(), new FormHttpMessageWriter()));
	}

	/**
	 * 获取部分写入器列表。
	 *
	 * @return 部分写入器列表
	 */
	private List<HttpMessageWriter<?>> getPartWriters() {
		if (this.multipartCodecs != null) {
			// 如果 多部分编解码器 不为null，则返回其写入器
			return this.multipartCodecs.getWriters();
		} else if (this.partWritersSupplier != null) {
			// 否则，如果 部分写入器提供者 不为null，则获取 部分写入器
			return this.partWritersSupplier.get();
		} else {
			// 如果以上条件都不满足，则返回空列表
			return Collections.emptyList();
		}
	}


	/**
	 * {@link ClientCodecConfigurer.MultipartCodecs}的默认实现。
	 */
	private class DefaultMultipartCodecs implements ClientCodecConfigurer.MultipartCodecs {
		/**
		 * 写入器列表
		 */
		private final List<HttpMessageWriter<?>> writers = new ArrayList<>();

		/**
		 * 构造一个新的 {@code DefaultMultipartCodecs} 实例。
		 */
		DefaultMultipartCodecs() {
		}

		/**
		 * 根据给定的 {@code DefaultMultipartCodecs} 实例创建一个新的实例。
		 *
		 * @param other 要复制的 {@code DefaultMultipartCodecs} 实例
		 */
		DefaultMultipartCodecs(DefaultMultipartCodecs other) {
			this.writers.addAll(other.writers);
		}

		/**
		 * 配置多部分编码器。
		 *
		 * @param encoder 编码器
		 * @return 当前 {@code DefaultMultipartCodecs} 实例
		 */
		@Override
		public ClientCodecConfigurer.MultipartCodecs encoder(Encoder<?> encoder) {
			writer(new EncoderHttpMessageWriter<>(encoder));
			// 初始化类型写入器
			initTypedWriters();
			return this;
		}

		/**
		 * 添加一个消息写入器。
		 *
		 * @param writer 消息写入器
		 * @return 当前 {@code DefaultMultipartCodecs} 实例
		 */
		@Override
		public ClientCodecConfigurer.MultipartCodecs writer(HttpMessageWriter<?> writer) {
			this.writers.add(writer);
			// 初始化类型写入器
			initTypedWriters();
			return this;
		}

		/**
		 * 返回写入器列表。
		 *
		 * @return 写入器列表
		 */
		List<HttpMessageWriter<?>> getWriters() {
			return this.writers;
		}
	}

}
