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

package org.springframework.http.server.reactive;

import org.apache.commons.logging.Log;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Operators;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 用于桥接事件监听器读取API和反应流的 {@code Publisher} 实现的抽象基类。
 *
 * <p>具体来说，这是从HTTP请求体中读取数据的基类，支持Servlet 3.1非阻塞I/O和Undertow XNIO，
 * 以及使用标准Java WebSocket（JSR-356）、Jetty和Undertow处理传入的WebSocket消息。
 *
 * @param <T> 发出元素的类型
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractListenerReadPublisher<T> implements Publisher<T> {

	/**
	 * 用于调试反应流信号的特殊日志记录器。
	 *
	 * @see LogDelegateFactory#getHiddenLog(Class)
	 * @see AbstractListenerWriteProcessor#rsWriteLogger
	 * @see AbstractListenerWriteFlushProcessor#rsWriteFlushLogger
	 * @see WriteResultPublisher#rsWriteResultLogger
	 */
	protected static Log rsReadLogger = LogDelegateFactory.getHiddenLog(AbstractListenerReadPublisher.class);

	/**
	 * 空的数据缓冲区
	 */
	final static DataBuffer EMPTY_BUFFER = DefaultDataBufferFactory.sharedInstance.allocateBuffer(0);

	/**
	 * 状态
	 */
	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	/**
	 * 需求数量
	 */
	private volatile long demand;

	/**
	 * 需求字段更新者
	 */
	@SuppressWarnings("rawtypes")
	private static final AtomicLongFieldUpdater<AbstractListenerReadPublisher> DEMAND_FIELD_UPDATER =
			AtomicLongFieldUpdater.newUpdater(AbstractListenerReadPublisher.class, "demand");

	/**
	 * 订阅者
	 */
	@Nullable
	private volatile Subscriber<? super T> subscriber;

	/**
	 * 是否待完成
	 */
	private volatile boolean completionPending;

	/**
	 * 待处理的异常
	 */
	@Nullable
	private volatile Throwable errorPending;

	/**
	 * 日志前缀
	 */
	private final String logPrefix;


	public AbstractListenerReadPublisher() {
		this("");
	}

	/**
	 * 使用给定的日志前缀创建一个实例。
	 *
	 * @since 5.1
	 */
	public AbstractListenerReadPublisher(String logPrefix) {
		this.logPrefix = logPrefix;
	}


	/**
	 * 返回配置的日志消息前缀。
	 *
	 * @since 5.1
	 */
	public String getLogPrefix() {
		return this.logPrefix;
	}

	// Publisher 实现...

	@Override
	public void subscribe(Subscriber<? super T> subscriber) {
		this.state.get().subscribe(this, subscriber);
	}

	// 异步I/O通知方法...

	/**
	 * 当可以读取时调用，可以在通过 {@link #checkOnDataAvailable()} 进行检查后的同一线程中调用，
	 * 或者作为底层容器的回调调用。
	 */
	public final void onDataAvailable() {
		rsReadLogger.trace(getLogPrefix() + "onDataAvailable");
		this.state.get().onDataAvailable(this);
	}

	/**
	 * 子类可以调用此方法，在所有数据都已读取时委托一个完成通知。
	 */
	public void onAllDataRead() {
		// 获取当前状态
		State state = this.state.get();

		// 如果日志级别允许跟踪，则记录日志
		if (rsReadLogger.isTraceEnabled()) {
			rsReadLogger.trace(getLogPrefix() + "onAllDataRead [" + state + "]");
		}

		// 执行当前状态的 onAllDataRead 方法，传入当前对象作为参数
		state.onAllDataRead(this);
	}

	/**
	 * 子类可以调用此方法来委派容器错误通知。
	 *
	 * @param ex 异常对象，表示发生的错误
	 */
	public final void onError(Throwable ex) {
		// 获取当前状态
		State state = this.state.get();

		// 如果日志级别允许跟踪，则记录带有异常信息和状态的日志
		if (rsReadLogger.isTraceEnabled()) {
			rsReadLogger.trace(getLogPrefix() + "onError: " + ex + " [" + state + "]");
		}

		// 调用当前状态的 onError 方法处理异常对象
		state.onError(this, ex);
	}

	// 待实现或需要重写的读取API方法...

	/**
	 * 检查数据是否可用，立即调用 {@link #onDataAvailable()} 或安排一个通知。
	 */
	protected abstract void checkOnDataAvailable();

	/**
	 * 从输入中读取一次数据，如果可能的话。
	 *
	 * @return 读取的项目；如果未读取到则返回 {@code null}
	 * @throws IOException 如果发生I/O错误
	 */
	@Nullable
	protected abstract T read() throws IOException;

	/**
	 * 在由于需求不足而暂停读取时调用。
	 * <p><strong>注意：</strong>此方法保证不会与 {@link #checkOnDataAvailable()} 竞争，
	 * 因此可以安全地用于暂停读取，如果底层API支持的话，即不会与通过 {@code checkOnDataAvailable()} 隐式调用的恢复竞争。
	 *
	 * @since 5.0.2
	 */
	protected abstract void readingPaused();

	/**
	 * 在底层服务器发生I/O读取错误或下游消费者发出取消信号后调用，
	 * 允许子类丢弃它们可能持有的任何当前缓存数据。
	 *
	 * @since 5.0.11
	 */
	protected abstract void discardData();


	// 要在状态中使用的私有方法...

	/**
	 * 逐个读取和发布数据，直到没有更多数据可读、没有更多需求或者已完成。
	 *
	 * @return {@code true} 如果还有需求；{@code false} 如果没有更多需求或已完成。
	 * @throws IOException 如果读取数据时发生I/O错误
	 */
	private boolean readAndPublish() throws IOException {
		long r;

		// 循环条件：需求大于0，并且状态不是 已完成
		while ((r = this.demand) > 0 && (this.state.get() != State.COMPLETED)) {
			// 读取数据
			T data = read();

			// 如果读取到的数据是 空的缓冲区
			if (data == EMPTY_BUFFER) {
				// 如果日志级别允许跟踪，则记录日志
				if (rsReadLogger.isTraceEnabled()) {
					rsReadLogger.trace(getLogPrefix() + "0 bytes read, trying again");
				}
			} else if (data != null) {
				// 如果数据不为 null
				// 如果需求数量不是 Long.MAX_VALUE
				if (r != Long.MAX_VALUE) {
					// 减少需求量
					DEMAND_FIELD_UPDATER.addAndGet(this, -1L);
				}

				// 获取订阅者
				Subscriber<? super T> subscriber = this.subscriber;
				// 断言确保订阅者不为 null
				Assert.state(subscriber != null, "No subscriber");

				// 如果日志级别允许跟踪，则记录发布数据的日志
				if (rsReadLogger.isTraceEnabled()) {
					rsReadLogger.trace(getLogPrefix() + "Publishing " + data.getClass().getSimpleName());
				}

				// 向订阅者发布数据
				subscriber.onNext(data);
			} else {
				// 数据为空
				// 如果日志级别允许跟踪，则记录日志
				if (rsReadLogger.isTraceEnabled()) {
					rsReadLogger.trace(getLogPrefix() + "No more to read");
				}
				// 返回 true 表示读取和发布成功
				return true;
			}
		}

		// 返回 false 表示读取和发布失败
		return false;
	}

	private boolean changeState(State oldState, State newState) {
		// 使用原子操作比较和设置（compareAndSet）来更新状态
		boolean result = this.state.compareAndSet(oldState, newState);

		// 如果更新成功并且日志级别允许跟踪，则记录状态转换的日志
		if (result && rsReadLogger.isTraceEnabled()) {
			rsReadLogger.trace(getLogPrefix() + oldState + " -> " + newState);
		}

		// 返回更新结果
		return result;

	}

	private void changeToDemandState(State oldState) {
		// 如果成功将状态从 旧的状态 转换为 需求状态
		if (changeState(oldState, State.DEMAND)) {
			// 保护不受 Undertow 中无限递归的影响，那里我们无法检查数据是否可用，所以我们只能尝试读取。
			// 通常情况下，如果我们刚刚从 readAndPublish() 中出来，就不需要检查...

			// 如果旧的状态不是读取中的状态
			if (oldState != State.READING) {
				// 调用方法检查数据是否可用
				checkOnDataAvailable();
			}
		}

	}

	private boolean handlePendingCompletionOrError() {
		// 获取当前状态
		State state = this.state.get();

		// 如果当前状态是 需求 或 无需求
		if (state == State.DEMAND || state == State.NO_DEMAND) {
			// 如果有完成操作挂起
			if (this.completionPending) {
				// 如果日志级别允许跟踪，则记录日志
				if (rsReadLogger.isTraceEnabled()) {
					rsReadLogger.trace(getLogPrefix() + "Processing pending completion");
				}
				// 调用当前状态的 onAllDataRead 方法处理所有数据已读取的情况
				this.state.get().onAllDataRead(this);
				// 返回 true 表示处理完成
				return true;
			}

			// 获取挂起的错误异常
			Throwable ex = this.errorPending;
			if (ex != null) {
				// 如果日志级别允许跟踪，则记录日志
				if (rsReadLogger.isTraceEnabled()) {
					rsReadLogger.trace(getLogPrefix() + "Processing pending completion with error: " + ex);
				}
				// 调用当前状态的 onError 方法处理发生的错误异常
				this.state.get().onError(this, ex);
				// 返回 true 表示处理完成
				return true;
			}
		}

		// 如果没有符合条件的处理需求，返回 false 表示未进行处理
		return false;
	}

	private Subscription createSubscription() {
		return new ReadSubscription();
	}


	/**
	 * 订阅器，将信号委派给 State。
	 */
	private final class ReadSubscription implements Subscription {


		@Override
		public final void request(long n) {
			// 如果日志级别允许跟踪
			if (rsReadLogger.isTraceEnabled()) {
				// 记录请求日志，包括请求的数量 n
				rsReadLogger.trace(getLogPrefix() + "request " + (n != Long.MAX_VALUE ? n : "Long.MAX_VALUE"));
			}

			// 获取当前状态，并调用其 request 方法处理请求
			state.get().request(AbstractListenerReadPublisher.this, n);
		}

		@Override
		public final void cancel() {
			// 获取当前状态
			State state = AbstractListenerReadPublisher.this.state.get();

			// 如果日志级别允许跟踪
			if (rsReadLogger.isTraceEnabled()) {
				// 记录取消操作的日志，包括当前状态
				rsReadLogger.trace(getLogPrefix() + "cancel [" + state + "]");
			}

			// 调用当前状态的 cancel 方法，取消操作由状态自行处理
			state.cancel(AbstractListenerReadPublisher.this);
		}
	}


	/**
	 * 表示 {@link Publisher} 可能处于的状态。
	 * <p>
	 * 状态转换示意图:
	 * <pre>
	 *        取消订阅
	 *             |
	 *             v
	 *          订阅中
	 *             |
	 *             v
	 *    +---- 无需求 --------------------> 需求 ---+
	 *    |        ^                         ^       |
	 *    |        |                         |       |
	 *    |        +-------- 读取中 <--------+       |
	 *    |                    |                     |
	 *    |                    v                     |
	 *    +---------------> 已完成 <-----------------+
	 * </pre>
	 */
	private enum State {

		/**
		 * 取消订阅
		 */
		UNSUBSCRIBED {
			@Override
			<T> void subscribe(AbstractListenerReadPublisher<T> publisher, Subscriber<? super T> subscriber) {
				Assert.notNull(publisher, "Publisher must not be null");
				Assert.notNull(subscriber, "Subscriber must not be null");
				// 尝试将发布者的状态改变为 订阅中，并检查是否成功
				if (publisher.changeState(this, SUBSCRIBING)) {
					// 创建一个订阅对象
					Subscription subscription = publisher.createSubscription();

					// 将发布者的订阅者设置为当前订阅者
					publisher.subscriber = subscriber;

					// 通知订阅者进行订阅
					subscriber.onSubscribe(subscription);

					// 将发布者的状态从 订阅中 改变为 无需求
					publisher.changeState(SUBSCRIBING, NO_DEMAND);

					// 处理可能存在的未决完成或错误情况
					publisher.handlePendingCompletionOrError();
				} else {
					// 如果无法将状态转换为 订阅中，则抛出 IllegalStateException 异常
					throw new IllegalStateException("Failed to transition to SUBSCRIBING, " +
							"subscriber: " + subscriber);
				}
			}

			@Override
			<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
				// 将待完成标志设置为true
				publisher.completionPending = true;
				// 处理待完成或错误
				publisher.handlePendingCompletionOrError();
			}

			@Override
			<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable ex) {
				// 设置待处理错误
				publisher.errorPending = ex;
				// 处理待完成或错误
				publisher.handlePendingCompletionOrError();
			}
		},

		/**
		 * 订阅中
		 * 很短暂的状态，我们知道有订阅者，但在调用 onSubscribe 之前不应发送 onComplete 和 onError。
		 */
		SUBSCRIBING {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				// 如果操作数 n 有效
				if (Operators.validate(n)) {
					// 使用 需求字段更新者 添加 n 个容量
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);

					// 将发布者的状态更新为 订阅中 的状态
					publisher.changeToDemandState(this);
				}
			}

			@Override
			<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
				// 将待完成标志设置为true
				publisher.completionPending = true;
				// 处理待完成或错误
				publisher.handlePendingCompletionOrError();
			}

			@Override
			<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable ex) {
				// 设置待处理错误
				publisher.errorPending = ex;
				// 处理待完成或错误
				publisher.handlePendingCompletionOrError();
			}
		},

		/**
		 * 无需求
		 */
		NO_DEMAND {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				// 如果操作数 n 有效
				if (Operators.validate(n)) {
					// 使用 需求字段更新者 添加 n 个容量
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					// 将发布者的状态更新为 无需求 的状态
					publisher.changeToDemandState(this);
				}
			}
		},

		/**
		 * 需求
		 */
		DEMAND {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				// 如果操作数 n 有效
				if (Operators.validate(n)) {
					// 使用 需求字段更新者 添加 n 个容量
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					// 在我们之前，并发读取是否转换为NO_DEMAND？
					// 将发布者的状态更新为 无需求 的状态
					publisher.changeToDemandState(NO_DEMAND);
				}
			}

			@Override
			<T> void onDataAvailable(AbstractListenerReadPublisher<T> publisher) {
				// 如果成功将状态改变为读取中
				if (publisher.changeState(this, READING)) {
					try {
						// 尝试执行读取并发布操作
						boolean demandAvailable = publisher.readAndPublish();
						if (demandAvailable) {
							// 如果有需求可用，将当前状态设置为读取中
							publisher.changeToDemandState(READING);
							// 处理待完成或错误
							publisher.handlePendingCompletionOrError();
						} else {
							// 如果没有需求可用
							publisher.readingPaused();
							if (publisher.changeState(READING, NO_DEMAND)) {
								// 尝试将状态从 读取中 改变为 无需求
								if (!publisher.handlePendingCompletionOrError()) {
									// 如果没有处理完待完成或错误
									// 可能在readAndPublish返回后到达了需求
									long r = publisher.demand;
									if (r > 0) {
										// 如果需求大于0，则将状态改变为 无需求
										publisher.changeToDemandState(NO_DEMAND);
									}
								}
							}
						}
					} catch (IOException ex) {
						// 捕获读取或发布过程中的IOException异常
						publisher.onError(ex);
					}
				}
				// 否则，要么竞争ondatavailable (请求vs容器)，或并发完成
			}
		},

		/**
		 * 读取中
		 */
		READING {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				// 如果操作数 n 有效
				if (Operators.validate(n)) {
					// 使用 需求字段更新者 添加 n 个容量
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					// 在我们之前，并发读取是否转换为 无需求？
					// 将发布者的状态更新为 无需求 的状态
					publisher.changeToDemandState(NO_DEMAND);
				}
			}

			@Override
			<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
				// 将待完成标志设置为true
				publisher.completionPending = true;
				// 处理待完成或错误
				publisher.handlePendingCompletionOrError();
			}

			@Override
			<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable ex) {
				// 设置待处理错误
				publisher.errorPending = ex;
				// 处理待完成或错误
				publisher.handlePendingCompletionOrError();
			}
		},

		/**
		 * 已完成
		 */
		COMPLETED {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				// 忽略
			}

			@Override
			<T> void cancel(AbstractListenerReadPublisher<T> publisher) {
				// 忽略
			}

			@Override
			<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
				// 忽略
			}

			@Override
			<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable t) {
				// 忽略
			}
		};

		<T> void subscribe(AbstractListenerReadPublisher<T> publisher, Subscriber<? super T> subscriber) {
			throw new IllegalStateException(toString());
		}

		<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
			throw new IllegalStateException(toString());
		}

		<T> void cancel(AbstractListenerReadPublisher<T> publisher) {
			if (publisher.changeState(this, COMPLETED)) {
				// 如果成功将状态改变为 已完成
				publisher.discardData();
			} else {
				// 如果无法将状态改变为 已完成，则执行取消操作
				publisher.state.get().cancel(publisher);
			}
		}

		<T> void onDataAvailable(AbstractListenerReadPublisher<T> publisher) {
			// 忽略
		}

		<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
			if (publisher.changeState(this, COMPLETED)) {
				// 如果成功将状态改变为 已完成
				// 获取订阅者对象
				Subscriber<? super T> s = publisher.subscriber;
				if (s != null) {
					// 如果订阅者存在，调用其onComplete()方法
					s.onComplete();
				}
			} else {
				// 如果无法将状态改变为 已完成，则执行 onAllDataRead 方法
				publisher.state.get().onAllDataRead(publisher);
			}
		}

		<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable t) {
			if (publisher.changeState(this, COMPLETED)) {
				// 如果成功将状态改变为 已完成
				// 丢弃数据
				publisher.discardData();
				// 获取订阅者对象
				Subscriber<? super T> s = publisher.subscriber;
				if (s != null) {
					// 如果存在订阅者，调用其onError方法，通知订阅者发生了异常
					s.onError(t);
				}
			} else {
				// 如果无法将状态改变为 已完成，则执行onError方法
				publisher.state.get().onError(publisher, t);
			}
		}
	}

}
