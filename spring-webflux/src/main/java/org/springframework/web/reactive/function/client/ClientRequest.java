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

package org.springframework.web.reactive.function.client;

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 表示由 {@link ExchangeFunction} 执行的类型化的不可变的客户端 HTTP 请求。可以通过静态构建器方法创建此接口的实例。
 *
 * <p>请注意，应用程序更可能通过 {@link WebClient} 而不是直接使用此接口来执行请求。
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ClientRequest {

	/**
	 * {@link #attributes()} 属性的名称，其值可用于关联此请求的日志消息。
	 * 使用 {@link #logPrefix()} 获取基于此属性的一致格式化前缀。
	 *
	 * @see #logPrefix()
	 * @since 5.1
	 */
	String LOG_ID_ATTRIBUTE = ClientRequest.class.getName() + ".LOG_ID";


	/**
	 * 返回 HTTP 方法。
	 */
	HttpMethod method();

	/**
	 * 返回请求 URI。
	 */
	URI url();

	/**
	 * 返回此请求的头信息。
	 */
	HttpHeaders headers();

	/**
	 * 返回此请求的 Cookie。
	 */
	MultiValueMap<String, String> cookies();

	/**
	 * 返回此请求的 BodyInserter。
	 */
	BodyInserter<?, ? super ClientHttpRequest> body();

	/**
	 * 如果存在，返回请求属性值。
	 *
	 * @param name 属性名
	 * @return 属性值的 Optional
	 */
	default Optional<Object> attribute(String name) {
		return Optional.ofNullable(attributes().get(name));
	}

	/**
	 * 返回此请求的属性。
	 */
	Map<String, Object> attributes();

	/**
	 * 返回配置用于访问 {@link ClientHttpRequest} 的消费者。
	 *
	 * @since 5.3
	 */
	@Nullable
	Consumer<ClientHttpRequest> httpRequest();

	/**
	 * 返回用于关联此请求消息的日志消息前缀。
	 * 前缀基于属性 {@link #LOG_ID_ATTRIBUTE LOG_ID_ATTRIBUTE} 的值，用 "[" 和 "]" 括起来。
	 *
	 * @return 日志消息前缀，如果 {@link #LOG_ID_ATTRIBUTE LOG_ID_ATTRIBUTE} 未设置，则为空字符串。
	 * @since 5.1
	 */
	String logPrefix();

	/**
	 * 将此请求写入给定的 {@link ClientHttpRequest}。
	 *
	 * @param request    要写入的客户端 HTTP 请求
	 * @param strategies 写入时要使用的策略
	 * @return {@code Mono<Void>}，指示写入完成的信号
	 */
	Mono<Void> writeTo(ClientHttpRequest request, ExchangeStrategies strategies);


	// 静态构建器方法

	/**
	 * 使用给定请求的 HTTP 方法、URL、头部、cookies、属性和 body 创建一个构建器。
	 *
	 * @param other 要复制的请求
	 * @return 构建器实例
	 */
	static Builder from(ClientRequest other) {
		return new DefaultClientRequestBuilder(other);
	}

	/**
	 * 使用给定的 HTTP 方法和 URL 创建一个构建器。
	 *
	 * @param method HTTP 方法（GET、POST 等）
	 * @param url    URL（作为 URI 实例）
	 * @return 创建的构建器
	 * @deprecated 推荐使用 {@link #create(HttpMethod, URI)}
	 */
	@Deprecated
	static Builder method(HttpMethod method, URI url) {
		return new DefaultClientRequestBuilder(method, url);
	}

	/**
	 * 使用给定的 HTTP 方法和 URL 创建一个请求构建器。
	 *
	 * @param method HTTP 方法（GET、POST 等）
	 * @param url    URL（作为 URI 实例）
	 * @return 创建的构建器
	 */
	static Builder create(HttpMethod method, URI url) {
		return new DefaultClientRequestBuilder(method, url);
	}


	/**
	 * 定义请求的构建器。
	 */
	interface Builder {

		/**
		 * 设置请求的方法。
		 *
		 * @param method 新的方法
		 * @return 构建器本身
		 * @since 5.0.1
		 */
		Builder method(HttpMethod method);

		/**
		 * 设置请求的 URL。
		 *
		 * @param url 新的 URL
		 * @return 构建器本身
		 * @since 5.0.1
		 */
		Builder url(URI url);

		/**
		 * 在给定名称下添加给定的头部值。
		 *
		 * @param headerName   头部名称
		 * @param headerValues 头部值
		 * @return 构建器本身
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * 使用给定的消费者操作此请求的头部。提供给消费者的头部是“活动”的，
		 * 因此消费者可以用来重写现有的头部值，删除值或使用任何其他 HttpHeaders 方法。
		 *
		 * @param headersConsumer 操作 HttpHeaders 的函数
		 * @return 构建器本身
		 */
		Builder headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 使用给定名称和值(s)添加 cookie。
		 *
		 * @param name   cookie 名称
		 * @param values cookie 值
		 * @return 构建器本身
		 */
		Builder cookie(String name, String... values);

		/**
		 * 使用给定的消费者操作此请求的 cookies。提供给消费者的 map 是“活动”的，
		 * 因此消费者可以用来重写现有的 cookie 值，删除 cookies 或使用任何其他 MultiValueMap 方法。
		 *
		 * @param cookiesConsumer 操作 cookies map 的函数
		 * @return 构建器本身
		 */
		Builder cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

		/**
		 * 将请求的 body 设置为给定的 {@code BodyInserter}。
		 *
		 * @param inserter 写入请求的 {@code BodyInserter}
		 * @return 构建器本身
		 */
		Builder body(BodyInserter<?, ? super ClientHttpRequest> inserter);

		/**
		 * 将请求的 body 设置为给定的 {@code Publisher} 并返回它。
		 *
		 * @param publisher    要写入请求的 {@code Publisher}
		 * @param elementClass publisher 中包含的元素的类
		 * @param <S>          publisher 中元素的类型
		 * @param <P>          {@code Publisher} 的类型
		 * @return 构建的请求
		 */
		<S, P extends Publisher<S>> Builder body(P publisher, Class<S> elementClass);

		/**
		 * 将请求的 body 设置为给定的 {@code Publisher} 并返回它。
		 *
		 * @param publisher     要写入请求的 {@code Publisher}
		 * @param typeReference 用于描述 publisher 中包含的元素的类型引用
		 * @param <S>           publisher 中元素的类型
		 * @param <P>           {@code Publisher} 的类型
		 * @return 构建的请求
		 */
		<S, P extends Publisher<S>> Builder body(P publisher, ParameterizedTypeReference<S> typeReference);

		/**
		 * 将具有给定名称的属性设置为给定的值。
		 *
		 * @param name  要添加的属性的名称
		 * @param value 要添加的属性的值
		 * @return 构建器本身
		 */
		Builder attribute(String name, Object value);

		/**
		 * 使用给定的消费者操作请求属性。提供给消费者的属性是“活动”的，
		 * 因此消费者可以用来检查属性，删除属性或使用任何其他 map 提供的方法。
		 *
		 * @param attributesConsumer 消费属性的函数
		 * @return 构建器本身
		 */
		Builder attributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * 回调以访问 {@link ClientHttpRequest}，该请求进而提供对底层 HTTP 库的本机请求的访问。
		 * 这对于设置由底层库公开的高级请求特性可能很有用。
		 *
		 * @param requestConsumer 使用 {@code ClientHttpRequest} 的消费者
		 * @return 构建器本身
		 * @since 5.3
		 */
		Builder httpRequest(Consumer<ClientHttpRequest> requestConsumer);

		/**
		 * 构建请求。
		 */
		ClientRequest build();
	}

}
