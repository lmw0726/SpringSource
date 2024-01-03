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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 包含实际 HTTP 响应数据的异常。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class WebClientResponseException extends WebClientException {

	private static final long serialVersionUID = 4127543205414951611L;

	/**
	 * HTTP 响应的状态码。
	 */
	private final int statusCode;

	/**
	 * HTTP 响应的状态描述信息。
	 */
	private final String statusText;

	/**
	 * HTTP 响应的主体内容字节数组。
	 */
	private final byte[] responseBody;

	/**
	 * HTTP 响应的头部信息。
	 */
	private final HttpHeaders headers;

	/**
	 * HTTP 响应的字符集编码（如果可用）。
	 */
	@Nullable
	private final Charset responseCharset;

	/**
	 * 发起请求的 HTTP 请求对象（如果可用）。
	 */
	@Nullable
	private final HttpRequest request;


	/**
	 * 使用仅包含响应数据和默认消息的构造函数。
	 *
	 * @param statusCode HTTP响应的状态码
	 * @param statusText HTTP响应的状态描述信息
	 * @param headers    HTTP响应的头部信息
	 * @param body       HTTP响应的主体内容字节数组
	 * @param charset    HTTP响应的字符集编码
	 * @since 5.1
	 */
	public WebClientResponseException(int statusCode, String statusText,
									  @Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset) {

		this(statusCode, statusText, headers, body, charset, null);
	}

	/**
	 * 使用仅包含响应数据和默认消息的构造函数。
	 *
	 * @param status       HTTP响应的状态码
	 * @param reasonPhrase HTTP响应的原因描述
	 * @param headers      HTTP响应的头部信息
	 * @param body         HTTP响应的主体内容字节数组
	 * @param charset      HTTP响应的字符集编码
	 * @param request      发起HTTP请求的请求对象
	 * @since 5.1.4
	 */
	public WebClientResponseException(int status, String reasonPhrase,
									  @Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset,
									  @Nullable HttpRequest request) {

		this(initMessage(status, reasonPhrase, request), status, reasonPhrase, headers, body, charset, request);
	}

	private static String initMessage(int status, String reasonPhrase, @Nullable HttpRequest request) {
		return status + " " + reasonPhrase +
				(request != null ? " from " + request.getMethodValue() + " " + request.getURI() : "");
	}

	/**
	 * 使用预先准备的消息创建异常的构造函数。
	 *
	 * @param message      准备好的消息
	 * @param statusCode   HTTP响应的状态码
	 * @param statusText   HTTP响应的状态描述信息
	 * @param headers      HTTP响应的头部信息
	 * @param responseBody HTTP响应的主体内容字节数组
	 * @param charset      HTTP响应的字符集编码
	 */
	public WebClientResponseException(String message, int statusCode, String statusText,
									  @Nullable HttpHeaders headers, @Nullable byte[] responseBody, @Nullable Charset charset) {
		this(message, statusCode, statusText, headers, responseBody, charset, null);
	}

	/**
	 * 使用预先准备的消息创建异常的构造函数。
	 *
	 * @param message      准备好的消息
	 * @param statusCode   HTTP响应的状态码
	 * @param statusText   HTTP响应的状态描述信息
	 * @param headers      HTTP响应的头部信息
	 * @param responseBody HTTP响应的主体内容字节数组
	 * @param charset      HTTP响应的字符集编码
	 * @param request      发起HTTP请求的请求对象
	 * @since 5.1.4
	 */
	public WebClientResponseException(String message, int statusCode, String statusText,
									  @Nullable HttpHeaders headers, @Nullable byte[] responseBody, @Nullable Charset charset,
									  @Nullable HttpRequest request) {

		super(message);

		this.statusCode = statusCode;
		this.statusText = statusText;
		this.headers = (headers != null ? headers : HttpHeaders.EMPTY);
		this.responseBody = (responseBody != null ? responseBody : new byte[0]);
		this.responseCharset = charset;
		this.request = request;
	}


	/**
	 * 返回HTTP状态码的值。
	 *
	 * @return HTTP状态码
	 * @throws IllegalArgumentException 如果是未知的HTTP状态码
	 */
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(this.statusCode);
	}

	/**
	 * 返回原始的HTTP状态码值。
	 *
	 * @return 原始的HTTP状态码值
	 */
	public int getRawStatusCode() {
		return this.statusCode;
	}

	/**
	 * 返回HTTP状态文本。
	 *
	 * @return HTTP状态文本
	 */
	public String getStatusText() {
		return this.statusText;
	}

	/**
	 * 返回HTTP响应的头部信息。
	 *
	 * @return HTTP响应的头部信息
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * 将响应主体作为字节数组返回。
	 *
	 * @return 响应主体的字节数组
	 */
	public byte[] getResponseBodyAsByteArray() {
		return this.responseBody;
	}

	/**
	 * 将响应内容作为字符串返回，使用响应的媒体类型字符集，如果可用，否则回退为 {@literal ISO-8859-1}。
	 * 如果想要回退到不同的默认字符集，请使用 {@link #getResponseBodyAsString(Charset)}。
	 *
	 * @return 响应内容的字符串表示
	 */
	public String getResponseBodyAsString() {
		return getResponseBodyAsString(StandardCharsets.ISO_8859_1);
	}

	/**
	 * {@link #getResponseBodyAsString()} 的变种，允许指定在响应的媒体类型中未指定字符集时要回退的字符集。
	 *
	 * @param defaultCharset 如果响应的 {@literal Content-Type} 没有指定字符集，则使用该字符集。
	 * @return 响应内容的字符串表示
	 * @since 5.3.7
	 */
	public String getResponseBodyAsString(Charset defaultCharset) {
		return new String(this.responseBody,
				(this.responseCharset != null ? this.responseCharset : defaultCharset));
	}

	/**
	 * 返回对应的请求对象。
	 *
	 * @return 对应的请求对象，如果没有则返回 null
	 * @since 5.1.4
	 */
	@Nullable
	public HttpRequest getRequest() {
		return this.request;
	}

	/**
	 * 创建 {@code WebClientResponseException} 或一个特定HTTP状态码的子类。
	 *
	 * @param statusCode HTTP响应的状态码
	 * @param statusText HTTP响应的状态描述信息
	 * @param headers HTTP响应的头部信息
	 * @param body HTTP响应的主体内容字节数组
	 * @param charset HTTP响应的字符集编码（可为空）
	 * @return 创建的异常对象
	 * @since 5.1
	 */
	public static WebClientResponseException create(
			int statusCode, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

		return create(statusCode, statusText, headers, body, charset, null);
	}
	/**
	 * 创建 {@code WebClientResponseException} 或基于特定HTTP状态码的子类异常。
	 *
	 * @param statusCode HTTP响应的状态码
	 * @param statusText HTTP响应的状态描述信息
	 * @param headers HTTP响应的头部信息
	 * @param body HTTP响应的主体内容字节数组
	 * @param charset HTTP响应的字符集编码（可为空）
	 * @param request 发起HTTP请求的请求对象（可为空）
	 * @return 创建的异常对象
	 * @since 5.1.4
	 */
	public static WebClientResponseException create(
			int statusCode, String statusText, HttpHeaders headers, byte[] body,
			@Nullable Charset charset, @Nullable HttpRequest request) {

		HttpStatus httpStatus = HttpStatus.resolve(statusCode);
		if (httpStatus != null) {
			switch (httpStatus) {
				case BAD_REQUEST:
					return new WebClientResponseException.BadRequest(statusText, headers, body, charset, request);
				case UNAUTHORIZED:
					return new WebClientResponseException.Unauthorized(statusText, headers, body, charset, request);
				case FORBIDDEN:
					return new WebClientResponseException.Forbidden(statusText, headers, body, charset, request);
				case NOT_FOUND:
					return new WebClientResponseException.NotFound(statusText, headers, body, charset, request);
				case METHOD_NOT_ALLOWED:
					return new WebClientResponseException.MethodNotAllowed(statusText, headers, body, charset, request);
				case NOT_ACCEPTABLE:
					return new WebClientResponseException.NotAcceptable(statusText, headers, body, charset, request);
				case CONFLICT:
					return new WebClientResponseException.Conflict(statusText, headers, body, charset, request);
				case GONE:
					return new WebClientResponseException.Gone(statusText, headers, body, charset, request);
				case UNSUPPORTED_MEDIA_TYPE:
					return new WebClientResponseException.UnsupportedMediaType(statusText, headers, body, charset, request);
				case TOO_MANY_REQUESTS:
					return new WebClientResponseException.TooManyRequests(statusText, headers, body, charset, request);
				case UNPROCESSABLE_ENTITY:
					return new WebClientResponseException.UnprocessableEntity(statusText, headers, body, charset, request);
				case INTERNAL_SERVER_ERROR:
					return new WebClientResponseException.InternalServerError(statusText, headers, body, charset, request);
				case NOT_IMPLEMENTED:
					return new WebClientResponseException.NotImplemented(statusText, headers, body, charset, request);
				case BAD_GATEWAY:
					return new WebClientResponseException.BadGateway(statusText, headers, body, charset, request);
				case SERVICE_UNAVAILABLE:
					return new WebClientResponseException.ServiceUnavailable(statusText, headers, body, charset, request);
				case GATEWAY_TIMEOUT:
					return new WebClientResponseException.GatewayTimeout(statusText, headers, body, charset, request);
			}
		}
		// 如果状态码未知或无法映射到特定的HTTP状态码子类异常，则创建普通的WebClientResponseException
		return new WebClientResponseException(statusCode, statusText, headers, body, charset, request);
	}

	// 特定客户端HTTP状态代码的子类

	/**
	 * 表示 HTTP 400 Bad Request 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class BadRequest extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 400 Bad Request 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		BadRequest(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				   @Nullable HttpRequest request) {
			super(HttpStatus.BAD_REQUEST.value(), statusText, headers, body, charset, request);
		}

	}

	/**
	 * 表示 HTTP 401 Unauthorized 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Unauthorized extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 401 Unauthorized 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		Unauthorized(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
					 @Nullable HttpRequest request) {
			super(HttpStatus.UNAUTHORIZED.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 表示 HTTP 403 Forbidden 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Forbidden extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 403 Forbidden 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		Forbidden(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				  @Nullable HttpRequest request) {
			super(HttpStatus.FORBIDDEN.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 表示 HTTP 404 Not Found 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class NotFound extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 404 Not Found 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		NotFound(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				 @Nullable HttpRequest request) {
			super(HttpStatus.NOT_FOUND.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 表示 HTTP 405 Method Not Allowed 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class MethodNotAllowed extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 405 Method Not Allowed 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		MethodNotAllowed(String statusText, HttpHeaders headers, byte[] body,
						 @Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.METHOD_NOT_ALLOWED.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 表示 HTTP 406 Not Acceptable 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class NotAcceptable extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 406 Not Acceptable 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		NotAcceptable(String statusText, HttpHeaders headers, byte[] body,
					  @Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.NOT_ACCEPTABLE.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 表示 HTTP 409 Conflict 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Conflict extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 409 Conflict 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		Conflict(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				 @Nullable HttpRequest request) {
			super(HttpStatus.CONFLICT.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 表示 HTTP 410 Gone 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Gone extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 410 Gone 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		Gone(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
			 @Nullable HttpRequest request) {
			super(HttpStatus.GONE.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 表示 HTTP 415 Unsupported Media Type 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class UnsupportedMediaType extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 415 Unsupported Media Type 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		UnsupportedMediaType(String statusText, HttpHeaders headers, byte[] body,
							 @Nullable Charset charset, @Nullable HttpRequest request) {

			super(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), statusText, headers, body, charset,
					request);
		}
	}

	/**
	 * 表示 HTTP 422 Unprocessable Entity 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class UnprocessableEntity extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 422 Unprocessable Entity 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		UnprocessableEntity(String statusText, HttpHeaders headers, byte[] body,
							@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.UNPROCESSABLE_ENTITY.value(), statusText, headers, body, charset,
					request);
		}
	}

	/**
	 * 表示 HTTP 429 Too Many Requests 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class TooManyRequests extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 429 Too Many Requests 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		TooManyRequests(String statusText, HttpHeaders headers, byte[] body,
						@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.TOO_MANY_REQUESTS.value(), statusText, headers, body, charset,
					request);
		}
	}


	// 特定服务器端HTTP状态代码的子类

	/**
	 * 表示 HTTP 500 Internal Server Error 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class InternalServerError extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 500 Internal Server Error 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		InternalServerError(String statusText, HttpHeaders headers, byte[] body,
							@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.INTERNAL_SERVER_ERROR.value(), statusText, headers, body, charset,
					request);
		}
	}

	/**
	 * 表示 HTTP 501 Not Implemented 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class NotImplemented extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 501 Not Implemented 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		NotImplemented(String statusText, HttpHeaders headers, byte[] body,
					   @Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.NOT_IMPLEMENTED.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 表示 HTTP 502 Bad Gateway 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class BadGateway extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 502 Bad Gateway 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		BadGateway(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				   @Nullable HttpRequest request) {
			super(HttpStatus.BAD_GATEWAY.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 表示 HTTP 503 Service Unavailable 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class ServiceUnavailable extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 503 Service Unavailable 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		ServiceUnavailable(String statusText, HttpHeaders headers, byte[] body,
						   @Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.SERVICE_UNAVAILABLE.value(), statusText, headers, body, charset,
					request);
		}
	}

	/**
	 * 表示 HTTP 504 Gateway Timeout 的 {@link WebClientResponseException}。
	 *
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class GatewayTimeout extends WebClientResponseException {

		/**
		 * 使用给定的参数构造一个 HTTP 504 Gateway Timeout 的异常。
		 *
		 * @param statusText 状态描述信息
		 * @param headers    响应头
		 * @param body       响应体字节数组
		 * @param charset    字符集编码
		 * @param request    HTTP请求
		 */
		GatewayTimeout(String statusText, HttpHeaders headers, byte[] body,
					   @Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.GATEWAY_TIMEOUT.value(), statusText, headers, body, charset,
					request);
		}
	}

}
