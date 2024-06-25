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

import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.http.codec.multipart.PartHttpMessageWriter;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * {@link ServerCodecConfigurer.ServerDefaultCodecs}的默认实现。
 *
 * @author Rossen Stoyanchev
 */
class ServerDefaultCodecsImpl extends BaseDefaultCodecs implements ServerCodecConfigurer.ServerDefaultCodecs {
	/**
	 * 多部分读取器
	 */
	@Nullable
	private HttpMessageReader<?> multipartReader;

	/**
	 * SSE编码器
	 */
	@Nullable
	private Encoder<?> sseEncoder;

	/**
	 * 构造一个新的 {@code ServerDefaultCodecsImpl} 实例。
	 */
	ServerDefaultCodecsImpl() {
	}

	/**
	 * 根据给定的 {@code ServerDefaultCodecsImpl} 实例创建一个新的实例。
	 *
	 * @param other 要复制的 {@code ServerDefaultCodecsImpl} 实例
	 */
	ServerDefaultCodecsImpl(ServerDefaultCodecsImpl other) {
		super(other);
		this.multipartReader = other.multipartReader;
		this.sseEncoder = other.sseEncoder;
	}

	/**
	 * 设置multipart读取器。
	 *
	 * @param reader multipart消息读取器
	 */
	@Override
	public void multipartReader(HttpMessageReader<?> reader) {
		this.multipartReader = reader;
		initTypedReaders();
	}

	/**
	 * 设置Server-Sent Events编码器。
	 *
	 * @param encoder Server-Sent Events编码器
	 */
	@Override
	public void serverSentEventEncoder(Encoder<?> encoder) {
		this.sseEncoder = encoder;
		initObjectWriters();
	}

	/**
	 * 扩展特定于服务器的类型读取器。
	 *
	 * @param typedReaders 类型读取器列表
	 */
	@Override
	protected void extendTypedReaders(List<HttpMessageReader<?>> typedReaders) {
		// 如果 多部分读取器 不为null，则将其添加到 类型读取器列表 并返回
		if (this.multipartReader != null) {
			addCodec(typedReaders, this.multipartReader);
			return;
		}

		// 创建一个新的DefaultPartHttpMessageReader实例
		DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();
		// 将 部分读取器 添加到 类型读取器列表
		addCodec(typedReaders, partReader);
		// 创建一个新的MultipartHttpMessageReader实例，并将 部分读取器 作为其参数
		addCodec(typedReaders, new MultipartHttpMessageReader(partReader));
	}

	/**
	 * 扩展特定于服务器的类型写入器。
	 *
	 * @param typedWriters 类型写入器列表
	 */
	@Override
	protected void extendTypedWriters(List<HttpMessageWriter<?>> typedWriters) {
		addCodec(typedWriters, new PartHttpMessageWriter());
	}

	/**
	 * 扩展特定于服务器的对象写入器。
	 *
	 * @param objectWriters 对象写入器列表
	 */
	@Override
	protected void extendObjectWriters(List<HttpMessageWriter<?>> objectWriters) {
		objectWriters.add(new ServerSentEventHttpMessageWriter(getSseEncoder()));
	}

	/**
	 * 获取Server-Sent Events编码器。
	 *
	 * @return Server-Sent Events编码器，如果不存在则返回null
	 */
	@Nullable
	private Encoder<?> getSseEncoder() {
		// 如果 SSE编码器 不为null，则返回 SSE编码器
		// 否则，根据条件返回对应的编码器，优先级为 SSE编码器 > Jackson 2 > Kotlin Serialization JSON > null
		return this.sseEncoder != null ? this.sseEncoder :
				jackson2Present ? getJackson2JsonEncoder() :
						kotlinSerializationJsonPresent ? getKotlinSerializationJsonEncoder() :
								null;
	}

}
