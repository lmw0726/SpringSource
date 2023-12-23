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

package org.springframework.web.reactive.result.method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * {@code HandlerMethodArgumentResolver}的基类，具有对{@code ReactiveAdapterRegistry}的访问权限以及用于检查方法参数支持的方法。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public abstract class HandlerMethodArgumentResolverSupport implements HandlerMethodArgumentResolver {

	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 响应式适配器注册器
	 */
	private final ReactiveAdapterRegistry adapterRegistry;


	protected HandlerMethodArgumentResolverSupport(ReactiveAdapterRegistry adapterRegistry) {
		Assert.notNull(adapterRegistry, "ReactiveAdapterRegistry is required");
		this.adapterRegistry = adapterRegistry;
	}


	/**
	 * 返回配置的 ReactiveAdapterRegistry。
	 */
	public ReactiveAdapterRegistry getAdapterRegistry() {
		return this.adapterRegistry;
	}


	/**
	 * 评估 Predicate 在方法参数类型上或反应式类型包装器内的泛型类型上的结果。
	 */
	protected boolean checkParameterType(MethodParameter parameter, Predicate<Class<?>> predicate) {
		// 获取参数的类型
		Class<?> type = parameter.getParameterType();

		// 获取适配器
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(type);

		// 如果适配器不为空
		if (adapter != null) {
			// 确保适配器有值
			assertHasValues(adapter, parameter);
			// 获取嵌套参数的类型
			type = parameter.nested().getNestedParameterType();
		}

		// 对类型应用断言条件并返回结果
		return predicate.test(type);
	}


	/**
	 * 确保适配器有值
	 *
	 * @param adapter 适配器
	 * @param param   方法参数
	 */
	private void assertHasValues(ReactiveAdapter adapter, MethodParameter param) {
		// 如果适配器没有值
		if (adapter.isNoValue()) {
			// 抛出非法参数异常，指明不支持没有值的反应式类型，并附带参数的泛型参数类型信息
			throw new IllegalArgumentException(
					"No value reactive types not supported: " + param.getGenericParameterType());
		}
	}


	/**
	 * 在方法参数类型上评估 {@code Predicate}，如果类型匹配响应式类型包装器中的泛型类型，则引发 {@code IllegalStateException}。
	 *
	 * @param parameter 方法参数
	 * @param predicate 类型断言函数
	 * @return 是否满足条件的参数类型
	 */
	protected boolean checkParameterTypeNoReactiveWrapper(MethodParameter parameter, Predicate<Class<?>> predicate) {
		// 获取参数的类型
		Class<?> type = parameter.getParameterType();

		// 获取适配器
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(type);

		// 如果适配器不为空
		if (adapter != null) {
			// 确保适配器有值
			assertHasValues(adapter, parameter);
			// 获取嵌套参数的类型
			type = parameter.nested().getNestedParameterType();
		}

		// 测试嵌套参数是否满足给定的断言条件
		if (predicate.test(type)) {
			// 如果适配器为空，则返回 true
			if (adapter == null) {
				return true;
			}
			// 抛出反应式封装异常
			throw buildReactiveWrapperException(parameter);
		}

		// 否则返回 false
		return false;
	}


	/**
	 * 构建响应式类型包装器异常。
	 *
	 * @param parameter 方法参数
	 * @return 响应式类型包装器异常
	 */
	private IllegalStateException buildReactiveWrapperException(MethodParameter parameter) {
		return new IllegalStateException(getClass().getSimpleName() +
				" does not support reactive type wrapper: " + parameter.getGenericParameterType());
	}

	/**
	 * 在方法参数类型上评估 {@code Predicate}，如果具有给定的注解，则嵌套在 {@link java.util.Optional} 内，
	 * 但如果相同的类型匹配响应式类型包装器中的泛型类型，则引发 {@code IllegalStateException}。
	 *
	 * @param parameter      方法参数
	 * @param annotationType 注解类型
	 * @param typePredicate  类型断言函数
	 * @param <A>            注解类型参数
	 * @return 是否满足条件的参数注解
	 */
	protected <A extends Annotation> boolean checkAnnotatedParamNoReactiveWrapper(
			MethodParameter parameter, Class<A> annotationType, BiPredicate<A, Class<?>> typePredicate) {

		// 获取方法参数上的注解
		A annotation = parameter.getParameterAnnotation(annotationType);
		if (annotation == null) {
			return false;
		}

		// 如果参数是可选的，则进行嵌套处理
		parameter = parameter.nestedIfOptional();
		Class<?> type = parameter.getNestedParameterType();

		// 获取响应式适配器
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(type);
		if (adapter == null) {
			// 检查类型断言函数是否符合条件
			return typePredicate.test(annotation, type);
		}

		// 确保适配器有值
		assertHasValues(adapter, parameter);
		// 继续嵌套处理
		parameter = parameter.nested();
		type = parameter.getNestedParameterType();

		// 检查类型断言函数是否符合条件
		if (typePredicate.test(annotation, type)) {
			// 如果嵌套类型满足条件，抛出异常，提示不支持嵌套类
			throw buildReactiveWrapperException(parameter);
		}

		return false;
	}

}
