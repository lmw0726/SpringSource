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
 * WebSocket 会话的处理程序。
 *
 * <p>服务器端的 {@code WebSocketHandler} 通过
 * {@link org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
 * SimpleUrlHandlerMapping} 和
 * {@link org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
 * WebSocketHandlerAdapter} 映射到请求。客户端的 {@code WebSocketHandler} 被传递给
 * {@link org.springframework.web.reactive.socket.client.WebSocketClient
 * WebSocketClient} 的 execute 方法。
 *
 * <p>使用 {@link WebSocketSession#receive() session.receive()} 来组合入站消息流，
 * 并使用 {@link WebSocketSession#send(Publisher) session.send(publisher)} 来处理出站消息流。
 * 下面是一个处理入站并发送出站消息的示例组合流程：
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
 * <p>如果处理入站和发送出站消息是独立的流程，可以使用 "zip" 操作符将它们合并在一起：
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
 * <p>{@code WebSocketHandler} 必须将入站和出站流组合成一个统一的流，并返回反映该流程完成的 {@code Mono<Void>}。
 * 这意味着无需检查连接是否打开，因为反应式流信号将终止活动。
 * 入站流接收到完成/错误信号，而出站流接收到取消信号。
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
