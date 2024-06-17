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

package org.springframework.http.client.support;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * {@link ClientHttpRequestInterceptor} 实现，用于在请求中应用给定的 HTTP Basic Authentication
 * 用户名/密码对，除非已经设置了自定义的 {@code Authorization} 头部。
 *
 * <p>此拦截器在每个请求上检查是否已存在 {@code Authorization} 头部，若不存在则添加基本认证信息。
 * 如果已经存在 {@code Authorization} 头部，则不做任何修改。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see HttpHeaders#setBasicAuth
 * @see HttpHeaders#AUTHORIZATION
 * @since 5.1.1
 */
public class BasicAuthenticationInterceptor implements ClientHttpRequestInterceptor {
	/**
	 * 加密的认证信息
	 */
	private final String encodedCredentials;


	/**
	 * 使用给定的用户名和密码创建一个新的拦截器，添加基本认证信息。
	 *
	 * @param username 使用的用户名
	 * @param password 使用的密码
	 * @see HttpHeaders#setBasicAuth(String, String)
	 * @see HttpHeaders#encodeBasicAuth(String, String, Charset)
	 */
	public BasicAuthenticationInterceptor(String username, String password) {
		this(username, password, null);
	}

	/**
	 * 使用给定的用户名、密码和字符集创建一个新的拦截器，添加基本认证信息。
	 *
	 * @param username 使用的用户名
	 * @param password 使用的密码
	 * @param charset  使用的字符集，如果为null，则使用默认字符集
	 * @see HttpHeaders#setBasicAuth(String, String, Charset)
	 * @see HttpHeaders#encodeBasicAuth(String, String, Charset)
	 */
	public BasicAuthenticationInterceptor(String username, String password, @Nullable Charset charset) {
		this.encodedCredentials = HttpHeaders.encodeBasicAuth(username, password, charset);
	}

	/**
	 * 拦截器的主要方法，用于在请求中添加基本认证信息，如果尚未设置 {@code Authorization} 头部。
	 *
	 * @param request   当前的HTTP请求对象
	 * @param body      请求体的字节数组
	 * @param execution 请求的执行器，用于继续执行请求链
	 * @return 客户端HTTP响应对象
	 * @throws IOException 如果在执行请求时发生I/O错误
	 */
	@Override
	public ClientHttpResponse intercept(
			HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

		// 从请求中获取所有的Http请求头
		HttpHeaders headers = request.getHeaders();
		// 如果 请求头 中不包含 Authorization 名称
		if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
			// 设置加密的认证信息
			headers.setBasicAuth(this.encodedCredentials);
		}
		// 执行请求并返回结果
		return execution.execute(request, body);
	}

}
