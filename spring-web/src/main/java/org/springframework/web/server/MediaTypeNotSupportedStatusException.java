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

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.List;

/**
 * 在适合响应状态 415（不支持的媒体类型）的错误的情况下使用的异常。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @deprecated 改用 {@link UnsupportedMediaTypeStatusException}，此类从未由 Spring 代码抛出，并将在 6.0 版本中删除
 */
@Deprecated
@SuppressWarnings("serial")
public class MediaTypeNotSupportedStatusException extends ResponseStatusException {
	/**
	 * 支持的媒体类型列表
	 */
	private final List<MediaType> supportedMediaTypes;


	/**
	 * 当内容类型无效时的构造方法。
	 */
	public MediaTypeNotSupportedStatusException(String reason) {
		super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, reason);
		this.supportedMediaTypes = Collections.emptyList();
	}

	/**
	 * 当内容类型不受支持时的构造方法。
	 */
	public MediaTypeNotSupportedStatusException(List<MediaType> supportedMediaTypes) {
		super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", null);
		this.supportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes);
	}


	/**
	 * 在解析但不支持 Accept 标头时返回受支持的内容类型列表，否则返回空列表。
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return this.supportedMediaTypes;
	}

}
