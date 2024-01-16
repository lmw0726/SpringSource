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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

/**
 * 在 BeanFactory 中使用的简单对象实例化策略。
 *
 * <p>不支持方法注入，尽管它提供了供子类覆盖的钩子以添加方法注入支持，例如通过覆盖方法。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 */
public class SimpleInstantiationStrategy implements InstantiationStrategy {


	/**
	 * 线程变量，正在创建Bean的Method对象
	 */
	private static final ThreadLocal<Method> currentlyInvokedFactoryMethod = new ThreadLocal<>();


	/**
	 * 返回当前正在调用的工厂方法，如果没有则返回 {@code null}。
	 * <p>允许工厂方法实现确定当前调用者是容器本身还是用户代码。
	 */
	@Nullable
	public static Method getCurrentlyInvokedFactoryMethod() {
		return currentlyInvokedFactoryMethod.get();
	}


	@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
		// 如果 BeanDefinition 中没有方法覆盖
		if (!bd.hasMethodOverrides()) {
			Constructor<?> constructorToUse;
			synchronized (bd.constructorArgumentLock) {
				// 使用同步块确保线程安全
				constructorToUse = (Constructor<?>) bd.resolvedConstructorOrFactoryMethod;
				if (constructorToUse == null) {
					final Class<?> clazz = bd.getBeanClass();
					if (clazz.isInterface()) {
						// 如果指定的类是接口，抛出 BeanInstantiationException
						throw new BeanInstantiationException(clazz, "Specified class is an interface");
					}
					try {
						if (System.getSecurityManager() != null) {
							// 如果存在安全管理器，通过特权访问获取默认构造函数
							constructorToUse = AccessController.doPrivileged(
									(PrivilegedExceptionAction<Constructor<?>>) clazz::getDeclaredConstructor);
						} else {
							// 如果不存在安全管理器，直接获取默认构造函数
							constructorToUse = clazz.getDeclaredConstructor();
						}
						bd.resolvedConstructorOrFactoryMethod = constructorToUse;
					} catch (Throwable ex) {
						// 获取构造函数出现异常，抛出 BeanInstantiationException
						throw new BeanInstantiationException(clazz, "No default constructor found", ex);
					}
				}
			}
			// 使用反射实例化对象
			return BeanUtils.instantiateClass(constructorToUse);
		} else {
			// 如果有方法覆盖，必须生成 CGLIB 子类
			return instantiateWithMethodInjection(bd, beanName, owner);
		}
	}

	/**
	 * 子类可以重写此方法。该方法被实现为抛出 UnsupportedOperationException，
	 * 如果它们可以使用给定 RootBeanDefinition 中指定的方法注入实例化对象。实例化应该使用无参构造函数。
	 */
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
		throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
	}

	@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
							  final Constructor<?> ctor, Object... args) {
		// 如果 BeanDefinition 中没有方法覆盖
		if (!bd.hasMethodOverrides()) {
			if (System.getSecurityManager() != null) {
				// 如果存在安全管理器，通过特权访问使构造函数可访问
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					ReflectionUtils.makeAccessible(ctor);
					return null;
				});
			}
			// 使用反射实例化对象
			return BeanUtils.instantiateClass(ctor, args);
		} else {
			// 如果有方法覆盖，必须生成 CGLIB 子类
			return instantiateWithMethodInjection(bd, beanName, owner, ctor, args);
		}
	}

	/**
	 * 子类可以重写此方法。该方法被实现为抛出 UnsupportedOperationException，
	 * 如果它们可以使用给定 RootBeanDefinition 中指定的方法注入实例化对象。实例化应该使用给定的构造函数和参数。
	 */
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName,
													BeanFactory owner, @Nullable Constructor<?> ctor, Object... args) {

		throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
	}

	@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
							  @Nullable Object factoryBean, final Method factoryMethod, Object... args) {
		try {
			if (System.getSecurityManager() != null) {
				// 如果存在安全管理器，通过特权访问使工厂方法可访问
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					//设置工厂方法可以访问
					ReflectionUtils.makeAccessible(factoryMethod);
					return null;
				});
			} else {
				// 如果不存在安全管理器，直接使工厂方法可访问
				ReflectionUtils.makeAccessible(factoryMethod);
			}

			//获取原 方法对象
			Method priorInvokedFactoryMethod = currentlyInvokedFactoryMethod.get();
			try {
				// 设置新的方法对象，到currentlyInvokedFactoryMethod中
				currentlyInvokedFactoryMethod.set(factoryMethod);
				// 利用反射创建bean对象
				Object result = factoryMethod.invoke(factoryBean, args);
				if (result == null) {
					result = new NullBean();
				}
				return result;
			} finally {
				// 重置当前调用的工厂方法
				if (priorInvokedFactoryMethod != null) {
					currentlyInvokedFactoryMethod.set(priorInvokedFactoryMethod);
				} else {
					currentlyInvokedFactoryMethod.remove();
				}
			}
		} catch (IllegalArgumentException ex) {
			// 处理 IllegalArgumentException 异常
			throw new BeanInstantiationException(factoryMethod,
					"Illegal arguments to factory method '" + factoryMethod.getName() + "'; " +
							"args: " + StringUtils.arrayToCommaDelimitedString(args), ex);
		} catch (IllegalAccessException ex) {
			// 处理 IllegalAccessException 异常
			throw new BeanInstantiationException(factoryMethod,
					"Cannot access factory method '" + factoryMethod.getName() + "'; is it public?", ex);
		} catch (InvocationTargetException ex) {
			// 处理 InvocationTargetException 异常
			String msg = "Factory method '" + factoryMethod.getName() + "' threw exception";
			if (bd.getFactoryBeanName() != null && owner instanceof ConfigurableBeanFactory &&
					((ConfigurableBeanFactory) owner).isCurrentlyInCreation(bd.getFactoryBeanName())) {
				// 处理循环引用的异常信息
				msg = "Circular reference involving containing bean '" + bd.getFactoryBeanName() + "' - consider " +
						"declaring the factory method as static for independence from its containing instance. " + msg;
			}
			throw new BeanInstantiationException(factoryMethod, msg, ex.getTargetException());
		}
	}

}
