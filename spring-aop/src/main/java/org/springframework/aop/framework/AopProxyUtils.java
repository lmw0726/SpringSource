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

package org.springframework.aop.framework;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.core.DecoratingProxy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * AOP 代理工厂的实用工具方法。
 * 主要供 AOP 框架内部使用。
 *
 * <p>请参阅 {@link org.springframework.aop.support.AopUtils}，其中包含不依赖 AOP 框架内部实现的
 * 通用 AOP 实用工具方法集合。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see org.springframework.aop.support.AopUtils
 */
public abstract class AopProxyUtils {

	// JDK 17 Class.isSealed() 方法是否可用？
	@Nullable
	private static final Method isSealedMethod = ClassUtils.getMethodIfAvailable(Class.class, "isSealed");


	/**
	 * 获取给定代理后面的单例目标对象（如果存在）。
	 * @param candidate 要检查的（潜在）代理
	 * @return 由 {@link SingletonTargetSource} 管理的单例目标对象，
	 * 或者在其他情况下返回 {@code null}（不是代理或不存在单例目标）
	 * @since 4.3.8
	 * @see Advised#getTargetSource()
	 * @see SingletonTargetSource#getTarget()
	 */
	@Nullable
	public static Object getSingletonTarget(Object candidate) {
		if (candidate instanceof Advised) {
			TargetSource targetSource = ((Advised) candidate).getTargetSource();
			if (targetSource instanceof SingletonTargetSource) {
				return ((SingletonTargetSource) targetSource).getTarget();
			}
		}
		return null;
	}

	/**
	 * 确定给定 bean 实例的最终目标类，不仅遍历顶层代理，还会遍历任意数量的嵌套代理 &mdash;
	 * 只要不会产生副作用，即仅适用于单例目标。
	 * @param candidate 要检查的实例（可能是 AOP 代理）
	 * @return 最终的目标类（或者作为回退的给定对象的原始类；永远不会是 {@code null}）
	 * @see org.springframework.aop.TargetClassAware#getTargetClass()
	 * @see Advised#getTargetSource()
	 */
	public static Class<?> ultimateTargetClass(Object candidate) {
		Assert.notNull(candidate, "Candidate object must not be null");
		Object current = candidate;
		Class<?> result = null;
		while (current instanceof TargetClassAware) {
			result = ((TargetClassAware) current).getTargetClass();
			current = getSingletonTarget(current);
		}
		if (result == null) {
			result = (AopUtils.isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
		}
		return result;
	}

	/**
	 * 确定为给定 AOP 配置代理的完整接口集。
	 * <p>这将始终添加 {@link Advised} 接口，除非 AdvisedSupport 的
	 * {@link AdvisedSupport#setOpaque "opaque"} 标志已开启。始终添加
	 * {@link org.springframework.aop.SpringProxy} 标记接口。
	 * @param advised 代理配置
	 * @return 要代理的完整接口集
	 * @see SpringProxy
	 * @see Advised
	 */
	public static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised) {
		return completeProxiedInterfaces(advised, false);
	}

	/**
	 * 确定为给定 AOP 配置代理的完整接口集。
	 * <p>这将始终添加 {@link Advised} 接口，除非 AdvisedSupport 的
	 * {@link AdvisedSupport#setOpaque "opaque"} 标志已开启。始终添加
	 * {@link org.springframework.aop.SpringProxy} 标记接口。
	 * @param advised 代理配置
	 * @param decoratingProxy 是否暴露 {@link DecoratingProxy} 接口
	 * @return 要代理的完整接口集
	 * @since 4.3
	 * @see SpringProxy
	 * @see Advised
	 * @see DecoratingProxy
	 */
	static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised, boolean decoratingProxy) {
		Class<?>[] specifiedInterfaces = advised.getProxiedInterfaces();
		if (specifiedInterfaces.length == 0) {
			// 未指定用户接口：检查目标类是否为接口。
			Class<?> targetClass = advised.getTargetClass();
			if (targetClass != null) {
				if (targetClass.isInterface()) {
					advised.setInterfaces(targetClass);
				}
				else if (Proxy.isProxyClass(targetClass) || ClassUtils.isLambdaClass(targetClass)) {
					advised.setInterfaces(targetClass.getInterfaces());
				}
				specifiedInterfaces = advised.getProxiedInterfaces();
			}
		}
		List<Class<?>> proxiedInterfaces = new ArrayList<>(specifiedInterfaces.length + 3);
		for (Class<?> ifc : specifiedInterfaces) {
			// 只有非密封接口才实际适用于 JDK 代理（在 JDK 17 上）
			if (isSealedMethod == null || Boolean.FALSE.equals(ReflectionUtils.invokeMethod(isSealedMethod, ifc))) {
				proxiedInterfaces.add(ifc);
			}
		}
		if (!advised.isInterfaceProxied(SpringProxy.class)) {
			proxiedInterfaces.add(SpringProxy.class);
		}
		if (!advised.isOpaque() && !advised.isInterfaceProxied(Advised.class)) {
			proxiedInterfaces.add(Advised.class);
		}
		if (decoratingProxy && !advised.isInterfaceProxied(DecoratingProxy.class)) {
			proxiedInterfaces.add(DecoratingProxy.class);
		}
		return ClassUtils.toClassArray(proxiedInterfaces);
	}

	/**
	 * 提取给定代理实现的用户指定接口，
	 * 即代理实现的所有非 Advised 接口。
	 * @param proxy 要分析的代理（通常是 JDK 动态代理）
	 * @return 代理实现的所有用户指定接口，
	 * 按原始顺序排列（永远不会是 {@code null} 或空）
	 * @see Advised
	 */
	public static Class<?>[] proxiedUserInterfaces(Object proxy) {
		Class<?>[] proxyInterfaces = proxy.getClass().getInterfaces();
		int nonUserIfcCount = 0;
		if (proxy instanceof SpringProxy) {
			nonUserIfcCount++;
		}
		if (proxy instanceof Advised) {
			nonUserIfcCount++;
		}
		if (proxy instanceof DecoratingProxy) {
			nonUserIfcCount++;
		}
		Class<?>[] userInterfaces = Arrays.copyOf(proxyInterfaces, proxyInterfaces.length - nonUserIfcCount);
		Assert.notEmpty(userInterfaces, "JDK proxy must implement one or more interfaces");
		return userInterfaces;
	}

	/**
	 * 检查给定 AdvisedSupport 对象后面的代理是否相等。
	 * 与 AdvisedSupport 对象的相等性不同：
	 * 而是检查接口、通知器和目标源的相等性。
	 */
	public static boolean equalsInProxy(AdvisedSupport a, AdvisedSupport b) {
		return (a == b ||
				(equalsProxiedInterfaces(a, b) && equalsAdvisors(a, b) && a.getTargetSource().equals(b.getTargetSource())));
	}

	/**
	 * 检查给定 AdvisedSupport 对象后面的代理接口是否相等。
	 */
	public static boolean equalsProxiedInterfaces(AdvisedSupport a, AdvisedSupport b) {
		return Arrays.equals(a.getProxiedInterfaces(), b.getProxiedInterfaces());
	}

	/**
	 * 检查给定 AdvisedSupport 对象后面的通知器是否相等。
	 */
	public static boolean equalsAdvisors(AdvisedSupport a, AdvisedSupport b) {
		return a.getAdvisorCount() == b.getAdvisorCount() && Arrays.equals(a.getAdvisors(), b.getAdvisors());
	}


	/**
	 * 如果需要，将给定参数适配到给定方法中的目标签名：
	 * 特别是当给定的可变参数数组与方法中声明的可变参数的数组类型不匹配时。
	 * @param method 目标方法
	 * @param arguments 给定的参数
	 * @return 克隆的参数数组，如果不需要适配则返回原始数组
	 * @since 4.2.3
	 */
	static Object[] adaptArgumentsIfNecessary(Method method, @Nullable Object[] arguments) {
		if (ObjectUtils.isEmpty(arguments)) {
			return new Object[0];
		}
		if (method.isVarArgs()) {
			if (method.getParameterCount() == arguments.length) {
				Class<?>[] paramTypes = method.getParameterTypes();
				int varargIndex = paramTypes.length - 1;
				Class<?> varargType = paramTypes[varargIndex];
				if (varargType.isArray()) {
					Object varargArray = arguments[varargIndex];
					if (varargArray instanceof Object[] && !varargType.isInstance(varargArray)) {
						Object[] newArguments = new Object[arguments.length];
						System.arraycopy(arguments, 0, newArguments, 0, varargIndex);
						Class<?> targetElementType = varargType.getComponentType();
						int varargLength = Array.getLength(varargArray);
						Object newVarargArray = Array.newInstance(targetElementType, varargLength);
						System.arraycopy(varargArray, 0, newVarargArray, 0, varargLength);
						newArguments[varargIndex] = newVarargArray;
						return newArguments;
					}
				}
			}
		}
		return arguments;
	}

}
