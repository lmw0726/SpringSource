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

package org.springframework.web.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 解析带有 @{@link RequestParam} 注解的 {@link Map} 方法参数，其中注解未指定请求参数名称。
 *
 * <p>创建的 {@link Map} 包含所有请求参数名称/值对，或者对于具有 {@link MultipartFile} 作为值类型的给定参数名称，包含所有多部分文件。
 * 如果方法参数类型为 {@link MultiValueMap}，则创建的映射包含所有请求参数及其值，以处理请求参数具有多个值（或相同名称的多个多部分文件）的情况。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see RequestParamMethodArgumentResolver
 * @see HttpServletRequest#getParameterMap()
 * @see MultipartRequest#getMultiFileMap()
 * @see MultipartRequest#getFileMap()
 * @since 3.1
 */
public class RequestParamMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 获取方法参数上的@RequestParam注解
		RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
		// 如果存在该注解，并且参数类型为Map实现类，并且@RequestParam注解未指定请求参数名称，返回true。否则，返回false。
		return (requestParam != null && Map.class.isAssignableFrom(parameter.getParameterType()) &&
				!StringUtils.hasText(requestParam.name()));
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		// 获取方法参数的可解析类型
		ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);

		if (MultiValueMap.class.isAssignableFrom(parameter.getParameterType())) {
			// 如果参数类型为MultiValueMap
			// 获取MultiValueMap的值类型
			Class<?> valueType = resolvableType.as(MultiValueMap.class).getGeneric(1).resolve();
			// 如果值类型为MultipartFile
			if (valueType == MultipartFile.class) {
				// 解析MultipartRequest以获取MultipartFile
				MultipartRequest multipartRequest = MultipartResolutionDelegate.resolveMultipartRequest(webRequest);
				// 返回MultipartFile的MultiValueMap
				return (multipartRequest != null ? multipartRequest.getMultiFileMap() : new LinkedMultiValueMap<>(0));
			} else if (valueType == Part.class) {
				// 如果值类型为Part
				// 从Web请求中获取HttpServletRequest
				HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
				// 如果 Http请求 不为空且为多部分请求
				if (servletRequest != null && MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
					// 获取所有Part
					Collection<Part> parts = servletRequest.getParts();
					// 创建LinkedMultiValueMap以存储Part
					LinkedMultiValueMap<String, Part> result = new LinkedMultiValueMap<>(parts.size());
					// 将Part添加到MultiValueMap中
					for (Part part : parts) {
						result.add(part.getName(), part);
					}
					return result;
				}
				// 返回空的LinkedMultiValueMap
				return new LinkedMultiValueMap<>(0);
			} else {
				// 否则，将请求参数转换为MultiValueMap<String, String>
				// 获取请求参数映射
				Map<String, String[]> parameterMap = webRequest.getParameterMap();
				// 创建MultiValueMap以存储请求参数
				MultiValueMap<String, String> result = new LinkedMultiValueMap<>(parameterMap.size());
				// 将请求参数映射添加到MultiValueMap中
				parameterMap.forEach((key, values) -> {
					for (String value : values) {
						result.add(key, value);
					}
				});
				return result;
			}
		} else {
			// 如果参数类型为普通Map
			// 获取Map的值类型
			Class<?> valueType = resolvableType.asMap().getGeneric(1).resolve();
			// 如果值类型为MultipartFile
			if (valueType == MultipartFile.class) {
				// 解析MultipartRequest以获取MultipartFile
				MultipartRequest multipartRequest = MultipartResolutionDelegate.resolveMultipartRequest(webRequest);
				// 返回MultipartFile的Map
				return (multipartRequest != null ? multipartRequest.getFileMap() : new LinkedHashMap<>(0));
			} else if (valueType == Part.class) {
				// 如果值类型为Part
				// 从Web请求中获取 http请求
				HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
				// 如果 http请求 不为空且为多部分请求
				if (servletRequest != null && MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
					// 获取所有Part
					Collection<Part> parts = servletRequest.getParts();
					// 创建LinkedHashMap以存储Part
					LinkedHashMap<String, Part> result = CollectionUtils.newLinkedHashMap(parts.size());
					// 将Part添加到Map中
					for (Part part : parts) {
						if (!result.containsKey(part.getName())) {
							result.put(part.getName(), part);
						}
					}
					// 返回存储 Part 的LinkedHashMap
					return result;
				}
				// 返回空的LinkedHashMap
				return new LinkedHashMap<>(0);
			} else {
				// 否则，将请求参数转换为Map<String, String>
				// 获取请求参数映射
				Map<String, String[]> parameterMap = webRequest.getParameterMap();
				// 创建LinkedHashMap以存储请求参数
				Map<String, String> result = CollectionUtils.newLinkedHashMap(parameterMap.size());
				// 将请求参数映射添加到Map中
				parameterMap.forEach((key, values) -> {
					if (values.length > 0) {
						result.put(key, values[0]);
					}
				});
				return result;
			}
		}
	}

}
