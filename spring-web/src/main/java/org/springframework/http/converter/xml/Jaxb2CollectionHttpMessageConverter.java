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
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.xml.StaxUtils;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 使用JAXB2读取XML集合的{@code HttpMessageConverter}。
 *
 * <p>此转换器可以读取包含带有{@link XmlRootElement}和{@link XmlType}注解的类的
 * {@linkplain Collection 集合}。请注意，此转换器不支持写操作。
 *
 * @param <T> 转换后的对象类型
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.2
 */
@SuppressWarnings("rawtypes")
public class Jaxb2CollectionHttpMessageConverter<T extends Collection>
		extends AbstractJaxb2HttpMessageConverter<T> implements GenericHttpMessageConverter<T> {
	/**
	 * XML输入工厂
	 */
	private final XMLInputFactory inputFactory = createXmlInputFactory();


	/**
	 * 始终返回 {@code false}，因为 Jaxb2CollectionHttpMessageConverter
	 * 需要泛型类型信息才能读取集合。
	 */
	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>Jaxb2CollectionHttpMessageConverter 可以读取带有泛型的
	 * {@link Collection}，其中泛型类型是带有 {@link XmlRootElement} 或 {@link XmlType} 注解的 JAXB 类型。
	 */
	@Override
	public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
		// 如果 类型 不是 ParameterizedType 的实例
		if (!(type instanceof ParameterizedType)) {
			// 返回 false
			return false;
		}
		// 将 类型 强制转换为 ParameterizedType 类型
		ParameterizedType parameterizedType = (ParameterizedType) type;
		// 如果 参数化类型 的原始类型不是 Class 的实例
		if (!(parameterizedType.getRawType() instanceof Class)) {
			// 返回 false
			return false;
		}
		// 将原始类型强制转换为 Class 类型
		Class<?> rawType = (Class<?>) parameterizedType.getRawType();
		// 如果 原始类型 不是 Collection 类的子类
		if (!(Collection.class.isAssignableFrom(rawType))) {
			// 返回 false
			return false;
		}
		// 如果 参数化类型 的实际类型参数长度不等于 1
		if (parameterizedType.getActualTypeArguments().length != 1) {
			// 返回 false
			return false;
		}
		// 获取实际类型参数
		Type typeArgument = parameterizedType.getActualTypeArguments()[0];
		// 如果 类型参数 不是 Class 的实例
		if (!(typeArgument instanceof Class)) {
			// 返回 false
			return false;
		}
		// 将实际类型参数强制转换为 Class 类型
		Class<?> typeArgumentClass = (Class<?>) typeArgument;
		// 返回类型参数类是否带有 XmlRootElement 注解或者 XmlType 注解，并且能读取指定的 mediaType
		return (typeArgumentClass.isAnnotationPresent(XmlRootElement.class) ||
				typeArgumentClass.isAnnotationPresent(XmlType.class)) && canRead(mediaType);
	}

	/**
	 * 始终返回 {@code false}，因为 Jaxb2CollectionHttpMessageConverter
	 * 不将集合转换为 XML。
	 */
	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return false;
	}

	/**
	 * 始终返回 {@code false}，因为 Jaxb2CollectionHttpMessageConverter
	 * 不将集合转换为 XML。
	 */
	@Override
	public boolean canWrite(@Nullable Type type, @Nullable Class<?> clazz, @Nullable MediaType mediaType) {
		return false;
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// 不应该被调用，因为我们重写了canRead/Write
		throw new UnsupportedOperationException();
	}

	@Override
	protected T readFromSource(Class<? extends T> clazz, HttpHeaders headers, Source source) throws Exception {
		// 不应该被调用，因为我们为canRead(Class) 返回false
		throw new UnsupportedOperationException();
	}

	@Override
	@SuppressWarnings("unchecked")
	public T read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		ParameterizedType parameterizedType = (ParameterizedType) type;
		// 获取参数类型的原始类型
		T result = createCollection((Class<?>) parameterizedType.getRawType());
		// 获取元素类型
		Class<?> elementClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];

		try {
			// 创建用于反序列化的 Unmarshaller
			Unmarshaller unmarshaller = createUnmarshaller(elementClass);
			// 创建 XMLStreamReader 对象
			XMLStreamReader streamReader = this.inputFactory.createXMLStreamReader(inputMessage.getBody());
			// 移动到根元素的第一个子节点
			int event = moveToFirstChildOfRootElement(streamReader);

			// 循环解析 XML
			while (event != XMLStreamReader.END_DOCUMENT) {
				// 根据元素类是否带有 XmlRootElement 注解选择不同的解析方式
				// 如果元素类型有 @XmlRootElement 注解
				if (elementClass.isAnnotationPresent(XmlRootElement.class)) {
					// 直接解组并添加到结果集合中
					result.add(unmarshaller.unmarshal(streamReader));
				} else if (elementClass.isAnnotationPresent(XmlType.class)) {
					// 如果元素类带有 XmlType 注解，则解组并添加其值到结果集合中
					result.add(unmarshaller.unmarshal(streamReader, elementClass).getValue());
				} else {
					// 如果既不带有 XmlRootElement 注解也不带有 XmlType 注解，抛出异常
					// 不应该发生，因为在 canRead(Type) 方法中已经检查过
					throw new HttpMessageNotReadableException(
							"Cannot unmarshal to [" + elementClass + "]", inputMessage);
				}
				// 移动到下一个 XML 元素
				event = moveToNextElement(streamReader);
			}
			return result;
		} catch (XMLStreamException ex) {
			throw new HttpMessageNotReadableException(
					"Failed to read XML stream: " + ex.getMessage(), ex, inputMessage);
		} catch (UnmarshalException ex) {
			throw new HttpMessageNotReadableException(
					"Could not unmarshal to [" + elementClass + "]: " + ex, ex, inputMessage);
		} catch (JAXBException ex) {
			throw new HttpMessageConversionException("Invalid JAXB setup: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 根据给定的类型和初始容量（如果集合类型支持）创建一个集合。
	 *
	 * @param collectionClass 要实例化的集合类型
	 * @return 创建的集合实例
	 */
	@SuppressWarnings("unchecked")
	protected T createCollection(Class<?> collectionClass) {
		if (!collectionClass.isInterface()) {
			// 如果集合类不是接口
			try {
				// 创建可访问的构造函数并实例化集合类
				return (T) ReflectionUtils.accessibleConstructor(collectionClass).newInstance();
			} catch (Throwable ex) {
				// 捕获异常并抛出带有详细信息的 非法参数异常
				throw new IllegalArgumentException(
						"Could not instantiate collection class: " + collectionClass.getName(), ex);
			}
		} else if (List.class == collectionClass) {
			// 如果集合类是 List 接口，转为并返回 ArrayList
			return (T) new ArrayList();
		} else if (SortedSet.class == collectionClass) {
			// 如果集合类是 SortedSet 接口，转为并返回 SortedSet
			return (T) new TreeSet();
		} else {
			// 默认情况下返回 LinkedHashSet
			return (T) new LinkedHashSet();
		}
	}

	private int moveToFirstChildOfRootElement(XMLStreamReader streamReader) throws XMLStreamException {
		// 根元素
		int event = streamReader.next();
		while (event != XMLStreamReader.START_ELEMENT) {
			// 如果事件不是开始元素，则获取下一个事件
			event = streamReader.next();
		}

		// 第一个子节点
		event = streamReader.next();
		while ((event != XMLStreamReader.START_ELEMENT) && (event != XMLStreamReader.END_DOCUMENT)) {
			// 如果事件不是开始元素，并且不是结束元素，则获取下一个事件
			event = streamReader.next();
		}
		return event;
	}

	private int moveToNextElement(XMLStreamReader streamReader) throws XMLStreamException {
		// 获取事件类型
		int event = streamReader.getEventType();
		// 循环直到找到下一个 开始元素 或者 结束元素 事件
		while (event != XMLStreamReader.START_ELEMENT && event != XMLStreamReader.END_DOCUMENT) {
			// 获取下一个事件
			event = streamReader.next();
		}
		// 返回找到的事件类型
		return event;
	}

	@Override
	public void write(T t, @Nullable Type type, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		throw new UnsupportedOperationException();
	}

	@Override
	protected void writeToResult(T t, HttpHeaders headers, Result result) throws Exception {
		throw new UnsupportedOperationException();
	}

	/**
	 * 创建一个 {@code XMLInputFactory}，用于创建 {@link javax.xml.stream.XMLStreamReader}
	 * 和 {@link javax.xml.stream.XMLEventReader} 对象。
	 * <p>可以在子类中重写此方法，进一步初始化工厂。生成的工厂会被缓存，因此此方法只会被调用一次。
	 *
	 * @see StaxUtils#createDefensiveInputFactory()
	 */
	protected XMLInputFactory createXmlInputFactory() {
		return StaxUtils.createDefensiveInputFactory();
	}

}
