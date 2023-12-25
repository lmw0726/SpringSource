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

package org.springframework.web.reactive.result.method;

import org.springframework.core.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@link HandlerMethod} 的扩展，通过一组 {@link HandlerMethodArgumentResolver} 从当前 HTTP 请求中解析参数值并调用底层方法。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class InvocableHandlerMethod extends HandlerMethod {

	/**
	 * 声明了一个 Mono 对象，表示空参数列表
	 */
	private static final Mono<Object[]> EMPTY_ARGS = Mono.just(new Object[0]);

	/**
	 * 声明了一个常量对象，表示不存在参数值
	 */
	private static final Object NO_ARG_VALUE = new Object();

	/**
	 * 处理方法参数解析器组合 对象，用于处理处理方法参数解析器的组合
	 */
	private final HandlerMethodArgumentResolverComposite resolvers = new HandlerMethodArgumentResolverComposite();

	/**
	 * 参数名发现器，默认为 DefaultParameterNameDiscoverer
	 */
	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	/**
	 * 反应式适配器注册表，默认使用 ReactiveAdapterRegistry 的共享实例
	 */
	private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();


	/**
	 * 从 {@code HandlerMethod} 创建实例。
	 *
	 * @param handlerMethod 要创建实例的 {@code HandlerMethod}。
	 */
	public InvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}

	/**
	 * 从 bean 实例和方法创建实例。
	 *
	 * @param bean   bean 实例。
	 * @param method 要使用的方法。
	 */
	public InvocableHandlerMethod(Object bean, Method method) {
		super(bean, method);
	}

	/**
	 * 配置用于在 {@code ServerWebExchange} 中解析方法参数值的参数解析器。
	 *
	 * @param resolvers 参数解析器列表。
	 */
	public void setArgumentResolvers(List<? extends HandlerMethodArgumentResolver> resolvers) {
		this.resolvers.addResolvers(resolvers);
	}

	/**
	 * 返回配置的参数解析器。
	 *
	 * @return 参数解析器列表
	 */
	public List<HandlerMethodArgumentResolver> getResolvers() {
		return this.resolvers.getResolvers();
	}

	/**
	 * 设置用于在需要时解析参数名称（例如默认请求属性名称）的 ParameterNameDiscoverer。
	 * 默认是 DefaultParameterNameDiscoverer。
	 *
	 * @param nameDiscoverer 参数名称发现器
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer nameDiscoverer) {
		this.parameterNameDiscoverer = nameDiscoverer;
	}

	/**
	 * 返回配置的参数名称发现器。
	 *
	 * @return 参数名称发现器
	 */
	public ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}

	/**
	 * 配置反应式适配器注册表。在控制器完全处理响应并与异步 void 返回值结合使用的情况下需要此项。
	 * 默认情况下，这是一个带有默认设置的 ReactiveAdapterRegistry。
	 *
	 * @param registry 反应式适配器注册表
	 */
	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
		this.reactiveAdapterRegistry = registry;
	}

	/**
	 * 为给定的交换执行方法。
	 *
	 * @param exchange       当前交换
	 * @param bindingContext 要使用的绑定上下文
	 * @param providedArgs   用于按类型匹配的可选参数值列表
	 * @return 一个包含 {@link HandlerResult} 的 Mono
	 */
	@SuppressWarnings("KotlinInternalInJava")
	public Mono<HandlerResult> invoke(
			ServerWebExchange exchange, BindingContext bindingContext, Object... providedArgs) {

		// 使用给定的参数值调用处理器方法，并返回一个 Mono 对象
		return getMethodArgumentValues(exchange, bindingContext, providedArgs).flatMap(args -> {
			Object value;
			try {
				Method method = getBridgedMethod();

				// 判断方法是否为 Kotlin 的挂起函数
				if (KotlinDetector.isSuspendingFunction(method)) {
					// 如果是挂起函数，则使用 CoroutinesUtils.invokeSuspendingFunction 方法调用
					value = CoroutinesUtils.invokeSuspendingFunction(method, getBean(), args);
				} else {
					// 否则使用反射调用处理器方法
					value = method.invoke(getBean(), args);
				}
			} catch (IllegalArgumentException ex) {
				// 处理参数错误的异常情况
				assertTargetBean(getBridgedMethod(), getBean(), args);
				String text = (ex.getMessage() != null ? ex.getMessage() : "Illegal argument");
				return Mono.error(new IllegalStateException(formatInvokeError(text, args), ex));
			} catch (InvocationTargetException ex) {
				// 处理处理器方法内部抛出的异常
				return Mono.error(ex.getTargetException());
			} catch (Throwable ex) {
				// 处理其他异常情况
				// 不太可能到达这里，但必须处理...
				return Mono.error(new IllegalStateException(formatInvokeError("Invocation failure", args), ex));
			}

			// 获取处理器方法的返回状态码
			HttpStatus status = getResponseStatus();
			if (status != null) {
				// 如果存在返回状态码，则设置响应的状态码
				exchange.getResponse().setStatusCode(status);
			}

			// 获取处理器方法的返回参数类型
			MethodParameter returnType = getReturnType();
			ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(returnType.getParameterType());

			// 判断返回值是否为空或者是异步 Void 类型
			boolean asyncVoid = isAsyncVoidReturnType(returnType, adapter);

			// 判断返回值是否已经在其他地方处理过，如果是，则直接返回空 Mono
			if ((value == null || asyncVoid) && isResponseHandled(args, exchange)) {
				return (asyncVoid ? Mono.from(adapter.toPublisher(value)) : Mono.empty());
			}

			// 创建 HandlerResult 对象，用于封装处理器方法的执行结果
			HandlerResult result = new HandlerResult(this, value, returnType, bindingContext);
			return Mono.just(result);
		});
	}

	/**
	 * 从提供的参数中获取方法参数的值。
	 *
	 * @param exchange       当前的 ServerWebExchange
	 * @param bindingContext 绑定上下文
	 * @param providedArgs   提供的参数值列表
	 * @return 一个 Mono，包含方法参数的对象数组
	 */
	private Mono<Object[]> getMethodArgumentValues(
			ServerWebExchange exchange, BindingContext bindingContext, Object... providedArgs) {

		// 获取处理器方法的所有参数
		MethodParameter[] parameters = getMethodParameters();

		// 如果参数为空，则返回一个空的 Mono<Object[]> 对象
		if (ObjectUtils.isEmpty(parameters)) {
			return EMPTY_ARGS;
		}

		// 创建一个用于存储参数值的 Mono 列表
		List<Mono<Object>> argMonos = new ArrayList<>(parameters.length);

		// 遍历处理器方法的每个参数
		for (MethodParameter parameter : parameters) {
			// 初始化参数名发现器
			parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);

			// 查找是否有提供的参数值
			Object providedArg = findProvidedArgument(parameter, providedArgs);
			if (providedArg != null) {
				// 如果存在提供的参数值，则将其添加到 Mono 列表中
				argMonos.add(Mono.just(providedArg));
				continue;
			}

			// 如果参数解析器不支持当前参数类型，则返回一个错误的 Mono 对象
			if (!this.resolvers.supportsParameter(parameter)) {
				return Mono.error(new IllegalStateException(
						formatArgumentError(parameter, "No suitable resolver")));
			}

			try {
				// 使用参数解析器解析参数值，并添加到 Mono 列表中
				argMonos.add(this.resolvers.resolveArgument(parameter, bindingContext, exchange)
						.defaultIfEmpty(NO_ARG_VALUE)
						.doOnError(ex -> logArgumentErrorIfNecessary(exchange, parameter, ex)));
			} catch (Exception ex) {
				// 捕获异常情况，并记录日志
				logArgumentErrorIfNecessary(exchange, parameter, ex);
				argMonos.add(Mono.error(ex));
			}
		}

		// 使用 Mono.zip 将参数值的 Mono 列表合并为一个 Mono<Object[]> 对象，其中包含了参数值的流
		return Mono.zip(argMonos, values ->
				Stream.of(values).map(value -> value != NO_ARG_VALUE ? value : null).toArray());
	}

	/**
	 * 如果有必要，记录参数错误。
	 *
	 * @param exchange  当前的 ServerWebExchange
	 * @param parameter 方法参数的描述
	 * @param ex        异常对象
	 */
	private void logArgumentErrorIfNecessary(ServerWebExchange exchange, MethodParameter parameter, Throwable ex) {
		// 如果错误未处理，留下堆栈跟踪信息...
		String exMsg = ex.getMessage();
		if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString())) {
			if (logger.isDebugEnabled()) {
				logger.debug(exchange.getLogPrefix() + formatArgumentError(parameter, exMsg));
			}
		}
	}

	/**
	 * 判断方法返回类型是否为异步的 void。
	 *
	 * @param returnType 方法返回类型的描述
	 * @param adapter    可能用于处理异步情况的适配器
	 * @return 如果是异步的 void 返回 true，否则返回 false
	 */
	private static boolean isAsyncVoidReturnType(MethodParameter returnType, @Nullable ReactiveAdapter adapter) {
		// 检查适配器是否存在且支持空值
		if (adapter != null && adapter.supportsEmpty()) {
			// 如果适配器返回值为无值，则返回 true
			if (adapter.isNoValue()) {
				return true;
			}
			// 获取方法参数的类型信息
			Type parameterType = returnType.getGenericParameterType();
			// 如果参数类型是 ParameterizedType（参数化类型）
			if (parameterType instanceof ParameterizedType) {
				ParameterizedType type = (ParameterizedType) parameterType;
				// 如果参数化类型只有一个实际类型参数
				if (type.getActualTypeArguments().length == 1) {
					// 返回是否参数类型为 Void
					return Void.class.equals(type.getActualTypeArguments()[0]);
				}
			}
		}
		// 如果不满足条件，则返回 false
		return false;

	}

	/**
	 * 检查是否已处理响应。
	 *
	 * @param args     方法参数数组
	 * @param exchange 当前的 ServerWebExchange
	 * @return 如果已处理响应返回 true，否则返回 false
	 */
	private boolean isResponseHandled(Object[] args, ServerWebExchange exchange) {
		// 检查是否设置了响应状态码或者响应已被标记为未修改
		if (getResponseStatus() != null || exchange.isNotModified()) {
			return true;
		}

		// 检查方法参数中是否包含 ServerHttpResponse 或者 ServerWebExchange 对象
		for (Object arg : args) {
			if (arg instanceof ServerHttpResponse || arg instanceof ServerWebExchange) {
				return true;
			}
		}

		// 如果未设置响应状态码且方法参数中不包含 ServerHttpResponse 或 ServerWebExchange 对象，则返回 false
		return false;

	}

}
