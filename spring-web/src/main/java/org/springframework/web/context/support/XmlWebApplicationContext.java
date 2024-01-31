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

package org.springframework.web.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

import java.io.IOException;

/**
 * {@link org.springframework.web.context.WebApplicationContext} 实现，它从 XML 文档中获取配置，
 * 由 {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader} 解析。
 * 这本质上相当于 {@link org.springframework.context.support.GenericXmlApplicationContext}，用于 web 环境。
 *
 * <p>默认情况下，根上下文的配置将从 "/WEB-INF/applicationContext.xml" 中获取，
 * 对于命名空间为 "test-servlet" 的上下文，配置将从 "/WEB-INF/test-servlet.xml" 中获取
 * （例如，对于 servlet-name 为 "test" 的 DispatcherServlet 实例）。
 *
 * <p>可以通过 {@link org.springframework.web.context.ContextLoader} 的 "contextConfigLocation"
 * 上下文参数和 {@link org.springframework.web.servlet.FrameworkServlet} 的 servlet 初始化参数覆盖默认的配置位置。
 * 配置位置可以是具体的文件，如 "/WEB-INF/context.xml"，也可以是 Ant 样式的模式，如 "/WEB-INF/*-context.xml"
 * （有关模式详细信息，请参阅 {@link org.springframework.util.PathMatcher} 的 Javadoc）。
 *
 * <p>注意：在存在多个配置位置的情况下，后面加载的文件中的 bean 定义将覆盖先前加载文件中定义的 bean 定义。
 * 这可以用于通过额外的 XML 文件有意地覆盖某些 bean 定义。
 *
 * <p><b>对于读取不同 bean 定义格式的 WebApplicationContext，创建一个 {@link AbstractRefreshableWebApplicationContext} 的类似子类。</b>
 * 可以将这样的上下文实现指定为 ContextLoader 的 "contextClass" 上下文参数或 FrameworkServlet 的 "contextClass" 初始化参数。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setNamespace
 * @see #setConfigLocations
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 * @see org.springframework.web.context.ContextLoader#initWebApplicationContext
 * @see org.springframework.web.servlet.FrameworkServlet#initWebApplicationContext
 */
public class XmlWebApplicationContext extends AbstractRefreshableWebApplicationContext {

	/**
	 * 根上下文的默认配置位置。
	 */
	public static final String DEFAULT_CONFIG_LOCATION = "/WEB-INF/applicationContext.xml";

	/**
	 * 为命名空间构建配置位置的默认前缀。
	 */
	public static final String DEFAULT_CONFIG_LOCATION_PREFIX = "/WEB-INF/";

	/**
	 * 为命名空间构建配置位置的默认后缀。
	 */
	public static final String DEFAULT_CONFIG_LOCATION_SUFFIX = ".xml";


	/**
	 * 通过 XmlBeanDefinitionReader 加载 Bean 定义。
	 *
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 * @see #initBeanDefinitionReader
	 * @see #loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
		// 为给定的 BeanFactory 创建一个新的 XmlBeanDefinitionReader。
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		// 使用此上下文的资源加载环境配置 bean 定义阅读器。
		beanDefinitionReader.setEnvironment(getEnvironment());
		beanDefinitionReader.setResourceLoader(this);
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		// 允许子类提供对阅读器的自定义初始化，
		// 然后继续实际加载 Bean 定义。
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
	}

	/**
	 * 初始化用于加载此上下文的 Bean 定义的 BeanDefinitionReader。默认实现为空。
	 * <p>可以在子类中覆盖，例如，关闭 XML 验证或使用不同的 XmlBeanDefinitionParser 实现。
	 *
	 * @param beanDefinitionReader 此上下文使用的 Bean 定义阅读器
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader#setValidationMode
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader#setDocumentReaderClass
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
	}

	/**
	 * 使用给定的 XmlBeanDefinitionReader 加载 Bean 定义。
	 * <p>Bean 工厂的生命周期由 refreshBeanFactory 方法处理；因此，此方法只需加载和/或注册 Bean 定义。
	 * <p>委托给 ResourcePatternResolver 来将位置模式解析为 Resource 实例。
	 *
	 * @throws IOException 如果找不到所需的 XML 文档
	 * @see #refreshBeanFactory
	 * @see #getConfigLocations
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws IOException {
		// 获取配置位置
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (String configLocation : configLocations) {
				//加载指定位置的Bean定义
				reader.loadBeanDefinitions(configLocation);
			}
		}
	}

	/**
	 * 获取默认的配置文件位置。对于根上下文，默认位置是"/WEB-INF/applicationContext.xml"，
	 * 对于命名空间为 "test-servlet" 的上下文，默认位置是 "/WEB-INF/test-servlet.xml"
	 * （例如，对于 servlet-name 为 "test" 的 DispatcherServlet 实例）。
	 */
	@Override
	protected String[] getDefaultConfigLocations() {
		if (getNamespace() != null) {
			return new String[]{DEFAULT_CONFIG_LOCATION_PREFIX + getNamespace() + DEFAULT_CONFIG_LOCATION_SUFFIX};
		} else {
			return new String[]{DEFAULT_CONFIG_LOCATION};
		}
	}

}
