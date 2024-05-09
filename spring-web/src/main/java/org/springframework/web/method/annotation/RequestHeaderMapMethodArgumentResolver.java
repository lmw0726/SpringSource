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

package org.springframework.web.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 解析使用 {@code @RequestHeader} 注解的 {@link Map} 方法参数。
 * 对于使用 {@code @RequestHeader} 注解的单个标头值，请参见 {@link RequestHeaderMethodArgumentResolver}。
 *
 * <p>创建的 {@link Map} 包含所有请求标头名称/值对。
 * 方法参数类型可以是 {@link MultiValueMap}，以接收标头的所有值，而不仅仅是第一个值。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RequestHeaderMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 如果参数上有@RequestHeader注解，并且参数类型是Map或Map的子类，则返回true，否则返回false
		return (parameter.hasParameterAnnotation(RequestHeader.class) &&
				Map.class.isAssignableFrom(parameter.getParameterType()));
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
		// 获取参数的类型
		Class<?> paramType = parameter.getParameterType();
		// 如果参数的类型是MultiValueMap或其子类
		if (MultiValueMap.class.isAssignableFrom(paramType)) {
			MultiValueMap<String, String> result;
			// 如果参数的类型是HttpHeaders或其子类，则创建一个HttpHeaders实例
			if (HttpHeaders.class.isAssignableFrom(paramType)) {
				result = new HttpHeaders();
			} else {
				// 否则，创建一个LinkedMultiValueMap实例
				result = new LinkedMultiValueMap<>();
			}
			// 遍历请求中的所有头部名称
			for (Iterator<String> iterator = webRequest.getHeaderNames(); iterator.hasNext(); ) {
				// 获取头部名称
				String headerName = iterator.next();
				// 获取指定头部名称对应的所有值
				String[] headerValues = webRequest.getHeaderValues(headerName);
				// 如果值不为空，则将值添加到结果中
				if (headerValues != null) {
					for (String headerValue : headerValues) {
						result.add(headerName, headerValue);
					}
				}
			}
			return result;
		} else {
			// 如果参数类型不是MultiValueMap或其子类，则创建一个LinkedHashMap实例来存储头部信息
			Map<String, String> result = new LinkedHashMap<>();
			// 遍历请求中的所有头部名称
			for (Iterator<String> iterator = webRequest.getHeaderNames(); iterator.hasNext(); ) {
				// 获取请求头名称
				String headerName = iterator.next();
				// 获取指定头部名称对应的值
				String headerValue = webRequest.getHeader(headerName);
				// 如果值不为空，则将键值对添加到结果中
				if (headerValue != null) {
					result.put(headerName, headerValue);
				}
			}
			return result;
		}
	}

}
