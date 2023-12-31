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

package org.springframework.web.reactive.accept;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

import java.util.Collections;
import java.util.List;

/**
 * 始终解析为固定媒体类型列表的解析器。可以作为“最后一个”策略使用，为当客户端未请求任何媒体类型时提供后备方案。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class FixedContentTypeResolver implements RequestedContentTypeResolver {

	private final List<MediaType> contentTypes;

	/**
	 * 使用单个默认的 {@code MediaType} 构造函数。
	 */
	public FixedContentTypeResolver(MediaType mediaType) {
		this(Collections.singletonList(mediaType));
	}

	/**
	 * 使用有序的默认 {@code MediaType} 列表的构造函数，用于返回在支持各种内容类型的应用程序中使用的默认 {@code MediaType}。
	 * <p>如果存在不支持其他默认媒体类型的目标，请考虑在最后追加 {@link MediaType#ALL}。
	 */
	public FixedContentTypeResolver(List<MediaType> contentTypes) {
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
	public List<MediaType> resolveMediaTypes(ServerWebExchange exchange) {
		return this.contentTypes;
	}

}
