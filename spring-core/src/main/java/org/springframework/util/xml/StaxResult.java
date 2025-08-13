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

package org.springframework.util.xml;

import org.springframework.lang.Nullable;
import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.sax.SAXResult;

/**
 * 实现{@code Result}标记接口的StAX写入器包装类。可以使用{@code XMLEventConsumer}
 * 或{@code XMLStreamWriter}构造。
 *
 * <p>由于JAXP 1.3中没有为StaxReaders提供{@code Source}实现，所以需要此类。
 * JAXP 1.4(JDK 1.6)中提供了{@code StAXResult}，但保留此类以保持向后兼容。
 *
 * <p>尽管{@code StaxResult}继承自{@code SAXResult}，但<strong>不支持</strong>
 * 调用{@code SAXResult}的方法。通常，此类唯一支持的操作是通过{@link #getHandler()}
 * 获取的{@code ContentHandler}来使用{@code XMLReader}解析输入源。
 * 调用{@link #setHandler(org.xml.sax.ContentHandler)}或
 * {@link #setLexicalHandler(org.xml.sax.ext.LexicalHandler)}
 * 将导致{@code UnsupportedOperationException}。
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see XMLEventWriter
 * @see XMLStreamWriter
 * @see javax.xml.transform.Transformer
 */
class StaxResult extends SAXResult {

	@Nullable
	private XMLEventWriter eventWriter;

	@Nullable
	private XMLStreamWriter streamWriter;


	/**
	 * 使用指定的{@code XMLEventWriter}构造新的{@code StaxResult}实例。
	 * @param eventWriter 要写入的{@code XMLEventWriter}
	 */
	public StaxResult(XMLEventWriter eventWriter) {
		StaxEventHandler handler = new StaxEventHandler(eventWriter);
		super.setHandler(handler);
		super.setLexicalHandler(handler);
		this.eventWriter = eventWriter;
	}

	/**
	 * 使用指定的{@code XMLStreamWriter}构造新的{@code StaxResult}实例。
	 * @param streamWriter 要写入的{@code XMLStreamWriter}
	 */
	public StaxResult(XMLStreamWriter streamWriter) {
		StaxStreamHandler handler = new StaxStreamHandler(streamWriter);
		super.setHandler(handler);
		super.setLexicalHandler(handler);
		this.streamWriter = streamWriter;
	}


	/**
	 * 返回此{@code StaxResult}使用的{@code XMLEventWriter}。
	 * <p>如果此{@code StaxResult}是用{@code XMLStreamWriter}创建的，
	 * 则结果为{@code null}。
	 * @return 此结果使用的StAX事件写入器
	 * @see #StaxResult(javax.xml.stream.XMLEventWriter)
	 */
	@Nullable
	public XMLEventWriter getXMLEventWriter() {
		return this.eventWriter;
	}

	/**
	 * 返回此{@code StaxResult}使用的{@code XMLStreamWriter}。
	 * <p>如果此{@code StaxResult}是用{@code XMLEventConsumer}创建的，
	 * 则结果为{@code null}。
	 * @return 此结果使用的StAX流写入器
	 * @see #StaxResult(javax.xml.stream.XMLStreamWriter)
	 */
	@Nullable
	public XMLStreamWriter getXMLStreamWriter() {
		return this.streamWriter;
	}


	/**
	 * 抛出{@code UnsupportedOperationException}。
	 * @throws UnsupportedOperationException 总是抛出
	 */
	@Override
	public void setHandler(ContentHandler handler) {
		throw new UnsupportedOperationException("setHandler is not supported");
	}

	/**
	 * 抛出{@code UnsupportedOperationException}。
	 * @throws UnsupportedOperationException 总是抛出
	 */
	@Override
	public void setLexicalHandler(LexicalHandler handler) {
		throw new UnsupportedOperationException("setLexicalHandler is not supported");
	}

}
