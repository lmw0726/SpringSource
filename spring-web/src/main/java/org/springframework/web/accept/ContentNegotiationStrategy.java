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

package org.springframework.web.accept;

import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Collections;
import java.util.List;

/**
 * 用于解析请求的媒体类型的策略。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
@FunctionalInterface
public interface ContentNegotiationStrategy {

	/**
	 * 当未请求特定媒体类型时，{@link #resolveMediaTypes} 返回包含 {@link MediaType#ALL} 的单例列表。
	 *
	 * @since 5.0.5
	 */
	List<MediaType> MEDIA_TYPE_ALL_LIST = Collections.singletonList(MediaType.ALL);


	/**
	 * 将给定请求解析为媒体类型列表。返回的列表首先按特异性排序，然后按质量参数排序。
	 *
	 * @param webRequest 当前请求
	 * @return 请求的媒体类型列表，如果未请求任何媒体类型，则返回 {@link #MEDIA_TYPE_ALL_LIST}。
	 * @throws HttpMediaTypeNotAcceptableException 如果无法解析请求的媒体类型
	 */
	List<MediaType> resolveMediaTypes(NativeWebRequest webRequest)
			throws HttpMediaTypeNotAcceptableException;

}
