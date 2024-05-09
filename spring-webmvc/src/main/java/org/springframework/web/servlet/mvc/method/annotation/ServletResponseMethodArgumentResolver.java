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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * 解析与 servlet 相关的响应方法参数。支持以下类型的值：
 * <ul>
 * <li>{@link ServletResponse}
 * <li>{@link OutputStream}
 * <li>{@link Writer}
 * </ul>
 * <p>
 * 作者：Arjen Poutsma
 * Rossen Stoyanchev
 * Juergen Hoeller
 *
 * @since 3.1
 */
public class ServletResponseMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 获取参数的类型
		Class<?> paramType = parameter.getParameterType();
		// 如果参数的类型是ServletResponse或其子类，或者是OutputStream或其子类，或者是Writer或其子类，则返回true，否则返回false
		return (ServletResponse.class.isAssignableFrom(paramType) ||
				OutputStream.class.isAssignableFrom(paramType) ||
				Writer.class.isAssignableFrom(paramType));
	}

	/**
	 * 设置 {@link ModelAndViewContainer#setRequestHandled(boolean)} 为
	 * {@code false}，表示方法签名提供了对响应的访问权限。如果随后底层方法返回
	 * {@code null}，则请求被认为是直接处理的。
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		// 如果存在ModelAndViewContainer，则设置请求已处理
		if (mavContainer != null) {
			mavContainer.setRequestHandled(true);
		}

		// 获取参数的类型
		Class<?> paramType = parameter.getParameterType();

		// 如果参数的类型是ServletResponse或其子类，则解析为原生响应
		if (ServletResponse.class.isAssignableFrom(paramType)) {
			return resolveNativeResponse(webRequest, paramType);
		}

		// 如果参数的类型不是ServletResponse或其子类，则解析原生响应，并将其用于进一步解析其他参数类型
		// （即：参数类型需要基于ServletResponse的子类）
		return resolveArgument(paramType, resolveNativeResponse(webRequest, ServletResponse.class));
	}

	private <T> T resolveNativeResponse(NativeWebRequest webRequest, Class<T> requiredType) {
		// 获取原生响应
		T nativeResponse = webRequest.getNativeResponse(requiredType);
		if (nativeResponse == null) {
			// 如果原生响应不存在，则抛出异常
			throw new IllegalStateException(
					"Current response is not of type [" + requiredType.getName() + "]: " + webRequest);
		}
		return nativeResponse;
	}

	private Object resolveArgument(Class<?> paramType, ServletResponse response) throws IOException {
		if (OutputStream.class.isAssignableFrom(paramType)) {
			// 如果参数类型是OutputStream或其子类，则返回响应的OutputStream
			return response.getOutputStream();
		} else if (Writer.class.isAssignableFrom(paramType)) {
			// 如果参数类型是Writer或其子类，则返回响应的Writer
			return response.getWriter();
		}
		// 不应该发生...
		throw new UnsupportedOperationException("Unknown parameter type: " + paramType);
	}

}
