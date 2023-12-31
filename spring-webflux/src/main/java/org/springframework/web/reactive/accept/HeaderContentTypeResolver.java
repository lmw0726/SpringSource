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

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

/**
 * 查看请求的 'Accept' 标头的解析器。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HeaderContentTypeResolver implements RequestedContentTypeResolver {

	/**
	 * 解析请求的 'Accept' 标头的解析器。
	 *
	 * @param exchange 当前的交换对象
	 * @return 请求的媒体类型列表，按特定性和质量参数排序；如果没有指定媒体类型，则返回 {@link #MEDIA_TYPE_ALL_LIST}。
	 * @throws NotAcceptableStatusException 如果 'Accept' 标头的媒体类型无效
	 */
	@Override
	public List<MediaType> resolveMediaTypes(ServerWebExchange exchange) throws NotAcceptableStatusException {
		try {
			// 获取请求中的媒体类型列表
			List<MediaType> mediaTypes = exchange.getRequest().getHeaders().getAccept();

			// 根据特定性和质量对媒体类型进行排序
			MediaType.sortBySpecificityAndQuality(mediaTypes);

			// 如果媒体类型列表不为空，返回该列表；否则返回所有媒体类型列表
			return (!CollectionUtils.isEmpty(mediaTypes) ? mediaTypes : MEDIA_TYPE_ALL_LIST);
		} catch (InvalidMediaTypeException ex) {
			// 捕获无效媒体类型异常
			// 获取请求中的 'Accept' 头的值
			String value = exchange.getRequest().getHeaders().getFirst("Accept");

			// 抛出不可接受状态异常，提示无法解析 'Accept' 头
			throw new NotAcceptableStatusException(
					"Could not parse 'Accept' header [" + value + "]: " + ex.getMessage());
		}
	}


}
