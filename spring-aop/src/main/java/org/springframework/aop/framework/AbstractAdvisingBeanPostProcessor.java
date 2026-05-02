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

package org.springframework.aop.framework;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.SmartClassLoader;
import org.springframework.lang.Nullable;

/**
 * {@link BeanPostProcessor} 实现的基类，
 * 用于将 Spring AOP {@link Advisor} 应用于特定 bean。
 *
 * @author Juergen Hoeller
 * @since 3.2
 */
@SuppressWarnings("serial")
public abstract class AbstractAdvisingBeanPostProcessor extends ProxyProcessorSupport implements BeanPostProcessor {

	@Nullable
	protected Advisor advisor;

	protected boolean beforeExistingAdvisors = false;

	private final Map<Class<?>, Boolean> eligibleBeans = new ConcurrentHashMap<>(256);


	/**
	 * 设置此后处理器的 advisor 在遇到已预先 advised 的对象时，
	 * 是否应应用在已有 advisor 之前。
	 * <p>默认值为 "false"，即在已有 advisor 之后应用该 advisor，
	 * 也就是尽可能靠近目标方法。将其切换为 "true" 可使此后处理器的
	 * advisor 同样包装已有 advisor。
	 * <p>注意：请检查具体后处理器的 javadoc，确认其是否可能
	 * 根据自身 advisor 的性质默认更改此标志。
	 */
	public void setBeforeExistingAdvisors(boolean beforeExistingAdvisors) {
		this.beforeExistingAdvisors = beforeExistingAdvisors;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (this.advisor == null || bean instanceof AopInfrastructureBean) {
			// 忽略 AOP 基础设施，例如 scoped proxies。
			return bean;
		}

		if (bean instanceof Advised) {
			Advised advised = (Advised) bean;
			if (!advised.isFrozen() && isEligible(AopUtils.getTargetClass(bean))) {
				// 将本地 Advisor 添加到已有代理的 Advisor 链中...
				if (this.beforeExistingAdvisors) {
					advised.addAdvisor(0, this.advisor);
				}
				else {
					advised.addAdvisor(this.advisor);
				}
				return bean;
			}
		}

		if (isEligible(bean, beanName)) {
			ProxyFactory proxyFactory = prepareProxyFactory(bean, beanName);
			if (!proxyFactory.isProxyTargetClass()) {
				evaluateProxyInterfaces(bean.getClass(), proxyFactory);
			}
			proxyFactory.addAdvisor(this.advisor);
			customizeProxyFactory(proxyFactory);

			// 如果 bean 类不是在覆盖类加载器中本地加载的，则使用原始 ClassLoader
			ClassLoader classLoader = getProxyClassLoader();
			if (classLoader instanceof SmartClassLoader && classLoader != bean.getClass().getClassLoader()) {
				classLoader = ((SmartClassLoader) classLoader).getOriginalClassLoader();
			}
			return proxyFactory.getProxy(classLoader);
		}

		// 不需要代理。
		return bean;
	}

	/**
	 * 检查给定 bean 是否适合使用此后处理器的 {@link Advisor} 进行 advising。
	 * <p>委托给 {@link #isEligible(Class)} 进行目标类检查。
	 * 可以重写，例如按名称专门排除某些 bean。
	 * <p>注意：仅针对常规 bean 实例调用，不会针对实现 {@link Advised}
	 * 且允许将本地 {@link Advisor} 添加到已有代理 {@link Advisor} 链中的
	 * 现有代理实例调用。对于后者，会直接调用 {@link #isEligible(Class)}，
	 * 并使用已有代理背后的实际目标类（由 {@link AopUtils#getTargetClass(Object)} 确定）。
	 * @param bean bean 实例
	 * @param beanName bean 的名称
	 * @see #isEligible(Class)
	 */
	protected boolean isEligible(Object bean, String beanName) {
		return isEligible(bean.getClass());
	}

	/**
	 * 检查给定类是否适合使用此后处理器的 {@link Advisor} 进行 advising。
	 * <p>按 bean 目标类缓存 {@code canApply} 结果。
	 * @param targetClass 要检查的类
	 * @see AopUtils#canApply(Advisor, Class)
	 */
	protected boolean isEligible(Class<?> targetClass) {
		Boolean eligible = this.eligibleBeans.get(targetClass);
		if (eligible != null) {
			return eligible;
		}
		if (this.advisor == null) {
			return false;
		}
		eligible = AopUtils.canApply(this.advisor, targetClass);
		this.eligibleBeans.put(targetClass, eligible);
		return eligible;
	}

	/**
	 * 为给定 bean 准备 {@link ProxyFactory}。
	 * <p>子类可以自定义目标实例的处理，尤其是目标类的暴露。
	 * 对于非目标类代理的默认接口内省和配置的 advisor 会在之后应用；
	 * {@link #customizeProxyFactory} 允许在代理创建前对这些部分进行后期自定义。
	 * @param bean 要为其创建代理的 bean 实例
	 * @param beanName 对应的 bean 名称
	 * @return 使用此处理器的 {@link ProxyConfig} 设置和指定 bean 初始化后的 ProxyFactory
	 * @since 4.2.3
	 * @see #customizeProxyFactory
	 */
	protected ProxyFactory prepareProxyFactory(Object bean, String beanName) {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.copyFrom(this);
		proxyFactory.setTarget(bean);
		return proxyFactory;
	}

	/**
	 * 子类可以选择实现此方法：例如，
	 * 更改所暴露的接口。
	 * <p>默认实现为空。
	 * @param proxyFactory 已经配置了目标、advisor 和接口的 ProxyFactory，
	 * 将在此方法返回后立即用于创建代理
	 * @since 4.2.3
	 * @see #prepareProxyFactory
	 */
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}

}
