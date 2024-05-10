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
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.PushBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.Principal;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 解析基于 Servlet 的请求相关的方法参数。支持以下类型的值：
 * <ul>
 * <li>{@link WebRequest}
 * <li>{@link ServletRequest}
 * <li>{@link MultipartRequest}
 * <li>{@link HttpSession}
 * <li>{@link PushBuilder} (自 Spring 5.0 开始，适用于 Servlet 4.0)
 * <li>{@link Principal}，但仅当未注解时允许自定义解析器解析它，并回退到 {@link PrincipalMethodArgumentResolver}。
 * <li>{@link InputStream}
 * <li>{@link Reader}
 * <li>{@link HttpMethod} (自 Spring 4.0 开始)
 * <li>{@link Locale}
 * <li>{@link TimeZone} (自 Spring 4.0 开始)
 * <li>{@link java.time.ZoneId} (自 Spring 4.0 和 Java 8 开始)
 * </ul>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ServletRequestMethodArgumentResolver implements HandlerMethodArgumentResolver {
	/**
	 * 推送构建器类型
	 */
	@Nullable
	private static Class<?> pushBuilder;

	static {
		try {
			// 尝试加载 javax.servlet.http.PushBuilder 类
			pushBuilder = ClassUtils.forName("javax.servlet.http.PushBuilder",
					ServletRequestMethodArgumentResolver.class.getClassLoader());
		} catch (ClassNotFoundException ex) {
			// Servlet 4.0 PushBuilder not found - not supported for injection
			// 类未找到，则设置 pushBuilder 为 null
			pushBuilder = null;
		}
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		// 是否为 WebRequest 类型
		return (WebRequest.class.isAssignableFrom(paramType) ||
				// 是否为 ServletRequest 类型
				ServletRequest.class.isAssignableFrom(paramType) ||
				// 是否为 MultipartRequest 类型
				MultipartRequest.class.isAssignableFrom(paramType) ||
				// 是否为 HttpSession 类型
				HttpSession.class.isAssignableFrom(paramType) ||
				// 是否为 PushBuilder 类型（Servlet 4.0）
				(pushBuilder != null && pushBuilder.isAssignableFrom(paramType)) ||
				// 是否为 Principal 类型并且没有参数注解
				(Principal.class.isAssignableFrom(paramType) && !parameter.hasParameterAnnotations()) ||
				// 是否为 InputStream 类型
				InputStream.class.isAssignableFrom(paramType) ||
				// 是否为 Reader 类型
				Reader.class.isAssignableFrom(paramType) ||
				// 是否为 HttpMethod 类型
				HttpMethod.class == paramType ||
				// 是否为 Locale 类型
				Locale.class == paramType ||
				// 是否为 TimeZone 类型
				TimeZone.class == paramType ||
				// 是否为 ZoneId 类型
				ZoneId.class == paramType);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		Class<?> paramType = parameter.getParameterType();

		// 如果参数类型是 WebRequest / NativeWebRequest / ServletWebRequest
		if (WebRequest.class.isAssignableFrom(paramType)) {
			// 如果当前请求不是指定的类型
			if (!paramType.isInstance(webRequest)) {
				// 抛出异常
				throw new IllegalStateException(
						"Current request is not of type [" + paramType.getName() + "]: " + webRequest);
			}
			return webRequest;
		}

		// 如果参数类型是 ServletRequest / HttpServletRequest / MultipartRequest / MultipartHttpServletRequest
		if (ServletRequest.class.isAssignableFrom(paramType) || MultipartRequest.class.isAssignableFrom(paramType)) {
			// 返回原生的Http请求
			return resolveNativeRequest(webRequest, paramType);
		}

		// 对于后续的所有参数类型，都需要 HttpServletRequest
		return resolveArgument(paramType, resolveNativeRequest(webRequest, HttpServletRequest.class));
	}

	private <T> T resolveNativeRequest(NativeWebRequest webRequest, Class<T> requiredType) {
		T nativeRequest = webRequest.getNativeRequest(requiredType);
		// 如果获取的原生请求对象为 null，则抛出异常
		if (nativeRequest == null) {
			throw new IllegalStateException(
					"Current request is not of type [" + requiredType.getName() + "]: " + webRequest);
		}

		return nativeRequest;
	}

	@Nullable
	private Object resolveArgument(Class<?> paramType, HttpServletRequest request) throws IOException {
		if (HttpSession.class.isAssignableFrom(paramType)) {
			// 如果参数类型是 HttpSession，则返回当前请求的会话对象
			HttpSession session = request.getSession();
			if (session != null && !paramType.isInstance(session)) {
				// 如果会话对象存在，并且参数类型不是 HttpSession，则抛出异常
				throw new IllegalStateException(
						"Current session is not of type [" + paramType.getName() + "]: " + session);
			}
			return session;
		} else if (pushBuilder != null && pushBuilder.isAssignableFrom(paramType)) {
			// 如果支持 推送构建器 且参数类型为 推送构建器，则解析 推送构建器
			return PushBuilderDelegate.resolvePushBuilder(request, paramType);
		} else if (InputStream.class.isAssignableFrom(paramType)) {
			// 如果参数类型是 InputStream，则返回请求的输入流对象
			InputStream inputStream = request.getInputStream();
			if (inputStream != null && !paramType.isInstance(inputStream)) {
				// 如果输入流存在，而参数类型不是输入流，抛出异常
				throw new IllegalStateException(
						"Request input stream is not of type [" + paramType.getName() + "]: " + inputStream);
			}
			return inputStream;
		} else if (Reader.class.isAssignableFrom(paramType)) {
			// 如果参数类型是 Reader，则返回请求的 Reader 对象
			Reader reader = request.getReader();
			if (reader != null && !paramType.isInstance(reader)) {
				// 如果 读取器存在，而参数类型不是读取器，抛出异常
				throw new IllegalStateException(
						"Request body reader is not of type [" + paramType.getName() + "]: " + reader);
			}
			return reader;
		} else if (Principal.class.isAssignableFrom(paramType)) {
			// 如果参数类型是 Principal，则返回当前用户的主体对象
			Principal userPrincipal = request.getUserPrincipal();
			if (userPrincipal != null && !paramType.isInstance(userPrincipal)) {
				// 如果用户主体存在，而参数类型不是用户主体，抛出异常
				throw new IllegalStateException(
						"Current user principal is not of type [" + paramType.getName() + "]: " + userPrincipal);
			}
			return userPrincipal;
		} else if (HttpMethod.class == paramType) {
			// 如果参数类型是 HttpMethod，则返回请求的 HTTP 方法
			return HttpMethod.resolve(request.getMethod());
		} else if (Locale.class == paramType) {
			// 如果参数类型是 Locale，则返回请求的地区信息
			return RequestContextUtils.getLocale(request);
		} else if (TimeZone.class == paramType) {
			// 如果参数类型是 TimeZone，则返回请求的时区信息
			TimeZone timeZone = RequestContextUtils.getTimeZone(request);
			return (timeZone != null ? timeZone : TimeZone.getDefault());
		} else if (ZoneId.class == paramType) {
			// 如果参数类型是 ZoneId，则返回请求的 ZoneId 信息
			TimeZone timeZone = RequestContextUtils.getTimeZone(request);
			return (timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault());
		}

		// 不应该发生...
		throw new UnsupportedOperationException("Unknown parameter type: " + paramType.getName());
	}


	/**
	 * 避免在运行时对 Servlet API 4.0 产生硬依赖的内部类。
	 */
	private static class PushBuilderDelegate {

		@Nullable
		public static Object resolvePushBuilder(HttpServletRequest request, Class<?> paramType) {
			// 创建一个新的 PushBuilder 对象
			PushBuilder pushBuilder = request.newPushBuilder();
			if (pushBuilder != null && !paramType.isInstance(pushBuilder)) {
				// 如果 pushBuilder 不为空，且不是指定类型的实例，则抛出异常
				throw new IllegalStateException(
						"Current push builder is not of type [" + paramType.getName() + "]: " + pushBuilder);
			}
			return pushBuilder;

		}
	}

}
