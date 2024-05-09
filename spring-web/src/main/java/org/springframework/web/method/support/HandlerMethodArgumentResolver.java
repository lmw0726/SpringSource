/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 解析方法参数为给定请求上下文中的参数值的策略接口。
 *
 * @author Arjen Poutsma
 * @see HandlerMethodReturnValueHandler
 * @since 3.1
 */
public interface HandlerMethodArgumentResolver {

	/**
	 * 检查此解析器是否支持给定的 {@linkplain MethodParameter 方法参数}。
	 *
	 * @param parameter 要检查的方法参数
	 * @return 如果此解析器支持提供的参数，则为 {@code true}；否则为 {@code false}
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * 从给定请求中解析方法参数为参数值。{@link ModelAndViewContainer} 提供了对请求的模型的访问权限。{@link WebDataBinderFactory} 提供了在需要时创建 {@link WebDataBinder} 实例的方法，用于数据绑定和类型转换目的。
	 *
	 * @param parameter     要解析的方法参数。此参数必须先前传递给 {@link #supportsParameter}，并且该方法必须返回 {@code true}。
	 * @param mavContainer  当前请求的 ModelAndViewContainer
	 * @param webRequest    当前请求
	 * @param binderFactory 用于创建 {@link WebDataBinder} 实例的工厂
	 * @return 已解析的参数值，如果无法解析，则为 {@code null}
	 * @throws Exception 准备参数值时发生错误时
	 */
	@Nullable
	Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
						   NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception;

}
