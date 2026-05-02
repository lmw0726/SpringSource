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

package org.springframework.aop.framework.autoproxy;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

/**
 * 当前代理创建上下文的持有者，由 {@link AbstractAdvisorAutoProxyCreator}
 * 等自动代理创建器暴露。
 *
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.5
 */
public final class ProxyCreationContext {

	/** 在 Advisor 匹配期间保存当前被代理 bean 名称的 ThreadLocal。 */
	private static final ThreadLocal<String> currentProxiedBeanName =
			new NamedThreadLocal<>("Name of currently proxied bean");


	private ProxyCreationContext() {
	}


	/**
	 * 返回当前被代理 bean 实例的名称。
	 * @return bean 的名称，如果没有则返回 {@code null}
	 */
	@Nullable
	public static String getCurrentProxiedBeanName() {
		return currentProxiedBeanName.get();
	}

	/**
	 * 设置当前被代理 bean 实例的名称。
	 * @param beanName bean 的名称，或 {@code null} 以重置
	 */
	static void setCurrentProxiedBeanName(@Nullable String beanName) {
		if (beanName != null) {
			currentProxiedBeanName.set(beanName);
		}
		else {
			currentProxiedBeanName.remove();
		}
	}

}
