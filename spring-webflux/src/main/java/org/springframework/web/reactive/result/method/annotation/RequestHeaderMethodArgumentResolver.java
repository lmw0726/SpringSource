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
import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.util.List;
import java.util.Map;

/**
 * 解析使用 {@code @RequestHeader} 注解的方法参数，除了 {@link Map} 类型的参数。
 * 有关使用 {@code @RequestHeader} 注解的 {@link Map} 参数的详细信息，请参阅 {@link RequestHeaderMapMethodArgumentResolver}。
 *
 * <p>{@code @RequestHeader} 是从请求头解析的命名值。它具有一个必需标志和一个默认值，在请求头不存在时可以回退使用。
 *
 * <p>将调用 {@link ConversionService} 来对已解析的请求头值应用类型转换，以匹配方法参数类型。
 *
 * @author Rossen Stoyanchev
 * @see RequestHeaderMapMethodArgumentResolver
 * @since 5.0
 */
public class RequestHeaderMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	/**
	 * 创建一个新的 {@link RequestHeaderMethodArgumentResolver} 实例。
	 *
	 * @param factory  用于解析默认值中的 {@code ${...}} 占位符和 {@code #{...}} SpEL 表达式的 bean 工厂；
	 *                 如果不期望默认值包含表达式，则为 {@code null}
	 * @param registry 用于检查响应式类型包装器的注册表
	 */
	public RequestHeaderMethodArgumentResolver(@Nullable ConfigurableBeanFactory factory,
											   ReactiveAdapterRegistry registry) {

		super(factory, registry);
	}

	/**
	 * 检查方法参数是否支持解析为请求头。
	 *
	 * @param param 方法参数
	 * @return 如果支持解析为请求头则返回 true，否则返回 false
	 */
	@Override
	public boolean supportsParameter(MethodParameter param) {
		//带有@RequestHeader注解，并且不是嵌套类，并且注解的类不是Map类
		return checkAnnotatedParamNoReactiveWrapper(param, RequestHeader.class, this::singleParam);
	}

	/**
	 * 检查单个参数是否能够被解析为请求头。
	 *
	 * @param annotation 参数上的注解
	 * @param type       参数的类型
	 * @return 如果不是 Map 类型，则返回 true，否则返回 false
	 */
	private boolean singleParam(RequestHeader annotation, Class<?> type) {
		return !Map.class.isAssignableFrom(type);
	}

	/**
	 * 创建命名值信息，用于解析请求头的方法参数。
	 *
	 * @param parameter 方法参数
	 * @return 用于解析请求头的命名值信息
	 * @throws IllegalStateException 如果没有 RequestHeader 注解，则抛出异常
	 */
	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestHeader ann = parameter.getParameterAnnotation(RequestHeader.class);
		Assert.state(ann != null, "No RequestHeader annotation");
		return new RequestHeaderNamedValueInfo(ann);
	}

	/**
	 * 解析请求头的命名值。
	 *
	 * @param name      请求头的名称
	 * @param parameter 方法参数
	 * @param exchange  服务器端 Web 交换对象
	 * @return 解析的请求头值；如果存在多个值，则返回列表，否则返回单个值
	 */
	@Override
	protected Object resolveNamedValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		// 获取请求中指定名称的请求头值列表
		List<String> headerValues = exchange.getRequest().getHeaders().get(name);

		Object result = null;
		if (headerValues != null) {
			// 如果存在值，则根据值的数量返回单个值或值的列表
			result = (headerValues.size() == 1 ? headerValues.get(0) : headerValues);
		}
		return result;
	}

	/**
	 * 处理缺失的请求头值。
	 *
	 * @param name      缺失的请求头名称
	 * @param parameter 方法参数
	 * @throws ServerWebInputException 如果缺少请求头值，则抛出异常
	 */
	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		String type = parameter.getNestedParameterType().getSimpleName();
		throw new ServerWebInputException("Missing request header '" + name + "' " +
				"for method parameter of type " + type, parameter);
	}

	/**
	 * {@link RequestHeader} 的命名值信息类。
	 */
	private static final class RequestHeaderNamedValueInfo extends NamedValueInfo {

		/**
		 * 使用 {@link RequestHeader} 注解初始化命名值信息对象。
		 *
		 * @param annotation {@link RequestHeader} 注解实例
		 */
		private RequestHeaderNamedValueInfo(RequestHeader annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
