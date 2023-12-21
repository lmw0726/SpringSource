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

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 解析使用{@link MatrixVariable @MatrixVariable}注释且注释未指定名称的{@link Map}类型的参数。
 * 换句话说，此解析器的目的是提供对多个矩阵变量的访问，要么全部，要么与特定路径变量关联。
 *
 * <p>当指定名称时，类型为Map的参数被视为具有Map值的单个属性，并由{@link MatrixVariableMethodArgumentResolver}解析。
 *
 * @author Rossen Stoyanchev
 * @see MatrixVariableMethodArgumentResolver
 * @since 5.0.1
 */
public class MatrixVariableMapMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public MatrixVariableMapMethodArgumentResolver(ReactiveAdapterRegistry registry) {
		super(registry);
	}


	/**
	 * 检查解析器是否支持给定的方法参数。
	 *
	 * @param parameter 方法参数
	 * @return true表示该解析器支持方法参数
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		//检查方法参数是否有@MatrixVariable注解，
		// 如果没有，进一步检查嵌套参数类型内有没有@MatrixVariable注解，且类型为Map类型，并且@MatrixVariable的名称不为空
		return checkAnnotatedParamNoReactiveWrapper(parameter, MatrixVariable.class,
				(ann, type) -> (Map.class.isAssignableFrom(type) && !StringUtils.hasText(ann.name())));
	}

	/**
	 * 解析参数值并返回对象。
	 *
	 * @param parameter      方法参数
	 * @param bindingContext 绑定上下文
	 * @param exchange       ServerWebExchange 对象
	 * @return 解析后的对象
	 */
	@Nullable
	@Override
	public Object resolveArgumentValue(MethodParameter parameter, BindingContext bindingContext,
									   ServerWebExchange exchange) {

		// 获取路径矩阵变量
		Map<String, MultiValueMap<String, String>> matrixVariables =
				exchange.getAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE);

		// 如果路径矩阵变量为空，返回空的 Map
		if (CollectionUtils.isEmpty(matrixVariables)) {
			return Collections.emptyMap();
		}

		// 创建 LinkedMultiValueMap 对象
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();

		// 获取方法参数上的 MatrixVariable 注解
		MatrixVariable annotation = parameter.getParameterAnnotation(MatrixVariable.class);

		// 断言确保注解不为 null
		Assert.state(annotation != null, "No MatrixVariable annotation");
		//获取路径变量
		String pathVariable = annotation.pathVar();

		// 如果 路径变量 是默认值
		if (pathVariable.equals(ValueConstants.DEFAULT_NONE)) {
			// 遍历所有路径矩阵变量的值
			for (MultiValueMap<String, String> vars : matrixVariables.values()) {
				vars.forEach((name, values) -> {
					// 将值逐个添加到 map 中
					for (String value : values) {
						map.add(name, value);
					}
				});
			}
		} else {
			// 获取指定路径矩阵变量的 mapForPathVariable
			MultiValueMap<String, String> mapForPathVariable = matrixVariables.get(pathVariable);
			// 如果 mapForPathVariable 为空，返回空的 Map
			if (mapForPathVariable == null) {
				return Collections.emptyMap();
			}
			// 将 mapForPathVariable 添加到 map 中
			map.putAll(mapForPathVariable);
		}

		// 判断是否为单值映射，如果是，则转换为单值映射返回；否则返回多值映射
		return (isSingleValueMap(parameter) ? map.toSingleValueMap() : map);
	}


	/**
	 * 判断给定的方法参数是否为单值映射（SingleValueMap）。
	 *
	 * @param parameter 要检查的方法参数
	 * @return 如果是单值映射，则返回 true；否则返回 false
	 */
	private boolean isSingleValueMap(MethodParameter parameter) {
		// 检查参数类型是否不是 MultiValueMap 的子类
		if (!MultiValueMap.class.isAssignableFrom(parameter.getParameterType())) {
			// 获取方法参数的泛型类型数组
			ResolvableType[] genericTypes = ResolvableType.forMethodParameter(parameter).getGenerics();
			// 如果泛型类型数组长度为2
			if (genericTypes.length == 2) {
				// 判断第二个泛型类型是否不是 List 的子类
				return !List.class.isAssignableFrom(genericTypes[1].toClass());
			}
		}
		// 如果不满足上述条件，则返回 false
		return false;
	}


}
