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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 解析使用 @{@link PathVariable} 注解但注解未指定路径变量名称的 {@link Map} 方法参数。
 * 创建的 {@link Map} 包含所有 URI 模板名称/值对。
 *
 * @author Rossen Stoyanchev
 * @see PathVariableMethodArgumentResolver
 * @since 3.2
 */
public class PathVariableMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 获取方法参数上的@PathVariable注解
		PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
		// 检查是否存在@PathVariable注解，并且参数类型是Map的子类或实现类，并且@PathVariable注解的值不为空
		return (ann != null && Map.class.isAssignableFrom(parameter.getParameterType()) &&
				!StringUtils.hasText(ann.value()));
	}

	/**
	 * 返回一个包含所有 URI 模板变量的 Map，或者返回一个空 Map。
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
		// 从原始请求中获取URL模板变量
		@SuppressWarnings("unchecked")
		Map<String, String> uriTemplateVars =
				(Map<String, String>) webRequest.getAttribute(
						HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		if (!CollectionUtils.isEmpty(uriTemplateVars)) {
			// 如果URI模板变量不为空，则返回一个新的LinkedHashMap
			return new LinkedHashMap<>(uriTemplateVars);
		} else {
			// 如果URI模板变量为空，则返回一个空的Map
			return Collections.emptyMap();
		}
	}

}
