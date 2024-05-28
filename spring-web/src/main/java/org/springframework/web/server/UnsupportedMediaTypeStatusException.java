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

package org.springframework.web.server;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * 表示适用于 Spring Web 应用程序的响应状态为 415（不支持的媒体类型）的错误的异常。
 * 该异常提供了额外的字段（例如，一个可选的 {@link MethodParameter} 如果与错误相关）。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class UnsupportedMediaTypeStatusException extends ResponseStatusException {
	/**
	 * 内容类型
	 */
	@Nullable
	private final MediaType contentType;

	/**
	 * 支持的媒体类型
	 */
	private final List<MediaType> supportedMediaTypes;

	/**
	 * 可解析的主体类型
	 */
	@Nullable
	private final ResolvableType bodyType;

	/**
	 * 方法
	 */
	@Nullable
	private final HttpMethod method;


	/**
	 * 当指定的 Content-Type 无效时的构造函数。
	 */
	public UnsupportedMediaTypeStatusException(@Nullable String reason) {
		super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, reason);
		this.contentType = null;
		this.supportedMediaTypes = Collections.emptyList();
		this.bodyType = null;
		this.method = null;
	}

	/**
	 * 当 Content-Type 可以解析但不受支持时的构造函数。
	 */
	public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes) {
		this(contentType, supportedTypes, null, null);
	}

	/**
	 * 尝试编码或解码到特定 Java 类型时的构造函数。
	 *
	 * @since 5.1
	 */
	public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
											   @Nullable ResolvableType bodyType) {
		this(contentType, supportedTypes, bodyType, null);
	}

	/**
	 * 提供 HTTP 方法的构造函数。
	 *
	 * @since 5.3.6
	 */
	public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
											   @Nullable HttpMethod method) {
		this(contentType, supportedTypes, null, method);
	}

	/**
	 * 尝试编码或解码到特定 Java 类型时的构造函数。
	 *
	 * @since 5.3.6
	 */
	public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
											   @Nullable ResolvableType bodyType, @Nullable HttpMethod method) {

		super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, initReason(contentType, bodyType));
		this.contentType = contentType;
		this.supportedMediaTypes = Collections.unmodifiableList(supportedTypes);
		this.bodyType = bodyType;
		this.method = method;
	}

	private static String initReason(@Nullable MediaType contentType, @Nullable ResolvableType bodyType) {
		return "Content type '" + (contentType != null ? contentType : "") + "' not supported" +
				(bodyType != null ? " for bodyType=" + bodyType.toString() : "");
	}


	/**
	 * 如果成功解析了请求的 Content-Type 头，则返回该头，否则返回 {@code null}。
	 */
	@Nullable
	public MediaType getContentType() {
		return this.contentType;
	}

	/**
	 * 在 Content-Type 标头被解析但不受支持的情况下返回支持的内容类型列表，否则返回空列表。
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return this.supportedMediaTypes;
	}

	/**
	 * 返回生成此异常的上下文中的 body 类型。
	 * <p>当尝试从特定 Java 类型编码或解码时，这是适用的。
	 *
	 * @return body 类型，如果不可用则返回 {@code null}
	 * @since 5.1
	 */
	@Nullable
	public ResolvableType getBodyType() {
		return this.bodyType;
	}

	@Override
	public HttpHeaders getResponseHeaders() {
		if (HttpMethod.PATCH != this.method || CollectionUtils.isEmpty(this.supportedMediaTypes)) {
			// 如果请求方法不是 PATCH 或者支持的媒体类型列表为空，则返回空的 HttpHeaders
			return HttpHeaders.EMPTY;
		}
		// 创建新的 HttpHeaders 对象
		HttpHeaders headers = new HttpHeaders();
		// 设置 Accept-Patch 头字段为支持的媒体类型列表
		headers.setAcceptPatch(this.supportedMediaTypes);
		return headers;
	}

}
