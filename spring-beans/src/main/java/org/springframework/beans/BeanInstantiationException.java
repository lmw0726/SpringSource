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

package org.springframework.beans;

import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 当 bean 实例化失败时抛出的异常。
 * 携带出错的 bean 类信息。
 *
 * @author Juergen Hoeller
 * @since 1.2.8
 */
@SuppressWarnings("serial")
public class BeanInstantiationException extends FatalBeanException {

	private final Class<?> beanClass;

	@Nullable
	private final Constructor<?> constructor;

	@Nullable
	private final Method constructingMethod;


	/**
	 * 创建一个新的 BeanInstantiationException。
	 * @param beanClass 出错的 bean 类
	 * @param msg 详细消息
	 */
	public BeanInstantiationException(Class<?> beanClass, String msg) {
		this(beanClass, msg, null);
	}

	/**
	 * 创建一个新的 BeanInstantiationException。
	 * @param beanClass 出错的 bean 类
	 * @param msg 详细消息
	 * @param cause 根本原因
	 */
	public BeanInstantiationException(Class<?> beanClass, String msg, @Nullable Throwable cause) {
		super("Failed to instantiate [" + beanClass.getName() + "]: " + msg, cause);
		this.beanClass = beanClass;
		this.constructor = null;
		this.constructingMethod = null;
	}

	/**
	 * 创建一个新的 BeanInstantiationException。
	 * @param constructor 出错的构造函数
	 * @param msg 详细消息
	 * @param cause 根本原因
	 * @since 4.3
	 */
	public BeanInstantiationException(Constructor<?> constructor, String msg, @Nullable Throwable cause) {
		super("Failed to instantiate [" + constructor.getDeclaringClass().getName() + "]: " + msg, cause);
		this.beanClass = constructor.getDeclaringClass();
		this.constructor = constructor;
		this.constructingMethod = null;
	}

	/**
	 * 创建一个新的 BeanInstantiationException。
	 * @param constructingMethod 用于 bean 构造的委托方法
	 * （通常为静态工厂方法，但不一定）
	 * @param msg 详细消息
	 * @param cause 根本原因
	 * @since 4.3
	 */
	public BeanInstantiationException(Method constructingMethod, String msg, @Nullable Throwable cause) {
		super("Failed to instantiate [" + constructingMethod.getReturnType().getName() + "]: " + msg, cause);
		this.beanClass = constructingMethod.getReturnType();
		this.constructor = null;
		this.constructingMethod = constructingMethod;
	}


	/**
	 * 返回出错的 bean 类（永不为 {@code null}）。
	 * @return 待实例化的类
	 */
	public Class<?> getBeanClass() {
		return this.beanClass;
	}

	/**
	 * 返回出错的构造函数（如果已知）。
	 * @return 使用的构造函数；如果是工厂方法或默认实例化，则返回 {@code null}
	 * @since 4.3
	 */
	@Nullable
	public Constructor<?> getConstructor() {
		return this.constructor;
	}

	/**
	 * 返回用于 bean 构造的委托方法（如果已知）。
	 * @return 使用的方法（通常为静态工厂方法）；如果基于构造函数实例化，则返回 {@code null}
	 * @since 4.3
	 */
	@Nullable
	public Method getConstructingMethod() {
		return this.constructingMethod;
	}

}
