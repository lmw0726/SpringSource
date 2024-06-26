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

package org.springframework.http.codec.json;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializersKt;
import kotlinx.serialization.descriptors.PolymorphicKind;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.json.Json;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 使用 <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>
 * 将字节流解码为 JSON 并转换为对象。
 *
 * <p>此解码器可用于绑定 {@code @Serializable} Kotlin 类，
 * 不支持 <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">开放多态序列化</a>。
 * 它支持带有各种字符集的 {@code application/json} 和 {@code application/*+json}，默认字符集为 {@code UTF-8}。
 *
 * <p>解码流尚不支持，详情见
 * <a href="https://github.com/Kotlin/kotlinx.serialization/issues/1073">kotlinx.serialization/issues/1073</a>
 * 相关问题。
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
public class KotlinSerializationJsonDecoder extends AbstractDecoder<Object> {

	/**
	 * 类型 —— 序列化器缓存
	 */
	private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

	/**
	 * Json实例
	 */
	private final Json json;

	/**
	 * 目前需要字符串解码，详情见 https://github.com/Kotlin/kotlinx.serialization/issues/204
	 */
	private final StringDecoder stringDecoder = StringDecoder.allMimeTypes(StringDecoder.DEFAULT_DELIMITERS, false);


	public KotlinSerializationJsonDecoder() {
		this(Json.Default);
	}

	public KotlinSerializationJsonDecoder(Json json) {
		super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
		this.json = json;
	}

	/**
	 * 配置在需要聚合输入流时可以缓冲的最大字节数。这可能是解码到单个 {@code DataBuffer}、
	 * {@link java.nio.ByteBuffer ByteBuffer}、{@code byte[]}、
	 * {@link org.springframework.core.io.Resource Resource}、{@code String} 等的结果。
	 * 这也可能在拆分输入流时发生，例如分隔文本，在这种情况下，限制适用于分隔符之间缓冲的数据。
	 * <p>默认情况下设置为 256K。
	 *
	 * @param byteCount 最大缓冲字节数，或 -1 表示无限制
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.stringDecoder.setMaxInMemorySize(byteCount);
	}

	/**
	 * 返回 {@link #setMaxInMemorySize 配置的}字节数限制。
	 */
	public int getMaxInMemorySize() {
		return this.stringDecoder.getMaxInMemorySize();
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		try {
			// 序列化元素类型的类型
			serializer(elementType.getType());
			// 是否可以解码，且 元素类型 不是 CharSequence 的子类
			return (super.canDecode(elementType, mimeType) && !CharSequence.class.isAssignableFrom(elementType.toClass()));
		} catch (Exception ex) {
			return false;
		}
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
							   @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.error(new UnsupportedOperationException());
	}

	@Override
	public Mono<Object> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
									 @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		// 将 输入流 解码为 Mono
		return this.stringDecoder
				.decodeToMono(inputStream, elementType, mimeType, hints)
				// 解码为指定类型的对象
				.map(jsonText -> this.json.decodeFromString(serializer(elementType.getType()), jsonText));
	}

	/**
	 * 尝试找到能够使用 kotlinx.serialization 编组或解组给定类型实例的序列化器。如果找不到序列化器，将抛出异常。
	 * <p>解析的序列化器将被缓存，并在后续调用时返回缓存结果。
	 * TODO 避免依赖抛出异常，当 https://github.com/Kotlin/kotlinx.serialization/pull/1164 修复时
	 *
	 * @param type 要查找序列化器的类型
	 * @return 给定类型的解析序列化器
	 * @throws RuntimeException 如果找不到支持给定类型的序列化器
	 */
	private KSerializer<Object> serializer(Type type) {
		// 从 序列化器缓存 中获取指定类型的序列化器
		KSerializer<Object> serializer = serializerCache.get(type);

		// 如果缓存中没有对应的序列化器
		if (serializer == null) {
			// 使用 SerializersKt 创建新的序列化器
			serializer = SerializersKt.serializer(type);

			// 检查序列化器的描述符是否包含多态性
			if (hasPolymorphism(serializer.getDescriptor(), new HashSet<>())) {
				// 如果包含多态性，抛出不支持的操作异常
				throw new UnsupportedOperationException("Open polymorphic serialization is not supported yet");
			}

			// 将新的序列化器存入缓存
			serializerCache.put(type, serializer);
		}

		// 返回序列化器
		return serializer;
	}

	private boolean hasPolymorphism(SerialDescriptor descriptor, Set<String> alreadyProcessed) {
		// 将当前描述符的名称添加到 已处理集合 中
		alreadyProcessed.add(descriptor.getSerialName());

		// 如果描述符的类型是开放的多态类型，则返回 true
		if (descriptor.getKind().equals(PolymorphicKind.OPEN.INSTANCE)) {
			return true;
		}

		// 遍历描述符的所有元素
		for (int i = 0; i < descriptor.getElementsCount(); i++) {
			// 获取当前元素的描述符
			SerialDescriptor elementDescriptor = descriptor.getElementDescriptor(i);
			// 如果当前元素的名称不在 已处理集合 中，并且递归调用 hasPolymorphism 方法返回 true，则返回 true
			if (!alreadyProcessed.contains(elementDescriptor.getSerialName()) && hasPolymorphism(elementDescriptor, alreadyProcessed)) {
				return true;
			}
		}

		// 如果所有检查都不符合条件，则返回 false
		return false;
	}

}
