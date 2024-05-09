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

package org.springframework.web.method.annotation;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 一个用于解析带有 {@code @CookieValue} 注解的方法参数的基础抽象类。子类从请求中提取 cookie 值。
 *
 * <p>{@code @CookieValue} 是一个命名值，从 cookie 中解析。它具有一个 required 标志和一个默认值，
 * 当 cookie 不存在时可以回退到默认值。
 *
 * <p>可能会调用 {@link WebDataBinder} 来对解析的 cookie 值应用类型转换。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractCookieValueMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	/**
	 * 创建一个新的 {@link AbstractCookieValueMethodArgumentResolver} 实例。
	 *
	 * @param beanFactory 用于解析默认值中的 ${...} 占位符和 #{...} SpEL 表达式的 Bean 工厂；
	 *                    如果不希望默认值包含表达式，则为 {@code null}。
	 */
	public AbstractCookieValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 检查是否含有@CookieValue注解
		return parameter.hasParameterAnnotation(CookieValue.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		// 获取方法参数上的@CookieValue注解
		CookieValue annotation = parameter.getParameterAnnotation(CookieValue.class);
		Assert.state(annotation != null, "No CookieValue annotation");
		// 构建 CookieValueNamedValueInfo
		return new CookieValueNamedValueInfo(annotation);
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
		throw new MissingRequestCookieException(name, parameter);
	}

	@Override
	protected void handleMissingValueAfterConversion(
			String name, MethodParameter parameter, NativeWebRequest request) throws Exception {

		throw new MissingRequestCookieException(name, parameter, true);
	}

	private static final class CookieValueNamedValueInfo extends NamedValueInfo {

		private CookieValueNamedValueInfo(CookieValue annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
