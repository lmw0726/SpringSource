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
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * 允许在请求体被读取并转换为对象之前自定义请求，并且也允许在结果对象被传递到控制器方法
 * 作为 {@code @RequestBody} 或 {@code HttpEntity} 方法参数之前进行处理。
 *
 * <p>此契约的实现可以直接注册到 {@code RequestMappingHandlerAdapter}，
 * 或者更可能地通过 {@code @ControllerAdvice} 进行注解，在这种情况下，它们会被自动检测。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface RequestBodyAdvice {

	/**
	 * 首先调用以确定此拦截器是否适用。
	 *
	 * @param methodParameter 方法参数
	 * @param targetType      目标类型，不一定与方法参数类型相同，例如 {@code HttpEntity<String>}。
	 * @param converterType   所选的转换器类型
	 * @return 是否应该调用此拦截器
	 */
	boolean supports(MethodParameter methodParameter, Type targetType,
					 Class<? extends HttpMessageConverter<?>> converterType);

	/**
	 * 在请求体被读取并转换之前第二次调用。
	 *
	 * @param inputMessage  请求
	 * @param parameter     目标方法参数
	 * @param targetType    目标类型，不一定与方法参数类型相同，例如 {@code HttpEntity<String>}。
	 * @param converterType 用于反序列化体的转换器
	 * @return 输入请求或新实例（永远不为 {@code null}）
	 */
	HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
									Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException;

	/**
	 * 在请求体转换为对象后第三次（也是最后一次）调用。
	 *
	 * @param body          在第一个建议被调用之前设置为转换器对象
	 * @param inputMessage  请求
	 * @param parameter     目标方法参数
	 * @param targetType    目标类型，不一定与方法参数类型相同，例如 {@code HttpEntity<String>}。
	 * @param converterType 用于反序列化体的转换器
	 * @return 相同的 body 或新实例
	 */
	Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
						 Type targetType, Class<? extends HttpMessageConverter<?>> converterType);

	/**
	 * 如果 body 为空时第二次（也是最后一次）调用。
	 *
	 * @param body          通常在第一个建议被调用之前设置为 {@code null}
	 * @param inputMessage  请求
	 * @param parameter     方法参数
	 * @param targetType    目标类型，不一定与方法参数类型相同，例如 {@code HttpEntity<String>}。
	 * @param converterType 所选的转换器类型
	 * @return 要使用的值，或 {@code null}，然后如果参数是必需的，则可能引发 {@code HttpMessageNotReadableException}
	 */
	@Nullable
	Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage, MethodParameter parameter,
						   Type targetType, Class<? extends HttpMessageConverter<?>> converterType);


}
