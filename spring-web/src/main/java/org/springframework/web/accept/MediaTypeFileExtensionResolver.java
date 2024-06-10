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

import java.util.List;

/**
 * 用于将 {@link MediaType} 解析为文件扩展名列表的策略，例如，将 "application/json" 解析为 "json"。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface MediaTypeFileExtensionResolver {

	/**
	 * 将给定的媒体类型解析为文件扩展名列表。
	 *
	 * @param mediaType 要解析的媒体类型
	 * @return 扩展名列表或空列表（永远不会是 {@code null}）
	 */
	List<String> resolveFileExtensions(MediaType mediaType);

	/**
	 * 获取所有已注册的文件扩展名。
	 *
	 * @return 扩展名列表或空列表（永远不会是 {@code null}）
	 */
	List<String> getAllFileExtensions();

}
