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

import org.apache.commons.logging.Log;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 用于在事件监听器写入API和响应式流之间进行桥接的 {@code Processor} 实现的抽象基类。
 *
 * <p>具体来说，这是一个基类，用于使用Servlet 3.1非阻塞I/O和Undertow XNIO写入HTTP响应主体，
 * 以及使用Java WebSocket API (JSR-356)、Jetty和Undertow写入WebSocket消息。
 *
 * @param <T> {@link Subscriber} 接收的元素类型
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractListenerWriteProcessor<T> implements Processor<T, Void> {

	/**
	 * 用于调试响应式流信号的特殊日志记录器。
	 *
	 * @see LogDelegateFactory#getHiddenLog(Class)
	 * @see AbstractListenerReadPublisher#rsReadLogger
	 * @see AbstractListenerWriteFlushProcessor#rsWriteFlushLogger
	 * @see WriteResultPublisher#rsWriteResultLogger
	 */
	protected static final Log rsWriteLogger = LogDelegateFactory.getHiddenLog(AbstractListenerWriteProcessor.class);

	/**
	 * 用于存储当前状态的原子引用。
	 */
	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	/**
	 * 订阅对象。
	 */
	@Nullable
	private Subscription subscription;

	/**
	 * 当前数据
	 */
	@Nullable
	private volatile T currentData;

	/**
	 * 表示在最后一次写入期间接收到了 "onComplete"。
	 */
	private volatile boolean sourceCompleted;

	/**
	 * 表示我们在 "onComplete" 后等待最后一个 isReady-onWritePossible 循环，
	 * 因为某些Servlet容器希望在调用 AsyncContext.complete() 之前进行此操作。
	 * 参见 https://github.com/eclipse-ee4j/servlet-api/issues/273
	 */
	private volatile boolean readyToCompleteAfterLastWrite;

	/**
	 * 写入结果发布者
	 */
	private final WriteResultPublisher resultPublisher;

	/**
	 * 日志前缀
	 */
	private final String logPrefix;


	public AbstractListenerWriteProcessor() {
		this("");
	}

	/**
	 * 使用给定的日志前缀创建一个实例。
	 *
	 * @since 5.1
	 */
	public AbstractListenerWriteProcessor(String logPrefix) {
		// AbstractListenerFlushProcessor 直接调用 cancelAndSetCompleted，因此此取消任务
		// 不会用于 HTTP 响应，但可以用于 WebSocket 会话。
		this.resultPublisher = new WriteResultPublisher(logPrefix + "[WP] ", this::cancelAndSetCompleted);
		this.logPrefix = (StringUtils.hasText(logPrefix) ? logPrefix : "");
	}


	/**
	 * 获取配置的日志前缀。
	 *
	 * @since 5.1
	 */
	public String getLogPrefix() {
		return this.logPrefix;
	}


	// 订阅者方法和异步I/O通知方法...

	@Override
	public final void onSubscribe(Subscription subscription) {
		this.state.get().onSubscribe(this, subscription);
	}

	@Override
	public final void onNext(T data) {
		// 如果跟踪日志级别已启用
		if (rsWriteLogger.isTraceEnabled()) {
			// 记录 onNext 信息，包括日志前缀和数据的简单类名
			rsWriteLogger.trace(getLogPrefix() + "onNext: " + data.getClass().getSimpleName());
		}

		// 调用当前状态对象的 onNext 方法，处理传入的数据
		this.state.get().onNext(this, data);
	}

	/**
	 * 来自上游 "write" Publisher 的错误信号。子类也会使用此方法来委托容器中的错误通知。
	 */
	@Override
	public final void onError(Throwable ex) {
		// 获取当前状态对象
		State state = this.state.get();
		// 如果跟踪日志级别已启用
		if (rsWriteLogger.isTraceEnabled()) {
			// 记录 onError 信息
			rsWriteLogger.trace(getLogPrefix() + "onError: " + ex + " [" + state + "]");
		}
		// 在当前状态对象中处理错误
		state.onError(this, ex);
	}

	/**
	 * 来自上游 "write" Publisher 的完成信号。子类也会使用此方法来委托容器中的完成通知。
	 */
	@Override
	public final void onComplete() {
		// 获取当前状态对象
		State state = this.state.get();
		// 如果跟踪日志级别已启用
		if (rsWriteLogger.isTraceEnabled()) {
			// 记录完成信息，包括日志前缀和当前状态
			rsWriteLogger.trace(getLogPrefix() + "onComplete [" + state + "]");
		}
		// 在当前状态对象中处理完成事件
		state.onComplete(this);
	}

	/**
	 * 在写入操作可行时调用，可以是通过 {@link #isWritePossible()} 进行检查后在同一线程中，
	 * 或作为底层容器的回调。
	 */
	public final void onWritePossible() {
		// 获取当前状态对象
		State state = this.state.get();
		// 如果跟踪日志级别已启用
		if (rsWriteLogger.isTraceEnabled()) {
			// 记录 onWritePossible 信息，包括日志前缀和当前状态
			rsWriteLogger.trace(getLogPrefix() + "onWritePossible [" + state + "]");
		}
		// 在当前状态对象中处理可写事件
		state.onWritePossible(this);
	}

	/**
	 * 取消上游的 "write" Publisher，例如由于Servlet容器的错误/完成通知。
	 * 通常应跟随调用 {@link #onError(Throwable)} 或 {@link #onComplete()} 来通知下游链，
	 * 除非取消操作来自下游。
	 */
	public void cancel() {
		// 如果跟踪日志级别已启用
		if (rsWriteLogger.isTraceEnabled()) {
			// 记录取消信息，包括日志前缀和当前状态
			rsWriteLogger.trace(getLogPrefix() + "cancel [" + this.state + "]");
		}
		// 如果订阅对象不为空，则取消订阅
		if (this.subscription != null) {
			this.subscription.cancel();
		}
	}

	/**
	 * 取消 "write" Publisher 并立即转换为 COMPLETED 状态，同时不通知下游。
	 * 用于取消操作来自下游时使用。
	 */
	void cancelAndSetCompleted() {
		// 取消订阅
		cancel();
		for (; ; ) {
			// 获取当前状态
			State prev = this.state.get();
			// 如果当前状态为 已完成，则跳出循环
			if (prev == State.COMPLETED) {
				break;
			}
			// 将当前状态设置为 已完成状态
			if (this.state.compareAndSet(prev, State.COMPLETED)) {
				// 如果设置成功
				// 如果跟踪日志级别已启用
				if (rsWriteLogger.isTraceEnabled()) {
					// 记录状态转换信息
					rsWriteLogger.trace(getLogPrefix() + prev + " -> " + this.state);
				}
				// 如果当前状态不是 写入中
				if (prev != State.WRITING) {
					// 丢弃当前数据
					discardCurrentData();
				}
				// 跳出循环
				break;
			}
		}
	}

	// 给Publisher实现的结果通知

	@Override
	public final void subscribe(Subscriber<? super Void> subscriber) {
		this.resultPublisher.subscribe(subscriber);
	}


	// 编写要实现的API方法或要重写的模板方法...

	/**
	 * 检查给定的数据项是否有任何内容可写。
	 * 如果返回 false，则不会写入该项数据。
	 */
	protected abstract boolean isDataEmpty(T data);

	/**
	 * 当通过 {@link Subscriber#onNext(Object)} 收到要写入的数据项时调用的模板方法。
	 * 默认实现会保存数据项，直到可以进行写入操作。
	 */
	protected void dataReceived(T data) {
		// 获取当前数据
		T prev = this.currentData;
		if (prev != null) {
			// 如果存在当前数据
			// 这种情况不应发生:
			//   1. dataReceived 只能在 REQUESTED 状态下调用
			//   2. 在请求之前会清除 currentData
			// 丢弃掉该数据
			discardData(data);
			// 取消订阅
			cancel();
			// 抛出异常
			onError(new IllegalStateException("Received new data while current not processed yet."));
		}
		// 将数据设置为当前数据项
		this.currentData = data;
	}

	/**
	 * 检查写入是否可行。
	 */
	protected abstract boolean isWritePossible();

	/**
	 * 写入给定的数据项。
	 * <p><strong>注意:</strong> 子类应负责在完全写入后释放与数据项关联的任何数据缓冲区，
	 * 如果底层容器适用于池化缓冲区。
	 *
	 * @param data 要写入的数据项
	 * @return {@code true} 如果当前数据项完全写入并请求了新的数据项，
	 * 或者 {@code false} 如果写入部分并且在完全写入之前需要更多写入回调
	 */
	protected abstract boolean write(T data) throws IOException;

	/**
	 * 当前数据已写入后，在请求来自上游写入Publisher的下一项之前调用。
	 * <p>默认实现为空操作。
	 *
	 * @deprecated 最初为Undertow引入，用于在没有数据可用时停止写入通知，但自5.0.6起已弃用，因为在每个请求的项上进行常量切换会导致显著减慢。
	 */
	@Deprecated
	protected void writingPaused() {
	}

	/**
	 * 在 onComplete 或 onError 通知后调用。
	 * <p>默认实现为空操作。
	 */
	protected void writingComplete() {
	}

	/**
	 * 在写入过程中发生I/O错误时调用。如果子类知道底层API将在容器线程中提供错误通知，则可以选择忽略此错误。
	 * <p>默认为空操作。
	 */
	protected void writingFailed(Throwable ex) {
	}

	/**
	 * 在任何错误（无论是来自上游写入Publisher还是底层服务器的I/O操作）和取消后调用，以丢弃正在写入时发生错误时处于进行中的数据。
	 *
	 * @param data 要释放的数据
	 * @since 5.0.11
	 */
	protected abstract void discardData(T data);


	// 用于从State的私有方法

	private boolean changeState(State oldState, State newState) {
		// 比较并设置状态
		boolean result = this.state.compareAndSet(oldState, newState);
		if (result && rsWriteLogger.isTraceEnabled()) {
			// 如果成功设置状态，并且跟踪日志级别已启用，则记录状态信息
			rsWriteLogger.trace(getLogPrefix() + oldState + " -> " + newState);
		}
		// 返回操作结果
		return result;
	}

	private void changeStateToReceived(State oldState) {
		if (changeState(oldState, State.RECEIVED)) {
			// 如果转为已接收状态，则写入数据
			writeIfPossible();
		}
	}

	private void changeStateToComplete(State oldState) {
		// 将当前状态转换为已完成状态
		if (changeState(oldState, State.COMPLETED)) {
			// 丢弃当前数据
			discardCurrentData();
			// 写入完成
			writingComplete();
			// 调用发布完成方法
			this.resultPublisher.publishComplete();
		} else {
			// 如果状态转换失败，则委托容器的完成通知
			this.state.get().onComplete(this);
		}
	}

	private void writeIfPossible() {
		// 检查是否可以进行写操作
		boolean result = isWritePossible();

		// 如果不可进行写操作，且日志记录器的追踪级别启用
		if (!result && rsWriteLogger.isTraceEnabled()) {
			// 记录不可写信息，包括日志前缀和信息
			rsWriteLogger.trace(getLogPrefix() + "isWritePossible false");
		}

		// 如果可以进行写操作
		if (result) {
			// 调用onWritePossible方法
			onWritePossible();
		}
	}

	private void discardCurrentData() {
		// 将当前数据存储在变量data中
		T data = this.currentData;
		// 将当前数据设置为null
		this.currentData = null;

		// 如果数据不为null，则丢弃数据
		if (data != null) {
			discardData(data);
		}
	}


	/**
	 * 表示 {@link Processor} 的状态。
	 *
	 * <p><pre>
	 *          未订阅
	 *             |
	 *             v
	 *   +----- 已请求 ---------------> 已收到 ----+
	 *   |        ^                       ^        |
	 *   |        |                       |        |
	 *   |        + ------ 写入中 <-------+        |
	 *   |                    |                    |
	 *   |                    v                    |
	 *   +---------------> 已完成 <----------------+
	 * </pre>
	 */
	private enum State {
		/**
		 * 未订阅
		 */
		UNSUBSCRIBED {
			@Override
			public <T> void onSubscribe(AbstractListenerWriteProcessor<T> processor, Subscription subscription) {
				Assert.notNull(subscription, "Subscription must not be null");
				// 如果处理器成功将状态更改为 已请求
				if (processor.changeState(this, REQUESTED)) {
					// 将处理器的订阅设置为传入的订阅对象
					processor.subscription = subscription;

					// 请求一个数据项
					subscription.request(1);
				} else {
					// 否则，调用父类的onSubscribe方法，处理订阅事件
					super.onSubscribe(processor, subscription);
				}
			}

			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				// 这可能发生在 (很早) 从容器完成通知。
				processor.changeStateToComplete(this);
			}
		},

		/**
		 * 已请求
		 */
		REQUESTED {
			@Override
			public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
				// 如果处理器的数据为空
				if (processor.isDataEmpty(data)) {
					// 断言处理器的订阅对象不为null
					Assert.state(processor.subscription != null, "No subscription");
					// 请求一个数据项
					processor.subscription.request(1);
				} else {
					// 如果处理器的数据不为空，则调用处理器的数据接收方法
					processor.dataReceived(data);

					// 将状态更改为已接收
					processor.changeStateToReceived(this);
				}
			}

			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				// 将处理器的 准备在最后一次写入后完成 标志设置为 true
				processor.readyToCompleteAfterLastWrite = true;

				// 将状态更改为 已接收
				processor.changeStateToReceived(this);
			}
		},

		/**
		 * 已接收
		 */
		RECEIVED {
			@SuppressWarnings("deprecation")
			@Override
			public <T> void onWritePossible(AbstractListenerWriteProcessor<T> processor) {
				// 如果处理器准备在最后一次写入后完成
				if (processor.readyToCompleteAfterLastWrite) {
					// 将当前状态转换为 已接收
					processor.changeStateToComplete(RECEIVED);
				} else if (processor.changeState(this, WRITING)) {
					// 如果成功将状态更改为 等待中
					// 获取当前数据
					T data = processor.currentData;

					// 断言当前数据不为null
					Assert.state(data != null, "No data");

					try {
						// 执行写入操作
						if (processor.write(data)) {
							// 如果写入成功，将状态由写入中更改为 已请求
							if (processor.changeState(WRITING, REQUESTED)) {
								// 清空当前数据
								processor.currentData = null;

								// 如果数据源已经完成
								if (processor.sourceCompleted) {
									// 将 准备在最后一次写入后完成 标志设置为true
									processor.readyToCompleteAfterLastWrite = true;
									// 将状态设置为 已请求
									processor.changeStateToReceived(REQUESTED);
								} else {
									// 否则，暂停写入操作
									processor.writingPaused();
									Assert.state(processor.subscription != null, "No subscription");
									// 请求一个数据项
									processor.subscription.request(1);
								}
							}
						} else {
							// 如果写入不成功，则将状态更改为 等待中
							processor.changeStateToReceived(WRITING);
						}
					} catch (IOException ex) {
						// 捕获写入过程中的IOException异常，处理写入失败的情况
						processor.writingFailed(ex);
					}
				}
			}

			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				// 设置处理器的数据源已完成标志为true
				processor.sourceCompleted = true;

				// 竞争写入可能很快完成
				// 如果当前处理器的状态为 已请求
				if (processor.state.get() == State.REQUESTED) {
					// 将处理器的状态由 已请求 更改为 已完成
					processor.changeStateToComplete(State.REQUESTED);
				}
			}
		},

		/**
		 * 写入中
		 */
		WRITING {
			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				// 设置处理器的数据源已完成标志为true
				processor.sourceCompleted = true;
				// 竞争写入可能很快完成
				// 如果当前处理器的状态为 已请求
				if (processor.state.get() == State.REQUESTED) {
					// 将处理器的状态由 已请求 更改为 已完成
					processor.changeStateToComplete(State.REQUESTED);
				}
			}
		},

		/**
		 * 已完成
		 */
		COMPLETED {
			@Override
			public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
				// 忽略
			}

			@Override
			public <T> void onError(AbstractListenerWriteProcessor<T> processor, Throwable ex) {
				// 忽略
			}

			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				// 忽略
			}
		};

		public <T> void onSubscribe(AbstractListenerWriteProcessor<T> processor, Subscription subscription) {
			subscription.cancel();
		}

		public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
			// 丢弃当前数据
			processor.discardData(data);
			// 取消订阅
			processor.cancel();
			// 处理异常
			processor.onError(new IllegalStateException("Illegal onNext without demand"));
		}

		public <T> void onError(AbstractListenerWriteProcessor<T> processor, Throwable ex) {
			// 如果处理器成功将状态更改为 已完成
			if (processor.changeState(this, COMPLETED)) {
				// 丢弃当前数据
				processor.discardCurrentData();

				// 执行写入完成的操作
				processor.writingComplete();

				// 发布异常到结果发布者
				processor.resultPublisher.publishError(ex);
			} else {
				// 否则，调用当前状态对象的onError方法，处理传入的异常信息
				processor.state.get().onError(processor, ex);
			}
		}

		public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
			throw new IllegalStateException(toString());
		}

		public <T> void onWritePossible(AbstractListenerWriteProcessor<T> processor) {
			// 忽略
		}
	}

}
