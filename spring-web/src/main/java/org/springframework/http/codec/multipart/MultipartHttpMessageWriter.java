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
import org.springframework.core.ResolvableTypeProvider;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.FormHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * {@link HttpMessageWriter} 实现，用于将 {@code MultiValueMap<String, ?>}
 * 序列化为 multipart 表单数据（即 {@code "multipart/form-data"}）并写入请求体中。
 *
 * <p>单个部分的序列化委托给其他写入器处理。默认情况下，仅支持 {@link String} 和 {@link Resource} 类型的部分，
 * 但可以通过构造函数参数配置其他类型的部分。
 *
 * <p>此写入器可以配置一个 {@link FormHttpMessageWriter} 来进行委托处理。
 * 这是支持表单数据和多部分数据的首选方式（而不是单独注册每个写入器），这样当未指定 {@link MediaType} 并且在目标元素类型上没有泛型时，
 * 我们可以检查实际映射中的值，并决定是写入普通表单数据（仅字符串值）还是其他内容。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see FormHttpMessageWriter
 * @since 5.0
 */
public class MultipartHttpMessageWriter extends MultipartWriterSupport
		implements HttpMessageWriter<MultiValueMap<String, ?>> {

	/**
	 * 用于抑制单个部分写入器的日志记录（在此级别记录完整映射）。
	 */
	private static final Map<String, Object> DEFAULT_HINTS = Hints.from(Hints.SUPPRESS_LOGGING_HINT, true);

	/**
	 * 部分写入器列表
	 */
	private final List<HttpMessageWriter<?>> partWriters;

	/**
	 * 表单写入器
	 */
	@Nullable
	private final HttpMessageWriter<MultiValueMap<String, String>> formWriter;


	/**
	 * 使用默认的部分写入器列表（String 和 Resource）构造函数。
	 */
	public MultipartHttpMessageWriter() {
		this(Arrays.asList(
				new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()),
				new ResourceHttpMessageWriter()
		));
	}

	/**
	 * 使用显式指定的部分写入器列表构造函数。
	 *
	 * @param partWriters 用于序列化部分的写入器列表
	 */
	public MultipartHttpMessageWriter(List<HttpMessageWriter<?>> partWriters) {
		this(partWriters, new FormHttpMessageWriter());
	}

	/**
	 * 使用显式指定的部分写入器列表和用于纯文本表单数据的写入器构造函数。
	 *
	 * @param partWriters 用于序列化部分的写入器列表
	 * @param formWriter  用于表单数据的写入器，如果未指定媒体类型并且实际映射包含仅字符串值，则为 {@code null}
	 */
	public MultipartHttpMessageWriter(List<HttpMessageWriter<?>> partWriters,
									  @Nullable HttpMessageWriter<MultiValueMap<String, String>> formWriter) {

		super(initMediaTypes(formWriter));
		this.partWriters = partWriters;
		this.formWriter = formWriter;
	}

	private static List<MediaType> initMediaTypes(@Nullable HttpMessageWriter<?> formWriter) {
		// 创建一个包含 Mime类型 所有元素的新 ArrayList
		List<MediaType> result = new ArrayList<>(MultipartHttpMessageReader.MIME_TYPES);
		// 如果 表单写入器 不为 null，则将 表单写入器 的可写媒体类型添加到 结果集 中
		if (formWriter != null) {
			result.addAll(formWriter.getWritableMediaTypes());
		}
		// 返回不可修改的结果集
		return Collections.unmodifiableList(result);
	}


	/**
	 * 返回配置的部分写入器列表。
	 *
	 * @return 部分写入器列表
	 * @since 5.0.7
	 */
	public List<HttpMessageWriter<?>> getPartWriters() {
		return Collections.unmodifiableList(this.partWriters);
	}


	/**
	 * 返回配置的表单写入器。
	 *
	 * @return 表单写入器
	 * @since 5.1.13
	 */
	@Nullable
	public HttpMessageWriter<MultiValueMap<String, String>> getFormWriter() {
		return this.formWriter;
	}


	@Override
	public Mono<Void> write(Publisher<? extends MultiValueMap<String, ?>> inputStream,
							ResolvableType elementType, @Nullable MediaType mediaType, ReactiveHttpOutputMessage outputMessage,
							Map<String, Object> hints) {

		// 将 输入流 转换为 Mono，并进行 flatMap 操作
		return Mono.from(inputStream)
				.flatMap(map -> {
					// 如果 表单写入器 为 null，如果是多部分数据
					if (this.formWriter == null || isMultipart(map, mediaType)) {
						// 调用 writeMultipart 方法写入数据
						return writeMultipart(map, outputMessage, mediaType, hints);
					} else {
						// 否则，将 map 转换为 MultiValueMap<String, String> 类型的 Mono
						@SuppressWarnings("unchecked")
						Mono<MultiValueMap<String, String>> input = Mono.just((MultiValueMap<String, String>) map);
						// 调用 表单写入器 的 write 方法写入数据
						return this.formWriter.write(input, elementType, mediaType, outputMessage, hints);
					}
				});
	}

	private boolean isMultipart(MultiValueMap<String, ?> map, @Nullable MediaType contentType) {
		// 如果 内容类型 不为 null，则检查其类型是否为 "multipart"
		if (contentType != null) {
			return contentType.getType().equalsIgnoreCase("multipart");
		}

		// 遍历 映射 中的所有值
		for (List<?> values : map.values()) {
			for (Object value : values) {
				// 如果值不为 null 并且不是 String 类型，则返回 true
				if (value != null && !(value instanceof String)) {
					return true;
				}
			}
		}

		// 如果没有找到非 String 类型的值，则返回 false
		return false;
	}

	private Mono<Void> writeMultipart(MultiValueMap<String, ?> map,
									  ReactiveHttpOutputMessage outputMessage, @Nullable MediaType mediaType, Map<String, Object> hints) {

		// 生成 多部分 的 边界
		byte[] boundary = generateMultipartBoundary();

		// 获取包含边界的 多部分 媒体类型
		mediaType = getMultipartMediaType(mediaType, boundary);
		// 将媒体类型设置到输出消息的头部
		outputMessage.getHeaders().setContentType(mediaType);

		// 打印日志信息，根据是否 启用日志记录请求详细信息 判断是否记录详细信息
		LogFormatUtils.traceDebug(logger, traceOn -> Hints.getLogPrefix(hints) + "Encoding " +
				(isEnableLoggingRequestDetails() ?
						LogFormatUtils.formatValue(map, !traceOn) :
						"parts " + map.keySet() + " (content masked)"));

		// 获取数据缓冲工厂
		DataBufferFactory bufferFactory = outputMessage.bufferFactory();

		// 创建 Flux<DataBuffer>，从 映射 的条目集合开始
		Flux<DataBuffer> body = Flux.fromIterable(map.entrySet())
				// 处理每个条目，调用 encodePartValues 方法编码部分值
				.concatMap(entry -> encodePartValues(boundary, entry.getKey(), entry.getValue(), bufferFactory))
				// 将生成的最后一行内容连接到 Flux<DataBuffer> 中
				.concatWith(generateLastLine(boundary, bufferFactory))
				// 当丢弃 PooledDataBuffer 类型时，释放 DataBuffer
				.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);

		// 如果日志级别为 DEBUG，则在每个数据缓冲上标记日志信息
		if (logger.isDebugEnabled()) {
			body = body.doOnNext(buffer -> Hints.touchDataBuffer(buffer, hints, logger));
		}

		// 将 响应数据 写入的输出消息
		return outputMessage.writeWith(body);
	}

	private Flux<DataBuffer> encodePartValues(
			byte[] boundary, String name, List<?> values, DataBufferFactory bufferFactory) {

		return Flux.fromIterable(values)
				.concatMap(value -> encodePart(boundary, name, value, bufferFactory));
	}

	@SuppressWarnings("unchecked")
	private <T> Flux<DataBuffer> encodePart(byte[] boundary, String name, T value, DataBufferFactory factory) {
		// 创建 MultipartHttpOutputMessage 对象
		MultipartHttpOutputMessage message = new MultipartHttpOutputMessage(factory);
		// 获取消息 头部 对象
		HttpHeaders headers = message.getHeaders();

		T body;
		ResolvableType resolvableType = null;

		// 如果 值 是 HttpEntity 类型
		if (value instanceof HttpEntity) {
			HttpEntity<T> httpEntity = (HttpEntity<T>) value;
			// 将 Http实体 的头部信息添加到消息头中
			headers.putAll(httpEntity.getHeaders());
			// 获取 Http实体 的 主体
			body = httpEntity.getBody();
			// 断言确保 主体 不为 null
			Assert.state(body != null, "MultipartHttpMessageWriter only supports HttpEntity with body");
			// 如果 Http实体 是 ResolvableTypeProvider 的实例，则获取其 可解析类型
			if (httpEntity instanceof ResolvableTypeProvider) {
				resolvableType = ((ResolvableTypeProvider) httpEntity).getResolvableType();
			}
		} else {
			// 否则，直接将 值 设置为 主体
			body = value;
		}

		// 如果 可解析类型 仍然为 null，则根据 主体 的类创建 可解析类型
		if (resolvableType == null) {
			resolvableType = ResolvableType.forClass(body.getClass());
		}

		// 如果消息头中不包含 Content-Disposition
		if (!headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
			// 如果 主体 是 Resource 类型
			if (body instanceof Resource) {
				// 使用 Resource 的文件名，设置内容的 Content-Disposition
				headers.setContentDispositionFormData(name, ((Resource) body).getFilename());
			} else if (resolvableType.resolve() == Resource.class) {
				// 如果 可解析类型 解析为 Resource.class
				// 将 主体 转换为 Mono，并在下一个元素到达时设置 Content-Disposition
				body = (T) Mono.from((Publisher<?>) body).doOnNext(o -> headers
						.setContentDispositionFormData(name, ((Resource) o).getFilename()));
			} else {
				// 否则，设置 Content-Disposition 为默认值
				headers.setContentDispositionFormData(name, null);
			}
		}

		// 获取消息头的 内容类型
		MediaType contentType = headers.getContentType();

		// 定义 最终主体类型 为 可解析类型
		final ResolvableType finalBodyType = resolvableType;

		// 从 部分写入器列表 中找到可以处理 最终主体类型 和 内容类型 的第一个 写入器
		Optional<HttpMessageWriter<?>> writer = this.partWriters.stream()
				.filter(partWriter -> partWriter.canWrite(finalBodyType, contentType))
				.findFirst();

		// 如果找不到合适的 写入器，则抛出 编解码器异常
		if (!writer.isPresent()) {
			return Flux.error(new CodecException("No suitable writer found for part: " + name));
		}

		// 如果 主体 是 Publisher 类型，则将其转换为 Publisher<T>
		Publisher<T> bodyPublisher = body instanceof Publisher ? (Publisher<T>) body : Mono.just(body);

		// 写入器 将调用 MultipartHttpOutputMessage#write 方法，
		// 该方法并不实际写入数据，而是存储 主体 Flux 并返回 Mono.empty()。

		// 执行 部分内容准备，可以从 MultipartHttpOutputMessage 中访问部分内容，并用于写入到实际的请求体中
		Mono<Void> partContentReady = ((HttpMessageWriter<T>) writer.get())
				// 使用 写入器 写入内容到 消息 中
				.write(bodyPublisher, resolvableType, contentType, message, DEFAULT_HINTS);

		// 完成 部分内容准备 后，从 MultipartHttpOutputMessage 中获取部分内容的 Flux<DataBuffer>
		Flux<DataBuffer> partContent = partContentReady.thenMany(Flux.defer(message::getBody));

		// 返回拼接后的 Flux，包括生成的边界行、部分内容和新的换行符
		return Flux.concat(
				generateBoundaryLine(boundary, factory),
				partContent,
				generateNewLine(factory));
	}


	private class MultipartHttpOutputMessage implements ReactiveHttpOutputMessage {

		/**
		 * 一个表示数据缓冲区工厂的字段，用于创建数据缓冲区。
		 */
		private final DataBufferFactory bufferFactory;

		/**
		 * 一个表示HTTP头的字段，用于存储HTTP头信息。
		 */
		private final HttpHeaders headers = new HttpHeaders();

		/**
		 * 一个表示HTTP消息是否已提交的字段，使用AtomicBoolean以确保线程安全。
		 */
		private final AtomicBoolean committed = new AtomicBoolean();

		/**
		 * 一个表示HTTP消息体的字段，使用Flux<DataBuffer>表示消息体的流。
		 */
		@Nullable
		private Flux<DataBuffer> body;

		public MultipartHttpOutputMessage(DataBufferFactory bufferFactory) {
			this.bufferFactory = bufferFactory;
		}

		@Override
		public HttpHeaders getHeaders() {
			return (this.body != null ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
		}

		@Override
		public DataBufferFactory bufferFactory() {
			return this.bufferFactory;
		}

		@Override
		public void beforeCommit(Supplier<? extends Mono<Void>> action) {
			this.committed.set(true);
		}

		@Override
		public boolean isCommitted() {
			return this.committed.get();
		}

		@Override
		public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
			// 如果 body 不为空
			if (this.body != null) {
				// 返回一个 Mono 错误，抛出 IllegalStateException 异常
				return Mono.error(new IllegalStateException("Multiple calls to writeWith() not supported"));
			}

			// 生成部分头信息，并将其与 body 连接，保存到 this.body 中
			this.body = generatePartHeaders(this.headers, this.bufferFactory).concatWith(body);

			// 实际上我们不想写入（只是保存 body 的  Flux）
			return Mono.empty();
		}

		@Override
		public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
			return Mono.error(new UnsupportedOperationException());
		}

		public Flux<DataBuffer> getBody() {
			return (this.body != null ? this.body :
					Flux.error(new IllegalStateException("Body has not been written yet")));
		}

		@Override
		public Mono<Void> setComplete() {
			return Mono.error(new UnsupportedOperationException());
		}
	}

}
