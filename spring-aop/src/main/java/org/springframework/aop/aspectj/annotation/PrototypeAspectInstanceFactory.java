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

package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;

import org.springframework.beans.factory.BeanFactory;

/**
 * 由 {@link BeanFactory} 提供的原型支持的
 * {@link org.springframework.aop.aspectj.AspectInstanceFactory}，强制原型语义。
 *
 * <p>请注意，这可能会实例化多次，这可能不会给你期望的语义。
 * 使用 {@link LazySingletonAspectInstanceFactoryDecorator}
 * 包装此工厂以确保只返回一个新的切面实例。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.factory.BeanFactory
 * @see LazySingletonAspectInstanceFactoryDecorator
 */
@SuppressWarnings("serial")
public class PrototypeAspectInstanceFactory extends BeanFactoryAspectInstanceFactory implements Serializable {

	/**
	 * 创建 PrototypeAspectInstanceFactory。将调用 AspectJ 来内省，
	 * 使用从 BeanFactory 为给定 bean 名称返回的类型创建 AJType 元数据。
	 * @param beanFactory 从中获取实例的 BeanFactory
	 * @param name bean 的名称
	 */
	public PrototypeAspectInstanceFactory(BeanFactory beanFactory, String name) {
		super(beanFactory, name);
		if (!beanFactory.isPrototype(name)) {
			throw new IllegalArgumentException(
					"Cannot use PrototypeAspectInstanceFactory with bean named '" + name + "': not a prototype");
		}
	}

}
