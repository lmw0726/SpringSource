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

package org.springframework.web.reactive.result.method.annotation;

import reactor.core.publisher.Mono;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ServerWebExchange;

/**
 * 解析 {@link Errors} 或 {@link BindingResult} 方法参数。
 *
 * <p>{@code Errors} 参数预期出现在方法签名中模型属性的后面。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public class ErrorsMethodArgumentResolver extends HandlerMethodArgumentResolverSupport {

	public ErrorsMethodArgumentResolver(ReactiveAdapterRegistry registry) {
		super(registry);
	}

	/**
	 * 检查方法参数是否支持。
	 * @param parameter 方法参数
	 * @return 如果参数类型是 Errors 或其子类，则返回 true，否则返回 false
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return checkParameterType(parameter, Errors.class::isAssignableFrom);
	}

	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		Object errors = getErrors(parameter, context);

		// 初始时，ModelAttributeMethodArgumentResolver 将 Errors/BindingResult 添加为 Mono 到模型中，
		// 即使它在控制器方法中不能被声明为这样。这样做是为了在此处启用早期参数解析。
		// 当 Mono 实际完成时，它将用实际值替换模型中的 Mono。

		if (Mono.class.isAssignableFrom(errors.getClass())) {
			// 如果是 Mono 类型，则将其转换为 Object 类型
			return ((Mono<?>) errors).cast(Object.class);
		} else if (Errors.class.isAssignableFrom(errors.getClass())) {
			// 如果是 Errors 类型，则包装为 Mono 并返回
			return Mono.just(errors);
		} else {
			// 抛出异常，指明意外的 Errors/BindingResult 类型
			throw new IllegalStateException("Unexpected Errors/BindingResult type: " + errors.getClass().getName());
		}
	}

	/**
	 * 获取错误实体类
	 * @param parameter 方法参数
	 * @param context 绑定上下文
	 * @return 错误实体类
	 */
	private Object getErrors(MethodParameter parameter, BindingContext context) {
		// 确保 Errors 参数紧跟在模型属性参数后面声明
		Assert.isTrue(parameter.getParameterIndex() > 0,
				"Errors argument must be declared immediately after a model attribute argument");

		// 获取模型属性参数的索引
		int index = parameter.getParameterIndex() - 1;
		// 使用 SynthesizingMethodParameter 创建模型属性参数
		MethodParameter attributeParam = SynthesizingMethodParameter.forExecutable(parameter.getExecutable(), index);
		// 获取模型属性参数的类型的异步适配器
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(attributeParam.getParameterType());

		// 确保不能同时使用异步类型包装器声明 @ModelAttribute 和 Errors/BindingResult 参数
		Assert.state(adapter == null, "An @ModelAttribute and an Errors/BindingResult argument " +
				"cannot both be declared with an async type wrapper. " +
				"Either declare the @ModelAttribute without an async wrapper type or " +
				"handle a WebExchangeBindException error signal through the async type.");

		// 获取模型属性参数上的 ModelAttribute 注解
		ModelAttribute ann = attributeParam.getParameterAnnotation(ModelAttribute.class);
		// 获取模型属性参数的名称，如果未指定名称，则使用参数的默认变量名
		String name = (ann != null && StringUtils.hasText(ann.name()) ? ann.name() :
				Conventions.getVariableNameForParameter(attributeParam));
		// 从上下文的模型中获取以指定名称为前缀的属性值
		Object errors = context.getModel().asMap().get(BindingResult.MODEL_KEY_PREFIX + name);

		// 确保 Errors/BindingResult 参数不为空
		Assert.state(errors != null, () -> "An Errors/BindingResult argument is expected " +
				"immediately after the @ModelAttribute argument to which it applies. " +
				"For @RequestBody and @RequestPart arguments, please declare them with a reactive " +
				"type wrapper and use its onError operators to handle WebExchangeBindException: " +
				parameter.getMethod());

		return errors;
	}

}
