/*
 * Copyright 2002-2015 the original author or authors.
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

import org.w3c.dom.Document;

import org.springframework.beans.factory.BeanDefinitionStoreException;

/**
 * 用于解析包含 Spring Bean 定义的 XML 文档的 SPI。
 * 由 {@link XmlBeanDefinitionReader} 使用，用于实际解析 DOM 文档。
 *
 * <p>每个待解析文档实例化一次：实现类可以在执行
 * {@code registerBeanDefinitions} 方法期间在实例变量中保存状态，
 * 例如文档中为所有 Bean 定义定义的全局设置。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 18.12.2003
 * @see XmlBeanDefinitionReader#setDocumentReaderClass
 */
public interface BeanDefinitionDocumentReader {

	/**
	 * 从给定的DOM文档中读取bean定义，并在给定的reader上下文中将其注册到注册表中。
	 * @param doc DOM 文档
	 * @param readerContext 读取器的当前上下文
	 * （包括目标注册表和正在解析的资源）
	 * @throws BeanDefinitionStoreException 在解析错误的情况下
	 */
	void registerBeanDefinitions(Document doc, XmlReaderContext readerContext)
			throws BeanDefinitionStoreException;

}
