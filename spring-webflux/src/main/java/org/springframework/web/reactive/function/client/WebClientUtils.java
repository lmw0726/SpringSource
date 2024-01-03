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

package org.springframework.web.reactive.function.client;

import org.reactivestreams.Publisher;
import org.springframework.core.codec.CodecException;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Predicate;

/**
 * {@link DefaultWebClient} 和 {@link DefaultClientResponse} 之间共享的内部方法。
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
abstract class WebClientUtils {

	private static final String VALUE_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

	/**
	 * 判断是否应该包装异常的谓词。
	 */
	public final static Predicate<? super Throwable> WRAP_EXCEPTION_PREDICATE =
			t -> !(t instanceof WebClientException) && !(t instanceof CodecException);


	/**
	 * 将给定的响应映射为单值的 {@code ResponseEntity<T>}。
	 *
	 * @param response 客户端响应对象
	 * @param bodyMono 响应体的 Mono 对象
	 * @param <T>      响应体的类型
	 * @return 映射后的 {@code ResponseEntity<T>} 的 Mono 对象
	 */
	@SuppressWarnings("unchecked")
	public static <T> Mono<ResponseEntity<T>> mapToEntity(ClientResponse response, Mono<T> bodyMono) {
		return ((Mono<Object>) bodyMono).defaultIfEmpty(VALUE_NONE).map(body ->
				new ResponseEntity<>(
						body != VALUE_NONE ? (T) body : null,
						response.headers().asHttpHeaders(),
						response.rawStatusCode()));
	}

	/**
	 * 将给定的响应映射为 {@code ResponseEntity<List<T>>}。
	 *
	 * @param response 客户端响应对象
	 * @param body     包含元素的 Publisher 对象
	 * @param <T>      列表中元素的类型
	 * @return 映射后的 {@code ResponseEntity<List<T>>} 的 Mono 对象
	 */
	public static <T> Mono<ResponseEntity<List<T>>> mapToEntityList(ClientResponse response, Publisher<T> body) {
		return Flux.from(body).collectList().map(list ->
				new ResponseEntity<>(list, response.headers().asHttpHeaders(), response.rawStatusCode()));
	}

}
