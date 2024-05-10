/*
 * Copyright 2002-2022 the original author or authors.
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
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;
import org.springframework.web.multipart.support.RequestPartServletServerHttpRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 解析以下方法参数：
 * <ul>
 * <li>带有 @{@link RequestPart} 注解的参数
 * <li>与 Spring 的 {@link MultipartResolver} 抽象一起使用的 {@link MultipartFile} 类型的参数
 * <li>与 Servlet 3.0 多部分请求一起使用的 {@code javax.servlet.http.Part} 类型的参数
 * </ul>
 *
 * <p>当参数带有 {@code @RequestPart} 注解时，部分的内容将通过 {@link HttpMessageConverter} 传递，
 * 以便根据请求部分的 'Content-Type' 解析方法参数。这类似于 @{@link RequestBody} 根据常规请求的内容解析参数的做法。
 *
 * <p>当参数没有带有 {@code @RequestPart} 注解或未指定部分的名称时，请求部分的名称将从方法参数的名称派生而来。
 *
 * <p>如果参数带有任何触发验证的 {@linkplain org.springframework.validation.annotation.ValidationAnnotationUtils#determineValidationHints 注解}，
 * 则可以应用自动验证。在验证失败的情况下，将引发 {@link MethodArgumentNotValidException}，并且如果配置了 {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver}，
 * 则返回 400 响应状态码。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 3.1
 */
public class RequestPartMethodArgumentResolver extends AbstractMessageConverterMethodArgumentResolver {

	/**
	 * 仅使用转换器的基本构造函数。
	 */
	public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters) {
		super(messageConverters);
	}

	/**
	 * 带有转换器和 {@code RequestBodyAdvice} 和
	 * {@code ResponseBodyAdvice} 的构造函数。
	 */
	public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters,
											 List<Object> requestResponseBodyAdvice) {

		super(messageConverters, requestResponseBodyAdvice);
	}


	/**
	 * 是否支持给定的 {@linkplain MethodParameter 方法参数} 作为多部分。支持以下方法参数：
	 * <ul>
	 * <li>带有 {@code @RequestPart} 注解
	 * <li>除非带有 {@code @RequestParam} 注解，否则类型为 {@link MultipartFile} 的参数
	 * <li>除非带有 {@code @RequestParam} 注解，否则类型为 {@code javax.servlet.http.Part} 的参数
	 * </ul>
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 如果参数被 @RequestPart 注解修饰，则返回 true
		if (parameter.hasParameterAnnotation(RequestPart.class)) {
			return true;
		} else {
			// 如果参数被 @RequestParam 注解修饰，则返回 false
			if (parameter.hasParameterAnnotation(RequestParam.class)) {
				return false;
			}
			// 否则，检查参数是否为多部分请求参数
			return MultipartResolutionDelegate.isMultipartArgument(parameter.nestedIfOptional());
		}
	}

	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest request, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		// 从请求中获取 HttpServletRequest 对象。
		HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
		// 断言 servletRequest 不为空，否则抛出异常。
		Assert.state(servletRequest != null, "No HttpServletRequest");

		// 获取方法参数上的 @RequestPart 注解。
		RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
		// 确定是否为必填参数。
		boolean isRequired = ((requestPart == null || requestPart.required()) && !parameter.isOptional());

		// 获取请求部分的名称。
		String name = getPartName(parameter, requestPart);
		// 如果参数为可选参数，则将其转换为内部参数。
		parameter = parameter.nestedIfOptional();
		// 初始化参数对象。
		Object arg = null;

		// 尝试解析多部分请求中的参数。
		Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, servletRequest);
		// 如果成功解析，则使用解析结果。
		if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
			arg = mpArg;
		} else {
			try {
				// 创建请求部分的 ServletServerHttpRequest 对象
				HttpInputMessage inputMessage = new RequestPartServletServerHttpRequest(servletRequest, name);
				// 使用消息转换器读取参数。
				arg = readWithMessageConverters(inputMessage, parameter, parameter.getNestedGenericParameterType());
				if (binderFactory != null) {
					// 如果存在数据绑定工厂，则对参数进行数据绑定。
					WebDataBinder binder = binderFactory.createBinder(request, arg, name);
					if (arg != null) {
						// 如果参数不为空，则验证参数是否适用
						validateIfApplicable(binder, parameter);
						// 如果绑定结果存在错误且需要抛出绑定异常，则抛出异常。
						if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
							throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
						}
					}
					// 将绑定结果添加到 ModelAndViewContainer 中。
					if (mavContainer != null) {
						mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
					}
				}
			} catch (MissingServletRequestPartException | MultipartException ex) {
				// 如果参数为必填且请求参数缺失，则抛出异常。
				if (isRequired) {
					throw ex;
				}
			}
		}

		// 如果参数为空且为必填，则抛出相应的异常。
		if (arg == null && isRequired) {
			// 如果当前请求不是多部分请求，则抛出多部分异常；否则抛出缺失请求部分异常。
			if (!MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
				throw new MultipartException("Current request is not a multipart request");
			} else {
				throw new MissingServletRequestPartException(name);
			}
		}
		// 返回适当调整的参数对象。
		return adaptArgumentIfNecessary(arg, parameter);
	}

	private String getPartName(MethodParameter methodParam, @Nullable RequestPart requestPart) {
		// 如果 @RequestPart 不为空，则使用 @RequestPart 注解中的名称；否则使用空字符串。
		String partName = (requestPart != null ? requestPart.name() : "");
		// 如果 部分名称 为空，则尝试从方法参数中获取参数名称。
		if (partName.isEmpty()) {
			// 获取参数u名称作为 部分名称
			partName = methodParam.getParameterName();
			// 如果无法从方法参数中获取参数名称，则抛出异常。
			if (partName == null) {
				throw new IllegalArgumentException("Request part name for argument type [" +
						methodParam.getNestedParameterType().getName() +
						"] not specified, and parameter name information not found in class file either.");
			}
		}
		return partName;
	}

	@Override
	void closeStreamIfNecessary(InputStream body) {
		// RequestPartServletServerHttpRequest 暴露了各个部分的流，
		// 可能来自临时文件 -> 在解析后明确调用 close 方法以防止文件描述符泄漏。
		try {
			body.close();
		} catch (IOException ex) {
			// 忽略
		}
	}

}
