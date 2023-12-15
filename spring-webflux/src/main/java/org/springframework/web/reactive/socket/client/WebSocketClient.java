/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.socket.client;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.WebSocketHandler;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 用于以响应式风格处理WebSocket会话的契约。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebSocketClient {

	/**
	 * 执行握手请求到指定的URL，并使用给定的处理程序处理WebSocket会话。
	 *
	 * @param url     握手的URL
	 * @param handler WebSocket会话的处理程序
	 * @return 表示WebSocket会话处理结果的完成Mono<Void>
	 */
	Mono<Void> execute(URI url, WebSocketHandler handler);

	/**
	 * 与{@link #execute(URI, WebSocketHandler)}的变体，带有自定义的头信息。
	 *
	 * @param url     握手的URL
	 * @param headers 握手请求的自定义头信息
	 * @param handler WebSocket会话的处理程序
	 * @return 表示WebSocket会话处理结果的完成Mono<Void>
	 */
	Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler);

}