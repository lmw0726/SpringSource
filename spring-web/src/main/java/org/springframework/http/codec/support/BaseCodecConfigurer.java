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

package org.springframework.http.codec.support;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.*;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * {@link CodecConfigurer} 的默认实现，作为客户端和服务器特定变体的基础。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
abstract class BaseCodecConfigurer implements CodecConfigurer {
	/**
	 * 默认编解码器
	 */
	protected final BaseDefaultCodecs defaultCodecs;

	/**
	 * 自定义编解码器
	 */
	protected final DefaultCustomCodecs customCodecs;


	/**
	 * 使用基础 {@link BaseDefaultCodecs} 构造器，可以是客户端或服务器特定变体。
	 */
	BaseCodecConfigurer(BaseDefaultCodecs defaultCodecs) {
		Assert.notNull(defaultCodecs, "'defaultCodecs' is required");
		this.defaultCodecs = defaultCodecs;
		this.customCodecs = new DefaultCustomCodecs();
	}

	/**
	 * 创建给定 {@link BaseCodecConfigurer} 的深层副本。
	 *
	 * @since 5.1.12
	 */
	protected BaseCodecConfigurer(BaseCodecConfigurer other) {
		this.defaultCodecs = other.cloneDefaultCodecs();
		this.customCodecs = new DefaultCustomCodecs(other.customCodecs);
	}

	/**
	 * 子类应该重写此方法，以创建 {@link BaseDefaultCodecs} 的深层副本，
	 * 可以是客户端或服务器特定的。
	 *
	 * @since 5.1.12
	 */
	protected abstract BaseDefaultCodecs cloneDefaultCodecs();


	@Override
	public DefaultCodecs defaultCodecs() {
		return this.defaultCodecs;
	}

	@Override
	public void registerDefaults(boolean shouldRegister) {
		this.defaultCodecs.registerDefaults(shouldRegister);
	}

	@Override
	public CustomCodecs customCodecs() {
		return this.customCodecs;
	}

	@Override
	public List<HttpMessageReader<?>> getReaders() {
		// 应用默认配置到自定义编解码器
		this.defaultCodecs.applyDefaultConfig(this.customCodecs);

		// 创建一个空的结果列表
		List<HttpMessageReader<?>> result = new ArrayList<>();

		// 将自定义编解码器中的类型化读取器添加到结果列表中
		result.addAll(this.customCodecs.getTypedReaders().keySet());

		// 将默认编解码器中的类型化读取器添加到结果列表中
		result.addAll(this.defaultCodecs.getTypedReaders());

		// 将自定义编解码器中的对象读取器添加到结果列表中
		result.addAll(this.customCodecs.getObjectReaders().keySet());

		// 将默认编解码器中的对象读取器添加到结果列表中
		result.addAll(this.defaultCodecs.getObjectReaders());

		// 将默认编解码器中的通用读取器添加到结果列表中
		result.addAll(this.defaultCodecs.getCatchAllReaders());

		// 返回填充了各种读取器的结果列表
		return result;
	}

	@Override
	public List<HttpMessageWriter<?>> getWriters() {
		// 应用默认配置到自定义编解码器
		this.defaultCodecs.applyDefaultConfig(this.customCodecs);

		// 创建一个空的结果列表
		List<HttpMessageWriter<?>> result = new ArrayList<>();

		// 将自定义编解码器中的类型化编码器添加到结果列表中
		result.addAll(this.customCodecs.getTypedWriters().keySet());

		// 将默认编解码器中的类型化编码器添加到结果列表中
		result.addAll(this.defaultCodecs.getTypedWriters());

		// 将自定义编解码器中的对象编码器添加到结果列表中
		result.addAll(this.customCodecs.getObjectWriters().keySet());

		// 将默认编解码器中的对象编码器添加到结果列表中
		result.addAll(this.defaultCodecs.getObjectWriters());

		// 将默认编解码器中的通用编码器添加到结果列表中
		result.addAll(this.defaultCodecs.getCatchAllWriters());

		// 返回填充了各种编码器的结果列表
		return result;
	}

	@Override
	public abstract CodecConfigurer clone();


	/**
	 * {@code CustomCodecs} 的默认实现。
	 */
	protected static final class DefaultCustomCodecs implements CustomCodecs {
		/**
		 * 类型读取器映射
		 */
		private final Map<HttpMessageReader<?>, Boolean> typedReaders = new LinkedHashMap<>(4);

		/**
		 * 类型写入器映射
		 */
		private final Map<HttpMessageWriter<?>, Boolean> typedWriters = new LinkedHashMap<>(4);

		/**
		 * 对象读取器映射
		 */
		private final Map<HttpMessageReader<?>, Boolean> objectReaders = new LinkedHashMap<>(4);

		/**
		 * 对象写入器映射
		 */
		private final Map<HttpMessageWriter<?>, Boolean> objectWriters = new LinkedHashMap<>(4);

		/**
		 * 默认配置消费者列表
		 */
		private final List<Consumer<DefaultCodecConfig>> defaultConfigConsumers = new ArrayList<>(4);

		DefaultCustomCodecs() {
		}

		/**
		 * 创建给定 {@link DefaultCustomCodecs} 的深层副本。
		 *
		 * @since 5.1.12
		 */
		DefaultCustomCodecs(DefaultCustomCodecs other) {
			this.typedReaders.putAll(other.typedReaders);
			this.typedWriters.putAll(other.typedWriters);
			this.objectReaders.putAll(other.objectReaders);
			this.objectWriters.putAll(other.objectWriters);
		}

		@Override
		public void register(Object codec) {
			addCodec(codec, false);
		}

		@Override
		public void registerWithDefaultConfig(Object codec) {
			addCodec(codec, true);
		}

		@Override
		public void registerWithDefaultConfig(Object codec, Consumer<DefaultCodecConfig> configConsumer) {
			addCodec(codec, false);
			this.defaultConfigConsumers.add(configConsumer);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void decoder(Decoder<?> decoder) {
			addCodec(decoder, false);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void encoder(Encoder<?> encoder) {
			addCodec(encoder, false);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void reader(HttpMessageReader<?> reader) {
			addCodec(reader, false);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void writer(HttpMessageWriter<?> writer) {
			addCodec(writer, false);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void withDefaultCodecConfig(Consumer<DefaultCodecConfig> codecsConfigConsumer) {
			this.defaultConfigConsumers.add(codecsConfigConsumer);
		}

		private void addCodec(Object codec, boolean applyDefaultConfig) {

			// 检查编解码器是否为解码器
			if (codec instanceof Decoder) {
				// 如果是解码器，创建对应的 DecoderHttpMessageReader
				codec = new DecoderHttpMessageReader<>((Decoder<?>) codec);
			} else if (codec instanceof Encoder) {
				// 如果编解码器是编码器，创建对应的 EncoderHttpMessageWriter
				codec = new EncoderHttpMessageWriter<>((Encoder<?>) codec);
			}

			// 如果编解码器是HttpMessageReader
			if (codec instanceof HttpMessageReader) {
				HttpMessageReader<?> reader = (HttpMessageReader<?>) codec;
				// 检查是否能够读取 Object 类型
				boolean canReadToObject = reader.canRead(ResolvableType.forClass(Object.class), null);
				// 如果可以读取 Object 类型，则将读取器和是否应用默认配置，添加到对象读取器中；否则添加到类型读取器中。
				(canReadToObject ? this.objectReaders : this.typedReaders).put(reader, applyDefaultConfig);
			} else if (codec instanceof HttpMessageWriter) {
				// 如果编解码器是 HttpMessageWriter
				HttpMessageWriter<?> writer = (HttpMessageWriter<?>) codec;
				// 检查是否能够写入 Object 类型
				boolean canWriteObject = writer.canWrite(ResolvableType.forClass(Object.class), null);
				// 如果可以写入，添加到对象写入器；否则添加到类型写入器
				(canWriteObject ? this.objectWriters : this.typedWriters).put(writer, applyDefaultConfig);
			} else {
				// 如果既不是解码器也不是编码器，则抛出异常
				throw new IllegalArgumentException("Unexpected codec type: " + codec.getClass().getName());
			}
		}

		// 包私有访问器...

		Map<HttpMessageReader<?>, Boolean> getTypedReaders() {
			return this.typedReaders;
		}

		Map<HttpMessageWriter<?>, Boolean> getTypedWriters() {
			return this.typedWriters;
		}

		Map<HttpMessageReader<?>, Boolean> getObjectReaders() {
			return this.objectReaders;
		}

		Map<HttpMessageWriter<?>, Boolean> getObjectWriters() {
			return this.objectWriters;
		}

		List<Consumer<DefaultCodecConfig>> getDefaultConfigConsumers() {
			return this.defaultConfigConsumers;
		}
	}

}
