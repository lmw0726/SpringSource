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

import org.springframework.http.converter.HttpMessageConversionException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 抽象基类，用于使用 JAXB2 的 {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverters}。
 * 懒惰地创建 {@link JAXBContext} 对象。
 *
 * @param <T> 转换后的对象类型
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public abstract class AbstractJaxb2HttpMessageConverter<T> extends AbstractXmlHttpMessageConverter<T> {

	/**
	 * Jax类型和Jarx上下文映射
	 */
	private final ConcurrentMap<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<>(64);


	/**
	 * 为给定类创建一个新的 {@link Marshaller}。
	 *
	 * @param clazz 要为其创建 marshaller 的类
	 * @return {@code Marshaller}
	 * @throws HttpMessageConversionException 在 JAXB 错误的情况下
	 */
	protected final Marshaller createMarshaller(Class<?> clazz) {
		try {
			// 获取指定类的 JAXB上下文
			JAXBContext jaxbContext = getJaxbContext(clazz);
			// 创建 Marshaller 对象
			Marshaller marshaller = jaxbContext.createMarshaller();
			// 自定义 Marshaller 的配置
			customizeMarshaller(marshaller);
			// 返回配置好的 Marshaller 对象
			return marshaller;
		} catch (JAXBException ex) {
			// 如果捕获到 JAXB异常，则抛出 Http消息转换异常
			throw new HttpMessageConversionException(
					"Could not create Marshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 在使用这个消息转换器写对象到输出之前自定义创建的 {@link Marshaller}。
	 *
	 * @param marshaller 要自定义的 marshaller
	 * @see #createMarshaller(Class)
	 * @since 4.0.3
	 */
	protected void customizeMarshaller(Marshaller marshaller) {
	}

	/**
	 * 为给定类创建一个新的 {@link Unmarshaller}。
	 *
	 * @param clazz 要为其创建 unmarshaller 的类
	 * @return {@code Unmarshaller}
	 * @throws HttpMessageConversionException 在 JAXB 错误的情况下
	 */
	protected final Unmarshaller createUnmarshaller(Class<?> clazz) {
		try {
			// 获取指定类的 JAXB上下文
			JAXBContext jaxbContext = getJaxbContext(clazz);
			// 创建 Unmarshaller 对象
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			// 自定义 Unmarshaller 的配置
			customizeUnmarshaller(unmarshaller);
			// 返回配置好的 Unmarshaller 对象
			return unmarshaller;
		} catch (JAXBException ex) {
			// 如果捕获到 JAXB异常，则抛出 Http消息转换异常
			throw new HttpMessageConversionException(
					"Could not create Unmarshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 在使用这个消息转换器从输入读取对象之前自定义创建的 {@link Unmarshaller}。
	 *
	 * @param unmarshaller 要自定义的 unmarshaller
	 * @see #createUnmarshaller(Class)
	 * @since 4.0.3
	 */
	protected void customizeUnmarshaller(Unmarshaller unmarshaller) {
	}

	/**
	 * 返回给定类的 {@link JAXBContext}。
	 *
	 * @param clazz 要返回上下文的类
	 * @return {@code JAXBContext}
	 * @throws HttpMessageConversionException 在 JAXB 错误的情况下
	 */
	protected final JAXBContext getJaxbContext(Class<?> clazz) {
		// 返回并缓存 JAXB上下文 对象，如果缓存中不存在则创建新的实例
		return this.jaxbContexts.computeIfAbsent(clazz, key -> {
			try {
				// 创建并返回新的 JAXB上下文 实例
				return JAXBContext.newInstance(clazz);
			} catch (JAXBException ex) {
				// 如果捕获到 JAXB异常，则抛出 Http消息转换异常
				throw new HttpMessageConversionException(
						"Could not create JAXBContext for class [" + clazz + "]: " + ex.getMessage(), ex);
			}
		});
	}

}
