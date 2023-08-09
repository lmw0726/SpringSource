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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.AliasRegistry;

/**
 * Interface for registries that hold bean definitions, for example RootBeanDefinition
 * and ChildBeanDefinition instances. Typically implemented by BeanFactories that
 * internally work with the AbstractBeanDefinition hierarchy.
 *
 * <p>This is the only interface in Spring's bean factory packages that encapsulates
 * <i>registration</i> of bean definitions. The standard BeanFactory interfaces
 * only cover access to a <i>fully configured factory instance</i>.
 *
 * <p>Spring's bean definition readers expect to work on an implementation of this
 * interface. Known implementors within the Spring core are DefaultListableBeanFactory
 * and GenericApplicationContext.
 *
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.config.BeanDefinition
 * @see AbstractBeanDefinition
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 * @see DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 * @see PropertiesBeanDefinitionReader
 * @since 26.11.2003
 */
public interface BeanDefinitionRegistry extends AliasRegistry {

	/**
	 * 在此注册表中注册一个新的bean定义。必须支持RootBeanDefinition和ChildBeanDefinition。
	 *
	 * @param beanName       要注册的bean实例的名称
	 * @param beanDefinition 要注册的bean实例的定义
	 * @throws BeanDefinitionStoreException    如果BeanDefinition无效
	 * @throws BeanDefinitionOverrideException 如果已经有指定bean名称的bean定义，并且我们不允许覆盖它
	 * @see GenericBeanDefinition
	 * @see RootBeanDefinition
	 * @see ChildBeanDefinition
	 */
	void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException;

	/**
	 * 删除给定名称的BeanDefinition。
	 *
	 * @param beanName 要注册的bean实例的名称
	 * @throws NoSuchBeanDefinitionException 如果没有这样的bean定义
	 */
	void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 返回给定bean名称的BeanDefinition。
	 *
	 * @param beanName 要查找的定义的bean的名称
	 * @return 给定名称的BeanDefinition (从不 {@code null})
	 * @throws NoSuchBeanDefinitionException 如果没有这样的bean定义
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 检查此注册表是否包含具有给定名称的bean定义。
	 *
	 * @param beanName 要找的bean的名字
	 * @return 如果此注册表包含具有给定名称的bean定义
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * 返回此注册表中定义的所有bean的名称。
	 *
	 * @return 此注册表中定义的所有bean的名称，如果未定义，则为空数组
	 */
	String[] getBeanDefinitionNames();

	/**
	 * 返回注册表中定义的bean数。
	 *
	 * @return 注册表中定义的bean数量
	 */
	int getBeanDefinitionCount();

	/**
	 * 确定给定的bean名称是否已在此注册表中使用，即是否存在本地bean或别名注册在此名称下。
	 *
	 * @param beanName 要检查的名称
	 * @return 给定的bean名称是否已经在使用中
	 */
	boolean isBeanNameInUse(String beanName);

}
