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

package org.springframework.http.codec.xml;

import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.evt.EventAllocatorImpl;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.xml.StaxUtils;
import reactor.core.publisher.Flux;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventAllocator;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 将 {@link DataBuffer} 流解码为 {@link XMLEvent} 流。
 *
 * <p>给定以下 XML：
 *
 * <pre class="code">
 * &lt;root&gt;
 *     &lt;child&gt;foo&lt;/child&gt;
 *     &lt;child&gt;bar&lt;/child&gt;
 * &lt;/root&gt;
 * </pre>
 * <p>
 * 该解码器将生成包含以下事件的 {@link Flux}：
 *
 * <ol>
 * <li>{@link javax.xml.stream.events.StartDocument}</li>
 * <li>{@link javax.xml.stream.events.StartElement} {@code root}</li>
 * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
 * <li>{@link javax.xml.stream.events.Characters} {@code foo}</li>
 * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
 * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
 * <li>{@link javax.xml.stream.events.Characters} {@code bar}</li>
 * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
 * <li>{@link javax.xml.stream.events.EndElement} {@code root}</li>
 * </ol>
 *
 * <p>注意，此解码器默认情况下未注册，但由其他默认注册的解码器内部使用。
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 5.0
 */
public class XmlEventDecoder extends AbstractDecoder<XMLEvent> {
	/**
	 * 输入工厂
	 */
	private static final XMLInputFactory inputFactory = StaxUtils.createDefensiveInputFactory();

	/**
	 * Aalto是否存在
	 */
	private static final boolean aaltoPresent = ClassUtils.isPresent(
			"com.fasterxml.aalto.AsyncXMLStreamReader", XmlEventDecoder.class.getClassLoader());
	/**
	 * 是否使用Aalto
	 */
	boolean useAalto = aaltoPresent;

	/**
	 * 最大内存大小
	 */
	private int maxInMemorySize = 256 * 1024;


	public XmlEventDecoder() {
		super(MimeTypeUtils.APPLICATION_XML, MimeTypeUtils.TEXT_XML, new MediaType("application", "*+xml"));
	}


	/**
	 * 设置此解码器可以缓冲的最大字节数。这是整个输入的大小，当整体解码时，或
	 * 使用 Aalto XML 的异步解析时，这是一个顶级 XML 树的大小。
	 * 当超过此限制时，将引发 {@link DataBufferLimitException}。
	 * <p>默认情况下设置为 256K。
	 *
	 * @param byteCount 缓冲的最大字节数，或 -1 表示无限制
	 * @since 5.1.11
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
	}

	/**
	 * 返回 {@link #setMaxInMemorySize 配置的} 字节数限制。
	 *
	 * @since 5.1.11
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}


	@Override
	@SuppressWarnings({"rawtypes", "unchecked", "cast"})  // XMLEventReader is Iterator<Object> on JDK 9
	public Flux<XMLEvent> decode(Publisher<DataBuffer> input, ResolvableType elementType,
								 @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (this.useAalto) {
			// 如果使用 Aalto 解析器
			AaltoDataBufferToXmlEvent mapper = new AaltoDataBufferToXmlEvent(this.maxInMemorySize);
			// 将输入转换为 Flux，并使用 mapper 进行映射
			return Flux.from(input)
					.flatMapIterable(mapper)
					// 当处理完成时，调用 mapper 的 endOfInput 方法
					.doFinally(signalType -> mapper.endOfInput());
		} else {
			// 如果不使用 Aalto 解析器
			return DataBufferUtils.join(input, this.maxInMemorySize)
					.flatMapIterable(buffer -> {
						try {
							// 将数据缓冲区转换为输入流
							InputStream is = buffer.asInputStream();
							// 创建 XML 事件读取器
							Iterator eventReader = inputFactory.createXMLEventReader(is);
							// 将事件读取器中的事件添加到结果列表中
							List<XMLEvent> result = new ArrayList<>();
							eventReader.forEachRemaining(event -> result.add((XMLEvent) event));
							// 返回结果列表
							return result;
						} catch (XMLStreamException ex) {
							// 捕获 XML 流异常，抛出解码异常
							throw new DecodingException(ex.getMessage(), ex);
						} finally {
							// 释放数据缓冲区资源
							DataBufferUtils.release(buffer);
						}
					});
		}
	}


	/*
	 * 单独的静态类，用于隔离 Aalto 依赖。
	 */
	private static class AaltoDataBufferToXmlEvent implements Function<DataBuffer, List<? extends XMLEvent>> {
		/**
		 * 输入工厂
		 */
		private static final AsyncXMLInputFactory inputFactory =
				StaxUtils.createDefensiveInputFactory(InputFactoryImpl::new);

		/**
		 * 流读取器
		 */
		private final AsyncXMLStreamReader<AsyncByteBufferFeeder> streamReader =
				inputFactory.createAsyncForByteBuffer();

		/**
		 * 事件分配器
		 */
		private final XMLEventAllocator eventAllocator = EventAllocatorImpl.getDefaultInstance();

		/**
		 * 最大内存大小
		 */
		private final int maxInMemorySize;

		/**
		 * 字节数量
		 */
		private int byteCount;

		/**
		 * 元素深度
		 */
		private int elementDepth;


		public AaltoDataBufferToXmlEvent(int maxInMemorySize) {
			this.maxInMemorySize = maxInMemorySize;
		}


		@Override
		public List<? extends XMLEvent> apply(DataBuffer dataBuffer) {
			try {
				// 增加已处理的字节计数
				increaseByteCount(dataBuffer);
				// 将数据缓冲区中的字节喂给流读取器的输入馈送器
				this.streamReader.getInputFeeder().feedInput(dataBuffer.asByteBuffer());
				// 创建一个列表来存储 XML 事件
				List<XMLEvent> events = new ArrayList<>();
				while (true) {
					// 读取下一个事件，如果事件不完整则退出循环
					if (this.streamReader.next() == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
						// 当前已喂给读取器的数据没有更多事件
						break;
					} else {
						// 分配并添加事件到列表中
						XMLEvent event = this.eventAllocator.allocate(this.streamReader);
						events.add(event);
						// 如果事件是文档结束事件，退出循环
						if (event.isEndDocument()) {
							break;
						}
						// 检查深度并重置字节计数
						checkDepthAndResetByteCount(event);
					}
				}
				// 如果内存使用超过最大限制，抛出限制异常
				if (this.maxInMemorySize > 0 && this.byteCount > this.maxInMemorySize) {
					raiseLimitException();
				}
				// 返回事件列表
				return events;
			} catch (XMLStreamException ex) {
				// 捕获 XML 流异常，抛出解码异常
				throw new DecodingException(ex.getMessage(), ex);
			} finally {
				// 释放数据缓冲区资源
				DataBufferUtils.release(dataBuffer);
			}
		}

		private void increaseByteCount(DataBuffer dataBuffer) {
			// 如果设置了最大内存大小限制
			if (this.maxInMemorySize > 0) {
				// 检查如果当前数据缓冲区的可读字节数加上已处理的字节数超过 Integer.MAX_VALUE
				if (dataBuffer.readableByteCount() > Integer.MAX_VALUE - this.byteCount) {
					// 抛出内存限制异常
					raiseLimitException();
				} else {
					// 否则，增加已处理的字节计数
					this.byteCount += dataBuffer.readableByteCount();
				}
			}
		}

		private void checkDepthAndResetByteCount(XMLEvent event) {
			// 如果设置了最大内存大小限制
			if (this.maxInMemorySize > 0) {
				// 如果事件是开始元素
				if (event.isStartElement()) {
					// 如果元素深度为 1，则重置字节计数为 0，否则保持现有字节计数
					this.byteCount = this.elementDepth == 1 ? 0 : this.byteCount;
					// 增加元素深度计数
					this.elementDepth++;
					// 如果事件是结束元素
				} else if (event.isEndElement()) {
					// 减少元素深度计数
					this.elementDepth--;
					// 如果元素深度为 1，则重置字节计数为 0，否则保持现有字节计数
					this.byteCount = this.elementDepth == 1 ? 0 : this.byteCount;
				}
			}
		}

		private void raiseLimitException() {
			throw new DataBufferLimitException(
					"Exceeded limit on max bytes per XML top-level node: " + this.maxInMemorySize);
		}

		public void endOfInput() {
			this.streamReader.getInputFeeder().endOfInput();
		}
	}


}
