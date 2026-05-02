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

package org.springframework.aop.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.aop.Advisor;
import org.springframework.aop.AopInvocationException;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.TargetClassAware;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodIntrospector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * AOP 支持代码的工具方法。
 *
 * <p>主要供 Spring 的 AOP 支持内部使用。
 *
 * <p>有关依赖 Spring AOP 框架实现内部细节的
 * 框架特定 AOP 工具方法集合，请参见
 * {@link org.springframework.aop.framework.AopProxyUtils}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see org.springframework.aop.framework.AopProxyUtils
 */
public abstract class AopUtils {

	/**
	 * 检查给定对象是 JDK 动态代理还是 CGLIB 代理。
	 * <p>此方法还会额外检查给定对象是否是 {@link SpringProxy} 的实例。
	 * @param object 要检查的对象
	 * @see #isJdkDynamicProxy
	 * @see #isCglibProxy
	 */
	public static boolean isAopProxy(@Nullable Object object) {
		return (object instanceof SpringProxy && (Proxy.isProxyClass(object.getClass()) ||
				object.getClass().getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)));
	}

	/**
	 * 检查给定对象是否为 JDK 动态代理。
	 * <p>此方法超出了 {@link Proxy#isProxyClass(Class)} 的实现，
	 * 还会额外检查给定对象是否是 {@link SpringProxy} 的实例。
	 * @param object 要检查的对象
	 * @see java.lang.reflect.Proxy#isProxyClass
	 */
	public static boolean isJdkDynamicProxy(@Nullable Object object) {
		return (object instanceof SpringProxy && Proxy.isProxyClass(object.getClass()));
	}

	/**
	 * 检查给定对象是否为 CGLIB 代理。
	 * <p>此方法超出了 {@link ClassUtils#isCglibProxy(Object)} 的实现，
	 * 还会额外检查给定对象是否是 {@link SpringProxy} 的实例。
	 * @param object 要检查的对象
	 * @see ClassUtils#isCglibProxy(Object)
	 */
	public static boolean isCglibProxy(@Nullable Object object) {
		return (object instanceof SpringProxy &&
				object.getClass().getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR));
	}

	/**
	 * 确定给定 bean 实例的目标类，该实例可能是 AOP 代理。
	 * <p>对于 AOP 代理返回目标类，否则返回普通类。
	 * @param candidate 要检查的实例（可能是 AOP 代理）
	 * @return 目标类（或作为回退的给定对象的普通类；永远不会为 {@code null}）
	 * @see org.springframework.aop.TargetClassAware#getTargetClass()
	 * @see org.springframework.aop.framework.AopProxyUtils#ultimateTargetClass(Object)
	 */
	public static Class<?> getTargetClass(Object candidate) {
		Assert.notNull(candidate, "Candidate object must not be null");
		Class<?> result = null;
		if (candidate instanceof TargetClassAware) {
			result = ((TargetClassAware) candidate).getTargetClass();
		}
		if (result == null) {
			result = (isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
		}
		return result;
	}

	/**
	 * 在目标类型上选择可调用的方法：如果给定方法实际暴露在目标类型上，
	 * 则为该方法本身；否则为目标类型的某个接口上或目标类型本身上的对应方法。
	 * @param method 要检查的方法
	 * @param targetType 要在其上搜索方法的目标类型（通常是 AOP 代理）
	 * @return 目标类型上对应的可调用方法
	 * @throws IllegalStateException 如果给定方法不能在给定目标类型上调用
	 * （通常是因为代理不匹配）
	 * @since 4.3
	 * @see MethodIntrospector#selectInvocableMethod(Method, Class)
	 */
	public static Method selectInvocableMethod(Method method, @Nullable Class<?> targetType) {
		if (targetType == null) {
			return method;
		}
		Method methodToUse = MethodIntrospector.selectInvocableMethod(method, targetType);
		if (Modifier.isPrivate(methodToUse.getModifiers()) && !Modifier.isStatic(methodToUse.getModifiers()) &&
				SpringProxy.class.isAssignableFrom(targetType)) {
			throw new IllegalStateException(String.format(
					"Need to invoke method '%s' found on proxy for target class '%s' but cannot " +
					"be delegated to target bean. Switch its visibility to package or protected.",
					method.getName(), method.getDeclaringClass().getSimpleName()));
		}
		return methodToUse;
	}

	/**
	 * 确定给定方法是否为 "equals" 方法。
	 * @see java.lang.Object#equals
	 */
	public static boolean isEqualsMethod(@Nullable Method method) {
		return ReflectionUtils.isEqualsMethod(method);
	}

	/**
	 * 确定给定方法是否为 "hashCode" 方法。
	 * @see java.lang.Object#hashCode
	 */
	public static boolean isHashCodeMethod(@Nullable Method method) {
		return ReflectionUtils.isHashCodeMethod(method);
	}

	/**
	 * 确定给定方法是否为 "toString" 方法。
	 * @see java.lang.Object#toString()
	 */
	public static boolean isToStringMethod(@Nullable Method method) {
		return ReflectionUtils.isToStringMethod(method);
	}

	/**
	 * 确定给定方法是否为 "finalize" 方法。
	 * @see java.lang.Object#finalize()
	 */
	public static boolean isFinalizeMethod(@Nullable Method method) {
		return (method != null && method.getName().equals("finalize") &&
				method.getParameterCount() == 0);
	}

	/**
	 * 给定一个可能来自接口的方法，以及当前 AOP 调用中使用的目标类，
	 * 如果存在对应的目标方法，则找到它。例如，方法可能是 {@code IFoo.bar()}，
	 * 而目标类可能是 {@code DefaultFoo}。在这种情况下，方法可能是
	 * {@code DefaultFoo.bar()}。这使得能够找到该方法上的属性。
	 * <p><b>注意：</b>与 {@link org.springframework.util.ClassUtils#getMostSpecificMethod} 不同，
	 * 此方法会解析桥接方法，以便从<i>原始</i>方法定义中检索属性。
	 * @param method 要调用的方法，可能来自接口
	 * @param targetClass 当前调用的目标类。
	 * 可以为 {@code null}，也可能甚至没有实现该方法。
	 * @return 具体的目标方法；如果 {@code targetClass} 未实现该方法或为 {@code null}，
	 * 则返回原始方法
	 * @see org.springframework.util.ClassUtils#getMostSpecificMethod
	 */
	public static Method getMostSpecificMethod(Method method, @Nullable Class<?> targetClass) {
		Class<?> specificTargetClass = (targetClass != null ? ClassUtils.getUserClass(targetClass) : null);
		Method resolvedMethod = ClassUtils.getMostSpecificMethod(method, specificTargetClass);
		// 如果正在处理带有泛型参数的方法，则查找原始方法。
		return BridgeMethodResolver.findBridgedMethod(resolvedMethod);
	}

	/**
	 * 给定切点是否完全可以应用于给定类？
	 * <p>这是一个重要测试，因为它可用于优化掉某个类的切点。
	 * @param pc 要检查的静态或动态切点
	 * @param targetClass 要测试的类
	 * @return 切点是否可以应用于任何方法
	 */
	public static boolean canApply(Pointcut pc, Class<?> targetClass) {
		return canApply(pc, targetClass, false);
	}

	/**
	 * 给定切点是否完全可以应用于给定类？
	 * <p>这是一个重要测试，因为它可用于优化掉某个类的切点。
	 * @param pc 要检查的静态或动态切点
	 * @param targetClass 要测试的类
	 * @param hasIntroductions 此 bean 的 advisor 链是否包含任何引介
	 * @return 切点是否可以应用于任何方法
	 */
	public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
		Assert.notNull(pc, "Pointcut must not be null");
		if (!pc.getClassFilter().matches(targetClass)) {
			return false;
		}

		MethodMatcher methodMatcher = pc.getMethodMatcher();
		if (methodMatcher == MethodMatcher.TRUE) {
			// 如果无论如何都匹配任何方法，则无需迭代方法...
			return true;
		}

		IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
		if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
			introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher) methodMatcher;
		}

		Set<Class<?>> classes = new LinkedHashSet<>();
		if (!Proxy.isProxyClass(targetClass)) {
			classes.add(ClassUtils.getUserClass(targetClass));
		}
		classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));

		for (Class<?> clazz : classes) {
			Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
			for (Method method : methods) {
				if (introductionAwareMethodMatcher != null ?
						introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions) :
						methodMatcher.matches(method, targetClass)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * 给定 advisor 是否完全可以应用于给定类？
	 * 这是一个重要测试，因为它可用于优化掉某个类的 advisor。
	 * @param advisor 要检查的 advisor
	 * @param targetClass 正在测试的类
	 * @return 切点是否可以应用于任何方法
	 */
	public static boolean canApply(Advisor advisor, Class<?> targetClass) {
		return canApply(advisor, targetClass, false);
	}

	/**
	 * 给定 advisor 是否完全可以应用于给定类？
	 * <p>这是一个重要测试，因为它可用于优化掉某个类的 advisor。
	 * 此版本还会考虑引介（用于 IntroductionAwareMethodMatchers）。
	 * @param advisor 要检查的 advisor
	 * @param targetClass 正在测试的类
	 * @param hasIntroductions 此 bean 的 advisor 链是否包含任何引介
	 * @return 切点是否可以应用于任何方法
	 */
	public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
		if (advisor instanceof IntroductionAdvisor) {
			return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
		}
		else if (advisor instanceof PointcutAdvisor) {
			PointcutAdvisor pca = (PointcutAdvisor) advisor;
			return canApply(pca.getPointcut(), targetClass, hasIntroductions);
		}
		else {
			// 它没有切点，因此我们假定它适用。
			return true;
		}
	}

	/**
	 * 确定 {@code candidateAdvisors} 列表中适用于给定类的子列表。
	 * @param candidateAdvisors 要评估的 Advisor
	 * @param clazz 目标类
	 * @return 可以应用于给定类对象的 Advisor 子列表
	 * （可以是传入的原始 List）
	 */
	public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
		if (candidateAdvisors.isEmpty()) {
			return candidateAdvisors;
		}
		List<Advisor> eligibleAdvisors = new ArrayList<>();
		for (Advisor candidate : candidateAdvisors) {
			if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
				eligibleAdvisors.add(candidate);
			}
		}
		boolean hasIntroductions = !eligibleAdvisors.isEmpty();
		for (Advisor candidate : candidateAdvisors) {
			if (candidate instanceof IntroductionAdvisor) {
				// 已处理
				continue;
			}
			if (canApply(candidate, clazz, hasIntroductions)) {
				eligibleAdvisors.add(candidate);
			}
		}
		return eligibleAdvisors;
	}

	/**
	 * Invoke the given target via reflection, as part of an AOP method invocation.
	 * @param target the target object
	 * @param method the method to invoke
	 * @param args the arguments for the method
	 * @return the invocation result, if any
	 * @throws Throwable if thrown by the target method
	 * @throws org.springframework.aop.AopInvocationException in case of a reflection error
	 */
	@Nullable
	public static Object invokeJoinpointUsingReflection(@Nullable Object target, Method method, Object[] args)
			throws Throwable {

		// Use reflection to invoke the method.
		try {
			ReflectionUtils.makeAccessible(method);
			return method.invoke(target, args);
		}
		catch (InvocationTargetException ex) {
			// Invoked method threw a checked exception.
			// We must rethrow it. The client won't see the interceptor.
			throw ex.getTargetException();
		}
		catch (IllegalArgumentException ex) {
			throw new AopInvocationException("AOP configuration seems to be invalid: tried calling method [" +
					method + "] on target [" + target + "]", ex);
		}
		catch (IllegalAccessException ex) {
			throw new AopInvocationException("Could not access method [" + method + "]", ex);
		}
	}

}
