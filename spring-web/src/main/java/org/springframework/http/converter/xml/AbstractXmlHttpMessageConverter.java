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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;

/**
 * 抽象基类，用于实现从/到 XML 的 {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}。
 *
 * <p>默认情况下，此转换器的子类支持 {@code text/xml}、{@code application/xml} 和 {@code application/*-xml}。
 * 可以通过设置 {@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes} 属性来覆盖这些设置。
 *
 * @param <T> 转换后的对象类型
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class AbstractXmlHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {
	/**
	 * 转换器工厂
	 */
	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();


	/**
	 * 受保护的构造函数，将 {@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes} 设置为
	 * {@code text/xml}、{@code application/xml} 和 {@code application/*-xml}。
	 */
	protected AbstractXmlHttpMessageConverter() {
		super(MediaType.APPLICATION_XML, MediaType.TEXT_XML, new MediaType("application", "*+xml"));
	}


	@Override
	public final T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		try {
			// 获取输入消息主体的输入流并包装成一个不会关闭的输入流
			InputStream inputStream = StreamUtils.nonClosing(inputMessage.getBody());
			// 从 流源 读取数据并反序列化为指定的类类型对象
			return readFromSource(clazz, inputMessage.getHeaders(), new StreamSource(inputStream));
		} catch (IOException | HttpMessageConversionException ex) {
			// 如果捕获到 IO异常 或 Http消息转换异常，则重新抛出异常
			throw ex;
		} catch (Exception ex) {
			// 如果捕获到其他异常，则抛出 Http消息不可读异常
			throw new HttpMessageNotReadableException("Could not unmarshal to [" + clazz + "]: " + ex.getMessage(),
					ex, inputMessage);
		}
	}

	@Override
	protected final void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		try {
			// 将 对象 写入结果，传递输出消息的头信息和输出流
			writeToResult(t, outputMessage.getHeaders(), new StreamResult(outputMessage.getBody()));
		} catch (IOException | HttpMessageConversionException ex) {
			// 如果捕获到 IO异常 或 Http消息转换异常，则重新抛出异常
			throw ex;
		} catch (Exception ex) {
			// 如果捕获到其他异常，则抛出 Http消息不可写异常
			throw new HttpMessageNotWritableException("Could not marshal [" + t + "]: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 将给定的 {@code Source} 转换为 {@code Result}。
	 *
	 * @param source 要转换的源
	 * @param result 要转换的结果
	 * @throws TransformerException 如果发生转换错误
	 */
	protected void transform(Source source, Result result) throws TransformerException {
		this.transformerFactory.newTransformer().transform(source, result);
	}


	/**
	 * 从 {@link #read(Class, HttpInputMessage)} 调用的抽象模板方法。
	 *
	 * @param clazz   要返回对象的类型
	 * @param headers HTTP 输入头
	 * @param source  HTTP 输入主体
	 * @return 转换后的对象
	 * @throws Exception 如果发生 I/O 或转换错误
	 */
	protected abstract T readFromSource(Class<? extends T> clazz, HttpHeaders headers, Source source) throws Exception;

	/**
	 * 从 {@link #writeInternal(Object, HttpOutputMessage)} 调用的抽象模板方法。
	 *
	 * @param t       要写入到输出消息的对象
	 * @param headers HTTP 输出头
	 * @param result  HTTP 输出主体
	 * @throws Exception 如果发生 I/O 或转换错误
	 */
	protected abstract void writeToResult(T t, HttpHeaders headers, Result result) throws Exception;

}
