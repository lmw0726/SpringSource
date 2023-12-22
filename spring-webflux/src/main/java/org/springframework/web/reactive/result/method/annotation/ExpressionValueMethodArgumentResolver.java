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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * 解析带有 {@code @Value} 注解的方法参数。
 *
 * <p>{@code @Value} 没有名称，但可以从默认值字符串中解析出来，该字符串可能包含 ${...} 占位符或 Spring 表达式语言 #{...} 表达式。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ExpressionValueMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	/**
	 * 创建一个新的 {@link ExpressionValueMethodArgumentResolver} 实例。
	 *
	 * @param factory 用于解析默认值中的 {@code ${...}} 占位符和 {@code #{...}} SpEL 表达式的 bean 工厂；
	 *                如果不期望默认值包含表达式，则为 {@code null}
	 * @param registry 用于检查响应式类型包装器的注册表
	 */
	public ExpressionValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory factory,
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
		//带有@Value注解，并且不是嵌套类型
		return checkAnnotatedParamNoReactiveWrapper(param, Value.class, (ann, type) -> true);
	}

	/**
	 * 创建命名值信息。
	 *
	 * @param parameter 方法参数
	 * @return 命名值信息
	 * @throws IllegalStateException 如果不存在 Value 注解，则抛出异常
	 */
	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		Value ann = parameter.getParameterAnnotation(Value.class);
		Assert.state(ann != null, "No Value annotation");
		return new ExpressionValueNamedValueInfo(ann);
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
		// 没有需要解析的名称
		return null;
	}


	/**
	 * 处理缺失的值。
	 *
	 * @param name       缺失值的名称
	 * @param parameter  方法参数
	 */
	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		throw new UnsupportedOperationException("@Value is never required: " + parameter.getMethod());
	}


	/**
	 * 内部类，用于存储表达式值的命名值信息。
	 */
	private static final class ExpressionValueNamedValueInfo extends NamedValueInfo {

		/**
		 * 构造函数，根据注解创建命名值信息。
		 *
		 * @param annotation 表达式值的注解信息
		 */
		private ExpressionValueNamedValueInfo(Value annotation) {
			super("@Value", false, annotation.value());
		}
	}

}
