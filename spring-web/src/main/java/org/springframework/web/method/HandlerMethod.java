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

package org.springframework.web.method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 封装关于处理程序方法的信息，包括一个 {@linkplain #getMethod() 方法} 和一个 {@linkplain #getBean() bean}。
 * 提供对方法参数、方法返回值、方法注解等的方便访问。
 *
 * <p>该类可以使用一个 bean 实例或一个 bean 名称（例如，延迟初始化的 bean、原型 bean）创建。
 * 使用 {@link #createWithResolvedBean()} 可以通过关联的 {@link BeanFactory} 解析一个 bean 实例来获取 {@code HandlerMethod} 实例。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 */
public class HandlerMethod {

	/**
	 * 可用于子类的记录器。
	 */
	protected static final Log logger = LogFactory.getLog(HandlerMethod.class);

	/**
	 * bean实例
	 */
	private final Object bean;

	/**
	 * bean工厂
	 */
	@Nullable
	private final BeanFactory beanFactory;

	/**
	 * 消息源
	 */
	@Nullable
	private final MessageSource messageSource;

	/**
	 * bean类型
	 */
	private final Class<?> beanType;

	/**
	 * 方法
	 */
	private final Method method;

	/**
	 * 桥接方法
	 */
	private final Method bridgedMethod;

	/**
	 * 方法参数数组
	 */
	private final MethodParameter[] parameters;

	/**
	 * 响应状态码
	 */
	@Nullable
	private HttpStatus responseStatus;

	/**
	 * 响应状态码原因
	 */
	@Nullable
	private String responseStatusReason;

	/**
	 * 处理程序解析的方法
	 */
	@Nullable
	private HandlerMethod resolvedFromHandlerMethod;

	/**
	 * 接口参数注解数组
	 */
	@Nullable
	private volatile List<Annotation[][]> interfaceParameterAnnotations;

	/**
	 * 描述
	 */
	private final String description;


	/**
	 * 从 bean 实例和方法创建一个实例。
	 */
	public HandlerMethod(Object bean, Method method) {
		this(bean, method, null);
	}

	/**
	 * {@link #HandlerMethod(Object, Method)} 的变体，还接受一个 {@link MessageSource} 供子类使用。
	 *
	 * @since 5.3.10
	 */
	protected HandlerMethod(Object bean, Method method, @Nullable MessageSource messageSource) {
		Assert.notNull(bean, "Bean is required");
		Assert.notNull(method, "Method is required");
		this.bean = bean;
		this.beanFactory = null;
		this.messageSource = messageSource;
		// 获取真实类型
		this.beanType = ClassUtils.getUserClass(bean);
		this.method = method;
		// 获取桥接方法
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
		// 将桥接方法设置为可进入方法
		ReflectionUtils.makeAccessible(this.bridgedMethod);
		// 获取方法参数列表
		this.parameters = initMethodParameters();
		// 评估响应状态码
		evaluateResponseStatus();
		// 初始化描述
		this.description = initDescription(this.beanType, this.method);
	}

	/**
	 * 通过 bean 实例、方法名称和参数类型创建一个实例。
	 *
	 * @throws NoSuchMethodException 当找不到方法时
	 */
	public HandlerMethod(Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		Assert.notNull(bean, "Bean is required");
		Assert.notNull(methodName, "Method name is required");
		this.bean = bean;
		this.beanFactory = null;
		this.messageSource = null;
		// 获取真实类型
		this.beanType = ClassUtils.getUserClass(bean);
		// 获取bean名称的方法
		this.method = bean.getClass().getMethod(methodName, parameterTypes);
		// 获取桥接方法
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(this.method);
		// 将桥接方法设置为可进入方法
		ReflectionUtils.makeAccessible(this.bridgedMethod);
		// 获取方法参数列表
		this.parameters = initMethodParameters();
		// 评估响应状态码
		evaluateResponseStatus();
		this.description = initDescription(this.beanType, this.method);
	}

	/**
	 * 通过 bean 名称、方法和 {@code BeanFactory} 创建一个实例。
	 * 稍后可以使用 {@link #createWithResolvedBean()} 方法重新创建带有初始化 bean 的 {@code HandlerMethod}。
	 */
	public HandlerMethod(String beanName, BeanFactory beanFactory, Method method) {
		this(beanName, beanFactory, null, method);
	}

	/**
	 * {@link #HandlerMethod(String, BeanFactory, Method)} 的变体，还接受一个 {@link MessageSource}。
	 */
	public HandlerMethod(
			String beanName, BeanFactory beanFactory,
			@Nullable MessageSource messageSource, Method method) {

		Assert.hasText(beanName, "Bean name is required");
		Assert.notNull(beanFactory, "BeanFactory is required");
		Assert.notNull(method, "Method is required");
		this.bean = beanName;
		this.beanFactory = beanFactory;
		this.messageSource = messageSource;
		// 根据bean名称获取bean类型
		Class<?> beanType = beanFactory.getType(beanName);
		if (beanType == null) {
			// 如果bean类型不存在，抛出异常
			throw new IllegalStateException("Cannot resolve bean type for bean with name '" + beanName + "'");
		}
		// 获取真实类型
		this.beanType = ClassUtils.getUserClass(beanType);
		this.method = method;
		// 获取桥接方法
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
		// 将桥接方法设置为可进入方法
		ReflectionUtils.makeAccessible(this.bridgedMethod);
		// 获取方法参数列表
		this.parameters = initMethodParameters();
		// 评估响应状态码
		evaluateResponseStatus();
		// 初始化描述
		this.description = initDescription(this.beanType, this.method);
	}

	/**
	 * 用于子类的复制构造函数。
	 */
	protected HandlerMethod(HandlerMethod handlerMethod) {
		Assert.notNull(handlerMethod, "HandlerMethod is required");
		this.bean = handlerMethod.bean;
		this.beanFactory = handlerMethod.beanFactory;
		this.messageSource = handlerMethod.messageSource;
		this.beanType = handlerMethod.beanType;
		this.method = handlerMethod.method;
		this.bridgedMethod = handlerMethod.bridgedMethod;
		this.parameters = handlerMethod.parameters;
		this.responseStatus = handlerMethod.responseStatus;
		this.responseStatusReason = handlerMethod.responseStatusReason;
		this.description = handlerMethod.description;
		this.resolvedFromHandlerMethod = handlerMethod.resolvedFromHandlerMethod;
	}

	/**
	 * 用解析后的处理器重新创建 HandlerMethod。
	 */
	private HandlerMethod(HandlerMethod handlerMethod, Object handler) {
		Assert.notNull(handlerMethod, "HandlerMethod is required");
		Assert.notNull(handler, "Handler object is required");
		this.bean = handler;
		this.beanFactory = handlerMethod.beanFactory;
		this.messageSource = handlerMethod.messageSource;
		this.beanType = handlerMethod.beanType;
		this.method = handlerMethod.method;
		this.bridgedMethod = handlerMethod.bridgedMethod;
		this.parameters = handlerMethod.parameters;
		this.responseStatus = handlerMethod.responseStatus;
		this.responseStatusReason = handlerMethod.responseStatusReason;
		this.resolvedFromHandlerMethod = handlerMethod;
		this.description = handlerMethod.description;
	}

	private MethodParameter[] initMethodParameters() {
		// 获取桥接方法的参数数量
		int count = this.bridgedMethod.getParameterCount();
		// 创建一个 MethodParameter 数组，用于存储处理方法的参数
		MethodParameter[] result = new MethodParameter[count];
		// 遍历参数数量
		for (int i = 0; i < count; i++) {
			// 使用 HandlerMethodParameter 类的实例化对象创建 MethodParameter，并添加到数组中
			result[i] = new HandlerMethodParameter(i);
		}
		// 返回存储参数的数组
		return result;
	}

	private void evaluateResponseStatus() {
		// 获取方法上的 ResponseStatus 注解
		ResponseStatus annotation = getMethodAnnotation(ResponseStatus.class);
		// 如果方法上没有 ResponseStatus 注解，则尝试获取类级别上的 ResponseStatus 注解
		if (annotation == null) {
			annotation = AnnotatedElementUtils.findMergedAnnotation(getBeanType(), ResponseStatus.class);
		}
		// 如果获取到 ResponseStatus 注解
		if (annotation != null) {
			// 获取注解中的 reason 属性值
			String reason = annotation.reason();
			// 解析 reason 属性值，使用消息源进行国际化处理
			String resolvedReason = (StringUtils.hasText(reason) && this.messageSource != null ?
					this.messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale()) :
					reason);

			// 获取注解中的 code 属性值和解析后的 reason 属性值，分别赋值给响应状态码和原因
			this.responseStatus = annotation.code();
			this.responseStatusReason = resolvedReason;
		}
	}

	private static String initDescription(Class<?> beanType, Method method) {
		// 创建 StringJoiner 对象，用于拼接参数类型名
		StringJoiner joiner = new StringJoiner(", ", "(", ")");
		// 遍历方法的参数类型，将参数类型的简单类名添加到 StringJoiner 中
		for (Class<?> paramType : method.getParameterTypes()) {
			joiner.add(paramType.getSimpleName());
		}
		// 返回拼接后的字符串，格式为：beanType.getName() + "#" + method.getName() + 参数类型列表
		return beanType.getName() + "#" + method.getName() + joiner.toString();
	}


	/**
	 * 返回此处理器方法的 bean。
	 */
	public Object getBean() {
		return this.bean;
	}

	/**
	 * 返回此处理器方法的方法。
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * 返回此处理器方法的处理器类型。
	 * <p>请注意，如果 bean 类型是 CGLIB 生成的类，则返回原始用户定义的类。
	 */
	public Class<?> getBeanType() {
		return this.beanType;
	}

	/**
	 * 如果 bean 方法是桥接方法，则此方法返回桥接的（用户定义的）方法。
	 * 否则，它返回与 {@link #getMethod()} 相同的方法。
	 */
	protected Method getBridgedMethod() {
		return this.bridgedMethod;
	}

	/**
	 * 返回此处理器方法的方法参数。
	 */
	public MethodParameter[] getMethodParameters() {
		return this.parameters;
	}

	/**
	 * 返回指定的响应状态（如果有）。
	 *
	 * @see ResponseStatus#code()
	 * @since 4.3.8
	 */
	@Nullable
	protected HttpStatus getResponseStatus() {
		return this.responseStatus;
	}

	/**
	 * 返回关联的响应状态原因（如果有）。
	 *
	 * @see ResponseStatus#reason()
	 * @since 4.3.8
	 */
	@Nullable
	protected String getResponseStatusReason() {
		return this.responseStatusReason;
	}

	/**
	 * 返回 HandlerMethod 的返回类型。
	 */
	public MethodParameter getReturnType() {
		return new HandlerMethodParameter(-1);
	}

	/**
	 * 返回实际的返回值类型。
	 */
	public MethodParameter getReturnValueType(@Nullable Object returnValue) {
		return new ReturnValueMethodParameter(returnValue);
	}

	/**
	 * 如果方法返回类型是 void，返回 {@code true}，否则返回 {@code false}。
	 */
	public boolean isVoid() {
		return Void.TYPE.equals(getReturnType().getParameterType());
	}

	/**
	 * 返回底层方法上的单个注解，如果在给定方法本身找不到注解，则遍历其超方法。
	 * <p>从 Spring Framework 4.2.2 开始，还支持具有属性覆盖的<em>合并</em>复合注解。
	 *
	 * @param annotationType 要在方法上进行内省的注解类型
	 * @return 注解，如果没有找到则返回 {@code null}
	 * @see AnnotatedElementUtils#findMergedAnnotation
	 */
	@Nullable
	public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
		return AnnotatedElementUtils.findMergedAnnotation(this.method, annotationType);
	}

	/**
	 * 返回参数是否声明了给定的注解类型。
	 *
	 * @param annotationType 要查找的注解类型
	 * @see AnnotatedElementUtils#hasAnnotation
	 * @since 4.3
	 */
	public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
		return AnnotatedElementUtils.hasAnnotation(this.method, annotationType);
	}

	/**
	 * 返回此 HandlerMethod 实例是通过 {@link #createWithResolvedBean()} 解析的 HandlerMethod。
	 */
	@Nullable
	public HandlerMethod getResolvedFromHandlerMethod() {
		return this.resolvedFromHandlerMethod;
	}

	/**
	 * 如果提供的实例包含一个 bean 名称而不是一个对象实例，
	 * 则在创建并返回 {@link HandlerMethod} 之前解析 bean 名称。
	 */
	public HandlerMethod createWithResolvedBean() {
		// 将处理器对象设置为 bean实例
		Object handler = this.bean;
		// 如果 bean实例 是字符串类型，则需要解析成具体的 bean 对象
		if (this.bean instanceof String) {
			// 确保 BeanFactory 不为空
			Assert.state(this.beanFactory != null, "Cannot resolve bean name without BeanFactory");
			// 将 bean 强制转换为字符串类型
			String beanName = (String) this.bean;
			// 通过 Bean工厂 获取具体的 bean 对象
			handler = this.beanFactory.getBean(beanName);
		}
		// 创建并返回 HandlerMethod 对象，其中包含了当前 ControllerAdviceBean 对象和处理器对象
		return new HandlerMethod(this, handler);
	}

	/**
	 * 返回此处理器方法的简短表示，用于日志消息目的。
	 *
	 * @since 4.3
	 */
	public String getShortLogMessage() {
		return getBeanType().getName() + "#" + this.method.getName() +
				"[" + this.method.getParameterCount() + " args]";
	}


	private List<Annotation[][]> getInterfaceParameterAnnotations() {
		// 获取接口方法的参数注解列表
		List<Annotation[][]> parameterAnnotations = this.interfaceParameterAnnotations;
		// 如果接口方法参数注解列表为空，则进行初始化
		if (parameterAnnotations == null) {
			parameterAnnotations = new ArrayList<>();
			// 遍历当前方法所在类的所有接口
			for (Class<?> ifc : ClassUtils.getAllInterfacesForClassAsSet(this.method.getDeclaringClass())) {
				// 遍历接口中的方法
				for (Method candidate : ifc.getMethods()) {
					// 如果当前方法是重写（Override）的方法，则将其参数注解列表添加到参数注解列表中
					if (isOverrideFor(candidate)) {
						parameterAnnotations.add(candidate.getParameterAnnotations());
					}
				}
			}
			// 将接口方法参数注解列表保存到成员变量中
			this.interfaceParameterAnnotations = parameterAnnotations;
		}
		// 返回接口方法参数注解列表
		return parameterAnnotations;
	}

	private boolean isOverrideFor(Method candidate) {
		// 判断候选方法是否和当前方法相匹配
		if (!candidate.getName().equals(this.method.getName()) ||
				candidate.getParameterCount() != this.method.getParameterCount()) {
			// 方法名不相同 或 参数数量不相同，返回false
			return false;
		}
		// 当前方法的参数类型数组
		Class<?>[] paramTypes = this.method.getParameterTypes();
		// 比较当前方法和候选方法的参数类型是否一致
		if (Arrays.equals(candidate.getParameterTypes(), paramTypes)) {
			// 参数类型一致，返回true
			return true;
		}
		// 参数类型不一致，逐个比较参数的实际类型是否一致
		for (int i = 0; i < paramTypes.length; i++) {
			if (paramTypes[i] !=
					ResolvableType.forMethodParameter(candidate, i, this.method.getDeclaringClass()).resolve()) {
				// 获取当前方法参数的实际类型，如果和候选方法的参数类型不一致，则返回false
				return false;
			}
		}
		// 参数类型一致，返回true
		return true;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HandlerMethod)) {
			return false;
		}
		HandlerMethod otherMethod = (HandlerMethod) other;
		return (this.bean.equals(otherMethod.bean) && this.method.equals(otherMethod.method));
	}

	@Override
	public int hashCode() {
		return (this.bean.hashCode() * 31 + this.method.hashCode());
	}

	@Override
	public String toString() {
		return this.description;
	}


	// 在 "InvocableHandlerMethod" 子类变体中使用的支持方法。

	/**
	 * 查找提供的参数。
	 *
	 * @param parameter    方法参数
	 * @param providedArgs 提供的参数数组
	 * @return 如果找到匹配的参数，则返回该参数，否则返回 null
	 */
	@Nullable
	protected static Object findProvidedArgument(MethodParameter parameter, @Nullable Object... providedArgs) {
		// 如果提供的参数不为空
		if (!ObjectUtils.isEmpty(providedArgs)) {
			// 遍历提供的参数列表
			for (Object providedArg : providedArgs) {
				// 如果参数的类型与当前方法参数的类型兼容，则返回该参数
				if (parameter.getParameterType().isInstance(providedArg)) {
					return providedArg;
				}
			}
		}
		// 如果提供的参数列表为空，或者没有找到匹配的参数，则返回null
		return null;
	}

	/**
	 * 格式化参数错误信息。
	 *
	 * @param param   方法参数
	 * @param message 错误信息
	 * @return 格式化后的错误信息
	 */
	protected static String formatArgumentError(MethodParameter param, String message) {
		return "Could not resolve parameter [" + param.getParameterIndex() + "] in " +
				param.getExecutable().toGenericString() + (StringUtils.hasText(message) ? ": " + message : "");
	}

	/**
	 * 断言目标 bean 类是给定方法声明的类的实例。在某些情况下，请求处理时实际的控制器实例可能是 JDK 动态代理
	 * (例如延迟初始化、原型 bean 等)。需要代理的 {@code @Controller} 应优先使用基于类的代理机制。
	 *
	 * @param method     方法对象
	 * @param targetBean 目标 bean 实例
	 * @param args       方法参数数组
	 * @throws IllegalStateException 如果目标 bean 类不是方法声明的类的实例
	 */
	protected void assertTargetBean(Method method, Object targetBean, Object[] args) {
		// 获取方法声明的类和目标bean的类
		Class<?> methodDeclaringClass = method.getDeclaringClass();
		Class<?> targetBeanClass = targetBean.getClass();

		// 如果方法声明的类不是目标bean类的子类，则抛出异常
		if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
			String text = "The mapped handler method class '" + methodDeclaringClass.getName() +
					"' is not an instance of the actual controller bean class '" +
					targetBeanClass.getName() + "'. If the controller requires proxying " +
					"(e.g. due to @Transactional), please use class-based proxying.";
			throw new IllegalStateException(formatInvokeError(text, args));
		}
	}

	protected String formatInvokeError(String text, Object[] args) {
		String formattedArgs = IntStream.range(0, args.length)
				.mapToObj(i -> (args[i] != null ?
						"[" + i + "] [type=" + args[i].getClass().getName() + "] [value=" + args[i] + "]" :
						"[" + i + "] [null]"))
				.collect(Collectors.joining(",\n", " ", " "));
		return text + "\n" +
				"Controller [" + getBeanType().getName() + "]\n" +
				"Method [" + getBridgedMethod().toGenericString() + "] " +
				"with argument values:\n" + formattedArgs;
	}


	/**
	 * 具有 HandlerMethod 特定行为的 MethodParameter。
	 */
	protected class HandlerMethodParameter extends SynthesizingMethodParameter {
		/**
		 * 组合注解数组
		 */
		@Nullable
		private volatile Annotation[] combinedAnnotations;

		/**
		 * 构造方法，根据参数索引创建 HandlerMethodParameter 实例。
		 *
		 * @param index 参数索引
		 */
		public HandlerMethodParameter(int index) {
			super(HandlerMethod.this.bridgedMethod, index);
		}

		/**
		 * 拷贝构造方法，创建 HandlerMethodParameter 的副本。
		 *
		 * @param original 要复制的 HandlerMethodParameter 实例
		 */
		protected HandlerMethodParameter(HandlerMethodParameter original) {
			super(original);
		}

		/**
		 * 获取方法。
		 *
		 * @return 方法对象
		 */
		@Override
		@NonNull
		public Method getMethod() {
			return HandlerMethod.this.bridgedMethod;
		}

		/**
		 * 获取包含类。
		 *
		 * @return 包含类的类型
		 */
		@Override
		public Class<?> getContainingClass() {
			return HandlerMethod.this.getBeanType();
		}

		/**
		 * 获取方法上的注解。
		 *
		 * @param annotationType 注解类型
		 * @param <T>            注解类型的类型参数
		 * @return 注解对象
		 */
		@Override
		public <T extends Annotation> T getMethodAnnotation(Class<T> annotationType) {
			return HandlerMethod.this.getMethodAnnotation(annotationType);
		}

		/**
		 * 判断方法上是否具有指定类型的注解。
		 *
		 * @param annotationType 注解类型
		 * @param <T>            注解类型的类型参数
		 * @return 如果存在返回 true，否则返回 false
		 */
		@Override
		public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
			return HandlerMethod.this.hasMethodAnnotation(annotationType);
		}

		/**
		 * 获取参数上的注解。
		 *
		 * @return 注解数组
		 */
		@Override
		public Annotation[] getParameterAnnotations() {
			// 声明一个注解数组来存储合并后的注解
			Annotation[] anns = this.combinedAnnotations;

			// 如果合并后的注解数组为空
			if (anns == null) {
				// 获取参数的注解
				anns = super.getParameterAnnotations();
				// 获取参数的索引
				int index = getParameterIndex();
				// 如果索引大于等于0
				if (index >= 0) {
					// 遍历接口参数注解的二维数组
					for (Annotation[][] ifcAnns : getInterfaceParameterAnnotations()) {
						// 如果索引小于二维数组的长度
						if (index < ifcAnns.length) {
							// 获取当前索引处的注解数组
							Annotation[] paramAnns = ifcAnns[index];
							// 如果注解数组的长度大于0
							if (paramAnns.length > 0) {
								// 创建一个新的列表来存储合并后的注解
								List<Annotation> merged = new ArrayList<>(anns.length + paramAnns.length);
								// 添加原有的注解到列表中
								merged.addAll(Arrays.asList(anns));
								// 遍历当前索引处的注解数组
								for (Annotation paramAnn : paramAnns) {
									boolean existingType = false;
									// 检查原有注解中是否存在相同类型的注解
									for (Annotation ann : anns) {
										if (ann.annotationType() == paramAnn.annotationType()) {
											// 如果注解类型和参数注解类型相同，则跳出当前循环
											existingType = true;
											break;
										}
									}
									// 如果不存在相同类型的注解
									if (!existingType) {
										// 将新的注解添加到列表中
										merged.add(adaptAnnotation(paramAnn));
									}
								}
								// 将合并后的注解列表转换为数组
								anns = merged.toArray(new Annotation[0]);
							}
						}
					}
				}
				// 将合并后的注解数组存储到成员变量中
				this.combinedAnnotations = anns;
			}
			// 返回合并后的注解数组
			return anns;
		}

		/**
		 * 克隆当前实例。
		 *
		 * @return 当前实例的副本
		 */
		@Override
		public HandlerMethodParameter clone() {
			return new HandlerMethodParameter(this);
		}
	}


	/**
	 * 基于实际返回值的 HandlerMethod 返回类型的 MethodParameter。
	 */
	private class ReturnValueMethodParameter extends HandlerMethodParameter {
		/**
		 * 返回值类型
		 */
		@Nullable
		private final Class<?> returnValueType;

		/**
		 * 构造方法，根据返回值创建 ReturnValueMethodParameter 实例。
		 *
		 * @param returnValue 方法的返回值，可以为 null
		 */
		public ReturnValueMethodParameter(@Nullable Object returnValue) {
			super(-1);
			this.returnValueType = (returnValue != null ? returnValue.getClass() : null);
		}

		/**
		 * 拷贝构造方法，创建 ReturnValueMethodParameter 的副本。
		 *
		 * @param original 要复制的 ReturnValueMethodParameter 实例
		 */
		protected ReturnValueMethodParameter(ReturnValueMethodParameter original) {
			super(original);
			this.returnValueType = original.returnValueType;
		}

		/**
		 * 获取参数类型。
		 *
		 * @return 返回值类型，如果未指定则返回父类的参数类型
		 */
		@Override
		public Class<?> getParameterType() {
			return (this.returnValueType != null ? this.returnValueType : super.getParameterType());
		}

		/**
		 * 克隆当前实例。
		 *
		 * @return 当前实例的副本
		 */
		@Override
		public ReturnValueMethodParameter clone() {
			return new ReturnValueMethodParameter(this);
		}
	}

}
