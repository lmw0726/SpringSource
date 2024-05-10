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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

/**
 * 解析类型为 {@link Principal} 的参数，类似于 {@link ServletRequestMethodArgumentResolver}，
 * 但不考虑参数是否已注释。这样做是为了能够自定义解析 {@link Principal} 参数（带有自定义注释）。
 *
 * @author Rossen Stoyanchev
 * @since 5.3.1
 */
public class PrincipalMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 检查参数类型是否是Principal以及实现类
		return Principal.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		// 从Web请求中获取Http请求
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		// 如果获取到的Http请求为空
		if (request == null) {
			// 抛出IllegalStateException异常，指示当前请求不是HttpServletRequest类型
			throw new IllegalStateException("Current request is not of type HttpServletRequest: " + webRequest);
		}

		// 从Http请求中获取Principal对象
		Principal principal = request.getUserPrincipal();
		// 如果Principal对象不为空，且不是参数类型的实例
		if (principal != null && !parameter.getParameterType().isInstance(principal)) {
			// 抛出IllegalStateException异常，指示当前用户Principal不是参数类型的实例
			throw new IllegalStateException("Current user principal is not of type [" +
					parameter.getParameterType().getName() + "]: " + principal);
		}

		// 返回Principal对象
		return principal;
	}

}
