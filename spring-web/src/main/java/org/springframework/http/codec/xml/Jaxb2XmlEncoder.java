/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractSingleValueEncoder;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;

/**
 * 将单个值编码为包含XML元素的字节流。
 *
 * <p>{@link javax.xml.bind.annotation.XmlElements @XmlElements} 和
 * {@link javax.xml.bind.annotation.XmlElement @XmlElement} 可用于指定如何编排集合。
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @see Jaxb2XmlDecoder
 * @since 5.0
 */
public class Jaxb2XmlEncoder extends AbstractSingleValueEncoder<Object> {
	/**
	 * Jaxb上下文
	 */
	private final JaxbContextContainer jaxbContexts = new JaxbContextContainer();

	/**
	 * 编组器处理器
	 */
	private Function<Marshaller, Marshaller> marshallerProcessor = Function.identity();


	public Jaxb2XmlEncoder() {
		super(MimeTypeUtils.APPLICATION_XML, MimeTypeUtils.TEXT_XML, new MediaType("application", "*+xml"));
	}


	/**
	 * 配置用于定制Marshaller实例的处理器函数。
	 *
	 * @param processor 要使用的函数
	 * @since 5.1.3
	 */
	public void setMarshallerProcessor(Function<Marshaller, Marshaller> processor) {
		this.marshallerProcessor = this.marshallerProcessor.andThen(processor);
	}

	/**
	 * 返回配置的用于定制Marshaller实例的处理器。
	 *
	 * @since 5.1.3
	 */
	public Function<Marshaller, Marshaller> getMarshallerProcessor() {
		return this.marshallerProcessor;
	}


	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		// 如果调用父类的 canEncode 方法检查可以进行编码
		if (super.canEncode(elementType, mimeType)) {
			// 获取输出类型的 Class 对象
			Class<?> outputClass = elementType.toClass();
			// 判断输出类是否被 @XmlRootElement 或 @XmlType 注解标记
			return (outputClass.isAnnotationPresent(XmlRootElement.class) ||
					outputClass.isAnnotationPresent(XmlType.class));
		} else {
			// 如果不能进行编码，则返回 false
			return false;
		}
	}

	@Override
	protected Flux<DataBuffer> encode(Object value, DataBufferFactory bufferFactory,
									  ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		// 我们依赖于基类中的 doOnDiscard 方法
		// 使用 Mono.fromCallable 包装编码操作的 Callable，返回一个 Flux
		return Mono.fromCallable(() -> encodeValue(value, bufferFactory, valueType, mimeType, hints)).flux();
	}

	@Override
	public DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory,
								  ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (!Hints.isLoggingSuppressed(hints)) {
			// 如果日志不被抑制，则输出调试日志
			LogFormatUtils.traceDebug(logger, traceOn -> {
				// 格式化输出值，输出详细信息
				String formatted = LogFormatUtils.formatValue(value, !traceOn);
				return Hints.getLogPrefix(hints) + "Encoding [" + formatted + "]";
			});
		}

		boolean release = true;
		// 使用 缓冲工厂 分配一个大小为 1024 的 DataBuffer
		DataBuffer buffer = bufferFactory.allocateBuffer(1024);
		try {
			// 获取 缓冲区 的输出流
			OutputStream outputStream = buffer.asOutputStream();
			// 获取 值 的实际类
			Class<?> clazz = ClassUtils.getUserClass(value);
			// 初始化 编组器
			Marshaller marshaller = initMarshaller(clazz);
			// 将 值 编组到 输出流
			marshaller.marshal(value, outputStream);
			// 如果成功编组，则将 释放标志 设置为 false
			release = false;
			// 返回 DataBuffer
			return buffer;
		} catch (MarshalException ex) {
			// 处理编组异常，抛出编码异常
			throw new EncodingException("Could not marshal " + value.getClass() + " to XML", ex);
		} catch (JAXBException ex) {
			// 处理 JAXB 配置异常，抛出编解码异常
			throw new CodecException("Invalid JAXB configuration", ex);
		} finally {
			// 如果 释放标志 为 true，则释放 buffer 资源
			if (release) {
				DataBufferUtils.release(buffer);
			}
		}
	}

	private Marshaller initMarshaller(Class<?> clazz) throws CodecException, JAXBException {
		// 使用 jaxb上下文 创建 编组器 实例
		Marshaller marshaller = this.jaxbContexts.createMarshaller(clazz);
		// 设置编码属性为 UTF-8
		marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
		// 使用 编组器处理器 对 编组器 实例进行处理
		marshaller = this.marshallerProcessor.apply(marshaller);
		// 返回处理后的 编组器 实例
		return marshaller;
	}

}
