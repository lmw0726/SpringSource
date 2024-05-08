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

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.method.ControllerAdviceBean;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 在执行 {@link RequestBodyAdvice} 和 {@link ResponseBodyAdvice} 时调用，
 * 每个实例可能（并且很可能）都被包装在 {@link org.springframework.web.method.ControllerAdviceBean ControllerAdviceBean} 中。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
class RequestResponseBodyAdviceChain implements RequestBodyAdvice, ResponseBodyAdvice<Object> {

	/**
	 * 请求体建言列表
	 */
	private final List<Object> requestBodyAdvice = new ArrayList<>(4);

	/**
	 * 响应体建言列表
	 */
	private final List<Object> responseBodyAdvice = new ArrayList<>(4);


	/**
	 * 从一个对象列表创建一个实例，这些对象可以是 {@code ControllerAdviceBean} 或 {@code RequestBodyAdvice} 类型。
	 */
	public RequestResponseBodyAdviceChain(@Nullable List<Object> requestResponseBodyAdvice) {
		this.requestBodyAdvice.addAll(getAdviceByType(requestResponseBodyAdvice, RequestBodyAdvice.class));
		this.responseBodyAdvice.addAll(getAdviceByType(requestResponseBodyAdvice, ResponseBodyAdvice.class));
	}

	@SuppressWarnings("unchecked")
	static <T> List<T> getAdviceByType(@Nullable List<Object> requestResponseBodyAdvice, Class<T> adviceType) {
		if (requestResponseBodyAdvice != null) {
			// 如果请求响应体建言不为空
			List<T> result = new ArrayList<>();
			for (Object advice : requestResponseBodyAdvice) {
				// 获取bean的类型
				Class<?> beanType = (advice instanceof ControllerAdviceBean ?
						((ControllerAdviceBean) advice).getBeanType() : advice.getClass());
				// 如果 bean类型 不为空，且 建言类型 是 bean类型 的子类或实现类
				if (beanType != null && adviceType.isAssignableFrom(beanType)) {
					// 将符合条件的 建言 添加到结果列表中
					result.add((T) advice);
				}
			}
			return result;
		}
		// 如果请求响应体建言为空，则返回空列表
		return Collections.emptyList();
	}


	@Override
	public boolean supports(MethodParameter param, Type type, Class<? extends HttpMessageConverter<?>> converterType) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage request, MethodParameter parameter,
										   Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {

		for (RequestBodyAdvice advice : getMatchingAdvice(parameter, RequestBodyAdvice.class)) {
			// 遍历匹配的RequestBodyAdvice列表
			if (advice.supports(parameter, targetType, converterType)) {
				// 如果当前 建言 支持当前参数、目标类型和转换器类型，调用 当前建言 的beforeBodyRead方法
				request = advice.beforeBodyRead(request, parameter, targetType, converterType);
			}
		}
		// 返回处理后的请求
		return request;
	}

	@Override
	public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
								Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

		for (RequestBodyAdvice advice : getMatchingAdvice(parameter, RequestBodyAdvice.class)) {
			// 遍历匹配的RequestBodyAdvice列表
			if (advice.supports(parameter, targetType, converterType)) {
				// 如果 当前建言 支持当前参数、目标类型和转换器类型，调用当前建言的afterBodyRead方法
				body = advice.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
			}
		}
		// 返回处理后的请求体
		return body;
	}

	@Override
	@Nullable
	public Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType, MediaType contentType,
								  Class<? extends HttpMessageConverter<?>> converterType,
								  ServerHttpRequest request, ServerHttpResponse response) {

		return processBody(body, returnType, contentType, converterType, request, response);
	}

	@Override
	@Nullable
	public Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage, MethodParameter parameter,
								  Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

		for (RequestBodyAdvice advice : getMatchingAdvice(parameter, RequestBodyAdvice.class)) {
			// 遍历匹配的RequestBodyAdvice列表
			if (advice.supports(parameter, targetType, converterType)) {
				// 如果 当前建言 支持当前参数、目标类型和转换器类型，调用当前建言的handleEmptyBody方法
				body = advice.handleEmptyBody(body, inputMessage, parameter, targetType, converterType);
			}
		}
		// 返回处理后的请求体
		return body;
	}


	@SuppressWarnings("unchecked")
	@Nullable
	private <T> Object processBody(@Nullable Object body, MethodParameter returnType, MediaType contentType,
								   Class<? extends HttpMessageConverter<?>> converterType,
								   ServerHttpRequest request, ServerHttpResponse response) {

		for (ResponseBodyAdvice<?> advice : getMatchingAdvice(returnType, ResponseBodyAdvice.class)) {
			// 遍历匹配的ResponseBodyAdvice列表
			if (advice.supports(returnType, converterType)) {
				// 如果 当前建言 支持当前返回类型和转换器类型，调用当前建言的beforeBodyWrite方法
				body = ((ResponseBodyAdvice<T>) advice).beforeBodyWrite((T) body, returnType,
						contentType, converterType, request, response);
			}
		}
		// 返回处理后的响应体
		return body;
	}

	@SuppressWarnings("unchecked")
	private <A> List<A> getMatchingAdvice(MethodParameter parameter, Class<? extends A> adviceType) {
		List<Object> availableAdvice = getAdvice(adviceType);
		if (CollectionUtils.isEmpty(availableAdvice)) {
			// 如果可用的建言为空，则返回空列表
			return Collections.emptyList();
		}
		List<A> result = new ArrayList<>(availableAdvice.size());
		for (Object advice : availableAdvice) {
			if (advice instanceof ControllerAdviceBean) {
				// 如果 建言 是ControllerAdviceBean类型
				ControllerAdviceBean adviceBean = (ControllerAdviceBean) advice;
				// 检查ControllerAdviceBean是否适用于参数的包含类
				if (!adviceBean.isApplicableToBeanType(parameter.getContainingClass())) {
					continue;
				}
				// 解析ControllerAdviceBean为其实际bean
				advice = adviceBean.resolveBean();
			}
			// 如果 建言类型 是 建言 的父类或实现类
			if (adviceType.isAssignableFrom(advice.getClass())) {
				// 将 建言 添加到结果列表中
				result.add((A) advice);
			}
		}
		return result;
	}

	private List<Object> getAdvice(Class<?> adviceType) {
		if (RequestBodyAdvice.class == adviceType) {
			// 如果adviceType是RequestBodyAdvice类型，则返回requestBodyAdvice
			return this.requestBodyAdvice;
		} else if (ResponseBodyAdvice.class == adviceType) {
			// 如果adviceType是ResponseBodyAdvice类型，则返回responseBodyAdvice
			return this.responseBodyAdvice;
		} else {
			// 如果adviceType既不是RequestBodyAdvice也不是ResponseBodyAdvice类型，则抛出异常
			throw new IllegalArgumentException("Unexpected adviceType: " + adviceType);
		}
	}

}
