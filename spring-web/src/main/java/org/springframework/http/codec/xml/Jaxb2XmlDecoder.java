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

package org.springframework.http.codec.xml;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.xml.StaxUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 从包含XML元素的字节流解码为{@code Object}s（POJOs）的流。
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @see Jaxb2XmlEncoder
 * @since 5.0
 */
public class Jaxb2XmlDecoder extends AbstractDecoder<Object> {

	/**
	 * JAXB注释的默认值。
	 *
	 * @see XmlRootElement#name()
	 * @see XmlRootElement#namespace()
	 * @see XmlType#name()
	 * @see XmlType#namespace()
	 */
	private static final String JAXB_DEFAULT_ANNOTATION_VALUE = "##default";

	/**
	 * 输入工厂
	 */
	private static final XMLInputFactory inputFactory = StaxUtils.createDefensiveInputFactory();

	/**
	 * XML事件解码器
	 */
	private final XmlEventDecoder xmlEventDecoder = new XmlEventDecoder();

	/**
	 * Jaxb上下文
	 */
	private final JaxbContextContainer jaxbContexts = new JaxbContextContainer();

	/**
	 * 反编组器处理器
	 */
	private Function<Unmarshaller, Unmarshaller> unmarshallerProcessor = Function.identity();

	/**
	 * 最大内存大小
	 */
	private int maxInMemorySize = 256 * 1024;

	/**
	 * 默认构造函数，支持的MIME类型为XML。
	 */
	public Jaxb2XmlDecoder() {
		super(MimeTypeUtils.APPLICATION_XML, MimeTypeUtils.TEXT_XML, new MediaType("application", "*+xml"));
	}

	/**
	 * 使用指定的MIME类型创建{@code Jaxb2XmlDecoder}。
	 *
	 * @param supportedMimeTypes 支持的MIME类型
	 * @since 5.1.9
	 */
	public Jaxb2XmlDecoder(MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
	}


	/**
	 * 配置一个处理器函数来定制Unmarshaller实例。
	 *
	 * @param processor 要使用的函数
	 * @since 5.1.3
	 */
	public void setUnmarshallerProcessor(Function<Unmarshaller, Unmarshaller> processor) {
		this.unmarshallerProcessor = this.unmarshallerProcessor.andThen(processor);
	}

	/**
	 * 返回配置的用于定制Unmarshaller实例的处理器。
	 *
	 * @since 5.1.3
	 */
	public Function<Unmarshaller, Unmarshaller> getUnmarshallerProcessor() {
		return this.unmarshallerProcessor;
	}

	/**
	 * 设置此解码器可以缓冲的最大字节数。
	 * 这可以是解码时整个输入的大小，或者在使用Aalto XML进行异步解析时，是一个顶级XML树的大小。
	 * 当超过此限制时，会抛出{@link DataBufferLimitException}。
	 * <p>默认情况下设置为256K。
	 *
	 * @param byteCount 要缓冲的最大字节数，或-1表示无限制
	 * @since 5.1.11
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
		this.xmlEventDecoder.setMaxInMemorySize(byteCount);
	}

	/**
	 * 返回{@link #setMaxInMemorySize 配置的}字节数限制。
	 *
	 * @since 5.1.11
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		// 获取输出类型的 Class 对象
		Class<?> outputClass = elementType.toClass();

		// 判断输出类是否被 @XmlRootElement 或 @XmlType 注解标记
		// 并且调用父类的 canDecode 方法进行进一步检查
		return (outputClass.isAnnotationPresent(XmlRootElement.class) ||
				outputClass.isAnnotationPresent(XmlType.class)) && super.canDecode(elementType, mimeType);
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
							   @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		// 使用 xml事件解码器 解码输入流为 XMLEvent 类型的 Flux
		Flux<XMLEvent> xmlEventFlux = this.xmlEventDecoder.decode(
				inputStream, ResolvableType.forClass(XMLEvent.class), mimeType, hints);

		// 获取输出类型的 Class 对象
		Class<?> outputClass = elementType.toClass();
		// 获取输出类型对应的 QName
		QName typeName = toQName(outputClass);
		// 将 XMLEvent Flux 按照 类型名称 分割成多个事件列表的 Flux
		Flux<List<XMLEvent>> splitEvents = split(xmlEventFlux, typeName);

		// 对每个事件列表进行映射处理
		return splitEvents.map(events -> {
			// 解组事件列表为指定的输出类对象
			Object value = unmarshal(events, outputClass);
			// 调试日志输出
			LogFormatUtils.traceDebug(logger, traceOn -> {
				String formatted = LogFormatUtils.formatValue(value, !traceOn);
				return Hints.getLogPrefix(hints) + "Decoded [" + formatted + "]";
			});
			// 返回解组后的对象值
			return value;
		});
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked", "cast"})  // XMLEventReader is Iterator<Object> on JDK 9
	public Mono<Object> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
									 @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return DataBufferUtils.join(input, this.maxInMemorySize)
				.map(dataBuffer -> decode(dataBuffer, elementType, mimeType, hints));
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked", "cast"})  // XMLEventReader is Iterator<Object> on JDK 9
	public Object decode(DataBuffer dataBuffer, ResolvableType targetType,
						 @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {

		try {
			// 使用输入工厂创建 XMLEventReader，读取数据缓冲区的输入流，并指定编码
			Iterator eventReader = inputFactory.createXMLEventReader(dataBuffer.asInputStream(), encoding(mimeType));
			// 将事件逐个添加到事件列表中
			List<XMLEvent> events = new ArrayList<>();
			eventReader.forEachRemaining(event -> events.add((XMLEvent) event));
			// 解组事件列表为目标类型的对象
			return unmarshal(events, targetType.toClass());
		} catch (XMLStreamException ex) {
			// XML 流异常，抛出解码异常
			throw new DecodingException(ex.getMessage(), ex);
		} catch (Throwable ex) {
			// 捕获所有其他异常
			Throwable cause = ex.getCause();
			if (cause instanceof XMLStreamException) {
				// 如果异常原因是 XML 流异常，抛出解码异常
				throw new DecodingException(cause.getMessage(), cause);
			} else {
				// 否则，传播原始异常
				throw Exceptions.propagate(ex);
			}
		} finally {
			// 释放数据缓冲区资源
			DataBufferUtils.release(dataBuffer);
		}
	}

	@Nullable
	private static String encoding(@Nullable MimeType mimeType) {
		if (mimeType == null) {
			// 如果 Mime类型 为 null，则返回 null
			return null;
		}

		// 获取 Mime类型 对应的字符集
		Charset charset = mimeType.getCharset();

		if (charset == null) {
			// 如果字符集为 null，则返回 null
			return null;
		} else {
			// 否则返回字符集的名称
			return charset.name();
		}
	}

	private Object unmarshal(List<XMLEvent> events, Class<?> outputClass) {
		try {
			// 初始化 Unmarshaller
			Unmarshaller unmarshaller = initUnmarshaller(outputClass);
			// 创建 XMLEventReader 以从事件流中读取 XML 事件
			XMLEventReader eventReader = StaxUtils.createXMLEventReader(events);

			// 如果输出类有 XmlRootElement 注解，则直接解组为根元素对象
			if (outputClass.isAnnotationPresent(XmlRootElement.class)) {
				return unmarshaller.unmarshal(eventReader);
			} else {
				// 否则，使用指定输出类解组为 JAXBElement
				JAXBElement<?> jaxbElement = unmarshaller.unmarshal(eventReader, outputClass);
				// 返回 JAXBElement 中的实际对象值
				return jaxbElement.getValue();
			}
		} catch (UnmarshalException ex) {
			// 处理解组异常，抛出解码异常
			throw new DecodingException("Could not unmarshal XML to " + outputClass, ex);
		} catch (JAXBException ex) {
			// 处理 JAXB 配置异常，抛出编解码异常
			throw new CodecException("Invalid JAXB configuration", ex);
		}
	}

	private Unmarshaller initUnmarshaller(Class<?> outputClass) throws CodecException, JAXBException {
		// 创建一个 Unmarshaller 实例，用于将 XML 数据转换为 Java 对象
		Unmarshaller unmarshaller = this.jaxbContexts.createUnmarshaller(outputClass);

		// 对 Unmarshaller 实例进行处理，并返回处理结果
		return this.unmarshallerProcessor.apply(unmarshaller);
	}

	/**
	 * 根据JAXB规范中的映射规则，返回给定类的限定名称。
	 */
	QName toQName(Class<?> outputClass) {
		String localPart;
		String namespaceUri;

		// 如果 输出类 有@XmlRootElement注解
		if (outputClass.isAnnotationPresent(XmlRootElement.class)) {
			// 获取@XmlRootElement注解实例
			XmlRootElement annotation = outputClass.getAnnotation(XmlRootElement.class);
			// 从注解中获取名称和命名空间
			localPart = annotation.name();
			namespaceUri = annotation.namespace();
		} else if (outputClass.isAnnotationPresent(XmlType.class)) {
			// 否则，如果 输出类 有@XmlType注解
			// 获取@XmlType注解实例
			XmlType annotation = outputClass.getAnnotation(XmlType.class);
			// 从注解中获取名称和命名空间
			localPart = annotation.name();
			namespaceUri = annotation.namespace();
		} else {
			// 如果既没有@XmlRootElement也没有@XmlType注解
			// 抛出非法参数异常
			throw new IllegalArgumentException("Output class [" + outputClass.getName() +
					"] is neither annotated with @XmlRootElement nor @XmlType");
		}

		// 如果 本地部分 等于JAXB默认值
		if (JAXB_DEFAULT_ANNOTATION_VALUE.equals(localPart)) {
			// 使用类名属性短名作为 本地部分
			localPart = ClassUtils.getShortNameAsProperty(outputClass);
		}

		// 如果 命名空间 等于JAXB默认值
		if (JAXB_DEFAULT_ANNOTATION_VALUE.equals(namespaceUri)) {
			// 获取 输出类 的包
			Package outputClassPackage = outputClass.getPackage();
			// 如果包存在且有@XmlSchema注解
			if (outputClassPackage != null && outputClassPackage.isAnnotationPresent(XmlSchema.class)) {
				// 获取@XmlSchema注解实例
				XmlSchema annotation = outputClassPackage.getAnnotation(XmlSchema.class);
				// 从注解中获取命名空间
				namespaceUri = annotation.namespace();
			}
			// 否则，使用默认的空命名空间
			else {
				namespaceUri = XMLConstants.NULL_NS_URI;
			}
		}

		// 返回包含命名空间和局部名称的 QName 对象
		return new QName(namespaceUri, localPart);
	}

	/**
	 * 将一个包含{@link XMLEvent XMLEvents}的flux拆分为多个XMLEvent列表的flux，每个列表对应树的一个分支，
	 * 该分支以给定的限定名称开始。
	 * 也就是说，给定{@linkplain XmlEventDecoder 这里}显示的XMLEvents，
	 * 和{@code desiredName} "{@code child}"，此方法返回一个flux，其中包含两个列表，
	 * 每个列表包含以"{@code child}"开始的树的特定分支的事件。
	 * <ol>
	 * <li>第一个列表，处理树的第一个分支:
	 * <ol>
	 * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
	 * <li>{@link javax.xml.stream.events.Characters} {@code foo}</li>
	 * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
	 * </ol>
	 * <li>第二个列表，处理树的第二个分支:
	 * <ol>
	 * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
	 * <li>{@link javax.xml.stream.events.Characters} {@code bar}</li>
	 * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
	 * </ol>
	 * </li>
	 * </ol>
	 */
	Flux<List<XMLEvent>> split(Flux<XMLEvent> xmlEventFlux, QName desiredName) {
		return xmlEventFlux.handle(new SplitHandler(desiredName));
	}


	private static class SplitHandler implements BiConsumer<XMLEvent, SynchronousSink<List<XMLEvent>>> {
		/**
		 * 所需名称
		 */
		private final QName desiredName;

		/**
		 * XML事件列表
		 */
		@Nullable
		private List<XMLEvent> events;

		/**
		 * 元素深度
		 */
		private int elementDepth = 0;

		/**
		 * 屏障值
		 */
		private int barrier = Integer.MAX_VALUE;

		public SplitHandler(QName desiredName) {
			this.desiredName = desiredName;
		}

		@Override
		public void accept(XMLEvent event, SynchronousSink<List<XMLEvent>> sink) {
			// 如果事件是起始元素事件
			if (event.isStartElement()) {
				// 如果屏障值为最大整数值
				if (this.barrier == Integer.MAX_VALUE) {
					// 获取起始元素的名称
					QName startElementName = event.asStartElement().getName();
					// 如果起始元素名称是所需的名称
					if (this.desiredName.equals(startElementName)) {
						// 初始化事件列表
						this.events = new ArrayList<>();
						// 设置屏障值为当前元素深度
						this.barrier = this.elementDepth;
					}
				}
				// 增加当前元素深度计数
				this.elementDepth++;
			}

			// 如果当前元素深度大于屏障值
			if (this.elementDepth > this.barrier) {
				// 断言事件列表不为空
				Assert.state(this.events != null, "No XMLEvent List");
				// 将当前事件添加到事件列表
				this.events.add(event);
			}

			// 如果事件是结束元素事件
			if (event.isEndElement()) {
				// 减少当前元素深度计数
				this.elementDepth--;
				// 如果当前元素深度等于屏障值
				if (this.elementDepth == this.barrier) {
					// 重置屏障值为最大整数值
					this.barrier = Integer.MAX_VALUE;
					// 断言事件列表不为空
					Assert.state(this.events != null, "No XMLEvent List");
					// 将事件列表传递给下一个处理器
					sink.next(this.events);
				}
			}
		}
	}

}
