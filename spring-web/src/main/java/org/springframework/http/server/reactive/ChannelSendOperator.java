/*
 * Copyright 2002-2019 the original author or authors.
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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.function.Function;

/**
 * 给定一个写函数，该函数接受一个源 {@code Publisher<T>} 来进行写操作，
 * 并返回 {@code Publisher<Void>} 作为结果，这个操作符帮助延迟写函数的调用，
 * 直到我们知道源发布者是否会在没有错误的情况下开始发布。如果第一个发出的信号是错误，
 * 则绕过写函数，并直接通过结果发布者发送错误。否则，调用写函数。
 *
 * @param <T> 发出元素的类型
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @since 5.0
 */
public class ChannelSendOperator<T> extends Mono<Void> implements Scannable {

	/**
	 * 写函数
	 */
	private final Function<Publisher<T>, Publisher<Void>> writeFunction;

	/**
	 * 数据源
	 */
	private final Flux<T> source;


	public ChannelSendOperator(Publisher<? extends T> source, Function<Publisher<T>, Publisher<Void>> writeFunction) {
		this.source = Flux.from(source);
		this.writeFunction = writeFunction;
	}


	@Override
	@Nullable
	@SuppressWarnings("rawtypes")
	public Object scanUnsafe(Attr key) {
		// 如果键是 预抓取取
		if (key == Attr.PREFETCH) {
			// 返回 Integer.MAX_VALUE 表示最大预取数量
			return Integer.MAX_VALUE;
		}

		// 如果键是 父属性
		if (key == Attr.PARENT) {
			// 返回 数据源
			return this.source;
		}

		// 如果键不是上述两个，则返回 null 表示未找到匹配的属性
		return null;
	}

	@Override
	public void subscribe(CoreSubscriber<? super Void> actual) {
		this.source.subscribe(new WriteBarrier(actual));
	}


	private enum State {

		/**
		 * 上游源尚未发出任何信号。
		 */
		NEW,

		/**
		 * 已经收到至少一个信号；我们准备调用写函数并继续实际写操作。
		 */
		FIRST_SIGNAL_RECEIVED,

		/**
		 * 写订阅者已订阅并请求；我们将发出缓存的信号。
		 */
		EMITTING_CACHED_SIGNALS,

		/**
		 * 写订阅者已订阅，缓存的信号已发给它；我们准备切换到简单的直通模式来处理所有剩余的信号。
		 */
		READY_TO_WRITE

	}


	/**
	 * 在写源和写订阅者（即 HTTP 服务器适配器）之间插入的屏障，
	 * 该屏障预取并等待第一个信号，然后再决定是否挂钩到写订阅者。
	 *
	 * <p>作为：
	 * <ul>
	 * <li>写源的订阅者。
	 * <li>写订阅者的订阅。
	 * <li>写订阅者的发布者。
	 * </ul>
	 *
	 * <p>还使用 {@link WriteCompletionBarrier} 来传达完成情况并检测完成订阅者的取消信号。
	 */
	private class WriteBarrier implements CoreSubscriber<T>, Subscription, Publisher<T> {

		/**
		 * 将信号桥接到完成订阅者和从完成订阅者传出
		 */
		private final WriteCompletionBarrier writeCompletionBarrier;

		/**
		 * 上游写源订阅
		 */
		@Nullable
		private Subscription subscription;

		/**
		 * 在 readyToWrite 之前缓存的数据项。
		 */
		@Nullable
		private T item;

		/**
		 * 在 readyToWrite 之前缓存的错误信号。
		 */
		@Nullable
		private Throwable error;

		/**
		 * 在 readyToWrite 之前缓存的 onComplete 信号。
		 */
		private boolean completed = false;

		/**
		 * 在发出缓存信号时递归需求。
		 */
		private long demandBeforeReadyToWrite;

		/**
		 * 当前状态。
		 */
		private State state = State.NEW;

		/**
		 * 来自 HTTP 服务器适配器的实际写订阅者。
		 */
		@Nullable
		private Subscriber<? super T> writeSubscriber;


		WriteBarrier(CoreSubscriber<? super Void> completionSubscriber) {
			this.writeCompletionBarrier = new WriteCompletionBarrier(completionSubscriber, this);
		}

		// Subscriber<T> 方法（我们是写源的订阅者）

		@Override
		public final void onSubscribe(Subscription s) {
			// 验证当前的订阅和新的订阅对象
			if (Operators.validate(this.subscription, s)) {
				// 如果验证通过，将新的订阅对象  保存到 上游写源订阅 中
				this.subscription = s;
				// 连接写入完成屏障
				this.writeCompletionBarrier.connect();
				// 请求一个数据项
				s.request(1);
			}
		}

		@Override
		public final void onNext(T item) {
			// 如果当前状态是 准备写入
			if (this.state == State.READY_TO_WRITE) {
				// 将 当前数据项 传入 必要的写入订阅服务器 进行处理
				requiredWriteSubscriber().onNext(item);
				return;
			}

			// FIXME 在可重入同步死锁的情况下重新访问
			// 进入同步块，以避免并发问题
			synchronized (this) {
				// 如果状态是否是 准备写入
				if (this.state == State.READY_TO_WRITE) {
					// 将 当前数据项 传入 必要的写入订阅服务器 进行处理
					requiredWriteSubscriber().onNext(item);
				} else if (this.state == State.NEW) {
					// 如果状态是 新建，则将 当前数据项 保存
					this.item = item;
					// 将状态更改为 收到第一个信号
					this.state = State.FIRST_SIGNAL_RECEIVED;
					Publisher<Void> result;
					try {
						// 进行写入操作，并将结果保存在 发行结果对象 中
						result = writeFunction.apply(this);
					} catch (Throwable ex) {
						// 如果捕获到异常，则调用 onError 方法处理异常
						this.writeCompletionBarrier.onError(ex);
						// 直接返回
						return;
					}
					// 订阅 写入完成障碍 处理写入结果
					result.subscribe(this.writeCompletionBarrier);
				} else {
					// 其他情况下（状态不符合预期）
					if (this.subscription != null) {
						// 如果有订阅对象，则取消订阅
						this.subscription.cancel();
					}
					// 调用 写入完成障碍 的 onError 方法，抛出非法状态异常
					this.writeCompletionBarrier.onError(new IllegalStateException("Unexpected item."));
				}
			}
		}

		private Subscriber<? super T> requiredWriteSubscriber() {
			Assert.state(this.writeSubscriber != null, "No write subscriber");
			return this.writeSubscriber;
		}

		@Override
		public final void onError(Throwable ex) {
			// 如果当前状态是 准备写入
			if (this.state == State.READY_TO_WRITE) {
				// 将 当前错误 传入 必要的写入订阅服务器 进行处理
				requiredWriteSubscriber().onError(ex);
				return;
			}

			// 进入同步块，以避免并发问题
			synchronized (this) {
				// 如果当前状态是 准备写入
				if (this.state == State.READY_TO_WRITE) {
					// 将 当前错误 传入 必要的写入订阅服务器 进行处理
					requiredWriteSubscriber().onError(ex);
				} else if (this.state == State.NEW) {
					// 如果状态是 新建
					// 将状态更改为 收到第一个信号
					this.state = State.FIRST_SIGNAL_RECEIVED;
					// 调用 写入完成障碍 的 onError方法处理 当前错误
					this.writeCompletionBarrier.onError(ex);
				} else {
					// 其他情况下（状态不符合预期），将错误保存到 当前类的错误变量 中
					this.error = ex;
				}
			}
		}

		@Override
		public final void onComplete() {
			// 如果当前状态是 准备写入
			if (this.state == State.READY_TO_WRITE) {
				// 则调用 必要的写入订阅服务器 的完成方法
				requiredWriteSubscriber().onComplete();
				// 直接返回
				return;
			}
			// 进入同步块，以避免并发问题
			synchronized (this) {
				// 如果当前状态是 准备写入
				if (this.state == State.READY_TO_WRITE) {
					// 则调用 必要的写入订阅服务器 的完成方法
					requiredWriteSubscriber().onComplete();
				} else if (this.state == State.NEW) {
					// 如果当前状态是 新建
					// 将 完成 标志设置为 true
					this.completed = true;
					// 将状态更改为 收到第一个信号
					this.state = State.FIRST_SIGNAL_RECEIVED;
					Publisher<Void> result;
					try {
						// 进行写入操作，并将结果保存在 发行结果对象 中
						result = writeFunction.apply(this);
					} catch (Throwable ex) {
						// 如果捕获到异常，则调用 onError 方法处理异常
						this.writeCompletionBarrier.onError(ex);
						// 直接返回
						return;
					}
					// 订阅 写入完成障碍 处理写入结果
					result.subscribe(this.writeCompletionBarrier);
				} else {
					// 如果状态不是 准备写入 也不是 新建
					// 将 完成 标志设置为 true
					this.completed = true;
				}
			}
		}

		@Override
		public Context currentContext() {
			return this.writeCompletionBarrier.currentContext();
		}

		// Subscription 方法（我们是写订阅者的订阅）

		@Override
		public void request(long n) {
			// 获取订阅方
			Subscription s = this.subscription;
			if (s == null) {
				// 如果不存在订阅方，直接返回
				return;
			}
			// 如果当前状态是准备写入状态
			if (this.state == State.READY_TO_WRITE) {
				// 调用订阅方的请求方法处理 n 个数据
				s.request(n);
				// 直接返回
				return;
			}
			// 进入同步块，以避免并发问题
			synchronized (this) {
				// 如果写订阅者不为空
				if (this.writeSubscriber != null) {
					// 如果当前状态是 发射缓存信号
					if (this.state == State.EMITTING_CACHED_SIGNALS) {
						// 将 准备写入之前的需求 设置为 n 个
						this.demandBeforeReadyToWrite = n;
						// 直接结束
						return;
					}
					try {
						// 如果当前状态是发射缓存信号
						this.state = State.EMITTING_CACHED_SIGNALS;
						if (emitCachedSignals()) {
							// 如果发送缓存信号成功，直接返回
							return;
						}
						// 否则累加之前的 准备写入之前的需求 数量-1
						n = n + this.demandBeforeReadyToWrite - 1;
						if (n == 0) {
							// 如果累加后的数量为0， 直接返回
							return;
						}
					} finally {
						// 将状态设置为准备写入状态
						this.state = State.READY_TO_WRITE;
					}
				}
			}
			// 调用订阅方的请求方法处理 n 个数据
			s.request(n);
		}

		private boolean emitCachedSignals() {
			// 如果存在错误
			if (this.error != null) {
				try {
					// 调用必要的写订阅者处理 当前错误
					requiredWriteSubscriber().onError(this.error);
				} finally {
					// 释放缓存的数据项
					releaseCachedItem();
				}
				// 返回true，表示成功释放缓存信号
				return true;
			}
			// 获取当前数据项
			T item = this.item;
			// 将当前数据项缓存清空
			this.item = null;
			if (item != null) {
				// 如果数据项不为空，则调用必要的写订阅者处理该数据项
				requiredWriteSubscriber().onNext(item);
			}
			if (this.completed) {
				// 如果已经处理完成，则调用必要的写订阅者的完成方法。
				requiredWriteSubscriber().onComplete();
				// 返回true，表示成功释放缓存信号
				return true;
			}
			// 返回false，表示没有释放缓存信号
			return false;
		}

		@Override
		public void cancel() {
			// 获取当前订阅的引用
			Subscription s = this.subscription;

			// 如果当前存在订阅
			if (s != null) {
				// 将订阅置为 null，防止重复取消
				this.subscription = null;
				try {
					// 尝试取消订阅
					s.cancel();
				} finally {
					// 无论取消订阅是否成功，都会执行释放缓存项目的方法
					releaseCachedItem();
				}
			}
		}

		private void releaseCachedItem() {
			// 进入同步块，以避免并发问题
			synchronized (this) {
				// 获取当前缓存的项目
				Object item = this.item;

				// 如果缓存的项目是 DataBuffer 类型
				if (item instanceof DataBuffer) {
					// 释放 数据缓冲区 对象
					DataBufferUtils.release((DataBuffer) item);
				}

				// 将缓存的项目置为 null
				this.item = null;
			}
		}

		// Publisher<T> 方法（我们是写订阅者的发布者）

		@Override
		public void subscribe(Subscriber<? super T> writeSubscriber) {
			// 进入同步块，以避免并发问题
			synchronized (this) {
				// 断言检查，确保当前没有写入订阅者，如果有则抛出异常
				Assert.state(this.writeSubscriber == null, "Only one write subscriber supported");

				// 设置新的写入订阅者
				this.writeSubscriber = writeSubscriber;

				// 如果当前存在错误或已完成
				if (this.error != null || this.completed) {
					// 通知写入订阅者使用一个空的订阅
					this.writeSubscriber.onSubscribe(Operators.emptySubscription());

					// 发出缓存的信号（错误或完成）
					emitCachedSignals();
				} else {
					// 否则，将当前对象作为订阅通知写入订阅者
					this.writeSubscriber.onSubscribe(this);
				}
			}
		}
	}


	/**
	 * 在 WriteBarrier 本身和实际完成订阅者之间需要一个额外的屏障。
	 *
	 * <p>completionSubscriber 最初订阅到 WriteBarrier。之后在收到第一个信号后，我们需要一个更多的订阅者实例
	 * （每个规范只能订阅一次）来订阅写函数并切换到从它委派完成信号。
	 */
	private class WriteCompletionBarrier implements CoreSubscriber<Void>, Subscription {

		/**
		 * 下游写完成订阅者
		 */
		private final CoreSubscriber<? super Void> completionSubscriber;
		/**
		 * 写屏障
		 */
		private final WriteBarrier writeBarrier;

		/**
		 * 订阅方
		 */
		@Nullable
		private Subscription subscription;

		/**
		 * 构造函数，初始化 WriteCompletionBarrier。
		 *
		 * @param subscriber   下游写完成订阅者
		 * @param writeBarrier WriteBarrier 实例
		 */
		public WriteCompletionBarrier(CoreSubscriber<? super Void> subscriber, WriteBarrier writeBarrier) {
			this.completionSubscriber = subscriber;
			this.writeBarrier = writeBarrier;
		}


		/**
		 * 连接底层完成订阅者到此屏障，以便跟踪取消信号并将其传递给写屏障。
		 */
		public void connect() {
			this.completionSubscriber.onSubscribe(this);
		}

		// 订阅者方法（我们是写函数的订阅者）。

		@Override
		public void onSubscribe(Subscription subscription) {
			// 将传入的订阅对象保存到成员变量 subscription 中
			this.subscription = subscription;
			// 请求尽可能多的数据项
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Void aVoid) {
		}

		@Override
		public void onError(Throwable ex) {
			try {
				// 通知 下游写完成订阅者 发生了错误
				this.completionSubscriber.onError(ex);
			} finally {
				// 最终释放缓存的项目
				this.writeBarrier.releaseCachedItem();
			}
		}

		@Override
		public void onComplete() {
			this.completionSubscriber.onComplete();
		}

		@Override
		public Context currentContext() {
			return this.completionSubscriber.currentContext();
		}


		@Override
		public void request(long n) {
			// 忽略：我们不产生数据
		}

		@Override
		public void cancel() {
			// 取消写入屏障的操作
			this.writeBarrier.cancel();

			// 获取当前的订阅对象
			Subscription subscription = this.subscription;

			// 如果订阅对象不为空，则取消该订阅
			if (subscription != null) {
				subscription.cancel();
			}
		}
	}

}
