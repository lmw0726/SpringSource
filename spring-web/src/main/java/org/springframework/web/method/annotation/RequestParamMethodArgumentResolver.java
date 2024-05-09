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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.UriComponentsContributor;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 解析使用 @{@link RequestParam} 注解的方法参数，以及与 Spring 的 {@link MultipartResolver} 抽象一起使用的 {@link MultipartFile} 类型的参数，
 * 以及与 Servlet 3.0 的多部分请求一起使用的 {@code javax.servlet.http.Part} 类型的参数。该解析器也可以以默认解析模式创建，在默认解析模式下，
 * 未使用 @{@link RequestParam} 注解的简单类型（如 int、long 等）也会被视为请求参数，参数名将从方法参数名派生而来。
 *
 * <p>如果方法参数类型为 {@link Map}，则使用注解中指定的名称来解析请求参数字符串值。然后，通过类型转换将该值转换为 {@link Map}，
 * 假设已注册了适当的 {@link Converter} 或 {@link PropertyEditor}。
 * 或者，如果未指定请求参数名称，则使用 {@link RequestParamMapMethodArgumentResolver} 来提供所有请求参数的访问权限，
 * 以映射形式表示。
 *
 * <p>调用 {@link WebDataBinder} 来对解析的请求头值进行类型转换，使其与方法参数类型匹配。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @see RequestParamMapMethodArgumentResolver
 * @since 3.1
 */
public class RequestParamMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver
		implements UriComponentsContributor {

	/**
	 * 字符窜类型描述符
	 */
	private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);
	/**
	 * 是否使用默认的解决方法
	 */
	private final boolean useDefaultResolution;


	/**
	 * 创建一个新的 {@link RequestParamMethodArgumentResolver} 实例。
	 *
	 * @param useDefaultResolution 在默认解析模式下，如果方法参数是简单类型（由 {@link BeanUtils#isSimpleProperty} 定义），
	 *                             则即使未使用 @{@link RequestParam} 注解，它也会被视为请求参数，请求参数名称将从方法参数名派生而来。
	 */
	public RequestParamMethodArgumentResolver(boolean useDefaultResolution) {
		this.useDefaultResolution = useDefaultResolution;
	}

	/**
	 * 创建一个新的 {@link RequestParamMethodArgumentResolver} 实例。
	 *
	 * @param beanFactory          用于解析默认值中的 ${...} 占位符和 #{...} SpEL 表达式的 Bean 工厂；如果不希望默认值包含表达式，则为 {@code null}。
	 * @param useDefaultResolution 在默认解析模式下，如果方法参数是简单类型（由 {@link BeanUtils#isSimpleProperty} 定义），
	 *                             则即使未使用 @{@link RequestParam} 注解，它也会被视为请求参数，请求参数名称将从方法参数名派生而来。
	 */
	public RequestParamMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory,
											  boolean useDefaultResolution) {

		super(beanFactory);
		this.useDefaultResolution = useDefaultResolution;
	}


	/**
	 * 支持以下内容：
	 * <ul>
	 * <li>带有 @{@link RequestParam} 注解的方法参数。
	 *    这不包括 {@link Map} 参数，其中注解没有指定名称。对于此类参数，请使用 {@link RequestParamMapMethodArgumentResolver}。
	 * <li>除非带有 @{@link RequestPart} 注解，否则类型为 {@link MultipartFile} 的参数。
	 * <li>除非带有 @{@link RequestPart} 注解，否则类型为 {@code Part} 的参数。
	 * <li>在默认解析模式下，简单类型参数，即使没有使用 @{@link RequestParam} 注解。
	 * </ul>
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 检查参数上是否有@RequestParam注解
		if (parameter.hasParameterAnnotation(RequestParam.class)) {
			// 如果参数的嵌套类型是Map或Map的子类，则检查@RequestParam注解的name属性是否存在
			if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
				// 获取方法参数上的@RequestParam注解
				RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
				// 如果注解存在，并且注解中指定了name属性，则返回true。
				return (requestParam != null && StringUtils.hasText(requestParam.name()));
			} else {
				// 否则，返回true
				return true;
			}
		} else {
			// 如果参数上没有@RequestParam注解
			if (parameter.hasParameterAnnotation(RequestPart.class)) {
				// 如果参数上有@RequestPart注解，则返回false
				return false;
			}
			// 获取参数的嵌套参数
			parameter = parameter.nestedIfOptional();
			// 检查参数是否为多部分内容
			if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
				return true;
			} else if (this.useDefaultResolution) {
				// 如果使用默认解析，并且参数是简单属性，则返回true
				return BeanUtils.isSimpleProperty(parameter.getNestedParameterType());
			} else {
				// 否则，返回false
				return false;
			}
		}
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		// 获取方法参数上的@RequestParam注解
		RequestParam ann = parameter.getParameterAnnotation(RequestParam.class);
		// 如果该注解存在，则使用该注解信息创建 RequestParamNamedValueInfo，否则创建空的 RequestParamNamedValueInfo
		return (ann != null ? new RequestParamNamedValueInfo(ann) : new RequestParamNamedValueInfo());
	}

	@Override
	@Nullable
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
		// 获取原生的HttpServletRequest对象
		HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);

		if (servletRequest != null) {
			// 如果原生的HttpServletRequest对象不为空，则尝试解析多部分内容参数
			Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, servletRequest);
			if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
				// 如果解析成功，则返回解析结果
				return mpArg;
			}
		}

		Object arg = null;
		// 获取多部分请求对象
		MultipartRequest multipartRequest = request.getNativeRequest(MultipartRequest.class);
		if (multipartRequest != null) {
			// 如果多部分请求对象不为空，则尝试获取参数对应的文件
			List<MultipartFile> files = multipartRequest.getFiles(name);
			if (!files.isEmpty()) {
				// 如果文件列表不为空，则返回单个文件或文件列表
				arg = (files.size() == 1 ? files.get(0) : files);
			}
		}
		if (arg == null) {
			// 如果arg仍然为空，则尝试获取普通参数值
			String[] paramValues = request.getParameterValues(name);
			if (paramValues != null) {
				// 如果参数值数组不为空，则返回单个参数值或参数值数组
				arg = (paramValues.length == 1 ? paramValues[0] : paramValues);
			}
		}
		// 返回参数值
		return arg;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter, NativeWebRequest request)
			throws Exception {

		handleMissingValueInternal(name, parameter, request, false);
	}

	@Override
	protected void handleMissingValueAfterConversion(
			String name, MethodParameter parameter, NativeWebRequest request) throws Exception {

		handleMissingValueInternal(name, parameter, request, true);
	}

	protected void handleMissingValueInternal(
			String name, MethodParameter parameter, NativeWebRequest request, boolean missingAfterConversion)
			throws Exception {

		// 获取原生的HttpServletRequest对象
		HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
		// 如果参数是多部分内容的参数
		if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
			if (servletRequest == null || !MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
				// 如果HttpServletRequest对象为空或不是多部分请求，则抛出MultipartException异常
				throw new MultipartException("Current request is not a multipart request");
			} else {
				// 否则，抛出MissingServletRequestPartException异常
				throw new MissingServletRequestPartException(name);
			}
		} else {
			// 如果参数不是多部分内容的参数，则抛出MissingServletRequestParameterException异常
			throw new MissingServletRequestParameterException(name,
					parameter.getNestedParameterType().getSimpleName(), missingAfterConversion);
		}
	}

	@Override
	public void contributeMethodArgument(MethodParameter parameter, @Nullable Object value,
										 UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

		// 获取参数的嵌套类型
		Class<?> paramType = parameter.getNestedParameterType();
		// 如果参数的嵌套类型是Map、MultipartFile或Part，则直接返回
		if (Map.class.isAssignableFrom(paramType) || MultipartFile.class == paramType || Part.class == paramType) {
			return;
		}

		// 获取@RequestParam注解
		RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
		// 获取参数名称
		String name = (requestParam != null && StringUtils.hasLength(requestParam.name()) ?
				requestParam.name() : parameter.getParameterName());
		Assert.state(name != null, "Unresolvable parameter name");

		// 获取参数的嵌套参数
		parameter = parameter.nestedIfOptional();
		// 如果值是Optional类型，则获取其实际值
		if (value instanceof Optional) {
			value = ((Optional<?>) value).orElse(null);
		}

		// 如果值为null
		if (value == null) {
			// 如果@RequestParam注解存在，并且参数非必需或有默认值，则直接返回
			if (requestParam != null &&
					(!requestParam.required() || !requestParam.defaultValue().equals(ValueConstants.DEFAULT_NONE))) {
				return;
			}
			// 否则，将参数添加到URI中
			builder.queryParam(name);
		} else if (value instanceof Collection) {
			// 如果值是集合类型，则逐个添加到URI中
			for (Object element : (Collection<?>) value) {
				// 将集合元素格式化后添加到URI中
				element = formatUriValue(conversionService, TypeDescriptor.nested(parameter, 1), element);
				builder.queryParam(name, element);
			}
		} else {
			// 如果值是单个值，则将其格式化后添加到URI中
			builder.queryParam(name, formatUriValue(conversionService, new TypeDescriptor(parameter), value));
		}
	}

	@Nullable
	protected String formatUriValue(
			@Nullable ConversionService cs, @Nullable TypeDescriptor sourceType, @Nullable Object value) {

		// 如果值为null，则直接返回null
		if (value == null) {
			return null;
		} else if (value instanceof String) {
			// 如果值已经是字符串类型，则直接返回
			return (String) value;
		} else if (cs != null) {
			// 如果转换服务不为空，则使用转换服务进行转换并返回结果
			return (String) cs.convert(value, sourceType, STRING_TYPE_DESCRIPTOR);
		} else {
			// 否则，将值转换为字符串类型并返回
			return value.toString();
		}
	}


	private static class RequestParamNamedValueInfo extends NamedValueInfo {

		public RequestParamNamedValueInfo() {
			super("", false, ValueConstants.DEFAULT_NONE);
		}

		public RequestParamNamedValueInfo(RequestParam annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
