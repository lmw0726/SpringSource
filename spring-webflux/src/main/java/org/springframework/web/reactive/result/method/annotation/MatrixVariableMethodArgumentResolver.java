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

package org.springframework.web.reactive.result.method.annotation;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 解析带有 {@link MatrixVariable @MatrixVariable} 注解的参数。
 *
 * <p>如果方法参数的类型是 {@link Map}，则由 {@link MatrixVariableMapMethodArgumentResolver} 解析，
 * 除非注解指定了名称，在这种情况下，它被视为映射类型的单个属性（而不是在映射中收集的多个属性）。
 *
 * @author Rossen Stoyanchev
 * @see MatrixVariableMapMethodArgumentResolver
 * @since 5.0.1
 */
public class MatrixVariableMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	public MatrixVariableMethodArgumentResolver(
			@Nullable ConfigurableBeanFactory factory, ReactiveAdapterRegistry registry) {

		super(factory, registry);
	}

	/**
	 * 检查方法参数是否支持。
	 *
	 * @param parameter 方法参数
	 * @return 是否支持该参数
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		//带有@MatrixVariable 注解，参数类型没有嵌套类，并且参数类型不是Map类型，或者@MatrixVariable有名称属性值。
		return checkAnnotatedParamNoReactiveWrapper(parameter, MatrixVariable.class,
				(ann, type) -> !Map.class.isAssignableFrom(type) || StringUtils.hasText(ann.name()));
	}

	/**
	 * 创建命名值信息。
	 *
	 * @param parameter 方法参数
	 * @return 命名值信息
	 */
	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		MatrixVariable ann = parameter.getParameterAnnotation(MatrixVariable.class);
		Assert.state(ann != null, "No MatrixVariable annotation");
		return new MatrixVariableNamedValueInfo(ann);
	}

	/**
	 * 解析命名值。
	 *
	 * @param name     名称
	 * @param param    参数
	 * @param exchange 服务器Web交换
	 * @return 解析后的对象
	 */
	@Nullable
	@Override
	protected Object resolveNamedValue(String name, MethodParameter param, ServerWebExchange exchange) {
		// 获取路径参数的矩阵变量
		Map<String, MultiValueMap<String, String>> pathParameters =
				exchange.getAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE);

		// 如果路径参数为空，返回null
		if (CollectionUtils.isEmpty(pathParameters)) {
			return null;
		}

		// 获取方法参数上的MatrixVariable注解
		MatrixVariable ann = param.getParameterAnnotation(MatrixVariable.class);
		Assert.state(ann != null, "No MatrixVariable annotation");

		// 获取MatrixVariable注解上的pathVar属性
		String pathVar = ann.pathVar();
		List<String> paramValues = null;

		// 如果pathVar属性不等于ValueConstants.DEFAULT_NONE
		if (!pathVar.equals(ValueConstants.DEFAULT_NONE)) {
			// 如果路径参数包含pathVar属性，获取对应参数值
			if (pathParameters.containsKey(pathVar)) {
				paramValues = pathParameters.get(pathVar).get(name);
			}
		} else {
			// 否则，遍历所有路径参数，查找匹配的参数值
			boolean found = false;
			paramValues = new ArrayList<>();
			for (MultiValueMap<String, String> params : pathParameters.values()) {
				if (params.containsKey(name)) {
					// 如果找到多个匹配项，抛出ServerErrorException异常
					if (found) {
						String paramType = param.getNestedParameterType().getName();
						throw new ServerErrorException(
								"Found more than one match for URI path parameter '" + name +
										"' for parameter type [" + paramType + "]. Use 'pathVar' attribute to disambiguate.",
								param, null);
					}
					paramValues.addAll(params.get(name));
					found = true;
				}
			}
		}

		if (CollectionUtils.isEmpty(paramValues)) {
			// 如果解析后的参数值列表为空，返回null；
			return null;
		} else if (paramValues.size() == 1) {
			// 如果只有一个参数值，返回该值；
			return paramValues.get(0);
		} else {
			// 如果有多个参数值，返回参数值列表
			return paramValues;
		}
	}

	/**
	 * 处理缺失值情况。
	 *
	 * @param name      参数名称
	 * @param parameter 方法参数
	 * @throws ServerWebInputException 服务器输入异常
	 */
	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServerWebInputException {
		String paramInfo = parameter.getNestedParameterType().getSimpleName();
		throw new ServerWebInputException("Missing matrix variable '" + name + "' " +
				"for method parameter of type " + paramInfo, parameter);
	}

	/**
	 * 矩阵变量命名值信息类。
	 */
	private static final class MatrixVariableNamedValueInfo extends NamedValueInfo {

		/**
		 * 构造方法，用于初始化矩阵变量命名值信息。
		 *
		 * @param annotation MatrixVariable注解
		 */
		private MatrixVariableNamedValueInfo(MatrixVariable annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}
}
