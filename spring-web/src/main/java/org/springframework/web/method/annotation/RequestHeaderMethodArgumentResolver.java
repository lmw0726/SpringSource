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
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Map;

/**
 * 解析使用 {@code @RequestHeader} 注解的方法参数，但不包括 {@link Map} 类型的参数。
 * 有关使用 {@code @RequestHeader} 注解的 {@link Map} 参数的详细信息，请参阅 {@link RequestHeaderMapMethodArgumentResolver}。
 * <p>
 * {@code @RequestHeader} 是从请求头中解析的命名值。它具有一个必需标志和一个默认值，在请求头不存在时可以返回。
 * <p>
 * 将调用 {@link WebDataBinder} 来对尚未与方法参数类型匹配的解析请求头值进行类型转换。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RequestHeaderMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	/**
	 * 创建一个新的 {@link RequestHeaderMethodArgumentResolver} 实例。
	 *
	 * @param beanFactory 用于解析默认值中的 ${...} 占位符和 #{...} SpEL 表达式的 bean 工厂；
	 *                    如果默认值不包含表达式，则为 {@code null}
	 */
	public RequestHeaderMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 检查参数是否带有@RequestHeader注解，并且参数的嵌套类型不是Map或Map的子类
		return (parameter.hasParameterAnnotation(RequestHeader.class) &&
				!Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType()));
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		// 获取方法参数上的@RequestHeader注解
		RequestHeader ann = parameter.getParameterAnnotation(RequestHeader.class);
		Assert.state(ann != null, "No RequestHeader annotation");
		// 使用@RequestHeader注解构建 RequestHeaderNamedValueInfo
		return new RequestHeaderNamedValueInfo(ann);
	}

	@Override
	@Nullable
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
		// 获取请求中指定名称的头信息值数组
		String[] headerValues = request.getHeaderValues(name);
		if (headerValues != null) {
			// 如果头信息值数组不为空，则返回单个值或整个数组
			return (headerValues.length == 1 ? headerValues[0] : headerValues);
		} else {
			// 如果头信息值数组为空，则返回null
			return null;
		}
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
		throw new MissingRequestHeaderException(name, parameter);
	}

	@Override
	protected void handleMissingValueAfterConversion(
			String name, MethodParameter parameter, NativeWebRequest request) throws Exception {

		throw new MissingRequestHeaderException(name, parameter, true);
	}

	private static final class RequestHeaderNamedValueInfo extends NamedValueInfo {

		private RequestHeaderNamedValueInfo(RequestHeader annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
