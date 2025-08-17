/*
 * Copyright 2002-2012 the original author or authors.
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

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.springframework.beans.factory.BeanDefinitionStoreException;

/**
 * XML 特定的 BeanDefinitionStoreException 子类，用于包装
 * {@link org.xml.sax.SAXException}，通常是包含错误位置信息的
 * {@link org.xml.sax.SAXParseException}。
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see #getLineNumber()
 * @see org.xml.sax.SAXParseException
 */
@SuppressWarnings("serial")
public class XmlBeanDefinitionStoreException extends BeanDefinitionStoreException {

	/**
	 * 创建一个新的 XmlBeanDefinitionStoreException。
	 * @param resourceDescription Bean 定义来源资源的描述
	 * @param msg 详细信息（直接作为异常消息使用）
	 * @param cause SAXException（通常为 SAXParseException）根本原因
	 * @see org.xml.sax.SAXParseException
	 */
	public XmlBeanDefinitionStoreException(String resourceDescription, String msg, SAXException cause) {
		super(resourceDescription, msg, cause);
	}

	/**
	 * 返回 XML 资源中出错的行号。
	 * @return 如果可用（SAXParseException 情况下）返回行号，否则返回 -1
	 * @see org.xml.sax.SAXParseException#getLineNumber()
	 */
	public int getLineNumber() {
		Throwable cause = getCause();
		if (cause instanceof SAXParseException) {
			return ((SAXParseException) cause).getLineNumber();
		}
		return -1;
	}

}
