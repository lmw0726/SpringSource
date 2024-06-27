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

package org.springframework.http.codec.multipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscription;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.FastByteArrayOutputStream;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.context.Context;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 订阅一个 token 流（即 {@link MultipartParser#parse(Flux, byte[], int, Charset)} 的结果），
 * 并生成一个 {@link Part} 对象的 flux。
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
final class PartGenerator extends BaseSubscriber<MultipartParser.Token> {

	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(PartGenerator.class);

	/**
	 * 表示当前状态的原子引用，初始状态为 {@link InitialState}。
	 */
	private final AtomicReference<State> state = new AtomicReference<>(new InitialState());

	/**
	 * 用于计数 部分 的原子整数。
	 */
	private final AtomicInteger partCount = new AtomicInteger();

	/**
	 * 标记是否有请求尚未完成的原子布尔值。
	 */
	private final AtomicBoolean requestOutstanding = new AtomicBoolean();

	/**
	 * 用于发出 {@link Part} 对象的 Flux sink。
	 */
	private final FluxSink<Part> sink;

	/**
	 * 最大 部分 数量。
	 */
	private final int maxParts;

	/**
	 * 标记是否为流式处理。
	 */
	private final boolean streaming;

	/**
	 * 内存中最大大小。
	 */
	private final int maxInMemorySize;

	/**
	 * 每个 部分 的最大磁盘使用量。
	 */
	private final long maxDiskUsagePerPart;

	/**
	 * 用于存储文件的目录的 Mono 对象。
	 */
	private final Mono<Path> fileStorageDirectory;

	/**
	 * 用于阻塞操作的调度器。
	 */
	private final Scheduler blockingOperationScheduler;


	private PartGenerator(FluxSink<Part> sink, int maxParts, int maxInMemorySize, long maxDiskUsagePerPart,
						  boolean streaming, Mono<Path> fileStorageDirectory, Scheduler blockingOperationScheduler) {

		this.sink = sink;
		this.maxParts = maxParts;
		this.maxInMemorySize = maxInMemorySize;
		this.maxDiskUsagePerPart = maxDiskUsagePerPart;
		this.streaming = streaming;
		this.fileStorageDirectory = fileStorageDirectory;
		this.blockingOperationScheduler = blockingOperationScheduler;
	}

	/**
	 * 从给定的 token 流创建 parts。
	 *
	 * @param tokens                     token 流
	 * @param maxParts                   最大 part 数量
	 * @param maxInMemorySize            内存中最大大小
	 * @param maxDiskUsagePerPart        每个 part 的最大磁盘使用量
	 * @param streaming                  是否为流式处理
	 * @param fileStorageDirectory       用于存储文件的目录的 Mono 对象
	 * @param blockingOperationScheduler 用于阻塞操作的调度器
	 * @return 生成的 part 对象的 Flux
	 */
	public static Flux<Part> createParts(Flux<MultipartParser.Token> tokens, int maxParts, int maxInMemorySize,
										 long maxDiskUsagePerPart, boolean streaming, Mono<Path> fileStorageDirectory,
										 Scheduler blockingOperationScheduler) {

		// 返回一个 Flux
		return Flux.create(sink -> {
			// 创建一个 部分生成器 对象
			PartGenerator generator = new PartGenerator(sink, maxParts, maxInMemorySize, maxDiskUsagePerPart, streaming,
					fileStorageDirectory, blockingOperationScheduler);

			// 当 sink 取消时，调用 部分生成器 的 onSinkCancel 方法
			sink.onCancel(generator::onSinkCancel);

			// 当 sink 请求时，调用 部分生成器 的 请求令牌 方法
			sink.onRequest(l -> generator.requestToken());

			// 订阅 令牌生成器
			tokens.subscribe(generator);
		});
	}

	@Override
	public Context currentContext() {
		return Context.of(this.sink.contextView());
	}

	@Override
	protected void hookOnSubscribe(Subscription subscription) {
		requestToken();
	}

	@Override
	protected void hookOnNext(MultipartParser.Token token) {
		// 将 请求尚未完成 标记设置为 false
		this.requestOutstanding.set(false);

		// 获取当前 状态
		State state = this.state.get();

		// 如果 令牌 是 MultipartParser.HeadersToken 类型
		if (token instanceof MultipartParser.HeadersToken) {
			// 完成上一个部分
			state.partComplete(false);

			// 如果部分数量过多，则返回
			if (tooManyParts()) {
				return;
			}

			// 传入当前 状态 和 令牌 的头信息，创建新的部分
			newPart(state, token.headers());
		} else {
			// 否则，将当前状态的 主体 设置为 令牌 的 缓存区
			state.body(token.buffer());
		}
	}

	private void newPart(State currentState, HttpHeaders headers) {
		// 如果 头部信息 表示一个表单字段
		if (isFormField(headers)) {
			// 切换到表单字段状态
			changeStateInternal(new FormFieldState(headers));
			// 请求新的 令牌
			requestToken();
		} else if (!this.streaming) {
			// 否则，如果不是流式处理
			// 切换到内存状态
			changeStateInternal(new InMemoryState(headers));
			// 请求新的 令牌
			requestToken();
		} else {
			// 否则，如果是流式处理
			// 创建一个流式内容的 Flux
			Flux<DataBuffer> streamingContent = Flux.create(contentSink -> {
				// 创建新的流式状态
				State newState = new StreamingState(contentSink);
				// 如果状态切换成功
				if (changeState(currentState, newState)) {
					// 当内容Sink的请求时， 请求 令牌
					contentSink.onRequest(l -> requestToken());
					// 请求新的 令牌
					requestToken();
				}
			});
			// 发出部分内容
			emitPart(DefaultParts.part(headers, streamingContent));
		}
	}

	@Override
	protected void hookOnComplete() {
		this.state.get().partComplete(true);
	}

	@Override
	protected void hookOnError(Throwable throwable) {
		// 获取当前状态并处理错误
		this.state.get().error(throwable);
		// 切换到已释放状态
		changeStateInternal(DisposedState.INSTANCE);
		// 向 sink 发出错误信号
		this.sink.error(throwable);
	}

	private void onSinkCancel() {
		// 切换到已释放状态
		changeStateInternal(DisposedState.INSTANCE);
		// 向 sink 发出取消信号
		cancel();
	}

	boolean changeState(State oldState, State newState) {
		// 如果当前状态与 旧状态 相同，则将其设置为 新状态
		if (this.state.compareAndSet(oldState, newState)) {
			// 如果日志记录级别为 Trace，则记录状态变更信息
			if (logger.isTraceEnabled()) {
				logger.trace("Changed state: " + oldState + " -> " + newState);
			}
			// 释放旧状态的资源
			oldState.dispose();
			// 返回 true 表示状态变更成功
			return true;
		} else {
			// 如果无法进行状态变更，则记录警告日志
			logger.warn("Could not switch from " + oldState +
					" to " + newState + "; current state:"
					+ this.state.get());
			// 返回 false 表示状态变更失败
			return false;
		}
	}

	private void changeStateInternal(State newState) {
		// 如果当前状态已经是 已释放状态，则返回
		if (this.state.get() == DisposedState.INSTANCE) {
			return;
		}
		// 设置新状态，获取旧状态
		State oldState = this.state.getAndSet(newState);
		// 如果日志记录级别为 Trace，则记录状态变更信息
		if (logger.isTraceEnabled()) {
			logger.trace("Changed state: " + oldState + " -> " + newState);
		}
		// 释放旧状态的资源
		oldState.dispose();
	}

	void emitPart(Part part) {
		// 如果日志记录级别为 Trace，则记录正在发射的 部分 信息
		if (logger.isTraceEnabled()) {
			logger.trace("Emitting: " + part);
		}
		// 向 sink 发射下一个 部分
		this.sink.next(part);
	}

	void emitComplete() {
		this.sink.complete();
	}


	void emitError(Throwable t) {
		// 取消操作
		cancel();
		// 使用 sink 处理错误
		this.sink.error(t);
	}

	void requestToken() {
		// 检查是否有上游
		if (upstream() != null &&
				// 检查 sink 是否未被取消
				!this.sink.isCancelled() &&
				// 检查下游是否请求了数据
				this.sink.requestedFromDownstream() > 0 &&
				// 检查并设置请求未完成标志
				this.requestOutstanding.compareAndSet(false, true)) {
			// 请求一个数据
			request(1);
		}
	}

	private boolean tooManyParts() {
		// 增加 部分 计数
		int count = this.partCount.incrementAndGet();

		// 检查是否超过最大允许的 部分 数量
		if (this.maxParts > 0 && count > this.maxParts) {
			// 触发错误事件，提示 部分 过多
			emitError(new DecodingException("Too many parts (" + count + "/" + this.maxParts + " allowed)"));
			// 返回true，表示 部分 过多
			return true;
		} else {
			// 返回false，表示 部分 数量在允许范围内
			return false;
		}
	}

	private static boolean isFormField(HttpHeaders headers) {
		// 获取请求头中的内容类型
		MediaType contentType = headers.getContentType();

		// 如果内容类型为 null，或者是文本类型（text/plain），且内容头中不包含文件名
		return (contentType == null || MediaType.TEXT_PLAIN.equalsTypeAndSubtype(contentType))
				&& headers.getContentDisposition().getFilename() == null;
	}

	/**
	 * 表示 {@link PartGenerator} 内部用于创建单个 {@link Part} 的状态。
	 * {@link State} 实例是有状态的，并且在接受新的 {@link MultipartParser.HeadersToken} 时创建
	 * （参见 {@link #newPart(State, HttpHeaders)}）。
	 * 创建者的状态由以下规则确定：
	 * <ol>
	 * <li>如果 part 是 {@linkplain #isFormField(HttpHeaders) 表单字段}，创建者将处于 {@link FormFieldState}。</li>
	 * <li>如果启用了 {@linkplain #streaming}，创建者将处于 {@link StreamingState}。</li>
	 * <li>否则，创建者最初将处于 {@link InMemoryState}，但当 part 字节计数超过 {@link #maxInMemorySize} 时，
	 * 将切换到 {@link CreateFileState}，然后切换到 {@link WritingFileState}（用于写入内存内容），
	 * 最后切换到 {@link IdleFileState}，当有更多的 body 数据进入时，会再次切换到 {@link WritingFileState}。</li>
	 * </ol>
	 */
	private interface State {

		/**
		 * 当接收到 {@link MultipartParser.BodyToken} 时调用。
		 */
		void body(DataBuffer dataBuffer);

		/**
		 * 当所有 部分 的 令牌 都已接收时调用。
		 *
		 * @param finalPart {@code true} 表示这是最后一个 part（应调用 {@link #emitComplete()}）；{@code false} 表示否则
		 */
		void partComplete(boolean finalPart);

		/**
		 * 当接收到错误时调用。
		 */
		default void error(Throwable throwable) {
		}

		/**
		 * 清理任何状态。
		 */
		default void dispose() {
		}
	}


	/**
	 * 创建者的初始状态。对于 {@link #body(DataBuffer)}，抛出异常。
	 */
	private final class InitialState implements State {

		private InitialState() {
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			// 释放 数据缓冲区
			DataBufferUtils.release(dataBuffer);
			// 发送错误信号，并抛出非法状态异常
			emitError(new IllegalStateException("Body token not expected"));
		}

		@Override
		public void partComplete(boolean finalPart) {
			if (finalPart) {
				// 如果是最后一个 part，则发出完成信号
				emitComplete();
			}
		}

		@Override
		public String toString() {
			return "INITIAL";
		}
	}


	/**
	 * 创建者状态，在接收到 {@linkplain #isFormField(HttpHeaders) 表单字段} 时处于此状态。
	 * 将所有的数据缓存在内存中（直到 {@link #maxInMemorySize}）。
	 */
	private final class FormFieldState implements State {
		/**
		 * 存储 值 的快速字节数组输出流
		 */
		private final FastByteArrayOutputStream value = new FastByteArrayOutputStream();

		/**
		 * Http头部
		 */
		private final HttpHeaders headers;

		public FormFieldState(HttpHeaders headers) {
			this.headers = headers;
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			// 计算当前值的大小，包括已存储的值和当前数据缓冲区的可读字节数
			int size = this.value.size() + dataBuffer.readableByteCount();

			// 如果 PartGenerator 的没有内存大小限制，或者当前大小小于最大内存大小限制
			if (PartGenerator.this.maxInMemorySize == -1 || size < PartGenerator.this.maxInMemorySize) {
				// 存储当前数据缓冲区的内容
				store(dataBuffer);
				// 请求下一个令牌
				requestToken();
			} else {
				// 如果超过了内存使用限制，则释放当前数据缓冲区
				DataBufferUtils.release(dataBuffer);
				// 发出数据缓冲区限制异常
				emitError(new DataBufferLimitException("Form field value exceeded the memory usage limit of " +
						PartGenerator.this.maxInMemorySize + " bytes"));
			}
		}

		private void store(DataBuffer dataBuffer) {
			try {
				// 读取数据缓冲区中的可读字节数组
				byte[] bytes = new byte[dataBuffer.readableByteCount()];
				dataBuffer.read(bytes);
				// 将读取的字节数组写入到 快速字节数组输出流 中
				this.value.write(bytes);
			} catch (IOException ex) {
				// 发生 IO异常 时，发出异常
				emitError(ex);
			} finally {
				// 无论是否发生异常，都释放数据缓冲区
				DataBufferUtils.release(dataBuffer);
			}
		}

		@Override
		public void partComplete(boolean finalPart) {
			// 将值转换为字节数组
			byte[] bytes = this.value.toByteArrayUnsafe();
			// 将 字节数组 根据 头部的字符集 转为字符串
			String value = new String(bytes, MultipartUtils.charset(this.headers));
			// 发出表单字段部分
			emitPart(DefaultParts.formFieldPart(this.headers, value));
			if (finalPart) {
				// 如果是最后一个部分，发出完成信号
				emitComplete();
			}
		}

		@Override
		public String toString() {
			return "FORM-FIELD";
		}

	}


	/**
	 * 创建者状态，当 {@link #streaming} 为 {@code true} 时（并且不处理表单字段）处于此状态。
	 * 将所有接收到的数据缓存到一个 sink 中。
	 */
	private final class StreamingState implements State {
		/**
		 * 主体Sink
		 */
		private final FluxSink<DataBuffer> bodySink;

		public StreamingState(FluxSink<DataBuffer> bodySink) {
			this.bodySink = bodySink;
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			if (!this.bodySink.isCancelled()) {
				// 如果 主体 sink 没有被取消，则向其发送数据缓冲区
				this.bodySink.next(dataBuffer);
				// 如果下游请求了数据，则请求一个新的 令牌
				if (this.bodySink.requestedFromDownstream() > 0) {
					requestToken();
				}
			} else {
				// 如果 主体 sink 已取消，则释放数据缓冲区
				DataBufferUtils.release(dataBuffer);
				// 即使 主体 sink 已取消，（外部）部分 sink 可能未取消，因此请求另一个 令牌
				requestToken();
			}
		}

		@Override
		public void partComplete(boolean finalPart) {
			if (!this.bodySink.isCancelled()) {
				// 如果 主体 sink 没有被取消，则完成它
				this.bodySink.complete();
			}

			if (finalPart) {
				// 如果是最后一个 部分，则发送完成信号
				emitComplete();
			}
		}

		@Override
		public void error(Throwable throwable) {
			if (!this.bodySink.isCancelled()) {
				// 如果 主体 sink 没有被取消，则向其发送错误信息
				this.bodySink.error(throwable);
			}
		}

		@Override
		public String toString() {
			return "STREAMING";
		}

	}


	/**
	 * 创建者状态，当 {@link #streaming} 为 {@code false} 时（并且不处理表单字段）处于此状态。
	 * 将所有接收到的数据缓存在队列中。
	 * 如果字节计数超过 {@link #maxInMemorySize}，则创建者状态将切换到 {@link CreateFileState}，
	 * 然后最终切换到 {@link CreateFileState}。
	 */
	private final class InMemoryState implements State {

		/**
		 * 字节计数
		 */
		private final AtomicLong byteCount = new AtomicLong();

		/**
		 * 用于存储内容的并发链表队列
		 */
		private final Queue<DataBuffer> content = new ConcurrentLinkedQueue<>();

		/**
		 * HTTP头部信息
		 */
		private final HttpHeaders headers;

		/**
		 * 在释放资源时，是否释放相关资源
		 */
		private volatile boolean releaseOnDispose = true;


		public InMemoryState(HttpHeaders headers) {
			this.headers = headers;
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			// 获取之前的 字节计数
			long prevCount = this.byteCount.get();

			// 将 数据缓冲区 中的可读字节数加到 字节计数 中，并获取当前累计值
			long count = this.byteCount.addAndGet(dataBuffer.readableByteCount());

			// 如果未设置最大内存大小限制，或者当前累计值小于等于最大限制值
			if (PartGenerator.this.maxInMemorySize == -1 ||
					count <= PartGenerator.this.maxInMemorySize) {
				// 将 数据缓冲区 存储起来
				storeBuffer(dataBuffer);
			} else if (prevCount <= PartGenerator.this.maxInMemorySize) {
				// 如果之前的累计值小于等于最大限制值，切换到文件存储方式
				switchToFile(dataBuffer, count);
			} else {
				// 如果超过了最大内存大小限制，则释放 数据缓冲区
				DataBufferUtils.release(dataBuffer);
				// 发送错误信号，并抛出非法状态异常
				emitError(new IllegalStateException("Body token not expected"));
			}
		}

		private void storeBuffer(DataBuffer dataBuffer) {
			// 将数据缓冲区添加到 内容队列
			this.content.add(dataBuffer);
			// 请求令牌
			requestToken();
		}

		private void switchToFile(DataBuffer current, long byteCount) {
			// 创建一个 内容列表
			List<DataBuffer> content = new ArrayList<>(this.content);
			// 将 当前的数据缓冲区 添加到 内容列表
			content.add(current);

			//在释放资源时，不释放相关资源
			this.releaseOnDispose = false;

			// 创建一个新的 创建文件状态 实例
			CreateFileState newState = new CreateFileState(this.headers, content, byteCount);

			// 如果状态成功更改为 新状态
			if (changeState(this, newState)) {
				// 调用 新状态 的 创建文件 方法
				newState.createFile();
			} else {
				// 如果状态更改失败，则释放 内容列表 中的所有数据缓冲区
				content.forEach(DataBufferUtils::release);
			}
		}

		@Override
		public void partComplete(boolean finalPart) {
			// 发出一个内存部分
			emitMemoryPart();

			// 如果是最后一个部分
			if (finalPart) {
				// 发出完成信号
				emitComplete();
			}
		}

		private void emitMemoryPart() {
			// 创建一个字节数组
			byte[] bytes = new byte[(int) this.byteCount.get()];

			int idx = 0;

			// 遍历 内容队列 中的每个 DataBuffer
			for (DataBuffer buffer : this.content) {
				// 获取当前 缓冲区 中可读的字节数
				int len = buffer.readableByteCount();

				// 从 缓冲区 中读取数据到 字节数组 数组中的指定位置
				buffer.read(bytes, idx, len);

				// 更新索引
				idx += len;

				// 释放当前 buffer
				DataBufferUtils.release(buffer);
			}

			// 清空 内容队列 集合，释放所有 DataBuffer
			this.content.clear();

			// 使用 字节数组 创建一个 Flux<DataBuffer>
			Flux<DataBuffer> content = Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));

			// 发出一个新的部分，使用给定的 Http头部 和 内容Flux
			emitPart(DefaultParts.part(this.headers, content));
		}

		@Override
		public void dispose() {
			// 如果在释放资源时，释放相关资源。
			if (this.releaseOnDispose) {
				// 遍历内容列表中的每一个数据缓冲区
				// 释放数据缓冲区
				this.content.forEach(DataBufferUtils::release);
			}
		}

		@Override
		public String toString() {
			return "IN-MEMORY";
		}

	}


	/**
	 * 当等待临时文件创建时处于的创建者状态。
	 * 当字节计数超过 {@link #maxInMemorySize} 时，{@link InMemoryState} 最初切换到此状态，
	 * 然后调用 {@link #createFile()} 切换到 {@link WritingFileState}。
	 */
	private final class CreateFileState implements State {

		/**
		 * HTTP头部信息
		 */
		private final HttpHeaders headers;

		/**
		 * 存储请求或响应的内容的数据缓冲区的集合
		 */
		private final Collection<DataBuffer> content;

		/**
		 * 字节计数
		 */
		private final long byteCount;

		/**
		 * 状态是否已完成
		 */
		private volatile boolean completed;

		/**
		 * 是否是最终部分数据
		 */
		private volatile boolean finalPart;

		/**
		 * 在释放资源时，是否释放相关资源。
		 */
		private volatile boolean releaseOnDispose = true;


		public CreateFileState(HttpHeaders headers, Collection<DataBuffer> content, long byteCount) {
			this.headers = headers;
			this.content = content;
			this.byteCount = byteCount;
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			// 释放 数据缓冲区
			DataBufferUtils.release(dataBuffer);
			// 发送错误信号，并抛出非法状态异常
			emitError(new IllegalStateException("Body token not expected"));
		}

		@Override
		public void partComplete(boolean finalPart) {
			this.completed = true;
			this.finalPart = finalPart;
		}

		public void createFile() {
			PartGenerator.this.fileStorageDirectory
					.map(this::createFileState)
					.subscribeOn(PartGenerator.this.blockingOperationScheduler)
					.subscribe(this::fileCreated, PartGenerator.this::emitError);
		}

		private WritingFileState createFileState(Path directory) {
			try {
				// 在指定目录中创建临时文件
				Path tempFile = Files.createTempFile(directory, null, ".multipart");

				// 如果日志跟踪已启用，记录临时文件的存储位置
				if (logger.isTraceEnabled()) {
					logger.trace("Storing multipart data in file " + tempFile);
				}

				// 打开临时文件的可写通道
				WritableByteChannel channel = Files.newByteChannel(tempFile, StandardOpenOption.WRITE);

				// 返回一个新的 WritingFileState 实例
				return new WritingFileState(this, tempFile, channel);
			} catch (IOException ex) {
				// 如果创建临时文件过程中发生 IO异常，则抛出 未检查的IO异常
				throw new UncheckedIOException("Could not create temp file in " + directory, ex);
			}
		}

		private void fileCreated(WritingFileState newState) {
			// 设置在释放时不释放资源
			this.releaseOnDispose = false;

			//  如果状态成功更改为新状态
			if (changeState(this, newState)) {
				//则使用 新状态 写入 内容列表 中的所有数据缓冲区
				newState.writeBuffers(this.content);

				// 如果操作已完成，使用 最后部分 将 新状态 的设为部分完成状态
				if (this.completed) {
					newState.partComplete(this.finalPart);
				}
			} else {
				// 如果状态更改失败，则关闭 新状态 的通道
				MultipartUtils.closeChannel(newState.channel);
				// 释放 内容列表 中的所有数据缓冲区
				this.content.forEach(DataBufferUtils::release);
			}
		}

		@Override
		public void dispose() {
			// 如果在释放资源时，释放相关资源。
			if (this.releaseOnDispose) {
				// 遍历内容列表中的每一个数据缓冲区
				// 释放数据缓冲区
				this.content.forEach(DataBufferUtils::release);
			}
		}

		@Override
		public String toString() {
			return "CREATE-FILE";
		}


	}

	private final class IdleFileState implements State {

		/**
		 * HTTP头部信息
		 */
		private final HttpHeaders headers;

		/**
		 * 文件的路径
		 */
		private final Path file;

		/**
		 * 可写字节通道
		 */
		private final WritableByteChannel channel;

		/**
		 * 字节计数器
		 */
		private final AtomicLong byteCount;

		/**
		 * 是否在释放资源时关闭通道
		 */
		private volatile boolean closeOnDispose = true;


		public IdleFileState(WritingFileState state) {
			this.headers = state.headers;
			this.file = state.file;
			this.channel = state.channel;
			this.byteCount = state.byteCount;
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			// 将 数据缓冲区 中的可读字节数加到 字节计数器 中，并获取当前累计值
			long count = this.byteCount.addAndGet(dataBuffer.readableByteCount());

			// 如果没有设置每个部分的最大磁盘使用量限制，或者当前累计值小于等于最大限制值
			if (PartGenerator.this.maxDiskUsagePerPart == -1 || count <= PartGenerator.this.maxDiskUsagePerPart) {

				// 在释放资源时不关闭通道
				this.closeOnDispose = false;

				// 创建一个新的 WritingFileState 实例
				WritingFileState newState = new WritingFileState(this);

				// 如果成功更改为新的状态
				if (changeState(this, newState)) {
					// 使用 新状态 写入数据缓冲区
					newState.writeBuffer(dataBuffer);
				} else {
					// 如果状态更改失败，则关闭通道
					MultipartUtils.closeChannel(this.channel);
					// 释放数据缓冲区
					DataBufferUtils.release(dataBuffer);
				}
			} else {
				// 如果超过了最大磁盘使用量限制，则释放数据缓冲区
				DataBufferUtils.release(dataBuffer);
				// 抛出异常
				emitError(new DataBufferLimitException(
						"Part exceeded the disk usage limit of " + PartGenerator.this.maxDiskUsagePerPart +
								" bytes"));
			}
		}

		@Override
		public void partComplete(boolean finalPart) {
			// 关闭当前通道
			MultipartUtils.closeChannel(this.channel);

			// 发出一个新的部分
			emitPart(DefaultParts.part(this.headers, this.file, PartGenerator.this.blockingOperationScheduler));

			// 如果是最后一个部分
			if (finalPart) {
				// 发出完成信号
				emitComplete();
			}
		}

		@Override
		public void dispose() {
			// 如果在释放资源时关闭通道
			if (this.closeOnDispose) {
				// 关闭可写字节通道
				MultipartUtils.closeChannel(this.channel);
			}
		}


		@Override
		public String toString() {
			return "IDLE-FILE";
		}

	}

	private final class WritingFileState implements State {


		/**
		 * HTTP 头信息。
		 */
		private final HttpHeaders headers;

		/**
		 * 文件路径。
		 */
		private final Path file;

		/**
		 * 可写字节通道。
		 */
		private final WritableByteChannel channel;

		/**
		 * 字节计数。
		 */
		private final AtomicLong byteCount;

		/**
		 * 表示部分是否已完成处理的标志。
		 */
		private volatile boolean completed;

		/**
		 * 表示部分是否为最后一个部分的标志。
		 */
		private volatile boolean finalPart;


		public WritingFileState(CreateFileState state, Path file, WritableByteChannel channel) {
			this.headers = state.headers;
			this.file = file;
			this.channel = channel;
			this.byteCount = new AtomicLong(state.byteCount);
		}

		public WritingFileState(IdleFileState state) {
			this.headers = state.headers;
			this.file = state.file;
			this.channel = state.channel;
			this.byteCount = state.byteCount;
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			// 释放 数据缓冲区
			DataBufferUtils.release(dataBuffer);
			// 发送错误信号，并抛出非法状态异常
			emitError(new IllegalStateException("Body token not expected"));
		}

		@Override
		public void partComplete(boolean finalPart) {
			this.completed = true;
			this.finalPart = finalPart;
		}

		public void writeBuffer(DataBuffer dataBuffer) {
			// 创建一个包含指定数据的 Mono
			Mono.just(dataBuffer)
					// 调用 写内部 方法处理数据缓冲区
					.flatMap(this::writeInternal)
					// 在指定的调度器上执行订阅操作
					.subscribeOn(PartGenerator.this.blockingOperationScheduler)
					// 不设置消费函数
					.subscribe(null,
							// 将 PartGenerator 的 发送错误 方法作为错误处理方法
							PartGenerator.this::emitError,
							// 完成事件处理器为当前类的 写入完成 方法
							this::writeComplete);
		}

		public void writeBuffers(Iterable<DataBuffer> dataBuffers) {
			// 依次处理数据缓冲区中的每个元素，并按顺序串联处理结果
			Flux.fromIterable(dataBuffers)
					// 将每个元素映射到 写内部 方法进行处理
					.concatMap(this::writeInternal)
					// 返回一个表示序列完成的 Mono
					.then()
					// 在 阻塞操作的调度器 上执行订阅操作
					.subscribeOn(PartGenerator.this.blockingOperationScheduler)
					// 订阅，不处理 onNext 事件，处理错误和完成事件
					.subscribe(null,
							PartGenerator.this::emitError,
							this::writeComplete);
		}

		private void writeComplete() {
			// 创建一个新的 空闲文件状态 实例
			IdleFileState newState = new IdleFileState(this);

			// 如果当前操作已完成
			if (this.completed) {
				// 使用 最后部分，标记 新状态 的部分完成状态
				newState.partComplete(this.finalPart);
			} else if (changeState(this, newState)) {
				// 如果成功改变为新状态，请求令牌
				requestToken();
			} else {
				// 如果状态更改失败，则关闭通道
				MultipartUtils.closeChannel(this.channel);
			}
		}

		@SuppressWarnings("BlockingMethodInNonBlockingContext")
		private Mono<Void> writeInternal(DataBuffer dataBuffer) {
			try {
				// 将 DataBuffer 转换为 ByteBuffer
				ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
				// 循环写入 ByteBuffer 中剩余的数据到通道中
				while (byteBuffer.hasRemaining()) {
					this.channel.write(byteBuffer);
				}
				// 返回一个表示空结果的 Mono
				return Mono.empty();
			} catch (IOException ex) {
				// 如果发生 IO异常，则返回一个包含异常的 Mono
				return Mono.error(ex);
			} finally {
				// 释放 DataBuffer
				DataBufferUtils.release(dataBuffer);
			}
		}

		@Override
		public String toString() {
			return "WRITE-FILE";
		}
	}


	private static final class DisposedState implements State {
		/**
		 * 已释放状态实例
		 */
		public static final DisposedState INSTANCE = new DisposedState();

		private DisposedState() {
		}

		@Override
		public void body(DataBuffer dataBuffer) {
			DataBufferUtils.release(dataBuffer);
		}

		@Override
		public void partComplete(boolean finalPart) {
		}

		@Override
		public String toString() {
			return "DISPOSED";
		}

	}

}
