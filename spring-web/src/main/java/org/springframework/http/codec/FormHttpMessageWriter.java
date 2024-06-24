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

package org.springframework.http.codec;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 将 {@code MultiValueMap<String, String>} 作为 HTML 表单数据写入请求体的 {@link HttpMessageWriter}，
 * 即 {@code "application/x-www-form-urlencoded"}。
 *
 * <p>请注意，除非显式将媒体类型设置为 {@link MediaType#APPLICATION_FORM_URLENCODED}，
 * 否则 {@link #canWrite} 方法需要泛型类型信息来确认目标映射有 String 值。
 * 这是因为具有非 String 值的 MultiValueMap 可用于写入多部分请求。
 *
 * <p>要同时支持表单数据和多部分请求，请考虑使用配置了此编写器的
 * {@link org.springframework.http.codec.multipart.MultipartHttpMessageWriter} 作为写入纯表单数据的后备。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see org.springframework.http.codec.multipart.MultipartHttpMessageWriter
 * @since 5.0
 */
public class FormHttpMessageWriter extends LoggingCodecSupport
		implements HttpMessageWriter<MultiValueMap<String, String>> {

	/**
	 * 编写器使用的默认字符集。
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * 默认表单数据媒体类型
	 */
	private static final MediaType DEFAULT_FORM_DATA_MEDIA_TYPE =
			new MediaType(MediaType.APPLICATION_FORM_URLENCODED, DEFAULT_CHARSET);

	/**
	 * 媒体类型列表
	 */
	private static final List<MediaType> MEDIA_TYPES =
			Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED);

	/**
	 * 多值类型
	 */
	private static final ResolvableType MULTIVALUE_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	/**
	 * 默认字符集
	 */
	private Charset defaultCharset = DEFAULT_CHARSET;


	/**
	 * 设置在响应 Content-Type 头未明确指定时用于写入表单数据的默认字符集。
	 * <p>默认设置为 "UTF-8"。
	 */
	public void setDefaultCharset(Charset charset) {
		Assert.notNull(charset, "Charset must not be null");
		this.defaultCharset = charset;
	}

	/**
	 * 返回配置的默认字符集。
	 */
	public Charset getDefaultCharset() {
		return this.defaultCharset;
	}


	@Override
	public List<MediaType> getWritableMediaTypes() {
		return MEDIA_TYPES;
	}


	@Override
	public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
		// 如果 元素类型 不是 MultiValueMap 的子类，则返回 false
		if (!MultiValueMap.class.isAssignableFrom(elementType.toClass())) {
			return false;
		}

		// 如果 媒体类型 兼容 application/x-www-form-urlencoded
		if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)) {
			// 乐观地认为，任何带或不带泛型的 MultiValueMap 都可以处理
			return true;
		}

		// 如果 媒体类型 为空
		if (mediaType == null) {
			// 仅处理基于 String 的 MultiValueMap
			return MULTIVALUE_TYPE.isAssignableFrom(elementType);
		}

		// 其他情况返回 false
		return false;
	}

	@Override
	public Mono<Void> write(Publisher<? extends MultiValueMap<String, String>> inputStream,
							ResolvableType elementType, @Nullable MediaType mediaType, ReactiveHttpOutputMessage message,
							Map<String, Object> hints) {

		// 获取处理后的媒体类型
		mediaType = getMediaType(mediaType);
		// 设置消息的内容类型为处理后的媒体类型
		message.getHeaders().setContentType(mediaType);

		// 获取字符集，如果媒体类型中未指定字符集，则使用默认字符集
		Charset charset = mediaType.getCharset() != null ? mediaType.getCharset() : getDefaultCharset();

		// 从输入流中获取数据，并将其转换为 Mono 对象
		return Mono.from(inputStream).flatMap(form -> {
			// 记录表单数据和提示信息
			logFormData(form, hints);
			// 将表单数据序列化为字符串
			String value = serializeForm(form, charset);
			// 将字符串编码为字节缓冲区
			ByteBuffer byteBuffer = charset.encode(value);
			// 将字节缓冲区包装为 DataBuffer 对象（仅包装，不进行内存分配）
			DataBuffer buffer = message.bufferFactory().wrap(byteBuffer);
			// 设置消息的内容长度为字节缓冲区的剩余字节数
			message.getHeaders().setContentLength(byteBuffer.remaining());
			// 使用包含 DataBuffer 的 Mono 对象写入消息
			return message.writeWith(Mono.just(buffer));
		});
	}

	protected MediaType getMediaType(@Nullable MediaType mediaType) {
		// 如果媒体类型为空
		if (mediaType == null) {
			// 返回默认的表单数据媒体类型
			return DEFAULT_FORM_DATA_MEDIA_TYPE;
		} else if (mediaType.getCharset() == null) {
			// 如果媒体类型中未指定字符集
			// 使用默认字符集创建新的媒体类型并返回
			return new MediaType(mediaType, getDefaultCharset());
		} else {
			// 返回原始媒体类型
			return mediaType;
		}
	}

	private void logFormData(MultiValueMap<String, String> form, Map<String, Object> hints) {
		// 使用 LogFormatUtils.traceDebug 方法记录调试信息
		// 获取日志前缀和表单数据
		LogFormatUtils.traceDebug(logger, traceOn -> Hints.getLogPrefix(hints) + "Writing " +
				// 判断是否启用详细日志记录
				(isEnableLoggingRequestDetails() ?
						// 如果启用详细日志记录，则格式化表单数据
						LogFormatUtils.formatValue(form, !traceOn) :
						// 否则，只记录表单字段的键，内容则被掩盖
						"form fields " + form.keySet() + " (content masked)"));
	}

	protected String serializeForm(MultiValueMap<String, String> formData, Charset charset) {
		// 创建一个构建表单数据 字符串构建器
		StringBuilder builder = new StringBuilder();

		// 遍历表单数据的每个键值对
		formData.forEach((name, values) ->
				// 遍历每个键对应的所有值
				values.forEach(value -> {
					try {
						// 如果 字符串构建器 已有内容，添加 '&' 分隔符
						if (builder.length() != 0) {
							builder.append('&');
						}
						// 将键名进行 URL 编码并添加到 字符串构建器 中
						builder.append(URLEncoder.encode(name, charset.name()));
						// 如果值不为空
						if (value != null) {
							// 添加 '=' 分隔符
							builder.append('=');
							// 将值进行 URL 编码并添加到 字符串构建器 中
							builder.append(URLEncoder.encode(value, charset.name()));
						}
					} catch (UnsupportedEncodingException ex) {
						// 处理不支持的编码异常，抛出非法状态异常
						throw new IllegalStateException(ex);
					}
				}));

		// 返回构建的表单数据字符串
		return builder.toString();
	}

}
