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

package org.springframework.web.method.annotation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.ServletException;

/**
 * 解析带有 {@code @Value} 注解的方法参数。
 *
 * <p>{@code @Value} 没有名称，但从默认值字符串中解析，该字符串可能包含 ${...} 占位符或 Spring 表达式
 * 语言 #{...} 表达式。
 *
 * <p>可能会调用 {@link WebDataBinder} 来对解析的参数值进行类型转换。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ExpressionValueMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	/**
	 * 创建一个新的 {@link ExpressionValueMethodArgumentResolver} 实例。
	 *
	 * @param beanFactory 用于解析默认值中的 ${...} 占位符和 #{...} SpEL 表达式的 bean 工厂；
	 *                    或 {@code null}，如果不期望默认值包含表达式
	 */
	public ExpressionValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 检查方法参数上是否有@Value注解
		return parameter.hasParameterAnnotation(Value.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		// 获取方法参数上的@Value注解
		Value annotation = parameter.getParameterAnnotation(Value.class);
		Assert.state(annotation != null, "No Value annotation");
		// 使用@Value注解创建 ExpressionValueNamedValueInfo
		return new ExpressionValueNamedValueInfo(annotation);
	}

	@Override
	@Nullable
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest webRequest) throws Exception {
		// 没有要解析的名称
		return null;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
		throw new UnsupportedOperationException("@Value is never required: " + parameter.getMethod());
	}


	private static final class ExpressionValueNamedValueInfo extends NamedValueInfo {

		private ExpressionValueNamedValueInfo(Value annotation) {
			super("@Value", false, annotation.value());
		}
	}

}
