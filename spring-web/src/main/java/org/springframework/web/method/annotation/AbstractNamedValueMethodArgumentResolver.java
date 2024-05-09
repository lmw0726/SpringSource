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

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.ServletException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从命名值解析方法参数的抽象基类。请求参数、请求头和路径变量是命名值的示例。
 * 每个命名值可能具有名称、必需标志和默认值。
 *
 * <p>子类定义如何执行以下操作：
 * <ul>
 * <li>获取方法参数的命名值信息
 * <li>将名称解析为参数值
 * <li>在需要参数值但缺少参数值时处理
 * <li>可选地处理解析的值
 * </ul>
 *
 * <p>默认值字符串可以包含 ${...} 占位符和 Spring 表达式语言 #{...} 表达式。
 * 为了使其工作，必须向类构造函数提供一个
 * {@link ConfigurableBeanFactory}。
 *
 * <p>如果解析的参数值与方法参数类型不匹配，则创建一个 {@link WebDataBinder} 来对其进行类型转换。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractNamedValueMethodArgumentResolver implements HandlerMethodArgumentResolver {
	/**
	 * 可配置的Bean工厂
	 */
	@Nullable
	private final ConfigurableBeanFactory configurableBeanFactory;

	/**
	 * Bean表达式上下文
	 */
	@Nullable
	private final BeanExpressionContext expressionContext;

	/**
	 * 方法参数 - 名称值信息映射
	 */
	private final Map<MethodParameter, NamedValueInfo> namedValueInfoCache = new ConcurrentHashMap<>(256);


	public AbstractNamedValueMethodArgumentResolver() {
		this.configurableBeanFactory = null;
		this.expressionContext = null;
	}

	/**
	 * 创建一个新的 {@link AbstractNamedValueMethodArgumentResolver} 实例。
	 *
	 * @param beanFactory 用于解析默认值中的 ${...} 占位符和 #{...} SpEL 表达式的 bean 工厂，如果不期望默认值包含表达式，则为 {@code null}
	 */
	public AbstractNamedValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
		// 设置可配置的Bean工厂
		this.configurableBeanFactory = beanFactory;
		// 如果Bean工厂不为空，则创建一个新的Bean表达式上下文
		this.expressionContext =
				(beanFactory != null ? new BeanExpressionContext(beanFactory, new RequestScope()) : null);
	}


	@Override
	@Nullable
	public final Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
										NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		// 获取参数的命名值信息
		NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
		// 如果参数是Optional类型，则获取其嵌套的参数
		MethodParameter nestedParameter = parameter.nestedIfOptional();

		// 解析嵌入式值和表达式，并确保解析后的名称不为空
		Object resolvedName = resolveEmbeddedValuesAndExpressions(namedValueInfo.name);
		if (resolvedName == null) {
			// 如果解析后的名称为空，则抛出异常
			throw new IllegalArgumentException(
					"Specified name must not resolve to null: [" + namedValueInfo.name + "]");
		}

		// 解析参数值
		Object arg = resolveName(resolvedName.toString(), nestedParameter, webRequest);
		if (arg == null) {
			// 如果参数值为空
			if (namedValueInfo.defaultValue != null) {
				// 如果存在默认值，则使用默认值
				arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
			} else if (namedValueInfo.required && !nestedParameter.isOptional()) {
				// 如果参数为必填项且不是Optional类型，则处理缺失值
				handleMissingValue(namedValueInfo.name, nestedParameter, webRequest);
			}
			// 处理空值
			arg = handleNullValue(namedValueInfo.name, arg, nestedParameter.getNestedParameterType());
		} else if ("".equals(arg) && namedValueInfo.defaultValue != null) {
			// 如果参数值为空字符串且存在默认值，则使用默认值
			arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
		}

		// 如果存在绑定工厂
		if (binderFactory != null) {
			// 创建WebDataBinder
			WebDataBinder binder = binderFactory.createBinder(webRequest, null, namedValueInfo.name);
			try {
				// 转换参数值
				arg = binder.convertIfNecessary(arg, parameter.getParameterType(), parameter);
			} catch (ConversionNotSupportedException ex) {
				// 处理转换不支持的异常
				throw new MethodArgumentConversionNotSupportedException(arg, ex.getRequiredType(),
						namedValueInfo.name, parameter, ex.getCause());
			} catch (TypeMismatchException ex) {
				// 处理类型不匹配的异常
				throw new MethodArgumentTypeMismatchException(arg, ex.getRequiredType(),
						namedValueInfo.name, parameter, ex.getCause());
			}
			// 在参数值转换后检查空值
			if (arg == null && namedValueInfo.defaultValue == null &&
					namedValueInfo.required && !nestedParameter.isOptional()) {
				// 如果参数值为空，默认值为空，并且值是必须的，且嵌套参数不是Optional类型，处理转换后的缺失值
				handleMissingValueAfterConversion(namedValueInfo.name, nestedParameter, webRequest);
			}
		}

		// 处理已解析的参数值
		handleResolvedValue(arg, namedValueInfo.name, parameter, mavContainer, webRequest);

		// 返回参数值
		return arg;
	}

	/**
	 * 获取给定方法参数的命名值信息。
	 */
	private NamedValueInfo getNamedValueInfo(MethodParameter parameter) {
		// 从缓存中获取参数的命名值信息
		NamedValueInfo namedValueInfo = this.namedValueInfoCache.get(parameter);
		if (namedValueInfo == null) {
			// 如果命名值信息为空，则创建并更新命名值信息，并将其放入缓存中
			namedValueInfo = createNamedValueInfo(parameter);
			namedValueInfo = updateNamedValueInfo(parameter, namedValueInfo);
			this.namedValueInfoCache.put(parameter, namedValueInfo);
		}
		// 返回命名值信息
		return namedValueInfo;
	}

	/**
	 * 为给定的方法参数创建 {@link NamedValueInfo} 对象。实现通常通过 {@link MethodParameter#getParameterAnnotation(Class)} 获取方法注解。
	 *
	 * @param parameter 方法参数
	 * @return 命名值信息
	 */
	protected abstract NamedValueInfo createNamedValueInfo(MethodParameter parameter);

	/**
	 * 根据给定的 NamedValueInfo 对象创建一个新的 NamedValueInfo，其中包含经过清理的值。
	 */
	private NamedValueInfo updateNamedValueInfo(MethodParameter parameter, NamedValueInfo info) {
		String name = info.name;
		// 如果命名值为空，则尝试获取参数的名称
		if (info.name.isEmpty()) {
			// 获取方法参数名称
			name = parameter.getParameterName();
			// 如果参数名称为空，则抛出异常
			if (name == null) {
				throw new IllegalArgumentException(
						"Name for argument of type [" + parameter.getNestedParameterType().getName() +
								"] not specified, and parameter name information not found in class file either.");
			}
		}
		// 处理默认值
		String defaultValue = (ValueConstants.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);
		// 返回新的命名值信息
		return new NamedValueInfo(name, info.required, defaultValue);
	}

	/**
	 * 解析给定的注解指定的值，可能包含占位符和表达式。
	 */
	@Nullable
	private Object resolveEmbeddedValuesAndExpressions(String value) {
		if (this.configurableBeanFactory == null || this.expressionContext == null) {
			// 如果可配置的Bean工厂或表达式上下文为空，则直接返回值
			return value;
		}
		// 解析值中的占位符
		String placeholdersResolved = this.configurableBeanFactory.resolveEmbeddedValue(value);
		// 获取Bean表达式解析器
		BeanExpressionResolver exprResolver = this.configurableBeanFactory.getBeanExpressionResolver();
		// 如果解析器为空，则直接返回值
		if (exprResolver == null) {
			return value;
		}
		// 使用表达式解析器对解析后的值进行求值
		return exprResolver.evaluate(placeholdersResolved, this.expressionContext);
	}

	/**
	 * 将给定的参数类型和值名称解析为参数值。
	 *
	 * @param name      要解析的值的名称
	 * @param parameter 要解析为参数值的方法参数（在声明为 {@link java.util.Optional} 时预先嵌套）
	 * @param request   当前请求
	 * @return 已解析的参数（可能为 {@code null}）
	 * @throws Exception 解析过程中出现的异常
	 */
	@Nullable
	protected abstract Object resolveName(String name, MethodParameter parameter, NativeWebRequest request)
			throws Exception;

	/**
	 * 当需要命名值但 {@link #resolveName(String, MethodParameter, NativeWebRequest)} 返回 {@code null}
	 * 且没有默认值时调用。子类通常在此情况下抛出异常。
	 *
	 * @param name      值的名称
	 * @param parameter 方法参数
	 * @param request   当前请求
	 * @since 4.3
	 */
	protected void handleMissingValue(String name, MethodParameter parameter, NativeWebRequest request)
			throws Exception {

		handleMissingValue(name, parameter);
	}

	/**
	 * 当需要命名值但 {@link #resolveName(String, MethodParameter, NativeWebRequest)} 返回 {@code null}
	 * 且没有默认值时调用。子类通常在此情况下抛出异常。
	 *
	 * @param name      值的名称
	 * @param parameter 方法参数
	 */
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
		throw new ServletRequestBindingException("Missing argument '" + name +
				"' for method parameter of type " + parameter.getNestedParameterType().getSimpleName());
	}

	/**
	 * 当命名值存在但在转换后变为 {@code null} 时调用。
	 *
	 * @param name      值的名称
	 * @param parameter 方法参数
	 * @param request   当前请求
	 * @since 5.3.6
	 */
	protected void handleMissingValueAfterConversion(String name, MethodParameter parameter, NativeWebRequest request)
			throws Exception {

		handleMissingValue(name, parameter, request);
	}

	/**
	 * {@code null} 值对于 {@code boolean} 类型返回 {@code false}，对于其他原始类型引发异常。
	 */
	@Nullable
	private Object handleNullValue(String name, @Nullable Object value, Class<?> paramType) {
		// 如果值为null
		if (value == null) {
			// 如果参数类型是boolean类型，则返回Boolean.FALSE
			if (Boolean.TYPE.equals(paramType)) {
				return Boolean.FALSE;
			} else if (paramType.isPrimitive()) {
				// 如果参数类型是基本类型，则抛出异常
				throw new IllegalStateException("Optional " + paramType.getSimpleName() + " parameter '" + name +
						"' is present but cannot be translated into a null value due to being declared as a " +
						"primitive type. Consider declaring it as object wrapper for the corresponding primitive type.");
			}
		}
		// 返回值
		return value;
	}

	/**
	 * 在值解析后调用。
	 *
	 * @param arg          已解析的参数值
	 * @param name         参数名称
	 * @param parameter    参数类型
	 * @param mavContainer ModelAndViewContainer（可能为 {@code null}）
	 * @param webRequest   当前请求
	 */
	protected void handleResolvedValue(@Nullable Object arg, String name, MethodParameter parameter,
									   @Nullable ModelAndViewContainer mavContainer, NativeWebRequest webRequest) {
	}


	/**
	 * 表示有关命名值的信息，包括名称、是否必需和默认值。
	 */
	protected static class NamedValueInfo {
		/**
		 * 名称
		 */
		private final String name;

		/**
		 * 是否必须
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
