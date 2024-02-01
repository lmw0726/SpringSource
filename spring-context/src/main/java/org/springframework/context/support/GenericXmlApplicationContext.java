/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * 带有内置XML支持的方便的应用程序上下文。
 * 这是{@link ClassPathXmlApplicationContext}和{@link FileSystemXmlApplicationContext}的灵活替代方案，
 * 可通过setter进行配置，最终通过{@link #refresh()}调用激活上下文。
 *
 * <p>在存在多个配置文件的情况下，后续文件中的bean定义将覆盖先前文件中定义的bean。
 * 这可以通过在列表中追加额外配置文件的方式有意地覆盖某些bean定义。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see #load
 * @see XmlBeanDefinitionReader
 * @see org.springframework.context.annotation.AnnotationConfigApplicationContext
 * @since 3.0
 */
public class GenericXmlApplicationContext extends GenericApplicationContext {

	/**
	 * XMLBean定义读取器
	 */
	private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);


	/**
	 * 创建一个新的GenericXmlApplicationContext，需要通过{@link #load}进行加载，
	 * 然后手动通过{@link #refresh}进行刷新。
	 */
	public GenericXmlApplicationContext() {
	}

	/**
	 * 创建一个新的GenericXmlApplicationContext，从给定的资源加载bean定义，
	 * 并自动刷新上下文。
	 *
	 * @param resources 要加载的资源
	 */
	public GenericXmlApplicationContext(Resource... resources) {
		load(resources);
		refresh();
	}

	/**
	 * 创建一个新的GenericXmlApplicationContext，从给定的资源位置加载bean定义，
	 * 并自动刷新上下文。
	 *
	 * @param resourceLocations 要加载的资源位置
	 */
	public GenericXmlApplicationContext(String... resourceLocations) {
		load(resourceLocations);
		refresh();
	}

	/**
	 * 创建一个新的GenericXmlApplicationContext，从给定的资源位置加载bean定义，
	 * 并自动刷新上下文。
	 *
	 * @param relativeClass 当加载每个指定资源名称时，其包将被用作前缀的类
	 * @param resourceNames 要加载的资源的相对合格名称
	 */
	public GenericXmlApplicationContext(Class<?> relativeClass, String... resourceNames) {
		load(relativeClass, resourceNames);
		refresh();
	}


	/**
	 * 暴露基础的{@link XmlBeanDefinitionReader}，用于额外的配置功能和{@code loadBeanDefinition}的变体。
	 */
	public final XmlBeanDefinitionReader getReader() {
		return this.reader;
	}

	/**
	 * 设置是否使用XML验证。默认值为{@code true}。
	 *
	 * @param validating 是否进行验证
	 */
	public void setValidating(boolean validating) {
		this.reader.setValidating(validating);
	}

	/**
	 * 将给定的环境委托给底层的{@link XmlBeanDefinitionReader}。
	 * 在调用任何{@code #load}之前应该被调用。
	 *
	 * @param environment 可配置的环境
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		super.setEnvironment(environment);
		this.reader.setEnvironment(getEnvironment());
	}


	//---------------------------------------------------------------------
	// 加载XML bean定义文件的便捷方法
	//---------------------------------------------------------------------

	/**
	 * 从给定的XML资源加载bean定义。
	 *
	 * @param resources 要加载的一个或多个资源
	 */
	public void load(Resource... resources) {
		this.reader.loadBeanDefinitions(resources);
	}

	/**
	 * 从给定的XML资源加载bean定义。
	 *
	 * @param resourceLocations 要加载的一个或多个资源位置
	 */
	public void load(String... resourceLocations) {
		this.reader.loadBeanDefinitions(resourceLocations);
	}

	/**
	 * 从给定的XML资源加载bean定义。
	 *
	 * @param relativeClass 当加载每个指定资源名称时，其包将被用作前缀的类
	 * @param resourceNames 要加载的资源的相对合格名称
	 */
	public void load(Class<?> relativeClass, String... resourceNames) {
		Resource[] resources = new Resource[resourceNames.length];
		for (int i = 0; i < resourceNames.length; i++) {
			resources[i] = new ClassPathResource(resourceNames[i], relativeClass);
		}
		this.load(resources);
	}

}
