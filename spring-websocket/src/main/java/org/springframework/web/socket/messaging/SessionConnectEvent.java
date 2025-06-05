/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.socket.messaging;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

import java.security.Principal;

/**
 * 当使用简单消息协议（例如 STOMP）作为 WebSocket 子协议的新客户端发出连接请求时引发的事件。
 *
 * <p>请注意，这与 WebSocket 会话的建立不同，而是客户端在子协议内的第一次连接尝试，
 * 例如发送 STOMP CONNECT 帧。
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public class SessionConnectEvent extends AbstractSubProtocolEvent {

	/**
	 * 创建一个新的 SessionConnectEvent。
	 * @param source 发布该事件的组件（不能为空）
	 * @param message 连接消息
	 */
	public SessionConnectEvent(Object source, Message<byte[]> message) {
		super(source, message);
	}

	public SessionConnectEvent(Object source, Message<byte[]> message, @Nullable Principal user) {
		super(source, message, user);
	}

}
