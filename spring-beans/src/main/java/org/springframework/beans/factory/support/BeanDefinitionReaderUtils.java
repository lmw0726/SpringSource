/*
 * Copyright 2002-2020 the original author or authors.
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
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods that are useful for bean definition reader implementations.
 * Mainly intended for internal use.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see PropertiesBeanDefinitionReader
 * @see org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader
 * @since 1.1
 */
public abstract class BeanDefinitionReaderUtils {

	/**
	 * 生成的bean名称的分隔符。如果类名或父名不是唯一的，则将附加 "#1"，"#2" 等，直到名称变得唯一。
	 */
	public static final String GENERATED_BEAN_NAME_SEPARATOR = BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;


	/**
	 * 为给定的父名和类名创建一个新的GenericBeanDefinition，如果指定了类加载程序，则急切地加载bean类。
	 *
	 * @param parentName  父bean的名称 (如果有)
	 * @param className   bean类的名称 (如果有)
	 * @param classLoader 用于加载bean类的类加载器 (可以是 {@code null}，仅按名称注册bean类)
	 * @return bean定义
	 * @throws ClassNotFoundException 如果无法加载bean类
	 */
	public static AbstractBeanDefinition createBeanDefinition(
			@Nullable String parentName, @Nullable String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
		//创建通用的bean定义实例
		GenericBeanDefinition bd = new GenericBeanDefinition();
		//设置父bean的名称
		bd.setParentName(parentName);
		if (className == null) {
			//如果类名为空，直接返回bean定义
			return bd;
		}
		if (classLoader != null) {
			//如果类加载器不为空，则根据类名和类加载器设置Bean类型
			bd.setBeanClass(ClassUtils.forName(className, classLoader));
		} else {
			//直接设置bean名称
			bd.setBeanClassName(className);
		}
		return bd;
	}

	/**
	 * 为给定的顶级bean定义生成一个bean名称，在给定的bean工厂中是唯一的。
	 *
	 * @param beanDefinition 生成bean名称的bean定义
	 * @param registry       定义将要注册的bean工厂 (检查现有的bean名称)
	 * @return 生成的bean名称
	 * @throws BeanDefinitionStoreException 如果不能为给定的bean定义生成唯一的名称
	 * @see #generateBeanName(BeanDefinition, BeanDefinitionRegistry, boolean)
	 */
	public static String generateBeanName(BeanDefinition beanDefinition, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {

		return generateBeanName(beanDefinition, registry, false);
	}

	/**
	 * 为给定的bean定义生成一个bean名称，在给定的bean工厂中是唯一的。
	 *
	 * @param definition  生成bean名称的bean定义
	 * @param registry    定义将要注册的bean工厂 (检查现有的bean名称)
	 * @param isInnerBean 给定的bean定义将注册为 内部 bean还是顶级bean (允许内部bean与顶级bean的特殊名称生成)
	 * @return 生成的bean名称
	 * @throws BeanDefinitionStoreException 如果不能为给定的bean定义生成唯一的名称
	 */
	public static String generateBeanName(
			BeanDefinition definition, BeanDefinitionRegistry registry, boolean isInnerBean)
			throws BeanDefinitionStoreException {
		//获取bean定义的类名
		String generatedBeanName = definition.getBeanClassName();
		if (generatedBeanName == null) {
			//如果该类名为空
			if (definition.getParentName() != null) {
				//如果父类名不为空，则生成的bean名称为父类名+$child
				generatedBeanName = definition.getParentName() + "$child";
			} else if (definition.getFactoryBeanName() != null) {
				//如果bean工厂名称不为空，则生成的bean名称为bean工厂名+$created
				generatedBeanName = definition.getFactoryBeanName() + "$created";
			}
		}
		if (!StringUtils.hasText(generatedBeanName)) {
			//如果生成的bean名称为空，抛出异常
			throw new BeanDefinitionStoreException("Unnamed bean definition specifies neither " +
					"'class' nor 'parent' nor 'factory-bean' - can't generate bean name");
		}

		if (isInnerBean) {
			//内部 bean: 生成标识hashcode后缀。
			return generatedBeanName + GENERATED_BEAN_NAME_SEPARATOR + ObjectUtils.getIdentityHexString(definition);
		}

		// 顶级 bean: 必要时使用带有唯一后缀的普通类名。
		return uniqueBeanName(generatedBeanName, registry);
	}

	/**
	 * 将给定的bean名称转换为给定bean工厂的唯一bean名称，并在必要时附加一个唯一计数器作为后缀。
	 *
	 * @param beanName 原始bean名称
	 * @param registry 定义将要注册的bean工厂 (检查现有的bean名称)
	 * @return 要使用的唯一bean名称
	 * @since 5.1
	 */
	public static String uniqueBeanName(String beanName, BeanDefinitionRegistry registry) {
		String id = beanName;
		int counter = -1;

		// 增加计数器，直到id唯一。
		String prefix = beanName + GENERATED_BEAN_NAME_SEPARATOR;
		while (counter == -1 || registry.containsBeanDefinition(id)) {
			counter++;
			id = prefix + counter;
		}
		return id;
	}

	/**
	 * 向给定的bean工厂注册给定的bean定义。
	 *
	 * @param definitionHolder bean定义，包括名称和别名
	 * @param registry         要注册的bean工厂
	 * @throws BeanDefinitionStoreException 如果注册失败
	 */
	public static void registerBeanDefinition(
			BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {
		// 在主名称下注册bean定义。
		String beanName = definitionHolder.getBeanName();
		//注册Bean定义，将Bean注册到BeanDefinitionMap中
		registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

		// 注册bean名称的别名 (如果有)。
		String[] aliases = definitionHolder.getAliases();
		if (aliases != null) {
			for (String alias : aliases) {
				//注册Bean别名
				registry.registerAlias(beanName, alias);
			}
		}
	}

	/**
	 * Register the given bean definition with a generated name,
	 * unique within the given bean factory.
	 *
	 * @param definition the bean definition to generate a bean name for
	 * @param registry   the bean factory to register with
	 * @return the generated bean name
	 * @throws BeanDefinitionStoreException if no unique name can be generated
	 *                                      for the given bean definition or the definition cannot be registered
	 */
	public static String registerWithGeneratedName(
			AbstractBeanDefinition definition, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {

		String generatedName = generateBeanName(definition, registry, false);
		registry.registerBeanDefinition(generatedName, definition);
		return generatedName;
	}

}
