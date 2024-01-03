/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function;

import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 一组函数的组合，可以填充 {@link ReactiveHttpOutputMessage} 的主体。
 *
 * @param <T> 要插入的数据类型
 * @param <M> 此插入器可应用于的 {@link ReactiveHttpOutputMessage} 的类型
 * @author Arjen Poutsma
 * @see BodyInserters
 * @since 5.0
 */
   @FunctionalInterface
public interface BodyInserter<T, M extends ReactiveHttpOutputMessage> {

	/**
	 * 插入到给定的输出消息中。
	 *
	 * @param outputMessage 要插入的响应
	 * @param context       要使用的上下文
	 * @return 一个表示完成或错误的 {@code Mono}
	 */
	Mono<Void> insert(M outputMessage, Context context);


	/**
	 * 定义插入过程中使用的上下文。
	 */
	interface Context {

		/**
		 * 返回用于响应主体转换的 {@link HttpMessageWriter HttpMessageWriters}。
		 *
		 * @return 消息写入器的流
		 */
		List<HttpMessageWriter<?>> messageWriters();

		/**
		 * 在可能的情况下返回 {@link ServerHttpRequest}。
		 */
		Optional<ServerHttpRequest> serverRequest();

		/**
		 * 返回用于响应主体转换的提示映射。
		 */
		Map<String, Object> hints();
	}

}
