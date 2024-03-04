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

package org.springframework.beans.factory.xml;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.Resource;

/**
 * {@link DefaultListableBeanFactory} 的便利扩展，从 XML 文档中读取 bean 定义。
 * 在底层委托给 {@link XmlBeanDefinitionReader}；与使用 XmlBeanDefinitionReader 和 DefaultListableBeanFactory 效果相同。
 *
 * <p>所需 XML 文档的结构、元素和属性名称在此类中是硬编码的。（当然，如果需要，可以运行转换来生成此格式）。
 * “beans” 不需要是 XML 文档的根元素：此类将解析 XML 文件中的所有 bean 定义元素。
 *
 * <p>此类使用 {@link DefaultListableBeanFactory} 超类向其注册每个 bean 定义，并依赖后者对 {@link BeanFactory} 接口的实现。
 * 它支持单例、原型和对这些种类的 bean 的引用。有关选项和配置样式的详细信息，请参见 “spring-beans-3.x.xsd”（或历史上的 “spring-beans-2.0.dtd”）。
 *
 * <p><b>对于高级需求，请考虑使用 {@link DefaultListableBeanFactory} 和 {@link XmlBeanDefinitionReader}。</b>
 * 后者允许从多个 XML 资源中读取，并且在其实际 XML 解析行为方面高度可配置。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see XmlBeanDefinitionReader
 * @since 15 April 2001
 * @deprecated 自 Spring 3.1 起已弃用，推荐使用 {@link DefaultListableBeanFactory} 和 {@link XmlBeanDefinitionReader}
 */
@Deprecated
@SuppressWarnings({"serial", "all"})
public class XmlBeanFactory extends DefaultListableBeanFactory {

	/**
	 * xmlBean定义阅读器
	 */
	private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);


	/**
	 * 使用给定的资源创建一个新的XmlBeanFactory，必须使用DOM进行解析。
	 *
	 * @param resource 从中加载bean定义的XML资源
	 * @throws BeansException 在加载或解析错误的情况下
	 */
	public XmlBeanFactory(Resource resource) throws BeansException {
		this(resource, null);
	}

	/**
	 * 使用给定的输入流创建一个新的XmlBeanFactory，必须使用DOM进行解析。
	 *
	 * @param resource          从中加载bean定义的XML资源
	 * @param parentBeanFactory 父bean工厂
	 * @throws BeansException 在加载或解析错误的情况下
	 */
	public XmlBeanFactory(Resource resource, BeanFactory parentBeanFactory) throws BeansException {
		super(parentBeanFactory);
		this.reader.loadBeanDefinitions(resource);
	}

}
