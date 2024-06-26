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
import org.springframework.core.codec.AbstractEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 使用 <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>
 * 从 {@code Object} 流编码到 JSON 对象的字节流。
 *
 * <p>此编码器可用于绑定 {@code @Serializable} Kotlin 类，
 * 不支持 <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">开放多态序列化</a>。
 * 它支持带有各种字符集的 {@code application/json} 和 {@code application/*+json}，默认字符集为 {@code UTF-8}。
 *
 * <p>注意：默认情况下，Protobuf 消息的实例生成空字节数组，因此通过网络发送 {@code Mono.just(Msg.getDefaultInstance())}
 * 将被反序列化为空的 {@link Mono}。
 *
 * <p>要生成 {@code Message} Java 类，需要安装 {@code protoc} 二进制文件。
 *
 * <p>此解码器需要 Protobuf 3 或更高版本，并支持官方的 {@code "com.google.protobuf:protobuf-java"} 库的
 * {@code "application/x-protobuf"} 和 {@code "application/octet-stream"}。
 *
 * <p>此编码器需要 Protobuf 3 或更高版本，并支持官方的 {@code "com.google.protobuf:protobuf-java"} 库的
 * {@code "application/x-protobuf"} 和 {@code "application/octet-stream"}。
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
public class KotlinSerializationJsonEncoder extends AbstractEncoder<Object> {
	/**
	 * 类型 —— 解析序列化器缓存。
	 */
	private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

	/**
	 * Json实例
	 */
	private final Json json;

	/**
	 * 目前需要 CharSequence 编码，详情见 https://github.com/Kotlin/kotlinx.serialization/issues/204
	 */
	private final CharSequenceEncoder charSequenceEncoder = CharSequenceEncoder.allMimeTypes();


	public KotlinSerializationJsonEncoder() {
		this(Json.Default);
	}

	public KotlinSerializationJsonEncoder(Json json) {
		super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
		this.json = json;
	}


	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		try {
			// 序列化元素类型的类型
			serializer(elementType.getType());
			// 如果可以进行编码，并且 元素类型 不是 String 类型，也不是 ServerSentEvent 类型，则返回 true
			return (super.canEncode(elementType, mimeType) && !String.class.isAssignableFrom(elementType.toClass()) &&
					!ServerSentEvent.class.isAssignableFrom(elementType.toClass()));
		} catch (Exception ex) {
			// 如果出现异常，则返回 false
			return false;
		}
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory,
								   ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (inputStream instanceof Mono) {
			// 如果 输入流 是 Mono 的实例
			return Mono.from(inputStream)
					// 将 Mono 中的值进行编码
					.map(value -> encodeValue(value, bufferFactory, elementType, mimeType, hints))
					// 将 Mono 转换为 Flux
					.flux();
		} else {
			// 如果 输入流 不是 Mono 的实例
			// 获取带有泛型的 List 类型
			ResolvableType listType = ResolvableType.forClassWithGenerics(List.class, elementType);
			// 将 输入流 转换为 Flux
			return Flux.from(inputStream)
					// 将 Flux 中的元素收集到一个 List 中
					.collectList()
					// 对 List 进行编码
					.map(list -> encodeValue(list, bufferFactory, listType, mimeType, hints))
					// 将 Mono 转换为 Flux
					.flux();
		}
	}

	@Override
	public DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory,
								  ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		// 对 值和序列化类型 进行编码，得到 JSON 字符串
		String json = this.json.encodeToString(serializer(valueType.getType()), value);

		// 对 JSON 字符串进行编码，并返回结果
		return this.charSequenceEncoder.encodeValue(json, bufferFactory, valueType, mimeType, null);
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
		// 从 序列化器缓存 中获取 类型 对应的序列化器
		KSerializer<Object> serializer = serializerCache.get(type);
		if (serializer == null) {
			// 如果缓存中没有，则使用 SerializersKt 获取序列化器
			serializer = SerializersKt.serializer(type);
			// 检查序列化器的描述符是否具有多态性
			if (hasPolymorphism(serializer.getDescriptor(), new HashSet<>())) {
				// 如果具有多态性，则抛出不支持的操作异常
				throw new UnsupportedOperationException("Open polymorphic serialization is not supported yet");
			}
			// 将新的序列化器缓存到 序列化器缓存 中
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
