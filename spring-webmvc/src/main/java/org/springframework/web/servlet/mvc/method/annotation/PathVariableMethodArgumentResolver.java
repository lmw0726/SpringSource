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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.method.support.UriComponentsContributor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.util.UriComponentsBuilder;

import java.beans.PropertyEditor;
import java.util.HashMap;
import java.util.Map;

/**
 * 解析带有 @{@link PathVariable} 注解的方法参数。
 *
 * <p>@{@link PathVariable}} 是从 URI 模板变量中解析的命名值。它始终是必需的，没有默认值可供回退。
 * 有关如何处理命名值的更多信息，请参见基类 {@link org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver}。
 *
 * <p>如果方法参数类型是 {@link Map}，则使用注解中指定的名称来解析 URI 变量字符串。然后，通过类型转换将值转换为 {@link Map}，
 * 假设已注册了合适的 {@link Converter} 或 {@link PropertyEditor}。
 *
 * <p>可能会调用 {@link WebDataBinder} 来对尚未与方法参数类型匹配的解析的路径变量值进行类型转换。
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.1
 */
public class PathVariableMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver
		implements UriComponentsContributor {
	/**
	 * 字符串类型描述符
	 */
	private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 如果参数上没有@PathVariable注解，则返回false
		if (!parameter.hasParameterAnnotation(PathVariable.class)) {
			return false;
		}
		// 如果参数的嵌套类型是Map或Map的子类
		if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
			// 获取方法参数上的@PathVariable注解
			PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
			// 则检查@PathVariable注解的value属性是否存在
			return (pathVariable != null && StringUtils.hasText(pathVariable.value()));
		}
		// 否则，返回true
		return true;
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		// 获取方法参数上的@PathVariable注解
		PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
		Assert.state(ann != null, "No PathVariable annotation");
		// 使用@PathVariable注解创建 PathVariableNamedValueInfo
		return new PathVariableNamedValueInfo(ann);
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nullable
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
		// 从请求属性中获取URI模板变量的映射关系
		Map<String, String> uriTemplateVars = (Map<String, String>) request.getAttribute(
				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		// 如果URI模板变量映射关系不为空，则根据名称获取对应的值并返回；否则，返回null
		return (uriTemplateVars != null ? uriTemplateVars.get(name) : null);
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
		throw new MissingPathVariableException(name, parameter);
	}

	@Override
	protected void handleMissingValueAfterConversion(
			String name, MethodParameter parameter, NativeWebRequest request) throws Exception {

		throw new MissingPathVariableException(name, parameter, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void handleResolvedValue(@Nullable Object arg, String name, MethodParameter parameter,
									   @Nullable ModelAndViewContainer mavContainer, NativeWebRequest request) {

		// 定义用于存储路径变量的键
		String key = View.PATH_VARIABLES;
		// 定义请求属性的作用域
		int scope = RequestAttributes.SCOPE_REQUEST;
		// 从请求属性中获取路径变量的映射关系
		Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(key, scope);
		// 如果路径变量映射关系为空，则创建一个新的HashMap来存储
		if (pathVars == null) {
			pathVars = new HashMap<>();
			// 将新创建的映射关系存储到请求属性中
			request.setAttribute(key, pathVars, scope);
		}
		// 将参数名和参数值存储到路径变量映射关系中
		pathVars.put(name, arg);
	}

	@Override
	public void contributeMethodArgument(MethodParameter parameter, Object value,
										 UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

		// 如果参数的嵌套类型是Map或Map的子类，则直接返回，不需要处理
		if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
			return;
		}

		// 获取参数上的@PathVariable注解
		PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
		// 如果@PathVariable注解不为空且value属性值非空，则使用value作为参数名，否则使用参数的名称
		String name = (ann != null && StringUtils.hasLength(ann.value()) ? ann.value() : parameter.getParameterName());
		// 格式化参数值
		String formatted = formatUriValue(conversionService, new TypeDescriptor(parameter.nestedIfOptional()), value);
		// 将参数名和格式化后的值存储到uriVariables中
		uriVariables.put(name, formatted);
	}

	@Nullable
	protected String formatUriValue(@Nullable ConversionService cs, @Nullable TypeDescriptor sourceType, Object value) {
		// 如果值是String类型，则直接返回
		if (value instanceof String) {
			return (String) value;
		} else if (cs != null) {
			// 如果转换服务不为空，则使用转换服务进行转换，并返回结果
			return (String) cs.convert(value, sourceType, STRING_TYPE_DESCRIPTOR);
		} else {
			// 否则，将值转换为字符串并返回
			return value.toString();
		}
	}


	private static class PathVariableNamedValueInfo extends NamedValueInfo {

		public PathVariableNamedValueInfo(PathVariable annotation) {
			super(annotation.name(), annotation.required(), ValueConstants.DEFAULT_NONE);
		}
	}

}
