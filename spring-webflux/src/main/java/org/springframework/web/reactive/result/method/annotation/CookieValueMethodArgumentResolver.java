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
import org.springframework.http.HttpCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * 解析带有 {@code @CookieValue} 注解的方法参数。
 *
 * <p>{@code @CookieValue} 是从 cookie 中解析出的命名值。
 * 它具有一个必需标志和一个默认值，在 cookie 不存在时可以使用该默认值。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class CookieValueMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	/**
	 * 创建一个新的 {@link CookieValueMethodArgumentResolver} 实例。
	 *
	 * @param factory  用于解析默认值中的 {@code ${...}} 占位符和 {@code #{...}} SpEL 表达式的 bean 工厂；
	 *                 如果不期望默认值包含表达式，则为 {@code null}
	 * @param registry 用于检查反应性类型包装器的注册表
	 */
	public CookieValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory factory,
											 ReactiveAdapterRegistry registry) {

		super(factory, registry);
	}

	/**
	 * 检查是否支持给定的方法参数。
	 *
	 * @param param 方法参数
	 * @return 如果参数被注解为 @CookieValue 并且不是响应式包装器，则返回 true；否则返回 false。
	 */
	@Override
	public boolean supportsParameter(MethodParameter param) {
		// 检查是否存在带有 @CookieValue 注解的参数，并且不是响应式包装器
		return checkAnnotatedParamNoReactiveWrapper(param, CookieValue.class, (annot, type) -> true);
	}

	/**
	 * 创建命名值信息对象，包括名称、必需性和默认值。
	 *
	 * @param parameter 方法参数
	 * @return 命名值信息对象
	 * @throws IllegalStateException 如果没有 CookieValue 注解，则抛出 IllegalStateException 异常
	 */
	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		//获取参数方法上的@CookieValue注解
		CookieValue ann = parameter.getParameterAnnotation(CookieValue.class);
		Assert.state(ann != null, "No CookieValue annotation");
		//构建成 Cookie 名称值信息
		return new CookieValueNamedValueInfo(ann);
	}
	/**
	 * 解析命名值，并根据参数类型返回相应的值。
	 *
	 * @param name 命名值的名称
	 * @param parameter 方法参数
	 * @param exchange 服务器 Web 交换对象
	 * @return 解析后的值，根据参数类型返回对应值
	 */
	@Override
	protected Object resolveNamedValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		// 获取指定名称的 HTTP Cookie
		HttpCookie cookie = exchange.getRequest().getCookies().getFirst(name);
		// 获取参数的嵌套类型
		Class<?> paramType = parameter.getNestedParameterType();
		// 检查参数类型是否是 HttpCookie 类型或其子类
		if (HttpCookie.class.isAssignableFrom(paramType)) {
			// 返回获取的 HTTP Cookie
			return cookie;
		}
		// 如果参数类型不是 HttpCookie 类型或其子类，则返回 HTTP Cookie 的值，如果为 null 则返回 null
		return (cookie != null ? cookie.getValue() : null);
	}

	/**
	 * 处理缺失的命名值情况。
	 *
	 * @param name 命名值的名称
	 * @param parameter 方法参数
	 */
	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		// 获取参数的嵌套类型的简单名称
		String type = parameter.getNestedParameterType().getSimpleName();
		// 构建异常原因字符串
		String reason = "Missing cookie '" + name + "' for method parameter of type " + type;
		// 抛出服务器 Web 输入异常，提供原因和相关参数信息
		throw new ServerWebInputException(reason, parameter);
	}


	/**
	 * {@code CookieValueNamedValueInfo} 是 {@link NamedValueInfo} 的具体实现，用于封装 {@code CookieValue} 注解的信息。
	 */
	private static final class CookieValueNamedValueInfo extends NamedValueInfo {

		private CookieValueNamedValueInfo(CookieValue annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}
}
