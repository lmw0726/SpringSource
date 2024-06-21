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

import org.springframework.core.codec.Encoder;

/**
 * 扩展 {@link CodecConfigurer}，用于服务器端相关的 HTTP 消息读取器和写入器选项。
 *
 * <p>默认注册的 HTTP 消息读取器包括：
 * <ul>{@code byte[]}
 * <li>{@link java.nio.ByteBuffer}
 * <li>{@link org.springframework.core.io.buffer.DataBuffer DataBuffer}
 * <li>{@link org.springframework.core.io.Resource Resource}
 * <li>{@link String}
 * <li>{@link org.springframework.util.MultiValueMap MultiValueMap&lt;String,String&gt;} 表单数据
 * <li>{@link org.springframework.util.MultiValueMap MultiValueMap&lt;String,Object&gt;} 多部分数据
 * <li>如果 Jackson 存在，支持 JSON 和 Smile
 * <li>如果 JAXB2 存在，支持 XML
 * </ul>
 *
 * <p>默认注册的 HTTP 消息写入器包括：
 * <ul>{@code byte[]}
 * <li>{@link java.nio.ByteBuffer}
 * <li>{@link org.springframework.core.io.buffer.DataBuffer DataBuffer}
 * <li>{@link org.springframework.core.io.Resource Resource}
 * <li>{@link String}
 * <li>{@link org.springframework.util.MultiValueMap MultiValueMap&lt;String,String&gt;} 表单数据
 * <li>如果 Jackson 存在，支持 JSON 和 Smile
 * <li>如果 JAXB2 存在，支持 XML
 * <li>服务器发送事件
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ServerCodecConfigurer extends CodecConfigurer {

	/**
	 * {@inheritDoc}
	 * <p>在服务器端，内置默认值还包括与 SSE 编码器相关的自定义设置。
	 */
	@Override
	ServerDefaultCodecs defaultCodecs();

	/**
	 * {@inheritDoc}.
	 */
	@Override
	ServerCodecConfigurer clone();


	/**
	 * 创建 {@code ServerCodecConfigurer} 的静态工厂方法。
	 */
	static ServerCodecConfigurer create() {
		return CodecConfigurerFactory.create(ServerCodecConfigurer.class);
	}


	/**
	 * {@link CodecConfigurer.DefaultCodecs} 扩展，具有额外的客户端选项。
	 */
	interface ServerDefaultCodecs extends DefaultCodecs {

		/**
		 * 配置用于多部分请求的 {@code HttpMessageReader}。
		 * <p>默认情况下，如果
		 * <a href="https://github.com/synchronoss/nio-multipart">Synchronoss NIO Multipart</a>
		 * 存在，这将设置为使用
		 * {@link org.springframework.http.codec.multipart.MultipartHttpMessageReader
		 * MultipartHttpMessageReader}，其由一个
		 * {@link org.springframework.http.codec.multipart.SynchronossPartHttpMessageReader
		 * SynchronossPartHttpMessageReader} 实例创建。
		 * <p>注意，如果配置了 {@link #maxInMemorySize(int)} 和/或
		 * {@link #enableLoggingRequestDetails(boolean)}，将应用于给定的读取器（如果适用）。
		 *
		 * @param reader 用于多部分请求的消息读取器。
		 * @since 5.1.11
		 */
		void multipartReader(HttpMessageReader<?> reader);

		/**
		 * 配置用于服务器发送事件的 {@code Encoder}。
		 * <p>默认情况下，如果未设置且 Jackson 可用，将使用 {@link #jackson2JsonEncoder} 替代。
		 * 使用此方法自定义 SSE 编码器。
		 */
		void serverSentEventEncoder(Encoder<?> encoder);
	}

}
