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
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.synchronoss.cloud.nio.multipart.MultipartUtils;
import org.synchronoss.cloud.nio.multipart.*;
import org.synchronoss.cloud.nio.stream.storage.NameAwarePurgableFileInputStream;
import org.synchronoss.cloud.nio.stream.storage.StreamStorage;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * {@code HttpMessageReader}用于使用Synchronoss NIO Multipart库解析 {@code "multipart/form-data"} 请求，
 * 将其解析为 {@link Part} 流。
 *
 * <p>此读取器可以提供给 {@link MultipartHttpMessageReader}，以便将所有部分聚合到映射中。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @see <a href="https://github.com/synchronoss/nio-multipart">同步 NIO多部分</a>
 * @see MultipartHttpMessageReader
 * @since 5.0
 */
public class SynchronossPartHttpMessageReader extends LoggingCodecSupport implements HttpMessageReader<Part> {
	/**
	 * 文件存储目录前缀
	 */
	private static final String FILE_STORAGE_DIRECTORY_PREFIX = "synchronoss-file-upload-";

	/**
	 * 最大内存大小
	 */
	private int maxInMemorySize = 256 * 1024;

	/**
	 * 每个部件的最大磁盘使用量
	 */
	private long maxDiskUsagePerPart = -1;

	/**
	 * 最大部分数量
	 */
	private int maxParts = -1;

	/**
	 * 文件存储目录
	 */
	private final AtomicReference<Path> fileStorageDirectory = new AtomicReference<>();


	/**
	 * 配置每个部分允许使用的最大内存量。
	 * 当超过限制时：
	 * <ul>
	 * <li>文件部分将被写入临时文件。
	 * <li>非文件部分将使用 {@link DataBufferLimitException} 拒绝。
	 * </ul>
	 * <p>默认情况下，此值设置为 256K。
	 *
	 * @param byteCount 内存限制（字节）；如果设置为 -1，则不强制执行此限制，所有部分都可以写入磁盘，仅受 {@link #setMaxDiskUsagePerPart(long) maxDiskUsagePerPart} 属性的限制。
	 * @since 5.1.11
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
	}

	/**
	 * 获取 {@link #setMaxInMemorySize 设置的} 最大内存大小。
	 *
	 * @since 5.1.11
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}

	/**
	 * 配置文件部分允许的最大磁盘空间量。
	 * <p>默认情况下，此值设置为 -1。
	 *
	 * @param maxDiskUsagePerPart 磁盘限制（字节），或者 -1 表示无限制
	 * @since 5.1.11
	 */
	public void setMaxDiskUsagePerPart(long maxDiskUsagePerPart) {
		this.maxDiskUsagePerPart = maxDiskUsagePerPart;
	}

	/**
	 * 获取 {@link #setMaxDiskUsagePerPart 设置的} 最大磁盘使用量。
	 *
	 * @since 5.1.11
	 */
	public long getMaxDiskUsagePerPart() {
		return this.maxDiskUsagePerPart;
	}

	/**
	 * 指定在给定的多部分请求中允许的最大部分数目。
	 *
	 * @since 5.1.11
	 */
	public void setMaxParts(int maxParts) {
		this.maxParts = maxParts;
	}

	/**
	 * 返回 {@link #setMaxParts 设置的} 部分数量限制。
	 *
	 * @since 5.1.11
	 */
	public int getMaxParts() {
		return this.maxParts;
	}

	/**
	 * 设置用于存储大于 {@link #setMaxInMemorySize(int) maxInMemorySize} 的部分的目录。
	 * 默认情况下，将创建一个新的临时目录。
	 *
	 * @throws IOException 如果发生 I/O 错误，或者父目录不存在
	 * @since 5.3.7
	 */
	public void setFileStorageDirectory(Path fileStorageDirectory) throws IOException {
		Assert.notNull(fileStorageDirectory, "FileStorageDirectory must not be null");
		if (!Files.exists(fileStorageDirectory)) {
			// 如果文件存储目录不存在，则创建目录
			Files.createDirectory(fileStorageDirectory);
		}
		// 设置文件存储目录
		this.fileStorageDirectory.set(fileStorageDirectory);
	}


	@Override
	public List<MediaType> getReadableMediaTypes() {
		return MultipartHttpMessageReader.MIME_TYPES;
	}

	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		// 检查 元素类型 是否为 Part 类型
		if (Part.class.equals(elementType.toClass())) {
			// 如果 媒体类型 为 null，则返回 true
			if (mediaType == null) {
				return true;
			}
			// 遍历可读取的媒体类型列表
			for (MediaType supportedMediaType : getReadableMediaTypes()) {
				// 如果 媒体类型 兼容于当前遍历的 支持的媒体类型，则返回 true
				if (supportedMediaType.isCompatibleWith(mediaType)) {
					return true;
				}
			}
		}
		// 如果不满足条件，则返回 false
		return false;
	}

	@Override
	public Flux<Part> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
		// 获取文件存储目录，并转换为 Flux
		return getFileStorageDirectory().flatMapMany(directory ->
				// 创建一个 SynchronossPartGenerator 实例，并以其为基础创建 Flux
				Flux.create(new SynchronossPartGenerator(message, directory))
						// 对每个生成的 Part 进行操作
						.doOnNext(part -> {
							// 如果未抑制日志记录
							if (!Hints.isLoggingSuppressed(hints)) {
								// 记录调试日志
								LogFormatUtils.traceDebug(logger, traceOn -> Hints.getLogPrefix(hints) + "Parsed " +
										// 如果启用请求详情日志记录，则格式化输出 部分
										(isEnableLoggingRequestDetails() ?
												LogFormatUtils.formatValue(part, !traceOn) :
												// 否则输出部件的名称和内容屏蔽提示
												"parts '" + part.name() + "' (content masked)"));
							}
						}));
	}

	@Override
	public Mono<Part> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
		return Mono.error(new UnsupportedOperationException("Cannot read multipart request body into single Part"));
	}

	private Mono<Path> getFileStorageDirectory() {
		// 创建一个延迟执行的 Mono
		return Mono.defer(() -> {
			// 获取文件存储目录路径
			Path directory = this.fileStorageDirectory.get();
			// 如果文件存储目录路径不为 null，则返回包含该路径的 Mono
			if (directory != null) {
				return Mono.just(directory);
			} else {
				// 如果文件存储目录路径为 null，则创建临时目录并返回其路径的 Mono
				return Mono.fromCallable(() -> {
					// 创建临时目录
					Path tempDirectory = Files.createTempDirectory(FILE_STORAGE_DIRECTORY_PREFIX);
					// 尝试设置文件存储目录路径为创建的临时目录路径
					if (this.fileStorageDirectory.compareAndSet(null, tempDirectory)) {
						// 如果设置成功，则返回临时目录路径的 Mono
						return tempDirectory;
					} else {
						// 如果设置失败，则删除临时目录
						try {
							Files.delete(tempDirectory);
						} catch (IOException ignored) {
						}
						// 返回当前文件存储目录路径的 Mono
						return this.fileStorageDirectory.get();
					}
					// 在有界弹性调度器上执行创建临时目录的操作
				}).subscribeOn(Schedulers.boundedElastic());
			}
		});
	}


	/**
	 * 订阅输入流并向 Synchronoss 解析器提供数据。然后监听解析器输出，创建部件并将它们推送到 FluxSink 中。
	 */
	private class SynchronossPartGenerator extends BaseSubscriber<DataBuffer> implements Consumer<FluxSink<Part>> {
		/**
		 * 响应式Http输入消息
		 */
		private final ReactiveHttpInputMessage inputMessage;

		/**
		 * 存储工厂
		 */
		private final LimitedPartBodyStreamStorageFactory storageFactory = new LimitedPartBodyStreamStorageFactory();

		/**
		 * 文件存储目录
		 */
		private final Path fileStorageDirectory;

		/**
		 * Nio 多部分解析监听器
		 */
		@Nullable
		private NioMultipartParserListener listener;

		/**
		 * Nio 多部分解析器
		 */
		@Nullable
		private NioMultipartParser parser;

		public SynchronossPartGenerator(ReactiveHttpInputMessage inputMessage, Path fileStorageDirectory) {
			this.inputMessage = inputMessage;
			this.fileStorageDirectory = fileStorageDirectory;
		}

		@Override
		public void accept(FluxSink<Part> sink) {
			// 获取输入消息的头部信息
			HttpHeaders headers = this.inputMessage.getHeaders();
			// 获取输入消息的内容类型
			MediaType mediaType = headers.getContentType();
			// 断言内容类型不为 null，否则抛出异常
			Assert.state(mediaType != null, "No content type set");

			// 获取内容长度
			int length = getContentLength(headers);
			// 获取字符集，如果内容类型中的字符集为 null，则使用默认的 UTF-8 字符集
			Charset charset = Optional.ofNullable(mediaType.getCharset()).orElse(StandardCharsets.UTF_8);

			// 创建 MultipartContext 对象
			MultipartContext context = new MultipartContext(mediaType.toString(), length, charset.name());

			// 创建 FluxSinkAdapterListener 对象
			this.listener = new FluxSinkAdapterListener(sink, context, this.storageFactory);

			// 创建 MultipartParser 对象，设置上下文信息、临时文件存储路径、部件体流存储工厂，以及 NIO 操作使用监听器
			this.parser = Multipart
					.multipart(context)
					.saveTemporaryFilesTo(this.fileStorageDirectory.toString())
					.usePartBodyStreamStorageFactory(this.storageFactory)
					.forNIO(this.listener);

			// 订阅输入消息的消息体，并将当前对象作为订阅者
			this.inputMessage.getBody().subscribe(this);
		}

		@Override
		protected void hookOnNext(DataBuffer buffer) {
			Assert.state(this.parser != null && this.listener != null, "Not initialized yet");

			// 获取缓冲区中可读取的字节数
			int size = buffer.readableByteCount();
			// 增加存储工厂的字节计数
			this.storageFactory.increaseByteCount(size);
			// 创建一个字节数组，用于存储从缓冲区中读取的数据
			byte[] resultBytes = new byte[size];
			// 从缓冲区中读取数据并存储到 字节结果 数组中
			buffer.read(resultBytes);

			try {
				// 将读取到的字节数组写入解析器中处理
				this.parser.write(resultBytes);
			} catch (IOException ex) {
				// 如果发生 IOException 异常，取消操作，并获取当前部件索引
				cancel();
				int index = this.storageFactory.getCurrentPartIndex();
				// 通过监听器报告解析器错误
				this.listener.onError("Parser error for part [" + index + "]", ex);
			} finally {
				// 释放缓冲区
				DataBufferUtils.release(buffer);
			}
		}

		@Override
		protected void hookOnError(Throwable ex) {
			// 如果监听器不为 null
			if (this.listener != null) {
				// 获取当前部件索引
				int index = this.storageFactory.getCurrentPartIndex();
				// 通过监听器报告解析器错误，包括部件索引和异常信息
				this.listener.onError("Failure while parsing part[" + index + "]", ex);
			}
		}

		@Override
		protected void hookOnComplete() {
			if (this.listener != null) {
				// 如果监听器不为空，则通知所有多部分已完成
				this.listener.onAllPartsFinished();
			}
		}

		@Override
		protected void hookFinally(SignalType type) {
			try {
				if (this.parser != null) {
					// 如果解析器不为空，关闭解析器
					this.parser.close();
				}
			} catch (IOException ex) {
				// 忽略
			}
		}

		private int getContentLength(HttpHeaders headers) {
			// 由于此问题尚未修复 https://github.com/synchronoss/nio-multipart/issues/10
			// 获取内容长度
			long length = headers.getContentLength();
			// 如果内容长度为 int 类型范围内的值，则将其转换为 int 返回；否则返回 -1
			return (int) length == length ? (int) length : -1;
		}
	}


	private class LimitedPartBodyStreamStorageFactory implements PartBodyStreamStorageFactory {
		/**
		 * 存储工厂
		 */
		private final PartBodyStreamStorageFactory storageFactory = (maxInMemorySize > 0 ?
				new DefaultPartBodyStreamStorageFactory(maxInMemorySize) :
				new DefaultPartBodyStreamStorageFactory());

		/**
		 * 索引位置
		 */
		private int index = 1;

		/**
		 * 是否是文件部分
		 */
		private boolean isFilePart;

		/**
		 * 部分大小
		 */
		private long partSize;

		public int getCurrentPartIndex() {
			return this.index;
		}

		@Override
		public StreamStorage newStreamStorageForPartBody(Map<String, List<String>> headers, int index) {
			this.index = index;
			this.isFilePart = (MultipartUtils.getFileName(headers) != null);
			this.partSize = 0;
			if (maxParts > 0 && index > maxParts) {
				//如果最大部分大于0，并且索引位置大于最大部分，抛出异常
				throw new DecodingException("Too many parts: Part[" + index + "] but maxParts=" + maxParts);
			}
			return this.storageFactory.newStreamStorageForPartBody(headers, index);
		}

		public void increaseByteCount(long byteCount) {
			// 增加部分大小
			this.partSize += byteCount;

			// 如果设置了最大内存大小，并且当前部件不是文件部件，并且部件大小超过了最大内存大小
			if (maxInMemorySize > 0 && !this.isFilePart && this.partSize >= maxInMemorySize) {
				// 抛出 数据缓冲区限制异常
				throw new DataBufferLimitException("Part[" + this.index + "] " +
						"exceeded the in-memory limit of " + maxInMemorySize + " bytes");
			}

			// 如果设置了最大磁盘使用量，并且当前部件是文件部件，并且部件大小超过了最大磁盘使用量
			if (maxDiskUsagePerPart > 0 && this.isFilePart && this.partSize > maxDiskUsagePerPart) {
				// 抛出 解码异常
				throw new DecodingException("Part[" + this.index + "] " +
						"exceeded the disk usage limit of " + maxDiskUsagePerPart + " bytes");
			}
		}

		public void partFinished() {
			this.index++;
			this.isFilePart = false;
			this.partSize = 0;
		}
	}


	/**
	 * 监听解析器输出并适配为 {@code Flux<Sink<Part>>}。
	 */
	private static class FluxSinkAdapterListener implements NioMultipartParserListener {
		/**
		 * 部分接收点
		 */
		private final FluxSink<Part> sink;

		/**
		 * 多部分上下文
		 */
		private final MultipartContext context;

		/**
		 * 存储工厂
		 */
		private final LimitedPartBodyStreamStorageFactory storageFactory;

		/**
		 * 是否终结
		 */
		private final AtomicInteger terminated = new AtomicInteger();

		FluxSinkAdapterListener(
				FluxSink<Part> sink, MultipartContext context, LimitedPartBodyStreamStorageFactory factory) {

			this.sink = sink;
			this.context = context;
			this.storageFactory = factory;
		}

		@Override
		public void onPartFinished(StreamStorage storage, Map<String, List<String>> headers) {
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.putAll(headers);
			this.storageFactory.partFinished();
			this.sink.next(createPart(storage, httpHeaders));
		}

		private Part createPart(StreamStorage storage, HttpHeaders httpHeaders) {
			// 从 HTTP 头部获取文件名
			String filename = MultipartUtils.getFileName(httpHeaders);

			// 如果文件名不为 null，则创建并返回 SynchronossFilePart 对象
			if (filename != null) {
				return new SynchronossFilePart(httpHeaders, filename, storage);
			} else if (MultipartUtils.isFormField(httpHeaders, this.context)) {
				// 如果不是文件部分，并且是表单字段
				// 从存储中读取表单参数值
				String value = MultipartUtils.readFormParameterValue(storage, httpHeaders);
				// 创建并返回 SynchronossFormFieldPart 对象
				return new SynchronossFormFieldPart(httpHeaders, value);
			} else {
				// 否则，创建并返回 SynchronossPart 对象
				return new SynchronossPart(httpHeaders, storage);
			}
		}

		@Override
		public void onError(String message, Throwable cause) {
			if (this.terminated.getAndIncrement() == 0) {
				// 如果已终结，抛出解码异常
				this.sink.error(new DecodingException(message, cause));
			}
		}

		@Override
		public void onAllPartsFinished() {
			if (this.terminated.getAndIncrement() == 0) {
				// 如果已终结，将 接收点 设置为已完成
				this.sink.complete();
			}
		}

		@Override
		public void onNestedPartStarted(Map<String, List<String>> headersFromParentPart) {
		}

		@Override
		public void onNestedPartFinished() {
		}
	}


	private abstract static class AbstractSynchronossPart implements Part {
		/**
		 * 部分
		 */
		private final String name;

		/**
		 * Http头部
		 */
		private final HttpHeaders headers;

		AbstractSynchronossPart(HttpHeaders headers) {
			Assert.notNull(headers, "HttpHeaders is required");
			this.name = MultipartUtils.getFieldName(headers);
			this.headers = headers;
		}

		@Override
		public String name() {
			return this.name;
		}

		@Override
		public HttpHeaders headers() {
			return this.headers;
		}

		@Override
		public String toString() {
			return "Part '" + this.name + "', headers=" + this.headers;
		}
	}


	private static class SynchronossPart extends AbstractSynchronossPart {
		/**
		 * 流存储
		 */
		private final StreamStorage storage;

		SynchronossPart(HttpHeaders headers, StreamStorage storage) {
			super(headers);
			Assert.notNull(storage, "StreamStorage is required");
			this.storage = storage;
		}

		@Override
		@SuppressWarnings("resource")
		public Flux<DataBuffer> content() {
			return DataBufferUtils.readInputStream(
					getStorage()::getInputStream, DefaultDataBufferFactory.sharedInstance, 4096);
		}

		protected StreamStorage getStorage() {
			return this.storage;
		}

		@Override
		public Mono<Void> delete() {
			return Mono.fromRunnable(() -> {
				// 获取文件
				File file = getFile();
				if (file != null) {
					// 如果获取到了文件，删除该文件
					file.delete();
				}
			});
		}

		@Nullable
		private File getFile() {
			InputStream inputStream = null;
			try {
				// 获取存储对象的输入流
				inputStream = getStorage().getInputStream();
				// 如果输入流是 NameAwarePurgableFileInputStream 类的实例
				if (inputStream instanceof NameAwarePurgableFileInputStream) {
					// 将输入流转换为 NameAwarePurgableFileInputStream 类型
					NameAwarePurgableFileInputStream stream = (NameAwarePurgableFileInputStream) inputStream;
					// 返回该流的文件对象
					return stream.getFile();
				}
			} finally {
				// 关闭输入流
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException ignore) {
						// 忽略关闭流时的异常
					}
				}
			}
			// 如果未获取到符合条件的流，返回 null
			return null;
		}
	}


	private static class SynchronossFilePart extends SynchronossPart implements FilePart {
		/**
		 * 文件通道选项
		 */
		private static final OpenOption[] FILE_CHANNEL_OPTIONS =
				{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};

		/**
		 * 文件名称
		 */
		private final String filename;

		SynchronossFilePart(HttpHeaders headers, String filename, StreamStorage storage) {
			super(headers, storage);
			this.filename = filename;
		}

		@Override
		public String filename() {
			return this.filename;
		}

		@Override
		public Mono<Void> transferTo(Path dest) {
			// 初始化输入输出通道为 null
			ReadableByteChannel input = null;
			FileChannel output = null;
			try {
				// 获取存储对象的输入流，并创建对应的输入通道
				input = Channels.newChannel(getStorage().getInputStream());
				// 打开目标文件的文件通道
				output = FileChannel.open(dest, FILE_CHANNEL_OPTIONS);
				// 获取输入通道的大小，如果是文件通道则获取其大小，否则设为 Long.MAX_VALUE
				long size = (input instanceof FileChannel ? ((FileChannel) input).size() : Long.MAX_VALUE);
				long totalWritten = 0;
				// 循环从输入通道向输出通道传输数据
				while (totalWritten < size) {
					long written = output.transferFrom(input, totalWritten, size - totalWritten);
					// 如果写入数据量小于等于 0，则退出循环
					if (written <= 0) {
						break;
					}
					totalWritten += written;
				}
			} catch (IOException ex) {
				// 如果发生 IO异常 异常，则返回一个包含异常的 Mono 对象
				return Mono.error(ex);
			} finally {
				// 关闭输入通道
				if (input != null) {
					try {
						input.close();
					} catch (IOException ignored) {
						// 忽略
					}
				}
				// 关闭输出通道
				if (output != null) {
					try {
						output.close();
					} catch (IOException ignored) {
						// 忽略
					}
				}
			}
			// 返回一个空的 Mono 对象，表示操作完成
			return Mono.empty();
		}

		@Override
		public String toString() {
			return "Part '" + name() + "', filename='" + this.filename + "'";
		}
	}


	private static class SynchronossFormFieldPart extends AbstractSynchronossPart implements FormFieldPart {
		/**
		 * 内容
		 */
		private final String content;

		SynchronossFormFieldPart(HttpHeaders headers, String content) {
			super(headers);
			this.content = content;
		}

		@Override
		public String value() {
			return this.content;
		}

		@Override
		public Flux<DataBuffer> content() {
			// 获取内容的字节数组
			byte[] bytes = this.content.getBytes(getCharset());
			// 将字节数组转换为 DataBuffer 对象
			return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
		}

		private Charset getCharset() {
			// 获取字符集
			String name = MultipartUtils.getCharEncoding(headers());
			// 如果字符集为空，则返回UTF_8。否则返回该字符集。
			return (name != null ? Charset.forName(name) : StandardCharsets.UTF_8);
		}

		@Override
		public String toString() {
			return "Part '" + name() + "=" + this.content + "'";
		}
	}

}
