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

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 用于使用 {@link Part} 进行写入的 {@link HttpMessageWriter}。
 * 这在服务器端非常有用，可以将从客户端接收的 {@code Flux<Part>} 写入某个远程服务。
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public class PartHttpMessageWriter extends MultipartWriterSupport implements HttpMessageWriter<Part> {


	public PartHttpMessageWriter() {
		super(MultipartHttpMessageReader.MIME_TYPES);
	}


	@Override
	public Mono<Void> write(Publisher<? extends Part> parts,
							ResolvableType elementType, @Nullable MediaType mediaType, ReactiveHttpOutputMessage outputMessage,
							Map<String, Object> hints) {

		// 生成一个多部分边界字节数组
		byte[] boundary = generateMultipartBoundary();

		// 根据 媒体类型 和 编辑 获取多部分的 媒体类型
		mediaType = getMultipartMediaType(mediaType, boundary);
		// 设置输出消息的内容类型为新生成的多部分 mediaType
		outputMessage.getHeaders().setContentType(mediaType);

		// 如果日志记录器开启调试模式，记录调试信息
		if (logger.isDebugEnabled()) {
			logger.debug(Hints.getLogPrefix(hints) + "Encoding Publisher<Part>");
		}

		// 从 部分发布者 中生成，建一个 Flux<DataBuffer>
		Flux<DataBuffer> body = Flux.from(parts)
				// 将每个 部分 编码为 DataBuffer，并拼接在一起
				.concatMap(part -> encodePart(boundary, part, outputMessage.bufferFactory()))
				// 添加最后一行边界
				.concatWith(generateLastLine(boundary, outputMessage.bufferFactory()))
				// 丢弃 PooledDataBuffer 时，释放相关资源
				.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);

		// 如果日志记录器开启调试模式，记录调试信息
		if (logger.isDebugEnabled()) {
			body = body.doOnNext(buffer -> Hints.touchDataBuffer(buffer, hints, logger));
		}

		// 将生成的 主体 写入输出消息并返回
		return outputMessage.writeWith(body);
	}

	private <T> Flux<DataBuffer> encodePart(byte[] boundary, Part part, DataBufferFactory bufferFactory) {
		// 从 部分 的 头部信息 中创建一个新的 HttpHeaders 对象
		HttpHeaders headers = new HttpHeaders(part.headers());

		// 获取 部分 的名称
		String name = part.name();
		// 如果 Http头部 中不包含 "Content-Disposition" 信息，设置该信息
		if (!headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
			headers.setContentDispositionFormData(name,
					(part instanceof FilePart ? ((FilePart) part).filename() : null));
		}

		// 返回一个 Flux<DataBuffer>，按顺序连接各个部分
		return Flux.concat(
				// 生成边界行
				generateBoundaryLine(boundary, bufferFactory),
				// 生成部分的 头部信息
				generatePartHeaders(headers, bufferFactory),
				// 添加 部分 的内容
				part.content(),
				// 生成换行符
				generateNewLine(bufferFactory));
	}

}
