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

package org.springframework.beans.factory.support;

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Represents an override of a method that looks up an object in the same IoC context,
 * either by bean name or by bean type (based on the declared method return type).
 *
 * <p>Methods eligible for lookup override may declare arguments in which case the
 * given arguments are passed to the bean retrieval operation.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.BeanFactory#getBean(String)
 * @see org.springframework.beans.factory.BeanFactory#getBean(Class)
 * @see org.springframework.beans.factory.BeanFactory#getBean(String, Object...)
 * @see org.springframework.beans.factory.BeanFactory#getBean(Class, Object...)
 * @see org.springframework.beans.factory.BeanFactory#getBeanProvider(ResolvableType)
 * @since 1.1
 */
public class LookupOverride extends MethodOverride {
	/**
	 * bean名称
	 */
	@Nullable
	private final String beanName;

	/**
	 * 要覆盖的方法
	 */
	@Nullable
	private Method method;


	/**
	 * 构建一个新的LookupOverride。
	 *
	 * @param methodName 要覆盖的方法名称
	 * @param beanName   重写方法应该返回的当前 {@code BeanFactory} 中的bean的名称
	 *                   (对于基于类型的bean检索可能是 {@code null})
	 */
	public LookupOverride(String methodName, @Nullable String beanName) {
		super(methodName);
		this.beanName = beanName;
	}

	/**
	 * 构建一个新的LookupOverride。
	 *
	 * @param method   要覆盖的声明方法
	 * @param beanName 重写方法应该返回的当前 {@code BeanFactory} 中的bean的名称
	 *                 (对于基于类型的bean检索可能是 {@code null})
	 */
	public LookupOverride(Method method, @Nullable String beanName) {
		super(method.getName());
		this.method = method;
		this.beanName = beanName;
	}


	/**
	 * 返回该方法应返回的bean的名称。
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 通过 {@link Method} 引用或方法名称匹配指定的方法。
	 * <p> 出于向后兼容性的原因，在具有给定名称的重载非抽象方法的场景中，只有方法的no-arg变体才会变成容器驱动的查找方法。
	 * <p> 如果提供了 {@link Method}，则仅考虑直接匹配，通常由 {@code @Lookup} 注解划分。
	 */
	@Override
	public boolean matches(Method method) {
		if (this.method != null) {
			return method.equals(this.method);
		} else {
			//先比较方法名称是否相同。
			//如果为非重写方法，返回true
			//如果是抽象方法，返回true
			//如果参数个数为0个，返回true。
			return (method.getName().equals(getMethodName()) && (!isOverloaded() ||
					Modifier.isAbstract(method.getModifiers()) || method.getParameterCount() == 0));
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (!(other instanceof LookupOverride) || !super.equals(other)) {
			return false;
		}
		LookupOverride that = (LookupOverride) other;
		return (ObjectUtils.nullSafeEquals(this.method, that.method) &&
				ObjectUtils.nullSafeEquals(this.beanName, that.beanName));
	}

	@Override
	public int hashCode() {
		return (29 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.beanName));
	}

	@Override
	public String toString() {
		return "LookupOverride for method '" + getMethodName() + "'";
	}

}
