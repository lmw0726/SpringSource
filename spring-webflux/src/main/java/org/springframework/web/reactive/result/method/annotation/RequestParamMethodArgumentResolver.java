/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.util.List;
import java.util.Map;

/**
 * 用于解析带有 @{@link RequestParam} 注解的方法参数，从 URI 查询字符串参数中获取。
 *
 * <p>此解析器还可以在默认解析模式下创建，在此模式下，未使用 @{@link RequestParam} 注解的简单类型（int、long 等）
 * 也被视为请求参数，参数名称派生自参数名称。
 *
 * <p>如果方法参数类型为 {@link Map}，则使用注解中指定的名称来解析请求参数字符串值。
 * 然后通过类型转换将其转换为 {@link Map}，假设已注册了合适的 {@link Converter}。
 * 或者，如果未指定请求参数名称，则使用 {@link RequestParamMapMethodArgumentResolver} 来提供以 map 形式访问所有请求参数。
 *
 * @author Rossen Stoyanchev
 * @see RequestParamMapMethodArgumentResolver
 * @since 5.0
 */
public class RequestParamMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	private final boolean useDefaultResolution;

	/**
	 * 构造函数，带有默认解析模式标志。
	 *
	 * @param factory              用于解析默认值中的 ${...} 占位符和 #{...} SpEL 表达式的 bean 工厂；
	 *                             如果不期望默认值包含表达式，则为 {@code null}
	 * @param registry             用于检查响应式类型包装器的注册表
	 * @param useDefaultResolution 在默认解析模式下，作为简单类型的方法参数（如 {@link BeanUtils#isSimpleProperty} 中定义的）
	 *                             即使未注解，也会被视为请求参数，请求参数名称派生自方法参数名称。
	 */
	public RequestParamMethodArgumentResolver(
			@Nullable ConfigurableBeanFactory factory, ReactiveAdapterRegistry registry, boolean useDefaultResolution) {

		super(factory, registry);
		this.useDefaultResolution = useDefaultResolution;
	}

	/**
	 * 检查是否支持给定的方法参数。
	 *
	 * @param param 方法参数
	 * @return 如果参数被注解为 @RequestParam 并且不是响应式包装器，则返回 true；
	 * 否则如果在默认解析模式下，参数是简单类型或者可选参数的嵌套类型是简单类型，则返回 true；
	 * 其他情况返回 false。
	 */
	@Override
	public boolean supportsParameter(MethodParameter param) {
		if (checkAnnotatedParamNoReactiveWrapper(param, RequestParam.class, this::singleParam)) {
			// 如果参数被注解为 @RequestParam 并且满足 singleParam 函数的条件，则返回 true
			return true;
		} else if (this.useDefaultResolution) {
			// 在默认解析模式下，如果参数类型是简单类型或者可选参数的嵌套类型是简单类型，则返回 true
			return checkParameterTypeNoReactiveWrapper(param, BeanUtils::isSimpleProperty) ||
					BeanUtils.isSimpleProperty(param.nestedIfOptional().getNestedParameterType());
		}
		// 其他情况返回 false
		return false;
	}

	/**
	 * 方法参数是单个参数
	 *
	 * @param requestParam 请求参数
	 * @param type         RequestParam类型
	 * @return true表示该解析器支持解析方法参数
	 */
	private boolean singleParam(RequestParam requestParam, Class<?> type) {
		// 如果类型不是 Map 类型，或者 @RequestParam 注解中指定了名称，则返回 true
		return !Map.class.isAssignableFrom(type) || StringUtils.hasText(requestParam.name());
	}

	/**
	 * 创建命名值信息对象，包括名称、是否必需以及默认值。
	 *
	 * @param parameter 方法参数
	 * @return 命名值信息对象
	 */
	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		// 获取参数上的 @RequestParam 注解
		RequestParam ann = parameter.getParameterAnnotation(RequestParam.class);
		// 如果注解不为 null，则创建一个 RequestParamNamedValueInfo 对象；否则创建一个默认的 RequestParamNamedValueInfo 对象
		return (ann != null ? new RequestParamNamedValueInfo(ann) : new RequestParamNamedValueInfo());
	}

	/**
	 * 解析命名值，并返回相应的值。
	 *
	 * @param name      命名值的名称
	 * @param parameter 方法参数
	 * @param exchange  服务器 Web 交换对象
	 * @return 解析后的值
	 */
	@Override
	protected Object resolveNamedValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		// 获取请求中指定名称的参数值列表
		List<String> paramValues = exchange.getRequest().getQueryParams().get(name);
		Object result = null;
		if (paramValues != null) {
			// 如果参数值列表不为 null，则根据参数值数量返回单个值或者整个列表
			result = (paramValues.size() == 1 ? paramValues.get(0) : paramValues);
		}
		return result;
	}

	/**
	 * 处理缺失的命名值情况。
	 *
	 * @param name      命名值的名称
	 * @param parameter 方法参数
	 * @param exchange  服务器 Web 交换对象
	 */
	@Override
	protected void handleMissingValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		// 获取参数的嵌套类型的简单名称
		String type = parameter.getNestedParameterType().getSimpleName();
		// 构建异常原因字符串
		String reason = "Required " + type + " parameter '" + name + "' is not present";
		// 抛出服务器 Web 输入异常，提供原因和相关参数信息
		throw new ServerWebInputException(reason, parameter);
	}


	/**
	 * {@code RequestParamNamedValueInfo} 是 {@link NamedValueInfo} 的具体实现，
	 * 用于封装 {@code RequestParam} 注解的信息。
	 */
	private static class RequestParamNamedValueInfo extends NamedValueInfo {

		/**
		 * 默认构造函数，创建一个未命名、非必需且没有默认值的命名值信息对象。
		 */
		RequestParamNamedValueInfo() {
			super("", false, ValueConstants.DEFAULT_NONE);
		}

		/**
		 * 带有 {@code RequestParam} 注解的构造函数，根据注解信息创建命名值信息对象。
		 *
		 * @param annotation {@code RequestParam} 注解
		 */
		RequestParamNamedValueInfo(RequestParam annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
