/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.codec.multipart;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支持多部分HTTP消息编写的支持类。
 *
 * <p>该类提供了生成多部分边界、准备多部分媒体类型以及检查是否能够写入特定类型的多部分数据的方法。
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public class MultipartWriterSupport extends LoggingCodecSupport {

	/**
	 * 默认使用的字符集。
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * 支持的媒体类型列表
	 */
	private final List<MediaType> supportedMediaTypes;

	/**
	 * 字符集
	 */
	private Charset charset = DEFAULT_CHARSET;


	/**
	 * 使用支持的媒体类型列表构造一个 {@code MultipartWriterSupport} 实例。
	 *
	 * @param supportedMediaTypes 支持的媒体类型列表
	 */
	protected MultipartWriterSupport(List<MediaType> supportedMediaTypes) {
		this.supportedMediaTypes = supportedMediaTypes;
	}


	/**
	 * 返回配置的部分头部的字符集。
	 *
	 * @return 部分头部的字符集
	 */
	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * 设置用于部分头部（如 "Content-Disposition" 及其 filename 参数）的字符集。
	 * <p>默认情况下设置为 "UTF-8"。如果更改此默认值，则 "Content-Type" 头部将具有指定使用的字符集的 "charset" 参数。
	 *
	 * @param charset 要设置的字符集，不能为空
	 */
	public void setCharset(Charset charset) {
		Assert.notNull(charset, "Charset must not be null");
		this.charset = charset;
	}

	/**
	 * 返回支持的可写入媒体类型列表。
	 *
	 * @return 可写入的媒体类型列表
	 */
	public List<MediaType> getWritableMediaTypes() {
		return this.supportedMediaTypes;
	}

	/**
	 * 检查是否可以写入给定元素类型及媒体类型的多部分数据。
	 *
	 * @param elementType 要写入的元素类型
	 * @param mediaType   要写入的媒体类型，可以为 {@code null}
	 * @return 如果可以写入，则返回 {@code true}；否则返回 {@code false}
	 */
	public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
		// 检查 元素类型 是否为MultiValueMap或其子类
		if (MultiValueMap.class.isAssignableFrom(elementType.toClass())) {
			// 如果 媒体类型 为null，则返回true
			if (mediaType == null) {
				return true;
			}
			// 检查 支持的媒体类型列表 中是否有与 媒体类型兼容的媒体类型
			for (MediaType supportedMediaType : this.supportedMediaTypes) {
				if (supportedMediaType.isCompatibleWith(mediaType)) {
					return true;
				}
			}
		}

		// 如果以上条件都不满足，则返回false
		return false;
	}

	/**
	 * 生成一个多部分边界。
	 * <p>默认情况下委托给 {@link MimeTypeUtils#generateMultipartBoundary()}。
	 *
	 * @return 生成的多部分边界
	 */
	protected byte[] generateMultipartBoundary() {
		return MimeTypeUtils.generateMultipartBoundary();
	}

	/**
	 * 准备要使用的 {@code MediaType}，向给定的 {@code mediaType} 添加 "boundary" 和 "charset" 参数，或者如果未指定则使用默认的 "multipart/form-data" 类型。
	 *
	 * @param mediaType 媒体类型，可以为 {@code null}
	 * @param boundary  边界字节数组
	 * @return 准备好的 {@code MediaType}
	 */
	protected MediaType getMultipartMediaType(@Nullable MediaType mediaType, byte[] boundary) {
		// 创建一个空的参数映射，用于存储媒体类型的参数信息
		Map<String, String> params = new HashMap<>();

		// 如果 媒体类型 不为null，则将其参数添加到 参数映射 中
		if (mediaType != null) {
			params.putAll(mediaType.getParameters());
		}

		// 将"边界"参数添加到 参数映射 中，使用US_ASCII字符集解码 边界 字节数组
		params.put("boundary", new String(boundary, StandardCharsets.US_ASCII));

		// 获取字符集
		Charset charset = getCharset();

		// 如果字符集不是UTF-8和US_ASCII，则将字符集名称作为"charset"参数添加到 参数映射 中
		if (!charset.equals(StandardCharsets.UTF_8) &&
				!charset.equals(StandardCharsets.US_ASCII)) {
			params.put("charset", charset.name());
		}

		// 如果 媒体类型 为null，则将其设置为默认的 multipart/form-data
		mediaType = (mediaType != null ? mediaType : MediaType.MULTIPART_FORM_DATA);

		// 使用 参数映射 创建新的MediaType对象
		mediaType = new MediaType(mediaType, params);

		// 返回构建好的MediaType对象
		return mediaType;
	}

	protected Mono<DataBuffer> generateBoundaryLine(byte[] boundary, DataBufferFactory bufferFactory) {
		return Mono.fromCallable(() -> {
			// 分配一个大小为边界长度 + 4的DataBuffer
			DataBuffer buffer = bufferFactory.allocateBuffer(boundary.length + 4);
			// 写入"--boundary\r\n"到buffer中
			buffer.write((byte) '-');
			buffer.write((byte) '-');
			buffer.write(boundary);
			buffer.write((byte) '\r');
			buffer.write((byte) '\n');
			// 返回构建好的 DataBuffer
			return buffer;
		});
	}

	protected Mono<DataBuffer> generateNewLine(DataBufferFactory bufferFactory) {
		return Mono.fromCallable(() -> {
			// 使用 缓冲工厂 分配一个大小为2的DataBuffer
			DataBuffer buffer = bufferFactory.allocateBuffer(2);
			// 向缓存中写入\r\n
			buffer.write((byte) '\r');
			buffer.write((byte) '\n');
			// 返回构建好的缓存
			return buffer;
		});
	}

	protected Mono<DataBuffer> generateLastLine(byte[] boundary, DataBufferFactory bufferFactory) {
		return Mono.fromCallable(() -> {
			// 分配一个大小为边界长度 + 6的DataBuffer
			DataBuffer buffer = bufferFactory.allocateBuffer(boundary.length + 6);
			// 写入"--boundary--\r\n"到缓存区中
			buffer.write((byte) '-');
			buffer.write((byte) '-');
			buffer.write(boundary);
			buffer.write((byte) '-');
			buffer.write((byte) '-');
			buffer.write((byte) '\r');
			buffer.write((byte) '\n');
			// 返回构建好的缓存区
			return buffer;
		});
	}

	protected Mono<DataBuffer> generatePartHeaders(HttpHeaders headers, DataBufferFactory bufferFactory) {
		return Mono.fromCallable(() -> {
			// 分配一个默认大小的DataBuffer
			DataBuffer buffer = bufferFactory.allocateBuffer();
			// 遍历头部信息中的每个条目
			for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
				// 使用指定的字符集，将头部名称转换为字节数组
				byte[] headerName = entry.getKey().getBytes(getCharset());
				// 遍历当前条目的每个头部值字符串
				for (String headerValueString : entry.getValue()) {
					// 使用指定的字符集，将 头部值 转换为字节数组
					byte[] headerValue = headerValueString.getBytes(getCharset());
					// 向 缓冲区 中写入头部名称
					buffer.write(headerName);
					// 向 缓冲区 中写入':'
					buffer.write((byte) ':');
					// 向 缓冲区 中写入空格
					buffer.write((byte) ' ');
					// 向 缓冲区 中写入 头部值
					buffer.write(headerValue);
					// 向 缓冲区 中写入'\r'
					buffer.write((byte) '\r');
					// 向 缓冲区 中写入'\n'
					buffer.write((byte) '\n');
				}
			}
			// 向 缓冲区 中写入最后的'\r\n'
			buffer.write((byte) '\r');
			buffer.write((byte) '\n');
			// 返回构建好的 缓冲区 
			return buffer;
		});
	}

}
