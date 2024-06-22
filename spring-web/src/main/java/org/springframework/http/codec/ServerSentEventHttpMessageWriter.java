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

package org.springframework.http.codec;

import org.apache.commons.logging.Log;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpLogging;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 用于 {@code "text/event-stream"} 响应的 {@code HttpMessageWriter}。
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ServerSentEventHttpMessageWriter implements HttpMessageWriter<Object> {

	/**
	 * 默认媒体类型
	 */
	private static final MediaType DEFAULT_MEDIA_TYPE = new MediaType("text", "event-stream", StandardCharsets.UTF_8);

	/**
	 * 可写媒体类型，默认为 text/event-stream
	 */
	private static final List<MediaType> WRITABLE_MEDIA_TYPES = Collections.singletonList(MediaType.TEXT_EVENT_STREAM);

	/**
	 * 日志记录器
	 */
	private static final Log logger = HttpLogging.forLogName(ServerSentEventHttpMessageWriter.class);

	/**
	 * 编码器
	 */
	@Nullable
	private final Encoder<?> encoder;


	/**
	 * 没有 {@code Encoder} 的构造函数。在这种模式下，只支持将 {@code String}
	 * 编码为事件数据。
	 */
	public ServerSentEventHttpMessageWriter() {
		this(null);
	}

	/**
	 * 具有 JSON {@code Encoder} 的构造函数，用于编码对象。
	 * 对 {@code String} 事件数据的支持是内置的。
	 *
	 * @param encoder 要使用的编码器（可以为 {@code null}）
	 */
	public ServerSentEventHttpMessageWriter(@Nullable Encoder<?> encoder) {
		this.encoder = encoder;
	}


	/**
	 * 返回配置的 {@code Encoder}（如果有）。
	 */
	@Nullable
	public Encoder<?> getEncoder() {
		return this.encoder;
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return WRITABLE_MEDIA_TYPES;
	}


	@Override
	public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
		// 如果符合以下条件之一，返回true：
		// 1. 媒体类型 为 null
		// 2. 媒体类型 包含在 text/event-stream 中
		// 3. 元素类型 的类是 ServerSentEvent 的子类
		return (mediaType == null || MediaType.TEXT_EVENT_STREAM.includes(mediaType) ||
				ServerSentEvent.class.isAssignableFrom(elementType.toClass()));
	}

	@Override
	public Mono<Void> write(Publisher<?> input, ResolvableType elementType, @Nullable MediaType mediaType,
							ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		// 如果 媒体类型 不为 空， 且其字符集不为 空，则使用该 媒体类型，否则使用默认的媒体类型
		mediaType = (mediaType != null && mediaType.getCharset() != null ? mediaType : DEFAULT_MEDIA_TYPE);
		// 获取数据缓冲区工厂
		DataBufferFactory bufferFactory = message.bufferFactory();

		// 设置消息头的内容类型为 媒体类型
		message.getHeaders().setContentType(mediaType);
		// 编码输入数据并将其写入消息，同时刷新
		return message.writeAndFlushWith(encode(input, elementType, mediaType, bufferFactory, hints));
	}

	private Flux<Publisher<DataBuffer>> encode(Publisher<?> input, ResolvableType elementType,
											   MediaType mediaType, DataBufferFactory factory, Map<String, Object> hints) {

		// 确定数据类型，如果 元素类型 是 ServerSentEvent 的子类，则获取其泛型类型，否则直接使用 元素类型
		ResolvableType dataType = (ServerSentEvent.class.isAssignableFrom(elementType.toClass()) ?
				elementType.getGeneric() : elementType);

		// 将输入数据转换为 Flux 流，并对每个元素进行映射处理
		return Flux.from(input).map(element -> {

			// 如果元素是 ServerSentEvent 类型，则直接使用，否则将其包装为 ServerSentEvent
			ServerSentEvent<?> sse = (element instanceof ServerSentEvent ?
					(ServerSentEvent<?>) element : ServerSentEvent.builder().data(element).build());

			// 创建 字符串构建器 用于构建事件字符串
			StringBuilder sb = new StringBuilder();
			String id = sse.id();
			String event = sse.event();
			Duration retry = sse.retry();
			String comment = sse.comment();
			Object data = sse.data();

			// 如果 编号 不为空，写入 "id" 字段
			if (id != null) {
				writeField("id", id, sb);
			}
			// 如果 事件 不为空，写入 "event" 字段
			if (event != null) {
				writeField("event", event, sb);
			}
			// 如果 重连时间 不为空，写入 "retry" 字段
			if (retry != null) {
				writeField("retry", retry.toMillis(), sb);
			}
			// 如果 注释 不为空，写入注释行
			if (comment != null) {
				sb.append(':').append(StringUtils.replace(comment, "\n", "\n:")).append('\n');
			}
			// 如果 数据 不为空，写入 "data:" 行
			if (data != null) {
				sb.append("data:");
			}

			// 根据 数据 的类型，选择合适的编码方式
			Flux<DataBuffer> result;
			if (data == null) {
				// 如果 数据 为空，只编码构建好的事件字符串并加上换行符
				result = Flux.just(encodeText(sb + "\n", mediaType, factory));
			} else if (data instanceof String) {
				// 如果 数据 是字符串，将换行符替换为 "\ndata:"，并编码
				data = StringUtils.replace((String) data, "\n", "\ndata:");
				result = Flux.just(encodeText(sb + (String) data + "\n\n", mediaType, factory));
			} else {
				// 如果 数据 是其他类型，使用特定的方法编码事件
				result = encodeEvent(sb, data, dataType, mediaType, factory, hints);
			}

			// 在结果 Flux 中处理丢弃的 PooledDataBuffer，释放资源
			return result.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
		});
	}

	@SuppressWarnings("unchecked")
	private <T> Flux<DataBuffer> encodeEvent(StringBuilder eventContent, T data, ResolvableType dataType,
											 MediaType mediaType, DataBufferFactory factory, Map<String, Object> hints) {

		// 如果没有配置 编码器， 并且数据不是字符串，抛出 编解码器异常
		if (this.encoder == null) {
			throw new CodecException("No SSE encoder configured and the data is not String.");
		}

		// 延迟执行以确保资源的懒惰初始化和管理
		return Flux.defer(() -> {
			// 编码事件内容的起始部分
			DataBuffer startBuffer = encodeText(eventContent, mediaType, factory);
			// 编码事件内容的结束部分
			DataBuffer endBuffer = encodeText("\n\n", mediaType, factory);
			// 使用配置的 编码器 编码数据部分
			DataBuffer dataBuffer = ((Encoder<T>) this.encoder).encodeValue(data, factory, dataType, mediaType, hints);
			// 处理 数据缓冲区，应用提示并记录日志
			Hints.touchDataBuffer(dataBuffer, hints, logger);
			// 返回包含起始部分、数据部分和结束部分的 Flux
			return Flux.just(startBuffer, dataBuffer, endBuffer);
		});
	}

	private void writeField(String fieldName, Object fieldValue, StringBuilder sb) {
		sb.append(fieldName).append(':').append(fieldValue).append('\n');
	}

	private DataBuffer encodeText(CharSequence text, MediaType mediaType, DataBufferFactory bufferFactory) {
		Assert.notNull(mediaType.getCharset(), "Expected MediaType with charset");
		// 将文本根据 媒体类型的字符集 转为 字节数组
		byte[] bytes = text.toString().getBytes(mediaType.getCharset());
		// 包装，不分配
		return bufferFactory.wrap(bytes);
	}

	@Override
	public Mono<Void> write(Publisher<?> input, ResolvableType actualType, ResolvableType elementType,
							@Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response,
							Map<String, Object> hints) {

		// 合并原始提示和从请求、响应等获取的编码提示
		Map<String, Object> allHints = Hints.merge(hints,
				getEncodeHints(actualType, elementType, mediaType, request, response));

		// 使用合并后的提示写入输入数据，并返回结果
		return write(input, elementType, mediaType, response, allHints);
	}

	private Map<String, Object> getEncodeHints(ResolvableType actualType, ResolvableType elementType,
											   @Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

		// 如果 编码器 是 HttpMessageEncoder 的实例
		if (this.encoder instanceof HttpMessageEncoder) {
			// 将 编码器 转换为 HttpMessageEncoder 类型
			HttpMessageEncoder<?> encoder = (HttpMessageEncoder<?>) this.encoder;
			// 获取编码提示并返回
			return encoder.getEncodeHints(actualType, elementType, mediaType, request, response);
		}
		// 否则，返回一个空的提示集合
		return Hints.none();
	}

}
