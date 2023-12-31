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
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

import java.util.Collections;
import java.util.List;

/**
 * 解析 {@code ServerWebExchange} 请求的媒体类型的策略。
 *
 * <p>参见 {@link RequestedContentTypeResolverBuilder} 创建一系列策略。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface RequestedContentTypeResolver {

	/**
	 * 当没有请求特定媒体类型时，从 {@link #resolveMediaTypes} 返回的单例列表，包含 {@link MediaType#ALL}。
	 *
	 * @since 5.0.5
	 */
	List<MediaType> MEDIA_TYPE_ALL_LIST = Collections.singletonList(MediaType.ALL);

	/**
	 * 解析给定请求的媒体类型列表。返回的列表首先按特定性排序，然后按质量参数排序。
	 *
	 * @param exchange 当前的交换对象
	 * @return 请求的媒体类型列表；如果没有请求，则返回 {@link #MEDIA_TYPE_ALL_LIST}。
	 * @throws NotAcceptableStatusException 如果请求的媒体类型无效
	 */
	List<MediaType> resolveMediaTypes(ServerWebExchange exchange);

}
