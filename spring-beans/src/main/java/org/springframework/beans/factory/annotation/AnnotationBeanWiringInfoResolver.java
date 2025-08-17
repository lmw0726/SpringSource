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

package org.springframework.beans.factory.annotation;

import org.springframework.beans.factory.wiring.BeanWiringInfo;
import org.springframework.beans.factory.wiring.BeanWiringInfoResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.beans.factory.wiring.BeanWiringInfoResolver} 实现类，
 * 使用 @Configurable 注解来识别哪些类需要自动装配。
 * 如果在 {@link Configurable} 注解中指定了 Bean 名称，则使用该名称进行查找；
 * 否则，默认使用正在配置的类的全限定类名。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see Configurable
 * @see org.springframework.beans.factory.wiring.ClassNameBeanWiringInfoResolver
 */
public class AnnotationBeanWiringInfoResolver implements BeanWiringInfoResolver {

	@Override
	@Nullable
	public BeanWiringInfo resolveWiringInfo(Object beanInstance) {
		Assert.notNull(beanInstance, "Bean instance must not be null");
		Configurable annotation = beanInstance.getClass().getAnnotation(Configurable.class);
		return (annotation != null ? buildWiringInfo(beanInstance, annotation) : null);
	}

	/**
	 * 为指定的 {@link Configurable} 注解构建 {@link BeanWiringInfo}。
	 * @param beanInstance Bean 实例
	 * @param annotation 在 Bean 类上找到的 Configurable 注解
	 * @return 解析得到的 BeanWiringInfo
	 */
	protected BeanWiringInfo buildWiringInfo(Object beanInstance, Configurable annotation) {
		if (!Autowire.NO.equals(annotation.autowire())) {
			// 按名称或按类型自动装配
			return new BeanWiringInfo(annotation.autowire().value(), annotation.dependencyCheck());
		}
		else if (!annotation.value().isEmpty()) {
			// 明确指定用于获取属性值的 Bean 定义名称
			return new BeanWiringInfo(annotation.value(), false);
		}
		else {
			// 默认 Bean 名称，用于获取属性值
			return new BeanWiringInfo(getDefaultBeanName(beanInstance), true);
		}
	}

	/**
	 * 确定指定 Bean 实例的默认 Bean 名称。
	 * <p>默认实现：对于 CGLIB 代理返回其父类名，否则返回普通 Bean 类名。
	 * @param beanInstance 用于生成默认名称的 Bean 实例
	 * @return 要使用的默认 Bean 名称
	 * @see org.springframework.util.ClassUtils#getUserClass(Class)
	 */
	protected String getDefaultBeanName(Object beanInstance) {
		return ClassUtils.getUserClass(beanInstance).getName();
	}

}
