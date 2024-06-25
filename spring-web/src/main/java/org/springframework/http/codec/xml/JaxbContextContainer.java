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

import org.springframework.core.codec.CodecException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link JAXBContext} 实例的持有者。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 */
final class JaxbContextContainer {
	/**
	 * Jaxb上下文
	 */
	private final ConcurrentMap<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<>(64);

	/**
	 * 创建指定类的 {@link Marshaller} 实例。
	 *
	 * @param clazz 要为其创建 Marshaller 的类
	 * @return 创建的 Marshaller 实例
	 * @throws CodecException 如果无法创建 JAXBContext
	 * @throws JAXBException  如果创建 Marshaller 失败
	 */
	public Marshaller createMarshaller(Class<?> clazz) throws CodecException, JAXBException {
		// 获取Jaxb上下文
		JAXBContext jaxbContext = getJaxbContext(clazz);
		// 创建编组器
		return jaxbContext.createMarshaller();
	}

	/**
	 * 创建指定类的 {@link Unmarshaller} 实例。
	 *
	 * @param clazz 要为其创建 Unmarshaller 的类
	 * @return 创建的 Unmarshaller 实例
	 * @throws CodecException 如果无法创建 JAXBContext
	 * @throws JAXBException  如果创建 Unmarshaller 失败
	 */
	public Unmarshaller createUnmarshaller(Class<?> clazz) throws CodecException, JAXBException {
		// 获取Jaxb上下文
		JAXBContext jaxbContext = getJaxbContext(clazz);
		// 创建反编组器
		return jaxbContext.createUnmarshaller();
	}

	/**
	 * 获取指定类的 {@link JAXBContext} 实例。
	 *
	 * @param clazz 要为其获取 JAXBContext 的类
	 * @return 与指定类相关联的 JAXBContext 实例
	 * @throws CodecException 如果无法创建 JAXBContext
	 */
	private JAXBContext getJaxbContext(Class<?> clazz) throws CodecException {
		// 如果 jaxb上下文列表 中不存在指定类的 JAXB上下文，则进行创建并放入缓存
		return this.jaxbContexts.computeIfAbsent(clazz, key -> {
			try {
				// 尝试为指定类创建 JAXB上下文 实例
				return JAXBContext.newInstance(clazz);
			} catch (JAXBException ex) {
				// 如果创建失败，抛出 编解码器异常，包含详细错误信息
				throw new CodecException(
						"Could not create JAXBContext for class [" + clazz + "]: " + ex.getMessage(), ex);
			}
		});
	}

}
