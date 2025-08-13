/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.util.xml;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.xml.sax.*;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于StAX实现的SAX {@code XMLReader}抽象基类。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see #setContentHandler(org.xml.sax.ContentHandler)
 * @see #setDTDHandler(org.xml.sax.DTDHandler)
 * @see #setEntityResolver(org.xml.sax.EntityResolver)
 * @see #setErrorHandler(org.xml.sax.ErrorHandler)
 */
abstract class AbstractStaxXMLReader extends AbstractXMLReader {

	private static final String NAMESPACES_FEATURE_NAME = "http://xml.org/sax/features/namespaces";

	private static final String NAMESPACE_PREFIXES_FEATURE_NAME = "http://xml.org/sax/features/namespace-prefixes";

	private static final String IS_STANDALONE_FEATURE_NAME = "http://xml.org/sax/features/is-standalone";


	private boolean namespacesFeature = true;

	private boolean namespacePrefixesFeature = false;

	@Nullable
	private Boolean isStandalone;

	private final Map<String, String> namespaces = new LinkedHashMap<>();


	@Override
	public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
		switch (name) {
			case NAMESPACES_FEATURE_NAME:
				return this.namespacesFeature;
			case NAMESPACE_PREFIXES_FEATURE_NAME:
				return this.namespacePrefixesFeature;
			case IS_STANDALONE_FEATURE_NAME:
				if (this.isStandalone != null) {
					return this.isStandalone;
				}
				else {
					throw new SAXNotSupportedException("startDocument() callback not completed yet");
				}
			default:
				return super.getFeature(name);
		}
	}

	@Override
	public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
		if (NAMESPACES_FEATURE_NAME.equals(name)) {
			this.namespacesFeature = value;
		}
		else if (NAMESPACE_PREFIXES_FEATURE_NAME.equals(name)) {
			this.namespacePrefixesFeature = value;
		}
		else {
			super.setFeature(name, value);
		}
	}

	protected void setStandalone(boolean standalone) {
		this.isStandalone = standalone;
	}

	/**
	 * 判断是否启用了SAX特性{@code http://xml.org/sax/features/namespaces}。
	 * @return 如果启用了命名空间特性则返回true，否则返回false
	 */
	protected boolean hasNamespacesFeature() {
		return this.namespacesFeature;
	}

	/**
	 * 判断是否启用了SAX特性{@code http://xml.org/sax/features/namespaces-prefixes}。
	 * @return 如果启用了命名空间前缀特性则返回true，否则返回false
	 */
	protected boolean hasNamespacePrefixesFeature() {
		return this.namespacePrefixesFeature;
	}

	/**
	 * 将{@code QName}转换为DOM和SAX使用的限定名称。
	 * 如果前缀已设置，返回的字符串格式为{@code prefix:localName}；
	 * 否则只返回{@code localName}。
	 * @param qName 要转换的{@code QName}
	 * @return 限定名称字符串
	 */
	protected String toQualifiedName(QName qName) {
		String prefix = qName.getPrefix();
		if (!StringUtils.hasLength(prefix)) {
			return qName.getLocalPart();
		}
		else {
			return prefix + ":" + qName.getLocalPart();
		}
	}


	/**
	 * 解析构造时传入的StAX XML读取器。
	 * <p><b>注意</b>：给定的{@code InputSource}不会被读取，而是被忽略。
	 * @param ignored 被忽略的输入源
	 * @throws SAXException 可能包装了{@code XMLStreamException}的SAX异常
	 */
	@Override
	public final void parse(InputSource ignored) throws SAXException {
		parse();
	}

	/**
	 * 解析构造时传入的StAX XML读取器。
	 * <p><b>注意</b>：给定的系统标识符不会被读取，而是被忽略。
	 * @param ignored 被忽略的系统标识符
	 * @throws SAXException 可能包装了{@code XMLStreamException}的SAX异常
	 */
	@Override
	public final void parse(String ignored) throws SAXException {
		parse();
	}

	private void parse() throws SAXException {
		try {
			parseInternal();
		}
		catch (XMLStreamException ex) {
			Locator locator = null;
			if (ex.getLocation() != null) {
				locator = new StaxLocator(ex.getLocation());
			}
			SAXParseException saxException = new SAXParseException(ex.getMessage(), locator, ex);
			if (getErrorHandler() != null) {
				getErrorHandler().fatalError(saxException);
			}
			else {
				throw saxException;
			}
		}
	}

	/**
	 * 模板方法，用于解析构造时传入的StAX读取器。
	 */
	protected abstract void parseInternal() throws SAXException, XMLStreamException;


	/**
	 * 为给定的前缀启动命名空间映射。
	 * @see org.xml.sax.ContentHandler#startPrefixMapping(String, String)
	 */
	protected void startPrefixMapping(@Nullable String prefix, String namespace) throws SAXException {
		if (getContentHandler() != null && StringUtils.hasLength(namespace)) {
			if (prefix == null) {
				prefix = "";
			}
			if (!namespace.equals(this.namespaces.get(prefix))) {
				getContentHandler().startPrefixMapping(prefix, namespace);
				this.namespaces.put(prefix, namespace);
			}
		}
	}

	/**
	 * 结束给定前缀的命名空间映射。
	 * @see org.xml.sax.ContentHandler#endPrefixMapping(String)
	 */
	protected void endPrefixMapping(String prefix) throws SAXException {
		if (getContentHandler() != null && this.namespaces.containsKey(prefix)) {
			getContentHandler().endPrefixMapping(prefix);
			this.namespaces.remove(prefix);
		}
	}


	/**
	 * 基于给定StAX {@code Location}的{@code Locator}接口实现。
	 * @see Locator
	 * @see Location
	 */
	private static class StaxLocator implements Locator {

		private final Location location;

		public StaxLocator(Location location) {
			this.location = location;
		}

		@Override
		public String getPublicId() {
			return this.location.getPublicId();
		}

		@Override
		public String getSystemId() {
			return this.location.getSystemId();
		}

		@Override
		public int getLineNumber() {
			return this.location.getLineNumber();
		}

		@Override
		public int getColumnNumber() {
			return this.location.getColumnNumber();
		}
	}

}
