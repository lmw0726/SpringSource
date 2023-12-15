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

package org.springframework.web.reactive.socket.adapter;

import io.undertow.websockets.core.*;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketMessage.Type;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Undertow {@link io.undertow.websockets.WebSocketConnectionCallback} 实现，
 * 它适应并委托给Spring {@link WebSocketHandler}。
 * UndertowWebSocketHandlerAdapter类，继承自AbstractReceiveListener。
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class UndertowWebSocketHandlerAdapter extends AbstractReceiveListener {

	/**
	 * UndertowWebSocketSession对象，用于处理WebSocket会话。
	 */
	private final UndertowWebSocketSession session;

	/**
	 * 构造一个UndertowWebSocketHandlerAdapter对象。
	 *
	 * @param session UndertowWebSocketSession对象
	 */
	public UndertowWebSocketHandlerAdapter(UndertowWebSocketSession session) {
		Assert.notNull(session, "UndertowWebSocketSession is required");
		this.session = session;
	}

	/**
	 * 处理完整的Text消息时调用。
	 *
	 * @param channel WebSocket通道
	 * @param message 缓冲文本消息
	 */
	@Override
	protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
		// 处理Text消息，并转换为消息类型为TEXT
		this.session.handleMessage(Type.TEXT, toMessage(Type.TEXT, message.getData()));
	}

	/**
	 * 处理完整的Binary消息时调用。
	 *
	 * @param channel WebSocket通道
	 * @param message 缓冲二进制消息
	 */
	@Override
	@SuppressWarnings("deprecation")
	protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) {
		// 处理Binary消息，并转换为消息类型为BINARY
		this.session.handleMessage(Type.BINARY, toMessage(Type.BINARY, message.getData().getResource()));
		// 释放消息数据资源
		message.getData().free();
	}

	/**
	 * 处理完整的Pong消息时调用。
	 *
	 * @param channel WebSocket通道
	 * @param message 缓冲二进制消息
	 */
	@Override
	@SuppressWarnings("deprecation")
	protected void onFullPongMessage(WebSocketChannel channel, BufferedBinaryMessage message) {
		// 处理Pong消息，并转换为消息类型为PONG
		this.session.handleMessage(Type.PONG, toMessage(Type.PONG, message.getData().getResource()));
		// 释放消息数据资源
		message.getData().free();
	}


	/**
	 * 处理完整的Close消息时调用。
	 *
	 * @param channel WebSocket通道
	 * @param message 缓冲二进制消息
	 */
	@Override
	@SuppressWarnings("deprecation")
	protected void onFullCloseMessage(WebSocketChannel channel, BufferedBinaryMessage message) {
		// 从消息数据中获取关闭消息
		CloseMessage closeMessage = new CloseMessage(message.getData().getResource());
		// 处理会话关闭，并设置关闭状态
		this.session.handleClose(CloseStatus.create(closeMessage.getCode(), closeMessage.getReason()));
		// 释放消息数据资源
		message.getData().free();
	}

	/**
	 * 处理WebSocket通道发生错误时调用。
	 *
	 * @param channel WebSocket通道
	 * @param error   异常信息
	 */
	@Override
	protected void onError(WebSocketChannel channel, Throwable error) {
		// 处理WebSocket通道发生的错误
		this.session.handleError(error);
	}

	/**
	 * 将WebSocket消息转换为WebSocketMessage。
	 *
	 * @param type    消息类型
	 * @param message 消息内容
	 * @param <T>     消息类型泛型
	 * @return WebSocketMessage对象
	 */
	private <T> WebSocketMessage toMessage(Type type, T message) {
		if (Type.TEXT.equals(type)) {
			// 如果消息类型为TEXT，将String类型的消息转换为字节数组，然后使用bufferFactory包装为DataBuffer
			byte[] bytes = ((String) message).getBytes(StandardCharsets.UTF_8);
			return new WebSocketMessage(Type.TEXT, this.session.bufferFactory().wrap(bytes));
		} else if (Type.BINARY.equals(type)) {
			// 如果消息类型为BINARY，使用bufferFactory分配DataBuffer并将ByteBuffer数组写入其中
			DataBuffer buffer = this.session.bufferFactory().allocateBuffer().write((ByteBuffer[]) message);
			return new WebSocketMessage(Type.BINARY, buffer);
		} else if (Type.PONG.equals(type)) {
			// 如果消息类型为PONG，使用bufferFactory分配DataBuffer并将ByteBuffer数组写入其中
			DataBuffer buffer = this.session.bufferFactory().allocateBuffer().write((ByteBuffer[]) message);
			return new WebSocketMessage(Type.PONG, buffer);
		} else {
			throw new IllegalArgumentException("Unexpected message type: " + message);
		}
	}

}
