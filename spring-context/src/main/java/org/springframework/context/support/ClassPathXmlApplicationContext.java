/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 独立的XML应用程序上下文，从类路径获取上下文定义文件，将普通路径解释为包含包路径的类路径资源名称（例如 "mypackage/myresource.txt"）。
 * 适用于测试工具以及嵌入在JAR中的应用程序上下文。
 * <p>
 * 默认情况下，可以通过 {@link #getConfigLocations} 覆盖配置位置。配置位置可以表示具体文件，如 "/myfiles/context.xml"，
 * 也可以是Ant样式的模式，如 "/myfiles/*-context.xml"（有关模式详细信息，请参见 {@link org.springframework.util.AntPathMatcher} javadoc）。
 * <p>
 * 注意：在多个配置位置的情况下，后加载的文件中定义的bean将覆盖先加载的文件中的bean。这可以用于通过额外的XML文件故意覆盖某些bean定义。
 *
 * <p><b>这是一个简单的、一站式的便利ApplicationContext。考虑使用 {@link GenericApplicationContext} 类
 * 结合 {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader} 进行更灵活的上下文设置。</b>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getResource
 * @see #getResourceByPath
 * @see GenericApplicationContext
 */
public class ClassPathXmlApplicationContext extends AbstractXmlApplicationContext {

	/**
	 * 配置资源
	 */
	@Nullable
	private Resource[] configResources;


	/**
	 * 为bean样式的配置创建一个新的ClassPathXmlApplicationContext。
	 *
	 * @see #setConfigLocation
	 * @see #setConfigLocations
	 * @see #afterPropertiesSet()
	 */
	public ClassPathXmlApplicationContext() {
	}

	/**
	 * 为bean样式的配置创建一个新的ClassPathXmlApplicationContext。
	 *
	 * @param parent 父上下文
	 * @see #setConfigLocation
	 * @see #setConfigLocations
	 * @see #afterPropertiesSet()
	 */
	public ClassPathXmlApplicationContext(ApplicationContext parent) {
		super(parent);
	}

	/**
	 * 创建一个新的ClassPathXmlApplicationContext，从给定的XML文件加载定义，并自动刷新上下文。
	 *
	 * @param configLocation 资源位置
	 * @throws BeansException 如果上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
		this(new String[]{configLocation}, true, null);
	}

	/**
	 * 创建一个新的ClassPathXmlApplicationContext，从给定的XML文件加载定义并自动刷新上下文。
	 *
	 * @param configLocations 资源位置数组
	 * @throws BeansException 如果上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String... configLocations) throws BeansException {
		this(configLocations, true, null);
	}

	/**
	 * 创建一个新的ClassPathXmlApplicationContext，使用给定的父上下文从给定的XML文件加载定义并自动刷新上下文。
	 *
	 * @param configLocations 资源位置数组
	 * @param parent          父上下文
	 * @throws BeansException 如果上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String[] configLocations, @Nullable ApplicationContext parent)
			throws BeansException {

		this(configLocations, true, parent);
	}

	/**
	 * 创建一个新的ClassPathXmlApplicationContext，从给定的XML文件加载定义。
	 *
	 * @param configLocations 资源位置数组
	 * @param refresh         是否自动刷新上下文，加载所有bean定义并创建所有单例。
	 *                        或者，在进一步配置上下文后手动调用refresh。
	 * @throws BeansException 如果上下文创建失败
	 * @see #refresh()
	 */
	public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
		this(configLocations, refresh, null);
	}

	/**
	 * 创建一个新的ClassPathXmlApplicationContext，带有给定的父级，从给定的XML文件加载定义。
	 *
	 * @param configLocations 资源位置数组
	 * @param refresh         是否自动刷新上下文，加载所有bean定义并创建所有单例。
	 *                        或者，在进一步配置上下文后手动调用refresh。
	 * @param parent          父级上下文
	 * @throws BeansException 如果上下文创建失败
	 * @see #refresh()
	 */
	public ClassPathXmlApplicationContext(
			String[] configLocations, boolean refresh, @Nullable ApplicationContext parent)
			throws BeansException {

		super(parent);
		setConfigLocations(configLocations);
		if (refresh) {
			refresh();
		}
	}


	/**
	 * 创建一个新的ClassPathXmlApplicationContext，从给定的XML文件加载定义，并自动刷新上下文。
	 * <p>这是一个方便的方法，用于相对于给定Class加载类路径资源。为了完全灵活，考虑使用GenericApplicationContext
	 * 与XmlBeanDefinitionReader和ClassPathResource参数。
	 *
	 * @param path  类路径中的相对（或绝对）路径
	 * @param clazz 用于加载资源的类（给定路径的基础）
	 * @throws BeansException 如果上下文创建失败
	 * @see org.springframework.core.io.ClassPathResource#ClassPathResource(String, Class)
	 * @see org.springframework.context.support.GenericApplicationContext
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	public ClassPathXmlApplicationContext(String path, Class<?> clazz) throws BeansException {
		this(new String[]{path}, clazz);
	}

	/**
	 * 创建一个新的ClassPathXmlApplicationContext，从给定的XML文件加载定义，并自动刷新上下文。
	 *
	 * @param paths 类路径中的相对（或绝对）路径数组
	 * @param clazz 用于加载资源的类（给定路径的基础）
	 * @throws BeansException 如果上下文创建失败
	 * @see org.springframework.core.io.ClassPathResource#ClassPathResource(String, Class)
	 * @see org.springframework.context.support.GenericApplicationContext
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	public ClassPathXmlApplicationContext(String[] paths, Class<?> clazz) throws BeansException {
		this(paths, clazz, null);
	}

	/**
	 * 创建一个新的ClassPathXmlApplicationContext，加载给定XML文件中的定义，并自动刷新上下文。
	 *
	 * @param paths  类路径中的相对（或绝对）路径数组
	 * @param clazz  用于加载资源的类（给定路径的基础）
	 * @param parent 父上下文
	 * @throws BeansException 如果上下文创建失败
	 * @see org.springframework.core.io.ClassPathResource#ClassPathResource(String, Class)
	 * @see org.springframework.context.support.GenericApplicationContext
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	public ClassPathXmlApplicationContext(String[] paths, Class<?> clazz, @Nullable ApplicationContext parent)
			throws BeansException {

		super(parent);
		Assert.notNull(paths, "Path array must not be null");
		Assert.notNull(clazz, "Class argument must not be null");
		this.configResources = new Resource[paths.length];
		for (int i = 0; i < paths.length; i++) {
			this.configResources[i] = new ClassPathResource(paths[i], clazz);
		}
		refresh();
	}


	@Override
	@Nullable
	protected Resource[] getConfigResources() {
		return this.configResources;
	}

}
