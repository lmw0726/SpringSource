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

package org.springframework.web.reactive.socket;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Representation of a WebSocket message.
 *
 * <p>See static factory methods in {@link WebSocketSession} for creating messages with
 * the {@link org.springframework.core.io.buffer.DataBufferFactory} for the session.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebSocketMessage {

	private final Type type;

	private final DataBuffer payload;

	@Nullable
	private final Object nativeMessage;


	/**
	 * WebSocketMessage的构造函数。
	 * <p>参见{@link WebSocketSession}中的静态工厂方法，或者可以使用{@link WebSocketSession#bufferFactory()}创建载荷，
	 * 然后调用此构造函数。
	 */
	public WebSocketMessage(Type type, DataBuffer payload) {
		this(type, payload, null);
	}

	/**
	 * 具有对底层消息的访问权限的入站消息的构造函数。
	 *
	 * @param type          WebSocket消息的类型
	 * @param payload       消息内容
	 * @param nativeMessage 应用于底层WebSocket库API的消息（如果适用）。
	 * @since 5.3
	 */
	public WebSocketMessage(Type type, DataBuffer payload, @Nullable Object nativeMessage) {
		Assert.notNull(type, "'type' must not be null");
		Assert.notNull(payload, "'payload' must not be null");
		this.type = type;
		this.payload = payload;
		this.nativeMessage = nativeMessage;
	}

	/**
	 * 返回消息类型（文本、二进制等）。
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * 返回消息载荷。
	 */
	public DataBuffer getPayload() {
		return this.payload;
	}

	/**
	 * 返回底层WebSocket库的消息。仅适用于入站消息以及底层消息除内容外具有其他字段的情况。
	 * 目前仅适用于Reactor Netty。
	 *
	 * @param <T> 底层消息要转换的类型
	 * @return 底层消息，或者{@code null}
	 * @since 5.3
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T getNativeMessage() {
		return (T) this.nativeMessage;
	}

	/**
	 * {@link #getPayloadAsText(Charset)}的变体，使用{@code UTF-8}解码原始内容为文本。
	 */
	public String getPayloadAsText() {
		return getPayloadAsText(StandardCharsets.UTF_8);
	}

	/**
	 * 解码消息的原始内容为文本的快捷方式，使用给定的字符编码。对于文本WebSocket消息或者负载预计包含文本的情况，这很有用。
	 *
	 * @param charset 字符编码
	 * @since 5.0.5
	 */
	public String getPayloadAsText(Charset charset) {
		return this.payload.toString(charset);
	}

	/**
	 * 保留消息负载的数据缓冲区，对于使用池化缓冲区的运行时（例如Netty）很有用。一个快捷方式：
	 * <pre>
	 * DataBuffer payload = message.getPayload();
	 * DataBufferUtils.retain(payload);
	 * </pre>
	 *
	 * @see DataBufferUtils#retain(DataBuffer)
	 */
	public WebSocketMessage retain() {
		DataBufferUtils.retain(this.payload);
		return this;
	}

	/**
	 * 释放负载{@code DataBuffer}，对于使用池化缓冲区的运行时（例如Netty）很有用。一个快捷方式：
	 * <pre>
	 * DataBuffer payload = message.getPayload();
	 * DataBufferUtils.release(payload);
	 * </pre>
	 *
	 * @see DataBufferUtils#release(DataBuffer)
	 */
	public void release() {
		DataBufferUtils.release(this.payload);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WebSocketMessage)) {
			return false;
		}
		WebSocketMessage otherMessage = (WebSocketMessage) other;
		return (this.type.equals(otherMessage.type) &&
				ObjectUtils.nullSafeEquals(this.payload, otherMessage.payload));
	}

	@Override
	public int hashCode() {
		return this.type.hashCode() * 29 + this.payload.hashCode();
	}

	@Override
	public String toString() {
		return "WebSocket " + this.type.name() + " message (" + this.payload.readableByteCount() + " bytes)";
	}


	/**
	 * WebSocket消息类型枚举。
	 */
	public enum Type {
		/**
		 * 文本WebSocket消息。
		 */
		TEXT,
		/**
		 * 二进制WebSocket消息。
		 */
		BINARY,
		/**
		 * WebSocket ping。
		 */
		PING,
		/**
		 * WebSocket pong。
		 */
		PONG
	}

}
