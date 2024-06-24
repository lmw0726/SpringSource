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
import org.springframework.core.codec.AbstractEncoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
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

import java.util.List;
import java.util.Map;

/**
 * 使用 {@link Encoder} 的 {@code HttpMessageWriter}。
 *
 * <p>也是一个 {@code HttpMessageWriter}，它从服务器端的额外信息（如请求或控制器方法注释）中预解析编码提示。
 *
 * @param <T> 输入流中的对象类型
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 5.0
 */
public class EncoderHttpMessageWriter<T> implements HttpMessageWriter<T> {

	/**
	 * 日志记录器
	 */
	private static final Log logger = HttpLogging.forLogName(EncoderHttpMessageWriter.class);

	/**
	 * 编码器
	 */
	private final Encoder<T> encoder;

	/**
	 * 媒体类型列表
	 */
	private final List<MediaType> mediaTypes;

	/**
	 * 默认媒体类型
	 */
	@Nullable
	private final MediaType defaultMediaType;


	/**
	 * 创建一个使用给定 {@link Encoder} 的实例。
	 */
	public EncoderHttpMessageWriter(Encoder<T> encoder) {
		Assert.notNull(encoder, "Encoder is required");
		// 初始化日志记录器
		initLogger(encoder);
		this.encoder = encoder;
		this.mediaTypes = MediaType.asMediaTypes(encoder.getEncodableMimeTypes());
		// 根据媒体类型列表初始化默认媒体类型
		this.defaultMediaType = initDefaultMediaType(this.mediaTypes);
	}

	private static void initLogger(Encoder<?> encoder) {
		// 检查 编码器 是否是 AbstractEncoder 的实例
		// 并且其类名是否以 "org.springframework.core.codec" 开头
		if (encoder instanceof AbstractEncoder &&
				encoder.getClass().getName().startsWith("org.springframework.core.codec")) {
			// 获取 编码器 的日志记录器
			Log logger = HttpLogging.forLog(((AbstractEncoder<?>) encoder).getLogger());
			// 为 编码器 设置新的日志记录器
			((AbstractEncoder<?>) encoder).setLogger(logger);
		}
	}

	@Nullable
	private static MediaType initDefaultMediaType(List<MediaType> mediaTypes) {
		return mediaTypes.stream().filter(MediaType::isConcrete).findFirst().orElse(null);
	}


	/**
	 * 返回此 writer 的 {@code Encoder}。
	 */
	public Encoder<T> getEncoder() {
		return this.encoder;
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return this.mediaTypes;
	}

	@Override
	public List<MediaType> getWritableMediaTypes(ResolvableType elementType) {
		return MediaType.asMediaTypes(getEncoder().getEncodableMimeTypes(elementType));
	}

	@Override
	public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
		return this.encoder.canEncode(elementType, mediaType);
	}

	@Override
	public Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType elementType,
							@Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		// 更新消息的内容类型
		MediaType contentType = updateContentType(message, mediaType);

		// 使用编码器将 输入流 编码为 DataBuffer 流
		Flux<DataBuffer> body = this.encoder.encode(
				inputStream, message.bufferFactory(), elementType, contentType, hints);

		// 如果 输入流 是 Mono 类型
		if (inputStream instanceof Mono) {
			return body
					// 只取流中的单个元素，如果流为空则返回空 Mono
					.singleOrEmpty()
					.switchIfEmpty(Mono.defer(() -> {
						// 如果流为空，则设置消息头的内容长度为 0，并完成消息
						message.getHeaders().setContentLength(0);
						return message.setComplete().then(Mono.empty());
					}))
					.flatMap(buffer -> {
						// 记录和处理 DataBuffer 的提示信息
						Hints.touchDataBuffer(buffer, hints, logger);
						// 设置消息头的内容长度
						message.getHeaders().setContentLength(buffer.readableByteCount());
						// 将 DataBuffer 写入消息，并在丢弃时释放
						return message.writeWith(Mono.just(buffer)
								.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release));
					})
					.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
		}

		// 如果内容类型是流媒体类型
		if (isStreamingMediaType(contentType)) {
			return message.writeAndFlushWith(body.map(buffer -> {
				// 记录和处理 DataBuffer 的提示信息
				Hints.touchDataBuffer(buffer, hints, logger);
				// 将 DataBuffer 写入消息，并在丢弃时释放
				return Mono.just(buffer).doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
			}));
		}

		// 如果处于调试模式，则记录和处理 DataBuffer 的提示信息
		if (logger.isDebugEnabled()) {
			body = body.doOnNext(buffer -> Hints.touchDataBuffer(buffer, hints, logger));
		}

		// 将 DataBuffer 流写入消息
		return message.writeWith(body);
	}

	@Nullable
	private MediaType updateContentType(ReactiveHttpOutputMessage message, @Nullable MediaType mediaType) {
		// 从消息头中获取 内容类型
		MediaType result = message.getHeaders().getContentType();
		if (result != null) {
			// 如果获取到了内容类型，直接返回该内容类型。
			return result;
		}

		// 如果消息头中没有 内容类型，使用默认的 MediaType
		MediaType fallback = this.defaultMediaType;

		// 确定使用回退值还是原始的 媒体类型
		result = (useFallback(mediaType, fallback) ? fallback : mediaType);

		// 如果获取到了媒体类型
		if (result != null) {
			// 添加默认字符集
			result = addDefaultCharset(result, fallback);
			// 设置消息头的 内容类型
			message.getHeaders().setContentType(result);
		}

		// 返回最终确定的 内容类型
		return result;
	}

	private static boolean useFallback(@Nullable MediaType main, @Nullable MediaType fallback) {
		return (main == null || !main.isConcrete() ||
				main.equals(MediaType.APPLICATION_OCTET_STREAM) && fallback != null);
	}

	private static MediaType addDefaultCharset(MediaType main, @Nullable MediaType defaultType) {
		// 如果 主要的媒体类型 的字符集为空，并且 默认媒体类型 不为空，且具有字符集
		if (main.getCharset() == null && defaultType != null && defaultType.getCharset() != null) {
			// 返回一个新的 媒体类型，字符集来自 默认媒体类型
			return new MediaType(main, defaultType.getCharset());
		}
		// 否则，返回原来的 主要的媒体类型
		return main;
	}

	private boolean isStreamingMediaType(@Nullable MediaType mediaType) {
		// 如果 媒体类型 为 null， 或者 编码器 不是 HttpMessageEncoder 的实例，则返回 false
		if (mediaType == null || !(this.encoder instanceof HttpMessageEncoder)) {
			return false;
		}
		// 遍历 HttpMessageEncoder 的流媒体类型列表
		for (MediaType streamingMediaType : ((HttpMessageEncoder<?>) this.encoder).getStreamingMediaTypes()) {
			// 如果 媒体类型 与流媒体类型兼容，并且参数匹配，则返回 true
			if (mediaType.isCompatibleWith(streamingMediaType) && matchParameters(mediaType, streamingMediaType)) {
				return true;
			}
		}
		// 如果没有找到兼容的流媒体类型，则返回 false
		return false;
	}

	private boolean matchParameters(MediaType streamingMediaType, MediaType mediaType) {
		// 遍历流媒体类型的参数名集合
		for (String name : streamingMediaType.getParameters().keySet()) {
			// 获取流媒体类型和 媒体类型 中对应参数的值
			String s1 = streamingMediaType.getParameter(name);
			String s2 = mediaType.getParameter(name);
			// 如果两个值均不为空，且不相等（忽略大小写），则返回 false
			if (StringUtils.hasText(s1) && StringUtils.hasText(s2) && !s1.equalsIgnoreCase(s2)) {
				return false;
			}
		}
		// 如果所有参数都匹配或流媒体类型没有参数，则返回 true
		return true;
	}


	// 仅服务器端...

	@Override
	public Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType actualType,
							ResolvableType elementType, @Nullable MediaType mediaType, ServerHttpRequest request,
							ServerHttpResponse response, Map<String, Object> hints) {

		// 合并所有的提示信息
		Map<String, Object> allHints = Hints.merge(hints,
				getWriteHints(actualType, elementType, mediaType, request, response));

		// 进行写入操作
		return write(inputStream, elementType, mediaType, response, allHints);
	}

	/**
	 * 获取编码的附加提示，例如基于服务器请求或控制器方法参数的注解。
	 * 默认情况下，如果编码器是 {@link HttpMessageEncoder} 的实例，则委托给编码器。
	 */
	protected Map<String, Object> getWriteHints(ResolvableType streamType, ResolvableType elementType,
												@Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

		// 检查 编码器 是否是 HttpMessageEncoder 的实例
		if (this.encoder instanceof HttpMessageEncoder) {
			// 将 编码器 转换为 HttpMessageEncoder 类型
			HttpMessageEncoder<?> encoder = (HttpMessageEncoder<?>) this.encoder;
			// 获取编码提示信息
			return encoder.getEncodeHints(streamType, elementType, mediaType, request, response);
		}
		// 如果不是 HttpMessageEncoder 实例，则返回空的提示信息
		return Hints.none();
	}

}
