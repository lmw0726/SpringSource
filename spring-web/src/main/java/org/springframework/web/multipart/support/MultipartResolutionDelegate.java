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

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@code HandlerMethodArgumentResolver} 实现的通用委托，需要解析 {@link MultipartFile} 和 {@link Part} 参数。
 *
 * @author Juergen Hoeller
 * @since 4.3
 */
public final class MultipartResolutionDelegate {

	/**
	 * 表示一个不可解决的值。
	 */
	public static final Object UNRESOLVABLE = new Object();


	private MultipartResolutionDelegate() {
	}


	@Nullable
	public static MultipartRequest resolveMultipartRequest(NativeWebRequest webRequest) {
		// 获取MultipartRequest对象，如果存在的话
		MultipartRequest multipartRequest = webRequest.getNativeRequest(MultipartRequest.class);
		if (multipartRequest != null) {
			// 如果multipartRequest不为空，则直接返回
			return multipartRequest;
		}

		// 获取HttpServletRequest对象
		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		if (servletRequest != null && isMultipartContent(servletRequest)) {
			// 如果servletRequest不为空，并且是multipart类型的请求，则创建并返回StandardMultipartHttpServletRequest对象
			return new StandardMultipartHttpServletRequest(servletRequest);
		}

		// 如果以上条件都不满足，则返回null
		return null;
	}

	public static boolean isMultipartRequest(HttpServletRequest request) {
		return (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null ||
				isMultipartContent(request));
	}

	private static boolean isMultipartContent(HttpServletRequest request) {
		// 获取请求的Content-Type
		String contentType = request.getContentType();
		// 检查Content-Type是否不为空且以"multipart/"开头
		return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
	}

	static MultipartHttpServletRequest asMultipartHttpServletRequest(HttpServletRequest request) {
		// 获取MultipartHttpServletRequest对象，如果存在的话
		MultipartHttpServletRequest unwrapped = WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
		if (unwrapped != null) {
			// 如果unwrapped不为空，则直接返回
			return unwrapped;
		}

		// 如果unwrapped为空，则创建并返回StandardMultipartHttpServletRequest对象
		return new StandardMultipartHttpServletRequest(request);
	}


	public static boolean isMultipartArgument(MethodParameter parameter) {
		// 获取参数的嵌套类型
		Class<?> paramType = parameter.getNestedParameterType();
		// 检查参数类型是否为MultipartFile类，或者参数是MultipartFile集合或数组，
		// 或者参数类型为Part类，或者参数是Part集合或数组
		return (MultipartFile.class == paramType ||
				isMultipartFileCollection(parameter) || isMultipartFileArray(parameter) ||
				(Part.class == paramType || isPartCollection(parameter) || isPartArray(parameter)));
	}

	@Nullable
	public static Object resolveMultipartArgument(String name, MethodParameter parameter, HttpServletRequest request)
			throws Exception {

		// 获取MultipartHttpServletRequest对象，如果存在的话
		MultipartHttpServletRequest multipartRequest =
				WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
		// 检查请求是否为multipart类型
		boolean isMultipart = (multipartRequest != null || isMultipartContent(request));

		if (MultipartFile.class == parameter.getNestedParameterType()) {
			// 如果参数类型为MultipartFile
			if (!isMultipart) {
				// 如果请求不是multipart类型，则返回null
				return null;
			}
			if (multipartRequest == null) {
				// 如果multipartRequest为空，则创建StandardMultipartHttpServletRequest对象
				multipartRequest = new StandardMultipartHttpServletRequest(request);
			}
			// 返回指定名称的文件
			return multipartRequest.getFile(name);
		} else if (isMultipartFileCollection(parameter)) {
			// 如果参数是MultipartFile集合
			if (!isMultipart) {
				// 如果请求不是multipart类型，则返回null
				return null;
			}
			if (multipartRequest == null) {
				// 如果multipartRequest为空，则创建StandardMultipartHttpServletRequest对象
				multipartRequest = new StandardMultipartHttpServletRequest(request);
			}
			// 返回指定名称的文件集合，如果为空则返回null
			List<MultipartFile> files = multipartRequest.getFiles(name);
			return (!files.isEmpty() ? files : null);
		} else if (isMultipartFileArray(parameter)) {
			// 如果参数是MultipartFile数组
			if (!isMultipart) {
				// 如果请求不是multipart类型，则返回null
				return null;
			}
			if (multipartRequest == null) {
				// 如果multipartRequest为空，则创建StandardMultipartHttpServletRequest对象
				multipartRequest = new StandardMultipartHttpServletRequest(request);
			}
			// 返回指定名称的文件数组，如果为空则返回null
			List<MultipartFile> files = multipartRequest.getFiles(name);
			return (!files.isEmpty() ? files.toArray(new MultipartFile[0]) : null);
		} else if (Part.class == parameter.getNestedParameterType()) {
			// 如果参数类型为Part
			if (!isMultipart) {
				// 如果请求不是multipart类型，则返回null
				return null;
			}
			// 返回指定名称的Part
			return request.getPart(name);
		} else if (isPartCollection(parameter)) {
			// 如果参数是Part集合
			if (!isMultipart) {
				// 如果请求不是multipart类型，则返回null
				return null;
			}
			// 解析指定名称的Part集合
			List<Part> parts = resolvePartList(request, name);
			return (!parts.isEmpty() ? parts : null);
		} else if (isPartArray(parameter)) {
			// 如果参数是Part数组
			if (!isMultipart) {
				// 如果请求不是multipart类型，则返回null
				return null;
			}
			// 解析指定名称的Part数组
			List<Part> parts = resolvePartList(request, name);
			return (!parts.isEmpty() ? parts.toArray(new Part[0]) : null);
		} else {
			// 如果参数类型无法解析，则返回UNRESOLVABLE
			return UNRESOLVABLE;
		}
	}

	private static boolean isMultipartFileCollection(MethodParameter methodParam) {
		return (MultipartFile.class == getCollectionParameterType(methodParam));
	}

	private static boolean isMultipartFileArray(MethodParameter methodParam) {
		return (MultipartFile.class == methodParam.getNestedParameterType().getComponentType());
	}

	private static boolean isPartCollection(MethodParameter methodParam) {
		return (Part.class == getCollectionParameterType(methodParam));
	}

	private static boolean isPartArray(MethodParameter methodParam) {
		return (Part.class == methodParam.getNestedParameterType().getComponentType());
	}

	@Nullable
	private static Class<?> getCollectionParameterType(MethodParameter methodParam) {
		// 获取方法参数的嵌套类型
		Class<?> paramType = methodParam.getNestedParameterType();
		// 如果参数类型是Collection类，或者是List类的子类
		if (Collection.class == paramType || List.class.isAssignableFrom(paramType)) {
			// 获取集合元素的类型
			Class<?> valueType = ResolvableType.forMethodParameter(methodParam).asCollection().resolveGeneric();
			// 如果集合元素的类型不为空，则返回该类型
			if (valueType != null) {
				return valueType;
			}
		}
		// 如果无法解析集合元素的类型，则返回null
		return null;
	}

	private static List<Part> resolvePartList(HttpServletRequest request, String name) throws Exception {
		// 获取请求中的所有Part
		Collection<Part> parts = request.getParts();
		// 创建一个ArrayList来存储匹配name的Part
		List<Part> result = new ArrayList<>(parts.size());
		// 遍历所有的Part
		for (Part part : parts) {
			// 如果Part的名称与给定的name相等，则将其添加到result中
			if (part.getName().equals(name)) {
				result.add(part);
			}
		}
		// 返回匹配name的Part集合
		return result;
	}

}
