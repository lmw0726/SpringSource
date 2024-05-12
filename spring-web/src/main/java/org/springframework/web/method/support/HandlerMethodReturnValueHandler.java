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

package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 策略接口，用于处理调用处理程序方法后返回的值。
 *
 * @author Arjen Poutsma
 * @since 3.1
 * @see HandlerMethodArgumentResolver
 */
public interface HandlerMethodReturnValueHandler {

	/**
	 * 检查此处理程序是否支持给定的 {@linkplain MethodParameter 方法返回类型}。
	 *
	 * @param returnType 要检查的方法返回类型
	 * @return 如果此处理程序支持提供的返回类型，则为 {@code true}；否则为 {@code false}
	 */
	boolean supportsReturnType(MethodParameter returnType);

	/**
	 * 通过向模型添加属性或设置视图，或者将 {@link ModelAndViewContainer#setRequestHandled} 标志设置为 {@code true}
	 * 来处理给定的返回值，以指示响应已直接处理。
	 *
	 * @param returnValue 调用处理程序方法后返回的值
	 * @param returnType 返回值的类型。此类型必须先前已传递给 {@link #supportsReturnType}，该方法必须返回 {@code true}。
	 * @param mavContainer 当前请求的 ModelAndViewContainer
	 * @param webRequest 当前请求
	 * @throws Exception 如果返回值处理导致错误
	 */
	void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
						   ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception;

}
