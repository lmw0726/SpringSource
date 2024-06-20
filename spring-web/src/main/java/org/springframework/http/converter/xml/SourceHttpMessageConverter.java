/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.converter.xml;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * 实现了 {@link org.springframework.http.converter.HttpMessageConverter}，能够读取和写入 {@link Source} 对象。
 *
 * @param <T> 转换后的对象类型
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public class SourceHttpMessageConverter<T extends Source> extends AbstractHttpMessageConverter<T> {
	/**
	 * 无操作实体解析器
	 */
	private static final EntityResolver NO_OP_ENTITY_RESOLVER =
			(publicId, systemId) -> new InputSource(new StringReader(""));

	/**
	 * 无操作XML解析器
	 */
	private static final XMLResolver NO_OP_XML_RESOLVER =
			(publicID, systemID, base, ns) -> StreamUtils.emptyInput();

	/**
	 * 支持的类型
	 */
	private static final Set<Class<?>> SUPPORTED_CLASSES = new HashSet<>(8);

	static {
		SUPPORTED_CLASSES.add(DOMSource.class);
		SUPPORTED_CLASSES.add(SAXSource.class);
		SUPPORTED_CLASSES.add(StAXSource.class);
		SUPPORTED_CLASSES.add(StreamSource.class);
		SUPPORTED_CLASSES.add(Source.class);
	}

	/**
	 * 转换器工厂
	 */
	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();

	/**
	 * 是否支持Dtd
	 */
	private boolean supportDtd = false;

	/**
	 * 是否处理外部实体
	 */
	private boolean processExternalEntities = false;


	/**
	 * 设置 {@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes}
	 * 为 {@code text/xml}、{@code application/xml} 和 {@code application/*-xml}。
	 */
	public SourceHttpMessageConverter() {
		super(MediaType.APPLICATION_XML, MediaType.TEXT_XML, new MediaType("application", "*+xml"));
	}


	/**
	 * 指示是否支持 DTD 解析。
	 * <p>默认值是 {@code false}，表示禁用 DTD。
	 */
	public void setSupportDtd(boolean supportDtd) {
		this.supportDtd = supportDtd;
	}

	/**
	 * 返回是否支持 DTD 解析。
	 */
	public boolean isSupportDtd() {
		return this.supportDtd;
	}

	/**
	 * 指示在转换为 Source 时是否处理外部 XML 实体。
	 * <p>默认值是 {@code false}，表示不解析外部实体。
	 * <p><strong>注意：</strong> 将此选项设置为 {@code true} 也会自动将 {@link #setSupportDtd} 设置为 {@code true}。
	 */
	public void setProcessExternalEntities(boolean processExternalEntities) {
		// 设置是否处理外部实体的标志
		this.processExternalEntities = processExternalEntities;

		// 如果允许处理外部实体
		if (processExternalEntities) {
			// 同时启用对 DTD 的支持
			this.supportDtd = true;
		}
	}

	/**
	 * 返回是否允许 XML 外部实体。
	 */
	public boolean isProcessExternalEntities() {
		return this.processExternalEntities;
	}


	@Override
	public boolean supports(Class<?> clazz) {
		return SUPPORTED_CLASSES.contains(clazz);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		// 获取不关闭流的输入流
		InputStream body = StreamUtils.nonClosing(inputMessage.getBody());

		// 如果目标类型是 DOMSource
		if (DOMSource.class == clazz) {
			// 读取并返回 DOMSource 类型的对象
			return (T) readDOMSource(body, inputMessage);
		} else if (SAXSource.class == clazz) {
			// 如果目标类型是 SAXSource
			// 读取并返回 SAXSource 类型的对象
			return (T) readSAXSource(body, inputMessage);
		} else if (StAXSource.class == clazz) {
			// 如果目标类型是 StAXSource
			// 读取并返回 StAXSource 类型的对象
			return (T) readStAXSource(body, inputMessage);
		} else if (StreamSource.class == clazz || Source.class == clazz) {
			// 如果目标类型是 StreamSource 或 Source
			// 读取并返回 StreamSource 类型的对象
			return (T) readStreamSource(body);
		} else {
			// 如果目标类型不支持
			// 抛出 HttpMessageNotReadableException 异常
			throw new HttpMessageNotReadableException("Could not read class [" + clazz +
					"]. Only DOMSource, SAXSource, StAXSource, and StreamSource are supported.", inputMessage);
		}
	}

	private DOMSource readDOMSource(InputStream body, HttpInputMessage inputMessage) throws IOException {
		try {
			// 创建一个新的 文档生成器工厂 实例
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			// 设置命名空间支持
			documentBuilderFactory.setNamespaceAware(true);
			// 设置是否禁止 DTD 声明
			documentBuilderFactory.setFeature(
					"http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
			// 设置是否处理外部实体
			documentBuilderFactory.setFeature(
					"http://xml.org/sax/features/external-general-entities", isProcessExternalEntities());
			// 创建 文档生成器
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			// 如果不处理外部实体，则设置一个无操作的 实体解析器
			if (!isProcessExternalEntities()) {
				documentBuilder.setEntityResolver(NO_OP_ENTITY_RESOLVER);
			}
			// 解析输入流并生成 文档对象 对象
			Document document = documentBuilder.parse(body);
			// 返回包含 文档对象 的 DOM来源 对象
			return new DOMSource(document);
		} catch (NullPointerException ex) {
			// 捕获 NullPointerException，并根据是否支持 DTD 抛出不同的异常信息
			if (!isSupportDtd()) {
				throw new HttpMessageNotReadableException("NPE while unmarshalling: This can happen " +
						"due to the presence of DTD declarations which are disabled.", ex, inputMessage);
			}
			throw ex;
		} catch (ParserConfigurationException ex) {
			// 捕获 解析器配置异常 并抛出 Http消息不可读异常 异常
			throw new HttpMessageNotReadableException(
					"Could not set feature: " + ex.getMessage(), ex, inputMessage);
		} catch (SAXException ex) {
			// 捕获 SAX异常 并抛出 Http消息不可读异常 异常
			throw new HttpMessageNotReadableException(
					"Could not parse document: " + ex.getMessage(), ex, inputMessage);
		}
	}

	@SuppressWarnings("deprecation")  // on JDK 9
	private SAXSource readSAXSource(InputStream body, HttpInputMessage inputMessage) throws IOException {
		try {
			// 创建一个 XML读取器 实例
			XMLReader xmlReader = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
			// 设置是否禁止 DTD 声明
			xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
			// 设置是否处理外部实体
			xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", isProcessExternalEntities());
			// 如果不处理外部实体，则设置一个无操作的实体解析器
			if (!isProcessExternalEntities()) {
				xmlReader.setEntityResolver(NO_OP_ENTITY_RESOLVER);
			}
			// 将输入流复制为字节数组
			byte[] bytes = StreamUtils.copyToByteArray(body);
			// 返回包含 XML读取器 和字节数组输入流的 SAX源 对象
			return new SAXSource(xmlReader, new InputSource(new ByteArrayInputStream(bytes)));
		} catch (SAXException ex) {
			// 捕获 SAX异常 并抛出 Http消息不可读异常 异常
			throw new HttpMessageNotReadableException(
					"Could not parse document: " + ex.getMessage(), ex, inputMessage);
		}
	}

	private Source readStAXSource(InputStream body, HttpInputMessage inputMessage) {
		try {
			// 创建一个 XML输入工厂 实例
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			// 设置是否支持 DTD
			inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, isSupportDtd());
			// 设置是否支持外部实体
			inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, isProcessExternalEntities());
			// 如果不处理外部实体，则设置一个无操作的 XML解析器
			if (!isProcessExternalEntities()) {
				inputFactory.setXMLResolver(NO_OP_XML_RESOLVER);
			}
			// 使用输入工厂创建一个 XML流读取器 实例
			XMLStreamReader streamReader = inputFactory.createXMLStreamReader(body);
			// 返回包含 XML流读取器 的 StAX源 对象
			return new StAXSource(streamReader);
		} catch (XMLStreamException ex) {
			// 捕获 XML流异常 并抛出 Http消息不可读异常 异常
			throw new HttpMessageNotReadableException(
					"Could not parse document: " + ex.getMessage(), ex, inputMessage);
		}
	}

	private StreamSource readStreamSource(InputStream body) throws IOException {
		// 将输入流的内容复制到字节数组中
		byte[] bytes = StreamUtils.copyToByteArray(body);
		// 将字节数组包装到 字节数组输入流 中，并返回一个新的 StreamSource 对象
		return new StreamSource(new ByteArrayInputStream(bytes));
	}

	@Override
	@Nullable
	protected Long getContentLength(T t, @Nullable MediaType contentType) {
		// 如果 输入对象 是否是 DOMSource 类型
		if (t instanceof DOMSource) {
			try {
				// 创建一个 计数输出流 实例，用于计算输出的字节数
				CountingOutputStream os = new CountingOutputStream();
				// 将 DOM来源 对象转换并写入 计数输出流 中
				transform(t, new StreamResult(os));
				// 返回计数的字节数
				return os.count;
			} catch (TransformerException ex) {
				// 忽略
			}
		}
		// 如果 t 不是 DOM来源 类型，返回 null
		return null;
	}

	@Override
	protected void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		try {
			// 创建 流结果 对象，将输出消息的主体传递给它
			Result result = new StreamResult(outputMessage.getBody());
			// 将输入对象 转换并写入 流结果 中
			transform(t, result);
		} catch (TransformerException ex) {
			// 如果发生 变压器异常 异常，抛出 Http消息不可写异常 异常
			throw new HttpMessageNotWritableException("Could not transform [" + t + "] to output message", ex);
		}
	}

	private void transform(Source source, Result result) throws TransformerException {
		this.transformerFactory.newTransformer().transform(source, result);
	}


	private static class CountingOutputStream extends OutputStream {
		/**
		 * 计数值
		 */
		long count = 0;

		@Override
		public void write(int b) throws IOException {
			this.count++;
		}

		@Override
		public void write(byte[] b) throws IOException {
			this.count += b.length;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			this.count += len;
		}
	}

}
