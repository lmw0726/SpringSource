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

package org.springframework.http.codec;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 支持 {@link ServerSentEvent ServerSentEvents} 流以及普通 {@link Object Objects} 的读取器，
 * 这与仅包含数据的 {@link ServerSentEvent} 相同。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public class ServerSentEventHttpMessageReader implements HttpMessageReader<Object> {
	/**
	 * 字符串类型
	 */
	private static final ResolvableType STRING_TYPE = ResolvableType.forClass(String.class);

	/**
	 * 解码器
	 */
	@Nullable
	private final Decoder<?> decoder;

	/**
	 * 行解码器
	 */
	private final StringDecoder lineDecoder = StringDecoder.textPlainOnly();


	/**
	 * 无 {@code Decoder} 的构造函数。在这种模式下，只有 {@code String}
	 * 被支持作为事件的数据。
	 */
	public ServerSentEventHttpMessageReader() {
		this(null);
	}

	/**
	 * 具有 JSON {@code Decoder} 的构造函数，用于解码为对象。
	 * 对解码为 {@code String} 事件数据的支持是内置的。
	 */
	public ServerSentEventHttpMessageReader(@Nullable Decoder<?> decoder) {
		this.decoder = decoder;
	}


	/**
	 * 返回配置的 {@code Decoder}。
	 */
	@Nullable
	public Decoder<?> getDecoder() {
		return this.decoder;
	}

	/**
	 * 配置每个 SSE 事件缓冲的最大字节数限制，缓冲后事件被解析。
	 * <p>请注意，如果提供了 {@link #getDecoder() 数据解码器}，则还必须相应地自定义以提高限制，
	 * 以便能够解析事件的数据部分。
	 * <p>默认情况下设置为 256K。
	 *
	 * @param byteCount 缓冲的最大字节数，或 -1 表示无限制
	 * @since 5.1.13
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.lineDecoder.setMaxInMemorySize(byteCount);
	}

	/**
	 * 返回 {@link #setMaxInMemorySize 配置的} 字节数限制。
	 *
	 * @since 5.1.13
	 */
	public int getMaxInMemorySize() {
		return this.lineDecoder.getMaxInMemorySize();
	}


	@Override
	public List<MediaType> getReadableMediaTypes() {
		return Collections.singletonList(MediaType.TEXT_EVENT_STREAM);
	}

	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		return (MediaType.TEXT_EVENT_STREAM.includes(mediaType) || isServerSentEvent(elementType));
	}

	private boolean isServerSentEvent(ResolvableType elementType) {
		return ServerSentEvent.class.isAssignableFrom(elementType.toClass());
	}


	@Override
	public Flux<Object> read(
			ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {

		// 创建一个 跟踪限制 实例
		LimitTracker limitTracker = new LimitTracker();

		// 判断是否为服务器发送事件，如果是则需要包装
		boolean shouldWrap = isServerSentEvent(elementType);
		// 如果需要包装，则获取 元素类型 的泛型类型，否则直接使用 元素类型
		ResolvableType valueType = (shouldWrap ? elementType.getGeneric() : elementType);

		// 使用 行解码器 解码消息体，获取解码后的流
		return this.lineDecoder.decode(message.getBody(), STRING_TYPE, null, hints)
				// 在每次解析一行后调用 跟踪限制 的 afterLineParsed 方法
				.doOnNext(limitTracker::afterLineParsed)
				// 将流中的数据按空行进行分组
				.bufferUntil(String::isEmpty)
				.concatMap(lines -> {
					// 将每组行数据转换为事件对象
					Object event = buildEvent(lines, valueType, shouldWrap, hints);
					// 将非空的事件对象包装为 Mono 并返回
					return (event != null ? Mono.just(event) : Mono.empty());
				});
	}

	@Nullable
	private Object buildEvent(List<String> lines, ResolvableType valueType, boolean shouldWrap,
							  Map<String, Object> hints) {

		// 根据条件决定是否创建 ServerSentEvent.Builder 对象
		ServerSentEvent.Builder<Object> sseBuilder = (shouldWrap ? ServerSentEvent.builder() : null);
		// 初始化用于存储数据和注释的 字符串构建器
		StringBuilder data = null;
		StringBuilder comment = null;

		// 遍历输入的每一行
		for (String line : lines) {
			// 如果行以 "data:" 开头
			if (line.startsWith("data:")) {
				int length = line.length();
				// 如果行长度大于 5
				if (length > 5) {
					// 确定数据内容的起始位置
					int index = (line.charAt(5) != ' ' ? 5 : 6);
					// 如果起始位置小于行的总长度
					if (length > index) {
						// 如果 数据 为 null，则创建一个新的 StringBuilder
						data = (data != null ? data : new StringBuilder());
						// 将数据行的内容添加到 数据 字符串构建器 中
						data.append(line, index, line.length());
						// 添加换行符
						data.append('\n');
					}
				}
			} else if (shouldWrap) {
				// 如果需要包装 SSE，
				if (line.startsWith("id:")) {
					// 如果该行以 "id:" 开头，则设置 ID 字段
					sseBuilder.id(line.substring(3).trim());
				} else if (line.startsWith("event:")) {
					// 如果该行以 "event:" 开头，则设置事件字段
					sseBuilder.event(line.substring(6).trim());
				} else if (line.startsWith("retry:")) {
					// 如果该行以 "retry:" 开头，则设置重试字段
					sseBuilder.retry(Duration.ofMillis(Long.parseLong(line.substring(6).trim())));
				} else if (line.startsWith(":")) {
					// 如果行以 ":" 开头，则为注释，将其添加到 注释 字符串构建器 中
					comment = (comment != null ? comment : new StringBuilder());
					// 添加换行符
					comment.append(line.substring(1).trim()).append('\n');
				}
			}
		}

		// 解码数据，如果存在的话
		Object decodedData = (data != null ? decodeData(data, valueType, hints) : null);

		// 如果需要包装 SSE
		if (shouldWrap) {
			// 如果存在注释，则设置注释内容
			if (comment != null) {
				sseBuilder.comment(comment.substring(0, comment.length() - 1));
			}
			// 如果解码后的数据不为 空，则设置数据内容
			if (decodedData != null) {
				sseBuilder.data(decodedData);
			}
			// 构建并返回最终的 ServerSentEvent 对象
			return sseBuilder.build();
		} else {
			// 否则，直接返回解码后的数据
			return decodedData;
		}
	}

	@Nullable
	private Object decodeData(StringBuilder data, ResolvableType dataType, Map<String, Object> hints) {
		// 如果 数据类型 解析为 字符串 类型
		if (String.class == dataType.resolve()) {
			// 返回去除末尾换行符后的字符串
			return data.substring(0, data.length() - 1);
		}

		// 如果没有配置 SSE 解码器并且数据不是 字符串 类型，则抛出 编解码器异常
		if (this.decoder == null) {
			throw new CodecException("No SSE decoder configured and the data is not String.");
		}

		// 将 数据 转换为 UTF-8 编码的字节数组
		byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
		// 创建一个包装字节数组的 数据缓冲区，不进行实际分配内存
		DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
		// 解码 数据缓冲区，并返回解码后的对象
		return this.decoder.decode(buffer, dataType, MediaType.TEXT_EVENT_STREAM, hints);
	}

	@Override
	public Mono<Object> readMono(
			ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {

		// 按照读取器的顺序，我们在 字符串 之前 + "*/*"
		// 如果调用此方法，则简单地委托给 字符串解码器

		// 如果 元素类型 解析为 字符串 类
		if (elementType.resolve() == String.class) {
			// 获取消息体的 数据缓冲区 流
			Flux<DataBuffer> body = message.getBody();
			// 解码消息体并转换为 Mono，然后强制转换为 Object 类型
			return this.lineDecoder.decodeToMono(body, elementType, null, null).cast(Object.class);
		}

		// 如果 元素类型 不是 字符串 类，则返回一个 不支持的操作异常
		return Mono.error(new UnsupportedOperationException(
				"ServerSentEventHttpMessageReader only supports reading stream of events as a Flux"));
	}


	private class LimitTracker {
		/**
		 * 计数值
		 */
		private int accumulated = 0;

		public void afterLineParsed(String line) {
			// 如果最大内存大小小于 0，则直接返回，不进行处理
			if (getMaxInMemorySize() < 0) {
				return;
			}

			// 如果当前行为空，则重置累积长度为 0
			if (line.isEmpty()) {
				this.accumulated = 0;
			}

			// 如果当前行的长度加上累积长度超过 Integer.MAX_VALUE，则抛出异常
			if (line.length() > Integer.MAX_VALUE - this.accumulated) {
				raiseLimitException();
			} else {
				// 否则，将当前行的长度累加到累积长度中
				this.accumulated += line.length();
				// 如果累积长度超过最大内存大小，则抛出异常
				if (this.accumulated > getMaxInMemorySize()) {
					raiseLimitException();
				}
			}
		}

		private void raiseLimitException() {
			// 不要在这里释放，很可能是通过doOnDiscard ..
			throw new DataBufferLimitException("Exceeded limit on max bytes to buffer : " + getMaxInMemorySize());
		}
	}

}
