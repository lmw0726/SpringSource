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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.ServletRequest;
import java.util.Collections;
import java.util.Map;

/**
 * 适用于 Servlet 的 {@link ModelAttributeMethodProcessor}，通过 {@link ServletRequestDataBinder} 类型的 WebDataBinder 应用数据绑定。
 *
 * <p>还添加了一种回退策略，如果名称匹配模型属性名称并且存在适当的类型转换策略，则可以从 URI 模板变量或请求参数实例化模型属性。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ServletModelAttributeMethodProcessor extends ModelAttributeMethodProcessor {

	/**
	 * 类构造函数。
	 *
	 * @param annotationNotRequired 如果为 "true"，则非简单方法参数和返回值被视为模型属性，无论是否带有 {@code @ModelAttribute} 注解
	 */
	public ServletModelAttributeMethodProcessor(boolean annotationNotRequired) {
		super(annotationNotRequired);
	}


	/**
	 * 如果从请求中找到的值可以用于通过 String 转换为目标类型实例化模型属性，则从 URI 模板变量或请求参数实例化模型属性。如果都不满足，则委托给基类。
	 *
	 * @see #createAttributeFromRequestValue
	 */
	@Override
	protected final Object createAttribute(String attributeName, MethodParameter parameter,
										   WebDataBinderFactory binderFactory, NativeWebRequest request) throws Exception {

		// 获取请求中与属性名称对应的值
		String value = getRequestValueForAttribute(attributeName, request);
		// 如果值不为空
		if (value != null) {
			// 从请求值创建属性
			Object attribute = createAttributeFromRequestValue(
					value, attributeName, parameter, binderFactory, request);
			// 如果属性不为空，则返回该属性
			if (attribute != null) {
				return attribute;
			}
		}
		// 否则，调用父类的方法创建属性
		return super.createAttribute(attributeName, parameter, binderFactory, request);
	}

	/**
	 * 获取可能用于通过类型转换从 String 转换为目标类型实例化模型属性的请求值。
	 * <p>默认实现首先查找属性名称匹配 URI 变量，然后查找请求参数。
	 *
	 * @param attributeName 模型属性名称
	 * @param request       当前请求
	 * @return 要尝试转换的请求值；如果没有，则为 {@code null}
	 */
	@Nullable
	protected String getRequestValueForAttribute(String attributeName, NativeWebRequest request) {
		// 获取URI模板变量
		Map<String, String> variables = getUriTemplateVariables(request);
		// 获取属性名称对应的变量值
		String variableValue = variables.get(attributeName);
		// 如果变量值不为空且有文本内容，则返回变量值
		if (StringUtils.hasText(variableValue)) {
			return variableValue;
		}
		// 获取请求参数值
		String parameterValue = request.getParameter(attributeName);
		// 如果参数值不为空且有文本内容，则返回参数值
		if (StringUtils.hasText(parameterValue)) {
			return parameterValue;
		}
		// 否则返回空
		return null;
	}

	protected final Map<String, String> getUriTemplateVariables(NativeWebRequest request) {
		@SuppressWarnings("unchecked")
		// 从请求中获取URI模板变量
		Map<String, String> variables = (Map<String, String>) request.getAttribute(
				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		// 如果URI模板变量不为空，则返回变量；否则返回一个空的Map
		return (variables != null ? variables : Collections.emptyMap());
	}

	/**
	 * 使用 String 请求值（例如 URI 模板变量、请求参数）通过类型转换创建模型属性。
	 * <p>默认实现仅在存在可以执行转换的注册 {@link Converter} 时进行转换。
	 *
	 * @param sourceValue   从中创建模型属性的源值
	 * @param attributeName 属性的名称（永远不为 {@code null}）
	 * @param parameter     方法参数
	 * @param binderFactory 用于创建 WebDataBinder 实例
	 * @param request       当前请求
	 * @return 创建的模型属性；如果找不到适当的转换，则为 {@code null}
	 */
	@Nullable
	protected Object createAttributeFromRequestValue(String sourceValue, String attributeName,
													 MethodParameter parameter, WebDataBinderFactory binderFactory, NativeWebRequest request)
			throws Exception {

		// 使用BinderFactory创建数据绑定器
		DataBinder binder = binderFactory.createBinder(request, null, attributeName);
		// 获取数据绑定器的转换服务
		ConversionService conversionService = binder.getConversionService();
		// 如果转换服务不为空
		if (conversionService != null) {
			// 定义源和目标类型描述符
			TypeDescriptor source = TypeDescriptor.valueOf(String.class);
			TypeDescriptor target = new TypeDescriptor(parameter);
			// 如果转换服务能够执行从源类型到目标类型的转换
			if (conversionService.canConvert(source, target)) {
				// 则执行必要的转换
				return binder.convertIfNecessary(sourceValue, parameter.getParameterType(), parameter);
			}
		}
		// 如果无法转换，则返回null
		return null;
	}

	/**
	 * 在绑定之前，此实现将 {@link WebDataBinder} 下降到 {@link ServletRequestDataBinder}。
	 *
	 * @see ServletRequestDataBinderFactory
	 */
	@Override
	protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {
		// 从原生Web请求中获取ServletRequest
		ServletRequest servletRequest = request.getNativeRequest(ServletRequest.class);
		// 断言ServletRequest不为空，如果为空则抛出异常
		Assert.state(servletRequest != null, "No ServletRequest");
		// 将绑定器转换为ServletRequestDataBinder类型
		ServletRequestDataBinder servletBinder = (ServletRequestDataBinder) binder;
		// 使用ServletRequestDataBinder绑定ServletRequest
		servletBinder.bind(servletRequest);
	}

	@Override
	@Nullable
	public Object resolveConstructorArgument(String paramName, Class<?> paramType, NativeWebRequest request)
			throws Exception {

		// 使用父类方法解析构造函数参数
		Object value = super.resolveConstructorArgument(paramName, paramType, request);
		// 如果解析结果不为空，则直接返回
		if (value != null) {
			return value;
		}
		// 从请求中获取ServletRequest
		ServletRequest servletRequest = request.getNativeRequest(ServletRequest.class);
		// 如果ServletRequest不为空，则从中获取URI模板变量并返回
		if (servletRequest != null) {
			// 获取URI模板变量的属性名
			String attr = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
			// 从ServletRequest中获取URI模板变量
			@SuppressWarnings("unchecked")
			Map<String, String> uriVars = (Map<String, String>) servletRequest.getAttribute(attr);
			// 根据参数名获取URI模板变量的值
			return uriVars.get(paramName);
		}
		// 如果ServletRequest为空，则返回null
		return null;
	}

}
