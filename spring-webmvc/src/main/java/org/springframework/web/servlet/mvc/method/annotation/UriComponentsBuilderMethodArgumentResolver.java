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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;


/**
 * 解析类型为 {@link UriComponentsBuilder} 的参数值。
 *
 * <p>返回的实例通过 {@link ServletUriComponentsBuilder#fromServletMapping(HttpServletRequest)} 进行初始化。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class UriComponentsBuilderMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 获取参数的类型
		Class<?> type = parameter.getParameterType();
		// 判断参数类型是否为 UriComponentsBuilder 或 ServletUriComponentsBuilder
		return (UriComponentsBuilder.class == type || ServletUriComponentsBuilder.class == type);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		// 从 WebRequest 中获取 HttpServletRequest
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		// 断言获取的 HttpServletRequest 不为空，如果为空则抛出异常
		Assert.state(request != null, "No HttpServletRequest");
		// 使用 HttpServletRequest 创建 ServletUriComponentsBuilder
		return ServletUriComponentsBuilder.fromServletMapping(request);
	}

}
