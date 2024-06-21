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

package org.springframework.http.codec;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;

/**
 * {@link CodecConfigurer} 的扩展，用于在客户端侧配置相关的HTTP消息读取器和写入器选项。
 *
 * <p>默认情况下，注册了以下类型的HTTP消息读取器：
 * <ul>{@code byte[]}
 * <li>{@link java.nio.ByteBuffer}
 * <li>{@link org.springframework.core.io.buffer.DataBuffer DataBuffer}
 * <li>{@link org.springframework.core.io.Resource Resource}
 * <li>{@link String}
 * <li>{@link org.springframework.util.MultiValueMap
 * MultiValueMap&lt;String,String&gt;} 用于表单数据
 * <li>如果Jackson存在，还有JSON和Smile
 * <li>如果JAXB2存在，还有XML
 * <li>用于服务器发送事件（SSE）
 * </ul>
 *
 * <p>默认情况下，注册了以下类型的HTTP消息写入器：
 * <ul>{@code byte[]}
 * <li>{@link java.nio.ByteBuffer}
 * <li>{@link org.springframework.core.io.buffer.DataBuffer DataBuffer}
 * <li>{@link org.springframework.core.io.Resource Resource}
 * <li>{@link String}
 * <li>{@link org.springframework.util.MultiValueMap
 * MultiValueMap&lt;String,String&gt;} 用于表单数据
 * <li>{@link org.springframework.util.MultiValueMap
 * MultiValueMap&lt;String,Object&gt;} 用于多部分数据
 * <li>如果Jackson存在，还有JSON和Smile
 * <li>如果JAXB2存在，还有XML
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ClientCodecConfigurer extends CodecConfigurer {

	/**
	 * {@inheritDoc}
	 * <p>在客户端侧，默认内置的默认值还包括与多部分读取器和写入器相关的定制，
	 * 以及SSE解码器。
	 */
	@Override
	ClientDefaultCodecs defaultCodecs();

	/**
	 * {@inheritDoc}.
	 */
	@Override
	ClientCodecConfigurer clone();


	/**
	 * 静态工厂方法，用于创建 {@code ClientCodecConfigurer}。
	 */
	static ClientCodecConfigurer create() {
		return CodecConfigurerFactory.create(ClientCodecConfigurer.class);
	}


	/**
	 * {@link CodecConfigurer.DefaultCodecs} 的扩展，包含额外的客户端侧选项。
	 */
	interface ClientDefaultCodecs extends DefaultCodecs {

		/**
		 * 配置编码器或写入器，用于与 {@link org.springframework.http.codec.multipart.MultipartHttpMessageWriter
		 * MultipartHttpMessageWriter} 一起使用。
		 */
		MultipartCodecs multipartCodecs();

		/**
		 * 配置用于服务器发送事件（SSE）的 {@code Decoder}。
		 * <p>默认情况下，如果未设置此项且Jackson可用，则使用 {@link #jackson2JsonDecoder} 覆盖。
		 * 如果要进一步自定义SSE解码器，请使用此属性。
		 * <p>注意，如果配置了 {@link #maxInMemorySize(int)}，将应用于给定的解码器。
		 *
		 * @param decoder 要使用的解码器
		 */
		void serverSentEventDecoder(Decoder<?> decoder);
	}


	/**
	 * 多部分HTTP消息写入器的注册和容器。
	 */
	interface MultipartCodecs {

		/**
		 * 添加一个部分 {@code Encoder}，内部包装为 {@link EncoderHttpMessageWriter}。
		 *
		 * @param encoder 要添加的编码器
		 */
		MultipartCodecs encoder(Encoder<?> encoder);

		/**
		 * 添加一个部分 {@link HttpMessageWriter}。对于类型为
		 * {@link EncoderHttpMessageWriter} 的写入器，考虑使用快捷方式
		 * {@link #encoder(Encoder)}。
		 *
		 * @param writer 要添加的写入器
		 */
		MultipartCodecs writer(HttpMessageWriter<?> writer);
	}

}
