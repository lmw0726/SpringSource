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

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 表示由 {@link WebClient} 和 {@link ExchangeFunction} 返回的 HTTP 响应。提供对响应状态和头部的访问，
 * 还提供了消耗响应体的方法。
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ClientResponse {

	/**
	 * 以 {@link HttpStatus} 枚举值的形式返回 HTTP 状态码。
	 *
	 * @return HTTP 状态作为 HttpStatus 枚举值（绝不为 {@code null}）
	 * @throws IllegalArgumentException 如果是未知的 HTTP 状态码
	 * @see HttpStatus#valueOf(int)
	 * @since #getRawStatusCode()
	 */
	HttpStatus statusCode();

	/**
	 * 返回此响应的（可能非标准的）状态码。
	 *
	 * @return 以整数值表示的 HTTP 状态
	 * @see #statusCode()
	 * @see HttpStatus#resolve(int)
	 * @since 5.1
	 */
	int rawStatusCode();

	/**
	 * 返回此响应的头部。
	 */
	Headers headers();

	/**
	 * 返回此响应的 Cookie。
	 */
	MultiValueMap<String, ResponseCookie> cookies();

	/**
	 * 返回用于转换此响应体的策略。
	 */
	ExchangeStrategies strategies();

	/**
	 * 使用给定的 {@code BodyExtractor} 提取响应体。
	 *
	 * @param extractor 从响应中读取的 {@code BodyExtractor}
	 * @param <T>       返回体的类型
	 * @return 提取的响应体
	 */
	<T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor);

	/**
	 * 将响应体提取为 {@code Mono}。
	 *
	 * @param elementClass {@code Mono} 中元素的类
	 * @param <T>          元素类型
	 * @return 包含给定类型 {@code T} 的响应体的 mono
	 */
	<T> Mono<T> bodyToMono(Class<? extends T> elementClass);

	/**
	 * 将响应体提取为 {@code Mono}。
	 *
	 * @param elementTypeRef {@code Mono} 中元素的类型引用
	 * @param <T>            元素类型
	 * @return 包含给定类型 {@code T} 的响应体的 mono
	 */
	<T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef);

	/**
	 * 将响应体提取为 {@code Flux}。
	 *
	 * @param elementClass {@code Flux} 中元素的类
	 * @param <T>          元素类型
	 * @return 包含给定类型 {@code T} 的响应体的 flux
	 */
	<T> Flux<T> bodyToFlux(Class<? extends T> elementClass);

	/**
	 * 将响应体提取为 {@code Flux}。
	 *
	 * @param elementTypeRef {@code Flux} 中元素的类型引用
	 * @param <T>            元素类型
	 * @return 包含给定类型 {@code T} 的响应体的 flux
	 */
	<T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef);

	/**
	 * 释放此响应的响应体。
	 *
	 * @return 完成信号
	 * @see org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer)
	 * @since 5.2
	 */
	Mono<Void> releaseBody();

	/**
	 * 将此响应作为延迟的 {@code ResponseEntity} 返回。
	 *
	 * @param bodyClass 预期响应体类型
	 * @param <T>       响应体类型
	 * @return {@code Mono} 包含 {@code ResponseEntity}
	 */
	<T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyClass);

	/**
	 * 将此响应作为延迟的 {@code ResponseEntity} 返回。
	 *
	 * @param bodyTypeReference 描述预期响应体类型的类型引用
	 * @param <T>               响应体类型
	 * @return {@code Mono} 包含 {@code ResponseEntity}
	 */
	<T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeReference);

	/**
	 * 将此响应作为延迟的 {@code ResponseEntity} 列表返回。
	 *
	 * @param elementClass 预期响应体列表元素类
	 * @param <T>          列表中元素的类型
	 * @return {@code Mono} 包含 {@code ResponseEntity} 列表
	 */
	<T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass);

	/**
	 * 将此响应作为延迟的 {@code ResponseEntity} 列表返回。
	 *
	 * @param elementTypeRef 预期响应体列表元素引用类型
	 * @param <T>            列表中元素的类型
	 * @return {@code Mono} 包含 {@code ResponseEntity} 列表
	 */
	<T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef);

	/**
	 * 将此响应作为延迟的 {@code ResponseEntity} 返回，其中包含状态和标头，但不包含消息体。
	 * 调用此方法将会{@linkplain #releaseBody() 释放}响应的消息体。
	 *
	 * @return {@code Mono} 包含无消息体的 {@code ResponseEntity}
	 * @since 5.2
	 */
	Mono<ResponseEntity<Void>> toBodilessEntity();

	/**
	 * 创建一个包含响应状态、标头、消息体和源请求的 {@link WebClientResponseException}。
	 *
	 * @return 包含创建的异常的 {@code Mono}
	 * @since 5.2
	 */
	Mono<WebClientResponseException> createException();

	/**
	 * 返回一个日志消息前缀，用于关联此交换的消息。
	 * <p>前缀基于 {@linkplain ClientRequest#logPrefix()}，它本身基于 {@link ClientRequest#LOG_ID_ATTRIBUTE LOG_ID_ATTRIBUTE} 请求属性的值，
	 * 并进一步用 "[" 和 "]" 括起来。
	 *
	 * @return 日志消息前缀；如果 {@link ClientRequest#LOG_ID_ATTRIBUTE LOG_ID_ATTRIBUTE} 未设置，则返回空字符串
	 * @since 5.2.3
	 */
	String logPrefix();

	/**
	 * 返回一个用于变更此响应的构建器，例如更改状态、标头、Cookie，并替换或转换消息体。
	 *
	 * @return 用于变更响应的构建器
	 * @since 5.3
	 */
	default Builder mutate() {
		return new DefaultClientResponseBuilder(this, true);
	}


	// 静态生成器方法

	/**
	 * 使用给定的响应的状态、标头和 Cookie 创建一个构建器。
	 * <p><strong>注意：</strong> 返回的构建器中默认情况下消息体是 {@link Flux#empty()}。
	 * 若要使用原始响应的消息体，请使用 {@code otherResponse.bodyToFlux(DataBuffer.class)} 或直接使用基于实例的 {@link #mutate()} 方法。
	 *
	 * @param other 要从中复制状态、标头和 Cookie 的响应
	 * @return 创建的构建器
	 * @deprecated 自 5.3 起，推荐使用基于实例的 {@link #mutate()} 方法。
	 */
	@Deprecated
	static Builder from(ClientResponse other) {
		return new DefaultClientResponseBuilder(other, false);
	}

	/**
	 * 使用给定的状态码并使用默认策略创建响应构建器，用于读取消息体。
	 *
	 * @param statusCode 状态码
	 * @return 创建的构建器
	 */
	static Builder create(HttpStatus statusCode) {
		return create(statusCode, ExchangeStrategies.withDefaults());
	}

	/**
	 * 使用给定的状态码和读取消息体策略创建响应构建器。
	 *
	 * @param statusCode 状态码
	 * @param strategies 策略
	 * @return 创建的构建器
	 */
	static Builder create(HttpStatus statusCode, ExchangeStrategies strategies) {
		return new DefaultClientResponseBuilder(strategies).statusCode(statusCode);
	}

	/**
	 * 使用给定的原始状态码和读取消息体策略创建响应构建器。
	 *
	 * @param statusCode 原始状态码
	 * @param strategies 策略
	 * @return 创建的构建器
	 */
	static Builder create(int statusCode, ExchangeStrategies strategies) {
		return new DefaultClientResponseBuilder(strategies).rawStatusCode(statusCode);
	}

	/**
	 * 使用给定的状态码和消息体读取器创建响应构建器。
	 *
	 * @param statusCode     状态码
	 * @param messageReaders 消息体读取器
	 * @return 创建的构建器
	 */
	static Builder create(HttpStatus statusCode, List<HttpMessageReader<?>> messageReaders) {
		return create(statusCode, new ExchangeStrategies() {
			@Override
			public List<HttpMessageReader<?>> messageReaders() {
				return messageReaders;
			}

			@Override
			public List<HttpMessageWriter<?>> messageWriters() {
				// 响应中未使用
				return Collections.emptyList();
			}
		});
	}


	/**
	 * 表示 HTTP 响应的头部。
	 *
	 * @see ClientResponse#headers()
	 */
	interface Headers {

		/**
		 * 返回以字节为单位的响应体长度，由 {@code Content-Length} 头部指定。
		 */
		OptionalLong contentLength();

		/**
		 * 返回响应体的 {@linkplain MediaType 媒体类型}，由 {@code Content-Type} 头部指定。
		 */
		Optional<MediaType> contentType();

		/**
		 * 返回给定名称的头部的头部值（如果有）。
		 * <p>如果未找到头部值，则返回空列表。
		 *
		 * @param headerName 头部名称
		 */
		List<String> header(String headerName);

		/**
		 * 将头部作为 {@link HttpHeaders} 实例返回。
		 */
		HttpHeaders asHttpHeaders();
	}


	/**
	 * 定义响应的构建器。
	 */
	interface Builder {

		/**
		 * 设置响应的状态码。
		 *
		 * @param statusCode 新的状态码
		 * @return 此构建器
		 */
		Builder statusCode(HttpStatus statusCode);

		/**
		 * 设置响应的原始状态码。
		 *
		 * @param statusCode 新的状态码
		 * @return 此构建器
		 * @since 5.1.9
		 */
		Builder rawStatusCode(int statusCode);

		/**
		 * 在给定名称下添加给定的头部值。
		 *
		 * @param headerName   头部名称
		 * @param headerValues 头部值
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * 使用给定的消费者处理此响应的头部。
		 * <p>提供给消费者的头部是“活动的”，因此消费者可以用于
		 * {@linkplain HttpHeaders#set(String, String) 覆盖}现有的头部值，
		 * {@linkplain HttpHeaders#remove(Object) 移除}值，或使用任何其他
		 * {@link HttpHeaders} 方法。
		 *
		 * @param headersConsumer 消费 {@code HttpHeaders} 的函数
		 * @return 此构建器
		 */
		Builder headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 使用给定名称和值添加一个cookie。
		 *
		 * @param name   cookie 名称
		 * @param values cookie 值
		 * @return 此构建器
		 */
		Builder cookie(String name, String... values);

		/**
		 * 使用给定的消费者处理此响应的cookies。
		 * <p>提供给消费者的映射是“活动的”，因此消费者可以用于
		 * {@linkplain MultiValueMap#set(Object, Object) 覆盖}现有的 cookie 值，
		 * {@linkplain MultiValueMap#remove(Object) 移除}值，或使用任何其他
		 * {@link MultiValueMap} 方法。
		 *
		 * @param cookiesConsumer 消费 cookie 映射的函数
		 * @return 此构建器
		 */
		Builder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer);

		/**
		 * 转换响应体（如果在构建器中设置了）。
		 *
		 * @param transformer 要使用的转换函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder body(Function<Flux<DataBuffer>, Flux<DataBuffer>> transformer);

		/**
		 * 设置响应的主体。
		 * <p><strong>注意：</strong> 如果构建器中已设置了主体，此方法将会消耗现有的主体。
		 *
		 * @param body 新的主体
		 * @return 此构建器
		 */
		Builder body(Flux<DataBuffer> body);

		/**
		 * 将响应的主体设置为给定字符串的 UTF-8 编码字节。
		 * <p><strong>注意：</strong> 如果构建器中已设置了主体，此方法将会消耗现有的主体。
		 *
		 * @param body 新的主体
		 * @return 此构建器
		 */
		Builder body(String body);

		/**
		 * 设置与响应关联的请求。
		 *
		 * @param request 请求
		 * @return 此构建器
		 * @since 5.2
		 */
		Builder request(HttpRequest request);

		/**
		 * 构建响应。
		 */
		ClientResponse build();
	}

}
