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

package org.springframework.http;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;

/**
 * 一种 "reactive" HTTP 输入消息，将输入暴露为 {@link Publisher}。
 *
 * <p>通常由服务器端的 HTTP 请求或客户端端的响应来实现。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ReactiveHttpInputMessage extends HttpMessage {

	/**
	 * 将消息体作为 {@link Publisher} 返回。
	 *
	 * @return 消息体内容的发布者
	 */
	Flux<DataBuffer> getBody();

}
