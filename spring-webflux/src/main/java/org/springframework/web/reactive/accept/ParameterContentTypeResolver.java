/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 解析器，检查查询参数并使用它查找匹配的 MediaType。可以注册查找键，或者可以使用 {@link MediaTypeFactory} 作为回退来执行查找。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ParameterContentTypeResolver implements RequestedContentTypeResolver {

	/**
	 * 主要用于按键查找媒体类型（例如 "json" -> "application/json"）的查找
	 */
	private final Map<String, MediaType> mediaTypes = new ConcurrentHashMap<>(64);

	private String parameterName = "format";


	public ParameterContentTypeResolver(Map<String, MediaType> mediaTypes) {
		mediaTypes.forEach((key, value) -> this.mediaTypes.put(formatKey(key), value));
	}

	private static String formatKey(String key) {
		return key.toLowerCase(Locale.ENGLISH);
	}


	/**
	 * 设置用于确定请求的媒体类型的参数名称。
	 * <p>默认情况下，此值设置为 {@literal "format"}。
	 */
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "'parameterName' is required");
		this.parameterName = parameterName;
	}

	public String getParameterName() {
		return this.parameterName;
	}


	/**
	 * 解析给定的 {@code ServerWebExchange} 请求，获取请求的媒体类型列表。
	 * 如果没有指定媒体类型，则返回 {@link #MEDIA_TYPE_ALL_LIST} 列表。
	 *
	 * @param exchange 当前的交换对象
	 * @return 请求的媒体类型列表；如果没有请求，则返回 {@link #MEDIA_TYPE_ALL_LIST}。
	 * @throws NotAcceptableStatusException 如果请求的媒体类型无效
	 */
	@Override
	public List<MediaType> resolveMediaTypes(ServerWebExchange exchange) throws NotAcceptableStatusException {
		// 从请求的查询参数中获取键值
		String key = exchange.getRequest().getQueryParams().getFirst(getParameterName());

		// 如果键值不存在或为空
		if (!StringUtils.hasText(key)) {
			// 返回所有媒体类型列表
			return MEDIA_TYPE_ALL_LIST;
		}

		// 格式化键值
		key = formatKey(key);

		// 获取匹配的媒体类型
		MediaType match = this.mediaTypes.get(key);

		// 如果未找到匹配的媒体类型
		if (match == null) {
			// 根据键值创建媒体类型或抛出异常
			match = MediaTypeFactory.getMediaType("filename." + key)
					.orElseThrow(() -> {
						List<MediaType> supported = new ArrayList<>(this.mediaTypes.values());
						return new NotAcceptableStatusException(supported);
					});
		}

		// 将键值和匹配的媒体类型放入映射中（如果键不存在）
		this.mediaTypes.putIfAbsent(key, match);

		// 返回包含匹配媒体类型的单元素列表
		return Collections.singletonList(match);
	}

}
