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

package org.springframework.web.reactive.socket.adapter;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.List;

/**
 * {@link WebSocketHandler} decorator that enriches the context of the target handler.
 *
 * @author Rossen Stoyanchev
 * @since 5.3.3
 */
public final class ContextWebSocketHandler implements WebSocketHandler {

	/**
	 * 代理 WebSocketHandler，用于处理WebSocket连接。
	 */
	private final WebSocketHandler delegate;

	/**
	 * 上下文视图，可能用于查看或管理当前的上下文信息。
	 */
	private final ContextView contextView;


	// 使用给定的WebSocket处理程序和上下文视图创建ContextWebSocketHandler实例
	private ContextWebSocketHandler(WebSocketHandler delegate, ContextView contextView) {
		this.delegate = delegate;
		this.contextView = contextView;
	}


	@Override
	public List<String> getSubProtocols() {
		// 调用委托的getSubProtocols方法
		return this.delegate.getSubProtocols();
	}

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		// 调用委托的handle方法，并使用contextView进行上下文写入
		return this.delegate.handle(session).contextWrite(this.contextView);
	}



	/**
	 * 返回给定的处理程序，用于插入给定上下文的装饰处理程序，
	 * 或者当上下文为空时返回相同的处理程序实例。
	 */
	public static WebSocketHandler decorate(WebSocketHandler handler, ContextView contextView) {
		// 如果上下文不为空，则返回一个新的ContextWebSocketHandler实例，否则返回原始处理程序
		return (!contextView.isEmpty() ? new ContextWebSocketHandler(handler, contextView) : handler);
	}

}
