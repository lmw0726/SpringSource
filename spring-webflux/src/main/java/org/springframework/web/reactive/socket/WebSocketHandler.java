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

package org.springframework.web.reactive.socket;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * Handler for a WebSocket session.
 *
 * <p>A server {@code WebSocketHandler} is mapped to requests with
 * {@link org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
 * SimpleUrlHandlerMapping} and
 * {@link org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
 * WebSocketHandlerAdapter}. A client {@code WebSocketHandler} is passed to the
 * {@link org.springframework.web.reactive.socket.client.WebSocketClient
 * WebSocketClient} execute method.
 *
 * <p>Use {@link WebSocketSession#receive() session.receive()} to compose on
 * the inbound message stream, and {@link WebSocketSession#send(Publisher)
 * session.send(publisher)} for the outbound message stream. Below is an
 * example, combined flow to process inbound and to send outbound messages:
 *
 * <pre class="code">
 * class ExampleHandler implements WebSocketHandler {
 *
 * 	&#064;Override
 * 	public Mono&lt;Void&gt; handle(WebSocketSession session) {
 *
 * 		Flux&lt;WebSocketMessage&gt; output = session.receive()
 * 			.doOnNext(message -&gt; {
 * 				// ...
 *            })
 * 			.concatMap(message -&gt; {
 * 				// ...
 *            })
 * 			.map(value -&gt; session.textMessage("Echo " + value));
 *
 * 		return session.send(output);
 *    }
 * }
 * </pre>
 *
 * <p>If processing inbound and sending outbound messages are independent
 * streams, they can be joined together with the "zip" operator:
 *
 * <pre class="code">
 * class ExampleHandler implements WebSocketHandler {
 *
 * 	&#064;Override
 * 	public Mono&lt;Void&gt; handle(WebSocketSession session) {
 *
 * 		Mono&lt;Void&gt; input = session.receive()
 * 			.doOnNext(message -&gt; {
 * 				// ...
 *            })
 * 			.concatMap(message -&gt; {
 * 				// ...
 *            })
 * 			.then();
 *
 * 		Flux&lt;String&gt; source = ... ;
 * 		Mono&lt;Void&gt; output = session.send(source.map(session::textMessage));
 *
 * 		return Mono.zip(input, output).then();
 *    }
 * }
 * </pre>
 *
 * <p>A {@code WebSocketHandler} must compose the inbound and outbound streams
 * into a unified flow and return a {@code Mono<Void>} that reflects the
 * completion of that flow. That means there is no need to check if the
 * connection is open, since Reactive Streams signals will terminate activity.
 * The inbound stream receives a completion/error signal, and the outbound
 * stream receives a cancellation signal.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebSocketHandler {

	/**
	 * 获取此处理程序支持的子协议列表。
	 * <p>默认情况下，返回一个空列表。
	 */
	default List<String> getSubProtocols() {
		return Collections.emptyList();
	}

	/**
	 * 当建立新的WebSocket连接时调用，允许处理会话。
	 *
	 * <p>请参阅类级别的文档和参考手册，以获取有关如何处理会话的更多详细信息和示例。
	 *
	 * @param session 要处理的会话
	 * @return 表示应用程序处理会话何时完成的指示，
	 * 这应该反映入站消息流的完成（即连接关闭），
	 * 可能还包括出站消息流的完成和消息的编写
	 */
	Mono<Void> handle(WebSocketSession session);

}
