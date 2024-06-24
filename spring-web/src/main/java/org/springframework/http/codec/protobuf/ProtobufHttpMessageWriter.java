/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.codec.protobuf;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Encoder;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageEncoder;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * {@code HttpMessageWriter} 实现，用于写入 protobuf {@link Message}，
 * 并添加 {@code X-Protobuf-Schema}、{@code X-Protobuf-Message} 头信息，
 * 如果序列化的是 flux，则在内容类型中添加 {@code delimited=true} 参数。
 *
 * <p>对于 {@code HttpMessageReader}，可以直接使用 {@code new DecoderHttpMessageReader(new ProtobufDecoder())}。
 *
 * @author Sebastien Deleuze
 * @see ProtobufEncoder
 * @since 5.1
 */
public class ProtobufHttpMessageWriter extends EncoderHttpMessageWriter<Message> {
	/**
	 * X-Protobuf-Schema 头部
	 */
	private static final String X_PROTOBUF_SCHEMA_HEADER = "X-Protobuf-Schema";

	/**
	 * X-Protobuf-Message 头部
	 */
	private static final String X_PROTOBUF_MESSAGE_HEADER = "X-Protobuf-Message";

	/**
	 * 类 —— 方法缓存
	 */
	private static final ConcurrentMap<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();


	/**
	 * 使用默认的 {@link ProtobufEncoder} 创建一个新的 {@code ProtobufHttpMessageWriter} 实例。
	 */
	public ProtobufHttpMessageWriter() {
		super(new ProtobufEncoder());
	}

	/**
	 * 使用指定的编码器创建一个新的 {@code ProtobufHttpMessageWriter} 实例。
	 *
	 * @param encoder 要使用的 Protobuf 消息编码器
	 */
	public ProtobufHttpMessageWriter(Encoder<Message> encoder) {
		super(encoder);
	}


	@SuppressWarnings("unchecked")
	@Override
	public Mono<Void> write(Publisher<? extends Message> inputStream, ResolvableType elementType,
							@Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		try {
			// 获取消息构建器
			Message.Builder builder = getMessageBuilder(elementType.toClass());
			// 获取消息描述符
			Descriptors.Descriptor descriptor = builder.getDescriptorForType();
			// 将 protobuf 消息的文件名和完整名称添加到消息头部
			message.getHeaders().add(X_PROTOBUF_SCHEMA_HEADER, descriptor.getFile().getName());
			message.getHeaders().add(X_PROTOBUF_MESSAGE_HEADER, descriptor.getFullName());

			// 如果输入流是 Flux 类型
			if (inputStream instanceof Flux) {
				// 如果 媒体类型 为 null，则设置内容类型为第一个流媒体类型
				if (mediaType == null) {
					message.getHeaders().setContentType(((HttpMessageEncoder<?>) getEncoder()).getStreamingMediaTypes().get(0));
				} else if (!ProtobufEncoder.DELIMITED_VALUE.equals(mediaType.getParameters().get(ProtobufEncoder.DELIMITED_KEY))) {
					// 如果 媒体类型 存在并且媒体类型参数 delimited 不是true，则将参数 delimited 设置为true
					Map<String, String> parameters = new HashMap<>(mediaType.getParameters());
					parameters.put(ProtobufEncoder.DELIMITED_KEY, ProtobufEncoder.DELIMITED_VALUE);
					message.getHeaders().setContentType(new MediaType(mediaType.getType(), mediaType.getSubtype(), parameters));
				}
			}

			// 调用父类的 write 方法进行写入操作
			return super.write(inputStream, elementType, mediaType, message, hints);
		} catch (Exception ex) {
			// 捕获异常并返回错误的 Mono
			return Mono.error(new DecodingException("Could not read Protobuf message: " + ex.getMessage(), ex));
		}
	}

	/**
	 * 为给定的类创建一个新的 {@code Message.Builder} 实例。
	 * <p>该方法使用 ConcurrentHashMap 进行方法查找的缓存。
	 */
	private static Message.Builder getMessageBuilder(Class<?> clazz) throws Exception {
		// 从方法缓存中获取与 类型 对应的方法
		Method method = methodCache.get(clazz);
		// 如果方法为空，则尝试获取名为 "newBuilder" 的方法，并将其存入方法缓存
		if (method == null) {
			method = clazz.getMethod("newBuilder");
			methodCache.put(clazz, method);
		}
		// 使用反射调用获取到的方法，并返回调用结果强制转换为 Message.Builder 类型
		return (Message.Builder) method.invoke(clazz);
	}

}
