/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * 已配置 AOP 代理的委托接口，允许创建实际的代理对象。
 *
 * <p>开箱即用的实现可用于 JDK 动态代理和 CGLIB 代理，
 * 由 {@link DefaultAopProxyFactory} 应用。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see DefaultAopProxyFactory
 */
public interface AopProxy {

	/**
	 * 创建新的代理对象。
	 * <p>使用 AopProxy 的默认类加载器（如果创建代理需要）：
	 * 通常是线程上下文类加载器。
	 * @return 新的代理对象（绝不为 {@code null}）
	 * @see Thread#getContextClassLoader()
	 */
	Object getProxy();

	/**
	 * 创建新的代理对象。
	 * <p>使用给定类加载器（如果创建代理需要）。
	 * {@code null} 会被直接向下传递，从而使用底层代理设施的默认值，
	 * 该默认值通常不同于 AopProxy 实现的 {@link #getProxy()} 方法选择的默认值。
	 * @param classLoader 用于创建代理的类加载器
	 * （或使用 {@code null} 表示底层代理设施的默认值）
	 * @return 新的代理对象（绝不为 {@code null}）
	 */
	Object getProxy(@Nullable ClassLoader classLoader);

}
