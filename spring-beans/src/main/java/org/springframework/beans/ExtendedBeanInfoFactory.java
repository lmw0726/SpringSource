/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Method;

/**
 * {@link BeanInfoFactory} 的实现，用于评估 bean 类是否具有“非标准”JavaBeans setter方法，因此可以由 Spring 的（包可见的）{@code ExtendedBeanInfo} 实现进行内省。
 *
 * <p>在 {@link Ordered#LOWEST_PRECEDENCE} 排序，以允许其他用户定义的 {@link BeanInfoFactory} 类型优先。
 *
 * @author Chris Beams
 * @since 3.2
 * @see BeanInfoFactory
 * @see CachedIntrospectionResults
 */
public class ExtendedBeanInfoFactory implements BeanInfoFactory, Ordered {

	/**
	 * 如果适用，则为给定的 bean 类返回一个 {@link ExtendedBeanInfo}。
	 */
	@Override
	@Nullable
	public BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
		return (supports(beanClass) ? new ExtendedBeanInfo(Introspector.getBeanInfo(beanClass)) : null);
	}

	/**
	 * 返回给定的 bean 类是否声明或继承任何非 void 返回的 bean 属性或索引属性 setter 方法。
	 */
	private boolean supports(Class<?> beanClass) {
		// 遍历 beanClass 的所有方法
		for (Method method : beanClass.getMethods()) {
			// 如果方法是候选的写方法
			if (ExtendedBeanInfo.isCandidateWriteMethod(method)) {
				// 返回 true
				return true;
			}
		}
		// 如果没有找到候选的写方法，返回 false
		return false;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
