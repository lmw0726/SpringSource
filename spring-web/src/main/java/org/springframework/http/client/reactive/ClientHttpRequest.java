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

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.util.MultiValueMap;

import java.net.URI;

/**
 * 表示客户端端的响应式 HTTP 请求。
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
public interface ClientHttpRequest extends ReactiveHttpOutputMessage {

	/**
	 * 返回请求的 HTTP 方法。
	 */
	HttpMethod getMethod();

	/**
	 * 返回请求的 URI。
	 */
	URI getURI();

	/**
	 * 返回一个可变的请求 cookie 映射，用于发送到服务器。
	 */
	MultiValueMap<String, HttpCookie> getCookies();

	/**
	 * 返回底层 HTTP 库的请求。
	 *
	 * @param <T> 要转换的请求的预期类型
	 * @since 5.3
	 */
	<T> T getNativeRequest();

}
