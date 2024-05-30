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

package org.springframework.web.method.annotation;

import org.springframework.core.ExceptionDepthComparator;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 在给定的类中查找{@linkplain ExceptionHandler @ExceptionHandler}方法，
 * 包括其所有超类，并帮助解析给定的{@link Exception}到给定{@link Method}支持的异常类型。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 */
public class ExceptionHandlerMethodResolver {

	/**
	 * 用于选择{@code @ExceptionHandler}方法的过滤器。
	 */
	public static final MethodFilter EXCEPTION_HANDLER_METHODS = method ->
			AnnotatedElementUtils.hasAnnotation(method, ExceptionHandler.class);

	/**
	 * 表示没有匹配异常处理程序方法的常量。
	 */
	private static final Method NO_MATCHING_EXCEPTION_HANDLER_METHOD;

	static {
		try {
			// 尝试获取名为"noMatchingExceptionHandler"的方法对象
			NO_MATCHING_EXCEPTION_HANDLER_METHOD =
					ExceptionHandlerMethodResolver.class.getDeclaredMethod("noMatchingExceptionHandler");
		} catch (NoSuchMethodException ex) {
			// 如果方法未找到，则抛出异常
			throw new IllegalStateException("Expected method not found: " + ex);
		}
	}

	/**
	 * 用于缓存异常处理方法的映射。
	 */
	private final Map<Class<? extends Throwable>, Method> mappedMethods = new HashMap<>(16);

	/**
	 * 用于缓存异常处理方法的查找结果，以提高解析效率。
	 */
	private final Map<Class<? extends Throwable>, Method> exceptionLookupCache = new ConcurrentReferenceHashMap<>(16);


	/**
	 * 查找给定类型中的{@link ExceptionHandler}方法的构造函数。
	 *
	 * @param handlerType 要内省的类型
	 */
	public ExceptionHandlerMethodResolver(Class<?> handlerType) {
		// 遍历处理器类中所有带有异常处理注解的方法
		for (Method method : MethodIntrospector.selectMethods(handlerType, EXCEPTION_HANDLER_METHODS)) {
			// 检测方法中定义的异常映射
			for (Class<? extends Throwable> exceptionType : detectExceptionMappings(method)) {
				// 将异常类型与对应的方法添加到异常映射中
				addExceptionMapping(exceptionType, method);
			}
		}
	}


	/**
	 * 首先从{@code @ExceptionHandler}注解中提取异常映射，
	 * 然后作为备用从方法签名中提取。
	 */
	@SuppressWarnings("unchecked")
	private List<Class<? extends Throwable>> detectExceptionMappings(Method method) {
		// 创建一个列表用于存储异常类型
		List<Class<? extends Throwable>> result = new ArrayList<>();
		// 检测方法中的注解异常映射
		detectAnnotationExceptionMappings(method, result);
		// 如果结果为空，则尝试从方法的参数类型中检测异常类型
		if (result.isEmpty()) {
			for (Class<?> paramType : method.getParameterTypes()) {
				// 如果参数类型是Throwable的子类，则将其添加到结果列表中
				if (Throwable.class.isAssignableFrom(paramType)) {
					result.add((Class<? extends Throwable>) paramType);
				}
			}
		}
		// 如果结果仍然为空，则抛出异常
		if (result.isEmpty()) {
			throw new IllegalStateException("No exception types mapped to " + method);
		}
		// 返回异常类型列表
		return result;
	}

	private void detectAnnotationExceptionMappings(Method method, List<Class<? extends Throwable>> result) {
		// 查找方法上合并的ExceptionHandler注解
		ExceptionHandler ann = AnnotatedElementUtils.findMergedAnnotation(method, ExceptionHandler.class);
		// 断言注解不为空，如果为空则抛出异常
		Assert.state(ann != null, "No ExceptionHandler annotation");
		// 将注解中的异常类型添加到结果列表中
		result.addAll(Arrays.asList(ann.value()));
	}

	private void addExceptionMapping(Class<? extends Throwable> exceptionType, Method method) {
		// 将异常类型与对应的方法添加到映射中，并返回之前与该异常类型关联的方法（如果存在）
		Method oldMethod = this.mappedMethods.put(exceptionType, method);
		if (oldMethod != null && !oldMethod.equals(method)) {
			// 如果存在之前关联的方法，并且该方法与当前方法不相等，则抛出异常
			throw new IllegalStateException("Ambiguous @ExceptionHandler method mapped for [" +
					exceptionType + "]: {" + oldMethod + ", " + method + "}");
		}
	}

	/**
	 * 是否包含任何异常映射。
	 */
	public boolean hasExceptionMappings() {
		return !this.mappedMethods.isEmpty();
	}

	/**
	 * 查找用于处理给定异常的{@link Method}。
	 * <p>如果找到多个匹配项，则使用{@link ExceptionDepthComparator}。
	 *
	 * @param exception 异常
	 * @return 处理异常的方法，如果没有找到则为{@code null}
	 */
	@Nullable
	public Method resolveMethod(Exception exception) {
		return resolveMethodByThrowable(exception);
	}

	/**
	 * 查找用于处理给定Throwable的{@link Method}。
	 * <p>如果找到多个匹配项，则使用{@link ExceptionDepthComparator}。
	 *
	 * @param exception 异常
	 * @return 处理异常的方法，如果没有找到则为{@code null}
	 * @since 5.0
	 */
	@Nullable
	public Method resolveMethodByThrowable(Throwable exception) {
		// 根据异常类型解析方法
		Method method = resolveMethodByExceptionType(exception.getClass());
		// 如果未找到与异常类型对应的方法，则尝试获取异常的原因并解析方法
		if (method == null) {
			Throwable cause = exception.getCause();
			// 如果异常有原因，则根据原因解析方法
			if (cause != null) {
				method = resolveMethodByThrowable(cause);
			}
		}
		// 返回解析得到的方法
		return method;
	}

	/**
	 * 查找用于处理给定异常类型的{@link Method}。
	 * 如果{@link Exception}实例不可用（例如工具），则这可能会有用。
	 * <p>如果找到多个匹配项，则使用{@link ExceptionDepthComparator}。
	 *
	 * @param exceptionType 异常类型
	 * @return 处理异常的方法，如果没有找到则为{@code null}
	 */
	@Nullable
	public Method resolveMethodByExceptionType(Class<? extends Throwable> exceptionType) {
		// 从异常查找缓存中获取与异常类型关联的方法
		Method method = this.exceptionLookupCache.get(exceptionType);
		if (method == null) {
			// 如果未找到缓存中的方法，则从映射中获取对应的方法，并将其放入缓存中
			method = getMappedMethod(exceptionType);
			this.exceptionLookupCache.put(exceptionType, method);
		}
		// 如果获取到的方法是noMatchingExceptionHandler，则返回null；否则返回该方法
		return (method != NO_MATCHING_EXCEPTION_HANDLER_METHOD ? method : null);
	}

	/**
	 * 返回与给定异常类型映射的{@link Method}，如果没有则返回{@link #NO_MATCHING_EXCEPTION_HANDLER_METHOD}。
	 */
	private Method getMappedMethod(Class<? extends Throwable> exceptionType) {
		// 创建一个列表用于存储与异常类型相关联的映射异常类型
		List<Class<? extends Throwable>> matches = new ArrayList<>();
		// 遍历已映射方法的异常类型
		for (Class<? extends Throwable> mappedException : this.mappedMethods.keySet()) {
			// 如果已映射的异常类型是当前异常类型的子类，则将其添加到匹配列表中
			if (mappedException.isAssignableFrom(exceptionType)) {
				matches.add(mappedException);
			}
		}
		// 如果存在匹配的映射异常类型
		if (!matches.isEmpty()) {
			// 如果匹配数量大于1，则按照异常类型的深度进行排序
			if (matches.size() > 1) {
				matches.sort(new ExceptionDepthComparator(exceptionType));
			}
			// 返回与深度最小的映射异常类型关联的方法
			return this.mappedMethods.get(matches.get(0));
		} else {
			// 如果不存在匹配的映射异常类型，则返回特殊标志
			return NO_MATCHING_EXCEPTION_HANDLER_METHOD;
		}
	}

	/**
	 * 用于{@link #NO_MATCHING_EXCEPTION_HANDLER_METHOD}常量。
	 */
	@SuppressWarnings("unused")
	private void noMatchingExceptionHandler() {
	}

}
