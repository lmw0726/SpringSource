/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop.scope;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utility class for creating a scoped proxy.
 *
 * <p>Used by ScopedProxyBeanDefinitionDecorator and ClassPathBeanDefinitionScanner.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 2.5
 */
public abstract class ScopedProxyUtils {

	private static final String TARGET_NAME_PREFIX = "scopedTarget.";

	private static final int TARGET_NAME_PREFIX_LENGTH = TARGET_NAME_PREFIX.length();


	/**
	 * 为提供的目标bean生成作用域代理，使用内部名称注册目标bean并在作用域代理上设置 “targetBeanName”。
	 *
	 * @param definition       原始的bean定义
	 * @param registry         bean定义注册
	 * @param proxyTargetClass 是否创建目标类代理
	 * @return 范围代理定义
	 * @see #getTargetBeanName(String)
	 * @see #getOriginalBeanName(String)
	 */
	public static BeanDefinitionHolder createScopedProxy(BeanDefinitionHolder definition,
														 BeanDefinitionRegistry registry, boolean proxyTargetClass) {
		//获取原始bean名称
		String originalBeanName = definition.getBeanName();
		//获取bean定义
		BeanDefinition targetDefinition = definition.getBeanDefinition();
		//获取目标bean名称
		String targetBeanName = getTargetBeanName(originalBeanName);

		// 为原始bean名称创建一个作用域的代理定义，在内部目标定义中 “隐藏” 目标bean。
		//创建ScopedProxyFactoryBean的RootBeanDefinition实例
		RootBeanDefinition proxyDefinition = new RootBeanDefinition(ScopedProxyFactoryBean.class);
		//设置装饰定义
		proxyDefinition.setDecoratedDefinition(new BeanDefinitionHolder(targetDefinition, targetBeanName));
		//设置原始bean定义
		proxyDefinition.setOriginatingBeanDefinition(targetDefinition);
		//设置源
		proxyDefinition.setSource(definition.getSource());
		//设置角色
		proxyDefinition.setRole(targetDefinition.getRole());
		//代理定义的属性值对中添加上targetBeanName键值对
		proxyDefinition.getPropertyValues().add("targetBeanName", targetBeanName);
		if (proxyTargetClass) {
			//如果需要创建代理类，将preserveTargetClassAttribute设置为true
			targetDefinition.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
			// ScopedProxyFactoryBean的 “proxyTargetClass” 默认值为TRUE，因此我们不需要在此处显式设置它。
		} else {
			//否则将proxyTargetClass 设置为false
			proxyDefinition.getPropertyValues().add("proxyTargetClass", Boolean.FALSE);
		}

		// 从原始bean定义复制自动装配设置。
		proxyDefinition.setAutowireCandidate(targetDefinition.isAutowireCandidate());
		//设置是否是主要的
		proxyDefinition.setPrimary(targetDefinition.isPrimary());
		if (targetDefinition instanceof AbstractBeanDefinition) {
			//如果目标bean定义是AbstractBeanDefinition类型，从目标bean定义中复制Qualifiers
			proxyDefinition.copyQualifiersFrom((AbstractBeanDefinition) targetDefinition);
		}

		// 应该忽略目标bean，以支持范围代理。
		//将目标bean定义设置为非自动装配候选者，且不是主要的
		targetDefinition.setAutowireCandidate(false);
		targetDefinition.setPrimary(false);

		// 将目标bean注册为工厂中的单独bean。
		registry.registerBeanDefinition(targetBeanName, targetDefinition);

		// 返回作用域代理定义作为主bean定义 (可能是内部bean)。
		return new BeanDefinitionHolder(proxyDefinition, originalBeanName, definition.getAliases());
	}

	/**
	 * 生成在作用域代理中用于引用目标bean的bean名称。
	 *
	 * @param originalBeanName 原始的bean名称
	 * @return 生成的要用于引用目标bean的bean
	 * @see #getOriginalBeanName(String)
	 */
	public static String getTargetBeanName(String originalBeanName) {
		return TARGET_NAME_PREFIX + originalBeanName;
	}

	/**
	 * Get the original bean name for the provided {@linkplain #getTargetBeanName
	 * target bean name}.
	 *
	 * @param targetBeanName the target bean name for the scoped proxy
	 * @return the original bean name
	 * @throws IllegalArgumentException if the supplied bean name does not refer
	 *                                  to the target of a scoped proxy
	 * @see #getTargetBeanName(String)
	 * @see #isScopedTarget(String)
	 * @since 5.1.10
	 */
	public static String getOriginalBeanName(@Nullable String targetBeanName) {
		Assert.isTrue(isScopedTarget(targetBeanName), () -> "bean name '" +
				targetBeanName + "' does not refer to the target of a scoped proxy");
		return targetBeanName.substring(TARGET_NAME_PREFIX_LENGTH);
	}

	/**
	 * Determine if the {@code beanName} is the name of a bean that references
	 * the target bean within a scoped proxy.
	 *
	 * @since 4.1.4
	 */
	public static boolean isScopedTarget(@Nullable String beanName) {
		return (beanName != null && beanName.startsWith(TARGET_NAME_PREFIX));
	}

}
