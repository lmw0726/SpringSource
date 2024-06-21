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

package org.springframework.http.converter.json;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializationException;
import kotlinx.serialization.SerializersKt;
import kotlinx.serialization.descriptors.PolymorphicKind;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.json.Json;
import org.springframework.core.GenericTypeResolver;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter} 的实现类，
 * 可以使用 <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a> 读取和写入 JSON。
 *
 * <p>此转换器可用于绑定 {@code @Serializable} Kotlin 类，
 * 不支持 <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">开放多态序列化</a>。
 * 它支持 {@code application/json} 和 {@code application/*+json}，并支持各种字符集，默认字符集为 {@code UTF-8}。
 *
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 5.3
 */
public class KotlinSerializationJsonHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

	/**
	 * 默认字符集
	 */
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * 序列化器缓存
	 */
	private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

	/**
	 * JSON实例
	 */
	private final Json json;


	/**
	 * 使用默认配置构造一个新的 {@code KotlinSerializationJsonHttpMessageConverter}。
	 */
	public KotlinSerializationJsonHttpMessageConverter() {
		this(Json.Default);
	}

	/**
	 * 使用自定义配置构造一个新的 {@code KotlinSerializationJsonHttpMessageConverter}。
	 */
	public KotlinSerializationJsonHttpMessageConverter(Json json) {
		super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
		this.json = json;
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		try {
			// 调用序列化器方法处理类
			serializer(clazz);
			// 如果没有异常，返回true
			return true;
		} catch (Exception ex) {
			// 如果发生异常，返回false
			return false;
		}
	}

	@Override
	public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
		try {
			// 调用 序列化器 方法处理通过 泛型类型解析器 解析出的类型
			serializer(GenericTypeResolver.resolveType(type, contextClass));
			// 检查指定的媒体类型是否可写
			return canRead(mediaType);
		} catch (Exception ex) {
			// 如果发生异常，返回false
			return false;
		}
	}

	@Override
	public boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType) {
		// 尝试执行
		try {
			// 如果类型不为null，通过 泛型类型解析器 解析类型，否则使用类
			serializer(type != null ? GenericTypeResolver.resolveType(type, clazz) : clazz);
			// 检查指定的媒体类型是否可写
			return canWrite(mediaType);
		} catch (Exception ex) {
			// 如果发生异常，返回false
			return false;
		}
	}

	@Override
	public final Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return decode(serializer(GenericTypeResolver.resolveType(type, contextClass)), inputMessage);
	}

	@Override
	protected final Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return decode(serializer(clazz), inputMessage);
	}

	private Object decode(KSerializer<Object> serializer, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		// 获取输入消息的内容类型
		MediaType contentType = inputMessage.getHeaders().getContentType();

		// 将输入消息的内容流转换为字符串，使用适当的字符集
		String jsonText = StreamUtils.copyToString(inputMessage.getBody(), getCharsetToUse(contentType));

		try {
			// 尝试使用流式API（当可用时）
			// 从字符串解码
			return this.json.decodeFromString(serializer, jsonText);
		} catch (SerializationException ex) {
			// 如果解析过程中发生异常，抛出 Http消息不可读异常
			throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex, inputMessage);
		}
	}

	@Override
	protected final void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		encode(object, serializer(type != null ? type : object.getClass()), outputMessage);
	}

	private void encode(Object object, KSerializer<Object> serializer, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		try {
			// 使用JSON编码器将对象转换为JSON字符串
			String json = this.json.encodeToString(serializer, object);

			// 获取输出消息的内容类型
			MediaType contentType = outputMessage.getHeaders().getContentType();

			// 将JSON字符串以适当的字符集写入输出流
			outputMessage.getBody().write(json.getBytes(getCharsetToUse(contentType)));

			// 刷新输出流
			outputMessage.getBody().flush();
		} catch (IOException ex) {
			// 如果IO操作发生异常，直接抛出该异常
			throw ex;
		} catch (Exception ex) {
			// 如果其他异常发生，抛出 Http消息不可写异常
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}

	private Charset getCharsetToUse(@Nullable MediaType contentType) {
		// 如果 内容类型 不为null，并且包含字符集信息，则返回其字符集
		if (contentType != null && contentType.getCharset() != null) {
			return contentType.getCharset();
		}

		// 返回默认字符集
		return DEFAULT_CHARSET;
	}

	/**
	 * 尝试查找能够使用 kotlinx.serialization 对给定类型的实例进行序列化或反序列化的序列化器。
	 * 如果找不到适合的序列化器，则抛出异常。
	 * <p>已解析的序列化器将被缓存，并在后续调用时返回缓存的结果。
	 * TODO 当 https://github.com/Kotlin/kotlinx.serialization/pull/1164 被修复后，避免依赖于抛出异常
	 *
	 * @param type 要查找序列化器的类型
	 * @return 给定类型的已解析序列化器
	 * @throws RuntimeException 如果找不到支持给定类型的序列化器
	 */
	private KSerializer<Object> serializer(Type type) {
		// 从 序列化器 缓存中获取与type对应的序列化器
		KSerializer<Object> serializer = serializerCache.get(type);

		// 如果 序列化器 为null，则需要创建并缓存新的序列化器
		if (serializer == null) {
			// 使用SerializersKt.serializer方法创建序列化器
			serializer = SerializersKt.serializer(type);

			// 检查序列化器是否具有多态性，如果有则抛出不支持的操作异常
			if (hasPolymorphism(serializer.getDescriptor(), new HashSet<>())) {
				throw new UnsupportedOperationException("Open polymorphic serialization is not supported yet");
			}

			// 将新创建的序列化器放入 序列化器缓存 中缓存起来
			serializerCache.put(type, serializer);
		}

		// 返回获取到的序列化器
		return serializer;
	}

	private boolean hasPolymorphism(SerialDescriptor descriptor, Set<String> alreadyProcessed) {
		// 将当前描述符的序列化名称添加到已处理集合中
		alreadyProcessed.add(descriptor.getSerialName());

		// 如果描述符的种类是开放式多态
		if (descriptor.getKind().equals(PolymorphicKind.OPEN.INSTANCE)) {
			// 返回true
			return true;
		}

		// 遍历描述符的所有元素
		for (int i = 0; i < descriptor.getElementsCount(); i++) {
			// 获取当前元素的序列化描述符
			SerialDescriptor elementDescriptor = descriptor.getElementDescriptor(i);

			// 如果已处理集合中不包含当前元素的序列化名称，并且当前元素具有多态性
			if (!alreadyProcessed.contains(elementDescriptor.getSerialName()) && hasPolymorphism(elementDescriptor, alreadyProcessed)) {
				// 返回true
				return true;
			}
		}

		// 如果以上条件都不满足，则返回false
		return false;
	}

}
