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

package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 定义了搜索与元数据相关的方法的算法，进行全面搜索，包括接口和父类，
 * 并且处理参数化方法以及接口和基于类的代理中常见的场景。
 *
 * <p>通常（但不一定）用于查找带注解的处理方法。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 4.2.3
 */
public final class MethodIntrospector {

	private MethodIntrospector() {
	}


	/**
	 * 基于相关元数据查找指定目标类型的方法。
	 * <p>调用者通过 {@link MetadataLookup} 参数定义感兴趣的方法，
	 * 允许将关联的元数据收集到结果映射中。
	 * @param targetType 要搜索方法的目标类型
	 * @param metadataLookup 一个 {@link MetadataLookup} 回调，用于检查感兴趣的方法，
	 * 返回非空元数据与给定方法关联，如果没有匹配则返回 {@code null}
	 * @return 关联其元数据的所选方法（按照检索顺序），
	 * 若无匹配则返回空映射
	 */
	public static <T> Map<Method, T> selectMethods(Class<?> targetType, final MetadataLookup<T> metadataLookup) {
		final Map<Method, T> methodMap = new LinkedHashMap<>();
		Set<Class<?>> handlerTypes = new LinkedHashSet<>();
		Class<?> specificHandlerType = null;

		if (!Proxy.isProxyClass(targetType)) {
			specificHandlerType = ClassUtils.getUserClass(targetType);
			handlerTypes.add(specificHandlerType);
		}
		handlerTypes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetType));

		for (Class<?> currentHandlerType : handlerTypes) {
			final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);

			ReflectionUtils.doWithMethods(currentHandlerType, method -> {
				Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
				T result = metadataLookup.inspect(specificMethod);
				if (result != null) {
					Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
					if (bridgedMethod == specificMethod || metadataLookup.inspect(bridgedMethod) == null) {
						methodMap.put(specificMethod, result);
					}
				}
			}, ReflectionUtils.USER_DECLARED_METHODS);
		}

		return methodMap;
	}

	/**
	 * 基于过滤器查找指定目标类型的方法。
	 * <p>调用者通过 {@code MethodFilter} 参数定义感兴趣的方法。
	 * @param targetType 要搜索方法的目标类型
	 * @param methodFilter 一个 {@code MethodFilter}，用于识别感兴趣的处理方法
	 * @return 选中的方法集合，若无匹配则返回空集合
	 */
	public static Set<Method> selectMethods(Class<?> targetType, final ReflectionUtils.MethodFilter methodFilter) {
		return selectMethods(targetType,
				(MetadataLookup<Boolean>) method -> (methodFilter.matches(method) ? Boolean.TRUE : null)).keySet();
	}

	/**
	 * 选择目标类型上可调用的方法：如果目标类型实际上暴露了给定方法，则返回该方法本身，
	 * 否则返回目标类型的接口之一或目标类型自身对应的方法。
	 * <p>优先匹配用户声明的接口，因为它们可能包含与目标类方法对应的相关元数据。
	 * @param method 要检查的方法
	 * @param targetType 要搜索方法的目标类型（通常是基于接口的 JDK 代理）
	 * @return 目标类型上对应的可调用方法
	 * @throws IllegalStateException 如果给定方法在指定目标类型上不可调用（通常因代理不匹配）
	 */
	public static Method selectInvocableMethod(Method method, Class<?> targetType) {
		if (method.getDeclaringClass().isAssignableFrom(targetType)) {
			return method;
		}
		try {
			String methodName = method.getName();
			Class<?>[] parameterTypes = method.getParameterTypes();
			for (Class<?> ifc : targetType.getInterfaces()) {
				try {
					return ifc.getMethod(methodName, parameterTypes);
				}
				catch (NoSuchMethodException ex) {
					// 该接口上没有该方法，继续尝试下一个接口
				}
			}
			// 最后尝试代理类本身
			return targetType.getMethod(methodName, parameterTypes);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException(String.format(
					"Need to invoke method '%s' declared on target class '%s', " +
					"but not found in any interface(s) of the exposed proxy type. " +
					"Either pull the method up to an interface or switch to CGLIB " +
					"proxies by enforcing proxy-target-class mode in your configuration.",
					method.getName(), method.getDeclaringClass().getSimpleName()));
		}
	}


	/**
	 * 给定方法元数据查找的回调接口。
	 * @param <T> 返回的元数据类型
	 */
	@FunctionalInterface
	public interface MetadataLookup<T> {

		/**
		 * 在给定方法上执行查找并返回相关元数据（如果有）。
		 * @param method 要检查的方法
		 * @return 与方法关联的非空元数据（若匹配），否则返回 {@code null}
		 */
		@Nullable
		T inspect(Method method);
	}

}
