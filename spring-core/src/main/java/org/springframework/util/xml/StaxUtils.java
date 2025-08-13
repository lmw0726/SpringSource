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
import org.springframework.util.StreamUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import java.util.List;
import java.util.function.Supplier;

/**
 * 用于简化StAX API操作的工具类。部分方法因JAXP 1.3兼容性而保留；
 * 从Spring 4.0开始，依赖于JDK 1.6及以上版本内置的JAXP 1.4。
 *
 * <p>特别提供了将StAX ({@code javax.xml.stream})与TrAX API ({@code javax.xml.transform})
 * 结合使用的方法，以及StAX读写器与SAX读写器/处理器之间的转换方法。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class StaxUtils {

	private static final XMLResolver NO_OP_XML_RESOLVER =
			(publicID, systemID, base, ns) -> StreamUtils.emptyInput();


	/**
	 * 创建具有Spring防御性配置的{@link XMLInputFactory}，
	 * 即不支持DTD和外部实体解析。
	 * @return 新创建并经过防御性初始化的输入工厂实例
	 * @since 5.0
	 */
	public static XMLInputFactory createDefensiveInputFactory() {
		return createDefensiveInputFactory(XMLInputFactory::newInstance);
	}

	/**
	 * {@link #createDefensiveInputFactory()}的变体，支持自定义实例创建。
	 * @param instanceSupplier 输入工厂实例的供应商
	 * @return 新创建并经过防御性初始化的输入工厂实例
	 * @since 5.0.12
	 */
	public static <T extends XMLInputFactory> T createDefensiveInputFactory(Supplier<T> instanceSupplier) {
		T inputFactory = instanceSupplier.get();
		inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		inputFactory.setXMLResolver(NO_OP_XML_RESOLVER);
		return inputFactory;
	}

	/**
	 * 为给定的{@link XMLStreamReader}创建JAXP 1.4 {@link StAXSource}。
	 * @param streamReader StAX流读取器
	 * @return 包装{@code streamReader}的源对象
	 */
	public static Source createStaxSource(XMLStreamReader streamReader) {
		return new StAXSource(streamReader);
	}

	/**
	 * 为给定的{@link XMLEventReader}创建JAXP 1.4 {@link StAXSource}。
	 * @param eventReader StAX事件读取器
	 * @return 包装{@code eventReader}的源对象
	 * @throws XMLStreamException 如果创建源时出错
	 */
	public static Source createStaxSource(XMLEventReader eventReader) throws XMLStreamException {
		return new StAXSource(eventReader);
	}

	/**
	 * 为给定的{@link XMLStreamReader}创建自定义的非JAXP 1.4 StAX {@link Source}。
	 * @param streamReader StAX流读取器
	 * @return 包装{@code streamReader}的源对象
	 */
	public static Source createCustomStaxSource(XMLStreamReader streamReader) {
		return new StaxSource(streamReader);
	}

	/**
	 * 为给定的{@link XMLEventReader}创建自定义的非JAXP 1.4 StAX {@link Source}。
	 * @param eventReader StAX事件读取器
	 * @return 包装{@code eventReader}的源对象
	 */
	public static Source createCustomStaxSource(XMLEventReader eventReader) {
		return new StaxSource(eventReader);
	}

	/**
	 * 判断给定的{@link Source}是否是JAXP 1.4 StAX Source或自定义StAX Source。
	 * @param source 要检查的源对象
	 * @return 如果{@code source}是JAXP 1.4 {@link StAXSource}或自定义StAX Source则返回{@code true}；
	 * 否则返回{@code false}
	 */
	public static boolean isStaxSource(Source source) {
		return (source instanceof StAXSource || source instanceof StaxSource);
	}

	/**
	 * 获取给定StAX Source的{@link XMLStreamReader}。
	 * @param source JAXP 1.4 {@link StAXSource}源对象
	 * @return 对应的{@link XMLStreamReader}
	 * @throws IllegalArgumentException 如果{@code source}不是JAXP 1.4 {@link StAXSource}
	 * 或自定义StAX Source
	 */
	@Nullable
	public static XMLStreamReader getXMLStreamReader(Source source) {
		if (source instanceof StAXSource) {
			return ((StAXSource) source).getXMLStreamReader();
		}
		else if (source instanceof StaxSource) {
			return ((StaxSource) source).getXMLStreamReader();
		}
		else {
			throw new IllegalArgumentException("Source '" + source + "' is neither StaxSource nor StAXSource");
		}
	}

	/**
	 * 获取给定StAX Source的{@link XMLEventReader}。
	 * @param source JAXP 1.4 {@link StAXSource}源对象
	 * @return 对应的{@link XMLEventReader}
	 * @throws IllegalArgumentException 如果{@code source}不是JAXP 1.4 {@link StAXSource}
	 * 或自定义StAX Source
	 */
	@Nullable
	public static XMLEventReader getXMLEventReader(Source source) {
		if (source instanceof StAXSource) {
			return ((StAXSource) source).getXMLEventReader();
		}
		else if (source instanceof StaxSource) {
			return ((StaxSource) source).getXMLEventReader();
		}
		else {
			throw new IllegalArgumentException("Source '" + source + "' is neither StaxSource nor StAXSource");
		}
	}

	/**
	 * 为给定的{@link XMLStreamWriter}创建JAXP 1.4 {@link StAXResult}。
	 * @param streamWriter StAX流写入器
	 * @return 包装{@code streamWriter}的结果对象
	 */
	public static Result createStaxResult(XMLStreamWriter streamWriter) {
		return new StAXResult(streamWriter);
	}

	/**
	 * 为给定的{@link XMLEventWriter}创建JAXP 1.4 {@link StAXResult}。
	 * @param eventWriter StAX事件写入器
	 * @return 包装{@code streamReader}的结果对象
	 */
	public static Result createStaxResult(XMLEventWriter eventWriter) {
		return new StAXResult(eventWriter);
	}

	/**
	 * 为给定的{@link XMLStreamWriter}创建自定义的非JAXP 1.4 StAX {@link Result}。
	 * @param streamWriter StAX流写入器
	 * @return 包装{@code streamWriter}的结果对象
	 */
	public static Result createCustomStaxResult(XMLStreamWriter streamWriter) {
		return new StaxResult(streamWriter);
	}

	/**
	 * 为给定的{@link XMLEventWriter}创建自定义的非JAXP 1.4 StAX {@link Result}。
	 * @param eventWriter StAX事件写入器
	 * @return 包装{@code eventWriter}的结果对象
	 */
	public static Result createCustomStaxResult(XMLEventWriter eventWriter) {
		return new StaxResult(eventWriter);
	}

	/**
	 * 判断给定的{@link Result}是否是JAXP 1.4 StAX Result或自定义StAX Result。
	 * @param result 要检查的结果对象
	 * @return 如果{@code result}是JAXP 1.4 {@link StAXResult}或自定义StAX Result则返回{@code true}；
	 * 否则返回{@code false}
	 */
	public static boolean isStaxResult(Result result) {
		return (result instanceof StAXResult || result instanceof StaxResult);
	}

	/**
	 * 获取给定StAX Result的{@link XMLStreamWriter}。
	 * @param result JAXP 1.4 {@link StAXResult}结果对象
	 * @return 对应的{@link XMLStreamWriter}
	 * @throws IllegalArgumentException 如果{@code source}不是JAXP 1.4 {@link StAXResult}
	 * 或自定义StAX Result
	 */
	@Nullable
	public static XMLStreamWriter getXMLStreamWriter(Result result) {
		if (result instanceof StAXResult) {
			return ((StAXResult) result).getXMLStreamWriter();
		}
		else if (result instanceof StaxResult) {
			return ((StaxResult) result).getXMLStreamWriter();
		}
		else {
			throw new IllegalArgumentException("Result '" + result + "' is neither StaxResult nor StAXResult");
		}
	}

	/**
	 * 获取给定StAX Result的{@link XMLEventWriter}。
	 * @param result JAXP 1.4 {@link StAXResult}结果对象
	 * @return 对应的{@link XMLEventWriter}
	 * @throws IllegalArgumentException 如果{@code source}不是JAXP 1.4 {@link StAXResult}
	 * 或自定义StAX Result
	 */
	@Nullable
	public static XMLEventWriter getXMLEventWriter(Result result) {
		if (result instanceof StAXResult) {
			return ((StAXResult) result).getXMLEventWriter();
		}
		else if (result instanceof StaxResult) {
			return ((StaxResult) result).getXMLEventWriter();
		}
		else {
			throw new IllegalArgumentException("Result '" + result + "' is neither StaxResult nor StAXResult");
		}
	}

	/**
	 * 从给定的{@link XMLEvent}列表创建{@link XMLEventReader}。
	 * @param events {@link XMLEvent}事件列表
	 * @return 从给定事件读取的{@code XMLEventReader}
	 * @since 5.0
	 */
	public static XMLEventReader createXMLEventReader(List<XMLEvent> events) {
		return new ListBasedXMLEventReader(events);
	}

	/**
	 * 创建一个向给定StAX {@link XMLStreamWriter}写入的SAX {@link ContentHandler}。
	 * @param streamWriter StAX流写入器
	 * @return 向{@code streamWriter}写入的内容处理器
	 */
	public static ContentHandler createContentHandler(XMLStreamWriter streamWriter) {
		return new StaxStreamHandler(streamWriter);
	}

	/**
	 * 创建一个向给定StAX {@link XMLEventWriter}写入事件的SAX {@link ContentHandler}。
	 * @param eventWriter StAX事件写入器
	 * @return 向{@code eventWriter}写入的内容处理器
	 */
	public static ContentHandler createContentHandler(XMLEventWriter eventWriter) {
		return new StaxEventHandler(eventWriter);
	}

	/**
	 * 创建一个从给定StAX {@link XMLStreamReader}读取的SAX {@link XMLReader}。
	 * @param streamReader StAX流读取器
	 * @return 从{@code streamWriter}读取的XML读取器
	 */
	public static XMLReader createXMLReader(XMLStreamReader streamReader) {
		return new StaxStreamXMLReader(streamReader);
	}

	/**
	 * 创建一个从给定StAX {@link XMLEventReader}读取的SAX {@link XMLReader}。
	 * @param eventReader StAX事件读取器
	 * @return 从{@code eventWriter}读取的XMLReader
	 */
	public static XMLReader createXMLReader(XMLEventReader eventReader) {
		return new StaxEventXMLReader(eventReader);
	}

	/**
	 * 返回从{@link XMLEventReader}读取的{@link XMLStreamReader}。
	 * 这个方法很有用，因为StAX {@code XMLInputFactory}允许从流读取器创建事件读取器，
	 * 但不支持反向操作。
	 * @param eventReader 事件读取器
	 * @return 从事件读取器读取的流读取器
	 * @throws XMLStreamException 如果创建流读取器时出错
	 */
	public static XMLStreamReader createEventStreamReader(XMLEventReader eventReader) throws XMLStreamException {
		return new XMLEventStreamReader(eventReader);
	}

	/**
	 * 返回写入到{@link XMLEventWriter}的{@link XMLStreamWriter}。
	 * @param eventWriter 事件写入器
	 * @return 写入到事件写入器的流写入器
	 * @since 3.2
	 */
	public static XMLStreamWriter createEventStreamWriter(XMLEventWriter eventWriter) {
		return new XMLEventStreamWriter(eventWriter, XMLEventFactory.newFactory());
	}

	/**
	 * 返回写入到{@link XMLEventWriter}的{@link XMLStreamWriter}。
	 * @param eventWriter 事件写入器
	 * @param eventFactory 用于创建事件的工厂
	 * @return 写入到事件写入器的流写入器
	 * @since 3.0.5
	 */
	public static XMLStreamWriter createEventStreamWriter(XMLEventWriter eventWriter, XMLEventFactory eventFactory) {
		return new XMLEventStreamWriter(eventWriter, eventFactory);
	}

}
