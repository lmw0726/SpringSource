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
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;

/**
 * 允许在执行 {@code @ResponseBody} 或 {@code ResponseEntity} 控制器方法之后，
 * 在使用 {@code HttpMessageConverter} 写入响应体之前自定义响应。
 *
 * <p>实现可以直接注册到 {@code RequestMappingHandlerAdapter} 和 {@code ExceptionHandlerExceptionResolver}，
 * 或者更可能地使用 {@code @ControllerAdvice} 注解进行注解，这样它们将被两者自动检测到。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 * @param <T> 响应体类型
 */
public interface ResponseBodyAdvice<T> {

	/**
	 * 判断此组件是否支持给定的控制器方法返回类型和选择的 {@code HttpMessageConverter} 类型。
	 * @param returnType 控制器方法的返回类型
	 * @param converterType 所选的转换器类型
	 * @return 如果应调用 {@link #beforeBodyWrite} 则返回 {@code true}；否则返回 {@code false}
	 */
	boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType);

	/**
	 * 在选择了 {@code HttpMessageConverter} 并在调用其写入方法之前调用。
	 * @param body 要写入的响应体
	 * @param returnType 控制器方法的返回类型
	 * @param selectedContentType 通过内容协商选择的内容类型
	 * @param selectedConverterType 选择用于写入响应的转换器类型
	 * @param request 当前请求
	 * @param response 当前响应
	 * @return 传入的响应体或经过修改（可能是新的）的实例
	 */
	@Nullable
	T beforeBodyWrite(@Nullable T body, MethodParameter returnType, MediaType selectedContentType,
					  Class<? extends HttpMessageConverter<?>> selectedConverterType,
					  ServerHttpRequest request, ServerHttpResponse response);

}
