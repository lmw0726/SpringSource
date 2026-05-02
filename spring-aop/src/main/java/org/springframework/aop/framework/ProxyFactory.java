/*
 * Copyright 2002-2016 the original author or authors.
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

import org.aopalliance.intercept.Interceptor;

import org.springframework.aop.TargetSource;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 供编程方式使用的 AOP 代理工厂，而不是通过 bean 工厂中的声明式
 * 配置使用。此类提供了一种在自定义用户代码中获取和配置
 * AOP 代理实例的简单方式。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 14.03.2003
 */
@SuppressWarnings("serial")
public class ProxyFactory extends ProxyCreatorSupport {

	/**
	 * 创建新的 ProxyFactory。
	 */
	public ProxyFactory() {
	}

	/**
	 * 创建新的 ProxyFactory。
	 * <p>将代理给定目标实现的所有接口。
	 * @param target 要被代理的目标对象
	 */
	public ProxyFactory(Object target) {
		setTarget(target);
		setInterfaces(ClassUtils.getAllInterfaces(target));
	}

	/**
	 * 创建新的 ProxyFactory。
	 * <p>没有目标，只有接口。必须添加拦截器。
	 * @param proxyInterfaces 代理应实现的接口
	 */
	public ProxyFactory(Class<?>... proxyInterfaces) {
		setInterfaces(proxyInterfaces);
	}

	/**
	 * 为给定接口和拦截器创建新的 ProxyFactory。
	 * <p>这是为单个拦截器创建代理的便捷方法，
	 * 假定该拦截器自行处理所有调用，而不是委托给目标，
	 * 例如远程代理的情况。
	 * @param proxyInterface 代理应实现的接口
	 * @param interceptor 代理应调用的拦截器
	 */
	public ProxyFactory(Class<?> proxyInterface, Interceptor interceptor) {
		addInterface(proxyInterface);
		addAdvice(interceptor);
	}

	/**
	 * 为指定的 {@code TargetSource} 创建 ProxyFactory，
	 * 使代理实现指定接口。
	 * @param proxyInterface 代理应实现的接口
	 * @param targetSource 代理应调用的 TargetSource
	 */
	public ProxyFactory(Class<?> proxyInterface, TargetSource targetSource) {
		addInterface(proxyInterface);
		setTargetSource(targetSource);
	}


	/**
	 * 根据此工厂中的设置创建新的代理。
	 * <p>可以重复调用。如果已添加或移除接口，效果会有所不同。
	 * 可以添加和移除拦截器。
	 * <p>使用默认类加载器：通常是线程上下文类加载器
	 * （如果创建代理需要）。
	 * @return 代理对象
	 */
	public Object getProxy() {
		return createAopProxy().getProxy();
	}

	/**
	 * 根据此工厂中的设置创建新的代理。
	 * <p>可以重复调用。如果已添加或移除接口，效果会有所不同。
	 * 可以添加和移除拦截器。
	 * <p>使用给定类加载器（如果创建代理需要）。
	 * @param classLoader 用于创建代理的类加载器
	 * （或使用 {@code null} 表示底层代理设施的默认值）
	 * @return 代理对象
	 */
	public Object getProxy(@Nullable ClassLoader classLoader) {
		return createAopProxy().getProxy(classLoader);
	}


	/**
	 * 为给定接口和拦截器创建新的代理。
	 * <p>这是为单个拦截器创建代理的便捷方法，
	 * 假定该拦截器自行处理所有调用，而不是委托给目标，
	 * 例如远程代理的情况。
	 * @param proxyInterface 代理应实现的接口
	 * @param interceptor 代理应调用的拦截器
	 * @return 代理对象
	 * @see #ProxyFactory(Class, org.aopalliance.intercept.Interceptor)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getProxy(Class<T> proxyInterface, Interceptor interceptor) {
		return (T) new ProxyFactory(proxyInterface, interceptor).getProxy();
	}

	/**
	 * 为指定的 {@code TargetSource} 创建代理，
	 * 实现指定接口。
	 * @param proxyInterface 代理应实现的接口
	 * @param targetSource 代理应调用的 TargetSource
	 * @return 代理对象
	 * @see #ProxyFactory(Class, org.springframework.aop.TargetSource)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getProxy(Class<T> proxyInterface, TargetSource targetSource) {
		return (T) new ProxyFactory(proxyInterface, targetSource).getProxy();
	}

	/**
	 * 为指定的 {@code TargetSource} 创建代理，
	 * 该代理扩展 {@code TargetSource} 的目标类。
	 * @param targetSource 代理应调用的 TargetSource
	 * @return 代理对象
	 */
	public static Object getProxy(TargetSource targetSource) {
		if (targetSource.getTargetClass() == null) {
			throw new IllegalArgumentException("Cannot create class proxy for TargetSource with null target class");
		}
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTargetSource(targetSource);
		proxyFactory.setProxyTargetClass(true);
		return proxyFactory.getProxy();
	}

}
