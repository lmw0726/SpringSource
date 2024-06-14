/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 请求方法枚举。用于与 {@link org.springframework.http.client.ClientHttpRequest}
 * 和 {@link org.springframework.web.client.RestTemplate} 一起使用。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public enum HttpMethod {

	GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;

	/**
	 * 方法名 —— 方法映射
	 */
	private static final Map<String, HttpMethod> mappings = new HashMap<>(16);

	static {
		// 遍历枚举类HTTP方法中的所有枚举值
		for (HttpMethod httpMethod : values()) {
			// 将枚举值的名称作为键，枚举值本身作为值，存入映射中
			mappings.put(httpMethod.name(), httpMethod);
		}
	}


	/**
	 * 将给定的方法值解析为 {@code HttpMethod}。
	 *
	 * @param method 方法值，作为字符串
	 * @return 对应的 {@code HttpMethod}，如果未找到则返回 {@code null}
	 * @since 4.2.4
	 */
	@Nullable
	public static HttpMethod resolve(@Nullable String method) {
		return (method != null ? mappings.get(method) : null);
	}


	/**
	 * 判断此 {@code HttpMethod} 是否与给定的方法值匹配。
	 *
	 * @param method HTTP 方法，作为字符串
	 * @return 如果匹配则返回 {@code true}，否则返回 {@code false}
	 * @since 4.2.4
	 */
	public boolean matches(String method) {
		return name().equals(method);
	}

}
