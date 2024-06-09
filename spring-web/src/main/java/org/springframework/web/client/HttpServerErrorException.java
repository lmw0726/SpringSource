/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;

/**
 * 当收到HTTP 5xx时抛出的异常。
 *
 * @author Arjen Poutsma
 * @see DefaultResponseErrorHandler
 * @since 3.0
 */
public class HttpServerErrorException extends HttpStatusCodeException {

	private static final long serialVersionUID = -2915754006618138282L;


	/**
	 * 仅使用状态码的构造函数。
	 */
	public HttpServerErrorException(HttpStatus statusCode) {
		super(statusCode);
	}

	/**
	 * 使用状态码和状态文本的构造函数。
	 */
	public HttpServerErrorException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	/**
	 * 基于状态码、状态文本和内容构造的构造函数。
	 */
	public HttpServerErrorException(
			HttpStatus statusCode, String statusText, @Nullable byte[] body, @Nullable Charset charset) {

		super(statusCode, statusText, body, charset);
	}

	/**
	 * 基于状态码、状态文本、头部和内容构造的构造函数。
	 */
	public HttpServerErrorException(HttpStatus statusCode, String statusText,
									@Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset) {

		super(statusCode, statusText, headers, body, charset);
	}

	/**
	 * 基于状态码、状态文本、头部、内容和准备好的消息构造的构造函数。
	 *
	 * @since 5.2.2
	 */
	public HttpServerErrorException(String message, HttpStatus statusCode, String statusText,
									@Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset) {

		super(message, statusCode, statusText, headers, body, charset);
	}

	/**
	 * 创建一个{@code HttpServerErrorException}或特定于HTTP状态的子类。
	 *
	 * @since 5.1
	 */
	public static HttpServerErrorException create(HttpStatus statusCode,
												  String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

		return create(null, statusCode, statusText, headers, body, charset);
	}

	/**
	 * 与可选准备好的消息一起的{@link #create(String, HttpStatus, String, HttpHeaders, byte[], Charset)}的变体。
	 *
	 * @since 5.2.2。
	 */
	public static HttpServerErrorException create(@Nullable String message, HttpStatus statusCode,
												  String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

		// 根据状态码创建相应的 HttpServerErrorException 实例
		switch (statusCode) {
			case INTERNAL_SERVER_ERROR:
				// 内部服务器错误
				return message != null ?
						new HttpServerErrorException.InternalServerError(message, statusText, headers, body, charset) :
						new HttpServerErrorException.InternalServerError(statusText, headers, body, charset);
			case NOT_IMPLEMENTED:
				// 未实现
				return message != null ?
						new HttpServerErrorException.NotImplemented(message, statusText, headers, body, charset) :
						new HttpServerErrorException.NotImplemented(statusText, headers, body, charset);
			case BAD_GATEWAY:
				// 错误的网关
				return message != null ?
						new HttpServerErrorException.BadGateway(message, statusText, headers, body, charset) :
						new HttpServerErrorException.BadGateway(statusText, headers, body, charset);
			case SERVICE_UNAVAILABLE:
				// 服务不可用
				return message != null ?
						new HttpServerErrorException.ServiceUnavailable(message, statusText, headers, body, charset) :
						new HttpServerErrorException.ServiceUnavailable(statusText, headers, body, charset);
			case GATEWAY_TIMEOUT:
				// 网关超时
				return message != null ?
						new HttpServerErrorException.GatewayTimeout(message, statusText, headers, body, charset) :
						new HttpServerErrorException.GatewayTimeout(statusText, headers, body, charset);
			default:
				// 默认情况下，创建通用的 HttpServerErrorException 实例
				return message != null ?
						new HttpServerErrorException(message, statusCode, statusText, headers, body, charset) :
						new HttpServerErrorException(statusCode, statusText, headers, body, charset);
		}
	}


	// 专门针对特定HTTP状态码的子类

	/**
	 * {@link HttpServerErrorException}表示状态为HTTP 500 Internal Server Error。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class InternalServerError extends HttpServerErrorException {

		private InternalServerError(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.INTERNAL_SERVER_ERROR, statusText, headers, body, charset);
		}

		private InternalServerError(String message, String statusText,
									HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.INTERNAL_SERVER_ERROR, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpServerErrorException}表示状态为HTTP 501 Not Implemented。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class NotImplemented extends HttpServerErrorException {

		private NotImplemented(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.NOT_IMPLEMENTED, statusText, headers, body, charset);
		}

		private NotImplemented(String message, String statusText,
							   HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.NOT_IMPLEMENTED, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpServerErrorException}表示状态为HTTP 502 Bad Gateway。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class BadGateway extends HttpServerErrorException {

		private BadGateway(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.BAD_GATEWAY, statusText, headers, body, charset);
		}

		private BadGateway(String message, String statusText,
						   HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.BAD_GATEWAY, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpServerErrorException}表示状态为HTTP 503 Service Unavailable。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class ServiceUnavailable extends HttpServerErrorException {

		private ServiceUnavailable(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.SERVICE_UNAVAILABLE, statusText, headers, body, charset);
		}

		private ServiceUnavailable(String message, String statusText,
								   HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.SERVICE_UNAVAILABLE, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpServerErrorException}表示状态为HTTP 504 Gateway Timeout。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class GatewayTimeout extends HttpServerErrorException {

		private GatewayTimeout(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.GATEWAY_TIMEOUT, statusText, headers, body, charset);
		}

		private GatewayTimeout(String message, String statusText,
							   HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.GATEWAY_TIMEOUT, statusText, headers, body, charset);
		}
	}

}
