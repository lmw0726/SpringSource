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
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.util.context.Context;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 订阅数据缓冲流，并生成 {@link Token} 实例的 Flux。
 *
 * <p>该类用于解析多部分数据流，将 {@link DataBuffer} 对象流转换为 {@link Token} 对象流。
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
final class MultipartParser extends BaseSubscriber<DataBuffer> {

	/**
	 * 回车符 '\r' 的字节表示
	 */
	private static final byte CR = '\r';

	/**
	 * 换行符 '\n' 的字节表示
	 */
	private static final byte LF = '\n';

	/**
	 * 表示回车换行组合的字节数组
	 */
	private static final byte[] CR_LF = {CR, LF};

	/**
	 * 破折号 '-' 的字节表示
	 */
	private static final byte HYPHEN = '-';

	/**
	 * 表示双破折号组合的字节数组
	 */
	private static final byte[] TWO_HYPHENS = {HYPHEN, HYPHEN};

	/**
	 * 头部条目之间的分隔符字符串
	 */
	private static final String HEADER_ENTRY_SEPARATOR = "\\r\\n";

	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(MultipartParser.class);

	/**
	 * 当前解析器的状态引用
	 */
	private final AtomicReference<State> state;

	/**
	 * FluxSink，用于向下游发送 Token 对象
	 */
	private final FluxSink<Token> sink;

	/**
	 * 边界字节数组
	 */
	private final byte[] boundary;

	/**
	 * 最大允许的头部大小
	 */
	private final int maxHeadersSize;

	/**
	 * 请求是否未完成
	 */
	private final AtomicBoolean requestOutstanding = new AtomicBoolean();

	/**
	 * 头部字符集
	 */
	private final Charset headersCharset;


	/**
	 * 构造方法，初始化解析器。
	 *
	 * @param sink           数据流的 sink，用于发送解析后的 Token
	 * @param boundary       多部分内容的边界标识，从 {@code Content-Type} 头部中获取
	 * @param maxHeadersSize 最大的缓存头部大小
	 * @param headersCharset 用于解码头部的字符集
	 */
	private MultipartParser(FluxSink<Token> sink, byte[] boundary, int maxHeadersSize, Charset headersCharset) {
		this.sink = sink;
		this.boundary = boundary;
		this.maxHeadersSize = maxHeadersSize;
		this.headersCharset = headersCharset;
		this.state = new AtomicReference<>(new PreambleState());
	}

	/**
	 * 解析给定的 {@link DataBuffer} 流为 {@link Token} 对象流。
	 *
	 * @param buffers        输入的数据缓冲区流
	 * @param boundary       多部分数据的边界标识，从 {@code Content-Type} 头部中获取
	 * @param maxHeadersSize 最大缓存的头部大小
	 * @param headersCharset 用于解码头部的字符集
	 * @return 解析后的 Token 流
	 */
	public static Flux<Token> parse(Flux<DataBuffer> buffers, byte[] boundary, int maxHeadersSize,
									Charset headersCharset) {
		// 创建并返回一个 Flux
		return Flux.create(sink -> {
			// 创建 MultipartParser 实例
			MultipartParser parser = new MultipartParser(sink, boundary, maxHeadersSize, headersCharset);

			// 当订阅取消时，设置 多部分解析器 的 取消方法 回调
			sink.onCancel(parser::onSinkCancel);

			// 当请求数据时，调用 多部分解析器 的 请求缓冲 方法
			sink.onRequest(n -> parser.requestBuffer());

			// 订阅 数据缓冲区流，并将数据传递给 多部分解析器 处理
			buffers.subscribe(parser);
		});
	}

	@Override
	public Context currentContext() {
		return Context.of(this.sink.contextView());
	}

	@Override
	protected void hookOnSubscribe(Subscription subscription) {
		requestBuffer();
	}

	@Override
	protected void hookOnNext(DataBuffer value) {
		// 将请求设置为未完成
		this.requestOutstanding.set(false);
		// 处理下一个 数据值
		this.state.get().onNext(value);
	}

	@Override
	protected void hookOnComplete() {
		this.state.get().onComplete();
	}

	@Override
	protected void hookOnError(Throwable throwable) {
		// 将状态设置为 已处理 状态，并获取旧状态值
		State oldState = this.state.getAndSet(DisposedState.INSTANCE);
		// 处理旧状态
		oldState.dispose();
		// 处理错误
		this.sink.error(throwable);
	}

	private void onSinkCancel() {
		// 将状态设置为 已处理 状态，并获取旧状态值
		State oldState = this.state.getAndSet(DisposedState.INSTANCE);
		// 处理旧状态
		oldState.dispose();
		// 取消请求
		cancel();
	}

	boolean changeState(State oldState, State newState, @Nullable DataBuffer remainder) {
		// 如果成功将旧状态更改为新状态
		if (this.state.compareAndSet(oldState, newState)) {
			// 如果日志跟踪已启用，记录状态更改信息
			if (logger.isTraceEnabled()) {
				logger.trace("Changed state: " + oldState + " -> " + newState);
			}

			// 处理旧状态
			oldState.dispose();

			// 如果 剩余数据 不为 null
			if (remainder != null) {
				// 如果 剩余数据 中有可读字节
				if (remainder.readableByteCount() > 0) {
					// 将 剩余数据 传递给 新状态 的 onNext 方法处理
					newState.onNext(remainder);
				} else {
					// 释放 剩余数据缓冲区
					DataBufferUtils.release(remainder);
					// 请求下一个数据缓冲区
					requestBuffer();
				}
			}

			// 返回状态更改成功
			return true;
		} else {
			// 如果状态更改失败，则释放 剩余数据缓冲区
			DataBufferUtils.release(remainder);
			// 返回失败
			return false;
		}
	}

	void emitHeaders(HttpHeaders headers) {
		// 如果日志跟踪已启用，记录发出的头信息
		if (logger.isTraceEnabled()) {
			logger.trace("Emitting headers: " + headers);
		}

		// 向 sink 发出一个包含给定的 头部信息 的HeadersToken
		this.sink.next(new HeadersToken(headers));
	}

	void emitBody(DataBuffer buffer) {
		// 如果日志跟踪已启用，记录发出的正文信息
		if (logger.isTraceEnabled()) {
			logger.trace("Emitting body: " + buffer);
		}

		// 向 sink 发出一个包含给定的 缓冲区 的 BodyToken
		this.sink.next(new BodyToken(buffer));
	}

	void emitError(Throwable t) {
		// 取消操作
		cancel();
		// 向 sink 发送一个错误信号
		this.sink.error(t);
	}

	void emitComplete() {
		// 取消操作
		cancel();
		// 向 sink 发送一个完成信号
		this.sink.complete();
	}

	private void requestBuffer() {
		// 如果上游数据源不为 null
		if (upstream() != null &&
				// 并且接收数据的 sink 没有被取消
				!this.sink.isCancelled() &&
				// 并且下游请求的数据量大于 0
				this.sink.requestedFromDownstream() > 0 &&
				// 并且成功将 请求是否未完成 标志从 false 设置为 true
				this.requestOutstanding.compareAndSet(false, true)) {
			// 则请求获取一个数据单元
			request(1);
		}
	}


	/**
	 * {@link #parse(Flux, byte[], int, Charset)} 方法的输出表示。
	 */
	public abstract static class Token {

		/**
		 * 获取 Token 对应的 HTTP 头部信息。
		 *
		 * @return Token 的 HTTP 头部信息
		 */
		public abstract HttpHeaders headers();

		/**
		 * 获取 Token 对应的数据缓冲区。
		 *
		 * @return Token 的数据缓冲区
		 */
		public abstract DataBuffer buffer();
	}


	/**
	 * 表示包含 {@link HttpHeaders} 的令牌。
	 */
	public final static class HeadersToken extends Token {
		/**
		 * Http头部信息
		 */
		private final HttpHeaders headers;

		/**
		 * 使用给定的 HttpHeaders 初始化 HeadersToken 对象。
		 *
		 * @param headers 要包含的 HttpHeaders 对象
		 */
		public HeadersToken(HttpHeaders headers) {
			this.headers = headers;
		}

		/**
		 * 返回此令牌的 HttpHeaders 对象。
		 *
		 * @return 此令牌的 HttpHeaders 对象
		 */
		@Override
		public HttpHeaders headers() {
			return this.headers;
		}

		/**
		 * 由于此令牌代表仅具有 HttpHeaders 的类型，因此不支持返回数据缓冲区。
		 *
		 * @throws IllegalStateException 始终抛出 IllegalStateException
		 */
		@Override
		public DataBuffer buffer() {
			throw new IllegalStateException();
		}
	}


	/**
	 * 表示包含 {@link DataBuffer} 的令牌。
	 */
	public final static class BodyToken extends Token {
		/**
		 * 存放 主体 的数据缓冲区
		 */
		private final DataBuffer buffer;

		/**
		 * 使用给定的 DataBuffer 初始化 BodyToken 对象。
		 *
		 * @param buffer 要包含的 DataBuffer 对象
		 */
		public BodyToken(DataBuffer buffer) {
			this.buffer = buffer;
		}

		/**
		 * 由于此令牌代表仅具有 DataBuffer 的类型，因此不支持返回 HttpHeaders。
		 *
		 * @throws IllegalStateException 始终抛出 IllegalStateException
		 */
		@Override
		public HttpHeaders headers() {
			throw new IllegalStateException();
		}

		/**
		 * 返回此令牌的 DataBuffer 对象。
		 *
		 * @return 此令牌的 DataBuffer 对象
		 */
		@Override
		public DataBuffer buffer() {
			return this.buffer;
		}
	}


	/**
	 * 表示 {@link MultipartParser} 的内部状态。
	 * 对于格式良好的多部分消息，流程如下所示：
	 * <p><pre>
	 *        序言
	 *         |
	 *         v
	 *  +--> 头部--->已处理
	 *  |      |
	 *  |      v
	 *  +----主体
	 *  </pre>
	 * 对于格式不正确的消息，流程会直接结束在 DISPOSED 状态，
	 * 同时当 sink 被 {@linkplain #onSinkCancel() 取消} 时，也会进入 DISPOSED 状态。
	 */
	private interface State {

		/**
		 * 处理下一个数据缓冲区。
		 *
		 * @param buf 输入的数据缓冲区
		 */
		void onNext(DataBuffer buf);

		/**
		 * 当完成所有数据处理时调用的方法。
		 */
		void onComplete();

		/**
		 * 默认方法，用于释放资源。
		 */
		default void dispose() {
		}
	}


	/**
	 * 解析器的初始状态。寻找多部分消息的第一个边界。
	 * 需要注意的是，第一个边界不一定以 {@code CR LF} 开头；只需要前缀 {@code --}。
	 */
	private final class PreambleState implements State {
		/**
		 * 第一个边界值匹配器
		 */
		private final DataBufferUtils.Matcher firstBoundary;

		/**
		 * 构造方法，初始化第一个边界的匹配器。
		 */
		public PreambleState() {
			this.firstBoundary = DataBufferUtils.matcher(
					MultipartUtils.concat(TWO_HYPHENS, MultipartParser.this.boundary));
		}

		/**
		 * 在给定的缓冲区中查找第一个边界。如果找到，切换状态到 {@link HeadersState}，
		 * 并传递缓冲区中剩余的部分。
		 *
		 * @param buf 输入的数据缓冲区
		 */
		@Override
		public void onNext(DataBuffer buf) {
			// 匹配第一个边界值，并获取结束索引
			int endIdx = this.firstBoundary.match(buf);
			if (endIdx != -1) {
				// 如果找到第一个边界位置
				if (logger.isTraceEnabled()) {
					// 如果日志跟踪被启用
					logger.trace("First boundary found @" + endIdx + " in " + buf);
				}
				// 从 数据缓冲区 中切片出头部数据缓冲区
				DataBuffer headersBuf = MultipartUtils.sliceFrom(buf, endIdx);
				// 释放原始缓冲区
				DataBufferUtils.release(buf);

				// 改变状态为 标题状态，并传入头部数据缓冲区
				changeState(this, new HeadersState(), headersBuf);
			} else {
				// 如果未找到第一个边界位置，则释放缓冲区
				DataBufferUtils.release(buf);
				// 请求缓冲区数据
				requestBuffer();
			}
		}

		/**
		 * 当完成所有数据处理时调用的方法。
		 * 如果无法找到第一个边界，则切换状态到 {@link DisposedState} 并抛出解码异常。
		 */
		@Override
		public void onComplete() {
			// 如果成功将状态改变为 已处理状态
			if (changeState(this, DisposedState.INSTANCE, null)) {
				// 发出解码异常，表示未能找到第一个边界
				emitError(new DecodingException("Could not find first boundary"));
			}
		}

		/**
		 * 返回对象的字符串表示，表示为 "PREAMBLE"。
		 *
		 * @return 对象的字符串表示
		 */
		@Override
		public String toString() {
			return "PREAMBLE";
		}

	}


	/**
	 * 解析器处理多部分消息头部的状态。将头部缓冲区解析为 {@link HttpHeaders} 实例，
	 * 确保总大小不超过 {@link #maxHeadersSize}。
	 */
	private final class HeadersState implements State {
		/**
		 * 结束头部匹配器
		 */
		private final DataBufferUtils.Matcher endHeaders = DataBufferUtils.matcher(MultipartUtils.concat(CR_LF, CR_LF));

		/**
		 * 字节计数
		 */
		private final AtomicInteger byteCount = new AtomicInteger();

		/**
		 * 缓冲区列表
		 */
		private final List<DataBuffer> buffers = new ArrayList<>();


		/**
		 * 首先检查此状态所处的多部分边界是否为最终边界。然后在给定的缓冲区中查找头部-主体边界
		 * （{@code CR LF CR LF}）。如果找到，检查所有头部缓冲区的大小是否不超过 {@link #maxHeadersSize}，
		 * 将收集到的所有缓冲区转换为 {@link HttpHeaders} 对象，并切换到 {@link BodyState} 状态，
		 * 并传递缓冲区中剩余的部分。如果未找到边界，则收集缓冲区，如果大小不超过 {@link #maxHeadersSize}。
		 *
		 * @param buf 输入的数据缓冲区
		 */
		@Override
		public void onNext(DataBuffer buf) {
			if (isLastBoundary(buf)) {
				// 如果 缓存区 是最后一个边界
				if (logger.isTraceEnabled()) {
					// 如果日志跟踪被启用，记录追踪日志
					logger.trace("Last boundary found in " + buf);
				}

				// 如果成功将状态改变为 已处理状态
				if (changeState(this, DisposedState.INSTANCE, buf)) {
					// 如果成功改变状态
					// 发出完成信号
					emitComplete();
				}
				// 返回
				return;
			}

			// 查找结束标头位置
			int endIdx = this.endHeaders.match(buf);
			if (endIdx != -1) {
				// 如果找到结束标头位置
				if (logger.isTraceEnabled()) {
					// 如果日志跟踪被启用
					logger.trace("End of headers found @" + endIdx + " in " + buf);
				}

				// 增加字节计数
				long count = this.byteCount.addAndGet(endIdx);
				if (belowMaxHeaderSize(count)) {
					// 如果字节计数在最大标头大小以下
					// 切片出标头数据缓冲区
					DataBuffer headerBuf = MultipartUtils.sliceTo(buf, endIdx);
					// 添加到缓冲区列表中
					this.buffers.add(headerBuf);
					// 切片出主体数据缓冲区
					DataBuffer bodyBuf = MultipartUtils.sliceFrom(buf, endIdx);
					// 释放原始缓冲区
					DataBufferUtils.release(buf);

					// 发出解析后的标头
					emitHeaders(parseHeaders());
					// 改变状态为 主体状态，并传入主体数据缓冲区
					changeState(this, new BodyState(), bodyBuf);
				}
			} else {
				// 如果未找到结束标头位置
				// 增加字节计数
				long count = this.byteCount.addAndGet(buf.readableByteCount());
				if (belowMaxHeaderSize(count)) {
					// 将 buf 添加到缓冲区列表中
					this.buffers.add(buf);
					// 请求更多缓冲区数据
					requestBuffer();
				}
			}
		}

		/**
		 * 检查给定的缓冲区是否是最后一个边界。
		 *
		 * @param buf 输入的数据缓冲区
		 * @return 如果是最后一个边界则返回 true，否则返回 false
		 */
		private boolean isLastBoundary(DataBuffer buf) {
			// 检查是否当前缓冲区列表为空
			return (this.buffers.isEmpty() &&
					// 并且 缓冲区 中至少有2个可读字节
					buf.readableByteCount() >= 2 &&
					// 并且这两个字节都是连字符
					buf.getByte(0) == HYPHEN && buf.getByte(1) == HYPHEN)
					||
					// 或者检查当前缓冲区列表是否只有一个缓冲区
					(this.buffers.size() == 1 &&
							// 并且该缓冲区只有一个可读字节
							this.buffers.get(0).readableByteCount() == 1 &&
							// 且为连字符
							this.buffers.get(0).getByte(0) == HYPHEN &&
							// 并且 缓冲区 中至少有一个可读字节
							buf.readableByteCount() >= 1 &&
							// 且为连字符
							buf.getByte(0) == HYPHEN);
		}

		/**
		 * 检查给定的 {@code count} 是否低于或等于 {@link #maxHeadersSize}，
		 * 如果不是则抛出 {@link DataBufferLimitException} 异常。
		 *
		 * @param count 要检查的大小
		 * @return 如果大小在限制内返回 true，否则返回 false
		 */
		private boolean belowMaxHeaderSize(long count) {
			// 如果头部大小 小于等于 最大允许的头部大小
			if (count <= MultipartParser.this.maxHeadersSize) {
				// 返回true，表示头部大小在允许范围内
				return true;
			} else {
				// 如果头部大小超过了允许的最大限制，则抛出 数据缓冲区限制异常
				emitError(new DataBufferLimitException("Part headers exceeded the memory usage limit of " +
						MultipartParser.this.maxHeadersSize + " bytes"));
				// 返回false
				return false;
			}
		}

		/**
		 * 将缓冲区列表解析为 {@link HttpHeaders} 实例。
		 * 使用 ISO=8859-1 将连接的缓冲区转换为字符串，并解析该字符串为键值对。
		 *
		 * @return 解析后的 HttpHeaders 对象
		 */
		private HttpHeaders parseHeaders() {
			// 如果缓冲区列表为空，则返回空的HttpHeaders对象
			if (this.buffers.isEmpty()) {
				return HttpHeaders.EMPTY;
			}

			// 将所有缓冲区内容合并为一个DataBuffer对象
			DataBuffer joined = this.buffers.get(0).factory().join(this.buffers);

			// 清空缓冲区列表，释放资源
			this.buffers.clear();

			// 将合并后的DataBuffer转换为字符串
			String string = joined.toString(MultipartParser.this.headersCharset);
			// 释放合并后的DataBuffer对象
			DataBufferUtils.release(joined);

			// 根据指定的分隔符拆分字符串为行数组
			String[] lines = string.split(HEADER_ENTRY_SEPARATOR);

			// 创建一个空的HttpHeaders对象，用于存储解析后的HTTP头部信息
			HttpHeaders result = new HttpHeaders();

			// 遍历每一行头部信息
			for (String line : lines) {
				// 查找头部名和值的分隔符':'
				int idx = line.indexOf(':');
				if (idx != -1) {
					// 提取头部名和值，并去除值的前导空格
					String name = line.substring(0, idx);
					String value = line.substring(idx + 1);
					while (value.startsWith(" ")) {
						// 如果值以空格开头，则将其去除
						value = value.substring(1);
					}
					// 将解析后的头部信息添加到HttpHeaders对象中
					result.add(name, value);
				}
			}

			// 返回解析后的HttpHeaders对象
			return result;
		}

		/**
		 * 当完成所有数据处理时调用的方法。
		 * 如果无法找到头部结束标志，则切换状态到 {@link DisposedState} 并抛出解码异常。
		 */
		@Override
		public void onComplete() {
			// 如果成功将当前状态更改为 已处理
			if (changeState(this, DisposedState.INSTANCE, null)) {
				// 抛出 解码异常，表示未找到头部结束标记
				emitError(new DecodingException("Could not find end of headers"));
			}
		}

		/**
		 * 释放所有收集的缓冲区。
		 */
		@Override
		public void dispose() {
			this.buffers.forEach(DataBufferUtils::release);
		}

		/**
		 * 返回对象的字符串表示，表示为 "HEADERS"。
		 *
		 * @return 对象的字符串表示
		 */
		@Override
		public String toString() {
			return "HEADERS";
		}


	}


	/**
	 * 解析器处理多部分消息主体的状态。将数据缓冲区作为 {@link BodyToken} 发送，直到找到边界（或者说：{@code CR LF - - boundary}）。
	 */
	private final class BodyState implements State {

		/**
		 * 边界匹配器
		 */
		private final DataBufferUtils.Matcher boundary;

		/**
		 * 边界的长度
		 */
		private final int boundaryLength;

		/**
		 * 缓冲区队列，用于存储数据缓冲区
		 */
		private final Deque<DataBuffer> queue = new ConcurrentLinkedDeque<>();

		/**
		 * 创建一个 {@link BodyState} 实例。
		 * 使用给定的边界计算边界匹配器和边界长度。
		 */
		public BodyState() {
			byte[] delimiter = MultipartUtils.concat(CR_LF, TWO_HYPHENS, MultipartParser.this.boundary);
			this.boundary = DataBufferUtils.matcher(delimiter);
			this.boundaryLength = delimiter.length;
		}

		/**
		 * 检查在 {@code buffer} 中是否可以找到（或者是） {@code CR LF - - boundary} 的（结尾的） needle。
		 * 如果找到，needle 可能会溢出到前一个缓冲区，因此我们计算长度并相应地切片当前和前一个缓冲区。
		 * 然后切换到 {@link HeadersState} 状态，并传递 {@code buffer} 的剩余部分。
		 * 如果未找到 needle，则将 {@code buffer} 设置为前一个缓冲区。
		 *
		 * @param buffer 输入的数据缓冲区
		 */
		@Override
		public void onNext(DataBuffer buffer) {
			// 查找边界在当前缓冲区中的位置
			int endIdx = this.boundary.match(buffer);

			// 如果找到了边界位置
			if (endIdx != -1) {
				// 如果日志跟踪被启用，记录边界位置信息
				if (logger.isTraceEnabled()) {
					logger.trace("Boundary found @" + endIdx + " in " + buffer);
				}

				// 计算边界在缓冲区内的长度
				int len = endIdx - buffer.readPosition() - this.boundaryLength + 1;

				// 如果边界完全在当前缓冲区内
				if (len > 0) {
					// 切片出消息体部分
					DataBuffer body = buffer.retainedSlice(buffer.readPosition(), len);
					// 将其加入队列
					enqueue(body);
					// 刷新
					flush();
				} else if (len < 0) {
					// 如果边界跨越多个缓冲区，并且已找到结尾
					// 需要逆序迭代缓冲区
					DataBuffer prev;
					// 逆序遍历队列中的缓冲区
					while ((prev = this.queue.pollLast()) != null) {
						// 计算前一个缓冲区加上当前负长度后的有效长度
						int prevLen = prev.readableByteCount() + len;

						// 如果前一个缓冲区包含消息体部分
						if (prevLen > 0) {
							// 切片出前一个缓冲区的消息体部分
							DataBuffer body = prev.retainedSlice(prev.readPosition(), prevLen);
							// 释放前一个缓冲区
							DataBufferUtils.release(prev);
							// 加入队列
							enqueue(body);
							// 刷新
							flush();
							break;
						} else {
							// 如果前一个缓冲区仅包含边界字节，释放前一个缓冲区
							DataBufferUtils.release(prev);
							// 调整长度
							len += prev.readableByteCount();
						}
					}
				} else /* if (len == 0) */ {
					// 缓冲区以完整分隔符开头，刷新前一个缓冲区
					flush();
				}

				// 切片出剩余的数据部分
				DataBuffer remainder = MultipartUtils.sliceFrom(buffer, endIdx);
				// 释放当前缓冲区
				DataBufferUtils.release(buffer);

				// 更改解析器的状态为 标题状态，并传递剩余的数据部分
				changeState(this, new HeadersState(), remainder);
			} else {
				// 如果未找到边界位置，则将当前缓冲区加入队列
				enqueue(buffer);
				// 请求更多缓冲区
				requestBuffer();
			}
		}

		/**
		 * 存储给定的缓冲区。发出不能包含边界字节的缓冲区，通过反向迭代队列并求和缓冲区大小。
		 * 通过边界长度的第一个缓冲区和后续缓冲区来发出（以正确的非反向顺序）。
		 *
		 * @param buf 要存储的数据缓冲区
		 */
		private void enqueue(DataBuffer buf) {
			// 将缓冲区添加到队列中
			this.queue.add(buf);

			int len = 0;
			// 创建一个双端队列用于存储要发射的缓冲区
			Deque<DataBuffer> emit = new ArrayDeque<>();

			// 使用逆序迭代器遍历队列中的缓冲区
			for (Iterator<DataBuffer> iterator = this.queue.descendingIterator(); iterator.hasNext(); ) {
				// 获取前一个缓冲区
				DataBuffer previous = iterator.next();

				// 如果长度超过边界长度
				if (len > this.boundaryLength) {
					// 将缓冲区添加到 队列头部
					emit.addFirst(previous);
					// 并从原队列中移除
					iterator.remove();
				}

				// 累加当前缓冲区的可读字节数
				len += previous.readableByteCount();
			}

			// 遍历 队列，对每个缓冲区调用 发射主体 方法
			emit.forEach(MultipartParser.this::emitBody);
		}

		/**
		 * 将所有缓冲区刷新出队列并发出。
		 */
		private void flush() {
			// 遍历队列，对每个缓冲区调用 发射主体 方法
			this.queue.forEach(MultipartParser.this::emitBody);
			// 清空队列
			this.queue.clear();
		}

		/**
		 * 当完成所有数据处理时调用的方法。
		 * 如果无法找到主体结束标志，则切换状态到 {@link DisposedState} 并抛出解码异常。
		 */
		@Override
		public void onComplete() {
			// 如果成功将当前状态更改为 已处理
			if (changeState(this, DisposedState.INSTANCE, null)) {
				// 抛出 解码异常，表示未找到消息体的结束标记
				emitError(new DecodingException("Could not find end of body"));
			}
		}

		/**
		 * 释放所有收集的缓冲区。
		 */
		@Override
		public void dispose() {
			// 遍历队列，释放所有缓冲区
			this.queue.forEach(DataBufferUtils::release);
			// 清空队列
			this.queue.clear();
		}

		/**
		 * 返回对象的字符串表示，表示为 "BODY"。
		 *
		 * @return 对象的字符串表示
		 */
		@Override
		public String toString() {
			return "BODY";
		}
	}


	/**
	 * 解析器完成时的状态，可能是由于找到最终边界或解析消息格式错误。释放所有传入的缓冲区。
	 */
	private static final class DisposedState implements State {

		/**
		 * 已处理完成的状态实例
		 */
		public static final DisposedState INSTANCE = new DisposedState();

		/**
		 * 私有构造方法，确保只有一个实例。
		 */
		private DisposedState() {
		}

		/**
		 * 当接收到数据缓冲区时调用的方法，释放缓冲区资源。
		 *
		 * @param buf 接收到的数据缓冲区
		 */
		@Override
		public void onNext(DataBuffer buf) {
			DataBufferUtils.release(buf);
		}

		/**
		 * 当完成所有数据处理时调用的方法。
		 */
		@Override
		public void onComplete() {
		}

		/**
		 * 返回对象的字符串表示，表示为 "DISPOSED"。
		 *
		 * @return 对象的字符串表示
		 */
		@Override
		public String toString() {
			return "DISPOSED";
		}
	}


}
