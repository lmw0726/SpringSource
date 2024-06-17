/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.multipart.support;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于标准 Servlet {@link Part} 处理的实用方法。
 *
 * @author Juergen Hoeller
 * @since  5.3
 * @see HttpServletRequest#getParts()
 * @see StandardServletMultipartResolver
 */
public abstract class StandardServletPartUtils {

	/**
	 * 从给定的 servlet 请求中检索所有部分。
	 *
	 * @param request servlet 请求
	 * @return MultiValueMap 中的部分
	 * @throws MultipartException 如果失败
	 */
	public static MultiValueMap<String, Part> getParts(HttpServletRequest request) throws MultipartException {
		try {
			// 创建一个MultiValueMap用于存储请求的各个部分
			MultiValueMap<String, Part> parts = new LinkedMultiValueMap<>();
			// 遍历请求中的各个部分
			for (Part part : request.getParts()) {
				// 将每个部分添加到MultiValueMap中
				parts.add(part.getName(), part);
			}
			// 返回包含请求各个部分的MultiValueMap
			return parts;
		} catch (Exception ex) {
			// 如果获取请求部分时出现异常，抛出MultipartException
			throw new MultipartException("Failed to get request parts", ex);
		}
	}

	/**
	 * 从给定的 servlet 请求中检索具有给定名称的所有部分。
	 *
	 * @param request servlet 请求
	 * @param name    要查找的名称
	 * @return MultiValueMap 中的部分
	 * @throws MultipartException 如果失败
	 */
	public static List<Part> getParts(HttpServletRequest request, String name) throws MultipartException {
		try {
			// 创建一个列表用于存储指定名称的部分
			List<Part> parts = new ArrayList<>(1);
			// 遍历请求中的各个部分
			for (Part part : request.getParts()) {
				// 如果部分的名称与指定名称相匹配，则将其添加到列表中
				if (part.getName().equals(name)) {
					parts.add(part);
				}
			}
			// 返回包含指定名称部分的列表
			return parts;
		} catch (Exception ex) {
			// 如果获取请求部分时出现异常，抛出MultipartException
			throw new MultipartException("Failed to get request parts", ex);
		}
	}

	/**
	 * 绑定给定 servlet 请求中的所有部分。
	 *
	 * @param request   servlet 请求
	 * @param mpvs      要绑定的属性值
	 * @param bindEmpty 是否绑定空部分
	 * @throws MultipartException 如果失败
	 */
	public static void bindParts(HttpServletRequest request, MutablePropertyValues mpvs, boolean bindEmpty)
			throws MultipartException {

		// 获取请求中的各个部分，并对其进行处理
		getParts(request).forEach((key, values) -> {
			if (values.size() == 1) {
				// 如果部分列表只包含一个部分
				Part part = values.get(0);
				if (bindEmpty || part.getSize() > 0) {
					// 如果允许绑定空值或部分的大小大于0，则将部分添加到MultiValueMapPropertyValues中
					mpvs.add(key, part);
				}
			} else {
				// 如果部分列表包含多个部分，则将其直接添加到MultiValueMapPropertyValues中
				mpvs.add(key, values);
			}
		});
	}

}
