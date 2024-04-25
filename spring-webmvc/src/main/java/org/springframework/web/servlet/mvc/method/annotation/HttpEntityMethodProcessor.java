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
import org.springframework.core.ResolvableType;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 解析 {@link HttpEntity} 和 {@link RequestEntity} 方法参数值，并处理 {@link HttpEntity} 和 {@link ResponseEntity} 返回值。
 *
 * <p>{@link HttpEntity} 返回类型具有特定目的。因此，应该在支持任何带有 {@code @ModelAttribute} 或 {@code @ResponseBody} 注解的返回值类型的处理程序之前配置此处理程序，以确保它们不会接管。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.1
 */
public class HttpEntityMethodProcessor extends AbstractMessageConverterMethodProcessor {

	/**
	 * 只有转换器的基本构造函数。适用于解析 {@code HttpEntity}。
	 * 要处理 {@code ResponseEntity}，还需考虑提供 {@code ContentNegotiationManager}。
	 */
	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters) {
		super(converters);
	}

	/**
	 * 带有转换器和 {@code ContentNegotiationManager} 的基本构造函数。
	 * 适用于解析 {@code HttpEntity} 和处理 {@code ResponseEntity}，无需 {@code Request~} 或 {@code ResponseBodyAdvice}。
	 */
	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters,
									 ContentNegotiationManager manager) {

		super(converters, manager);
	}

	/**
	 * 用于解析 {@code HttpEntity} 方法参数的完整构造函数。
	 * 要处理 {@code ResponseEntity}，还需考虑提供 {@code ContentNegotiationManager}。
	 *
	 * @since 4.2
	 */
	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters,
									 List<Object> requestResponseBodyAdvice) {

		super(converters, null, requestResponseBodyAdvice);
	}

	/**
	 * 用于解析 {@code HttpEntity} 和处理 {@code ResponseEntity} 的完整构造函数。
	 */
	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters,
									 @Nullable ContentNegotiationManager manager, List<Object> requestResponseBodyAdvice) {

		super(converters, manager, requestResponseBodyAdvice);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 如果是HttpEntity类型或者是RequesetEntity类型
		return (HttpEntity.class == parameter.getParameterType() ||
				RequestEntity.class == parameter.getParameterType());
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// 如果返回类型不是HttpEntity，也不是RequestEntity
		return (HttpEntity.class.isAssignableFrom(returnType.getParameterType()) &&
				!RequestEntity.class.isAssignableFrom(returnType.getParameterType()));
	}

	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory)
			throws IOException, HttpMediaTypeNotSupportedException {

		// 创建 ServletServerHttpRequest 实例作为输入消息
		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
		// 获取参数的类型
		Type paramType = getHttpEntityType(parameter);
		if (paramType == null) {
			// 如果参数的类型为 null，则抛出异常
			throw new IllegalArgumentException("HttpEntity parameter '" + parameter.getParameterName() +
					"' in method " + parameter.getMethod() + " is not parameterized");
		}

		// 通过消息转换器读取请求体
		Object body = readWithMessageConverters(webRequest, parameter, paramType);
		// 如果参数类型是 RequestEntity，则创建 RequestEntity 实例
		if (RequestEntity.class == parameter.getParameterType()) {
			return new RequestEntity<>(body, inputMessage.getHeaders(),
					inputMessage.getMethod(), inputMessage.getURI());
		} else {
			// 否则，创建 HttpEntity 实例
			return new HttpEntity<>(body, inputMessage.getHeaders());
		}
	}

	@Nullable
	private Type getHttpEntityType(MethodParameter parameter) {
		// 检查参数类型是否可分配给 HttpEntity 类型
		Assert.isAssignable(HttpEntity.class, parameter.getParameterType());
		// 获取参数的泛型类型
		Type parameterType = parameter.getGenericParameterType();
		// 如果参数类型是 ParameterizedType
		if (parameterType instanceof ParameterizedType) {
			ParameterizedType type = (ParameterizedType) parameterType;
			if (type.getActualTypeArguments().length != 1) {
				// 如果实际类型参数的数量不是 1，则抛出异常
				throw new IllegalArgumentException("Expected single generic parameter on '" +
						parameter.getParameterName() + "' in method " + parameter.getMethod());
			}
			// 返回实际类型参数的第一个类型
			return type.getActualTypeArguments()[0];
		} else if (parameterType instanceof Class) {
			// 如果参数类型是 Class
			// 返回 Object 类型
			return Object.class;
		} else {
			// 其他情况返回 null
			return null;
		}
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		// 设置请求已处理标志为 true
		mavContainer.setRequestHandled(true);
		// 如果返回值为空，则直接返回
		if (returnValue == null) {
			return;
		}

		// 创建输入和输出消息
		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
		ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);

		// 断言返回值是 HttpEntity 类型
		Assert.isInstanceOf(HttpEntity.class, returnValue);
		HttpEntity<?> responseEntity = (HttpEntity<?>) returnValue;

		// 获取输出消息头和实体消息头
		HttpHeaders outputHeaders = outputMessage.getHeaders();
		HttpHeaders entityHeaders = responseEntity.getHeaders();

		if (!entityHeaders.isEmpty()) {
			// 如果实体消息头不为空，则将其添加到输出消息头
			entityHeaders.forEach((key, value) -> {
				// 如果 key 是 HttpHeaders.VARY 并且输出头包含 HttpHeaders.VARY
				if (HttpHeaders.VARY.equals(key) && outputHeaders.containsKey(HttpHeaders.VARY)) {
					// 获取要添加到 Vary 请求头的值列表
					List<String> values = getVaryRequestHeadersToAdd(outputHeaders, entityHeaders);
					// 如果值列表不为空
					if (!values.isEmpty()) {
						// 设置Http响应 Vary 请求头
						outputHeaders.setVary(values);
					}
				} else {
					// 否则，将键值对放入输出头中
					outputHeaders.put(key, value);
				}
			});
		}

		// 如果返回值是 ResponseEntity 类型
		if (responseEntity instanceof ResponseEntity) {
			// 获取返回状态码
			int returnStatus = ((ResponseEntity<?>) responseEntity).getStatusCodeValue();
			// 设置输出消息的状态码
			outputMessage.getServletResponse().setStatus(returnStatus);
			// 如果状态码是 200
			if (returnStatus == 200) {
				HttpMethod method = inputMessage.getMethod();
				// 如果是 GET 或者 HEAD 方法，并且资源未修改，则直接返回
				if ((HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method))
						&& isResourceNotModified(inputMessage, outputMessage)) {
					outputMessage.flush();
					return;
				}
			} else if (returnStatus / 100 == 3) {
				// 如果状态码以 3 开头
				String location = outputHeaders.getFirst("");
				// 如果有重定向地址，则保存 闪存 属性
				if (location != null) {
					saveFlashAttributes(mavContainer, webRequest, location);
				}
			}
		}

		// 使用消息转换器写入消息体
		writeWithMessageConverters(responseEntity.getBody(), returnType, inputMessage, outputMessage);

		// 确保即使没有写入消息体，头部也会被刷新
		outputMessage.flush();
	}

	private List<String> getVaryRequestHeadersToAdd(HttpHeaders responseHeaders, HttpHeaders entityHeaders) {
		// 获取实体头中的 Vary 头列表
		List<String> entityHeadersVary = entityHeaders.getVary();
		// 获取响应头中的 Vary 头列表
		List<String> vary = responseHeaders.get(HttpHeaders.VARY);
		// 如果响应头中有 Vary 头列表
		if (vary != null) {
			// 创建一个结果列表，初始化为实体头中的 Vary 头列表
			List<String> result = new ArrayList<>(entityHeadersVary);
			// 遍历响应头中的 Vary 头
			for (String header : vary) {
				// 将 Vary 头拆分成单个的值
				for (String existing : StringUtils.tokenizeToStringArray(header, ",")) {
					// 如果值为 "*"，则返回一个空列表
					if ("*".equals(existing)) {
						return Collections.emptyList();
					}
					// 遍历实体头中的 Vary 头
					for (String value : entityHeadersVary) {
						// 如果实体头中的 Vary 头与响应头中的 Vary 头匹配，则将其从结果列表中移除
						if (value.equalsIgnoreCase(existing)) {
							result.remove(value);
						}
					}
				}
			}
			// 返回结果列表
			return result;
		}
		// 如果响应头中没有 Vary 头列表，则直接返回实体头中的 Vary 头列表
		return entityHeadersVary;
	}

	private boolean isResourceNotModified(ServletServerHttpRequest request, ServletServerHttpResponse response) {
		// 创建 ServletWebRequest 对象
		ServletWebRequest servletWebRequest =
				new ServletWebRequest(request.getServletRequest(), response.getServletResponse());
		// 获取响应头部信息
		HttpHeaders responseHeaders = response.getHeaders();
		// 获取 ETag 和最后修改时间戳
		String etag = responseHeaders.getETag();
		long lastModifiedTimestamp = responseHeaders.getLastModified();
		// 如果请求方法是 GET 或 HEAD，则移除响应头中的 ETag 和 Last-Modified
		if (request.getMethod() == HttpMethod.GET || request.getMethod() == HttpMethod.HEAD) {
			responseHeaders.remove(HttpHeaders.ETAG);
			responseHeaders.remove(HttpHeaders.LAST_MODIFIED);
		}
		// 检查资源是否未被修改
		return servletWebRequest.checkNotModified(etag, lastModifiedTimestamp);
	}

	private void saveFlashAttributes(ModelAndViewContainer mav, NativeWebRequest request, String location) {
		// 设置重定向模型场景为 true
		mav.setRedirectModelScenario(true);
		// 获取模型对象
		ModelMap model = mav.getModel();
		// 如果模型对象是 RedirectAttributes 类型
		if (model instanceof RedirectAttributes) {
			// 获取闪存属性
			Map<String, ?> flashAttributes = ((RedirectAttributes) model).getFlashAttributes();
			// 如果闪存属性不为空
			if (!CollectionUtils.isEmpty(flashAttributes)) {
				// 获取原生 HttpServletRequest 和 HttpServletResponse
				HttpServletRequest req = request.getNativeRequest(HttpServletRequest.class);
				HttpServletResponse res = request.getNativeResponse(HttpServletResponse.class);
				// 如果请求不为空
				if (req != null) {
					// 将闪存属性放入输出闪存映射
					RequestContextUtils.getOutputFlashMap(req).putAll(flashAttributes);
					// 如果响应不为空
					if (res != null) {
						// 保存输出闪存映射
						RequestContextUtils.saveOutputFlashMap(location, req, res);
					}
				}
			}
		}
	}

	@Override
	protected Class<?> getReturnValueType(@Nullable Object returnValue, MethodParameter returnType) {
		// 如果返回值不为空，则返回其类
		if (returnValue != null) {
			return returnValue.getClass();
		} else {
			// 否则，获取返回类型的 HTTP 实体类型
			Type type = getHttpEntityType(returnType);
			// 如果获取到了类型，则使用该类型，否则默认为 Object 类型
			type = (type != null ? type : Object.class);
			// 使用返回类型和实体类型创建 ResolvableType，并将其转换为 Class 类型
			return ResolvableType.forMethodParameter(returnType, type).toClass();
		}
	}

}
