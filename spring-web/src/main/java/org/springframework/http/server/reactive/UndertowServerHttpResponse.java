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

package org.springframework.http.server.reactive;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.xnio.channels.StreamSinkChannel;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 将 {@link ServerHttpResponse} 适配到 Undertow {@link HttpServerExchange}。
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 5.0
 */
class UndertowServerHttpResponse extends AbstractListenerServerHttpResponse implements ZeroCopyHttpOutputMessage {
	/**
	 * Http服务器交换
	 */
	private final HttpServerExchange exchange;

	/**
	 * Undertow服务器Http请求
	 */
	private final UndertowServerHttpRequest request;

	/**
	 * 响应通道
	 */
	@Nullable
	private StreamSinkChannel responseChannel;


	UndertowServerHttpResponse(
			HttpServerExchange exchange, DataBufferFactory bufferFactory, UndertowServerHttpRequest request) {

		super(bufferFactory, createHeaders(exchange));
		Assert.notNull(exchange, "HttpServerExchange must not be null");
		this.exchange = exchange;
		this.request = request;
	}

	private static HttpHeaders createHeaders(HttpServerExchange exchange) {
		// 使用 UndertowHeadersAdapter 适配交换的响应头
		UndertowHeadersAdapter headersMap = new UndertowHeadersAdapter(exchange.getResponseHeaders());
		// 使用适配后的头部创建HttpHeaders对象
		return new HttpHeaders(headersMap);
	}


	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeResponse() {
		return (T) this.exchange;
	}

	@Override
	public HttpStatus getStatusCode() {
		// 获取父类的状态码
		HttpStatus status = super.getStatusCode();
		// 如果父类的状态码不为空，则返回该状态码；
		// 否则解析交换的状态码并返回
		return (status != null ? status : HttpStatus.resolve(this.exchange.getStatusCode()));
	}

	@Override
	public Integer getRawStatusCode() {
		// 获取父类的原始状态码
		Integer status = super.getRawStatusCode();
		// 如果父类的状态码不为空，则返回该状态码；
		// 否则返回交换的状态码
		return (status != null ? status : this.exchange.getStatusCode());
	}

	@Override
	protected void applyStatusCode() {
		// 调用父类方法获取原始状态码
		Integer status = super.getRawStatusCode();

		// 如果状态码不为 null
		if (status != null) {
			// 将状态码设置到当前请求的交换对象中
			this.exchange.setStatusCode(status);
		}
	}

	@Override
	protected void applyHeaders() {
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void applyCookies() {
		// 遍历所有 Cookie 的名称
		for (String name : getCookies().keySet()) {
			// 遍历每个 Cookie 的响应对象
			for (ResponseCookie httpCookie : getCookies().get(name)) {
				// 创建一个 Undertow 的 Cookie 对象
				Cookie cookie = new CookieImpl(name, httpCookie.getValue());

				// 如果 Cookie 的过期时间不为负数
				if (!httpCookie.getMaxAge().isNegative()) {
					// 设置为 Cookie 的最大年龄（以秒为单位）
					cookie.setMaxAge((int) httpCookie.getMaxAge().getSeconds());
				}

				// 如果 Cookie 的域名不为 null
				if (httpCookie.getDomain() != null) {
					// 设置为 Cookie 的域名
					cookie.setDomain(httpCookie.getDomain());
				}

				// 如果 Cookie 的路径不为 null
				if (httpCookie.getPath() != null) {
					// 设置为 Cookie 的路径
					cookie.setPath(httpCookie.getPath());
				}

				// 设置 Cookie 的安全性
				cookie.setSecure(httpCookie.isSecure());

				// 设置 Cookie 是否为 HttpOnly
				cookie.setHttpOnly(httpCookie.isHttpOnly());

				// 设置 Cookie 的 SameSite 模式
				cookie.setSameSiteMode(httpCookie.getSameSite());

				// 将 Cookie 添加到响应的 Cookie 列表中
				// 在 Undertow 2.2 中，getResponseCookies() 已被弃用
				this.exchange.getResponseCookies().putIfAbsent(name, cookie);
			}
		}
	}

	@Override
	public Mono<Void> writeWith(Path file, long position, long count) {
		// 执行提交操作，该操作是一个异步操作，返回一个 Mono 对象
		return doCommit(() ->
				Mono.create(sink -> {
					try {
						// 打开文件通道以读取文件
						FileChannel source = FileChannel.open(file, StandardOpenOption.READ);

						// 创建一个传输体监听器，用于从文件通道传输数据到响应通道
						TransferBodyListener listener = new TransferBodyListener(source, position,
								count, sink);

						// 在 Mono 被销毁时，关闭文件通道
						sink.onDispose(listener::closeSource);

						// 获取响应通道
						StreamSinkChannel destination = this.exchange.getResponseChannel();

						// 设置响应通道的写操作处理器为传输体监听器的 transfer 方法
						destination.getWriteSetter().set(listener::transfer);

						// 开始传输数据
						listener.transfer(destination);
					} catch (IOException ex) {
						// 如果发生 IO 异常，则向 sink 发送错误信号
						sink.error(ex);
					}
				}));
	}

	@Override
	protected Processor<? super Publisher<? extends DataBuffer>, Void> createBodyFlushProcessor() {
		return new ResponseBodyFlushProcessor();
	}

	private ResponseBodyProcessor createBodyProcessor() {
		// 如果响应通道为空
		if (this.responseChannel == null) {
			// 获取响应通道并赋值给实例变量
			this.responseChannel = this.exchange.getResponseChannel();
		}

		// 返回一个新的 ResponseBodyProcessor 对象，该对象使用响应通道进行处理
		return new ResponseBodyProcessor(this.responseChannel);
	}


	private class ResponseBodyProcessor extends AbstractListenerWriteProcessor<DataBuffer> {

		/**
		 * 流式传输通道
		 */
		private final StreamSinkChannel channel;

		/**
		 * 字节缓冲区
		 */
		@Nullable
		private volatile ByteBuffer byteBuffer;


		/**
		 * 为 {@link #writePossible} 记录写监听器调用。
		 */
		private volatile boolean writePossible;


		public ResponseBodyProcessor(StreamSinkChannel channel) {
			super(request.getLogPrefix());
			Assert.notNull(channel, "StreamSinkChannel must not be null");
			this.channel = channel;
			this.channel.getWriteSetter().set(c -> {
				// 将可写标志设置为true
				this.writePossible = true;
				// 恢复写监听器
				onWritePossible();
			});
			this.channel.suspendWrites();
		}

		@Override
		protected boolean isWritePossible() {
			// 恢复可写的状态
			this.channel.resumeWrites();
			return this.writePossible;
		}

		@Override
		protected boolean write(DataBuffer dataBuffer) throws IOException {
			// 获取字节缓冲区
			ByteBuffer buffer = this.byteBuffer;
			// 如果字节缓冲区为空，则返回false
			if (buffer == null) {
				return false;
			}

			// 从这里开始跟踪写入监听器调用
			this.writePossible = false;

			// 在发生IOException的情况下，onError处理应该调用discardData(DataBuffer)..
			// 记录需要写入的总字节数
			int total = buffer.remaining();
			// 写入字节缓冲区并获取写入的字节数
			int written = writeByteBuffer(buffer);

			// 如果跟踪日志启用
			if (rsWriteLogger.isTraceEnabled()) {
				// 记录跟踪日志，显示写入的字节数和总字节数
				rsWriteLogger.trace(getLogPrefix() + "Wrote " + written + " of " + total + " bytes");
			}
			// 如果写入的字节数不等于总字节数，则返回false
			if (written != total) {
				return false;
			}

			// 我们已经写入了全部，所以仍然可以继续写入
			this.writePossible = true;

			// 释放数据缓冲区
			DataBufferUtils.release(dataBuffer);
			// 将字节缓冲区置为空
			this.byteBuffer = null;
			// 返回true表示成功写入
			return true;
		}

		private int writeByteBuffer(ByteBuffer byteBuffer) throws IOException {
			int written;
			// 初始化已写入的总字节数
			int totalWritten = 0;
			// 循环写入直到字节缓冲区没有剩余字节或写入的字节数为0
			do {
				// 将字节写入通道并获取写入的字节数
				written = this.channel.write(byteBuffer);
				// 累加已写入的字节数
				totalWritten += written;
				// 当字节缓冲区仍有剩余字节且写入的字节数大于0时继续循环
			} while (byteBuffer.hasRemaining() && written > 0);
			// 返回已写入的总字节数
			return totalWritten;
		}

		@Override
		protected void dataReceived(DataBuffer dataBuffer) {
			// 调用父类方法处理接收到的数据缓冲区
			super.dataReceived(dataBuffer);
			// 将数据缓冲区转换为字节缓冲区并赋值给byteBuffer
			this.byteBuffer = dataBuffer.asByteBuffer();
		}

		@Override
		protected boolean isDataEmpty(DataBuffer dataBuffer) {
			return (dataBuffer.readableByteCount() == 0);
		}

		@Override
		protected void writingComplete() {
			// 移除写入监听器
			this.channel.getWriteSetter().set(null);
			// 恢复写入
			this.channel.resumeWrites();
		}

		@Override
		protected void writingFailed(Throwable ex) {
			// 调用取消方法
			cancel();
			// 调用处理错误的方法
			onError(ex);
		}

		@Override
		protected void discardData(DataBuffer dataBuffer) {
			DataBufferUtils.release(dataBuffer);
		}
	}


	private class ResponseBodyFlushProcessor extends AbstractListenerWriteFlushProcessor<DataBuffer> {

		public ResponseBodyFlushProcessor() {
			super(request.getLogPrefix());
		}

		@Override
		protected Processor<? super DataBuffer, Void> createWriteProcessor() {
			return UndertowServerHttpResponse.this.createBodyProcessor();
		}

		@Override
		protected void flush() throws IOException {
			// 获取响应通道
			StreamSinkChannel channel = UndertowServerHttpResponse.this.responseChannel;
			// 如果通道不为空
			if (channel != null) {
				// 如果跟踪日志启用
				if (rsWriteFlushLogger.isTraceEnabled()) {
					// 记录跟踪日志
					rsWriteFlushLogger.trace(getLogPrefix() + "flush");
				}
				// 刷新通道
				channel.flush();
			}
		}

		@Override
		protected boolean isWritePossible() {
			// 获取响应通道
			StreamSinkChannel channel = UndertowServerHttpResponse.this.responseChannel;
			// 如果通道不为空
			if (channel != null) {
				// 恢复写入
				channel.resumeWrites();
				// 返回true表示成功恢复写入
				return true;
			}
			// 返回false表示通道为空，无需恢复写入
			return false;
		}

		@Override
		protected boolean isFlushPending() {
			return false;
		}
	}


	private static class TransferBodyListener {

		/**
		 * 源文件通道
		 */
		private final FileChannel source;

		/**
		 * MonoSink
		 */
		private final MonoSink<Void> sink;

		/**
		 * 文件读取位置
		 */
		private long position;

		/**
		 * 读取字节数
		 */
		private long count;


		public TransferBodyListener(FileChannel source, long position, long count, MonoSink<Void> sink) {
			this.source = source;
			this.sink = sink;
			this.position = position;
			this.count = count;
		}

		public void transfer(StreamSinkChannel destination) {
			try {
				// 循环直到剩余字节数为0
				while (this.count > 0) {
					// 将数据从源通道传输到目标通道
					long len = destination.transferFrom(this.source, this.position, this.count);
					// 如果传输的字节数不为0
					if (len != 0) {
						// 更新位置和剩余字节数
						this.position += len;
						this.count -= len;
					} else {
						// 如果传输的字节数为0，恢复写入并返回
						destination.resumeWrites();
						return;
					}
				}
				// 如果循环结束，说明传输完成，通知sink成功
				this.sink.success();
			} catch (IOException ex) {
				// 捕获IO异常，通知sink出错
				this.sink.error(ex);
			}

		}

		public void closeSource() {
			try {
				// 关闭源文件通道
				this.source.close();
			} catch (IOException ignore) {
			}
		}


	}

}
