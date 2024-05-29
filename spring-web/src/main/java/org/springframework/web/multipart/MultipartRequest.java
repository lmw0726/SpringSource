/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.multipart;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 此接口定义了对实际多部分请求暴露的多部分请求访问操作。它由 {@link MultipartHttpServletRequest} 扩展。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 2.5.2
 */
public interface MultipartRequest {

	/**
	 * 返回包含此请求中的多部分文件的参数名称的 {@link java.util.Iterator}。
	 * 这些是表单的字段名称（与普通参数一样），而不是原始文件名。
	 *
	 * @return 文件的名称
	 */
	Iterator<String> getFileNames();

	/**
	 * 返回此请求中上传文件的内容加描述，如果不存在则返回 {@code null}。
	 *
	 * @param name 指定多部分文件的参数名称的字符串
	 * @return 以 {@link MultipartFile} 对象形式的上传内容
	 */
	@Nullable
	MultipartFile getFile(String name);

	/**
	 * 返回此请求中上传文件的内容加描述，如果不存在则返回空列表。
	 *
	 * @param name 指定多部分文件的参数名称的字符串
	 * @return 以 {@link MultipartFile} 列表形式的上传内容
	 * @since 3.0
	 */
	List<MultipartFile> getFiles(String name);

	/**
	 * 返回此请求中包含的多部分文件的内容的 {@link java.util.Map}。
	 *
	 * @return 包含参数名称作为键，{@link MultipartFile} 对象作为值的映射
	 */
	Map<String, MultipartFile> getFileMap();

	/**
	 * 返回此请求中包含的多部分文件的内容的 {@link MultiValueMap}。
	 *
	 * @return 包含参数名称作为键，{@link MultipartFile} 对象列表作为值的映射
	 * @since 3.0
	 */
	MultiValueMap<String, MultipartFile> getMultiFileMap();

	/**
	 * 确定指定请求部分的内容类型。
	 *
	 * @param paramOrFileName 部分的名称
	 * @return 关联的内容类型，如果未定义则返回 {@code null}
	 * @since 3.1
	 */
	@Nullable
	String getMultipartContentType(String paramOrFileName);

}
