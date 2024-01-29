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

package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * 独立的XML应用程序上下文，从文件系统或URL获取上下文定义文件，将纯路径解释为相对文件系统位置（例如“mydir/myfile.txt”）。
 * 适用于测试用途以及独立环境。
 *
 * <p><b>注意：</b>纯路径将始终被解释为相对于当前VM工作目录，即使它们以斜杠开头也是如此。
 * （这与Servlet容器中的语义一致。）<b>使用明确的“file:”前缀来强制使用绝对文件路径。</b>
 *
 * <p>可以通过{@link #getConfigLocations}覆盖配置位置的默认值，
 * 配置位置可以是具体文件（如“/myfiles/context.xml”）或Ant风格的模式（如“/myfiles/*-context.xml”）。
 *
 * <p>注意：在多个配置位置的情况下，后面加载的文件中的bean定义将覆盖先前加载的文件中定义的bean定义。
 * 这可以用于通过额外的XML文件有意覆盖某些bean定义。
 *
 * <p><b>这是一个简单的、一站式的便捷应用程序上下文。
 * 考虑结合{@link GenericApplicationContext}类和{@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}以获得更灵活的上下文设置。</b>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getResource
 * @see #getResourceByPath
 * @see GenericApplicationContext
 */
public class FileSystemXmlApplicationContext extends AbstractXmlApplicationContext {

	/**
	 * 为bean样式配置创建一个新的FileSystemXmlApplicationContext。
	 *
	 * @see #setConfigLocation
	 * @see #setConfigLocations
	 * @see #afterPropertiesSet()
	 */
	public FileSystemXmlApplicationContext() {
	}

	/**
	 * 为bean样式配置创建一个新的FileSystemXmlApplicationContext。
	 *
	 * @param parent 父上下文
	 * @see #setConfigLocation
	 * @see #setConfigLocations
	 * @see #afterPropertiesSet()
	 */
	public FileSystemXmlApplicationContext(ApplicationContext parent) {
		super(parent);
	}

	/**
	 * 创建一个新的FileSystemXmlApplicationContext，从给定的XML文件加载定义并自动刷新上下文。
	 *
	 * @param configLocation 文件路径
	 * @throws BeansException 如果上下文创建失败
	 */
	public FileSystemXmlApplicationContext(String configLocation) throws BeansException {
		this(new String[]{configLocation}, true, null);
	}

	/**
	 * 创建一个新的FileSystemXmlApplicationContext，从给定的XML文件加载定义并自动刷新上下文。
	 *
	 * @param configLocations 文件路径数组
	 * @throws BeansException 如果上下文创建失败
	 */
	public FileSystemXmlApplicationContext(String... configLocations) throws BeansException {
		this(configLocations, true, null);
	}

	/**
	 * 创建一个新的FileSystemXmlApplicationContext，从给定的XML文件加载定义并自动刷新上下文。
	 *
	 * @param configLocations 文件路径数组
	 * @param parent          父上下文
	 * @throws BeansException 如果上下文创建失败
	 */
	public FileSystemXmlApplicationContext(String[] configLocations, ApplicationContext parent) throws BeansException {
		this(configLocations, true, parent);
	}

	/**
	 * 创建一个新的FileSystemXmlApplicationContext，从给定的XML文件加载定义。
	 *
	 * @param configLocations 文件路径数组
	 * @param refresh         是否自动刷新上下文，加载所有bean定义并创建所有单例。
	 *                        或者，在进一步配置上下文后手动调用refresh。
	 * @throws BeansException 如果上下文创建失败
	 * @see #refresh()
	 */
	public FileSystemXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
		this(configLocations, refresh, null);
	}

	/**
	 * 创建一个新的FileSystemXmlApplicationContext，从给定的XML文件加载定义，并自动刷新上下文。
	 *
	 * @param configLocations 文件路径数组
	 * @param refresh         是否自动刷新上下文，加载所有bean定义并创建所有单例。
	 *                        或者，在进一步配置上下文后手动调用refresh。
	 * @param parent          父上下文
	 * @throws BeansException 如果上下文创建失败
	 * @see #refresh()
	 */
	public FileSystemXmlApplicationContext(
			String[] configLocations, boolean refresh, @Nullable ApplicationContext parent)
			throws BeansException {

		super(parent);
		setConfigLocations(configLocations);
		if (refresh) {
			refresh();
		}
	}


	/**
	 * 将资源路径解析为文件系统路径。
	 * <p>注意：即使给定的路径以斜杠开头，它也将被解释为相对于当前VM工作目录。
	 * 这与Servlet容器中的语义一致。
	 *
	 * @param path 资源路径
	 * @return 资源处理器
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#getResourceByPath
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		return new FileSystemResource(path);
	}

}
