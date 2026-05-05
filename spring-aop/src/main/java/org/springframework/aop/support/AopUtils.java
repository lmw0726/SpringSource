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

import org.springframework.aop.*;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodIntrospector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
		// 断言：Pointcut 不能为空
		Assert.notNull(pc, "Pointcut must not be null");
		// ===================== 第一步：类级别匹配 =====================
		// 如果 ClassFilter 不匹配当前目标类，直接返回 false
		// 👉 不需要继续判断方法（性能优化）
		if (!pc.getClassFilter().matches(targetClass)) {
			return false;
		}
		// ===================== 第二步：获取方法匹配器 =====================
		// 获取 MethodMatcher（用于判断方法是否匹配）
		MethodMatcher methodMatcher = pc.getMethodMatcher();

		// 如果是“全匹配”（匹配所有方法）
		if (methodMatcher == MethodMatcher.TRUE) {
			// 如果无论如何都匹配任何方法，则无需迭代方法...
			// 👉 不需要遍历方法，直接返回 true
			return true;
		}
		// ===================== 第三步：是否支持引介增强 =====================
		IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
		// 如果该 MethodMatcher 支持“引介增强感知”
		if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
			// 转换为支持引介的匹配器
			introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher) methodMatcher;
		}
		// ===================== 第四步：收集需要检查的类 =====================
		// 使用 LinkedHashSet 保证顺序 + 去重
		Set<Class<?>> classes = new LinkedHashSet<>();
		// 如果目标类不是 JDK 动态代理类
		if (!Proxy.isProxyClass(targetClass)) {
			// 获取用户真实类（去掉 CGLIB 代理）
			classes.add(ClassUtils.getUserClass(targetClass));
		}
		// 加入该类实现的所有接口
		classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));
		// ===================== 第五步：方法级匹配（核心） =====================
		// 遍历所有类（目标类 + 接口）
		for (Class<?> clazz : classes) {
			// 获取该类的所有方法（包括私有方法）
			Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
			// 遍历方法
			for (Method method : methods) {
				// 如果是“支持引介增强”的匹配器
				if (introductionAwareMethodMatcher != null ?
						// 使用带 hasIntroductions 参数的匹配方法
						introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions) :
						// 否则使用普通匹配
						methodMatcher.matches(method, targetClass)) {
					// 只要有一个方法匹配成功 → 返回 true
					return true;
				}
			}
		}

		// 如果没有任何方法匹配 → 返回 false
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
		// ===================== 情况1：引介增强（IntroductionAdvisor） =====================
		// 如果是引介增强（@DeclareParents）
		if (advisor instanceof IntroductionAdvisor) {

			// 判断该引介增强的 ClassFilter 是否匹配目标类
			// 👉 只关心“类级别匹配”，不涉及方法
			return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
		}

		// ===================== 情况2：普通切点增强（PointcutAdvisor） =====================
		// 如果是普通 Advisor（包含 Pointcut + Advice）
		else if (advisor instanceof PointcutAdvisor) {
			// 强转为 PointcutAdvisor
			PointcutAdvisor pca = (PointcutAdvisor) advisor;
			// 调用重载方法，判断该 Pointcut 是否可以应用到目标类
			// 👉 内部会检查：
			//   - ClassFilter（类匹配）
			//   - MethodMatcher（方法匹配）
			//   - hasIntroductions（是否有引介增强影响）
			return canApply(pca.getPointcut(), targetClass, hasIntroductions);
		}
		// ===================== 情况3：无切点的 Advisor =====================
		else {
			// 如果既不是 IntroductionAdvisor，也不是 PointcutAdvisor
			// 👉 说明没有切点限制（例如一些特殊 Advisor）
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
		// 如果候选 Advisor 为空，直接返回
		if (candidateAdvisors.isEmpty()) {
			return candidateAdvisors;
		}
		// 用于存放最终“可应用”的 Advisor
		List<Advisor> eligibleAdvisors = new ArrayList<>();
		// ===================== 第一轮：处理 IntroductionAdvisor =====================
		// 遍历所有候选 Advisor
		for (Advisor candidate : candidateAdvisors) {
			// 如果是“引介增强”（@DeclareParents 对应的 Advisor）
			if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
				// 如果该引介增强可以作用在当前类上，加入结果
				eligibleAdvisors.add(candidate);
			}
		}
		// 判断当前是否存在“引介增强”
		boolean hasIntroductions = !eligibleAdvisors.isEmpty();
		// ===================== 第二轮：处理普通 Advisor =====================
		// 再次遍历所有候选 Advisor
		for (Advisor candidate : candidateAdvisors) {
			// 如果是 IntroductionAdvisor（第一轮已经处理过）
			if (candidate instanceof IntroductionAdvisor) {
				// 已处理
				continue;
			}
			// 判断普通 Advisor 是否可以应用到当前类
			// ⚠️ 注意：这里会传入 hasIntroductions（是否有引介增强）
			if (canApply(candidate, clazz, hasIntroductions)) {
				eligibleAdvisors.add(candidate);
			}
		}
		// 返回最终“适用于当前类”的 Advisor 列表
		return eligibleAdvisors;
	}

	/**
	 * 通过反射调用给定的目标方法，作为 AOP 方法调用的一部分。
	 * @param target 目标对象
	 * @param method 要调用的方法
	 * @param args 方法的参数
	 * @return 调用结果（如果有）
	 * @throws Throwable 如果目标方法抛出异常
	 * @throws org.springframework.aop.AopInvocationException 在反射调用出错时抛出
	 */
	@Nullable
	public static Object invokeJoinpointUsingReflection(@Nullable Object target, Method method, Object[] args)
			throws Throwable {

		// 使用反射调用该方法
		try {
			ReflectionUtils.makeAccessible(method);
			return method.invoke(target, args);
		}
		catch (InvocationTargetException ex) {
			// 被调用的方法抛出了异常（受检异常）
			// 我们必须重新抛出它，客户端不会看到拦截器
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
