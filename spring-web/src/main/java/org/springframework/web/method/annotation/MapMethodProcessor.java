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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Map;

/**
 * 解析 {@link Map} 方法参数并处理 {@link Map} 返回值。
 *
 * <p>根据是否存在诸如 {@code @ModelAttribute} 或 {@code @ResponseBody} 等注解，Map 返回值可以有多种解释方式。从 5.2 版本开始，如果参数被注解，此解析器将返回 false。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class MapMethodProcessor implements HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 检查参数类型是否是Map的子类或实现类，并且参数上没有注解
		return (Map.class.isAssignableFrom(parameter.getParameterType()) &&
				parameter.getParameterAnnotations().length == 0);
	}

	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		Assert.state(mavContainer != null, "ModelAndViewContainer is required for model exposure");
		// 获取模型数据
		return mavContainer.getModel();
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// 检查返回类型是否是Map的子类或实现类
		return Map.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue instanceof Map) {
			// 如果返回值是Map类型，则将其添加到模型中
			mavContainer.addAllAttributes((Map) returnValue);
		} else if (returnValue != null) {
			// 如果返回值不为空，但不是Map类型，则抛出异常
			throw new UnsupportedOperationException("Unexpected return type [" +
					returnType.getParameterType().getName() + "] in method: " + returnType.getMethod());
		}
	}

}
