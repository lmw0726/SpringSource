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

package org.springframework.web.client;

import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Spring 的 {@link ResponseErrorHandler} 接口的默认实现。
 *
 * <p>此错误处理程序检查 {@link ClientHttpResponse} 上的状态码。任何 4xx 或 5xx 系列中的代码都被认为是错误。
 * 可以通过覆盖 {@link #hasError(HttpStatus)} 来更改此行为。
 * {@link #hasError(ClientHttpResponse)} 会忽略未知的状态码。
 *
 * <p>有关特定异常类型的详细信息，请参阅 {@link #handleError(ClientHttpResponse)}。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see RestTemplate#setErrorHandler
 * @since 3.0
 */
public class DefaultResponseErrorHandler implements ResponseErrorHandler {

	/**
	 * 委托给 {@link #hasError(HttpStatus)}（对于标准状态枚举值）或
	 * {@link #hasError(int)}（对于未知的状态码）处理响应的状态码。
	 *
	 * @see ClientHttpResponse#getRawStatusCode()
	 * @see #hasError(HttpStatus)
	 * @see #hasError(int)
	 */
	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		// 获取响应的原始状态码
		int rawStatusCode = response.getRawStatusCode();

		// 将原始状态码解析为HttpStatus对象
		HttpStatus statusCode = HttpStatus.resolve(rawStatusCode);

		// 根据是否成功解析为HttpStatus对象，调用不同参数类型的HasError方法判断
		return (statusCode != null ? hasError(statusCode) : hasError(rawStatusCode));
	}

	/**
	 * {@link #hasError(ClientHttpResponse)} 调用的模板方法。
	 * <p>默认实现检查 {@link HttpStatus#isError()}。
	 * 可以在子类中覆盖。
	 *
	 * @param statusCode 枚举值的 HTTP 状态码
	 * @return 如果响应表示错误，则为 {@code true}；否则为 {@code false}
	 * @see HttpStatus#isError()
	 */
	protected boolean hasError(HttpStatus statusCode) {
		return statusCode.isError();
	}

	/**
	 * {@link #hasError(ClientHttpResponse)} 调用的模板方法。
	 * <p>默认实现检查给定的状态码是否是
	 * {@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR CLIENT_ERROR} 或
	 * {@link org.springframework.http.HttpStatus.Series#SERVER_ERROR SERVER_ERROR}。
	 * 可以在子类中覆盖。
	 *
	 * @param unknownStatusCode 原始值的 HTTP 状态码
	 * @return 如果响应表示错误，则为 {@code true}；否则为 {@code false}
	 * @see org.springframework.http.HttpStatus.Series#CLIENT_ERROR
	 * @see org.springframework.http.HttpStatus.Series#SERVER_ERROR
	 * @since 4.3.21
	 */
	protected boolean hasError(int unknownStatusCode) {
		// 将未知的状态码解析为HttpStatus.Series对象
		HttpStatus.Series series = HttpStatus.Series.resolve(unknownStatusCode);

		// 检查该系列是否是客户端错误或服务器错误
		return (series == HttpStatus.Series.CLIENT_ERROR || series == HttpStatus.Series.SERVER_ERROR);
	}

	/**
	 * 使用给定的解析状态码处理给定响应中的错误。
	 * <p>默认实现会抛出：
	 * <ul>
	 * <li>如果状态码在 4xx 系列中，则抛出 {@link HttpClientErrorException}，
	 * 或其子类（例如 {@link HttpClientErrorException.BadRequest} 等）。
	 * <li>如果状态码在 5xx 系列中，则抛出 {@link HttpServerErrorException}，
	 * 或其子类（例如 {@link HttpServerErrorException.InternalServerError} 等）。
	 * <li>对于不在 {@link HttpStatus} 枚举范围内的错误状态码，抛出 {@link UnknownHttpStatusCodeException}。
	 * </ul>
	 *
	 * @throws UnknownHttpStatusCodeException 如果无法解析的状态码
	 * @see #handleError(ClientHttpResponse, HttpStatus)
	 */
	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		// 解析响应的原始状态码为 HttpStatus 对象
		HttpStatus statusCode = HttpStatus.resolve(response.getRawStatusCode());
		// 如果状态码为 null
		if (statusCode == null) {
			// 获取响应体、错误消息和字符集
			byte[] body = getResponseBody(response);
			String message = getErrorMessage(response.getRawStatusCode(),
					response.getStatusText(), body, getCharset(response));
			// 抛出未知的 HTTP 状态码异常
			throw new UnknownHttpStatusCodeException(message,
					response.getRawStatusCode(), response.getStatusText(),
					response.getHeaders(), body, getCharset(response));
		}
		// 处理错误信息
		handleError(response, statusCode);
	}

	/**
	 * 返回包含来自响应主体的详细信息的错误消息。例如：
	 * <pre>
	 * 404 Not Found: [{'id': 123, 'message': 'my message'}]
	 * </pre>
	 */
	private String getErrorMessage(
			int rawStatusCode, String statusText, @Nullable byte[] responseBody, @Nullable Charset charset) {

		// 创建一个字符串，包括状态码、状态文本和前缀
		String preface = rawStatusCode + " " + statusText + ": ";

		// 如果响应体为空
		if (ObjectUtils.isEmpty(responseBody)) {
			// 返回包含提示信息的前缀
			return preface + "[no body]";
		}

		// 如果字符集不为空，则使用指定的字符集，否则使用UTF-8字符集
		charset = (charset != null ? charset : StandardCharsets.UTF_8);

		// 使用指定的字符集将响应体转换为字符串
		String bodyText = new String(responseBody, charset);
		// 格式化响应体字符串，将其转换为可读格式
		bodyText = LogFormatUtils.formatValue(bodyText, -1, true);

		// 返回包含前缀和格式化后的响应体字符串
		return preface + bodyText;
	}

	/**
	 * 根据解析的状态码处理错误。	 *
	 * 默认实现将 4xx 范围内的错误委托给 HttpClientErrorException.create，
	 * 将 5xx 范围内的错误委托给 HttpServerErrorException.create，
	 * 否则抛出 UnknownHttpStatusCodeException。	 *
	 *
	 * @see HttpClientErrorException#create
	 * @see HttpServerErrorException#create
	 * @since 5.0
	 */
	protected void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
		// 获取响应状态文本、响应头、响应体和字符集
		String statusText = response.getStatusText();
		HttpHeaders headers = response.getHeaders();
		byte[] body = getResponseBody(response);
		Charset charset = getCharset(response);
		// 获取错误消息
		String message = getErrorMessage(statusCode.value(), statusText, body, charset);

		// 根据状态码系列抛出相应的异常
		switch (statusCode.series()) {
			case CLIENT_ERROR:
				// 如果是客户端错误，抛出客户端错误异常
				throw HttpClientErrorException.create(message, statusCode, statusText, headers, body, charset);
			case SERVER_ERROR:
				// 如果是服务器错误，抛出服务器错误异常
				throw HttpServerErrorException.create(message, statusCode, statusText, headers, body, charset);
			default:
				// 抛出未知状态码异常
				throw new UnknownHttpStatusCodeException(message, statusCode.value(), statusText, headers, body, charset);
		}
	}

	/**
	 * 确定给定响应的 HTTP 状态。
	 *
	 * @param response 要检查的响应
	 * @return 关联的 HTTP 状态
	 * @throws IOException                    在 I/O 错误时
	 * @throws UnknownHttpStatusCodeException 在无法用 HttpStatus 枚举表示的未知状态码时
	 * @since 4.3.8
	 * @deprecated 自 5.0 起，使用 {@link #handleError(ClientHttpResponse, HttpStatus)} 代替
	 */
	@Deprecated
	protected HttpStatus getHttpStatusCode(ClientHttpResponse response) throws IOException {
		// 解析原始状态码并返回对应的HttpStatus
		HttpStatus statusCode = HttpStatus.resolve(response.getRawStatusCode());
		if (statusCode == null) {
			// 如果无法解析状态码，则抛出未知的HTTP状态码异常
			throw new UnknownHttpStatusCodeException(response.getRawStatusCode(), response.getStatusText(),
					response.getHeaders(), getResponseBody(response), getCharset(response));
		}
		return statusCode;
	}

	/**
	 * 读取给定响应的主体（用于包含在状态异常中）。
	 *
	 * @param response 要检查的响应
	 * @return 响应主体作为字节数组，
	 * 如果无法读取主体，则返回空字节数组
	 * @since 4.3.8
	 */
	protected byte[] getResponseBody(ClientHttpResponse response) {
		try {
			// 返回从响应体复制的字节数组
			return FileCopyUtils.copyToByteArray(response.getBody());
		} catch (IOException ex) {
			// 忽略
		}
		return new byte[0];
	}

	/**
	 * 确定响应的字符集（用于包含在状态异常中）。
	 *
	 * @param response 要检查的响应
	 * @return 关联的字符集，如果没有则为 null
	 * @since 4.3.8
	 */
	@Nullable
	protected Charset getCharset(ClientHttpResponse response) {
		// 获取HTTP响应的头信息
		HttpHeaders headers = response.getHeaders();

		// 获取内容类型
		MediaType contentType = headers.getContentType();

		// 如果内容类型存在，则返回对应的字符集；否则返回null。
		return (contentType != null ? contentType.getCharset() : null);
	}

}
