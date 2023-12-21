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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

import java.util.Collections;
import java.util.Map;

/**
 * 用于解析带有 {@link PathVariable @PathVariable} 注解但注解未指定路径变量名称的 {@link Map} 方法参数的解析器。
 * 结果 {@link Map} 参数是 URI 模板名称-值对的副本。
 *
 * @author Rossen Stoyanchev
 * @see PathVariableMethodArgumentResolver
 * @since 5.0
 */
public class PathVariableMapMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public PathVariableMapMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}


	/**
	 * 检查是否支持给定的参数类型
	 *
	 * @param parameter 方法参数
	 * @return true表示该解析器支持方法参数
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return checkAnnotatedParamNoReactiveWrapper(parameter, PathVariable.class, this::allVariables);
	}


	/**
	 * 检查是否所有变量都符合条件
	 *
	 * @param pathVariable 路径变量
	 * @param type         参数类型
	 * @return true表示所有变量都符合条件
	 */
	private boolean allVariables(PathVariable pathVariable, Class<?> type) {
		//类型是Map类型，并且路径变量有值
		return (Map.class.isAssignableFrom(type) && !StringUtils.hasText(pathVariable.value()));
	}

	/**
	 * 解析参数值
	 *
	 * @param methodParameter 方法参数
	 * @param context         要使用的绑定上下文
	 * @param exchange        当前交换
	 * @return 参数值
	 */
	@Override
	public Object resolveArgumentValue(
			MethodParameter methodParameter, BindingContext context, ServerWebExchange exchange) {
		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		// 返回 URI 模板变量的属性或者空 Map（如果不存在）
		return exchange.getAttributeOrDefault(name, Collections.emptyMap());
	}

}
