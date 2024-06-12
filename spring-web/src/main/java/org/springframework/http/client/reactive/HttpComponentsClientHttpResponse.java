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

package org.springframework.http.client.reactive;

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.Message;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 Apache HttpComponents HttpClient 5.x 的 {@link ClientHttpResponse} 实现。
 *
 * @author Martin Tarjányi
 * @author Arjen Poutsma
 * @see <a href="https://hc.apache.org/index.html">Apache HttpComponents</a>
 * @since 5.3
 */
class HttpComponentsClientHttpResponse implements ClientHttpResponse {

	/**
	 * 数据缓冲区工厂，用于创建数据缓冲区。
	 */
	private final DataBufferFactory dataBufferFactory;

	/**
	 * HTTP 响应消息，包含 HTTP 响应头和消息体。
	 */
	private final Message<HttpResponse, Publisher<ByteBuffer>> message;

	/**
	 * HTTP 响应的头部信息。
	 */
	private final HttpHeaders headers;

	/**
	 * HTTP 客户端上下文。
	 */
	private final HttpClientContext context;

	/**
	 * 是否拒绝订阅者的原子布尔值，用于保证 HTTP 响应体只能被消费一次。
	 */
	private final AtomicBoolean rejectSubscribers = new AtomicBoolean();


	public HttpComponentsClientHttpResponse(DataBufferFactory dataBufferFactory,
											Message<HttpResponse, Publisher<ByteBuffer>> message, HttpClientContext context) {

		// 设置数据缓冲工厂
		this.dataBufferFactory = dataBufferFactory;

		// 设置消息
		this.message = message;

		// 设置上下文
		this.context = context;

		// 使用消息头创建一个HttpComponentsHeadersAdapter
		MultiValueMap<String, String> adapter = new HttpComponentsHeadersAdapter(message.getHead());
		// 将其转换为只读的HttpHeaders对象
		this.headers = HttpHeaders.readOnlyHttpHeaders(adapter);
	}


	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(this.message.getHead().getCode());
	}

	@Override
	public int getRawStatusCode() {
		return this.message.getHead().getCode();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		// 创建一个LinkedMultiValueMap来存储转换后的ResponseCookie对象
		LinkedMultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();

		// 遍历上下文中的CookieStore中的所有Cookie
		this.context.getCookieStore().getCookies().forEach(cookie ->
				// 将ClientResponse中的Cookie转换为ResponseCookie对象，并设置其属性
				result.add(cookie.getName(),
						ResponseCookie.fromClientResponse(cookie.getName(), cookie.getValue())
								.domain(cookie.getDomain())
								.path(cookie.getPath())
								// 获取最大过期时间
								.maxAge(getMaxAgeSeconds(cookie))
								.secure(cookie.isSecure())
								// 是否包含httponly属性
								.httpOnly(cookie.containsAttribute("httponly"))
								// 获取SameSite属性
								.sameSite(cookie.getAttribute("samesite"))
								.build()));

		// 返回转换后的结果
		return result;
	}

	private long getMaxAgeSeconds(Cookie cookie) {
		// 从Cookie中获取Max-Age属性值
		String maxAgeAttribute = cookie.getAttribute(Cookie.MAX_AGE_ATTR);

		// 如果Max-Age属性值不为null，则将其解析为长整型，否则返回-1
		return (maxAgeAttribute != null ? Long.parseLong(maxAgeAttribute) : -1);
	}

	@Override
	public Flux<DataBuffer> getBody() {
		// 从响应消息中获取数据流，并在订阅时执行操作
		return Flux.from(this.message.getBody())
				.doOnSubscribe(s -> {
					// 当订阅时执行的操作
					if (!this.rejectSubscribers.compareAndSet(false, true)) {
						// 如果拒绝订阅者的标志已经设置为true
						// 抛出异常，表示客户端响应主体只能消费一次
						throw new IllegalStateException("The client response body can only be consumed once.");
					}
				})
				// 映射每个数据缓冲，使用数据缓冲工厂对其进行包装
				.map(this.dataBufferFactory::wrap);
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

}
