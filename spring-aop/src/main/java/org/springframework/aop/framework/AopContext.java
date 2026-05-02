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

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

/**
 * 包含静态方法的类，用于获取当前 AOP 调用的相关信息。
 *
 * <p>如果 AOP 框架配置为暴露当前代理（非默认行为），
 * 则可以使用 {@code currentProxy()} 方法。它返回正在使用的 AOP 代理。
 * 目标对象或 advice 可以使用它进行 advised 调用，方式类似于在 EJB 中使用
 * {@code getEJBObject()}。它们也可以使用它查找 advice 配置。
 *
 * <p>Spring 的 AOP 框架默认不暴露代理，因为这样做会产生性能成本。
 *
 * <p>此类中的功能可由需要访问调用上资源的目标对象使用。
 * 然而，当存在合理替代方案时，不应使用这种方式，
 * 因为它会使应用程序代码依赖于在 AOP 下运行，尤其依赖 Spring AOP 框架。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13.03.2003
 */
public final class AopContext {

	/**
	 * 与此线程关联的 AOP 代理的 ThreadLocal 持有者。
	 * 除非控制代理配置上的 "exposeProxy" 属性已设置为 "true"，
	 * 否则将包含 {@code null}。
	 * @see ProxyConfig#setExposeProxy
	 */
	private static final ThreadLocal<Object> currentProxy = new NamedThreadLocal<>("Current AOP proxy");


	private AopContext() {
	}


	/**
	 * 尝试返回当前 AOP 代理。仅当调用方法是通过 AOP 调用的，
	 * 且 AOP 框架已设置为暴露代理时，此方法才可用。
	 * 否则，此方法将抛出 IllegalStateException。
	 * @return 当前 AOP 代理（绝不返回 {@code null}）
	 * @throws IllegalStateException 如果无法找到代理，因为该方法在 AOP 调用上下文之外被调用，
	 * 或者 AOP 框架未配置为暴露代理
	 */
	public static Object currentProxy() throws IllegalStateException {
		Object proxy = currentProxy.get();
		if (proxy == null) {
			throw new IllegalStateException(
					"Cannot find current proxy: Set 'exposeProxy' property on Advised to 'true' to make it available, and " +
							"ensure that AopContext.currentProxy() is invoked in the same thread as the AOP invocation context.");
		}
		return proxy;
	}

	/**
	 * 通过 {@code currentProxy()} 方法使给定代理可用。
	 * <p>注意，调用者应根据需要小心保存旧值。
	 * @param proxy 要暴露的代理（或使用 {@code null} 重置）
	 * @return 旧代理；如果未绑定任何代理，则可能为 {@code null}
	 * @see #currentProxy()
	 */
	@Nullable
	static Object setCurrentProxy(@Nullable Object proxy) {
		Object old = currentProxy.get();
		if (proxy != null) {
			currentProxy.set(proxy);
		}
		else {
			currentProxy.remove();
		}
		return old;
	}

}
