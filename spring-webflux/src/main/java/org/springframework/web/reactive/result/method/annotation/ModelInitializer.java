/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.core.*;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用于辅助{@link RequestMappingHandlerAdapter}通过{@code @ModelAttribute}方法进行默认模型初始化的包私有类。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ModelInitializer {

	/**
	 * 控制器方法解析器
	 */
	private final ControllerMethodResolver methodResolver;

	/**
	 * 反应式适配器注册器
	 */
	private final ReactiveAdapterRegistry adapterRegistry;

	/**
	 * 构造函数，初始化{@link ModelInitializer}。
	 *
	 * @param methodResolver  ControllerMethodResolver实例，不能为空
	 * @param adapterRegistry ReactiveAdapterRegistry实例，不能为空
	 */
	public ModelInitializer(ControllerMethodResolver methodResolver, ReactiveAdapterRegistry adapterRegistry) {
		Assert.notNull(methodResolver, "ControllerMethodResolver is required");
		Assert.notNull(adapterRegistry, "ReactiveAdapterRegistry is required");
		this.methodResolver = methodResolver;
		this.adapterRegistry = adapterRegistry;
	}


	/**
	 * 根据（类型级）{@code @SessionAttributes}注解和{@code @ModelAttribute}方法初始化{@link org.springframework.ui.Model Model}。
	 *
	 * @param handlerMethod  目标控制器方法
	 * @param bindingContext 包含模型的上下文
	 * @param exchange       当前交换
	 * @return 当模型被填充时的{@code Mono}
	 */
	@SuppressWarnings("Convert2MethodRef")
	public Mono<Void> initModel(HandlerMethod handlerMethod, InitBinderBindingContext bindingContext,
								ServerWebExchange exchange) {

		// 获取所有标注有 @ModelAttribute 的方法
		List<InvocableHandlerMethod> modelMethods = this.methodResolver.getModelAttributeMethods(handlerMethod);

		// 获取会话属性处理器
		SessionAttributesHandler sessionAttributesHandler = this.methodResolver.getSessionAttributesHandler(handlerMethod);

		// 如果没有会话属性，直接调用模型属性方法并返回结果
		if (!sessionAttributesHandler.hasSessionAttributes()) {
			return invokeModelAttributeMethods(bindingContext, modelMethods, exchange);
		}

		// 如果存在会话属性，通过交换对象获取会话，并处理会话中的属性
		return exchange.getSession().flatMap(session -> {
			// 从会话属性处理器中获取会话中的属性，并将其合并到模型中
			Map<String, Object> attributes = sessionAttributesHandler.retrieveAttributes(session);
			bindingContext.getModel().mergeAttributes(attributes);

			// 设置会话上下文以便后续操作
			bindingContext.setSessionContext(sessionAttributesHandler, session);

			// 调用模型属性方法，并处理方法返回的结果
			return invokeModelAttributeMethods(bindingContext, modelMethods, exchange)
					.doOnSuccess(aVoid ->
							// 对于在会话中但不在模型中的属性，将其添加到模型中
							findModelAttributes(handlerMethod, sessionAttributesHandler).forEach(name -> {
								if (!bindingContext.getModel().containsAttribute(name)) {
									Object value = session.getRequiredAttribute(name);
									bindingContext.getModel().addAttribute(name, value);
								}
							}));
		});

	}

	/**
	 * 调用ModelAttribute方法的方法。
	 *
	 * @param bindingContext BindingContext实例
	 * @param modelMethods   InvocableHandlerMethod列表
	 * @param exchange       ServerWebExchange实例
	 * @return 一个Void类型的Mono
	 */
	private Mono<Void> invokeModelAttributeMethods(BindingContext bindingContext,
												   List<InvocableHandlerMethod> modelMethods, ServerWebExchange exchange) {

		// 创建一个存放方法调用结果的 Mono 列表
		List<Mono<HandlerResult>> resultList = new ArrayList<>();

		// 对模型方法进行迭代，并执行每个方法，将其结果添加到列表中
		modelMethods.forEach(invocable -> resultList.add(invocable.invoke(exchange, bindingContext)));

		// 使用 Mono.zip 将结果列表中的 Mono 对象进行组合，然后处理组合后的结果
		return Mono
				.zip(resultList, objectArray ->
						Arrays.stream(objectArray)
								// 对每个处理器结果调用 handleResult 方法，并将结果收集为列表
								.map(object -> handleResult(((HandlerResult) object), bindingContext))
								.collect(Collectors.toList()))
				// 扁平化处理 Mono 列表
				.flatMap(Mono::when);
	}


	/**
	 * 处理HandlerResult的方法。
	 *
	 * @param handlerResult  HandlerResult实例
	 * @param bindingContext BindingContext实例
	 * @return 一个Void类型的Mono
	 */
	private Mono<Void> handleResult(HandlerResult handlerResult, BindingContext bindingContext) {
		// 获取处理器返回的值
		Object value = handlerResult.getReturnValue();

		// 如果返回值不为 null
		if (value != null) {
			// 获取返回值的 ResolvableType
			ResolvableType type = handlerResult.getReturnType();
			// 通过适配器注册表获取与值对应的反应式适配器
			ReactiveAdapter adapter = this.adapterRegistry.getAdapter(type.resolve(), value);

			// 如果符合异步 Void 类型的条件
			if (isAsyncVoidType(type, adapter)) {
				// 将值转换为发布者，并返回一个 Mono
				return Mono.from(adapter.toPublisher(value));
			}

			// 获取属性名称
			String name = getAttributeName(handlerResult.getReturnTypeSource());
			// 将值放入模型中（如果模型中不存在该属性）
			bindingContext.getModel().asMap().putIfAbsent(name, value);
		}

		// 返回一个空的 Mono
		return Mono.empty();
	}


	/**
	 * 检查是否为异步void类型的方法。
	 *
	 * @param type    ResolvableType实例
	 * @param adapter 可为null的ReactiveAdapter实例
	 * @return 如果是异步void类型，则为true；否则为false
	 */
	private boolean isAsyncVoidType(ResolvableType type, @Nullable ReactiveAdapter adapter) {
		// 检查适配器是否存在且满足以下条件：
		// 适配器不为空且
		return (adapter != null
				// 要么适配器指示没有值，或者
				&& (adapter.isNoValue()
				// 泛型解析为 Void 类型
				|| type.resolveGeneric() == Void.class));

	}


	/**
	 * 获取属性名称的方法。
	 *
	 * @param param MethodParameter实例
	 * @return 属性名称字符串
	 */
	private String getAttributeName(MethodParameter param) {
		return Optional
				// 尝试获取参数上合并的 @ModelAttribute 注解
				.ofNullable(AnnotatedElementUtils.findMergedAnnotation(param.getAnnotatedElement(), ModelAttribute.class))
				// 过滤出注解值不为空的情况
				.filter(ann -> StringUtils.hasText(ann.value()))
				// 获取注解的值作为参数的名称，或者使用默认的参数名称
				.map(ModelAttribute::value)
				.orElseGet(() -> Conventions.getVariableNameForParameter(param));
	}


	/**
	 * 查找同时列在{@code @SessionAttributes}中的{@code @ModelAttribute}参数。
	 *
	 * @param handlerMethod            目标控制器方法
	 * @param sessionAttributesHandler SessionAttributesHandler实例
	 * @return 包含找到的参数名称的列表
	 */
	private List<String> findModelAttributes(HandlerMethod handlerMethod,
											 SessionAttributesHandler sessionAttributesHandler) {

		// 创建一个列表，用于存储符合条件的参数名称
		List<String> result = new ArrayList<>();

		// 遍历 handlerMethod 中的方法参数
		for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
			// 检查参数是否被 @ModelAttribute 注解标记
			if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
				// 获取参数的名称
				String name = getNameForParameter(parameter);
				// 获取参数的类型
				Class<?> paramType = parameter.getParameterType();
				// 检查参数是否同时被 @SessionAttributes 注解标记
				if (sessionAttributesHandler.isHandlerSessionAttribute(name, paramType)) {
					// 如果是，则将参数名称添加到结果列表中
					result.add(name);
				}
			}
		}
		// 返回符合条件的参数名称列表
		return result;
	}

	/**
	 * 根据 {@code @ModelAttribute} 参数注解（如果存在）或者基于参数类型的约定，
	 * 推断给定方法参数的模型属性名称。
	 *
	 * @param parameter 方法参数的描述符
	 * @return 推断出的名称
	 * @see Conventions#getVariableNameForParameter(MethodParameter)
	 */
	public static String getNameForParameter(MethodParameter parameter) {
		// 获取参数上的 ModelAttribute 注解
		ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
		// 获取注解中的值，如果注解为空则为 null
		String name = (ann != null ? ann.value() : null);
		// 如果注解中的值不为空，则返回注解中的值，否则使用参数的默认变量名
		return (StringUtils.hasText(name) ? name : Conventions.getVariableNameForParameter(parameter));
	}

}
