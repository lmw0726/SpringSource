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

package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.Advisor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.beans.factory.NamedBean;
import org.springframework.lang.Nullable;

/**
 * 用于创建 advisor 的便捷方法，这些 advisor 可在自动代理
 * Spring IoC 容器创建的 bean 时使用，将 bean 名称绑定到当前调用。
 * 可能支持 AspectJ 的 {@code bean()} 切点指示符。
 *
 * <p>通常用于 Spring 自动代理，此时 bean 名称在代理创建时已知。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.factory.NamedBean
 */
public abstract class ExposeBeanNameAdvisors {

	/**
	 * 当前正在调用的 bean 的 bean 名称绑定，
	 * 位于 ReflectiveMethodInvocation userAttributes Map 中。
	 */
	private static final String BEAN_NAME_ATTRIBUTE = ExposeBeanNameAdvisors.class.getName() + ".BEAN_NAME";


	/**
	 * 查找当前调用的 bean 名称。假定 ExposeBeanNameAdvisor
	 * 已包含在拦截器链中，并且调用已通过 ExposeInvocationInterceptor 暴露。
	 * @return bean 名称（绝不为 {@code null}）
	 * @throws IllegalStateException 如果 bean 名称尚未暴露
	 */
	public static String getBeanName() throws IllegalStateException {
		return getBeanName(ExposeInvocationInterceptor.currentInvocation());
	}

	/**
	 * 查找给定调用的 bean 名称。假定 ExposeBeanNameAdvisor
	 * 已包含在拦截器链中。
	 * @param mi 应包含 bean 名称作为属性的 MethodInvocation
	 * @return bean 名称（绝不为 {@code null}）
	 * @throws IllegalStateException 如果 bean 名称尚未暴露
	 */
	public static String getBeanName(MethodInvocation mi) throws IllegalStateException {
		if (!(mi instanceof ProxyMethodInvocation)) {
			throw new IllegalArgumentException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
		}
		ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
		String beanName = (String) pmi.getUserAttribute(BEAN_NAME_ATTRIBUTE);
		if (beanName == null) {
			throw new IllegalStateException("Cannot get bean name; not set on MethodInvocation: " + mi);
		}
		return beanName;
	}

	/**
	 * 创建一个新的 advisor，用于暴露给定 bean 名称，
	 * 不带引介。
	 * @param beanName 要暴露的 bean 名称
	 */
	public static Advisor createAdvisorWithoutIntroduction(String beanName) {
		return new DefaultPointcutAdvisor(new ExposeBeanNameInterceptor(beanName));
	}

	/**
	 * 创建一个新的 advisor，用于暴露给定 bean 名称，并引介
	 * NamedBean 接口，使 bean 名称可访问，而无需强制目标对象
	 * 感知此 Spring IoC 概念。
	 * @param beanName 要暴露的 bean 名称
	 */
	public static Advisor createAdvisorIntroducingNamedBean(String beanName) {
		return new DefaultIntroductionAdvisor(new ExposeBeanNameIntroduction(beanName));
	}


	/**
	 * 将指定 bean 名称作为调用属性暴露的拦截器。
	 */
	private static class ExposeBeanNameInterceptor implements MethodInterceptor {

		private final String beanName;

		public ExposeBeanNameInterceptor(String beanName) {
			this.beanName = beanName;
		}

		@Override
		@Nullable
		public Object invoke(MethodInvocation mi) throws Throwable {
			// 判断当前 MethodInvocation 是否为 Spring 的 ProxyMethodInvocation
			if (!(mi instanceof ProxyMethodInvocation)) {
				// 如果不是，说明当前不在 Spring AOP 代理调用链中，抛出异常
				throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
			}
			ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
			// 将当前 bean 的名称保存到调用上下文中
			pmi.setUserAttribute(BEAN_NAME_ATTRIBUTE, this.beanName);
			// 继续执行拦截器链
			return mi.proceed();
		}
	}


	/**
	 * 将指定 bean 名称作为调用属性暴露的引介。
	 */
	@SuppressWarnings("serial")
	private static class ExposeBeanNameIntroduction extends DelegatingIntroductionInterceptor implements NamedBean {

		private final String beanName;

		public ExposeBeanNameIntroduction(String beanName) {
			this.beanName = beanName;
		}

		@Override
		@Nullable
		public Object invoke(MethodInvocation mi) throws Throwable {

			// 判断当前 MethodInvocation 是否是 Spring 的 ProxyMethodInvocation
			if (!(mi instanceof ProxyMethodInvocation)) {
				// 如果不是，说明不在 Spring AOP 代理调用链中，直接抛异常
				throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
			}

			ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;

			// 将当前 Bean 的名称存入调用上下文（userAttribute）
			// 👉 后续 Advice 可以通过该属性获取当前被代理的 beanName
			pmi.setUserAttribute(BEAN_NAME_ATTRIBUTE, this.beanName);

			// 调用父类的 invoke 方法，继续执行拦截器链
			return super.invoke(mi);
		}

		@Override
		public String getBeanName() {
			return this.beanName;
		}
	}

}
