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

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 实现 {@link HttpMessageReader} 接口，用于读取 HTML 表单数据，即媒体类型为 {@code "application/x-www-form-urlencoded"} 的请求体。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class FormHttpMessageReader extends LoggingCodecSupport
		implements HttpMessageReader<MultiValueMap<String, String>> {

	/**
	 * 读取器使用的默认字符集。
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * 多值字符串类型
	 */
	private static final ResolvableType MULTIVALUE_STRINGS_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	/**
	 * 默认字符集
	 */
	private Charset defaultCharset = DEFAULT_CHARSET;

	/**
	 * 最大字节计数
	 */
	private int maxInMemorySize = 256 * 1024;


	/**
	 * 设置在请求的 Content-Type 头未明确指定时，用于读取表单数据的默认字符集。
	 * <p>默认情况下，此值设置为 "UTF-8"。
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

	/**
	 * 设置输入表单数据的最大字节数。由于表单数据在解析之前会被缓冲，这有助于限制缓冲的数量。
	 * 一旦超过限制，将引发 {@link DataBufferLimitException}。
	 * <p>默认情况下，此值设置为 256K。
	 *
	 * @param byteCount 要缓冲的最大字节数，或者为 -1 表示无限制
	 * @since 5.1.11
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
	}

	/**
	 * 返回 {@link #setMaxInMemorySize 设置的} 字节计数限制。
	 *
	 * @since 5.1.11
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}


	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		// 检查 元素类型 是否有无法解析的泛型，并且是 MultiValueMap 类型的子类或接口
		boolean multiValueUnresolved =
				elementType.hasUnresolvableGenerics() &&
						MultiValueMap.class.isAssignableFrom(elementType.toClass());

		// 如果满足以下一个条件，则返回true：
		// 元素类型 是 多值字符串类型 的子类或接口，或者无法解析的多值类型
		// 同时 元素类型 为 null， 或者兼容 application/x-www-form-urlencoded
		return ((MULTIVALUE_STRINGS_TYPE.isAssignableFrom(elementType) || multiValueUnresolved) &&
				(mediaType == null || MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)));
	}

	@Override
	public Flux<MultiValueMap<String, String>> read(ResolvableType elementType,
													ReactiveHttpInputMessage message, Map<String, Object> hints) {

		return Flux.from(readMono(elementType, message, hints));
	}

	@Override
	public Mono<MultiValueMap<String, String>> readMono(ResolvableType elementType,
														ReactiveHttpInputMessage message, Map<String, Object> hints) {

		// 获取消息的内容类型
		MediaType contentType = message.getHeaders().getContentType();
		// 获取内容类型的字符集
		Charset charset = getMediaTypeCharset(contentType);

		// 将消息体合并为一个 数据缓冲区，并设置在内存中的最大大小
		return DataBufferUtils.join(message.getBody(), this.maxInMemorySize)
				.map(buffer -> {
					// 将 DataBuffer 解码为 CharBuffer
					CharBuffer charBuffer = charset.decode(buffer.asByteBuffer());
					// 将 CharBuffer 转换为字符串
					String body = charBuffer.toString();
					// 释放 DataBuffer
					DataBufferUtils.release(buffer);
					// 解析表单数据并返回 MultiValueMap
					MultiValueMap<String, String> formData = parseFormData(charset, body);
					// 记录解析的表单数据
					logFormData(formData, hints);
					return formData;
				});
	}

	private void logFormData(MultiValueMap<String, String> formData, Map<String, Object> hints) {
		// 记录调试日志
		LogFormatUtils.traceDebug(logger, traceOn -> Hints.getLogPrefix(hints) + "Read " +
				// 构造日志消息，包括日志前缀和消息内容
				// 如果启用请求详情日志记录，则格式化输出 格式化数据
				(isEnableLoggingRequestDetails() ?
						LogFormatUtils.formatValue(formData, !traceOn) :
						// 否则输出表单字段的键集合和内容屏蔽提示
						"form fields " + formData.keySet() + " (content masked)"));
	}

	private Charset getMediaTypeCharset(@Nullable MediaType mediaType) {
		// 如果 媒体类型 不为 null 并且其字符集不为 null，则返回该字符集
		if (mediaType != null && mediaType.getCharset() != null) {
			return mediaType.getCharset();
		} else {
			// 否则返回默认的字符集
			return getDefaultCharset();
		}
	}

	private MultiValueMap<String, String> parseFormData(Charset charset, String body) {
		// 将 body 按 '&' 符号分割为字符串数组
		String[] pairs = StringUtils.tokenizeToStringArray(body, "&");
		// 创建一个 LinkedMultiValueMap 来存储解析后的键值对
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>(pairs.length);
		try {
			// 遍历每个键值对字符串
			for (String pair : pairs) {
				// 查找键值对中 '=' 的位置
				int idx = pair.indexOf('=');
				if (idx == -1) {
					// 如果没有找到 '='，则解码键，并添加空值到 结果集 中
					result.add(URLDecoder.decode(pair, charset.name()), null);
				} else {
					// 如果找到 '='，则解码键和值，并添加到 结果集 中
					String name = URLDecoder.decode(pair.substring(0, idx), charset.name());
					String value = URLDecoder.decode(pair.substring(idx + 1), charset.name());
					result.add(name, value);
				}
			}
		} catch (UnsupportedEncodingException ex) {
			// 如果解码过程中出现异常，则抛出 非法状态异常
			throw new IllegalStateException(ex);
		}
		// 返回解析后的 结果集
		return result;
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED);
	}

}
