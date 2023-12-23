/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 通过使用兼容的 {@code HttpMessageReader} 读取请求体来解析使用 {@code @RequestBody} 注解的方法参数。
 *
 * <p>{@code @RequestBody} 方法参数如果使用 {@code @javax.validation.Valid} 或
 * {@link org.springframework.validation.annotation.Validated} 进行了注解，也会进行验证。
 * 验证失败会导致 {@link ServerWebInputException}。
 *
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RequestBodyMethodArgumentResolver extends AbstractMessageReaderArgumentResolver {

	public RequestBodyMethodArgumentResolver(List<HttpMessageReader<?>> readers, ReactiveAdapterRegistry registry) {
		super(readers, registry);
	}

	/**
	 * 检查是否支持给定的方法参数
	 *
	 * @param parameter 方法参数
	 * @return 如果方法参数带有@RequestBody注解则返回true，否则返回false
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 方法参数上有@RequestBody注解
		return parameter.hasParameterAnnotation(RequestBody.class);
	}


	/**
	 * 解析方法参数
	 *
	 * @param param          方法参数
	 * @param bindingContext 绑定上下文
	 * @param exchange       服务器Web交换对象
	 * @return 解析后的参数对象的Mono
	 */
	@Override
	public Mono<Object> resolveArgument(
			MethodParameter param, BindingContext bindingContext, ServerWebExchange exchange) {
		// 获取 RequestBody 注解
		RequestBody ann = param.getParameterAnnotation(RequestBody.class);
		Assert.state(ann != null, "No RequestBody annotation");
		// 读取请求体并返回结果
		return readBody(param, ann.required(), bindingContext, exchange);
	}
}
