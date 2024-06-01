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

package org.springframework.web.method.support;

import org.springframework.context.MessageSource;
import org.springframework.core.*;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * {@link HandlerMethod} 的扩展，通过一系列 {@link HandlerMethodArgumentResolver}
 * 从当前 HTTP 请求中解析参数值，并调用底层方法。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.1
 */
public class InvocableHandlerMethod extends HandlerMethod {
	/**
	 * 空的方法参数
	 */
	private static final Object[] EMPTY_ARGS = new Object[0];

	/**
	 * 处理器方法参数解析程序组合
	 */
	private HandlerMethodArgumentResolverComposite resolvers = new HandlerMethodArgumentResolverComposite();

	/**
	 * 参数名称发现器
	 */
	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	/**
	 * Web数据绑定者工厂
	 */
	@Nullable
	private WebDataBinderFactory dataBinderFactory;


	/**
	 * 从 {@code HandlerMethod} 创建一个实例。
	 */
	public InvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}

	/**
	 * 使用 bean 实例和方法创建一个实例。
	 */
	public InvocableHandlerMethod(Object bean, Method method) {
		super(bean, method);
	}

	/**
	 * {@link #InvocableHandlerMethod(Object, Method)} 的变体，还接受一个 {@link MessageSource}，供子类使用。
	 *
	 * @since 5.3.10
	 */
	protected InvocableHandlerMethod(Object bean, Method method, @Nullable MessageSource messageSource) {
		super(bean, method, messageSource);
	}

	/**
	 * 使用给定的 bean 实例、方法名称和参数构造一个新的处理程序方法。
	 *
	 * @param bean           对象 bean
	 * @param methodName     方法名称
	 * @param parameterTypes 方法参数类型
	 * @throws NoSuchMethodException 如果找不到方法
	 */
	public InvocableHandlerMethod(Object bean, String methodName, Class<?>... parameterTypes)
			throws NoSuchMethodException {

		super(bean, methodName, parameterTypes);
	}


	/**
	 * 设置用于解析方法参数值的 {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}。
	 */
	public void setHandlerMethodArgumentResolvers(HandlerMethodArgumentResolverComposite argumentResolvers) {
		this.resolvers = argumentResolvers;
	}

	/**
	 * 设置 ParameterNameDiscoverer 以在需要时解析参数名称（例如，默认请求属性名称）。
	 * <p>默认值是 {@link org.springframework.core.DefaultParameterNameDiscoverer}。
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * 设置 {@link WebDataBinderFactory}，以便传递给参数解析器，允许它们为数据绑定和类型转换目的创建 {@link WebDataBinder}。
	 */
	public void setDataBinderFactory(WebDataBinderFactory dataBinderFactory) {
		this.dataBinderFactory = dataBinderFactory;
	}


	/**
	 * 在给定请求的上下文中解析其参数值后调用方法。
	 * <p>通常通过 {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers} 解析参数值。
	 * 但是，providedArgs 参数可以提供要直接使用的参数值，即无需参数解析。提供的参数值在参数解析器之前进行检查。
	 * <p>委托给 {@link #getMethodArgumentValues} 并调用 {@link #doInvoke} 以传递解析后的参数。
	 *
	 * @param request      当前请求
	 * @param mavContainer 此请求的 ModelAndViewContainer
	 * @param providedArgs 按类型匹配的“给定”参数，未解析
	 * @return 调用方法返回的原始值
	 * @throws Exception 如果找不到适当的参数解析器，或者如果方法引发异常
	 * @see #getMethodArgumentValues
	 * @see #doInvoke
	 */
	@Nullable
	public Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
								   Object... providedArgs) throws Exception {

		// 获取方法的参数值
		Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);

		// 如果日志级别为跟踪，则记录参数信息
		if (logger.isTraceEnabled()) {
			logger.trace("Arguments: " + Arrays.toString(args));
		}

		// 执行方法调用并返回结果
		return doInvoke(args);
	}

	/**
	 * 获取当前请求的方法参数值，检查提供的参数值并回退到配置的参数解析器。
	 * <p>结果数组将传递到 {@link #doInvoke}。
	 *
	 * @since 5.1.2
	 */
	protected Object[] getMethodArgumentValues(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
											   Object... providedArgs) throws Exception {

		// 获取方法的参数信息
		MethodParameter[] parameters = getMethodParameters();

		// 如果参数信息为空，则返回空参数数组
		if (ObjectUtils.isEmpty(parameters)) {
			return EMPTY_ARGS;
		}

		// 创建参数值数组
		Object[] args = new Object[parameters.length];

		// 遍历方法的参数信息
		for (int i = 0; i < parameters.length; i++) {
			MethodParameter parameter = parameters[i];

			// 初始化参数名发现器
			parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);

			// 查找已提供的参数值
			args[i] = findProvidedArgument(parameter, providedArgs);

			// 如果找到已提供的参数值，则继续下一个参数
			if (args[i] != null) {
				continue;
			}

			// 如果当前解析器不支持该参数，则抛出异常
			if (!this.resolvers.supportsParameter(parameter)) {
				throw new IllegalStateException(formatArgumentError(parameter, "No suitable resolver"));
			}

			try {
				// 解析参数值
				args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory);
			} catch (Exception ex) {
				// 如果解析出现异常，则记录错误信息并抛出异常
				// 留下堆栈跟踪以供稍后使用，实际上可能解析并处理异常...
				if (logger.isDebugEnabled()) {
					String exMsg = ex.getMessage();
					if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString())) {
						logger.debug(formatArgumentError(parameter, exMsg));
					}
				}
				throw ex;
			}
		}

		// 返回参数值数组
		return args;
	}

	/**
	 * 使用给定的参数值调用处理程序方法。
	 */
	@Nullable
	protected Object doInvoke(Object... args) throws Exception {
		Method method = getBridgedMethod();
		try {
			// 检查方法是否是 Kotlin 的挂起函数
			if (KotlinDetector.isSuspendingFunction(method)) {
				// 如果是 Kotlin 的挂起函数，则使用 CoroutinesUtils 调用
				return CoroutinesUtils.invokeSuspendingFunction(method, getBean(), args);
			}
			// 否则直接调用方法
			return method.invoke(getBean(), args);
		} catch (IllegalArgumentException ex) {
			// 参数异常处理
			assertTargetBean(method, getBean(), args);
			String text = (ex.getMessage() != null ? ex.getMessage() : "Illegal argument");
			throw new IllegalStateException(formatInvokeError(text, args), ex);
		} catch (InvocationTargetException ex) {
			// 处理反射调用的目标异常
			Throwable targetException = ex.getTargetException();
			if (targetException instanceof RuntimeException) {
				throw (RuntimeException) targetException;
			} else if (targetException instanceof Error) {
				throw (Error) targetException;
			} else if (targetException instanceof Exception) {
				throw (Exception) targetException;
			} else {
				throw new IllegalStateException(formatInvokeError("Invocation failure", args), targetException);
			}
		}
	}

}
