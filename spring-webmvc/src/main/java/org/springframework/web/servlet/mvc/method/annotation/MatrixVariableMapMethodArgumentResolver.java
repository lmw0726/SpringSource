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
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 用于解析带有 {@link MatrixVariable @MatrixVariable} 注解且未指定名称的 {@link Map} 类型参数。
 * 换句话说，此解析器的目的是提供对多个矩阵变量的访问，可以是所有变量或与特定路径变量相关联的变量。
 *
 * <p>当指定了名称时，将 Map 类型的参数视为具有 Map 值的单个属性，并由 {@link MatrixVariableMethodArgumentResolver} 解析。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MatrixVariableMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 获取方法参数上的@MatrixVariable注解
		MatrixVariable matrixVariable = parameter.getParameterAnnotation(MatrixVariable.class);
		// 如果该注解存在，并且方法参数类型是Map以及实现类，矩阵变量的名称不为空，返回true。否则返回false。
		return (matrixVariable != null && Map.class.isAssignableFrom(parameter.getParameterType()) &&
				!StringUtils.hasText(matrixVariable.name()));
	}

	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest request, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		// 从请求属性中获取矩阵变量
		@SuppressWarnings("unchecked")
		Map<String, MultiValueMap<String, String>> matrixVariables =
				(Map<String, MultiValueMap<String, String>>) request.getAttribute(
						HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		// 如果矩阵变量为空，则返回空的映射
		if (CollectionUtils.isEmpty(matrixVariables)) {
			return Collections.emptyMap();
		}

		// 创建一个 LinkedMultiValueMap 对象
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		// 获取方法参数上的 @MatrixVariable 注解
		MatrixVariable ann = parameter.getParameterAnnotation(MatrixVariable.class);
		// 断言注解不为空
		Assert.state(ann != null, "No MatrixVariable annotation");
		// 获取路径变量名
		String pathVariable = ann.pathVar();

		// 如果路径变量不是默认值
		if (!pathVariable.equals(ValueConstants.DEFAULT_NONE)) {
			// 获取指定路径变量名的矩阵变量映射
			MultiValueMap<String, String> mapForPathVariable = matrixVariables.get(pathVariable);
			// 如果映射为空，则返回空的映射
			if (mapForPathVariable == null) {
				return Collections.emptyMap();
			}
			// 将映射中的所有值添加到 map 中
			map.putAll(mapForPathVariable);
		} else {
			// 遍历所有的矩阵变量映射
			for (MultiValueMap<String, String> vars : matrixVariables.values()) {
				// 遍历当前映射中的所有键值对
				vars.forEach((name, values) -> {
					// 将每个值添加到 map 中
					for (String value : values) {
						map.add(name, value);
					}
				});
			}
		}

		// 如果方法参数是单值映射，则将 map 转换为单值映射并返回，否则返回原始映射
		return (isSingleValueMap(parameter) ? map.toSingleValueMap() : map);
	}

	private boolean isSingleValueMap(MethodParameter parameter) {
		// 如果参数类型不是 MultiValueMap 类型
		if (!MultiValueMap.class.isAssignableFrom(parameter.getParameterType())) {
			// 获取方法参数的泛型类型
			ResolvableType[] genericTypes = ResolvableType.forMethodParameter(parameter).getGenerics();
			// 如果泛型类型数组长度为 2
			if (genericTypes.length == 2) {
				// 返回第二个泛型类型不是 List 类型
				return !List.class.isAssignableFrom(genericTypes[1].toClass());
			}
		}
		// 否则返回 false
		return false;
	}

}
