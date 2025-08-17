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

package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.beans.factory.parsing.ReaderEventListener;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.StringReader;

/**
 * {@link org.springframework.beans.factory.parsing.ReaderContext} 的扩展类，
 * 专用于 {@link XmlBeanDefinitionReader}。
 * 提供对 {@link XmlBeanDefinitionReader} 中配置的
 * {@link NamespaceHandlerResolver} 的访问。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class XmlReaderContext extends ReaderContext {

	private final XmlBeanDefinitionReader reader;
	/**
	 * Namespace处理器解析器
	 */
	private final NamespaceHandlerResolver namespaceHandlerResolver;


	/**
	 * 构造一个新的 {@code XmlReaderContext}。
	 *
	 * @param resource                 XML bean定义资源
	 * @param problemReporter          使用中的问题报告者
	 * @param eventListener            正在使用的事件侦听器
	 * @param sourceExtractor          正在使用的源提取器
	 * @param reader                   正在使用的XML bean定义阅读器
	 * @param namespaceHandlerResolver XML命名空间解析器
	 */
	public XmlReaderContext(
			Resource resource, ProblemReporter problemReporter,
			ReaderEventListener eventListener, SourceExtractor sourceExtractor,
			XmlBeanDefinitionReader reader, NamespaceHandlerResolver namespaceHandlerResolver) {

		super(resource, problemReporter, eventListener, sourceExtractor);
		this.reader = reader;
		this.namespaceHandlerResolver = namespaceHandlerResolver;
	}


	/**
	 * 返回正在使用的XML bean定义读取器。
	 */
	public final XmlBeanDefinitionReader getReader() {
		return this.reader;
	}

	/**
	 * 返回要使用的bean定义注册表。
	 *
	 * @see XmlBeanDefinitionReader#XmlBeanDefinitionReader(BeanDefinitionRegistry)
	 */
	public final BeanDefinitionRegistry getRegistry() {
		return this.reader.getRegistry();
	}

	/**
	 * 返回要使用的资源加载器（如果有）。
	 * <p>在常规场景下，这通常不会为 null，
	 * 并允许访问资源类加载器。
	 *
	 * @see XmlBeanDefinitionReader#setResourceLoader
	 * @see ResourceLoader#getClassLoader()
	 */
	@Nullable
	public final ResourceLoader getResourceLoader() {
		return this.reader.getResourceLoader();
	}

	/**
	 * 返回要使用的 Bean 类加载器（如果有）。
	 * <p>注意，在常规场景下，这通常为 null，
	 * 用于指示延迟解析 Bean 类。
	 *
	 * @see XmlBeanDefinitionReader#setBeanClassLoader
	 */
	@Nullable
	public final ClassLoader getBeanClassLoader() {
		return this.reader.getBeanClassLoader();
	}

	/**
	 * 返回正在使用的环境
	 *
	 * @see XmlBeanDefinitionReader#setEnvironment
	 */
	public final Environment getEnvironment() {
		return this.reader.getEnvironment();
	}

	/**
	 * 返回命名空间解析器。
	 *
	 * @see XmlBeanDefinitionReader#setNamespaceHandlerResolver
	 */
	public final NamespaceHandlerResolver getNamespaceHandlerResolver() {
		return this.namespaceHandlerResolver;
	}


	// 委托给的便利方法

	/**
	 * 调用给定bean定义的bean名称生成器。
	 *
	 * @see XmlBeanDefinitionReader#getBeanNameGenerator()
	 * @see org.springframework.beans.factory.support.BeanNameGenerator#generateBeanName
	 */
	public String generateBeanName(BeanDefinition beanDefinition) {
		return this.reader.getBeanNameGenerator().generateBeanName(beanDefinition, getRegistry());
	}

	/**
	 * 调用给定 Bean 定义的 Bean 名称生成器，
	 * 并使用生成的名称注册该 Bean 定义。
	 *
	 * @see XmlBeanDefinitionReader#getBeanNameGenerator()
	 * @see org.springframework.beans.factory.support.BeanNameGenerator#generateBeanName
	 * @see BeanDefinitionRegistry#registerBeanDefinition
	 */
	public String registerWithGeneratedName(BeanDefinition beanDefinition) {
		String generatedName = generateBeanName(beanDefinition);
		getRegistry().registerBeanDefinition(generatedName, beanDefinition);
		return generatedName;
	}

	/**
	 * 从给定的字符串读取 XML 文档。
	 *
	 * @see #getReader()
	 */
	public Document readDocumentFromString(String documentContent) {
		InputSource is = new InputSource(new StringReader(documentContent));
		try {
			return this.reader.doLoadDocument(is, getResource());
		} catch (Exception ex) {
			throw new BeanDefinitionStoreException("Failed to read XML document", ex);
		}
	}

}
