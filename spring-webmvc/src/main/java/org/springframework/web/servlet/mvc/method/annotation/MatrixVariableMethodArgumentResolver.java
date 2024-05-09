/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingMatrixVariableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.servlet.HandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 解析带有 @{@link MatrixVariable} 注解的参数。
 *
 * <p>如果方法参数的类型是 {@link Map}，则会由 {@link MatrixVariableMapMethodArgumentResolver} 解析，
 * 除非注解指定了一个名称，在这种情况下，它被视为类型映射的单个属性（与在映射中收集的多个属性相对）。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class MatrixVariableMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	public MatrixVariableMethodArgumentResolver() {
		super(null);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 如果参数上没有@MatrixVariable注解，则返回false
		if (!parameter.hasParameterAnnotation(MatrixVariable.class)) {
			return false;
		}
		// 如果参数的嵌套类型是Map或Map的子类
		if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
			// 获取方法参数上的@MatrixVariable注解
			MatrixVariable matrixVariable = parameter.getParameterAnnotation(MatrixVariable.class);
			// 检查该注解的名称属性是否存在
			return (matrixVariable != null && StringUtils.hasText(matrixVariable.name()));
		}
		// 否则，返回true
		return true;
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		// 获取方法参数上的@MatrixVariable注解
		MatrixVariable annotation = parameter.getParameterAnnotation(MatrixVariable.class);
		Assert.state(annotation != null, "No MatrixVariable annotation");
		// 使用@MatrixVariable的信息创建 MatrixVariableNamedValueInfo
		return new MatrixVariableNamedValueInfo(annotation);
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nullable
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
		// 从请求属性中获取路径参数的映射关系
		Map<String, MultiValueMap<String, String>> pathParameters = (Map<String, MultiValueMap<String, String>>)
				request.getAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		// 如果路径参数映射关系为空，则返回null
		if (CollectionUtils.isEmpty(pathParameters)) {
			return null;
		}

		// 获取参数上的@MatrixVariable注解
		MatrixVariable annotation = parameter.getParameterAnnotation(MatrixVariable.class);
		Assert.state(annotation != null, "No MatrixVariable annotation");
		// 获取@MatrixVariable注解的pathVar属性值
		String pathVar = annotation.pathVar();
		List<String> paramValues = null;

		// 如果没有指定URI 路径变量的名称
		if (!pathVar.equals(ValueConstants.DEFAULT_NONE)) {
			// 如果路径参数映射中包含指定的 URI 路径变量的名称
			if (pathParameters.containsKey(pathVar)) {
				// 获取对应名称的参数值列表
				paramValues = pathParameters.get(pathVar).get(name);
			}
		} else {
			// 否则，遍历所有的路径参数映射
			boolean found = false;
			paramValues = new ArrayList<>();
			for (MultiValueMap<String, String> params : pathParameters.values()) {
				// 如果存在指定的名称参数
				if (params.containsKey(name)) {
					// 如果之前已经找到了匹配的参数，抛出异常
					if (found) {
						String paramType = parameter.getNestedParameterType().getName();
						throw new ServletRequestBindingException(
								"Found more than one match for URI path parameter '" + name +
										"' for parameter type [" + paramType + "]. Use 'pathVar' attribute to disambiguate.");
					}
					// 否则，将参数值添加到列表中，并标记为已找到匹配的参数
					paramValues.addAll(params.get(name));
					found = true;
				}
			}
		}

		if (CollectionUtils.isEmpty(paramValues)) {
			// 如果参数值列表为空，则返回null；
			return null;
		} else if (paramValues.size() == 1) {
			// 如果只有一个值，则返回该值；
			return paramValues.get(0);
		} else {
			// 否则，返回值列表
			return paramValues;
		}
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
		throw new MissingMatrixVariableException(name, parameter);
	}

	@Override
	protected void handleMissingValueAfterConversion(
			String name, MethodParameter parameter, NativeWebRequest request) throws Exception {

		throw new MissingMatrixVariableException(name, parameter, true);
	}

	private static final class MatrixVariableNamedValueInfo extends NamedValueInfo {

		private MatrixVariableNamedValueInfo(MatrixVariable annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
