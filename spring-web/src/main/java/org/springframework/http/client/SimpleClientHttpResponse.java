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

package org.springframework.http.client;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * {@link ClientHttpResponse} 的实现，使用标准的 JDK 设施。
 * 通过 {@link SimpleBufferingClientHttpRequest#execute()} 和 {@link SimpleStreamingClientHttpRequest#execute()} 获取。
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 3.0
 */
final class SimpleClientHttpResponse extends AbstractClientHttpResponse {
	/**
	 * Http URL 连接
	 */
	private final HttpURLConnection connection;

	/**
	 * Http响应头
	 */
	@Nullable
	private HttpHeaders headers;

	/**
	 * 响应流
	 */
	@Nullable
	private InputStream responseStream;


	SimpleClientHttpResponse(HttpURLConnection connection) {
		this.connection = connection;
	}


	@Override
	public int getRawStatusCode() throws IOException {
		return this.connection.getResponseCode();
	}

	@Override
	public String getStatusText() throws IOException {
		// 获取HTTP连接对象的响应消息
		String result = this.connection.getResponseMessage();
		// 如果响应消息不为null，则返回响应消息；
		// 否则返回空字符串
		return (result != null) ? result : "";
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			// 如果当前实例的响应头属性为null，则进行初始化
			this.headers = new HttpHeaders();
			// 获取第一个头字段的名称，大多数情况下是状态行，但在Google App Engine (GAE)上不是
			String name = this.connection.getHeaderFieldKey(0);
			// 如果名称不为空
			if (StringUtils.hasLength(name)) {
				// 将第一个头字段添加到响应头对象中
				this.headers.add(name, this.connection.getHeaderField(0));
			}
			// 初始化索引
			int i = 1;
			// 无限循环，用于获取所有头字段的信息
			while (true) {
				// 获取第i个头字段的名称
				name = this.connection.getHeaderFieldKey(i);
				// 如果名称为空，表示已经获取完所有头字段信息，退出循环
				if (!StringUtils.hasLength(name)) {
					break;
				}
				// 将第i个头字段添加到响应头对象中
				this.headers.add(name, this.connection.getHeaderField(i));
				// 增加索引，准备获取下一个头字段
				i++;
			}
		}
		// 返回获取到的头信息
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		// 获取HTTP连接的错误流
		InputStream errorStream = this.connection.getErrorStream();

		// 如果错误流不为null，则将其赋值给当前响应流
		// 否则将输入流赋值给当前响应流
		this.responseStream = (errorStream != null ? errorStream : this.connection.getInputStream());

		// 返回响应流对象
		return this.responseStream;
	}

	@Override
	public void close() {
		try {
			// 如果响应流为null，则调用getBody()方法获取响应体
			if (this.responseStream == null) {
				getBody();
			}

			// 使用StreamUtils.drain方法消耗响应流的内容
			StreamUtils.drain(this.responseStream);

			// 关闭响应流
			this.responseStream.close();
		} catch (Exception ex) {
			// 捕获并忽略任何异常
			// 可能的异常类型包括IO异常和其他未知异常
			// 忽略这些异常可以简化异常处理逻辑
		}
	}

}
