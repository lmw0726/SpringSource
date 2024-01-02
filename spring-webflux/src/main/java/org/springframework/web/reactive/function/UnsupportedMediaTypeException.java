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

package org.springframework.web.reactive.function;

import org.springframework.core.NestedRuntimeException;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * 表示 {@code Content-Type} 不受支持的异常。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
@SuppressWarnings("serial")
public class UnsupportedMediaTypeException extends NestedRuntimeException {

	/**
	 * 内容类型
	 */
	@Nullable
	private final MediaType contentType;

	/**
	 * 支持的媒体类型列表
	 */
	private final List<MediaType> supportedMediaTypes;

	/**
	 * body类型
	 */
	@Nullable
	private final ResolvableType bodyType;


	/**
	 * 当指定的 Content-Type 无效时使用的构造函数。
	 */
	public UnsupportedMediaTypeException(String reason) {
		super(reason);
		this.contentType = null;
		this.supportedMediaTypes = Collections.emptyList();
		this.bodyType = null;
	}

	/**
	 * 当 Content-Type 可以解析但不受支持时使用的构造函数。
	 */
	public UnsupportedMediaTypeException(@Nullable MediaType contentType, List<MediaType> supportedTypes) {
		this(contentType, supportedTypes, null);
	}

	/**
	 * 当尝试从特定 Java 类型编码或解码时使用的构造函数。
	 * @since 5.1
	 */
	public UnsupportedMediaTypeException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
										 @Nullable ResolvableType bodyType) {

		super(initReason(contentType, bodyType));
		this.contentType = contentType;
		this.supportedMediaTypes = Collections.unmodifiableList(supportedTypes);
		this.bodyType = bodyType;
	}

	private static String initReason(@Nullable MediaType contentType, @Nullable ResolvableType bodyType) {
		return "Content type '" + (contentType != null ? contentType : "") + "' not supported" +
				(bodyType != null ? " for bodyType=" + bodyType.toString() : "");
	}


	/**
	 * 如果成功解析了请求的 Content-Type 头部，则返回其值，否则返回 {@code null}。
	 */
	@Nullable
	public MediaType getContentType() {
		return this.contentType;
	}

	/**
	 * 在 Content-Type 头部已解析但不受支持的情况下返回受支持的内容类型列表，否则返回空列表。
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return this.supportedMediaTypes;
	}

	/**
	 * 返回生成此异常的上下文中的主体类型。当异常由于尝试从特定 Java 类型编码或解码而引发时适用。
	 * @return 主体类型，如果不可用则返回 {@code null}
	 * @since 5.1
	 */
	@Nullable
	public ResolvableType getBodyType() {
		return this.bodyType;
	}

}
