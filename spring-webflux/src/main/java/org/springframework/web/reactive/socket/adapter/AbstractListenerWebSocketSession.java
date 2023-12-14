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

package org.springframework.web.reactive.socket.adapter;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.AbstractListenerReadPublisher;
import org.springframework.http.server.reactive.AbstractListenerWriteProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.*;
import org.springframework.web.reactive.socket.WebSocketMessage.Type;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for {@link WebSocketSession} implementations that bridge between
 * event-listener WebSocket APIs (e.g. Java WebSocket API JSR-356, Jetty,
 * Undertow) and Reactive Streams.
 *
 * <p>Also implements {@code Subscriber<Void>} so it can be used to subscribe to
 * the completion of {@link WebSocketHandler#handle(WebSocketSession)}.
 *
 * @param <T> the native delegate type
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractListenerWebSocketSession<T> extends AbstractWebSocketSession<T>
		implements Subscriber<Void> {

	// 定义没有流量控制的情况下用于接收消息的“背压”缓冲区大小
	private static final int RECEIVE_BUFFER_SIZE = 8192;

	// 处理完成的空信号（Sinks）
	@Nullable
	private final Sinks.Empty<Void> handlerCompletionSink;

	// 处理完成的 Mono 处理器
	@Nullable
	@SuppressWarnings("deprecation")
	private final reactor.core.publisher.MonoProcessor<Void> handlerCompletionMono;

	// 接收发布器
	private final WebSocketReceivePublisher receivePublisher;

	// 发送处理器
	@Nullable
	private volatile WebSocketSendProcessor sendProcessor;

	// 标记是否已调用发送方法
	private final AtomicBoolean sendCalled = new AtomicBoolean();

	// 关闭状态的 Sink
	private final Sinks.One<CloseStatus> closeStatusSink = Sinks.one();


	/**
	 * 基础构造函数。
	 *
	 * @param delegate      实际的 WebSocket 会话、通道或连接
	 * @param id            会话 ID
	 * @param info          握手信息
	 * @param bufferFactory 当前连接的 DataBuffer 工厂
	 */
	public AbstractListenerWebSocketSession(
			T delegate, String id, HandshakeInfo info, DataBufferFactory bufferFactory) {

		this(delegate, id, info, bufferFactory, (Sinks.Empty<Void>) null);
	}

	/**
	 * 带有完成信号 Sink 的备用构造函数，用于指示会话处理何时完成（成功或错误）。
	 * 主要用于与 {@code WebSocketClient} 一起使用，以便能够进行通信结束处理。
	 */
	public AbstractListenerWebSocketSession(T delegate, String id, HandshakeInfo info,
											DataBufferFactory bufferFactory, @Nullable Sinks.Empty<Void> handlerCompletionSink) {

		super(delegate, id, info, bufferFactory);
		this.receivePublisher = new WebSocketReceivePublisher();
		this.handlerCompletionSink = handlerCompletionSink;
		this.handlerCompletionMono = null;
	}

	/**
	 * 带有完成 Mono 处理器的备用构造函数，用于指示会话处理何时完成（成功或错误）。
	 * 主要用于与 {@code WebSocketClient} 一起使用，以便能够进行通信结束处理。
	 *
	 * @deprecated 自 5.3 起，弃用，使用 {@link #AbstractListenerWebSocketSession(Object, String, HandshakeInfo, DataBufferFactory, Sinks.Empty)} 替代
	 */
	@Deprecated
	public AbstractListenerWebSocketSession(T delegate, String id, HandshakeInfo info,
											DataBufferFactory bufferFactory, @Nullable reactor.core.publisher.MonoProcessor<Void> handlerCompletion) {

		super(delegate, id, info, bufferFactory);
		this.receivePublisher = new WebSocketReceivePublisher();
		this.handlerCompletionMono = handlerCompletion;
		this.handlerCompletionSink = null;
	}

	/**
	 * 获取发送处理器。
	 *
	 * @return 发送处理器
	 */
	protected WebSocketSendProcessor getSendProcessor() {
		WebSocketSendProcessor sendProcessor = this.sendProcessor;
		Assert.state(sendProcessor != null, "No WebSocketSendProcessor available");
		return sendProcessor;
	}

	/**
	 * 接收 WebSocket 消息的 Flux。
	 *
	 * @return WebSocket 消息的 Flux
	 */
	@Override
	public Flux<WebSocketMessage> receive() {
		// 判断是否可以挂起接收
		return (canSuspendReceiving() ? Flux.from(this.receivePublisher) :
				// 不支持挂起时，使用带有背压缓冲的 Flux
				Flux.from(this.receivePublisher).onBackpressureBuffer(RECEIVE_BUFFER_SIZE));
	}

	/**
	 * 发送 WebSocket 消息的 Mono。
	 *
	 * @param messages 要发送的 WebSocket 消息
	 * @return Mono 表示发送状态
	 */
	@Override
	public Mono<Void> send(Publisher<WebSocketMessage> messages) {
		if (this.sendCalled.compareAndSet(false, true)) {
			WebSocketSendProcessor sendProcessor = new WebSocketSendProcessor();
			this.sendProcessor = sendProcessor;
			return Mono.from(subscriber -> {
				// 订阅发送处理器并将订阅结果传递给订阅者
				messages.subscribe(sendProcessor);
				sendProcessor.subscribe(subscriber);
			});
		} else {
			return Mono.error(new IllegalStateException("send() has already been called"));
		}
	}

	/**
	 * 获取关闭状态的 Mono。
	 *
	 * @return 关闭状态的 Mono
	 */
	@Override
	public Mono<CloseStatus> closeStatus() {
		return this.closeStatusSink.asMono();
	}

	/**
	 * 检查底层 WebSocket API 是否具有流量控制，并且能够暂停和恢复接收消息。
	 * <p><strong>注意：</strong>鼓励子类在可能的情况下始终从暂停模式开始，并等待接收到需求。
	 *
	 * @return 如果能够暂停接收消息，则为 true；否则为 false
	 */
	protected abstract boolean canSuspendReceiving();

	/**
	 * 暂停接收消息，直到接收到的消息被处理并由下游 Subscriber 生成更多需求。
	 * <p><strong>注意：</strong>如果底层 WebSocket API 不提供接收消息的流量控制，此方法应为 无操作，
	 * 并且 {@link #canSuspendReceiving()} 应返回 {@code false}。
	 */
	protected abstract void suspendReceiving();

	/**
	 * 在由下游 Subscriber 生成更多需求后，恢复接收新消息。
	 * <p><strong>注意：</strong>如果底层 WebSocket API 不提供接收消息的流量控制，此方法应为 无操作，
	 * 并且 {@link #canSuspendReceiving()} 应返回 {@code false}。
	 */
	protected abstract void resumeReceiving();

	/**
	 * 发送给定的 WebSocket 消息。
	 * <p><strong>注意：</strong>子类负责在完全写入后释放负载数据缓冲区（如果底层容器适用于池化缓冲区）。
	 *
	 * @param message 要发送的 WebSocket 消息
	 * @return 如果成功发送，则为 true
	 */
	protected abstract boolean sendMessage(WebSocketMessage message) throws IOException;


	// WebSocketHandler 适配器委托方法

	/**
	 * 处理来自WebSocketHandler适配器的消息回调。
	 *
	 * @param type    消息类型
	 * @param message WebSocket 消息
	 */
	void handleMessage(Type type, WebSocketMessage message) {
		this.receivePublisher.handleMessage(message);
	}

	/**
	 * 处理来自 WebSocket 引擎的错误回调。
	 *
	 * @param ex 异常信息
	 */
	void handleError(Throwable ex) {
		// 忽略结果：不会溢出，如果不是第一个或没有人监听，则没有问题
		this.closeStatusSink.tryEmitEmpty();
		this.receivePublisher.onError(ex);
		WebSocketSendProcessor sendProcessor = this.sendProcessor;
		if (sendProcessor != null) {
			sendProcessor.cancel();
			sendProcessor.onError(ex);
		}
	}

	/**
	 * 处理来自 WebSocket 引擎的关闭回调。
	 *
	 * @param closeStatus 关闭状态
	 */
	void handleClose(CloseStatus closeStatus) {
		// 忽略结果：不会溢出，如果不是第一个或没有人监听，则没有问题
		this.closeStatusSink.tryEmitValue(closeStatus);
		this.receivePublisher.onAllDataRead();
		WebSocketSendProcessor sendProcessor = this.sendProcessor;
		if (sendProcessor != null) {
			sendProcessor.cancel();
			sendProcessor.onComplete();
		}
	}
	//  Subscriber<Void> 实现 跟踪 WebSocketHandler#handle 完成

	/**
	 * 实现 {@link Subscriber} 接口，处理订阅事件
	 */
	@Override
	public void onSubscribe(Subscription subscription) {
		subscription.request(Long.MAX_VALUE);
	}

	/**
	 * 实现 {@link Subscriber} 接口，处理 onNext 事件。
	 * 无操作。
	 */
	@Override
	public void onNext(Void aVoid) {
		// 无操作
	}

	/**
	 * 实现 {@link Subscriber} 接口，处理 onError 事件。
	 *
	 * @param ex 发生的异常
	 */
	@Override
	public void onError(Throwable ex) {
		if (this.handlerCompletionSink != null) {
			// 忽略结果：不会溢出，如果不是第一个或没有人监听，则没有问题
			this.handlerCompletionSink.tryEmitError(ex);
		}
		if (this.handlerCompletionMono != null) {
			this.handlerCompletionMono.onError(ex);
		}
		close(CloseStatus.SERVER_ERROR.withReason(ex.getMessage()));
	}

	/**
	 * 实现 {@link Subscriber} 接口，处理 完成 事件。
	 */
	@Override
	public void onComplete() {
		if (this.handlerCompletionSink != null) {
			// 忽略结果：不会溢出，如果不是第一个或没有人监听，则没有问题
			this.handlerCompletionSink.tryEmitEmpty();
		}
		if (this.handlerCompletionMono != null) {
			this.handlerCompletionMono.onComplete();
		}
		close();
	}

	// WebSocket 消息接收发布者，继承自 {@link AbstractListenerReadPublisher}。
	private final class WebSocketReceivePublisher extends AbstractListenerReadPublisher<WebSocketMessage> {
		/**
		 * 挂起的消息队列
		 */
		private volatile Queue<Object> pendingMessages = Queues.unbounded(Queues.SMALL_BUFFER_SIZE).get();


		WebSocketReceivePublisher() {
			super(AbstractListenerWebSocketSession.this.getLogPrefix());
		}

		/**
		 * 检查是否有可用数据。
		 */
		@Override
		protected void checkOnDataAvailable() {
			resumeReceiving();
			int size = this.pendingMessages.size();
			if (rsReadLogger.isTraceEnabled()) {
				rsReadLogger.trace(getLogPrefix() + "checkOnDataAvailable (" + size + " pending)");
			}
			if (size > 0) {
				onDataAvailable();
			}
		}

		/**
		 * 暂停读取数据
		 */
		@Override
		protected void readingPaused() {
			suspendReceiving();
		}

		/**
		 * 读取数据。
		 *
		 * @return 读取的 WebSocket 消息
		 * @throws IOException 读取过程中可能抛出的异常
		 */
		@Override
		@Nullable
		protected WebSocketMessage read() throws IOException {
			return (WebSocketMessage) this.pendingMessages.poll();
		}

		/**
		 * 处理接收到的 WebSocket 消息。
		 *
		 * @param message 接收到的 WebSocket 消息
		 */
		void handleMessage(WebSocketMessage message) {
			if (logger.isTraceEnabled()) {
				logger.trace(getLogPrefix() + "Received " + message);
			} else if (rsReadLogger.isTraceEnabled()) {
				rsReadLogger.trace(getLogPrefix() + "Received " + message);
			}
			if (!this.pendingMessages.offer(message)) {
				discardData();
				throw new IllegalStateException(
						"Too many messages. Please ensure WebSocketSession.receive() is subscribed to.");
			}
			onDataAvailable();
		}

		/**
		 * 丢弃数据。
		 */
		@Override
		protected void discardData() {
			while (true) {
				WebSocketMessage message = (WebSocketMessage) this.pendingMessages.poll();
				if (message == null) {
					return;
				}
				message.release();
			}
		}
	}


	/**
	 * 处理发送 WebSocket 消息的处理器。
	 */
	protected final class WebSocketSendProcessor extends AbstractListenerWriteProcessor<WebSocketMessage> {

		/**
		 * 标识是否准备好发送消息
		 */
		private volatile boolean isReady = true;

		/**
		 * WebSocket 发送处理器的构造函数。
		 */
		WebSocketSendProcessor() {
			super(receivePublisher.getLogPrefix());
		}


		/**
		 * 写入 WebSocket 消息。
		 *
		 * @param message 要发送的 WebSocket 消息
		 * @return 写入是否成功
		 * @throws IOException 写入过程中可能抛出的异常
		 */
		@Override
		protected boolean write(WebSocketMessage message) throws IOException {
			if (logger.isTraceEnabled()) {
				logger.trace(getLogPrefix() + "Sending " + message);
			} else if (rsWriteLogger.isTraceEnabled()) {
				rsWriteLogger.trace(getLogPrefix() + "Sending " + message);
			}
			// 在IOException的情况下，onError处理应该调用discardData(WebSocketMessage)。
			return sendMessage(message);
		}

		/**
		 * 检查数据是否为空。
		 *
		 * @param message WebSocket 消息
		 * @return 数据是否为空
		 */
		@Override
		protected boolean isDataEmpty(WebSocketMessage message) {
			return (message.getPayload().readableByteCount() == 0);
		}

		/**
		 * 检查是否可以写入数据。
		 *
		 * @return 是否可以写入数据
		 */
		@Override
		protected boolean isWritePossible() {
			return (this.isReady);
		}

		/**
		 * 设置准备发送标志。
		 * 子类可以在发送消息 (false) 之前和接收异步发送回调 (true) 后调用此方法，有效的将异步完成回调转换为简单的流控制。
		 *
		 * @param ready 是否准备好发送消息
		 */
		public void setReadyToSend(boolean ready) {
			if (ready && rsWriteLogger.isTraceEnabled()) {
				rsWriteLogger.trace(getLogPrefix() + "Ready to send");
			}
			this.isReady = ready;
		}

		/**
		 * 丢弃数据。
		 *
		 * @param message 要丢弃的 WebSocket 消息
		 */
		@Override
		protected void discardData(WebSocketMessage message) {
			message.release();
		}
	}

}
