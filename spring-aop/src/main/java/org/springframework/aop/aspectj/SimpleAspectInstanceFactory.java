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

package org.springframework.aop.aspectj;

import java.lang.reflect.InvocationTargetException;

import org.springframework.aop.framework.AopConfigException;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link AspectInstanceFactory} 的实现，为每次
 * {@link #getAspectInstance()} 调用创建指定切面类的新实例。
 *
 * @author Juergen Hoeller
 * @since 2.0.4
 */
public class SimpleAspectInstanceFactory implements AspectInstanceFactory {

	private final Class<?> aspectClass;


	/**
	 * 为给定的切面类创建新的 SimpleAspectInstanceFactory。
	 * @param aspectClass 切面类
	 */
	public SimpleAspectInstanceFactory(Class<?> aspectClass) {
		Assert.notNull(aspectClass, "Aspect class must not be null");
		this.aspectClass = aspectClass;
	}


	/**
	 * 返回指定的切面类（绝不为 {@code null}）。
	 */
	public final Class<?> getAspectClass() {
		return this.aspectClass;
	}

	@Override
	public final Object getAspectInstance() {
		try {
			return ReflectionUtils.accessibleConstructor(this.aspectClass).newInstance();
		}
		catch (NoSuchMethodException ex) {
			throw new AopConfigException(
					"No default constructor on aspect class: " + this.aspectClass.getName(), ex);
		}
		catch (InstantiationException ex) {
			throw new AopConfigException(
					"Unable to instantiate aspect class: " + this.aspectClass.getName(), ex);
		}
		catch (IllegalAccessException ex) {
			throw new AopConfigException(
					"Could not access aspect constructor: " + this.aspectClass.getName(), ex);
		}
		catch (InvocationTargetException ex) {
			throw new AopConfigException(
					"Failed to invoke aspect constructor: " + this.aspectClass.getName(), ex.getTargetException());
		}
	}

	@Override
	@Nullable
	public ClassLoader getAspectClassLoader() {
		return this.aspectClass.getClassLoader();
	}

	/**
	 * 确定此工厂的切面实例的顺序，
	 * 可以是通过实现 {@link org.springframework.core.Ordered} 接口
	 * 表达的实例特定顺序，
	 * 或者是回退顺序。
	 * @see org.springframework.core.Ordered
	 * @see #getOrderForAspectClass
	 */
	@Override
	public int getOrder() {
		return getOrderForAspectClass(this.aspectClass);
	}

	/**
	 * 确定切面实例不通过实现 {@link org.springframework.core.Ordered} 接口
	 * 表达实例特定顺序的情况下的回退顺序。
	 * <p>默认实现简单地返回 {@code Ordered.LOWEST_PRECEDENCE}。
	 * @param aspectClass 切面类
	 */
	protected int getOrderForAspectClass(Class<?> aspectClass) {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
