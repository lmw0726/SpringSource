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

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebExchange;

import java.util.Collections;
import java.util.Map;

/**
 * 解析带有 @{@link PathVariable} 注解的方法参数。
 *
 * <p>{@link PathVariable} 是从 URI 模板变量解析出来的命名值。它始终是必需的，没有默认值可供回退。
 * 有关命名值处理的更多信息，请参阅基类
 * {@link org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver}。
 *
 * <p>如果方法参数类型为 {@link Map}，则使用注解中指定的名称解析 URI 变量的字符串值。
 * 然后通过类型转换将该值转换为 {@link Map}，假设存在合适的 {@link Converter}。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see PathVariableMapMethodArgumentResolver
 * @since 5.0
 */
public class PathVariableMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	/**
	 * 创建新的 {@link PathVariableMethodArgumentResolver}。
	 *
	 * @param factory  用于解析默认值中的 {@code ${...}} 占位符和 {@code #{...}} SpEL 表达式的 bean 工厂；
	 *                 如果不期望默认值包含表达式，则为 {@code null}
	 * @param registry 用于检查响应式类型包装器的注册表
	 */
	public PathVariableMethodArgumentResolver(@Nullable ConfigurableBeanFactory factory,
											  ReactiveAdapterRegistry registry) {

		super(factory, registry);
	}


	/**
	 * 判断是否支持解析给定的方法参数。
	 *
	 * @param parameter 要检查的方法参数
	 * @return 如果支持解析参数则为 true，否则为 false
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		//带有@PathVariable注解，并且不是注解在嵌套类内，并满足不是Map类型或者@PathVariable注解指定了名称
		return checkAnnotatedParamNoReactiveWrapper(parameter, PathVariable.class, this::singlePathVariable);
	}

	/**
	 * 判断单个路径变量的处理方式。
	 *
	 * @param pathVariable 路径变量注解
	 * @param type         方法参数的类型
	 * @return 如果不是 Map 类型或路径变量注解的名称不为空，则返回 true；否则返回 false
	 */
	private boolean singlePathVariable(PathVariable pathVariable, Class<?> type) {
		//注解所在的类型不是Map类型，或者@PathVariable指定了名称属性。
		return !Map.class.isAssignableFrom(type) || StringUtils.hasText(pathVariable.name());
	}

	/**
	 * 创建命名值信息。
	 *
	 * @param parameter 方法参数
	 * @return 命名值信息
	 * @throws IllegalStateException 如果不存在 PathVariable 注解，则抛出异常
	 */
	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
		Assert.state(ann != null, "No PathVariable annotation");
		return new PathVariableNamedValueInfo(ann);
	}

	/**
	 * 解析命名值。
	 *
	 * @param name       命名值的名称
	 * @param parameter  方法参数
	 * @param exchange   服务器Web交换对象
	 * @return 解析后的值
	 */
	@Override
	protected Object resolveNamedValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		String attributeName = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		return exchange.getAttributeOrDefault(attributeName, Collections.emptyMap()).get(name);
	}

	/**
	 * 处理缺失的命名值。
	 *
	 * @param name      缺失命名值的名称
	 * @param parameter 方法参数
	 * @throws ServerErrorException 抛出服务器错误异常
	 */
	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		throw new ServerErrorException(name, parameter, null);
	}

	/**
	 * 处理已解析的值。
	 *
	 * @param arg        已解析的参数值
	 * @param name       值的名称
	 * @param parameter  方法参数
	 * @param model      模型对象
	 * @param exchange   服务器Web交换对象
	 */
	@Override
	protected void handleResolvedValue(
			@Nullable Object arg, String name, MethodParameter parameter, Model model, ServerWebExchange exchange) {
		// TODO: View.PATH_VARIABLES ?
	}


	/**
	 * 内部类，用于存储路径变量的命名值信息。
	 */
	private static class PathVariableNamedValueInfo extends NamedValueInfo {

		/**
		 * 构造函数，根据注解创建命名值信息。
		 *
		 * @param annotation 路径变量的注解信息
		 */
		public PathVariableNamedValueInfo(PathVariable annotation) {
			super(annotation.name(), annotation.required(), ValueConstants.DEFAULT_NONE);
		}
	}

}
