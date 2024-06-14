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

import org.springframework.util.InvalidMimeTypeException;

/**
 * 在 {@link MediaType#parseMediaType(String)} 中遇到无效媒体类型规范字符串时抛出的异常。
 *
 * @author Juergen Hoeller
 * @since 3.2.2
 */
@SuppressWarnings("serial")
public class InvalidMediaTypeException extends IllegalArgumentException {

	/**
	 * 引起异常的媒体类型字符串。
	 */
	private final String mediaType;


	/**
	 * 使用给定的媒体类型和详细消息创建一个新的 InvalidMediaTypeException。
	 *
	 * @param mediaType 引起异常的媒体类型
	 * @param message   描述无效部分的详细消息
	 */
	public InvalidMediaTypeException(String mediaType, String message) {
		super("Invalid media type \"" + mediaType + "\": " + message);
		this.mediaType = mediaType;
	}

	/**
	 * 允许包装 {@link InvalidMimeTypeException} 的构造方法。
	 *
	 * @param ex {@link InvalidMimeTypeException} 异常对象
	 */
	InvalidMediaTypeException(InvalidMimeTypeException ex) {
		super(ex.getMessage(), ex);
		this.mediaType = ex.getMimeType();
	}


	/**
	 * 返回引起异常的媒体类型。
	 *
	 * @return 引起异常的媒体类型字符串
	 */
	public String getMediaType() {
		return this.mediaType;
	}

}
