/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * 枚举类型，用于确定自动装配状态：即一个 Bean 是否应由 Spring 容器通过 setter 注入自动注入其依赖。
 * 这是 Spring DI 的核心概念。
 *
 * <p>可用于基于注解的配置，例如 AspectJ 的 AnnotationBeanConfigurer 切面。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.factory.annotation.Configurable
 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory
 */
public enum Autowire {

	/**
	 * 表示不进行任何自动装配的常量。
	 */
	NO(AutowireCapableBeanFactory.AUTOWIRE_NO),

	/**
	 * 表示按名称自动装配 Bean 属性的常量。
	 */
	BY_NAME(AutowireCapableBeanFactory.AUTOWIRE_BY_NAME),

	/**
	 * 表示按类型自动装配 Bean 属性的常量。
	 */
	BY_TYPE(AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE);


	private final int value;


	Autowire(int value) {
		this.value = value;
	}

	public int value() {
		return this.value;
	}

	/**
	 * 判断该枚举值是否代表实际的自动装配。
	 * @return 是否指定了实际的自动装配（BY_NAME 或 BY_TYPE）
	 */
	public boolean isAutowire() {
		return (this == BY_NAME || this == BY_TYPE);
	}

}
