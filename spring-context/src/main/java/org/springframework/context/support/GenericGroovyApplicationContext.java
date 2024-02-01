/*
 * Copyright 2002-2021 the original author or authors.
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

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * 一个{@link org.springframework.context.ApplicationContext}实现，扩展了
 * {@link GenericApplicationContext}并实现了{@link GroovyObject}，以便可以使用点解引用语法检索bean，
 * 而不是使用{@link #getBean}。
 *
 * <p>可以将其视为Groovy bean定义的{@link GenericXmlApplicationContext}的等效物，甚至可以升级它，
 * 因为它可以无缝地理解XML bean定义文件。主要区别是，在Groovy脚本中，可以将上下文与内联的bean定义闭包一起使用，如下所示：
 *
 * <pre class="code">
 * import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 *
 * def context = new GenericGroovyApplicationContext()
 * context.reader.beans {
 *     dataSource(BasicDataSource) {                  // &lt;--- invokeMethod
 *         driverClassName = "org.hsqldb.jdbcDriver"
 *         url = "jdbc:hsqldb:mem:grailsDB"
 *         username = "sa"                            // &lt;-- setProperty
 *         password = ""
 *         settings = [mynew:"setting"]
 *     }
 *     sessionFactory(SessionFactory) {
 *         dataSource = dataSource                    // &lt;-- getProperty for retrieving references
 *     }
 *     myService(MyService) {
 *         nestedBean = { AnotherBean bean -&gt;         // &lt;-- setProperty with closure for nested bean
 *             dataSource = dataSource
 *         }
 *     }
 * }
 * context.refresh()
 * </pre>
 *
 * <p>或者，从外部资源加载Groovy bean定义脚本（例如“applicationContext.groovy”文件）：
 *
 * <pre class="code">
 * import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 *
 * beans {
 *     dataSource(BasicDataSource) {
 *         driverClassName = "org.hsqldb.jdbcDriver"
 *         url = "jdbc:hsqldb:mem:grailsDB"
 *         username = "sa"
 *         password = ""
 *         settings = [mynew:"setting"]
 *     }
 *     sessionFactory(SessionFactory) {
 *         dataSource = dataSource
 *     }
 *     myService(MyService) {
 *         nestedBean = { AnotherBean bean -&gt;
 *             dataSource = dataSource
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>使用以下Java代码创建{@code GenericGroovyApplicationContext}
 * （可能使用Ant样式的'*'/'**'位置模式）：
 *
 * <pre class="code">
 * GenericGroovyApplicationContext context = new GenericGroovyApplicationContext();
 * context.load("org/myapp/applicationContext.groovy");
 * context.refresh();
 * </pre>
 *
 * <p>或者更简洁，前提是不需要额外的配置：
 *
 * <pre class="code">
 * ApplicationContext context = new GenericGroovyApplicationContext("org/myapp/applicationContext.groovy");
 * </pre>
 *
 * <p><b>此应用程序上下文还理解XML bean定义文件，允许与Groovy bean定义文件无缝混合和匹配。</b>
 * ".xml"文件将被解析为XML内容；所有其他类型的资源将被解析为Groovy脚本。
 *
 * @author Juergen Hoeller
 * @author Jeff Brown
 * @see org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader
 * @since 4.0
 */
public class GenericGroovyApplicationContext extends GenericApplicationContext implements GroovyObject {

	/**
	 * Groovybean定义读取器
	 */
	private final GroovyBeanDefinitionReader reader = new GroovyBeanDefinitionReader(this);

	/**
	 * 创建上下文包装器
	 */
	private final BeanWrapper contextWrapper = new BeanWrapperImpl(this);

	/**
	 * 元类
	 */
	private MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());


	/**
	 * 创建一个新的GenericGroovyApplicationContext，需要先{@link #load 加载}，然后手动{@link #refresh 刷新}。
	 */
	public GenericGroovyApplicationContext() {
	}

	/**
	 * 创建一个新的GenericGroovyApplicationContext，从给定的资源加载bean定义，并自动刷新上下文。
	 *
	 * @param resources 要加载的资源
	 */
	public GenericGroovyApplicationContext(Resource... resources) {
		load(resources);
		refresh();
	}

	/**
	 * 创建一个新的GenericGroovyApplicationContext，从给定的资源位置加载bean定义，并自动刷新上下文。
	 *
	 * @param resourceLocations 要加载的资源位置
	 */
	public GenericGroovyApplicationContext(String... resourceLocations) {
		load(resourceLocations);
		refresh();
	}

	/**
	 * 创建一个新的GenericGroovyApplicationContext，从给定的资源位置加载bean定义，并自动刷新上下文。
	 *
	 * @param relativeClass 包的类，将在加载每个指定资源名称时用作前缀
	 * @param resourceNames 要加载的资源的相对限定名称
	 */
	public GenericGroovyApplicationContext(Class<?> relativeClass, String... resourceNames) {
		load(relativeClass, resourceNames);
		refresh();
	}


	/**
	 * 提供底层的{@link GroovyBeanDefinitionReader}，以方便访问其上的{@code loadBeanDefinition}方法，
	 * 以及指定内联Groovy bean定义闭包的能力。
	 *
	 * @see GroovyBeanDefinitionReader#loadBeanDefinitions(org.springframework.core.io.Resource...)
	 * @see GroovyBeanDefinitionReader#loadBeanDefinitions(String...)
	 */
	public final GroovyBeanDefinitionReader getReader() {
		return this.reader;
	}

	/**
	 * 将给定的环境委托给底层的{@link GroovyBeanDefinitionReader}。
	 * 应在任何对{@code #load}的调用之前调用。
	 *
	 * @param environment 配置环境
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		super.setEnvironment(environment);
		this.reader.setEnvironment(getEnvironment());
	}

	/**
	 * 从给定的Groovy脚本或XML文件加载bean定义。
	 * <p>注意，".xml"文件将被解析为XML内容；所有其他类型的资源将被解析为Groovy脚本。
	 *
	 * @param resources 要加载的一个或多个资源
	 */
	public void load(Resource... resources) {
		this.reader.loadBeanDefinitions(resources);
	}

	/**
	 * 从给定的Groovy脚本或XML文件加载bean定义。
	 * <p>注意，".xml"文件将被解析为XML内容；所有其他类型的资源将被解析为Groovy脚本。
	 *
	 * @param resourceLocations 要加载的一个或多个资源位置
	 */
	public void load(String... resourceLocations) {
		this.reader.loadBeanDefinitions(resourceLocations);
	}

	/**
	 * 从给定的Groovy脚本或XML文件加载bean定义。
	 * <p>注意，".xml"文件将被解析为XML内容；所有其他类型的资源将被解析为Groovy脚本。
	 *
	 * @param relativeClass 用于加载每个指定资源名称时作为前缀的包的类
	 * @param resourceNames 要加载的资源的相对限定名称
	 */
	public void load(Class<?> relativeClass, String... resourceNames) {
		Resource[] resources = new Resource[resourceNames.length];
		for (int i = 0; i < resourceNames.length; i++) {
			resources[i] = new ClassPathResource(resourceNames[i], relativeClass);
		}
		load(resources);
	}


	// GroovyObject接口的实现

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
		// 检查newValue是否为BeanDefinition类型
		if (newValue instanceof BeanDefinition) {
			// 如果是BeanDefinition类型，则注册为BeanDefinition
			registerBeanDefinition(property, (BeanDefinition) newValue);
		} else {
			// 如果不是BeanDefinition类型，则通过MetaClass设置属性值
			this.metaClass.setProperty(this, property, newValue);
		}
	}

	@Override
	@Nullable
	public Object getProperty(String property) {
		// 检查是否包含指定名称的Bean
		if (containsBean(property)) {
			// 如果包含，则直接获取Bean并返回
			return getBean(property);
		} else if (this.contextWrapper.isReadableProperty(property)) {
			// 如果属性可读，则通过BeanWrapper获取属性值
			return this.contextWrapper.getPropertyValue(property);
		}
		// 如果没有找到对应的Bean或属性，则抛出NoSuchBeanDefinitionException异常
		throw new NoSuchBeanDefinitionException(property);
	}

}
