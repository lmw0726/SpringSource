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

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从命名值中解析方法参数的抽象基类。
 * 请求参数、请求头和路径变量是命名值的示例。每个命名值可能具有名称、必需标志和默认值。
 *
 * <p>子类定义以下内容的处理方式：
 * <ul>
 * <li>获取方法参数的命名值信息
 * <li>将名称解析为参数值
 * <li>在需要参数值但参数值缺失时处理缺失的参数值
 * <li>可选择处理已解析的值
 * </ul>
 *
 * <p>默认值字符串可以包含 ${...} 占位符和 Spring 表达式语言 #{...} 表达式。为了使其工作，类构造函数必须提供 {@link ConfigurableBeanFactory}。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractNamedValueArgumentResolver extends HandlerMethodArgumentResolverSupport {

	@Nullable
	private final ConfigurableBeanFactory configurableBeanFactory;

	@Nullable
	private final BeanExpressionContext expressionContext;

	private final Map<MethodParameter, NamedValueInfo> namedValueInfoCache = new ConcurrentHashMap<>(256);


	/**
	 * 创建一个新的 {@link AbstractNamedValueArgumentResolver} 实例。
	 *
	 * @param factory  用于解析默认值中的 {@code ${...}} 占位符和 {@code #{...}} SpEL 表达式的 Bean 工厂，
	 *                 如果不期望默认值包含表达式，则为 {@code null}
	 * @param registry 用于检查响应式类型包装器的注册表
	 */
	public AbstractNamedValueArgumentResolver(@Nullable ConfigurableBeanFactory factory,
											  ReactiveAdapterRegistry registry) {
		super(registry);
		this.configurableBeanFactory = factory;
		this.expressionContext = (factory != null ? new BeanExpressionContext(factory, null) : null);
	}

	/**
	 * 解析参数并返回处理后的结果。
	 *
	 * @param parameter      方法参数
	 * @param bindingContext 绑定上下文
	 * @param exchange       当前的服务器Web交换
	 * @return 解析后的参数结果
	 */
	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {

		// 获取命名值信息
		NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
		// 获取嵌套的方法参数
		MethodParameter nestedParameter = parameter.nestedIfOptional();

		// 解析嵌入的值和表达式
		Object resolvedName = resolveEmbeddedValuesAndExpressions(namedValueInfo.name);
		if (resolvedName == null) {
			return Mono.error(new IllegalArgumentException(
					"Specified name must not resolve to null: [" + namedValueInfo.name + "]"));
		}

		// 获取 BindingContext 中的 Model
		Model model = bindingContext.getModel();

		return resolveName(resolvedName.toString(), nestedParameter, exchange)
				.flatMap(arg -> {
					if ("".equals(arg) && namedValueInfo.defaultValue != null) {
						arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
					}
					// 应用转换
					arg = applyConversion(arg, namedValueInfo, parameter, bindingContext, exchange);
					// 处理已解析的值
					handleResolvedValue(arg, namedValueInfo.name, parameter, model, exchange);
					return Mono.justOrEmpty(arg);
				})
				// 如果为空，则获取默认值
				.switchIfEmpty(getDefaultValue(
						namedValueInfo, parameter, bindingContext, model, exchange));
	}


	/**
	 * 获取给定方法参数的命名值信息。
	 *
	 * @param parameter 方法参数
	 * @return 方法参数的命名值信息
	 */
	private NamedValueInfo getNamedValueInfo(MethodParameter parameter) {
		NamedValueInfo namedValueInfo = this.namedValueInfoCache.get(parameter);
		if (namedValueInfo == null) {
			// 创建命名值信息
			namedValueInfo = createNamedValueInfo(parameter);
			// 更新命名值信息
			namedValueInfo = updateNamedValueInfo(parameter, namedValueInfo);
			//添加进缓存
			this.namedValueInfoCache.put(parameter, namedValueInfo);
		}
		return namedValueInfo;
	}

	/**
	 * 为给定的方法参数创建 {@link NamedValueInfo} 对象。
	 * 实现通常通过 {@link MethodParameter#getParameterAnnotation(Class)} 方法获取方法注解。
	 *
	 * @param parameter 方法参数
	 * @return 命名值信息
	 */
	protected abstract NamedValueInfo createNamedValueInfo(MethodParameter parameter);

	/**
	 * 基于给定的 NamedValueInfo 创建一个新的 NamedValueInfo，并清理值。
	 *
	 * @param parameter 方法参数
	 * @param info      原始的命名值信息
	 * @return 更新后的命名值信息
	 */
	private NamedValueInfo updateNamedValueInfo(MethodParameter parameter, NamedValueInfo info) {
		String name = info.name;
		if (info.name.isEmpty()) {
			//如果命名值中的名称为空，则该名称为参数名称
			name = parameter.getParameterName();
			if (name == null) {
				//如果参数名称为空，则抛出异常。
				throw new IllegalArgumentException(
						"Name for argument of type [" + parameter.getNestedParameterType().getName() +
								"] not specified, and parameter name information not found in class file either.");
			}
		}
		// 获取默认值
		String defaultValue = (ValueConstants.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);
		return new NamedValueInfo(name, info.required, defaultValue);
	}

	/**
	 * 解析给定的注解指定值，可能包含占位符和表达式。
	 *
	 * @param value 注解指定的值
	 * @return 解析后的值
	 */
	@Nullable
	private Object resolveEmbeddedValuesAndExpressions(String value) {
		// 如果配置的可配置Bean工厂为null或者表达式上下文为null，则直接返回原始数值。
		if (this.configurableBeanFactory == null || this.expressionContext == null) {
			return value;
		}

		// 解析占位符，获取解析后的数值。
		String placeholdersResolved = this.configurableBeanFactory.resolveEmbeddedValue(value);

		// 获取可配置Bean工厂的表达式解析器。
		BeanExpressionResolver exprResolver = this.configurableBeanFactory.getBeanExpressionResolver();

		// 如果表达式解析器为null，则直接返回解析后的数值。
		if (exprResolver == null) {
			return value;
		}

		// 使用表达式解析器对解析后的数值进行表达式求值，并返回结果。
		return exprResolver.evaluate(placeholdersResolved, this.expressionContext);
	}

	/**
	 * 解析给定的参数类型和值名称为参数值。
	 *
	 * @param name      要解析的值的名称
	 * @param parameter 要解析为参数值的方法参数
	 *                  （在 {@link java.util.Optional} 声明的情况下预先嵌套）
	 * @param exchange  当前的交换对象
	 * @return 解析后的参数（可能为空的 {@link Mono}）
	 */
	protected abstract Mono<Object> resolveName(String name, MethodParameter parameter, ServerWebExchange exchange);

	/**
	 * 如果需要，应用类型转换。
	 *
	 * @param value          待转换的值
	 * @param namedValueInfo 命名值信息
	 * @param parameter      方法参数
	 * @param bindingContext 绑定上下文
	 * @param exchange       服务器Web交换对象
	 * @return 应用转换后的值
	 */
	@Nullable
	private Object applyConversion(@Nullable Object value, NamedValueInfo namedValueInfo, MethodParameter parameter,
								   BindingContext bindingContext, ServerWebExchange exchange) {

		// 创建数据绑定器
		WebDataBinder binder = bindingContext.createDataBinder(exchange, namedValueInfo.name);
		try {
			// 如果需要，对值进行类型转换
			value = binder.convertIfNecessary(value, parameter.getParameterType(), parameter);
		} catch (ConversionNotSupportedException ex) {
			// 如果转换不支持，抛出服务器错误异常
			throw new ServerErrorException("Conversion not supported.", parameter, ex);
		} catch (TypeMismatchException ex) {
			// 如果类型不匹配，抛出服务器Web输入异常
			throw new ServerWebInputException("Type mismatch.", parameter, ex);
		}
		return value;
	}

	/**
	 * 解析默认值（如果有的话）。
	 *
	 * @param namedValueInfo 命名值信息
	 * @param parameter      方法参数
	 * @param bindingContext 绑定上下文
	 * @param model          数据模型
	 * @param exchange       服务器Web交换对象
	 * @return 默认值解析后的 {@link Mono}
	 */
	private Mono<Object> getDefaultValue(NamedValueInfo namedValueInfo, MethodParameter parameter,
										 BindingContext bindingContext, Model model, ServerWebExchange exchange) {

		return Mono.fromSupplier(() -> {
			Object value = null;

			// 如果默认值不为null，解析嵌入值和表达式
			if (namedValueInfo.defaultValue != null) {
				value = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
			} else if (namedValueInfo.required && !parameter.isOptional()) {
				// 如果值必填且参数不是可选的，则处理缺失的值
				handleMissingValue(namedValueInfo.name, parameter, exchange);
			}

			// 处理空值
			value = handleNullValue(namedValueInfo.name, value, parameter.getNestedParameterType());

			// 应用类型转换
			value = applyConversion(value, namedValueInfo, parameter, bindingContext, exchange);

			// 处理已解析的值
			handleResolvedValue(value, namedValueInfo.name, parameter, model, exchange);

			return value;
		});
	}

	/**
	 * 当需要命名值但是 {@link #resolveName(String, MethodParameter, ServerWebExchange)} 返回
	 * {@code null} 并且没有默认值时被调用。在这种情况下，子类通常会抛出异常。
	 *
	 * @param name      值的名称
	 * @param parameter 方法参数
	 * @param exchange  当前的交换对象
	 */
	@SuppressWarnings("UnusedParameters")
	protected void handleMissingValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		handleMissingValue(name, parameter);
	}

	/**
	 * 当需要命名值但是 {@link #resolveName(String, MethodParameter, ServerWebExchange)} 返回
	 * {@code null} 并且没有默认值时被调用。在这种情况下，子类通常会抛出异常。
	 *
	 * @param name      值的名称
	 * @param parameter 方法参数
	 */
	protected void handleMissingValue(String name, MethodParameter parameter) {
		String typeName = parameter.getNestedParameterType().getSimpleName();
		throw new ServerWebInputException("Missing argument '" + name + "' for method " +
				"parameter of type " + typeName, parameter);
	}

	/**
	 * 对于 {@code boolean} 类型的值，{@code null} 将导致返回 {@code false}；
	 * 对于其他原始类型，将导致抛出异常。
	 *
	 * @param name      值的名称
	 * @param value     待处理的值
	 * @param paramType 参数类型
	 * @return 处理后的值
	 */
	@Nullable
	private Object handleNullValue(String name, @Nullable Object value, Class<?> paramType) {
		if (value == null) {
			if (Boolean.TYPE.equals(paramType)) {
				return Boolean.FALSE;
			} else if (paramType.isPrimitive()) {
				throw new IllegalStateException("Optional " + paramType.getSimpleName() +
						" parameter '" + name + "' is present but cannot be translated into a" +
						" null value due to being declared as a primitive type. " +
						"Consider declaring it as object wrapper for the corresponding primitive type.");
			}
		}
		return value;
	}

	/**
	 * 在值解析后被调用。
	 *
	 * @param arg       已解析的参数值
	 * @param name      参数名称
	 * @param parameter 参数类型
	 * @param model     数据模型
	 * @param exchange  当前的交换对象
	 */
	@SuppressWarnings("UnusedParameters")
	protected void handleResolvedValue(
			@Nullable Object arg, String name, MethodParameter parameter, Model model, ServerWebExchange exchange) {
	}

	/**
	 * 表示有关命名值的信息，包括名称、是否必需以及默认值。
	 */
	protected static class NamedValueInfo {
		/**
		 * 名称
		 */
		private final String name;

		/**
		 * 是否必要
		 */
		private final boolean required;

		/**
		 * 默认值
		 */
		@Nullable
		private final String defaultValue;

		public NamedValueInfo(String name, boolean required, @Nullable String defaultValue) {
			this.name = name;
			this.required = required;
			this.defaultValue = defaultValue;
		}
	}

}
