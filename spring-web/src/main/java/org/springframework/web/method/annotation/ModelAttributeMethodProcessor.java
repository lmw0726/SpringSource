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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.*;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.support.StandardServletPartUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 解析带有 {@code @ModelAttribute} 注解的方法参数并处理带有 {@code @ModelAttribute} 注解的方法的返回值。
 *
 * <p>模型属性是从模型中获取的，或者使用其默认值实例化（然后添加到模型中）。
 * 一旦创建了属性，就会通过数据绑定到 Servlet 请求参数进行填充。
 * 如果参数带有 {@code @javax.validation.Valid} 或 Spring 自己的 {@code @org.springframework.validation.annotation.Validated} 注解，
 * 则可以应用验证。
 *
 * <p>当使用 {@code annotationNotRequired=true} 创建此处理程序时，任何非简单类型的方法参数和返回值都被视为模型属性，
 * 无论是否存在 {@code @ModelAttribute} 注解。
 * <p>
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Vladislav Kisel
 * @since 3.1
 */
public class ModelAttributeMethodProcessor implements HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler {

	/**
	 * 日志记录器
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 是否将非简单方法参数和返回值视为模型属性
	 */
	private final boolean annotationNotRequired;


	/**
	 * 类构造函数。
	 *
	 * @param annotationNotRequired 如果为“true”，则非简单方法参数和返回值被视为模型属性，无论是否带有 {@code @ModelAttribute} 注解
	 */
	public ModelAttributeMethodProcessor(boolean annotationNotRequired) {
		this.annotationNotRequired = annotationNotRequired;
	}

	/**
	 * 如果参数带有 {@link ModelAttribute} 注解，或者在默认解析模式下，任何不是简单类型的方法参数都将支持。
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 如果参数有@ModelAttribute注解，或者注解不是必需的并且参数类型不是简单属性类型，则返回true，否则返回false
		return (parameter.hasParameterAnnotation(ModelAttribute.class) ||
				(this.annotationNotRequired && !BeanUtils.isSimpleProperty(parameter.getParameterType())));
	}

	/**
	 * 从模型中解析参数，如果未找到则使用其默认值进行实例化。然后通过数据绑定和可选的验证对模型属性进行填充。
	 *
	 * @throws BindException 如果数据绑定和验证导致错误，并且下一个方法参数不是 {@link Errors} 类型
	 * @throws Exception     如果 WebDataBinder 初始化失败
	 */
	@Override
	@Nullable
	public final Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
										NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		Assert.state(mavContainer != null, "ModelAttributeMethodProcessor requires ModelAndViewContainer");
		Assert.state(binderFactory != null, "ModelAttributeMethodProcessor requires WebDataBinderFactory");

		// 获取参数的名称
		String name = ModelFactory.getNameForParameter(parameter);
		// 获取参数上的@ModelAttribute注解
		ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
		if (ann != null) {
			// 设置绑定标志
			mavContainer.setBinding(name, ann.binding());
		}

		Object attribute = null;
		BindingResult bindingResult = null;

		// 如果模型容器中包含属性，则从模型中获取属性
		if (mavContainer.containsAttribute(name)) {
			attribute = mavContainer.getModel().get(name);
		} else {
			// 创建属性实例
			try {
				attribute = createAttribute(name, parameter, binderFactory, webRequest);
			} catch (BindException ex) {
				if (isBindExceptionRequired(parameter)) {
					// 没有 BindingResult 参数 -> 使用 BindException 抛出异常
					throw ex;
				}
				// 否则，暴露空值/空绑定结果
				if (parameter.getParameterType() == Optional.class) {
					attribute = Optional.empty();
				} else {
					attribute = ex.getTarget();
				}
				bindingResult = ex.getBindingResult();
			}
		}

		if (bindingResult == null) {
			// Bean 属性绑定和验证；在构造过程中跳过绑定失败的情况。
			WebDataBinder binder = binderFactory.createBinder(webRequest, attribute, name);
			if (binder.getTarget() != null) {
				if (!mavContainer.isBindingDisabled(name)) {
					// 绑定请求参数到属性
					bindRequestParameters(binder, webRequest);
				}
				// 如果适用，则验证
				validateIfApplicable(binder, parameter);
				if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
					// 如果绑定结果包含错误并且需要BindException，则抛出BindException
					throw new BindException(binder.getBindingResult());
				}
			}
			// 值类型适配，也包括 java.util.Optional
			if (!parameter.getParameterType().isInstance(attribute)) {
				attribute = binder.convertIfNecessary(binder.getTarget(), parameter.getParameterType(), parameter);
			}
			bindingResult = binder.getBindingResult();
		}

		// 在模型的末尾添加解析的属性和 BindingResult
		Map<String, Object> bindingResultModel = bindingResult.getModel();
		mavContainer.removeAttributes(bindingResultModel);
		mavContainer.addAllAttributes(bindingResultModel);

		return attribute;
	}

	/**
	 * 如果在模型中未找到属性，则扩展点用于创建模型属性，并通过 bean 属性进行后续参数绑定（除非禁止）。
	 * <p>默认实现通常使用唯一的公共无参数构造函数（如果有的话），但也可以处理数据类的“主构造函数”方法：
	 * 它理解 JavaBeans {@code ConstructorProperties} 注解，以及字节码中保留的运行时参数名称，将请求参数与构造函数参数关联起来。
	 * 如果未找到此类构造函数，则将使用默认构造函数（即使不是公共的），假设后续的 bean 属性绑定通过 setter 方法进行。
	 *
	 * @param attributeName 属性的名称（永远不会为 {@code null}）
	 * @param parameter     方法参数声明
	 * @param binderFactory 用于创建 WebDataBinder 实例
	 * @param webRequest    当前请求
	 * @return 创建的模型属性（永远不会为 {@code null}）
	 * @throws BindException 如果构造函数参数绑定失败
	 * @throws Exception     如果构造函数调用失败
	 * @see #constructAttribute(Constructor, String, MethodParameter, WebDataBinderFactory, NativeWebRequest)
	 * @see BeanUtils#findPrimaryConstructor(Class)
	 */
	protected Object createAttribute(String attributeName, MethodParameter parameter,
									 WebDataBinderFactory binderFactory, NativeWebRequest webRequest) throws Exception {

		// 获取嵌套的参数
		MethodParameter nestedParameter = parameter.nestedIfOptional();
		// 获取嵌套参数的类型
		Class<?> clazz = nestedParameter.getNestedParameterType();

		// 获取可解析的构造函数
		Constructor<?> ctor = BeanUtils.getResolvableConstructor(clazz);
		// 构造属性实例
		Object attribute = constructAttribute(ctor, attributeName, parameter, binderFactory, webRequest);
		// 如果参数不等于嵌套参数，则将属性包装在Optional中
		if (parameter != nestedParameter) {
			attribute = Optional.of(attribute);
		}
		return attribute;
	}

	/**
	 * 使用给定的构造函数构造一个新的属性实例。
	 * <p>在 {@link #createAttribute(String, MethodParameter, WebDataBinderFactory, NativeWebRequest)} 方法中调用，
	 * 在构造函数解析后。
	 *
	 * @param ctor          要使用的构造函数
	 * @param attributeName 属性的名称（永远不会为 {@code null}）
	 * @param binderFactory 用于创建 WebDataBinder 实例
	 * @param webRequest    当前请求
	 * @return 创建的模型属性（永远不会为 {@code null}）
	 * @throws BindException 如果构造函数参数绑定失败
	 * @throws Exception     如果构造函数调用失败
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	protected Object constructAttribute(Constructor<?> ctor, String attributeName, MethodParameter parameter,
										WebDataBinderFactory binderFactory, NativeWebRequest webRequest) throws Exception {

		// 如果构造函数的参数个数为0，表示单个默认构造函数，直接实例化对象并返回
		if (ctor.getParameterCount() == 0) {
			// 单个默认构造函数 -> 明显是标准的 JavaBeans 安排。
			return BeanUtils.instantiateClass(ctor);
		}

		// 构造函数的参数个数不为0，表示单个数据类构造函数，需要从请求参数中解析构造函数参数
		// 获取构造函数参数的名称
		String[] paramNames = BeanUtils.getParameterNames(ctor);
		// 获取构造函数参数的类型
		Class<?>[] paramTypes = ctor.getParameterTypes();
		// 构造函数参数值的数组
		Object[] args = new Object[paramTypes.length];
		// 创建WebDataBinder
		WebDataBinder binder = binderFactory.createBinder(webRequest, null, attributeName);
		// 获取字段默认前缀
		String fieldDefaultPrefix = binder.getFieldDefaultPrefix();
		// 获取字段标记前缀
		String fieldMarkerPrefix = binder.getFieldMarkerPrefix();
		boolean bindingFailure = false; // 绑定失败标志
		// 存储绑定失败的参数名称的集合
		Set<String> failedParams = new HashSet<>(4);

		// 遍历构造函数参数
		for (int i = 0; i < paramNames.length; i++) {
			// 获取参数名称
			String paramName = paramNames[i];
			// 获取参数类型
			Class<?> paramType = paramTypes[i];
			// 获取参数值
			Object value = webRequest.getParameterValues(paramName);

			// 解析单个值参数
			if (ObjectUtils.isArray(value) && Array.getLength(value) == 1) {
				value = Array.get(value, 0);
			}

			// 如果参数值为空
			if (value == null) {
				// 尝试从参数名添加字段默认前缀获取值
				if (fieldDefaultPrefix != null) {
					value = webRequest.getParameter(fieldDefaultPrefix + paramName);
				}
				// 如果值仍为空
				if (value == null) {
					// 如果字段标记前缀存在且存在与字段标记前缀+参数名对应的请求参数，则使用空值填充
					if (fieldMarkerPrefix != null && webRequest.getParameter(fieldMarkerPrefix + paramName) != null) {
						value = binder.getEmptyValue(paramType);
					} else {
						// 否则，从请求参数中解析参数值
						value = resolveConstructorArgument(paramName, paramType, webRequest);
					}
				}
			}

			try {
				// 尝试将参数值转换为目标类型
				MethodParameter methodParam = new FieldAwareConstructorParameter(ctor, i, paramName);
				if (value == null && methodParam.isOptional()) {
					args[i] = (methodParam.getParameterType() == Optional.class ? Optional.empty() : null);
				} else {
					args[i] = binder.convertIfNecessary(value, paramType, methodParam);
				}
			} catch (TypeMismatchException ex) {
				// 如果转换失败，记录绑定失败信息
				// 初始化绑定失败的参数名
				ex.initPropertyName(paramName);
				// 将参数值设置为null
				args[i] = null;
				// 将绑定失败的参数名添加到集合中
				failedParams.add(paramName);
				// 记录绑定失败的字段值
				binder.getBindingResult().recordFieldValue(paramName, paramType, value);
				// 处理属性访问异常，将异常信息添加到绑定结果中
				binder.getBindingErrorProcessor().processPropertyAccessException(ex, binder.getBindingResult());
				// 标记绑定失败
				bindingFailure = true;
			}
		}

		// 如果存在绑定失败，抛出绑定异常
		if (bindingFailure) {
			// 获取绑定结果对象
			BindingResult result = binder.getBindingResult();
			// 遍历参数名数组
			for (int i = 0; i < paramNames.length; i++) {
				String paramName = paramNames[i];
				// 如果当前参数名不在失败的参数名集合中
				if (!failedParams.contains(paramName)) {
					// 获取当前参数对应的值
					Object value = args[i];
					// 记录字段值到绑定结果对象中
					result.recordFieldValue(paramName, paramTypes[i], value);
					// 如果适用，则验证值
					validateValueIfApplicable(binder, parameter, ctor.getDeclaringClass(), paramName, value);
				}
			}
			// 如果参数不可选，尝试实例化对象并抛出绑定异常
			if (!parameter.isOptional()) {
				try {
					// 使用构造函数和参数实例化目标对象
					Object target = BeanUtils.instantiateClass(ctor, args);
					// 抛出包含绑定结果的绑定异常，其中包含目标对象
					throw new BindException(result) {
						@Override
						public Object getTarget() {
							return target;
						}
					};
				} catch (BeanInstantiationException ex) {
					// 吞下并处理没有目标示例的情况
				}
			}
			throw new BindException(result);
		}

		// 构造函数参数绑定成功，实例化对象并返回
		return BeanUtils.instantiateClass(ctor, args);
	}

	/**
	 * 扩展点，用于将请求绑定到目标对象。
	 *
	 * @param binder  用于绑定的数据绑定器实例
	 * @param request 当前请求
	 */
	protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {
		((WebRequestDataBinder) binder).bind(request);
	}

	@Nullable
	public Object resolveConstructorArgument(String paramName, Class<?> paramType, NativeWebRequest request)
			throws Exception {

		// 尝试从请求中获取 MultipartRequest
		MultipartRequest multipartRequest = request.getNativeRequest(MultipartRequest.class);
		if (multipartRequest != null) {
			// 如果存在 MultipartRequest
			// 获取参数名对应的文件列表
			List<MultipartFile> files = multipartRequest.getFiles(paramName);
			if (!files.isEmpty()) {
				// 如果文件列表不为空，则返回单个文件或文件列表
				return (files.size() == 1 ? files.get(0) : files);
			}
		} else if (StringUtils.startsWithIgnoreCase(request.getHeader(HttpHeaders.CONTENT_TYPE), MediaType.MULTIPART_FORM_DATA_VALUE)) {
			// 如果请求头中的 Content-Type 是 multipart/form-data
			HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
			if (servletRequest != null && HttpMethod.POST.matches(servletRequest.getMethod())) {
				// 从 HttpServletRequest 中获取 Part 列表
				List<Part> parts = StandardServletPartUtils.getParts(servletRequest, paramName);
				if (!parts.isEmpty()) {
					// 如果 Part 列表不为空，则返回单个 Part 或 Part 列表
					return (parts.size() == 1 ? parts.get(0) : parts);
				}
			}
		}
		// 如果未找到文件或 Part，则返回 null
		return null;
	}

	/**
	 * 如果适用，则验证模型属性。
	 * <p>默认实现检查 {@code @javax.validation.Valid}、Spring 的 {@link org.springframework.validation.annotation.Validated}
	 * 和以“Valid”开头的自定义注解。
	 *
	 * @param binder    要使用的 DataBinder
	 * @param parameter 方法参数声明
	 * @see WebDataBinder#validate(Object...)
	 * @see SmartValidator#validate(Object, Errors, Object...)
	 */
	protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
		// 遍历方法参数的所有注解
		for (Annotation ann : parameter.getParameterAnnotations()) {
			// 确定验证提示
			Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
			// 如果验证提示不为空
			if (validationHints != null) {
				// 使用验证提示对数据绑定器进行验证
				binder.validate(validationHints);
				// 跳出循环
				break;
			}
		}
	}

	/**
	 * 如果适用，则验证指定的候选值。
	 * <p>默认实现检查 {@code @javax.validation.Valid}、Spring 的 {@link org.springframework.validation.annotation.Validated}
	 * 和以“Valid”开头的自定义注解。
	 *
	 * @param binder     要使用的 DataBinder
	 * @param parameter  方法参数声明
	 * @param targetType 目标类型
	 * @param fieldName  字段名称
	 * @param value      候选值
	 * @see #validateIfApplicable(WebDataBinder, MethodParameter)
	 * @see SmartValidator#validateValue(Class, String, Object, Errors, Object...)
	 * @since 5.1
	 */
	protected void validateValueIfApplicable(WebDataBinder binder, MethodParameter parameter,
											 Class<?> targetType, String fieldName, @Nullable Object value) {

		// 遍历方法参数的所有注解
		for (Annotation ann : parameter.getParameterAnnotations()) {
			// 确定验证提示
			Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
			// 如果验证提示不为空
			if (validationHints != null) {
				// 遍历数据绑定器中的验证器
				for (Validator validator : binder.getValidators()) {
					// 如果验证器是智能验证器
					if (validator instanceof SmartValidator) {
						try {
							// 对目标类型、字段名、值进行验证，传入数据绑定结果和验证提示
							((SmartValidator) validator).validateValue(targetType, fieldName, value,
									binder.getBindingResult(), validationHints);
						} catch (IllegalArgumentException ex) {
							// 目标类上没有相应的字段...
						}
					}
				}
				// 跳出循环
				break;
			}
		}
	}

	/**
	 * 在验证错误时是否引发致命的绑定异常。
	 * <p>默认实现委托给 {@link #isBindExceptionRequired(MethodParameter)}。
	 *
	 * @param binder    用于执行数据绑定的数据绑定器
	 * @param parameter 方法参数声明
	 * @return 如果下一个方法参数不是 {@link Errors} 类型，则返回 {@code true}
	 * @see #isBindExceptionRequired(MethodParameter)
	 */
	protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
		return isBindExceptionRequired(parameter);
	}

	/**
	 * 在验证错误时是否引发致命的绑定异常。
	 *
	 * @param parameter 方法参数声明
	 * @return 如果下一个方法参数不是 {@link Errors} 类型，则返回 {@code true}
	 * @since 5.0
	 */
	protected boolean isBindExceptionRequired(MethodParameter parameter) {
		// 获取方法参数的索引
		int i = parameter.getParameterIndex();
		// 获取方法参数类型数组
		Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
		// 判断是否有绑定结果参数
		boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
		// 返回是否不需要抛出致命的绑定异常
		return !hasBindingResult;
	}

	/**
	 * 如果有方法级 {@code @ModelAttribute}，或者在默认解析模式下，对于不是简单类型的任何返回值类型，返回 {@code true}。
	 */
	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// 检查是否有@ModelAttribute注解
		return (returnType.hasMethodAnnotation(ModelAttribute.class) ||
				// 或者将非简单方法参数和返回值视为模型属性，并且返回值类型不是简单类型
				(this.annotationNotRequired && !BeanUtils.isSimpleProperty(returnType.getParameterType())));
	}

	/**
	 * 将非空返回值添加到 {@link ModelAndViewContainer}。
	 */
	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue != null) {
			//如果有返回值，获取返回值名称
			String name = ModelFactory.getNameForReturnValue(returnValue, returnType);
			// 将名称和返回值，添加进 模型与视图容器 中
			mavContainer.addAttribute(name, returnValue);
		}
	}


	/**
	 * {@link MethodParameter} 的子类，可检测字段注解。
	 *
	 * @since 5.1
	 */
	private static class FieldAwareConstructorParameter extends MethodParameter {

		/**
		 * 参数名称
		 */
		private final String parameterName;

		/**
		 * 组合注解
		 */
		@Nullable
		private volatile Annotation[] combinedAnnotations;

		public FieldAwareConstructorParameter(Constructor<?> constructor, int parameterIndex, String parameterName) {
			super(constructor, parameterIndex);
			this.parameterName = parameterName;
		}

		@Override
		public Annotation[] getParameterAnnotations() {
			// 获取已合并的注解数组
			Annotation[] anns = this.combinedAnnotations;
			// 如果尚未合并
			if (anns == null) {
				// 获取方法参数的所有注解
				anns = super.getParameterAnnotations();
				try {
					// 获取声明当前参数的类的声明的字段
					Field field = getDeclaringClass().getDeclaredField(this.parameterName);
					// 获取字段的所有注解
					Annotation[] fieldAnns = field.getAnnotations();
					// 如果字段有注解
					if (fieldAnns.length > 0) {
						// 创建一个新的列表，长度为已有注解数组和字段注解数组长度之和
						List<Annotation> merged = new ArrayList<>(anns.length + fieldAnns.length);
						// 将已有注解数组添加到新列表中
						merged.addAll(Arrays.asList(anns));
						// 遍历字段的注解数组
						for (Annotation fieldAnn : fieldAnns) {
							// 检查是否已经存在相同类型的注解
							boolean existingType = false;
							for (Annotation ann : anns) {
								if (ann.annotationType() == fieldAnn.annotationType()) {
									existingType = true;
									break;
								}
							}
							// 如果不存在相同类型的注解，则将该注解添加到新列表中
							if (!existingType) {
								merged.add(fieldAnn);
							}
						}
						// 将新列表转换为注解数组
						anns = merged.toArray(new Annotation[0]);
					}
				} catch (NoSuchFieldException | SecurityException ex) {
					// 忽略异常
				}
				// 缓存合并后的注解数组
				this.combinedAnnotations = anns;
			}
			// 返回合并后的注解数组
			return anns;
		}

		@Override
		public String getParameterName() {
			return this.parameterName;
		}
	}

}
