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
 * 当收到 HTTP 4xx 时抛出的异常。
 *
 * @author Arjen Poutsma
 * @see DefaultResponseErrorHandler
 * @since 3.0
 */
public class HttpClientErrorException extends HttpStatusCodeException {

	private static final long serialVersionUID = 5177019431887513952L;


	/**
	 * 仅包含状态码的构造方法。
	 */
	public HttpClientErrorException(HttpStatus statusCode) {
		super(statusCode);
	}

	/**
	 * 包含状态码和状态文本的构造方法。
	 */
	public HttpClientErrorException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	/**
	 * 包含状态码、状态文本和内容的构造方法。
	 */
	public HttpClientErrorException(
			HttpStatus statusCode, String statusText, @Nullable byte[] body, @Nullable Charset responseCharset) {

		super(statusCode, statusText, body, responseCharset);
	}

	/**
	 * 包含状态码、状态文本、头部和内容的构造方法。
	 */
	public HttpClientErrorException(HttpStatus statusCode, String statusText,
									@Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset responseCharset) {

		super(statusCode, statusText, headers, body, responseCharset);
	}

	/**
	 * 包含状态码、状态文本、头部、内容和准备消息的构造方法。
	 *
	 * @since 5.2.2
	 */
	public HttpClientErrorException(String message, HttpStatus statusCode, String statusText,
									@Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset responseCharset) {

		super(message, statusCode, statusText, headers, body, responseCharset);
	}


	/**
	 * 创建 {@code HttpClientErrorException} 或具体 HTTP 状态的子类。
	 *
	 * @since 5.1
	 */
	public static HttpClientErrorException create(
			HttpStatus statusCode, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

		return create(null, statusCode, statusText, headers, body, charset);
	}

	/**
	 * 可选准备消息的 {@link #create(HttpStatus, String, HttpHeaders, byte[], Charset)} 的变体。
	 *
	 * @since 5.2.2
	 */
	public static HttpClientErrorException create(@Nullable String message, HttpStatus statusCode,
												  String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

		// 根据状态码创建相应的 HttpClientErrorException 实例
		switch (statusCode) {
			case BAD_REQUEST:
				// 错误的请求
				return message != null ?
						new HttpClientErrorException.BadRequest(message, statusText, headers, body, charset) :
						new HttpClientErrorException.BadRequest(statusText, headers, body, charset);
			case UNAUTHORIZED:
				// 未授权
				return message != null ?
						new HttpClientErrorException.Unauthorized(message, statusText, headers, body, charset) :
						new HttpClientErrorException.Unauthorized(statusText, headers, body, charset);
			case FORBIDDEN:
				// 禁止访问
				return message != null ?
						new HttpClientErrorException.Forbidden(message, statusText, headers, body, charset) :
						new HttpClientErrorException.Forbidden(statusText, headers, body, charset);
			case NOT_FOUND:
				// 未找到资源
				return message != null ?
						new HttpClientErrorException.NotFound(message, statusText, headers, body, charset) :
						new HttpClientErrorException.NotFound(statusText, headers, body, charset);
			case METHOD_NOT_ALLOWED:
				// 方法不允许
				return message != null ?
						new HttpClientErrorException.MethodNotAllowed(message, statusText, headers, body, charset) :
						new HttpClientErrorException.MethodNotAllowed(statusText, headers, body, charset);
			case NOT_ACCEPTABLE:
				// 不可接受的内容
				return message != null ?
						new HttpClientErrorException.NotAcceptable(message, statusText, headers, body, charset) :
						new HttpClientErrorException.NotAcceptable(statusText, headers, body, charset);
			case CONFLICT:
				// 冲突
				return message != null ?
						new HttpClientErrorException.Conflict(message, statusText, headers, body, charset) :
						new HttpClientErrorException.Conflict(statusText, headers, body, charset);
			case GONE:
				// 资源不存在
				return message != null ?
						new HttpClientErrorException.Gone(message, statusText, headers, body, charset) :
						new HttpClientErrorException.Gone(statusText, headers, body, charset);
			case UNSUPPORTED_MEDIA_TYPE:
				// 不支持的媒体类型
				return message != null ?
						new HttpClientErrorException.UnsupportedMediaType(message, statusText, headers, body, charset) :
						new HttpClientErrorException.UnsupportedMediaType(statusText, headers, body, charset);
			case TOO_MANY_REQUESTS:
				// 请求过多
				return message != null ?
						new HttpClientErrorException.TooManyRequests(message, statusText, headers, body, charset) :
						new HttpClientErrorException.TooManyRequests(statusText, headers, body, charset);
			case UNPROCESSABLE_ENTITY:
				// 请求实体无法处理
				return message != null ?
						new HttpClientErrorException.UnprocessableEntity(message, statusText, headers, body, charset) :
						new HttpClientErrorException.UnprocessableEntity(statusText, headers, body, charset);
			default:
				// 默认情况下，创建通用的 HttpClientErrorException 实例
				return message != null ?
						new HttpClientErrorException(message, statusCode, statusText, headers, body, charset) :
						new HttpClientErrorException(statusCode, statusText, headers, body, charset);
		}
	}


	// 特定HTTP状态代码的子类。

	/**
	 * 表示状态为 HTTP 400 Bad Request 的 {@link HttpClientErrorException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class BadRequest extends HttpClientErrorException {

		private BadRequest(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.BAD_REQUEST, statusText, headers, body, charset);
		}

		private BadRequest(String message, String statusText,
						   HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.BAD_REQUEST, statusText, headers, body, charset);
		}
	}


	/**
	 * 表示状态为 HTTP 401 Unauthorized 的 {@link HttpClientErrorException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class Unauthorized extends HttpClientErrorException {

		private Unauthorized(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.UNAUTHORIZED, statusText, headers, body, charset);
		}

		private Unauthorized(String message, String statusText,
							 HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.UNAUTHORIZED, statusText, headers, body, charset);
		}
	}

	/**
	 * 表示状态为 HTTP 403 Forbidden 的 {@link HttpClientErrorException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class Forbidden extends HttpClientErrorException {

		private Forbidden(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.FORBIDDEN, statusText, headers, body, charset);
		}

		private Forbidden(String message, String statusText,
						  HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.FORBIDDEN, statusText, headers, body, charset);
		}
	}

	/**
	 * 表示状态为 HTTP 404 Not Found 的 {@link HttpClientErrorException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class NotFound extends HttpClientErrorException {

		private NotFound(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.NOT_FOUND, statusText, headers, body, charset);
		}

		private NotFound(String message, String statusText,
						 HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.NOT_FOUND, statusText, headers, body, charset);
		}
	}

	/**
	 * 表示状态为 HTTP 405 Method Not Allowed 的 {@link HttpClientErrorException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class MethodNotAllowed extends HttpClientErrorException {

		private MethodNotAllowed(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.METHOD_NOT_ALLOWED, statusText, headers, body, charset);
		}

		private MethodNotAllowed(String message, String statusText,
								 HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.METHOD_NOT_ALLOWED, statusText, headers, body, charset);
		}
	}

	/**
	 * 表示状态为 HTTP 406 Not Acceptable 的 {@link HttpClientErrorException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class NotAcceptable extends HttpClientErrorException {

		private NotAcceptable(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.NOT_ACCEPTABLE, statusText, headers, body, charset);
		}

		private NotAcceptable(String message, String statusText,
							  HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.NOT_ACCEPTABLE, statusText, headers, body, charset);
		}
	}

	/**
	 * 表示状态为 HTTP 409 Conflict 的 {@link HttpClientErrorException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class Conflict extends HttpClientErrorException {

		private Conflict(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.CONFLICT, statusText, headers, body, charset);
		}

		private Conflict(String message, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(message, HttpStatus.CONFLICT, statusText, headers, body, charset);
		}
	}

	/**
	 * 表示状态为HTTP 410 Gone的{@link HttpClientErrorException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class Gone extends HttpClientErrorException {

		private Gone(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.GONE, statusText, headers, body, charset);
		}

		private Gone(String message, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(message, HttpStatus.GONE, statusText, headers, body, charset);
		}
	}

	/**
	 * 表示状态为HTTP 415 Unsupported Media Type的 {@link HttpClientErrorException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class UnsupportedMediaType extends HttpClientErrorException {

		private UnsupportedMediaType(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusText, headers, body, charset);
		}

		private UnsupportedMediaType(String message, String statusText,
									 HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusText, headers, body, charset);
		}
	}

	/**
	 * 表示状态为HTTP 422 Unprocessable Entity的{@link HttpClientErrorException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class UnprocessableEntity extends HttpClientErrorException {

		private UnprocessableEntity(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.UNPROCESSABLE_ENTITY, statusText, headers, body, charset);
		}

		private UnprocessableEntity(String message, String statusText,
									HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.UNPROCESSABLE_ENTITY, statusText, headers, body, charset);
		}
	}

	/**
	 * 表示状态为HTTP 429 Too Many Requests的{@link HttpClientErrorException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class TooManyRequests extends HttpClientErrorException {

		private TooManyRequests(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.TOO_MANY_REQUESTS, statusText, headers, body, charset);
		}

		private TooManyRequests(String message, String statusText,
								HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.TOO_MANY_REQUESTS, statusText, headers, body, charset);
		}
	}

}
