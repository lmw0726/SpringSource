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

package org.springframework.http.codec;

import org.apache.commons.logging.Log;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.codec.ResourceEncoder;
import org.springframework.core.codec.ResourceRegionEncoder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 可以写入 {@link Resource} 的 {@code HttpMessageWriter}。
 *
 * <p>也是 {@code HttpMessageWriter} 的一个实现，支持根据请求中指定的 HTTP 范围写入一个或多个 {@link ResourceRegion}。
 *
 * <p>要读取到 Resource，请使用 {@link ResourceDecoder} 并用 {@link DecoderHttpMessageReader} 包装。
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see ResourceEncoder
 * @see ResourceRegionEncoder
 * @see HttpRange
 * @since 5.0
 */
public class ResourceHttpMessageWriter implements HttpMessageWriter<Resource> {
	/**
	 * 区域类型
	 */
	private static final ResolvableType REGION_TYPE = ResolvableType.forClass(ResourceRegion.class);

	/**
	 * 日志记录器
	 */
	private static final Log logger = HttpLogging.forLogName(ResourceHttpMessageWriter.class);

	/**
	 * 资源编码器
	 */
	private final ResourceEncoder encoder;

	/**
	 * 资源区域编码器
	 */
	private final ResourceRegionEncoder regionEncoder;

	/**
	 * 媒体类型列表
	 */
	private final List<MediaType> mediaTypes;


	public ResourceHttpMessageWriter() {
		this(ResourceEncoder.DEFAULT_BUFFER_SIZE);
	}

	public ResourceHttpMessageWriter(int bufferSize) {
		this.encoder = new ResourceEncoder(bufferSize);
		this.regionEncoder = new ResourceRegionEncoder(bufferSize);
		this.mediaTypes = MediaType.asMediaTypes(this.encoder.getEncodableMimeTypes());
	}


	@Override
	public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
		return this.encoder.canEncode(elementType, mediaType);
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return this.mediaTypes;
	}


	// 客户端或服务器: 单一资源...

	@Override
	public Mono<Void> write(Publisher<? extends Resource> inputStream, ResolvableType elementType,
							@Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		return Mono.from(inputStream).flatMap(resource ->
				writeResource(resource, elementType, mediaType, message, hints));
	}

	private Mono<Void> writeResource(Resource resource, ResolvableType type, @Nullable MediaType mediaType,
									 ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		// 获取消息头
		HttpHeaders headers = message.getHeaders();
		// 获取资源的媒体类型
		MediaType resourceMediaType = getResourceMediaType(mediaType, resource, hints);
		// 设置消息头的内容类型
		headers.setContentType(resourceMediaType);

		// 如果消息头未设置内容长度
		if (headers.getContentLength() < 0) {
			// 获取资源的长度
			long length = lengthOf(resource);
			// 如果资源长度已知
			if (length != -1) {
				// 设置消息头的内容长度
				headers.setContentLength(length);
			}
		}

		// 尝试使用零拷贝传输资源，如果不支持则采用编码方式处理
		return zeroCopy(resource, null, message, hints)
				.orElseGet(() -> {
					// 如果不支持零拷贝，使用编码器将资源编码为数据流
					Mono<Resource> input = Mono.just(resource);
					// 获取 数据缓冲区工厂
					DataBufferFactory factory = message.bufferFactory();
					// 编码输入的资源
					Flux<DataBuffer> body = this.encoder.encode(input, factory, type, resourceMediaType, hints);
					// 如果日志级别为调试，则在每个数据缓冲区上标记触摸日志
					if (logger.isDebugEnabled()) {
						body = body.doOnNext(buffer -> Hints.touchDataBuffer(buffer, hints, logger));
					}
					// 将编码后的数据流写入消息体
					return message.writeWith(body);
				});
	}

	private static MediaType getResourceMediaType(
			@Nullable MediaType mediaType, Resource resource, Map<String, Object> hints) {

		// 如果媒体类型不为空、是具体类型
		// 且媒体类型不是 application/octet-stream，则直接返回该媒体类型
		if (mediaType != null && mediaType.isConcrete() && !mediaType.equals(MediaType.APPLICATION_OCTET_STREAM)) {
			return mediaType;
		}

		// 根据资源获取媒体类型，如果获取不到则默认为 application/octet-stream
		mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);

		// 如果日志级别为调试且未禁止日志记录，则记录与资源关联的媒体类型信息
		if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
			logger.debug(Hints.getLogPrefix(hints) + "Resource associated with '" + mediaType + "'");
		}

		return mediaType;
	}

	private static long lengthOf(Resource resource) {
		// 不消耗 输入流...
		if (InputStreamResource.class != resource.getClass()) {
			// 如果资源不是 InputStreamResource 类型，则尝试获取其内容长度
			try {
				return resource.contentLength();
			} catch (IOException ignored) {
			}
		}
		return -1;
	}

	private static Optional<Mono<Void>> zeroCopy(Resource resource, @Nullable ResourceRegion region,
												 ReactiveHttpOutputMessage message, Map<String, Object> hints) {
		// 如果消息是 ZeroCopyHttpOutputMessage 类型，并且资源是一个文件
		if (message instanceof ZeroCopyHttpOutputMessage && resource.isFile()) {
			try {
				// 获取文件对象
				File file = resource.getFile();
				// 获取区域起始位置（如果有），否则默认从文件开头
				long pos = region != null ? region.getPosition() : 0;
				// 获取区域长度（如果有），否则使用整个文件长度
				long count = region != null ? region.getCount() : file.length();
				// 如果日志级别为调试模式
				if (logger.isDebugEnabled()) {
					// 构建日志信息，显示区域范围或整个文件
					String formatted = region != null ? "region " + pos + "-" + (count) + " of " : "";
					logger.debug(Hints.getLogPrefix(hints) + "Zero-copy " + formatted + "[" + resource + "]");
				}
				// 返回一个包含写入操作结果的Optional对象
				return Optional.of(((ZeroCopyHttpOutputMessage) message).writeWith(file, pos, count));
			} catch (IOException ex) {
				// 不应该发生的异常情况
			}
		}
		// 如果不符合零拷贝条件，则返回空的Optional对象
		return Optional.empty();
	}


	// 仅服务器端: 单个资源或子区域...

	@Override
	public Mono<Void> write(Publisher<? extends Resource> inputStream, @Nullable ResolvableType actualType,
							ResolvableType elementType, @Nullable MediaType mediaType, ServerHttpRequest request,
							ServerHttpResponse response, Map<String, Object> hints) {
		// 获取响应头
		HttpHeaders headers = response.getHeaders();
		// 设置 Accept-Ranges 头
		headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

		List<HttpRange> ranges;
		try {
			// 尝试获取请求头中的Range信息
			ranges = request.getHeaders().getRange();
		} catch (IllegalArgumentException ex) {
			// 如果Range请求格式不正确，则返回416状态码
			response.setStatusCode(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
			return response.setComplete();
		}

		return Mono.from(inputStream).flatMap(resource -> {
			if (ranges.isEmpty()) {
				// 如果Range列表为空，则直接写入完整资源
				return writeResource(resource, elementType, mediaType, response, hints);
			}
			// 设置响应状态码为206（Partial Content）
			response.setStatusCode(HttpStatus.PARTIAL_CONTENT);
			// 将HttpRange列表转换为ResourceRegion列表
			List<ResourceRegion> regions = HttpRange.toResourceRegions(ranges, resource);
			// 获取资源的媒体类型
			MediaType resourceMediaType = getResourceMediaType(mediaType, resource, hints);
			if (regions.size() == 1) {
				// 如果只有一个区域
				ResourceRegion region = regions.get(0);
				// 设置消息头的内容类型
				headers.setContentType(resourceMediaType);
				// 获取资源的长度
				long contentLength = lengthOf(resource);
				if (contentLength != -1) {
					// 如果内容长度已知
					// 计算并设置Content-Range和Content-Length
					long start = region.getPosition();
					long end = start + region.getCount() - 1;
					end = Math.min(end, contentLength - 1);
					headers.add("Content-Range", "bytes " + start + '-' + end + '/' + contentLength);
					headers.setContentLength(end - start + 1);
				}
				// 写入单个区域的内容
				return writeSingleRegion(region, response, hints);
			} else {
				// 如果有多个区域，生成multipart/byteranges类型的响应
				String boundary = MimeTypeUtils.generateMultipartBoundaryString();
				// 解析为媒体类型
				MediaType multipartType = MediaType.parseMediaType("multipart/byteranges;boundary=" + boundary);
				// 将多部分类型设置到头部中
				headers.setContentType(multipartType);
				// 合并所有提示信息，包括分隔符字符串提示
				Map<String, Object> allHints = Hints.merge(hints, ResourceRegionEncoder.BOUNDARY_STRING_HINT, boundary);
				// 编码并写入多个区域的内容
				return encodeAndWriteRegions(Flux.fromIterable(regions), resourceMediaType, response, allHints);
			}
		});
	}

	private Mono<Void> writeSingleRegion(ResourceRegion region, ReactiveHttpOutputMessage message,
										 Map<String, Object> hints) {

		return zeroCopy(region.getResource(), region, message, hints)
				.orElseGet(() -> {
					// 如果无法进行零拷贝，则使用传统方式处理
					Publisher<? extends ResourceRegion> input = Mono.just(region);
					// 获取消息的媒体类型
					MediaType mediaType = message.getHeaders().getContentType();
					// 编码并写入区域内容
					return encodeAndWriteRegions(input, mediaType, message, hints);
				});
	}

	private Mono<Void> encodeAndWriteRegions(Publisher<? extends ResourceRegion> publisher,
											 @Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		// 使用区域编码器对数据流进行编码，生成Flux<DataBuffer>
		Flux<DataBuffer> body = this.regionEncoder.encode(
				publisher, message.bufferFactory(), REGION_TYPE, mediaType, hints);

		// 将编码后的数据流写入到消息中
		return message.writeWith(body);
	}

}
