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
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import java.io.IOException;

/**
 * {@link org.springframework.context.ApplicationContext}实现的方便基类，
 * 从包含由{@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}理解的bean定义的XML文档中获取配置。
 *
 * <p>子类只需实现{@link #getConfigResources}和/或{@link #getConfigLocations}方法。
 * 此外，它们可能会覆盖{@link #getResourceByPath}钩子以环境特定的方式解释相对路径，
 * 和/或{@link #getResourcePatternResolver}以进行扩展的模式解析。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getConfigResources
 * @see #getConfigLocations
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 */
public abstract class AbstractXmlApplicationContext extends AbstractRefreshableConfigApplicationContext {

	/**
	 * 是否检验XML，默认检查XML
	 */
	private boolean validating = true;


	/**
	 * 创建一个没有父级的新AbstractXmlApplicationContext。
	 */
	public AbstractXmlApplicationContext() {
	}

	/**
	 * 使用给定的父上下文创建一个新的AbstractXmlApplicationContext。
	 *
	 * @param parent 父上下文
	 */
	public AbstractXmlApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * 设置是否使用XML验证。默认值为{@code true}。
	 *
	 * @param validating 是否进行XML验证
	 */
	public void setValidating(boolean validating) {
		this.validating = validating;
	}


	/**
	 * 通过XmlBeanDefinitionReader加载Bean定义。
	 *
	 * @param beanFactory DefaultListableBeanFactory实例，用于加载Bean定义
	 * @throws BeansException 如果发生Beans异常
	 * @throws IOException 如果发生IO异常
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 * @see #initBeanDefinitionReader
	 * @see #loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
		// 为给定的 BeanFactory 创建一个新的 XmlBeanDefinitionReader。
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		// 使用当前上下文的资源加载环境配置 bean 定义阅读器。
		beanDefinitionReader.setEnvironment(this.getEnvironment());
		beanDefinitionReader.setResourceLoader(this);
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		// 允许子类提供阅读器的自定义初始化，然后继续实际加载 bean 定义。
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
	}

	/**
	 * 初始化用于加载此上下文的Bean定义的Bean定义阅读器。默认实现为空。
	 * <p>可以在子类中进行覆盖，例如，用于关闭XML验证或使用不同的XmlBeanDefinitionParser实现。
	 *
	 * @param reader 此上下文使用的Bean定义阅读器
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader#setDocumentReaderClass
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
		reader.setValidating(this.validating);
	}

	/**
	 * 使用给定的XmlBeanDefinitionReader加载Bean定义。
	 * <p>Bean工厂的生命周期由{@link #refreshBeanFactory}方法处理；因此，此方法只是用于加载和/或注册Bean定义。
	 *
	 * @param reader 要使用的XmlBeanDefinitionReader
	 * @throws BeansException 如果发生Bean注册错误
	 * @throws IOException 如果找不到所需的XML文档
	 * @see #refreshBeanFactory
	 * @see #getConfigLocations
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
		// 获取配置资源数组。
		Resource[] configResources = getConfigResources();
		if (configResources != null) {
			// 使用阅读器加载配置资源的 bean 定义。
			reader.loadBeanDefinitions(configResources);
		}

		// 获取配置位置数组。
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			// 使用阅读器加载配置位置的 bean 定义。
			reader.loadBeanDefinitions(configLocations);
		}
	}

	/**
	 * 返回一个Resource对象数组，指向应使用这个上下文构建的XML bean定义文件。
	 * <p>默认实现返回{@code null}。子类可以覆盖此方法以提供预构建的Resource对象，而不是位置字符串。
	 *
	 * @return 一个Resource对象数组，如果没有则为{@code null}
	 * @see #getConfigLocations()
	 */
	@Nullable
	protected Resource[] getConfigResources() {
		return null;
	}

}
