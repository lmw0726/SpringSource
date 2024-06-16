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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Operators;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 从 {@link ServerHttpResponse#writeWith(Publisher)} 返回的发布者。
 *
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class WriteResultPublisher implements Publisher<Void> {

	/**
	 * 用于调试响应流信号的特殊日志记录器。
	 *
	 * @see LogDelegateFactory#getHiddenLog(Class)
	 * @see AbstractListenerReadPublisher#rsReadLogger
	 * @see AbstractListenerWriteProcessor#rsWriteLogger
	 * @see AbstractListenerWriteFlushProcessor#rsWriteFlushLogger
	 */
	private static final Log rsWriteResultLogger = LogDelegateFactory.getHiddenLog(WriteResultPublisher.class);

	/**
	 * 当前发布者的状态
	 */
	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	/**
	 * 取消后运行的任务
	 */
	private final Runnable cancelTask;

	/**
	 * 订阅者
	 */
	@Nullable
	private volatile Subscriber<? super Void> subscriber;

	/**
	 * 是否在订阅前已完成
	 */
	private volatile boolean completedBeforeSubscribed;

	/**
	 * 订阅前抛出的错误
	 */
	@Nullable
	private volatile Throwable errorBeforeSubscribed;

	/**
	 * 日志前缀
	 */
	private final String logPrefix;


	public WriteResultPublisher(String logPrefix, Runnable cancelTask) {
		this.cancelTask = cancelTask;
		this.logPrefix = logPrefix;
	}


	@Override
	public final void subscribe(Subscriber<? super Void> subscriber) {
		// 如果跟踪日志级别已启用
		if (rsWriteResultLogger.isTraceEnabled()) {
			// 记录跟踪日志，包含前缀和订阅者信息
			rsWriteResultLogger.trace(this.logPrefix + "got subscriber " + subscriber);
		}
		// 进行订阅操作
		this.state.get().subscribe(this, subscriber);
	}

	/**
	 * 调用此方法将完成信号委派给订阅者。
	 */
	public void publishComplete() {
		// 获取当前状态
		State state = this.state.get();
		// 如果跟踪日志级别已启用
		if (rsWriteResultLogger.isTraceEnabled()) {
			// 记录跟踪日志，输出完成的状态信息
			rsWriteResultLogger.trace(this.logPrefix + "completed [" + state + "]");
		}
		// 调用状态的发布完成方法
		state.publishComplete(this);
	}

	/**
	 * 调用此方法将错误信号委派给订阅者。
	 *
	 * @param t 异常对象，表示发生的错误
	 */
	public void publishError(Throwable t) {
		// 获取当前状态
		State state = this.state.get();
		// 如果跟踪日志级别已启用
		if (rsWriteResultLogger.isTraceEnabled()) {
			// 记录跟踪日志，输出失败的异常信息和当前状态
			rsWriteResultLogger.trace(this.logPrefix + "failed: " + t + " [" + state + "]");
		}
		// 调用状态的发布错误方法
		state.publishError(this, t);
	}

	private boolean changeState(State oldState, State newState) {
		return this.state.compareAndSet(oldState, newState);
	}


	/**
	 * 订阅者接收并将请求和取消信号委派给此发布者的订阅。
	 */
	private static final class WriteResultSubscription implements Subscription {
		/**
		 * 写入结果发布者
		 */
		private final WriteResultPublisher publisher;

		public WriteResultSubscription(WriteResultPublisher publisher) {
			this.publisher = publisher;
		}

		@Override
		public final void request(long n) {
			// 如果启用跟踪日志
			if (rsWriteResultLogger.isTraceEnabled()) {
				// 记录请求日志
				rsWriteResultLogger.trace(this.publisher.logPrefix +
						"request " + (n != Long.MAX_VALUE ? n : "Long.MAX_VALUE"));
			}
			// 调用状态的请求方法
			getState().request(this.publisher, n);
		}

		@Override
		public final void cancel() {
			// 获取当前状态
			State state = getState();
			// 如果启用跟踪日志
			if (rsWriteResultLogger.isTraceEnabled()) {
				// 记录取消操作日志
				rsWriteResultLogger.trace(this.publisher.logPrefix + "cancel [" + state + "]");
			}
			// 调用状态的cancel方法
			state.cancel(this.publisher);
		}

		private State getState() {
			return this.publisher.state.get();
		}
	}


	/**
	 * 表示 {@link Publisher} 可能处于的状态。
	 * <p>
	 * <pre>
	 *       取消订阅
	 *          |
	 *          v
	 *         订阅
	 *          |
	 *          v
	 *        已订阅
	 *          |
	 *          v
	 *        已完成
	 * </pre>
	 */
	private enum State {
		/**
		 * 取消订阅
		 */
		UNSUBSCRIBED {
			@Override
			void subscribe(WriteResultPublisher publisher, Subscriber<? super Void> subscriber) {
				Assert.notNull(subscriber, "Subscriber must not be null");
				// 如果成功将状态改变为 订阅中
				if (publisher.changeState(this, SUBSCRIBING)) {
					// 创建一个写入结果订阅对象
					Subscription subscription = new WriteResultSubscription(publisher);
					// 设置发布者的订阅者为指定的订阅者
					publisher.subscriber = subscriber;
					// 调用订阅者的onSubscribe方法
					subscriber.onSubscribe(subscription);
					// 将状态从 订阅中 改变为 已订阅
					publisher.changeState(SUBSCRIBING, SUBSCRIBED);
					// 现在可以安全地检查"beforeSubscribed"标志，一旦转为NO_DEMAND状态，它们将不会改变
					// 如果发布者在订阅前已完成
					if (publisher.completedBeforeSubscribed) {
						// 调用当前状态的 发布完成 方法
						publisher.state.get().publishComplete(publisher);
					}
					// 获取订阅前的错误异常
					Throwable ex = publisher.errorBeforeSubscribed;
					// 如果异常不为空
					if (ex != null) {
						// 调用当前状态的 发布错误 方法
						publisher.state.get().publishError(publisher, ex);
					}
				} else {
					// 如果无法将状态改变为 订阅中 ，则抛出IllegalStateException异常
					throw new IllegalStateException(toString());
				}
			}

			@Override
			void publishComplete(WriteResultPublisher publisher) {
				// 将 是否在订阅前已完成 标志设置为true
				publisher.completedBeforeSubscribed = true;
				// 如果当前状态是 已订阅
				if (State.SUBSCRIBED == publisher.state.get()) {
					// 调用当前状态的 发布完成 方法
					publisher.state.get().publishComplete(publisher);
				}
			}

			@Override
			void publishError(WriteResultPublisher publisher, Throwable ex) {
				// 将 订阅前抛出的错误 设置为当前的错误
				publisher.errorBeforeSubscribed = ex;
				// 如果当前状态是 已订阅
				if (State.SUBSCRIBED == publisher.state.get()) {
					// 调用当前状态的 发布错误 方法
					publisher.state.get().publishError(publisher, ex);
				}
			}
		},

		/**
		 * 订阅中
		 */
		SUBSCRIBING {
			@Override
			void request(WriteResultPublisher publisher, long n) {
				Operators.validate(n);
			}

			@Override
			void publishComplete(WriteResultPublisher publisher) {
				// 将 是否在订阅前已完成 标志设置为true
				publisher.completedBeforeSubscribed = true;
				// 如果当前状态是 已订阅
				if (State.SUBSCRIBED == publisher.state.get()) {
					// 调用当前状态的 发布完成 方法
					publisher.state.get().publishComplete(publisher);
				}
			}

			@Override
			void publishError(WriteResultPublisher publisher, Throwable ex) {
				// 将 订阅前抛出的错误 设置为当前的错误
				publisher.errorBeforeSubscribed = ex;
				// 如果当前状态是 已订阅
				if (State.SUBSCRIBED == publisher.state.get()) {
					// 调用当前状态的 发布错误 方法
					publisher.state.get().publishError(publisher, ex);
				}
			}
		},

		/**
		 * 已订阅
		 */
		SUBSCRIBED {
			@Override
			void request(WriteResultPublisher publisher, long n) {
				Operators.validate(n);
			}
		},

		/**
		 * 已完成
		 */
		COMPLETED {
			@Override
			void request(WriteResultPublisher publisher, long n) {
				// 忽略
			}

			@Override
			void cancel(WriteResultPublisher publisher) {
				// 忽略
			}

			@Override
			void publishComplete(WriteResultPublisher publisher) {
				// 忽略
			}

			@Override
			void publishError(WriteResultPublisher publisher, Throwable t) {
				// 忽略
			}
		};

		void subscribe(WriteResultPublisher publisher, Subscriber<? super Void> subscriber) {
			throw new IllegalStateException(toString());
		}

		void request(WriteResultPublisher publisher, long n) {
			throw new IllegalStateException(toString());
		}

		void cancel(WriteResultPublisher publisher) {
			// 如果成功将状态改变为 已完成
			if (publisher.changeState(this, COMPLETED)) {
				// 运行取消任务
				publisher.cancelTask.run();
			} else {
				// 否则，调用当前状态的 取消 方法
				publisher.state.get().cancel(publisher);
			}
		}

		void publishComplete(WriteResultPublisher publisher) {
			// 如果成功将状态改变为 已完成
			if (publisher.changeState(this, COMPLETED)) {
				// 获取订阅者
				Subscriber<? super Void> s = publisher.subscriber;
				// 断言订阅者不为空，如果为空则抛出异常
				Assert.state(s != null, "No subscriber");
				// 调用订阅者的 onComplete 方法
				s.onComplete();
			} else {
				// 否则，调用当前状态的 发布完成 方法
				publisher.state.get().publishComplete(publisher);
			}
		}

		void publishError(WriteResultPublisher publisher, Throwable t) {
			// 如果成功将状态改变为 已完成
			if (publisher.changeState(this, COMPLETED)) {
				// 获取订阅者
				Subscriber<? super Void> s = publisher.subscriber;
				// 断言订阅者不为空，如果为空则抛出异常
				Assert.state(s != null, "No subscriber");
				// 调用订阅者的onError方法
				s.onError(t);
			} else {
				// 否则，调用当前状态的 发布错误 方法
				publisher.state.get().publishError(publisher, t);
			}
		}
	}

}
