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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.xml.XmlValidationModeDetector;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Spring 默认的 {@link DocumentLoader} 实现。
 *
 * <p>使用标准的 JAXP 配置的 XML 解析器加载 {@link Document 文档}。
 * 如果想更改用于加载文档的 {@link DocumentBuilder}，一种策略是在启动 JVM 时
 * 定义相应的 Java 系统属性。例如，要使用 Oracle 的 {@link DocumentBuilder}，
 * 可以像如下方式启动应用程序：
 *
 * <pre class="code">java -Djavax.xml.parsers.DocumentBuilderFactory=oracle.xml.jaxp.JXDocumentBuilderFactory MyMainClass</pre>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class DefaultDocumentLoader implements DocumentLoader {

	/**
	 * 用于配置模式语言以进行验证的JAXP属性。
	 */
	private static final String SCHEMA_LANGUAGE_ATTRIBUTE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	/**
	 * 指示XSD模式语言的JAXP属性值。
	 */
	private static final String XSD_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";


	private static final Log logger = LogFactory.getLog(DefaultDocumentLoader.class);


	/**
	 * 使用标准JAXP配置的XML解析器在提供的 {@link InputSource} 处加载 {@link Document}。
	 */
	@Override
	public Document loadDocument(InputSource inputSource, EntityResolver entityResolver,
								 ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception {
		//创建DocumentBuilderFactory
		DocumentBuilderFactory factory = createDocumentBuilderFactory(validationMode, namespaceAware);
		if (logger.isTraceEnabled()) {
			logger.trace("Using JAXP provider [" + factory.getClass().getName() + "]");
		}
		//创建DocumentBuilder
		DocumentBuilder builder = createDocumentBuilder(factory, entityResolver, errorHandler);
		//解析XML InputSource返回Document对象
		return builder.parse(inputSource);
	}

	/**
	 * 创建{@link DocumentBuilderFactory}实例
	 *
	 * @param validationMode 验证模式： {@link XmlValidationModeDetector#VALIDATION_DTD DTD}
	 *                       或者 {@link XmlValidationModeDetector#VALIDATION_XSD XSD})
	 * @param namespaceAware 返回的工厂是否为XML命名空间提供支持
	 * @return JAXP DocumentBuilderFactory实例
	 * @throws ParserConfigurationException 如果我们未能建立一个合适的DocumentBuilderFactory
	 */
	protected DocumentBuilderFactory createDocumentBuilderFactory(int validationMode, boolean namespaceAware)
			throws ParserConfigurationException {
		//创建DocumentBuilderFactory实例
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//设置命名空间支持
		factory.setNamespaceAware(namespaceAware);

		if (validationMode != XmlValidationModeDetector.VALIDATION_NONE) {
			//开启校验
			factory.setValidating(true);
			if (validationMode == XmlValidationModeDetector.VALIDATION_XSD) {
				//如果是XSD验证模式，强制设置命名空间支持
				factory.setNamespaceAware(true);
				try {
					// 设置 SCHEMA_LANGUAGE_ATTRIBUTE
					factory.setAttribute(SCHEMA_LANGUAGE_ATTRIBUTE, XSD_SCHEMA_LANGUAGE);
				} catch (IllegalArgumentException ex) {
					ParserConfigurationException pcex = new ParserConfigurationException(
							"Unable to validate using XSD: Your JAXP provider [" + factory +
									"] does not support XML Schema. Are you running on Java 1.4 with Apache Crimson? " +
									"Upgrade to Apache Xerces (or Java 1.5) for full XSD support.");
					pcex.initCause(ex);
					throw pcex;
				}
			}
		}

		return factory;
	}

	/**
	 * 创建一个 JAXP DocumentBuilder，供此 BeanDefinitionReader 用于解析 XML 文档。
	 * 子类可以重写此方法，对 DocumentBuilder 进行进一步初始化。
	 *
	 * @param factory        用于创建 DocumentBuilder 的 JAXP DocumentBuilderFactory
	 * @param entityResolver 要使用的 SAX EntityResolver
	 * @param errorHandler   要使用的 SAX ErrorHandler
	 * @return 创建的 JAXP DocumentBuilder
	 * @throws ParserConfigurationException 如果 JAXP 方法抛出异常
	 */
	protected DocumentBuilder createDocumentBuilder(DocumentBuilderFactory factory,
													@Nullable EntityResolver entityResolver, @Nullable ErrorHandler errorHandler)
			throws ParserConfigurationException {

		DocumentBuilder docBuilder = factory.newDocumentBuilder();
		if (entityResolver != null) {
			docBuilder.setEntityResolver(entityResolver);
		}
		if (errorHandler != null) {
			docBuilder.setErrorHandler(errorHandler);
		}
		return docBuilder;
	}

}
