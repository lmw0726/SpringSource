/*
 * Copyright 2002-2017 the original author or authors.
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
 * 表示服务器对客户端连接请求的响应事件。
 * 参见 {@link org.springframework.web.socket.messaging.SessionConnectEvent}。
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public class SessionConnectedEvent extends AbstractSubProtocolEvent {

	/**
	 * 创建一个新的 SessionConnectedEvent。
	 * @param source 发布该事件的组件（不能为空）
	 * @param message 连接消息（不能为空）
	 */
	public SessionConnectedEvent(Object source, Message<byte[]> message) {
		super(source, message);
	}

	public SessionConnectedEvent(Object source, Message<byte[]> message, @Nullable Principal user) {
		super(source, message, user);
	}

}
