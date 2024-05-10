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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * 通过读取和写入请求或响应的主体，使用 {@link HttpMessageConverter} 来解析带有 {@code @RequestBody} 注解的方法参数，
 * 并处理带有 {@code @ResponseBody} 注解的方法返回值。
 *
 * <p>如果 {@code @RequestBody} 方法参数带有任何触发验证的
 * {@linkplain org.springframework.validation.annotation.ValidationAnnotationUtils#determineValidationHints 注解}，
 * 则也会对其进行验证。在验证失败的情况下，将引发 {@link MethodArgumentNotValidException}，
 * 如果配置了 {@link DefaultHandlerExceptionResolver}，则会导致 HTTP 400 响应状态码。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class RequestResponseBodyMethodProcessor extends AbstractMessageConverterMethodProcessor {

	/**
	 * 仅使用转换器的基本构造函数。适用于解析 {@code @RequestBody}。
	 * 要处理 {@code @ResponseBody}，还需提供一个 {@code ContentNegotiationManager}。
	 */
	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters) {
		super(converters);
	}

	/**
	 * 带有转换器和 {@code ContentNegotiationManager} 的基本构造函数。
	 * 适用于解析 {@code @RequestBody} 和处理 {@code @ResponseBody}，但没有 {@code Request~} 或 {@code ResponseBodyAdvice}。
	 */
	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters,
											  @Nullable ContentNegotiationManager manager) {

		super(converters, manager);
	}

	/**
	 * 用于解析 {@code @RequestBody} 方法参数的完整构造函数。
	 * 要处理 {@code @ResponseBody}，还需提供一个 {@code ContentNegotiationManager}。
	 *
	 * @since 4.2
	 */
	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters,
											  @Nullable List<Object> requestResponseBodyAdvice) {

		super(converters, null, requestResponseBodyAdvice);
	}

	/**
	 * 用于解析 {@code @RequestBody} 并处理 {@code @ResponseBody} 的完整构造函数。
	 */
	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters,
											  @Nullable ContentNegotiationManager manager, @Nullable List<Object> requestResponseBodyAdvice) {

		super(converters, manager, requestResponseBodyAdvice);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 检查方法参数上是否含有@RequestBody注解
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// 检查返回类型所在类是否具有 @ResponseBody 注解，
		// 或者返回类型的方法是否具有 @ResponseBody 注解。
		return (AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ResponseBody.class) ||
				returnType.hasMethodAnnotation(ResponseBody.class));
	}

	/**
	 * 如果验证失败，则抛出 MethodArgumentNotValidException。
	 *
	 * @throws HttpMessageNotReadableException 如果 {@link RequestBody#required()} 为 {@code true}，并且没有主体内容，或者没有适合的转换器来读取内容。
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		// 如果参数是 Optional 类型，则获取其嵌套类型的参数。
		parameter = parameter.nestedIfOptional();
		// 使用消息转换器读取参数值。
		Object arg = readWithMessageConverters(webRequest, parameter, parameter.getNestedGenericParameterType());
		// 根据参数生成名称。
		String name = Conventions.getVariableNameForParameter(parameter);

		if (binderFactory != null) {
			// 如果有绑定工厂，则创建数据绑定器。
			WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name);
			if (arg != null) {
				// 如果参数不为 null，则进行验证。
				validateIfApplicable(binder, parameter);
				// 如果绑定结果存在错误并且需要绑定异常，则抛出方法参数验证异常。
				if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
					throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
				}
			}
			// 如果模型和视图容器不为 null，则将绑定结果添加到模型中。
			if (mavContainer != null) {
				mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
			}
		}

		// 将参数适应为必要的类型并返回。
		return adaptArgumentIfNecessary(arg, parameter);
	}

	@Override
	protected <T> Object readWithMessageConverters(NativeWebRequest webRequest, MethodParameter parameter,
												   Type paramType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {

		// 从 WebRequest 中获取原生的 HttpServletRequest 对象。
		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		// 断言确保 HttpServletRequest 不为 null。
		Assert.state(servletRequest != null, "No HttpServletRequest");
		// 创建 ServletServerHttpRequest 对象，用于读取请求体。
		ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(servletRequest);

		// 使用消息转换器读取参数值。
		Object arg = readWithMessageConverters(inputMessage, parameter, paramType);
		// 如果参数值为 null 且参数为必需，则抛出 HTTP 消息不可读异常。
		if (arg == null && checkRequired(parameter)) {
			throw new HttpMessageNotReadableException("Required request body is missing: " +
					parameter.getExecutable().toGenericString(), inputMessage);
		}
		// 返回参数值。
		return arg;
	}

	protected boolean checkRequired(MethodParameter parameter) {
		// 获取方法参数上的 @RequestBody 注解。
		RequestBody requestBody = parameter.getParameterAnnotation(RequestBody.class);
		// 如果 @RequestBody 注解不为 null，并且标记为必需，且参数不是 Optional 类型，则返回 true。
		return (requestBody != null && requestBody.required() && !parameter.isOptional());
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
			throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

		// 将请求处理标记为已处理。
		mavContainer.setRequestHandled(true);
		// 创建 ServletServerHttpRequest 输入消息。
		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
		// 创建 ServletServerHttpResponse 输出消息。
		ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);

		// 将返回值使用消息转换器写入输出消息。即使返回值为 null，ResponseBodyAdvice 也可能会介入。
		writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);
	}

}
