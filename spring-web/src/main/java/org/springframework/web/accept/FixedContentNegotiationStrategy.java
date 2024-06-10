/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.accept;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Collections;
import java.util.List;

/**
 * 一个返回固定内容类型的 {@code ContentNegotiationStrategy}。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class FixedContentNegotiationStrategy implements ContentNegotiationStrategy {
	/**
	 * 内容类型列表
	 */
	private final List<MediaType> contentTypes;


	/**
	 * 使用单个默认 {@code MediaType} 的构造函数。
	 */
	public FixedContentNegotiationStrategy(MediaType contentType) {
		this(Collections.singletonList(contentType));
	}

	/**
	 * 使用有序的 {@code MediaType} 列表的构造函数，用于返回一系列默认 {@code MediaType}，
	 * 以便在支持多种内容类型的应用程序中使用。
	 * <p>如果存在不支持任何其他默认媒体类型的目标，请考虑在末尾附加 {@link MediaType#ALL}。
	 *
	 * @since 5.0
	 */
	public FixedContentNegotiationStrategy(List<MediaType> contentTypes) {
		Assert.notNull(contentTypes, "'contentTypes' must not be null");
		this.contentTypes = Collections.unmodifiableList(contentTypes);
	}


	/**
	 * 返回配置的媒体类型列表。
	 */
	public List<MediaType> getContentTypes() {
		return this.contentTypes;
	}


	@Override
	public List<MediaType> resolveMediaTypes(NativeWebRequest request) {
		return this.contentTypes;
	}

}
