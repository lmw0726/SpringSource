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

package org.springframework.http.codec.multipart;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 默认的 {@code HttpMessageReader}，用于将 {@code "multipart/form-data"} 请求解析为 {@link Part} 流。
 *
 * <p>在默认的非流式模式下，此消息读取器将小于 {@link #setMaxInMemorySize(int) maxInMemorySize} 的部分的 {@linkplain Part#content() 内容} 存储在内存中，
 * 而大于此大小的部分存储到 {@link #setFileStorageDirectory(Path) fileStorageDirectory} 中的临时文件中。
 * <p>在 {@linkplain #setStreaming(boolean) streaming} 模式下，部分内容直接从解析的输入缓冲流中流式传输，不存储在内存或文件中。
 *
 * <p>此读取器可以提供给 {@link MultipartHttpMessageReader}，以便将所有部分聚合到一个 Map 中。
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
public class DefaultPartHttpMessageReader extends LoggingCodecSupport implements HttpMessageReader<Part> {

	/**
	 * 最大内存大小，默认为 256 KB。
	 */
	private int maxInMemorySize = 256 * 1024;

	/**
	 * 每个部分头部的最大内存大小，默认为 10 KB。
	 */
	private int maxHeadersSize = 10 * 1024;

	/**
	 * 每个文件部分的最大磁盘使用量，默认为 -1，表示没有限制。
	 */
	private long maxDiskUsagePerPart = -1;

	/**
	 * 给定多部分请求中允许的最大部分数，默认为 -1，表示没有限制。
	 */
	private int maxParts = -1;

	/**
	 * 是否启用流式处理。
	 */
	private boolean streaming;

	/**
	 * 用于阻塞操作的调度程序
	 */
	private Scheduler blockingOperationScheduler = Schedulers.boundedElastic();

	/**
	 * 文件存储策略
	 */
	private FileStorage fileStorage = FileStorage.tempDirectory(this::getBlockingOperationScheduler);

	/**
	 * 头部解码使用的字符集，默认为 UTF-8。
	 */
	private Charset headersCharset = StandardCharsets.UTF_8;


	/**
	 * 配置每个部分的头部段允许的最大内存量。
	 * 当超过限制时：
	 * <ul>
	 * <li>文件部分将被写入临时文件。
	 * <li>非文件部分将通过 {@link DataBufferLimitException} 被拒绝。
	 * </ul>
	 *
	 * @param byteCount 头部的最大内存量
	 */
	public void setMaxHeadersSize(int byteCount) {
		this.maxHeadersSize = byteCount;
	}

	/**
	 * 获取配置的最大内存大小 {@link #setMaxInMemorySize}。
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}

	/**
	 * 配置每个部分允许的最大内存量。
	 * 当超过限制时：
	 * <ul>
	 * <li>文件部分将被写入临时文件。
	 * <li>非文件部分将通过 {@link DataBufferLimitException} 被拒绝。
	 * </ul>
	 * <p>默认设置为 256K。
	 * <p>注意，在启用 {@linkplain #setStreaming(boolean) streaming} 时，此属性将被忽略。
	 *
	 * @param maxInMemorySize 内存中限制的字节数；如果设置为 -1，则整个内容将存储在内存中
	 */
	public void setMaxInMemorySize(int maxInMemorySize) {
		this.maxInMemorySize = maxInMemorySize;
	}

	/**
	 * 配置文件部分允许的最大磁盘空间。
	 * <p>默认设置为 -1，表示没有最大限制。
	 * <p>注意，在启用 {@linkplain #setStreaming(boolean) streaming} 或 {@link #setMaxInMemorySize(int) maxInMemorySize} 设置为 -1 时，此属性将被忽略。
	 *
	 * @param maxDiskUsagePerPart 每个部分的磁盘限制（字节），或者 -1 表示无限制
	 */
	public void setMaxDiskUsagePerPart(long maxDiskUsagePerPart) {
		this.maxDiskUsagePerPart = maxDiskUsagePerPart;
	}

	/**
	 * 指定给定多部分请求中允许的最大部分数。
	 * <p>默认设置为 -1，表示没有最大限制。
	 */
	public void setMaxParts(int maxParts) {
		this.maxParts = maxParts;
	}

	/**
	 * 设置用于存储大于 {@link #setMaxInMemorySize(int) maxInMemorySize} 的部分的目录。
	 * 默认情况下，创建一个名为 {@code spring-webflux-multipart} 的新临时目录。
	 * <p>注意，在启用 {@linkplain #setStreaming(boolean) streaming} 或 {@link #setMaxInMemorySize(int) maxInMemorySize} 设置为 -1 时，此属性将被忽略。
	 *
	 * @throws IOException 如果发生 I/O 错误，或者父目录不存在
	 */
	public void setFileStorageDirectory(Path fileStorageDirectory) throws IOException {
		Assert.notNull(fileStorageDirectory, "FileStorageDirectory must not be null");
		this.fileStorage = FileStorage.fromPath(fileStorageDirectory);
	}

	/**
	 * 设置用于创建文件和目录以及写入文件的 Reactor {@link Scheduler}。
	 * 默认情况下，使用 {@link Schedulers#boundedElastic()}，但此属性允许将其更改为外部管理的调度程序。
	 * <p>注意，在启用 {@linkplain #setStreaming(boolean) streaming} 或 {@link #setMaxInMemorySize(int) maxInMemorySize} 设置为 -1 时，此属性将被忽略。
	 *
	 * @see Schedulers#newBoundedElastic
	 */
	public void setBlockingOperationScheduler(Scheduler blockingOperationScheduler) {
		Assert.notNull(blockingOperationScheduler, "FileCreationScheduler must not be null");
		this.blockingOperationScheduler = blockingOperationScheduler;
	}

	private Scheduler getBlockingOperationScheduler() {
		return this.blockingOperationScheduler;
	}

	/**
	 * 当设置为 {@code true} 时，{@linkplain Part#content() 部分内容} 将直接从解析的输入缓冲流中流式传输，不存储在内存或文件中。
	 * 当设置为 {@code false} 时，部分内容由内存和/或文件存储支持。默认为 {@code false}。
	 * <p><strong>注意</strong>，启用流式传输时，此消息读取器生成的 {@code Flux<Part>} 必须按原始顺序消耗，即 HTTP 消息的顺序。
	 * 此外，在移动到下一个部分之前，{@linkplain Part#content() body contents} 必须完全消耗或取消。
	 * <p>还请注意，启用此属性将有效地忽略 {@link #setMaxInMemorySize(int) maxInMemorySize}、
	 * {@link #setMaxDiskUsagePerPart(long) maxDiskUsagePerPart}、
	 * {@link #setFileStorageDirectory(Path) fileStorageDirectory} 和
	 * {@link #setBlockingOperationScheduler(Scheduler) fileCreationScheduler}。
	 */
	public void setStreaming(boolean streaming) {
		this.streaming = streaming;
	}

	/**
	 * 设置用于解码头部的字符集。
	 * 根据 RFC 7578，默认设置为 UTF-8。
	 *
	 * @param headersCharset 用于解码头部的字符集
	 * @see <a href="https://tools.ietf.org/html/rfc7578#section-5.1">RFC-7578 第 5.1 节</a>
	 * @since 5.3.6
	 */
	public void setHeadersCharset(Charset headersCharset) {
		Assert.notNull(headersCharset, "HeadersCharset must not be null");
		this.headersCharset = headersCharset;
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return Collections.singletonList(MediaType.MULTIPART_FORM_DATA);
	}

	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		// 检查给定的 元素类型 是否为 Part 类型
		return Part.class.equals(elementType.toClass()) &&
				// 并且检查 媒体类型 是否为 null，或者兼容 multipart/form-data
				(mediaType == null || MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType));
	}

	@Override
	public Mono<Part> readMono(ResolvableType elementType, ReactiveHttpInputMessage message,
							   Map<String, Object> hints) {
		return Mono.error(new UnsupportedOperationException("Cannot read multipart request body into single Part"));
	}

	@Override
	public Flux<Part> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
		return Flux.defer(() -> {
			// 获取消息的 边界 字节数组
			byte[] boundary = boundary(message);

			// 如果未找到 边界，则抛出 解码异常
			if (boundary == null) {
				return Flux.error(new DecodingException("No multipart boundary found in Content-Type: \"" +
						message.getHeaders().getContentType() + "\""));
			}

			// 解析消息体并生成 token 流
			Flux<MultipartParser.Token> tokens = MultipartParser.parse(message.getBody(), boundary,
					this.maxHeadersSize, this.headersCharset);

			// 创建部分生成器，生成部分流
			return PartGenerator.createParts(tokens, this.maxParts, this.maxInMemorySize, this.maxDiskUsagePerPart,
					this.streaming, this.fileStorage.directory(), this.blockingOperationScheduler);
		});
	}

	@Nullable
	private byte[] boundary(HttpMessage message) {
		// 获取消息的内容类型
		MediaType contentType = message.getHeaders().getContentType();

		// 如果内容类型不为 null
		if (contentType != null) {
			// 获取 边界 参数
			String boundary = contentType.getParameter("boundary");

			// 如果 边界 参数不为 null
			if (boundary != null) {
				int len = boundary.length();

				// 如果 边界 的长度大于 2，并且第一个字符是双引号，最后一个字符也是双引号
				if (len > 2 && boundary.charAt(0) == '"' && boundary.charAt(len - 1) == '"') {
					// 去除 边界 字符串两端的双引号
					boundary = boundary.substring(1, len - 1);
				}

				// 使用指定的字符集将 边界 字符串转换为字节数组并返回
				return boundary.getBytes(this.headersCharset);
			}
		}

		// 如果未找到符合条件的 边界 参数，则返回 null
		return null;
	}

}
