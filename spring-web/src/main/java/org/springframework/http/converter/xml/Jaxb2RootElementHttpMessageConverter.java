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

package org.springframework.http.converter.xml;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.bind.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

/**
 * 实现了 {@link org.springframework.http.converter.HttpMessageConverter
 * HttpMessageConverter} 接口，用于使用 JAXB2 读取和写入 XML。
 *
 * <p>此转换器可以读取使用 {@link XmlRootElement} 和 {@link XmlType} 注解的类，
 * 并且可以写入使用 {@link XmlRootElement} 注解的类或其子类。
 *
 * <p>注意: 当使用 Spring 的 {@code spring-oxm} 中的 Marshaller/Unmarshaller 抽象时，
 * 应该使用 {@link MarshallingHttpMessageConverter}。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see MarshallingHttpMessageConverter
 * @since 3.0
 */
public class Jaxb2RootElementHttpMessageConverter extends AbstractJaxb2HttpMessageConverter<Object> {
	/**
	 * 是否支持DTD
	 */
	private boolean supportDtd = false;

	/**
	 * 是否 处理外部实体
	 */
	private boolean processExternalEntities = false;


	/**
	 * 设置是否支持 DTD 解析。
	 * <p>默认为 {@code false}，表示禁用 DTD。
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
	 * 设置在转换为 Source 时是否处理外部 XML 实体。
	 * <p>默认为 {@code false}，表示不解析外部实体。
	 * <p><strong>注意:</strong> 将此选项设置为 {@code true} 也会自动将 {@link #setSupportDtd}
	 * 设置为 {@code true}。
	 */
	public void setProcessExternalEntities(boolean processExternalEntities) {
		this.processExternalEntities = processExternalEntities;
		// 如果需要处理外部实体
		if (processExternalEntities) {
			// 启用支持 DTD
			this.supportDtd = true;
		}
	}

	/**
	 * 返回是否允许处理外部 XML 实体。
	 */
	public boolean isProcessExternalEntities() {
		return this.processExternalEntities;
	}


	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return (clazz.isAnnotationPresent(XmlRootElement.class) || clazz.isAnnotationPresent(XmlType.class)) &&
				canRead(mediaType);
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return (AnnotationUtils.findAnnotation(clazz, XmlRootElement.class) != null && canWrite(mediaType));
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// 不应该被调用，因为我们重写了canRead/Write
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object readFromSource(Class<?> clazz, HttpHeaders headers, Source source) throws Exception {
		try {
			// 处理源数据
			source = processSource(source);
			// 创建 Unmarshaller 对象
			Unmarshaller unmarshaller = createUnmarshaller(clazz);
			// 如果类标注了 @XmlRootElement
			if (clazz.isAnnotationPresent(XmlRootElement.class)) {
				// 直接解组源数据并返回
				return unmarshaller.unmarshal(source);
			} else {
				// 否则，以给定的类解组源数据并返回值
				JAXBElement<?> jaxbElement = unmarshaller.unmarshal(source, clazz);
				return jaxbElement.getValue();
			}
		} catch (NullPointerException ex) {
			// 如果出现空指针异常
			if (!isSupportDtd()) {
				// 如果不支持 DTD
				throw new IllegalStateException("NPE while unmarshalling. " +
						"This can happen due to the presence of DTD declarations which are disabled.", ex);
			}
			// 抛出空指针异常
			throw ex;
		} catch (UnmarshalException ex) {
			// 抛出 UnmarshalException 异常
			throw ex;
		} catch (JAXBException ex) {
			// 抛出 JAXBException 异常
			throw new HttpMessageConversionException("Invalid JAXB setup: " + ex.getMessage(), ex);
		}
	}

	@SuppressWarnings("deprecation")  // on JDK 9
	protected Source processSource(Source source) {
		if (source instanceof StreamSource) {
			// 如果源数据是 StreamSource 类型
			StreamSource streamSource = (StreamSource) source;
			// 根据 流源 获取输入流并创建 InputSource
			InputSource inputSource = new InputSource(streamSource.getInputStream());
			try {
				// 创建 XML读取器
				XMLReader xmlReader = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
				// 设置禁用文档类型声明特性
				xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
				// 设置外部通用实体特性
				String featureName = "http://xml.org/sax/features/external-general-entities";
				xmlReader.setFeature(featureName, isProcessExternalEntities());
				// 如果不处理外部实体
				if (!isProcessExternalEntities()) {
					// 设置实体解析器为 无操作实体解析器
					xmlReader.setEntityResolver(NO_OP_ENTITY_RESOLVER);
				}
				// 返回用 XML读取器 和 输入源 创建的 SAX源
				return new SAXSource(xmlReader, inputSource);
			} catch (SAXException ex) {
				// 如果无法禁用外部实体处理，记录警告并返回原始 来源
				logger.warn("Processing of external entities could not be disabled", ex);
				return source;
			}
		} else {
			// 对于非 流源 类型的源数据，直接返回原始 来源
			return source;
		}
	}

	@Override
	protected void writeToResult(Object o, HttpHeaders headers, Result result) throws Exception {
		try {
			// 获取对象的用户类
			Class<?> clazz = ClassUtils.getUserClass(o);
			// 创建 Marshaller 对象
			Marshaller marshaller = createMarshaller(clazz);
			// 设置字符集
			setCharset(headers.getContentType(), marshaller);
			// 执行对象 的编组操作到 结果 中
			marshaller.marshal(o, result);
		} catch (MarshalException ex) {
			// 抛出 MarshalException 异常
			throw ex;
		} catch (JAXBException ex) {
			// 抛出 JAXBException 异常
			throw new HttpMessageConversionException("Invalid JAXB setup: " + ex.getMessage(), ex);
		}
	}

	private void setCharset(@Nullable MediaType contentType, Marshaller marshaller) throws PropertyException {
		if (contentType != null && contentType.getCharset() != null) {
			// 如果 内容类型 和其字符集不为空，则设置编码属性
			marshaller.setProperty(Marshaller.JAXB_ENCODING, contentType.getCharset().name());
		}
	}

	/**
	 * 无操作实体解析器
	 */
	private static final EntityResolver NO_OP_ENTITY_RESOLVER =
			(publicId, systemId) -> new InputSource(new StringReader(""));

}
