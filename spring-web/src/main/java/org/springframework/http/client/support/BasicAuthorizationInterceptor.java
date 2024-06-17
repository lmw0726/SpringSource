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

package org.springframework.http.client.support;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * {@link ClientHttpRequestInterceptor} 实现，用于添加 BASIC 认证头部信息。
 *
 * <p>注意：该类自版本5.1.1起已废弃，请使用 {@link BasicAuthenticationInterceptor} 替代，
 * 它重用 {@link org.springframework.http.HttpHeaders#setBasicAuth}，
 * 并共享其默认的字符集 ISO-8859-1，而非本类中使用的 UTF-8。
 *
 * @author Phillip Webb
 * @since 4.3.1
 * @deprecated 自5.1.1起废弃，建议使用 {@link BasicAuthenticationInterceptor}
 */
@Deprecated
public class BasicAuthorizationInterceptor implements ClientHttpRequestInterceptor {
	/**
	 * 用户名
	 */
	private final String username;

	/**
	 * 密码
	 */
	private final String password;


	/**
	 * 创建一个新的拦截器，用于为给定的用户名和密码添加 BASIC 认证头部信息。
	 *
	 * @param username 使用的用户名
	 * @param password 使用的密码
	 */
	public BasicAuthorizationInterceptor(@Nullable String username, @Nullable String password) {
		Assert.doesNotContain(username, ":", "Username must not contain a colon");
		this.username = (username != null ? username : "");
		this.password = (password != null ? password : "");
	}

	/**
	 * 拦截器的主要方法，用于在请求中添加 BASIC 认证头部信息。
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

		// 将用户名和密码加密，生成Base64编码的认证Token，
		String token = Base64Utils.encodeToString(
				(this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));
		// 添加到Authorization头部
		request.getHeaders().add("Authorization", "Basic " + token);
		// 执行剩下的客户端Http请求执行链
		return execution.execute(request, body);
	}

}
