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

package org.springframework.http.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 使用标准 JDK 设施执行缓冲请求的 {@link ClientHttpRequest} 实现。
 * 通过 {@link SimpleClientHttpRequestFactory} 创建。
 *
 * <p>该类通过包装 HttpURLConnection 提供 HTTP 请求的具体实现，并允许对请求进行缓冲，
 * 以便在发送前可以修改或查看请求内容。提供了对输出流的支持，并且可以根据输出的需要
 * 调整缓冲的大小。
 *
 * <p>此实现提供了一种便捷的方式来处理 HTTP 请求和响应，特别是在需要缓冲请求内容的情况下。
 * 它支持在请求发送前设置 HTTP 头信息和请求体内容。
 * <p>
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see SimpleClientHttpRequestFactory#createRequest(java.net.URI, HttpMethod)
 * @since 3.0
 */
final class SimpleBufferingClientHttpRequest extends AbstractBufferingClientHttpRequest {

	/**
	 * 用于执行 HTTP 请求的 HttpURLConnection 对象。
	 */
	private final HttpURLConnection connection;

	/**
	 * 是否以流模式输出请求体。
	 */
	private final boolean outputStreaming;

	/**
	 * 构造一个 {@code SimpleBufferingClientHttpRequest} 实例。
	 *
	 * @param connection      用于执行请求的 HttpURLConnection 对象
	 * @param outputStreaming 是否以流模式输出请求体
	 */
	SimpleBufferingClientHttpRequest(HttpURLConnection connection, boolean outputStreaming) {
		this.connection = connection;
		this.outputStreaming = outputStreaming;
	}

	/**
	 * 获取 HTTP 请求的方法值。
	 *
	 * @return HTTP 请求的方法值
	 */
	@Override
	public String getMethodValue() {
		return this.connection.getRequestMethod();
	}

	/**
	 * 获取 HTTP 请求的 URI。
	 *
	 * @return HTTP 请求的 URI
	 * @throws IllegalStateException 如果无法获取 URI
	 */
	@Override
	public URI getURI() {
		try {
			// 尝试获取连接的URL并转换为URI对象
			return this.connection.getURL().toURI();
		} catch (URISyntaxException ex) {
			// 如果发生URI语法异常，抛出非法状态异常，并附带异常信息
			throw new IllegalStateException("Could not get HttpURLConnection URI: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 执行 HTTP 请求的内部实现，将给定的头部和内容写入到请求中，并处理响应。
	 *
	 * @param headers        HTTP 请求头
	 * @param bufferedOutput 请求体的字节数组内容
	 * @return 执行请求后的响应对象
	 * @throws IOException 如果发生 I/O 错误
	 */
	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		// 添加请求头部信息
		addHeaders(this.connection, headers);

		// JDK 1.8以下版本不支持HTTP DELETE请求的getOutputStream方法
		if (getMethod() == HttpMethod.DELETE && bufferedOutput.length == 0) {
			// 如果是DELETE方法且没有输出内容，设置不输出
			this.connection.setDoOutput(false);
		}

		// 如果连接设置为输出且启用了流式输出
		if (this.connection.getDoOutput() && this.outputStreaming) {
			// 设置固定长度的流模式
			this.connection.setFixedLengthStreamingMode(bufferedOutput.length);
		}

		// 连接服务器
		this.connection.connect();

		// 如果连接设置为输出
		if (this.connection.getDoOutput()) {
			// 将缓冲输出内容复制到连接的输出流中
			FileCopyUtils.copy(bufferedOutput, this.connection.getOutputStream());
		} else {
			// 在无输出情况下立即触发请求，通过获取响应码
			this.connection.getResponseCode();
		}

		// 返回一个简单的客户端HTTP响应对象
		return new SimpleClientHttpResponse(this.connection);
	}

	/**
	 * 将给定的 HTTP 头部添加到给定的 HTTP 连接中。
	 *
	 * @param connection HTTP 连接
	 * @param headers    要添加的头部
	 */
	static void addHeaders(HttpURLConnection connection, HttpHeaders headers) {
		// 获取连接的请求方法
		String method = connection.getRequestMethod();

		// 如果请求方法是PUT或DELETE
		if (method.equals("PUT") || method.equals("DELETE")) {
			// 如果请求头中没有Accept字段，或其值为空
			if (!StringUtils.hasText(headers.getFirst(HttpHeaders.ACCEPT))) {
				// 设置Accept头部为"*/*"以避免从HttpUrlConnection中返回默认的"Accept"头部值
				// 这样可以防止返回JSON错误响应详情时出错
				headers.set(HttpHeaders.ACCEPT, "*/*");
			}
		}

		// 对请求头部信息进行迭代处理
		headers.forEach((headerName, headerValues) -> {
			// 如果是"Cookie"头部，按照RFC 6265规范处理
			if (HttpHeaders.COOKIE.equalsIgnoreCase(headerName)) {
				// 将多个Cookie值拼接成一个字符串，使用分号和空格分隔
				String headerValue = StringUtils.collectionToDelimitedString(headerValues, "; ");
				// 将处理后的Cookie头部值设置到连接的请求属性中
				connection.setRequestProperty(headerName, headerValue);
			} else {
				// 对于其他头部信息
				for (String headerValue : headerValues) {
					// 如果头部值不为空，使用实际值，否则使用空字符串
					String actualHeaderValue = headerValue != null ? headerValue : "";
					// 将每个头部值添加到连接的请求属性中
					connection.addRequestProperty(headerName, actualHeaderValue);
				}
			}
		});
	}

}
