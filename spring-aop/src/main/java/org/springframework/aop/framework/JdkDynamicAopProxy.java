/*
 * Copyright 2002-2020 the original author or authors.
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

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.AopInvocationException;
import org.springframework.aop.RawTargetAccess;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DecoratingProxy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Spring AOP 框架中基于 JDK 的 {@link AopProxy} 实现，
 * 基于 JDK {@link java.lang.reflect.Proxy 动态代理}。
 *
 * <p>创建动态代理，实现 AopProxy 暴露的接口。
 * 动态代理<i>不能</i>用于代理定义在类中而非接口中的方法。
 *
 * <p>此类型的对象应通过代理工厂获取，
 * 并由 {@link AdvisedSupport} 类配置。此类是 Spring AOP 框架的内部类，
 * 客户端代码无需直接使用。
 *
 * <p>如果底层（目标）类是线程安全的，则使用此类创建的代理也是线程安全的。
 *
 * <p>只要所有 Advisor（包括 Advice 和 Pointcut）以及 TargetSource 可序列化，
 * 代理就是可序列化的。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @author Sergey Tsypanov
 * @see java.lang.reflect.Proxy
 * @see AdvisedSupport
 * @see ProxyFactory
 */
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

	/**
	 * 使用 Spring 1.2 中的 serialVersionUID 以实现互操作性。
	 */
	private static final long serialVersionUID = 5531744639992436476L;


	/*
	 * 注意：我们可以通过将 "invoke" 重构为模板方法，
	 * 来避免此类和 CGLIB 代理之间的代码重复。然而，相比复制粘贴方案，
	 * 这种方式至少会增加 10% 的性能开销，因此我们为性能牺牲了优雅性。
	 * （我们有良好的测试套件来确保不同代理行为一致 :-）
	 * 这样，我们也可以更容易地利用每个类中的小优化。
	 */

	/**
	 * 使用 static Log 以避免序列化问题。
	 */
	private static final Log logger = LogFactory.getLog(JdkDynamicAopProxy.class);

	/**
	 * 用于配置此代理的配置。
	 */
	private final AdvisedSupport advised;

	private final Class<?>[] proxiedInterfaces;

	/**
	 * {@link #equals} 方法是否定义在被代理接口上？
	 */
	private boolean equalsDefined;

	/**
	 * {@link #hashCode} 方法是否定义在被代理接口上？
	 */
	private boolean hashCodeDefined;


	/**
	 * 为给定 AOP 配置构造新的 JdkDynamicAopProxy。
	 *
	 * @param config 作为 AdvisedSupport 对象的 AOP 配置
	 * @throws AopConfigException 如果配置无效。在这种情况下，我们会尝试抛出信息充分的异常，
	 * 而不是让令人困惑的失败稍后发生。
	 */
	public JdkDynamicAopProxy(AdvisedSupport config) throws AopConfigException {
		Assert.notNull(config, "AdvisedSupport must not be null");
		if (config.getAdvisorCount() == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
			throw new AopConfigException("No advisors and no TargetSource specified");
		}
		this.advised = config;
		this.proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised, true);
		findDefinedEqualsAndHashCodeMethods(this.proxiedInterfaces);
	}


	@Override
	public Object getProxy() {
		return getProxy(ClassUtils.getDefaultClassLoader());
	}

	@Override
	public Object getProxy(@Nullable ClassLoader classLoader) {
		if (logger.isTraceEnabled()) {
			logger.trace("Creating JDK dynamic proxy: " + this.advised.getTargetSource());
		}
		return Proxy.newProxyInstance(classLoader, this.proxiedInterfaces, this);
	}

	/**
	 * 查找所提供接口集合中可能定义的任何 {@link #equals} 或 {@link #hashCode} 方法。
	 *
	 * @param proxiedInterfaces 要内省的接口
	 */
	private void findDefinedEqualsAndHashCodeMethods(Class<?>[] proxiedInterfaces) {
		for (Class<?> proxiedInterface : proxiedInterfaces) {
			Method[] methods = proxiedInterface.getDeclaredMethods();
			for (Method method : methods) {
				if (AopUtils.isEqualsMethod(method)) {
					this.equalsDefined = true;
				}
				if (AopUtils.isHashCodeMethod(method)) {
					this.hashCodeDefined = true;
				}
				if (this.equalsDefined && this.hashCodeDefined) {
					return;
				}
			}
		}
	}


	/**
	 * {@code InvocationHandler.invoke} 的实现。
	 * <p>调用者将看到目标抛出的准确异常，
	 * 除非某个钩子方法抛出异常。
	 */
	@Override
	@Nullable
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object oldProxy = null;
		boolean setProxyContext = false;

		TargetSource targetSource = this.advised.targetSource;
		Object target = null;

		try {
			if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
				// 目标自身未实现 equals(Object) 方法。
				return equals(args[0]);
			} else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
				// 目标自身未实现 hashCode() 方法。
				return hashCode();
			} else if (method.getDeclaringClass() == DecoratingProxy.class) {
				// 这里只声明了 getDecoratedClass() -> 分派到代理配置。
				return AopProxyUtils.ultimateTargetClass(this.advised);
			} else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
					method.getDeclaringClass().isAssignableFrom(Advised.class)) {
				// 使用代理配置调用 ProxyConfig 上的服务方法...
				return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
			}

			Object retVal;

			if (this.advised.exposeProxy) {
				// 如有必要，使调用可用。
				oldProxy = AopContext.setCurrentProxy(proxy);
				setProxyContext = true;
			}

			// 尽可能晚地获取目标，以便在目标来自池的情况下
			// 尽量缩短我们“持有”目标的时间。
			target = targetSource.getTarget();
			Class<?> targetClass = (target != null ? target.getClass() : null);

			// 获取此方法的拦截链。
			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

			// 检查是否有任何 advice。如果没有，可以回退到直接
			// 反射调用目标，并避免创建 MethodInvocation。
			if (chain.isEmpty()) {
				// 可以跳过创建 MethodInvocation：只需直接调用目标
				// 注意，最终调用器必须是 InvokerInterceptor，因此我们知道它
				// 除了对目标执行反射操作外什么也不做，也不会进行热交换或花哨的代理。
				Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
				retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
			} else {
				// 需要创建一个方法调用...
				MethodInvocation invocation =
						new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
				// 通过拦截器链继续执行到连接点。
				retVal = invocation.proceed();
			}

			// 如有必要，调整返回值。
			Class<?> returnType = method.getReturnType();
			if (retVal != null && retVal == target &&
					returnType != Object.class && returnType.isInstance(proxy) &&
					!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
				// 特殊情况：它返回了 "this"，且方法返回类型
				// 类型兼容。注意，如果目标在另一个返回对象中设置了
				// 对自身的引用，我们无法处理这种情况。
				retVal = proxy;
			} else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
				throw new AopInvocationException(
						"Null return value from advice does not match primitive return type for: " + method);
			}
			return retVal;
		} finally {
			if (target != null && !targetSource.isStatic()) {
				// 一定来自 TargetSource。
				targetSource.releaseTarget(target);
			}
			if (setProxyContext) {
				// 恢复旧代理。
				AopContext.setCurrentProxy(oldProxy);
			}
		}
	}


	/**
	 * 相等意味着接口、advisor 和 TargetSource 都相等。
	 * <p>被比较对象可能是 JdkDynamicAopProxy 实例本身，
	 * 也可能是包装 JdkDynamicAopProxy 实例的动态代理。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		JdkDynamicAopProxy otherProxy;
		if (other instanceof JdkDynamicAopProxy) {
			otherProxy = (JdkDynamicAopProxy) other;
		} else if (Proxy.isProxyClass(other.getClass())) {
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			if (!(ih instanceof JdkDynamicAopProxy)) {
				return false;
			}
			otherProxy = (JdkDynamicAopProxy) ih;
		} else {
			// 不是有效比较...
			return false;
		}

		// 如果执行到这里，otherProxy 就是另一个 AopProxy。
		return AopProxyUtils.equalsInProxy(this.advised, otherProxy.advised);
	}

	/**
	 * 代理使用 TargetSource 的哈希码。
	 */
	@Override
	public int hashCode() {
		return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
	}

}
