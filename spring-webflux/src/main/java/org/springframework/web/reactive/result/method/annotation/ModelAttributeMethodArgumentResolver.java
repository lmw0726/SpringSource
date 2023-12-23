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

package org.springframework.web.reactive.result.method.annotation;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 解析带有 {@code @ModelAttribute} 注解的方法参数。
 *
 * <p>模型属性从模型中获取，或者使用默认构造函数创建，然后添加到模型中。一旦创建，属性会通过数据绑定到请求（表单数据、查询参数）中进行填充。
 * 如果参数带有 {@code @javax.validation.Valid} 或 Spring 的 {@code @org.springframework.validation.annotation.Validated} 注解，还可能进行验证。
 *
 * <p>当此处理器以 {@code useDefaultResolution=true} 创建时，任何非简单类型的参数和返回值都被视为模型属性，无论是否存在 {@code @ModelAttribute} 注解。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.0
 */
public class ModelAttributeMethodArgumentResolver extends HandlerMethodArgumentResolverSupport {

	private final boolean useDefaultResolution;

	/**
	 * 类构造函数，带有默认解析模式标志。
	 *
	 * @param adapterRegistry      用于将其他响应式类型转换为 Mono 类型以及将 Mono 类型转换为其他响应式类型的适配器注册表
	 * @param useDefaultResolution 如果为 "true"，则非简单类型的方法参数和返回值都被视为模型属性，无论是否存在 {@code @ModelAttribute} 注解
	 */
	public ModelAttributeMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry,
												boolean useDefaultResolution) {
		super(adapterRegistry);
		this.useDefaultResolution = useDefaultResolution;
	}

	/**
	 * 检查方法参数是否支持。
	 *
	 * @param parameter 方法参数
	 * @return 如果参数带有 {@code @ModelAttribute} 注解，则返回 true；
	 * 如果使用默认解析模式并且参数类型不是简单属性类型，则返回 true；
	 * 否则返回 false
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
			// 如果参数带有 @ModelAttribute 注解，返回true
			return true;
		} else if (this.useDefaultResolution) {
			//如果使用默认解析模式，并且参数类型不是简单属性类型，返回true
			return checkParameterType(parameter, type -> !BeanUtils.isSimpleProperty(type));
		}
		return false;
	}

	/**
	 * 解析方法参数。
	 *
	 * @param parameter 方法参数
	 * @param context   绑定上下文
	 * @param exchange  服务器网络交换对象
	 * @return 返回 Mono 包含解析后的参数值
	 */
	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		// 获取方法参数的 ResolvableType
		ResolvableType type = ResolvableType.forMethodParameter(parameter);
		// 解析参数类型
		Class<?> resolvedType = type.resolve();
		// 获取参数类型的响应式适配器
		ReactiveAdapter adapter = (resolvedType != null ? getAdapterRegistry().getAdapter(resolvedType) : null);
		// 获取参数类型的泛型类型
		ResolvableType valueType = (adapter != null ? type.getGeneric() : type);

		// 确保不支持多值响应式类型包装器
		Assert.state(adapter == null || !adapter.isMultiValue(),
				() -> getClass().getSimpleName() + " does not support multi-value reactive type wrapper: " +
						parameter.getGenericParameterType());

		// 获取参数名称
		String name = ModelInitializer.getNameForParameter(parameter);
		// 准备属性的 Mono 对象
		Mono<?> valueMono = prepareAttributeMono(name, valueType, context, exchange);

		// unsafe(): 我们正在拦截，已经序列化的 Publisher 信号
		Sinks.One<BindingResult> bindingResultSink = Sinks.unsafe().one();

		// 将 BindingResult 放入模型中
		Map<String, Object> model = context.getModel().asMap();
		model.put(BindingResult.MODEL_KEY_PREFIX + name, bindingResultSink.asMono());

		return valueMono.flatMap(value -> {
			// 创建数据绑定器
			WebExchangeDataBinder binder = context.createDataBinder(exchange, value, name);
			return (bindingDisabled(parameter) ? Mono.empty() : bindRequestParameters(binder, exchange))
					.doOnError(bindingResultSink::tryEmitError)
					.doOnSuccess(aVoid -> {
						// 如果绑定未被禁用，则进行验证
						validateIfApplicable(binder, parameter);
						BindingResult bindingResult = binder.getBindingResult();
						// 将 BindingResult 和属性值放入模型中
						model.put(BindingResult.MODEL_KEY_PREFIX + name, bindingResult);
						model.put(name, value);
						// 忽略结果：已序列化和缓冲（不应该失败）
						bindingResultSink.tryEmitValue(bindingResult);
					})
					.then(Mono.fromCallable(() -> {
						// 获取绑定结果
						BindingResult errors = binder.getBindingResult();
						if (adapter != null) {
							// 如果存在适配器，则将结果转换为适配器支持的类型
							return adapter.fromPublisher(errors.hasErrors() ?
									Mono.error(new WebExchangeBindException(parameter, errors)) : valueMono);
						} else {
							// 如果不存在适配器，则检查是否有错误，并根据情况抛出 WebExchangeBindException 异常或返回值
							if (errors.hasErrors() && !hasErrorsArgument(parameter)) {
								throw new WebExchangeBindException(parameter, errors);
							}
							return value;
						}
					}));
		});
	}


	/**
	 * 根据 {@link ModelAttribute#binding} 注解属性，确定是否应禁用给定的 {@link MethodParameter} 的绑定。
	 *
	 * @since 5.2.15
	 */
	private boolean bindingDisabled(MethodParameter parameter) {
		// 获取参数上的 ModelAttribute 注解
		ModelAttribute modelAttribute = parameter.getParameterAnnotation(ModelAttribute.class);
		// 如果注解不为空且 binding 属性为 false，则返回 true（禁用绑定），否则返回 false（不禁用绑定）
		return (modelAttribute != null && !modelAttribute.binding());
	}


	/**
	 * 扩展点，用于将请求绑定到目标对象。
	 *
	 * @param binder   用于绑定的数据绑定器实例
	 * @param exchange 当前请求
	 * @since 5.2.6
	 */
	protected Mono<Void> bindRequestParameters(WebExchangeDataBinder binder, ServerWebExchange exchange) {
		return binder.bind(exchange);
	}

	/**
	 * 准备属性的 Mono 对象。
	 *
	 * @param attributeName 属性名称
	 * @param attributeType 属性类型的 ResolvableType
	 * @param context       绑定上下文
	 * @param exchange      服务器网络交换对象
	 * @return 返回准备好的属性 Mono 对象
	 */
	private Mono<?> prepareAttributeMono(String attributeName, ResolvableType attributeType,
										 BindingContext context, ServerWebExchange exchange) {

		// 从上下文的模型中获取属性
		Object attribute = context.getModel().asMap().get(attributeName);

		// 如果属性为空，则查找并移除响应式属性
		if (attribute == null) {
			attribute = findAndRemoveReactiveAttribute(context.getModel(), attributeName);
		}

		// 如果属性仍为空，则创建属性
		if (attribute == null) {
			return createAttribute(attributeName, attributeType.toClass(), context, exchange);
		}

		// 获取属性的响应式适配器
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(null, attribute);
		// 如果适配器不为空，则转换属性为 Mono
		if (adapter != null) {
			Assert.isTrue(!adapter.isMultiValue(), "Data binding only supports single-value async types");
			return Mono.from(adapter.toPublisher(attribute));
		} else {
			// 否则返回属性的 Mono 对象
			return Mono.justOrEmpty(attribute);
		}
	}

	/**
	 * 查找并移除响应式属性。
	 *
	 * @param model         模型对象
	 * @param attributeName 属性名称
	 * @return 如果找到响应式属性，则返回该属性值；否则返回 null
	 */
	@Nullable
	private Object findAndRemoveReactiveAttribute(Model model, String attributeName) {
		return model.asMap().entrySet().stream()
				.filter(entry -> {
					if (!entry.getKey().startsWith(attributeName)) {
						// 过滤掉名称和属性名称不同的属性值对
						return false;
					}
					// 获取值的响应式适配器
					ReactiveAdapter adapter = getAdapterRegistry().getAdapter(null, entry.getValue());
					if (adapter == null) {
						// 如果没有响应式适配器，则忽略
						return false;
					}
					String name = attributeName + ClassUtils.getShortName(adapter.getReactiveType());
					return entry.getKey().equals(name);
				})
				.findFirst()
				.map(entry -> {
					// 移除属性，因为将重新插入解析后的属性值
					model.asMap().remove(entry.getKey());
					return entry.getValue();
				})
				.orElse(null);
	}

	/**
	 * 创建属性。
	 *
	 * @param attributeName 属性名称
	 * @param clazz         属性的类
	 * @param context       绑定上下文
	 * @param exchange      服务器网络交换对象
	 * @return 返回一个 Mono，表示创建的属性
	 */
	private Mono<?> createAttribute(
			String attributeName, Class<?> clazz, BindingContext context, ServerWebExchange exchange) {
		//获取类构造器
		Constructor<?> ctor = BeanUtils.getResolvableConstructor(clazz);
		//通过构造器推断属性
		return constructAttribute(ctor, attributeName, context, exchange);
	}


	/**
	 * 构造属性。
	 *
	 * @param ctor          构造函数
	 * @param attributeName 属性名称
	 * @param context       绑定上下文
	 * @param exchange      服务器网络交换对象
	 * @return 返回一个 Mono，表示构造的属性
	 */
	private Mono<?> constructAttribute(Constructor<?> ctor, String attributeName,
									   BindingContext context, ServerWebExchange exchange) {

		if (ctor.getParameterCount() == 0) {
			// 单个默认构造函数 -> 明显是标准的 JavaBeans 排列。
			return Mono.just(BeanUtils.instantiateClass(ctor));
		}

		// 单个数据类构造函数 -> 从请求参数中解析构造函数参数。
		// 创建 WebExchangeDataBinder 对象，用于绑定请求参数到目标对象
		WebExchangeDataBinder binder = context.createDataBinder(exchange, null, attributeName);
		// 获取要绑定的值，并通过 map 操作进行处理
		return getValuesToBind(binder, exchange).map(bindValues -> {
			// 获取构造函数的参数名和参数类型
			String[] paramNames = BeanUtils.getParameterNames(ctor);
			Class<?>[] paramTypes = ctor.getParameterTypes();
			Object[] args = new Object[paramTypes.length];
			// 获取 Binder 的字段默认前缀和字段标记前缀
			String fieldDefaultPrefix = binder.getFieldDefaultPrefix();
			String fieldMarkerPrefix = binder.getFieldMarkerPrefix();
			// 遍历构造函数的参数
			for (int i = 0; i < paramNames.length; i++) {
				String paramName = paramNames[i];
				Class<?> paramType = paramTypes[i];
				// 获取要绑定的值
				Object value = bindValues.get(paramName);
				if (value == null) {
					// 如果值为空，尝试使用字段默认前缀获取值
					if (fieldDefaultPrefix != null) {
						value = bindValues.get(fieldDefaultPrefix + paramName);
					}
					// 如果值仍为空，且字段标记前缀不为空，检查字段标记前缀是否存在，若存在则使用 Binder 的空值
					if (value == null && fieldMarkerPrefix != null) {
						if (bindValues.get(fieldMarkerPrefix + paramName) != null) {
							value = binder.getEmptyValue(paramType);
						}
					}
				}
				// 将 List 类型的值转换为数组
				value = (value instanceof List ? ((List<?>) value).toArray() : value);
				// 创建 MethodParameter 对象
				MethodParameter methodParam = new MethodParameter(ctor, i);
				// 如果值为空且参数为 Optional 类型，使用 Optional.empty()，否则进行值的类型转换
				if (value == null && methodParam.isOptional()) {
					args[i] = (methodParam.getParameterType() == Optional.class ? Optional.empty() : null);
				} else {
					args[i] = binder.convertIfNecessary(value, paramTypes[i], methodParam);
				}
			}
			// 通过构造函数和参数创建对象实例
			return BeanUtils.instantiateClass(ctor, args);
		});
	}


	/**
	 * 获取用于数据绑定的值的受保护方法。默认情况下，此方法委托给 {@link WebExchangeDataBinder#getValuesToBind}。
	 *
	 * @param binder   正在使用的数据绑定器
	 * @param exchange 当前交换对象
	 * @return 绑定值的映射
	 * @since 5.3
	 */
	public Mono<Map<String, Object>> getValuesToBind(WebExchangeDataBinder binder, ServerWebExchange exchange) {
		return binder.getValuesToBind(exchange);
	}

	/**
	 * 检查方法参数中是否有 Errors 类型的参数。
	 *
	 * @param parameter 方法参数的描述符
	 * @return 如果方法参数中有 Errors 类型的参数，则返回 true；否则返回 false
	 */
	private boolean hasErrorsArgument(MethodParameter parameter) {
		int i = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
		return (paramTypes.length > i + 1 && Errors.class.isAssignableFrom(paramTypes[i + 1]));
	}

	/**
	 * 如果适用，则进行验证。
	 *
	 * @param binder    数据绑定器
	 * @param parameter 方法参数描述符
	 */
	private void validateIfApplicable(WebExchangeDataBinder binder, MethodParameter parameter) {
		for (Annotation ann : parameter.getParameterAnnotations()) {
			// 根据注解获取验证提示
			Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
			if (validationHints != null) {
				// 进行校验
				binder.validate(validationHints);
			}
		}
	}

}
