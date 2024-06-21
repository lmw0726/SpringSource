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

package org.springframework.http.codec;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * 定义了一个通用接口，用于配置客户端或服务器端的HTTP消息读取器和写入器。使用方法如下：
 * <ul>
 * <li>使用 {@link ClientCodecConfigurer#create()} 或 {@link ServerCodecConfigurer#create()} 创建实例。
 * <li>使用 {@link #defaultCodecs()} 定制默认注册的HTTP消息读取器或写入器。
 * <li>使用 {@link #customCodecs()} 添加自定义的HTTP消息读取器或写入器。
 * <li>使用 {@link #getReaders()} 和 {@link #getWriters()} 获取配置的HTTP消息读取器和写入器列表。
 * </ul>
 *
 * <p>HTTP消息读取器和写入器分为以下3类，并按顺序排列：
 * <ol>
 * <li>支持特定类型的类型化读取器和写入器，例如 byte[], String。
 * <li>对象读取器和写入器，例如 JSON, XML。
 * <li>通用读取器或写入器，例如适用于任何媒体类型的String。
 * </ol>
 *
 * <p>类型化和对象读取器进一步分为以下子类，并按顺序排列：
 * <ol>
 * <li>默认的HTTP读取器和写入器注册。
 * <li>自定义的读取器和写入器。
 * </ol>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface CodecConfigurer {

	/**
	 * 提供一种定制或替换默认注册的HTTP消息读取器和写入器的方式。
	 *
	 * @see #registerDefaults(boolean)
	 */
	DefaultCodecs defaultCodecs();

	/**
	 * 注册自定义的HTTP消息读取器或写入器，除了默认注册的那些。
	 */
	CustomCodecs customCodecs();

	/**
	 * 提供了一种完全关闭默认注册的HTTP消息读取器和写入器，并且仅依赖于通过 {@link #customCodecs()} 提供的读取器和写入器的方式。
	 * <p>默认情况下，此项设置为 {@code true}，此时会进行默认注册；将其设置为 {@code false} 将禁用默认注册。
	 */
	void registerDefaults(boolean registerDefaults);


	/**
	 * 获取配置的HTTP消息读取器。
	 */
	List<HttpMessageReader<?>> getReaders();

	/**
	 * 获取配置的HTTP消息写入器。
	 */
	List<HttpMessageWriter<?>> getWriters();

	/**
	 * 创建此 {@link CodecConfigurer} 的副本。
	 * 返回的克隆对象具有自己的默认和自定义编解码器列表，通常可以独立配置。
	 * 不过需要注意的是，编解码器实例（如果已配置）本身不会被克隆。
	 *
	 * @since 5.1.12
	 */
	CodecConfigurer clone();


	/**
	 * 自定义或替换默认注册的HTTP消息读取器和写入器。
	 * 这些选项还可以通过 {@link ClientCodecConfigurer.ClientDefaultCodecs ClientDefaultCodecs}
	 * 和 {@link ServerCodecConfigurer.ServerDefaultCodecs ServerDefaultCodecs} 进行进一步扩展。
	 */
	interface DefaultCodecs {

		/**
		 * 覆盖默认的 Jackson JSON {@code Decoder}。
		 * <p>注意，如果配置了 {@link #maxInMemorySize(int)}，将应用于给定的解码器。
		 *
		 * @param decoder 要使用的解码器实例
		 * @see org.springframework.http.codec.json.Jackson2JsonDecoder
		 */
		void jackson2JsonDecoder(Decoder<?> decoder);

		/**
		 * 覆盖默认的 Jackson JSON {@code Encoder}。
		 *
		 * @param encoder 要使用的编码器实例
		 * @see org.springframework.http.codec.json.Jackson2JsonEncoder
		 */
		void jackson2JsonEncoder(Encoder<?> encoder);

		/**
		 * 覆盖默认的 Jackson Smile {@code Decoder}。
		 * <p>注意，如果配置了 {@link #maxInMemorySize(int)}，将应用于给定的解码器。
		 *
		 * @param decoder 要使用的解码器实例
		 * @see org.springframework.http.codec.json.Jackson2SmileDecoder
		 */
		void jackson2SmileDecoder(Decoder<?> decoder);

		/**
		 * 覆盖默认的 Jackson Smile {@code Encoder}。
		 *
		 * @param encoder 要使用的编码器实例
		 * @see org.springframework.http.codec.json.Jackson2SmileEncoder
		 */
		void jackson2SmileEncoder(Encoder<?> encoder);

		/**
		 * 覆盖默认的 Protobuf {@code Decoder}。
		 * <p>注意，如果配置了 {@link #maxInMemorySize(int)}，将应用于给定的解码器。
		 *
		 * @param decoder 要使用的解码器实例
		 * @see org.springframework.http.codec.protobuf.ProtobufDecoder
		 * @since 5.1
		 */
		void protobufDecoder(Decoder<?> decoder);

		/**
		 * 覆盖默认的 Protobuf {@code Encoder}。
		 *
		 * @param encoder 要使用的编码器实例
		 * @see org.springframework.http.codec.protobuf.ProtobufEncoder
		 * @see org.springframework.http.codec.protobuf.ProtobufHttpMessageWriter
		 * @since 5.1
		 */
		void protobufEncoder(Encoder<?> encoder);

		/**
		 * 覆盖默认的 JAXB2 {@code Decoder}。
		 * <p>注意，如果配置了 {@link #maxInMemorySize(int)}，将应用于给定的解码器。
		 *
		 * @param decoder 要使用的解码器实例
		 * @see org.springframework.http.codec.xml.Jaxb2XmlDecoder
		 * @since 5.1.3
		 */
		void jaxb2Decoder(Decoder<?> decoder);

		/**
		 * 覆盖默认的 JAXB2 {@code Encoder}。
		 *
		 * @param encoder 要使用的编码器实例
		 * @see org.springframework.http.codec.xml.Jaxb2XmlEncoder
		 * @since 5.1.3
		 */
		void jaxb2Encoder(Encoder<?> encoder);

		/**
		 * 覆盖默认的 Kotlin Serialization JSON {@code Decoder}。
		 *
		 * @param decoder 要使用的解码器实例
		 * @see org.springframework.http.codec.json.KotlinSerializationJsonDecoder
		 * @since 5.3
		 */
		void kotlinSerializationJsonDecoder(Decoder<?> decoder);

		/**
		 * 覆盖默认的 Kotlin Serialization JSON {@code Encoder}。
		 *
		 * @param encoder 要使用的编码器实例
		 * @see org.springframework.http.codec.json.KotlinSerializationJsonEncoder
		 * @since 5.3
		 */
		void kotlinSerializationJsonEncoder(Encoder<?> encoder);

		/**
		 * 注册一个消费者来应用于默认的配置实例。这可用于配置而不是替换特定的编解码器或多个编解码器。
		 * 消费者应用于每个默认的 {@link Encoder}、
		 * {@link Decoder}、{@link HttpMessageReader} 和 {@link HttpMessageWriter} 实例。
		 *
		 * @param codecConsumer 要应用的消费者
		 * @since 5.3.4
		 */
		void configureDefaultCodec(Consumer<Object> codecConsumer);

		/**
		 * 配置在需要聚合输入流时可以缓冲的最大字节数限制。这可以是解码到单个 {@code DataBuffer}、
		 * {@link java.nio.ByteBuffer ByteBuffer}、{@code byte[]}、
		 * {@link org.springframework.core.io.Resource Resource}、{@code String} 等的结果。它也可以发生在分割输入流时，
		 * 例如分隔文本，此时限制适用于分隔符之间的数据缓冲区。
		 * <p>默认情况下，此项未设置，此时适用个别编解码器的默认值。所有编解码器默认为最大256K。
		 *
		 * @param byteCount 要缓冲的最大字节数，或者为 -1 表示无限制
		 * @since 5.1.11
		 */
		void maxInMemorySize(int byteCount);

		/**
		 * 是否记录表单数据和头部的详细信息。这两者可能包含敏感信息。
		 * <p>默认设置为 {@code false}，因此不显示请求详细信息。
		 *
		 * @param enable 是否启用
		 * @since 5.1
		 */
		void enableLoggingRequestDetails(boolean enable);
	}


	/**
	 * 自定义HTTP消息读取器和写入器的注册表。
	 */
	interface CustomCodecs {

		/**
		 * 注册自定义编解码器。预期是以下之一：
		 * <ul>
		 * <li>{@link HttpMessageReader}
		 * <li>{@link HttpMessageWriter}
		 * <li>{@link Encoder}（内部使用 {@link EncoderHttpMessageWriter} 包装）
		 * <li>{@link Decoder}（内部使用 {@link DecoderHttpMessageReader} 包装）
		 * </ul>
		 *
		 * @param codec 要注册的编解码器
		 * @since 5.1.13
		 */
		void register(Object codec);

		/**
		 * {@link #register(Object)} 的变体，还通过 {@link #defaultCodecs()} 配置的属性应用于以下内容：
		 * <ul>
		 * <li>{@link CodecConfigurer.DefaultCodecs#maxInMemorySize(int) maxInMemorySize}
		 * <li>{@link CodecConfigurer.DefaultCodecs#enableLoggingRequestDetails(boolean) enableLoggingRequestDetails}
		 * </ul>
		 * <p>
		 * 每次使用 {@link #getReaders()} 或 {@link #getWriters()} 获取配置的读取器或写入器列表时，都会应用这些属性。
		 *
		 * @param codec 要注册并应用默认配置的编解码器
		 * @since 5.1.13
		 */
		void registerWithDefaultConfig(Object codec);

		/**
		 * {@link #register(Object)} 的变体，允许调用者将 {@link DefaultCodecConfig} 中的属性应用于给定的编解码器。
		 * 如果要应用所有属性，请优先使用 {@link #registerWithDefaultConfig(Object)}。
		 * <p>
		 * 每次使用 {@link #getReaders()} 或 {@link #getWriters()} 获取配置的读取器或写入器列表时，都会调用消费者。
		 *
		 * @param codec          要注册的编解码器
		 * @param configConsumer 默认配置的消费者
		 * @since 5.1.13
		 */
		void registerWithDefaultConfig(Object codec, Consumer<DefaultCodecConfig> configConsumer);

		/**
		 * 添加自定义 {@code Decoder}（内部使用 {@link DecoderHttpMessageReader} 包装）。
		 *
		 * @param decoder 要添加的解码器
		 * @deprecated 自5.1.13起，请改用 {@link #register(Object)} 或 {@link #registerWithDefaultConfig(Object)}。
		 */
		@Deprecated
		void decoder(Decoder<?> decoder);

		/**
		 * 添加自定义 {@code Encoder}（内部使用 {@link EncoderHttpMessageWriter} 包装）。
		 *
		 * @param encoder 要添加的编码器
		 * @deprecated 自5.1.13起，请改用 {@link #register(Object)} 或 {@link #registerWithDefaultConfig(Object)}。
		 */
		@Deprecated
		void encoder(Encoder<?> encoder);

		/**
		 * 添加自定义 {@link HttpMessageReader}。
		 * 对于类型为 {@link DecoderHttpMessageReader} 的读取器，请考虑使用快捷方式 {@link #decoder(Decoder)}。
		 *
		 * @param reader 要添加的读取器
		 * @deprecated 自5.1.13起，请改用 {@link #register(Object)} 或 {@link #registerWithDefaultConfig(Object)}。
		 */
		@Deprecated
		void reader(HttpMessageReader<?> reader);

		/**
		 * 添加自定义 {@link HttpMessageWriter}。
		 * 对于类型为 {@link EncoderHttpMessageWriter} 的写入器，请考虑使用快捷方式 {@link #encoder(Encoder)}。
		 *
		 * @param writer 要添加的写入器
		 * @deprecated 自5.1.13起，请改用 {@link #register(Object)} 或 {@link #registerWithDefaultConfig(Object)}。
		 */
		@Deprecated
		void writer(HttpMessageWriter<?> writer);

		/**
		 * 注册用于默认编解码器的 {@link DefaultCodecConfig 配置} 回调。
		 * 这允许自定义编解码器遵循应用于默认编解码器的一般准则，例如记录详细信息和限制缓冲数据量。
		 *
		 * @param codecsConfigConsumer 默认编解码器配置回调
		 * @deprecated 自5.1.13起，请改用 {@link #registerWithDefaultConfig(Object)} 或 {@link #registerWithDefaultConfig(Object, Consumer)}。
		 */
		@Deprecated
		void withDefaultCodecConfig(Consumer<DefaultCodecConfig> codecsConfigConsumer);
	}


	/**
	 * 通过 {@link #defaultCodecs()} 配置的属性值公开，这些属性值应用于默认编解码器。
	 * 此接口的主要目的是提供对它们的访问，以便在需要时也可以应用于自定义编解码器。
	 *
	 * @see CustomCodecs#registerWithDefaultConfig(Object, Consumer)
	 * @since 5.1.12
	 */
	interface DefaultCodecConfig {

		/**
		 * 获取配置的字节限制，用于每当需要聚合输入流时可以缓冲的字节数。
		 */
		@Nullable
		Integer maxInMemorySize();

		/**
		 * 是否以 DEBUG 级别记录表单数据，并以 TRACE 级别记录头信息。
		 * 这两者都可能包含敏感信息。
		 */
		@Nullable
		Boolean isEnableLoggingRequestDetails();
	}

}
