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

package org.springframework.http.codec.protobuf;

import com.google.protobuf.Message;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageEncoder;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 使用<a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>
 * 写入{@link com.google.protobuf.Message}的{@code Encoder}。
 *
 * <p>Flux使用<a href="https://developers.google.com/protocol-buffers/docs/techniques?hl=en#streaming">定界的Protobuf消息</a>序列化，
 * 每个消息的大小在消息本身之前指定。单个值使用常规的Protobuf消息格式序列化（消息之前没有附加大小）。
 *
 * <p>要生成{@code Message} Java类，您需要安装{@code protoc}二进制文件。
 *
 * <p>此编码器需要Protobuf 3或更高版本，并支持{@code "application/x-protobuf"}和{@code "application/octet-stream"}，
 * 使用官方的{@code "com.google.protobuf:protobuf-java"}库。
 *
 * @author Sebastien Deleuze
 * @see ProtobufDecoder
 * @since 5.1
 */
public class ProtobufEncoder extends ProtobufCodecSupport implements HttpMessageEncoder<Message> {
	/**
	 * 流媒体类型列表
	 */
	private static final List<MediaType> streamingMediaTypes = MIME_TYPES
			.stream()
			.map(mimeType -> new MediaType(mimeType.getType(), mimeType.getSubtype(),
					Collections.singletonMap(DELIMITED_KEY, DELIMITED_VALUE)))
			.collect(Collectors.toList());


	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return Message.class.isAssignableFrom(elementType.toClass()) && supportsMimeType(mimeType);
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<? extends Message> inputStream, DataBufferFactory bufferFactory,
								   ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.from(inputStream).map(message ->
				encodeValue(message, bufferFactory, !(inputStream instanceof Mono)));
	}

	@Override
	public DataBuffer encodeValue(Message message, DataBufferFactory bufferFactory,
								  ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return encodeValue(message, bufferFactory, false);
	}

	private DataBuffer encodeValue(Message message, DataBufferFactory bufferFactory, boolean delimited) {

		// 从 缓冲工厂 分配一个新的 数据缓冲区
		DataBuffer buffer = bufferFactory.allocateBuffer();
		boolean release = true;
		try {
			// 如果是定界的
			if (delimited) {
				// 使用 writeDelimitedTo 方法将消息写入缓冲区的输出流
				message.writeDelimitedTo(buffer.asOutputStream());
			} else {
				// 否则，使用 writeTo 方法将消息写入缓冲区的输出流
				message.writeTo(buffer.asOutputStream());
			}
			// 成功写入后，不需要释放缓冲区，设置 release 为 false
			release = false;
			// 返回缓冲区
			return buffer;
		} catch (IOException ex) {
			// 处理写入过程中发生的 I/O 异常，并抛出 非法状态异常
			throw new IllegalStateException("Unexpected I/O error while writing to data buffer", ex);
		} finally {
			// 如果需要释放缓冲区（写入失败时），则释放缓冲区资源
			if (release) {
				DataBufferUtils.release(buffer);
			}
		}
	}

	@Override
	public List<MediaType> getStreamingMediaTypes() {
		return streamingMediaTypes;
	}

	@Override
	public List<MimeType> getEncodableMimeTypes() {
		return getMimeTypes();
	}

}
