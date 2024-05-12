/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.RequestToViewNameTranslator;

/**
 * 处理 {@code void} 和 {@code String} 类型的返回值，将它们解释为视图名称引用。从 4.2 版本开始，它还可以处理一般的 {@code CharSequence} 类型，
 * 例如 {@code StringBuilder} 或 Groovy 的 {@code GString}，作为视图名称。
 *
 * <p>{@code null} 返回值，无论是由于 {@code void} 返回类型还是作为实际返回值，都保持不变，允许配置的 {@link RequestToViewNameTranslator}
 * 按照约定选择视图名称。
 *
 * <p>字符串返回值可以根据是否存在 {@code @ModelAttribute} 或 {@code @ResponseBody} 等注解而被解释为不止一种方式。因此，此处理器应该配置在支持这些注解的处理器之后。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ViewNameMethodReturnValueHandler implements HandlerMethodReturnValueHandler {
	/**
	 * 转发URL模式
	 */
	@Nullable
	private String[] redirectPatterns;


	/**
	 * 配置一个或多个简单模式（如 {@link PatternMatchUtils#simpleMatch} 中描述的）以用于识别自定义重定向前缀，除了 "redirect:" 之外。
	 * <p>请注意，仅配置此属性并不会使自定义重定向前缀生效。必须有一个自定义视图也识别该前缀。
	 *
	 * @since 4.1
	 */
	public void setRedirectPatterns(@Nullable String... redirectPatterns) {
		this.redirectPatterns = redirectPatterns;
	}

	/**
	 * 已配置的重定向模式，如果有的话。
	 */
	@Nullable
	public String[] getRedirectPatterns() {
		return this.redirectPatterns;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// 获取返回值类型
		Class<?> paramType = returnType.getParameterType();
		// 如果返回值类型为void类型，或者是 CharSequence 类型及其实现类
		return (void.class == paramType || CharSequence.class.isAssignableFrom(paramType));
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		// 如果返回值是 CharSequence 类型
		if (returnValue instanceof CharSequence) {
			// 获取视图名称
			String viewName = returnValue.toString();
			// 向模型和视图容器设置视图名称
			mavContainer.setViewName(viewName);
			if (isRedirectViewName(viewName)) {
				// 如果是重定向视图名称，设置重定向模型场景为 true
				mavContainer.setRedirectModelScenario(true);
			}
		} else if (returnValue != null) {
			// 不应该发生的情况
			throw new UnsupportedOperationException("Unexpected return type: " +
					returnType.getParameterType().getName() + " in method: " + returnType.getMethod());
		}
	}

	/**
	 * 判断给定的视图名称是否是重定向视图引用。默认实现检查配置的重定向模式以及视图名称是否以 "redirect:" 前缀开头。
	 *
	 * @param viewName 要检查的视图名称，永远不会为 {@code null}
	 * @return 如果给定的视图名称被识别为重定向视图引用，则为 {@code true}；否则为 {@code false}。
	 */
	protected boolean isRedirectViewName(String viewName) {
		// 检查视图名是否与重定向模式匹配或者以 "redirect:" 开头
		return (PatternMatchUtils.simpleMatch(this.redirectPatterns, viewName) || viewName.startsWith("redirect:"));
	}

}
