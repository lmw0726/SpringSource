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

import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Arrays;
import java.util.List;

/**
 * 一个检查 'Accept' 请求头的 {@code ContentNegotiationStrategy}。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public class HeaderContentNegotiationStrategy implements ContentNegotiationStrategy {

	/**
	 * {@inheritDoc}
	 *
	 * @throws HttpMediaTypeNotAcceptableException 如果无法解析 'Accept' 头
	 */
	@Override
	public List<MediaType> resolveMediaTypes(NativeWebRequest request)
			throws HttpMediaTypeNotAcceptableException {

		// 获取请求中Accept头的值数组
		String[] headerValueArray = request.getHeaderValues(HttpHeaders.ACCEPT);
		// 如果Accept头的值数组为空，则返回默认的媒体类型列表
		if (headerValueArray == null) {
			return MEDIA_TYPE_ALL_LIST;
		}

		// 将Accept头的值数组转换为列表
		List<String> headerValues = Arrays.asList(headerValueArray);
		try {
			// 解析Accept头的媒体类型
			List<MediaType> mediaTypes = MediaType.parseMediaTypes(headerValues);
			// 按照优先级和质量进行排序
			MediaType.sortBySpecificityAndQuality(mediaTypes);
			// 如果解析的媒体类型列表不为空，则返回该列表；
			// 否则返回默认的媒体类型列表
			return !CollectionUtils.isEmpty(mediaTypes) ? mediaTypes : MEDIA_TYPE_ALL_LIST;
		} catch (InvalidMediaTypeException ex) {
			// 如果解析过程中发生错误，则抛出HttpMediaTypeNotAcceptableException异常
			throw new HttpMediaTypeNotAcceptableException(
					"Could not parse 'Accept' header " + headerValues + ": " + ex.getMessage());
		}
	}

}
