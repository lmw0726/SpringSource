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

package org.springframework.util.xml;

import org.springframework.lang.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.sax.SAXSource;

/**
 * 实现{@code Source}标记接口的StAX读取器包装类。可以使用{@code XMLEventReader}
 * 或{@code XMLStreamReader}构造。
 *
 * <p>由于JAXP 1.3中没有为StAX读取器提供{@code Source}实现，所以需要此类。
 * JAXP 1.4(JDK 1.6)中提供了{@code StAXSource}，但保留此类以保持向后兼容。
 *
 * <p>尽管{@code StaxSource}继承自{@code SAXSource}，但<strong>不支持</strong>
 * 调用{@code SAXSource}的方法。通常，此类唯一支持的操作是通过{@link #getXMLReader()}
 * 获取的{@code XMLReader}来解析通过{@link #getInputSource()}获取的输入源。
 * 调用{@link #setXMLReader(XMLReader)}或{@link #setInputSource(InputSource)}
 * 将导致{@code UnsupportedOperationException}。
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see XMLEventReader
 * @see XMLStreamReader
 * @see javax.xml.transform.Transformer
 */
class StaxSource extends SAXSource {

	@Nullable
	private XMLEventReader eventReader;

	@Nullable
	private XMLStreamReader streamReader;


	/**
	 * 使用指定的{@code XMLEventReader}构造新的{@code StaxSource}实例。
	 * 提供的事件读取器必须处于{@code XMLStreamConstants.START_DOCUMENT}或
	 * {@code XMLStreamConstants.START_ELEMENT}状态。
	 * @param eventReader 要读取的{@code XMLEventReader}
	 * @throws IllegalStateException 如果读取器不处于文档或元素的开始位置
	 */
	StaxSource(XMLEventReader eventReader) {
		super(new StaxEventXMLReader(eventReader), new InputSource());
		this.eventReader = eventReader;
	}

	/**
	 * 使用指定的{@code XMLStreamReader}构造新的{@code StaxSource}实例。
	 * 提供的流读取器必须处于{@code XMLStreamConstants.START_DOCUMENT}或
	 * {@code XMLStreamConstants.START_ELEMENT}状态。
	 * @param streamReader 要读取的{@code XMLStreamReader}
	 * @throws IllegalStateException 如果读取器不处于文档或元素的开始位置
	 */
	StaxSource(XMLStreamReader streamReader) {
		super(new StaxStreamXMLReader(streamReader), new InputSource());
		this.streamReader = streamReader;
	}


	/**
	 * 返回此{@code StaxSource}使用的{@code XMLEventReader}。
	 * <p>如果此{@code StaxSource}是用{@code XMLStreamReader}创建的，
	 * 则结果为{@code null}。
	 * @return 此源使用的StAX事件读取器
	 * @see StaxSource#StaxSource(javax.xml.stream.XMLEventReader)
	 */
	@Nullable
	XMLEventReader getXMLEventReader() {
		return this.eventReader;
	}

	/**
	 * 返回此{@code StaxSource}使用的{@code XMLStreamReader}。
	 * <p>如果此{@code StaxSource}是用{@code XMLEventReader}创建的，
	 * 则结果为{@code null}。
	 * @return 此源使用的StAX流读取器
	 * @see StaxSource#StaxSource(javax.xml.stream.XMLEventReader)
	 */
	@Nullable
	XMLStreamReader getXMLStreamReader() {
		return this.streamReader;
	}


	/**
	 * 抛出{@code UnsupportedOperationException}。
	 * @throws UnsupportedOperationException 总是抛出
	 */
	@Override
	public void setInputSource(InputSource inputSource) {
		throw new UnsupportedOperationException("setInputSource is not supported");
	}

	/**
	 * 抛出{@code UnsupportedOperationException}。
	 * @throws UnsupportedOperationException 总是抛出
	 */
	@Override
	public void setXMLReader(XMLReader reader) {
		throw new UnsupportedOperationException("setXMLReader is not supported");
	}

}
