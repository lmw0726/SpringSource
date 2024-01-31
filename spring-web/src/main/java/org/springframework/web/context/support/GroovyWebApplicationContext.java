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

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.lang.Nullable;

import java.io.IOException;

/**
 * {@link org.springframework.web.context.WebApplicationContext} 实现，从 Groovy
 * bean 定义脚本和/或 XML 文件中获取配置，由 {@link org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader}
 * 解析。在 web 环境中，这本质上相当于 {@link org.springframework.context.support.GenericGroovyApplicationContext}。
 * <p>
 * 默认情况下，根上下文的配置将从 "/WEB-INF/applicationContext.groovy" 获取，
 * 具有命名空间 "test-servlet" 的上下文将从 "/WEB-INF/test-servlet.groovy" 获取
 * （例如，对于 servlet-name 为 "test" 的 DispatcherServlet 实例）。
 * <p>
 * 配置位置的默认值可以通过 {@link org.springframework.web.context.ContextLoader} 的
 * "contextConfigLocation" 上下文参数和 {@link org.springframework.web.servlet.FrameworkServlet}
 * 的 servlet 初始化参数 "contextConfigLocation" 进行覆盖。配置位置可以表示具体的文件，如
 * "/WEB-INF/context.groovy"，也可以表示 Ant 风格的模式，如 "/WEB-INF/*-context.groovy"
 * （请参阅 {@link org.springframework.util.PathMatcher} 的文档以了解模式的详细信息）。注意，
 * ".xml" 文件将被解析为 XML 内容；所有其他类型的资源将被解析为 Groovy 脚本。
 * <p>
 * 注意：在存在多个配置位置的情况下，后加载的文件将覆盖先加载的文件中定义的 bean 定义。
 * 可以利用这一点有意覆盖某些 bean 定义，通过额外的 Groovy 脚本。
 *
 * <b>对于读取不同 bean 定义格式的 WebApplicationContext，请创建 {@link AbstractRefreshableWebApplicationContext}
 * 的类似子类。</b> 这样的上下文实现可以被指定为 ContextLoader 的 "contextClass" 上下文参数
 * 或 FrameworkServlet 的 servlet 初始化参数 "contextClass"。
 *
 * @author Juergen Hoeller
 * @see #setNamespace
 * @see #setConfigLocations
 * @see org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader
 * @see org.springframework.web.context.ContextLoader#initWebApplicationContext
 * @see org.springframework.web.servlet.FrameworkServlet#initWebApplicationContext
 * @since 4.1
 */
@SuppressWarnings("JavadocReference")
public class GroovyWebApplicationContext extends AbstractRefreshableWebApplicationContext implements GroovyObject {

	/**
	 * 根上下文的默认配置位置。
	 */
	public static final String DEFAULT_CONFIG_LOCATION = "/WEB-INF/applicationContext.groovy";

	/**
	 * 用于构建命名空间配置位置的默认前缀。
	 */
	public static final String DEFAULT_CONFIG_LOCATION_PREFIX = "/WEB-INF/";

	/**
	 * 用于构建命名空间配置位置的默认后缀。
	 */
	public static final String DEFAULT_CONFIG_LOCATION_SUFFIX = ".groovy";


	/**
	 * 上下文Bean包装器
	 */
	private final BeanWrapper contextWrapper = new BeanWrapperImpl(this);

	/**
	 * 元类
	 */
	private MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());


	/**
	 * 通过 GroovyBeanDefinitionReader 加载 bean 定义。
	 *
	 * @see org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader
	 * @see #initBeanDefinitionReader
	 * @see #loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
		// 为给定的 BeanFactory 创建一个新的 GroovyBeanDefinitionReader。
		GroovyBeanDefinitionReader beanDefinitionReader = new GroovyBeanDefinitionReader(beanFactory);

		// 使用此上下文的资源加载环境配置 bean 定义读取器。
		beanDefinitionReader.setEnvironment(getEnvironment());
		beanDefinitionReader.setResourceLoader(this);

		// 允许子类提供读者的自定义初始化，然后继续加载 bean 定义。
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
	}

	/**
	 * 初始化用于加载此上下文的 bean 定义读取器。默认实现为空。
	 * <p>可以在子类中重写。
	 *
	 * @param beanDefinitionReader 此上下文使用的 bean 定义读取器
	 */
	protected void initBeanDefinitionReader(GroovyBeanDefinitionReader beanDefinitionReader) {
	}

	/**
	 * 使用给定的 GroovyBeanDefinitionReader 加载 bean 定义。
	 * <p>Bean 工厂的生命周期由 refreshBeanFactory 方法处理；因此，此方法只是用于加载和/或注册 bean 定义。
	 * <p>委托给 ResourcePatternResolver 将位置模式解析为 Resource 实例。
	 *
	 * @throws IOException 如果找不到所需的 Groovy 脚本或 XML 文件
	 * @see #refreshBeanFactory
	 * @see #getConfigLocations
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	protected void loadBeanDefinitions(GroovyBeanDefinitionReader reader) throws IOException {
		// 获取配置位置
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (String configLocation : configLocations) {
				// 加载配置中的Bean定义
				reader.loadBeanDefinitions(configLocation);
			}
		}
	}

	/**
	 * 获取默认的配置位置数组。对于根上下文，默认位置是 "/WEB-INF/applicationContext.groovy"，
	 * 对于命名空间为 "test-servlet" 的上下文，默认位置是 "/WEB-INF/test-servlet.groovy"
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


	// Implementation of the GroovyObject interface

	@Override
	public void setMetaClass(MetaClass metaClass) {
		this.metaClass = metaClass;
	}

	@Override
	public MetaClass getMetaClass() {
		return this.metaClass;
	}

	@Override
	public Object invokeMethod(String name, Object args) {
		return this.metaClass.invokeMethod(this, name, args);
	}

	@Override
	public void setProperty(String property, Object newValue) {
		this.metaClass.setProperty(this, property, newValue);
	}

	@Override
	@Nullable
	public Object getProperty(String property) {
		// 检查是否包含名为property的Bean
		if (containsBean(property)) {
			// 如果包含，通过getBean方法获取该Bean
			return getBean(property);
		} else if (this.contextWrapper.isReadableProperty(property)) {
			// 如果不包含，但是上下文包装器可读取该属性，则通过contextWrapper的getPropertyValue方法获取属性值
			return this.contextWrapper.getPropertyValue(property);
		}

		// 如果都不满足，则抛出NoSuchBeanDefinitionException异常
		throw new NoSuchBeanDefinitionException(property);
	}

}
