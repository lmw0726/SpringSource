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

package org.springframework.http;

import org.springframework.lang.Nullable;

import java.net.URI;

/**
 * 表示一个HTTP请求消息，包括{@linkplain #getMethod() 方法}和{@linkplain #getURI() URI}。
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public interface HttpRequest extends HttpMessage {

	/**
	 * 返回请求的HTTP方法。
	 *
	 * @return HTTP方法作为HttpMethod枚举值，如果无法解析则返回{@code null}（例如非标准的HTTP方法）
	 * @see #getMethodValue()
	 * @see HttpMethod#resolve(String)
	 */
	@Nullable
	default HttpMethod getMethod() {
		return HttpMethod.resolve(getMethodValue());
	}

	/**
	 * 返回请求的HTTP方法作为字符串值。
	 *
	 * @return HTTP方法作为普通字符串
	 * @see #getMethod()
	 * @since 5.0
	 */
	String getMethodValue();

	/**
	 * 返回请求的URI（包括查询字符串，如果对URI表示形式有效）。
	 *
	 * @return 请求的URI（永远不为{@code null}）
	 */
	URI getURI();

}
