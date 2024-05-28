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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 适合响应状态 406（不可接受）的错误的异常。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class NotAcceptableStatusException extends ResponseStatusException {
	/**
	 * 支持的媒体类型列表
	 */
	private final List<MediaType> supportedMediaTypes;


	/**
	 * 当请求的 Content-Type 无效时的构造函数。
	 */
	public NotAcceptableStatusException(String reason) {
		super(HttpStatus.NOT_ACCEPTABLE, reason);
		this.supportedMediaTypes = Collections.emptyList();
	}

	/**
	 * 当请求的 Content-Type 不受支持时的构造函数。
	 */
	public NotAcceptableStatusException(List<MediaType> supportedMediaTypes) {
		super(HttpStatus.NOT_ACCEPTABLE, "Could not find acceptable representation");
		this.supportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes);
	}


	/**
	 * 返回带有 "Accept" 头的映射。
	 *
	 * @since 5.1.11
	 */
	@SuppressWarnings("deprecation")
	@Override
	public Map<String, String> getHeaders() {
		return getResponseHeaders().toSingleValueMap();
	}

	/**
	 * 返回带有 "Accept" 头的 HttpHeaders 实例，如果为空则返回空实例。
	 *
	 * @since 5.1.13
	 */
	@Override
	public HttpHeaders getResponseHeaders() {
		// 如果支持的媒体类型集合为空
		if (CollectionUtils.isEmpty(this.supportedMediaTypes)) {
			// 返回空的 HttpHeaders
			return HttpHeaders.EMPTY;
		}
		// 创建新的 HttpHeaders
		HttpHeaders headers = new HttpHeaders();
		// 设置 Accept 头部字段为支持的媒体类型集合
		headers.setAccept(this.supportedMediaTypes);
		// 返回 HttpHeaders
		return headers;
	}

	/**
	 * 在 Accept 头被解析但不受支持时返回受支持的内容类型列表，否则返回空列表。
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return this.supportedMediaTypes;
	}

}
