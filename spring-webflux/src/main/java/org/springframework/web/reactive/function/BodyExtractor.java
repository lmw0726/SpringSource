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

import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 可从 {@link ReactiveHttpInputMessage} 主体中提取数据的函数。
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @param <T> 要提取的数据类型
 * @param <M> 此提取器可应用于的 {@link ReactiveHttpInputMessage} 类型
 * @see BodyExtractors
 */
@FunctionalInterface
public interface BodyExtractor<T, M extends ReactiveHttpInputMessage> {

	/**
	 * 从给定的输入消息中提取数据。
	 * @param inputMessage 要提取的请求
	 * @param context 要使用的配置
	 * @return 提取的数据
	 */
	T extract(M inputMessage, Context context);


	/**
	 * 定义提取过程中使用的上下文。
	 */
	interface Context {

		/**
		 * 返回用于主体提取的 {@link HttpMessageReader HttpMessageReaders}。
		 * @return 消息读取器流
		 */
		List<HttpMessageReader<?>> messageReaders();

		/**
		 * 可选地返回 {@link ServerHttpResponse}（如果存在）。
		 */
		Optional<ServerHttpResponse> serverResponse();

		/**
		 * 返回用于自定义主体提取的提示映射。
		 */
		Map<String, Object> hints();
	}

}
