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

package org.springframework.web.reactive.result.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * {@code HandlerResultHandler} 处理带有 {@code @ResponseBody} 注解方法的返回值，使用 {@link HttpMessageWriter} 将内容写入请求或响应体中。
 *
 * <p>默认情况下，此结果处理程序的顺序设置为100。由于它检测到了 {@code @ResponseBody} 的存在，因此它应该排在寻找特定返回类型的结果处理程序之后。但请注意，此处理程序确实识别并明确忽略了 {@code ResponseEntity} 返回类型。
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ResponseBodyResultHandler extends AbstractMessageWriterResultHandler implements HandlerResultHandler {

	/**
	 * 基本构造函数使用默认的 {@link ReactiveAdapterRegistry}。
	 *
	 * @param writers  用于将内容序列化到响应体中的写入器
	 * @param resolver 用于确定请求的内容类型
	 */
	public ResponseBodyResultHandler(List<HttpMessageWriter<?>> writers, RequestedContentTypeResolver resolver) {
		this(writers, resolver, ReactiveAdapterRegistry.getSharedInstance());
	}

	/**
	 * 使用 {@link ReactiveAdapterRegistry} 实例的构造函数。
	 *
	 * @param writers  用于将内容序列化到响应体中的写入器
	 * @param resolver 用于确定请求的内容类型
	 * @param registry 用于适应反应式类型的注册表
	 */
	public ResponseBodyResultHandler(List<HttpMessageWriter<?>> writers,
									 RequestedContentTypeResolver resolver, ReactiveAdapterRegistry registry) {
		super(writers, resolver, registry);
		setOrder(100);
	}

	/**
	 * 确定此结果处理程序是否支持给定的处理结果。
	 *
	 * @param result 处理结果
	 * @return 如果支持，则为 true；否则为 false
	 */
	@Override
	public boolean supports(HandlerResult result) {
		// 获取方法返回类型的参数
		MethodParameter returnType = result.getReturnTypeSource();
		// 获取包含该返回类型的类
		Class<?> containingClass = returnType.getContainingClass();
		// 判断包含类上是否有 @ResponseBody 注解，或者方法上是否有 @ResponseBody 注解
		return (AnnotatedElementUtils.hasAnnotation(containingClass, ResponseBody.class) ||
				returnType.hasMethodAnnotation(ResponseBody.class));

	}

	/**
	 * 处理结果方法，将返回值写入到响应体中。
	 *
	 * @param exchange 当前的交换对象
	 * @param result   处理结果
	 * @return 表示完成或错误的 Mono
	 */
	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		// 获取处理结果中的返回值
		Object body = result.getReturnValue();
		// 获取返回类型的方法参数信息
		MethodParameter bodyTypeParameter = result.getReturnTypeSource();
		// 将返回值写入响应体中
		return writeBody(body, bodyTypeParameter, exchange);
	}

}
