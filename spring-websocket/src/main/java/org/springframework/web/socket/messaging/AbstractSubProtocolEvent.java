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

import org.springframework.context.ApplicationEvent;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import java.security.Principal;

/**
 * 用于处理从 WebSocket 客户端接收的消息并将其解析为更高级子协议（如 STOMP）的事件基类。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
@SuppressWarnings("serial")
public abstract class AbstractSubProtocolEvent extends ApplicationEvent {

	private final Message<byte[]> message;

	@Nullable
	private final Principal user;


	/**
	 * 创建一个新的 AbstractSubProtocolEvent。
	 * @param source 发布该事件的组件（不能为空）
	 * @param message 接收到的消息（不能为空）
	 */
	protected AbstractSubProtocolEvent(Object source, Message<byte[]> message) {
		this(source, message, null);
	}

	/**
	 * 创建一个新的 AbstractSubProtocolEvent。
	 * @param source 发布该事件的组件（不能为空）
	 * @param message 接收到的消息（不能为空）
	 * @param user 与该事件关联的会话用户，可为空
	 */
	protected AbstractSubProtocolEvent(Object source, Message<byte[]> message, @Nullable Principal user) {
		super(source);
		Assert.notNull(message, "Message must not be null");
		this.message = message;
		this.user = user;
	}


	/**
	 * 返回与该事件关联的 Message。以下示例演示了如何获取会话 ID 或消息头中的任何信息：
	 * <pre class="code">
	 * StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
	 * headers.getSessionId();
	 * headers.getSessionAttributes();
	 * headers.getPrincipal();
	 * </pre>
	 */
	public Message<byte[]> getMessage() {
		return this.message;
	}

	/**
	 * 返回与该事件关联的会话用户。
	 */
	@Nullable
	public Principal getUser() {
		return this.user;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + this.message + "]";
	}

}
