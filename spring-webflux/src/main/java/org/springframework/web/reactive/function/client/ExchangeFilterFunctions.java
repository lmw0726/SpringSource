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

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 提供用于基本身份验证、错误处理等 {@link ExchangeFilterFunction} 内置实现的静态工厂方法。
 *
 * @author Rob Winch
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 5.0
 */
public abstract class ExchangeFilterFunctions {

	/**
	 * {@link #basicAuthentication()} 使用的 {@link Credentials} 的请求属性的名称。
	 *
	 * @deprecated 自 Spring 5.1 起，推荐在构建请求时使用 {@link HttpHeaders#setBasicAuth(String, String)}。
	 */
	@Deprecated
	public static final String BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE =
			ExchangeFilterFunctions.class.getName() + ".basicAuthenticationCredentials";


	/**
	 * 从响应体中消耗指定字节数，并在到达更多数据时取消。
	 * <p>内部委托给 {@link DataBufferUtils#takeUntilByteCount}。
	 *
	 * @param maxByteCount 作为字节数的限制
	 * @return 用于限制响应大小的过滤器
	 * @since 5.1
	 */
	public static ExchangeFilterFunction limitResponseSize(long maxByteCount) {
		return (request, next) ->
				// 对下一个处理器的请求进行处理，映射为一个新的响应
				next.exchange(request)
						.map(response ->
								// 对响应进行变换，截取 body 数据直到达到最大字节数
								response.mutate()
										.body(body -> DataBufferUtils.takeUntilByteCount(body, maxByteCount))
										.build());
	}

	/**
	 * 返回一个过滤器，在给定的 {@link HttpStatus} 谓词匹配时生成错误信号。
	 *
	 * @param statusPredicate   用于检查 HTTP 状态的谓词
	 * @param exceptionFunction 用于创建异常的函数
	 * @return 生成错误信号的过滤器
	 */
	public static ExchangeFilterFunction statusError(Predicate<HttpStatus> statusPredicate,
													 Function<ClientResponse, ? extends Throwable> exceptionFunction) {
		Assert.notNull(statusPredicate, "Predicate must not be null");
		Assert.notNull(exceptionFunction, "Function must not be null");

		return ExchangeFilterFunction.ofResponseProcessor(
				response -> (statusPredicate.test(response.statusCode()) ?
						Mono.error(exceptionFunction.apply(response)) : Mono.just(response)));
	}

	/**
	 * 返回一个过滤器，通过 {@link HttpHeaders#setBasicAuth(String)} 和
	 * {@link HttpHeaders#encodeBasicAuth(String, String, Charset)} 向请求头添加 HTTP 基本身份验证。
	 *
	 * @param username 用户名
	 * @param password 密码
	 * @return 添加身份验证头的过滤器
	 * @see HttpHeaders#encodeBasicAuth(String, String, Charset)
	 * @see HttpHeaders#setBasicAuth(String)
	 */
	public static ExchangeFilterFunction basicAuthentication(String username, String password) {
		String encodedCredentials = HttpHeaders.encodeBasicAuth(username, password, null);

		return (request, next) ->
				// 使用下一个处理器处理请求，但在构建请求时设置基本身份验证头部信息
				next.exchange(ClientRequest.from(request)
						.headers(headers -> headers.setBasicAuth(encodedCredentials))
						.build());
	}

	/**
	 * {@link #basicAuthentication(String, String)} 的变体，从
	 * {@link #BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE 请求属性} 中查找
	 * {@link Credentials Credentials}。
	 *
	 * @return 要使用的过滤器
	 * @see Credentials
	 * @deprecated 自 Spring 5.1 起，推荐在构建请求时使用 {@link HttpHeaders#setBasicAuth(String, String)}。
	 */
	@Deprecated
	public static ExchangeFilterFunction basicAuthentication() {
		return (request, next) -> {
			// 检查请求属性中是否存在基本身份验证凭据
			Object attr = request.attributes().get(BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE);
			if (attr instanceof Credentials) {
				// 如果存在，提取凭据并使用它构建带有基本身份验证头部的新请求
				Credentials cred = (Credentials) attr;
				return next.exchange(ClientRequest.from(request)
						.headers(headers -> headers.setBasicAuth(cred.username, cred.password))
						.build());
			} else {
				// 如果不存在，直接使用原始请求
				return next.exchange(request);
			}
		};
	}


	/**
	 * 存储用于 HTTP 基本身份验证的用户名和密码。
	 *
	 * @deprecated 自 Spring 5.1 起，推荐在构建请求时使用 {@link HttpHeaders#setBasicAuth(String, String)}。
	 */
	@Deprecated
	public static final class Credentials {

		private final String username;

		private final String password;

		/**
		 * 使用给定的用户名和密码创建一个新的 {@code Credentials} 实例。
		 *
		 * @param username 用户名
		 * @param password 密码
		 */
		public Credentials(String username, String password) {
			Assert.notNull(username, "'username' must not be null");
			Assert.notNull(password, "'password' must not be null");
			this.username = username;
			this.password = password;
		}

		/**
		 * 返回一个 {@literal Consumer}，将给定的用户名和密码作为 {@code Credentials} 类型的请求属性存储，
		 * 然后由 {@link ExchangeFilterFunctions#basicAuthentication()} 使用。
		 *
		 * @param username 用户名
		 * @param password 密码
		 * @return 一个可传递给 {@linkplain ClientRequest.Builder#attributes(java.util.function.Consumer)} 的消费者
		 * @see ClientRequest.Builder#attributes(java.util.function.Consumer)
		 * @see #BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE
		 */
		public static Consumer<Map<String, Object>> basicAuthenticationCredentials(String username, String password) {
			Credentials credentials = new Credentials(username, password);
			return (map -> map.put(BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE, credentials));
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof Credentials)) {
				return false;
			}
			Credentials otherCred = (Credentials) other;
			return (this.username.equals(otherCred.username) && this.password.equals(otherCred.password));
		}

		@Override
		public int hashCode() {
			return 31 * this.username.hashCode() + this.password.hashCode();
		}
	}

}
