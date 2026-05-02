/*
 * Copyright 2002-2015 the original author or authors.
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

import java.io.Serializable;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link AspectInstanceFactory} 的实现，由指定的单例对象支持，
 * 为每次 {@link #getAspectInstance()} 调用返回相同的实例。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see SimpleAspectInstanceFactory
 */
@SuppressWarnings("serial")
public class SingletonAspectInstanceFactory implements AspectInstanceFactory, Serializable {

	private final Object aspectInstance;


	/**
	 * 为给定的切面实例创建新的 SingletonAspectInstanceFactory。
	 * @param aspectInstance 单例切面实例
	 */
	public SingletonAspectInstanceFactory(Object aspectInstance) {
		Assert.notNull(aspectInstance, "Aspect instance must not be null");
		this.aspectInstance = aspectInstance;
	}


	@Override
	public final Object getAspectInstance() {
		return this.aspectInstance;
	}

	@Override
	@Nullable
	public ClassLoader getAspectClassLoader() {
		return this.aspectInstance.getClass().getClassLoader();
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
		if (this.aspectInstance instanceof Ordered) {
			return ((Ordered) this.aspectInstance).getOrder();
		}
		return getOrderForAspectClass(this.aspectInstance.getClass());
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
