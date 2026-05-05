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

import org.springframework.aop.SpringProxy;
import org.springframework.core.NativeDetector;
import org.springframework.util.ClassUtils;

import java.io.Serializable;
import java.lang.reflect.Proxy;

/**
 * 默认 {@link AopProxyFactory} 实现，创建 CGLIB 代理或 JDK 动态代理。
 *
 * <p>对于给定的 {@link AdvisedSupport} 实例，如果满足以下任一条件，
 * 则创建 CGLIB 代理：
 * <ul>
 * <li>设置了 {@code optimize} 标志
 * <li>设置了 {@code proxyTargetClass} 标志
 * <li>未指定代理接口
 * </ul>
 *
 * <p>通常，指定 {@code proxyTargetClass} 以强制使用 CGLIB 代理，
 * 或指定一个或多个接口以使用 JDK 动态代理。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 12.03.2004
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 */
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

	private static final long serialVersionUID = 7930414337282325166L;


	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		// ===================== 第一层判断：是否使用 CGLIB 分支 =====================
		// 条件成立时，会优先考虑使用 CGLIB
		// 当前不是 GraalVM 原生镜像环境
		// 是否开启优化（通常意味着使用 CGLIB）
		// 是否强制使用 CGLIB（proxyTargetClass=true）
		// 是否没有用户提供的接口
		if (!NativeDetector.inNativeImage() &&
				(config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config))) {
			// 获取目标类（要被代理的类）
			Class<?> targetClass = config.getTargetClass();

			// 如果目标类为空，直接抛异常（无法创建代理）
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			// ===================== 特殊情况：仍然使用 JDK 代理 =====================
			// 如果目标类是接口 / 已经是代理类 / lambda 表达式
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass) || ClassUtils.isLambdaClass(targetClass)) {
				// 使用 JDK 动态代理
				return new JdkDynamicAopProxy(config);
			}
			// ===================== 默认：使用 CGLIB =====================
			// 使用 CGLIB 代理（基于继承）
			return new ObjenesisCglibAopProxy(config);
		}
		// ===================== 第二层：默认使用 JDK 代理 =====================
		else {
			// 使用 JDK 动态代理
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * 确定提供的 {@link AdvisedSupport} 是否只指定了
	 * {@link org.springframework.aop.SpringProxy} 接口
	 * （或完全未指定代理接口）。
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
