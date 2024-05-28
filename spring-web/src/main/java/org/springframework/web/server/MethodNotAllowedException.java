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

package org.springframework.web.server;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 适合响应状态 405（方法不允许）的错误的异常。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class MethodNotAllowedException extends ResponseStatusException {
	/**
	 * 方法名称
	 */
	private final String method;

	/**
	 * Http方法集合
	 */
	private final Set<HttpMethod> httpMethods;


	public MethodNotAllowedException(HttpMethod method, Collection<HttpMethod> supportedMethods) {
		this(method.name(), supportedMethods);
	}

	public MethodNotAllowedException(String method, @Nullable Collection<HttpMethod> supportedMethods) {
		super(HttpStatus.METHOD_NOT_ALLOWED, "Request method '" + method + "' not supported");
		Assert.notNull(method, "'method' is required");
		if (supportedMethods == null) {
			supportedMethods = Collections.emptySet();
		}
		this.method = method;
		this.httpMethods = Collections.unmodifiableSet(new LinkedHashSet<>(supportedMethods));
	}


	/**
	 * 返回带有 "Allow" 头的映射。
	 *
	 * @since 5.1.11
	 */
	@SuppressWarnings("deprecation")
	@Override
	public Map<String, String> getHeaders() {
		return getResponseHeaders().toSingleValueMap();
	}

	/**
	 * 返回带有 "Allow" 头的 HttpHeaders。
	 *
	 * @since 5.1.13
	 */
	@Override
	public HttpHeaders getResponseHeaders() {
		// 如果 HTTP 方法集合为空
		if (CollectionUtils.isEmpty(this.httpMethods)) {
			// 返回空的 HttpHeaders
			return HttpHeaders.EMPTY;
		}
		// 创建新的 HttpHeaders
		HttpHeaders headers = new HttpHeaders();
		// 设置 Allow 头部字段为 HTTP 方法集合
		headers.setAllow(this.httpMethods);
		// 返回 HttpHeaders
		return headers;
	}

	/**
	 * 返回失败请求的 HTTP 方法。
	 */
	public String getHttpMethod() {
		return this.method;
	}

	/**
	 * 返回受支持的 HTTP 方法列表。
	 */
	public Set<HttpMethod> getSupportedMethods() {
		return this.httpMethods;
	}

}
