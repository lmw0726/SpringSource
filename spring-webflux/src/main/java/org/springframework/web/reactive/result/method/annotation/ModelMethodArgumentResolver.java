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

package org.springframework.web.reactive.result.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.ui.Model;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;

/**
 * 用于解析控制器方法参数的解析器，参数类型为 {@link Model}，也可以解析为 {@link java.util.Map}。
 *
 * <p>一个 Map 返回值可以根据 {@code @ModelAttribute} 或 {@code @ResponseBody} 等注解的存在以不止一种方式进行解释。
 * 从 5.2 版本开始，如果 {@code Map} 类型的参数也被注解，则此解析器返回 false。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class ModelMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public ModelMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}


	/**
	 * 该参数解析器是否支持方法参数
	 *
	 * @param param 方法参数
	 * @return 是否支持方法参数
	 */
	@Override
	public boolean supportsParameter(MethodParameter param) {
		return checkParameterTypeNoReactiveWrapper(param, type ->
				// 参数类型或它的嵌套类型是否为 Model 类型
				Model.class.isAssignableFrom(type) ||
						// 或者是否为未带注解的 Map 类型
						(Map.class.isAssignableFrom(type) && param.getParameterAnnotations().length == 0));
	}

	/**
	 * 解析参数值
	 *
	 * @param parameter 方法参数
	 * @param context   绑定上下文
	 * @param exchange  当前交换
	 * @return 参数值
	 */
	@Override
	public Object resolveArgumentValue(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		Class<?> type = parameter.getParameterType();
		if (Model.class.isAssignableFrom(type)) {
			// 如果参数类型是 Model 类型，则返回 BindingContext 中的 Model 对象
			return context.getModel();
		} else if (Map.class.isAssignableFrom(type)) {
			// 如果参数类型是 Map 类型，则返回 BindingContext 中的 Model 对象的 Map 表示形式
			return context.getModel().asMap();
		} else {
			// 不应该发生的情况...
			throw new IllegalStateException("Unexpected method parameter type: " + type);
		}
	}

}
