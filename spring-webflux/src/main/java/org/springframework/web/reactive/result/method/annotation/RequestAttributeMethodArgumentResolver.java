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

package org.springframework.web.reactive.result.method.annotation;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * 解析带有 @{@link RequestAttribute} 注解的方法参数。
 *
 * @author Rossen Stoyanchev
 * @see SessionAttributeMethodArgumentResolver
 * @since 5.0
 */
public class RequestAttributeMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	/**
	 * 创建一个新的 {@link RequestAttributeMethodArgumentResolver} 实例。
	 *
	 * @param factory  用于解析默认值中的 {@code ${...}} 占位符和 {@code #{...}} SpEL 表达式的 bean 工厂；
	 *                 如果不期望默认值包含表达式，则为 {@code null}
	 * @param registry 用于检查响应式类型包装器的注册表
	 */
	public RequestAttributeMethodArgumentResolver(@Nullable ConfigurableBeanFactory factory,
												  ReactiveAdapterRegistry registry) {

		super(factory, registry);
	}

	/**
	 * 判断是否支持解析给定的方法参数。
	 *
	 * @param param 要检查的方法参数
	 * @return 如果支持解析参数则为 true，否则为 false
	 */
	@Override
	public boolean supportsParameter(MethodParameter param) {
		return param.hasParameterAnnotation(RequestAttribute.class);
	}


	/**
	 * 创建命名值信息。
	 *
	 * @param parameter 方法参数
	 * @return 命名值信息
	 * @throws IllegalStateException 如果不存在 RequestAttribute 注解，则抛出异常
	 */
	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestAttribute ann = parameter.getParameterAnnotation(RequestAttribute.class);
		Assert.state(ann != null, "No RequestAttribute annotation");
		return new NamedValueInfo(ann.name(), ann.required(), ValueConstants.DEFAULT_NONE);
	}

	/**
	 * 解析命名值。
	 *
	 * @param name      命名值的名称
	 * @param parameter 方法参数
	 * @param exchange  服务器Web交换对象
	 * @return 解析后的值
	 */
	@Override
	protected Object resolveNamedValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		// 获取指定名称的请求属性值
		Object value = exchange.getAttribute(name);

		// 获取要适配的响应式适配器
		ReactiveAdapter toAdapter = getAdapterRegistry().getAdapter(parameter.getParameterType());

		// 如果没有找到适配器，则直接返回属性值
		if (toAdapter == null) {
			return value;
		}

		// 如果值为空
		if (value == null) {
			// 检查目标类型是否支持空值，如果不支持则抛出异常
			Assert.isTrue(toAdapter.supportsEmpty(),
					() -> "No request attribute '" + name + "' and target type " +
							parameter.getGenericParameterType() + " doesn't support empty values.");
			// 返回一个空的 Mono
			return toAdapter.fromPublisher(Mono.empty());
		}

		// 如果参数类型与值的类型兼容，则直接返回值
		if (parameter.getParameterType().isAssignableFrom(value.getClass())) {
			return value;
		}

		// 获取值类型的响应式适配器
		ReactiveAdapter fromAdapter = getAdapterRegistry().getAdapter(value.getClass());

		// 检查值类型的适配器是否存在，如果不存在则抛出异常
		Assert.isTrue(fromAdapter != null,
				() -> getClass().getSimpleName() + " doesn't support " +
						"reactive type wrapper: " + parameter.getGenericParameterType());

		// 将值从源适配器转换到目标适配器，然后从目标适配器获取结果
		return toAdapter.fromPublisher(fromAdapter.toPublisher(value));

	}

	/**
	 * 处理缺失的值。
	 *
	 * @param name      缺失值的名称
	 * @param parameter 方法参数
	 */
	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		String type = parameter.getNestedParameterType().getSimpleName();
		String reason = "Missing request attribute '" + name + "' of type " + type;
		throw new ServerWebInputException(reason, parameter);
	}

}
