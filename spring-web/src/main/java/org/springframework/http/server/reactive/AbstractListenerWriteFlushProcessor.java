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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 一个替代 {@link AbstractListenerWriteProcessor} 的类，但用于写入带有刷新边界的 {@code Publisher<Publisher<T>>}，
 * 在每个嵌套的 Publisher 完成后强制刷新边界。
 *
 * @param <T> 向 {@link Subscriber} 发出信号的元素类型
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractListenerWriteFlushProcessor<T> implements Processor<Publisher<? extends T>, Void> {

	/**
	 * 用于调试Reactive Streams信号的特殊日志记录器。
	 *
	 * @see LogDelegateFactory#getHiddenLog(Class)
	 * @see AbstractListenerReadPublisher#rsReadLogger
	 * @see AbstractListenerWriteProcessor#rsWriteLogger
	 * @see WriteResultPublisher#rsWriteResultLogger
	 */
	protected static final Log rsWriteFlushLogger =
			LogDelegateFactory.getHiddenLog(AbstractListenerWriteFlushProcessor.class);


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
	 * 标志源是否已完成。
	 */
	private volatile boolean sourceCompleted;

	/**
	 * 当前的写处理器。
	 */
	@Nullable
	private volatile AbstractListenerWriteProcessor<?> currentWriteProcessor;

	/**
	 * 写结果发布者。
	 */
	private final WriteResultPublisher resultPublisher;

	/**
	 * 日志前缀。
	 */
	private final String logPrefix;


	/**
	 * 默认构造函数。
	 */
	public AbstractListenerWriteFlushProcessor() {
		this("");
	}

	/**
	 * 使用给定的日志前缀创建实例。
	 *
	 * @param logPrefix 日志前缀
	 * @since 5.1
	 */
	public AbstractListenerWriteFlushProcessor(String logPrefix) {
		this.logPrefix = logPrefix;
		this.resultPublisher = new WriteResultPublisher(logPrefix + "[WFP] ",
				() -> {
					// 取消订阅
					cancel();
					// 立即完成
					// 将当前状态设置为已完成状态，并获取旧的状态
					State oldState = this.state.getAndSet(State.COMPLETED);
					// 如果跟踪日志级别已启用
					if (rsWriteFlushLogger.isTraceEnabled()) {
						// 记录状态转变的跟踪日志
						rsWriteFlushLogger.trace(getLogPrefix() + oldState + " -> " + this.state);
					}
					// 传播到当前的“写入”处理器
					AbstractListenerWriteProcessor<?> writeProcessor = this.currentWriteProcessor;
					if (writeProcessor != null) {
						// 如果存在写入处理器，则执行取消和设置完成状态的方法。
						writeProcessor.cancelAndSetCompleted();
					}
					// 将当前写入处理器设置为空
					this.currentWriteProcessor = null;
				});
	}


	/**
	 * 返回日志前缀。
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
	public final void onNext(Publisher<? extends T> publisher) {
		// 如果跟踪日志级别已启用
		if (rsWriteFlushLogger.isTraceEnabled()) {
			// 记录onNext被调用的跟踪日志
			rsWriteFlushLogger.trace(getLogPrefix() + "onNext: \"write\" Publisher");
		}
		// 调用状态的onNext方法
		this.state.get().onNext(this, publisher);
	}

	/**
	 * 来自上游写入Publisher的错误信号。子类也使用它来委托容器的错误通知。
	 */
	@Override
	public final void onError(Throwable ex) {
		// 获取当前状态对象
		State state = this.state.get();

		// 如果跟踪日志级别已启用
		if (rsWriteFlushLogger.isTraceEnabled()) {
			// 记录错误信息，包括日志前缀、错误信息和当前状态
			rsWriteFlushLogger.trace(getLogPrefix() + "onError: " + ex + " [" + state + "]");
		}

		// 在当前状态对象中处理错误
		state.onError(this, ex);
	}

	/**
	 * 来自上游写入Publisher的完成信号。子类也使用它来委托容器的完成通知。
	 */
	@Override
	public final void onComplete() {
		// 获取当前状态对象
		State state = this.state.get();

		// 如果跟踪日志级别已启用
		if (rsWriteFlushLogger.isTraceEnabled()) {
			// 记录完成信息，包括日志前缀和当前状态
			rsWriteFlushLogger.trace(getLogPrefix() + "onComplete [" + state + "]");
		}

		// 在当前状态对象中处理完成事件
		state.onComplete(this);
	}

	/**
	 * 在刷新可能时调用，可以是在通过 {@link #isWritePossible()} 检查后的同一线程中，
	 * 或作为底层容器的回调。
	 */
	protected final void onFlushPossible() {
		this.state.get().onFlushPossible(this);
	}

	/**
	 * 取消上游链中的“写入”Publisher，例如由于Servlet容器的错误/完成通知。
	 * 通常应跟随调用 {@link #onError(Throwable)} 或 {@link #onComplete()} 来通知下游链，
	 * 除非取消操作来自下游。
	 */
	protected void cancel() {
		// 如果跟踪日志级别已启用
		if (rsWriteFlushLogger.isTraceEnabled()) {
			// 记录取消信息，包括日志前缀和当前状态
			rsWriteFlushLogger.trace(getLogPrefix() + "cancel [" + this.state + "]");
		}

		// 如果订阅对象不为空，则取消订阅
		if (this.subscription != null) {
			this.subscription.cancel();
		}
	}


	// 结果通知的Publisher实现...

	@Override
	public final void subscribe(Subscriber<? super Void> subscriber) {
		this.resultPublisher.subscribe(subscriber);
	}


	// 编写要实现的API方法或要重写的模板方法...

	/**
	 * 创建当前刷新边界的新处理器。
	 */
	protected abstract Processor<? super T, Void> createWriteProcessor();

	/**
	 * 检查是否可以进行写入/刷新操作。
	 */
	protected abstract boolean isWritePossible();

	/**
	 * 如果准备就绪则刷新输出，否则 {@link #isFlushPending()} 应返回 true。
	 * <p>这主要用于Servlet非阻塞I/O API，在没有准备好写入的情况下不能调用flush。
	 */
	protected abstract void flush() throws IOException;

	/**
	 * 检查是否有刷新操作待处理。
	 * <p>这主要用于Servlet非阻塞I/O API，在没有准备好写入的情况下不能调用flush。
	 */
	protected abstract boolean isFlushPending();

	/**
	 * 当刷新过程中发生错误时调用。
	 * <p>默认实现取消上游写入Publisher，并将错误传播给下游作为请求处理的结果。
	 */
	protected void flushingFailed(Throwable t) {
		// 取消订阅
		cancel();
		// 处理错误
		onError(t);
	}


	// 要在状态中使用的私有方法...

	private boolean changeState(State oldState, State newState) {
		// 比较并设置状态
		boolean result = this.state.compareAndSet(oldState, newState);

		// 如果状态设置成功，且日志记录器的追踪级别启用
		if (result && rsWriteFlushLogger.isTraceEnabled()) {
			// 记录状态转换信息，包括日志前缀、旧状态和新状态
			rsWriteFlushLogger.trace(getLogPrefix() + oldState + " -> " + newState);
		}

		// 返回操作结果
		return result;
	}

	private void flushIfPossible() {
		// 判断是否可以进行写操作
		boolean result = isWritePossible();

		// 如果日志记录器的追踪级别启用
		if (rsWriteFlushLogger.isTraceEnabled()) {
			// 记录是否可以写操作的信息，包括日志前缀和结果
			rsWriteFlushLogger.trace(getLogPrefix() + "isWritePossible[" + result + "]");
		}

		// 如果可以进行写操作，则调用onFlushPossible方法
		if (result) {
			onFlushPossible();
		}
	}


	/**
	 * 表示 {@link Processor} 的状态。
	 *
	 * <p><pre>
	 *         取消订阅
	 *            |
	 *            v
	 *         已请求 <-----> 已收到 --------+
	 *            |              |           |
	 *            |              v           |
	 *            |            冲洗          |
	 *            |              |           |
	 *            |              v           |
	 *            +--------> 已完成 <--------+
	 * </pre>
	 */
	private enum State {
		/**
		 * 取消订阅
		 */
		UNSUBSCRIBED {
			@Override
			public <T> void onSubscribe(AbstractListenerWriteFlushProcessor<T> processor, Subscription subscription) {
				Assert.notNull(subscription, "Subscription must not be null");
				// 如果处理器的状态成功更改为 已请求 状态
				if (processor.changeState(this, REQUESTED)) {
					// 设置处理器的订阅为当前订阅
					processor.subscription = subscription;
					// 请求一个数据项
					subscription.request(1);
				} else {
					// 否则，调用父类的onSubscribe方法
					super.onSubscribe(processor, subscription);
				}
			}

			@Override
			public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
				// 这可能发生在容器发送非常早期的完成通知时
				// 如果处理器的状态成功更改为 已完成 状态
				if (processor.changeState(this, COMPLETED)) {
					// 发布完成事件
					processor.resultPublisher.publishComplete();
				} else {
					// 否则，调用当前状态的onComplete方法
					processor.state.get().onComplete(processor);
				}
			}
		},

		/**
		 * 已请求
		 */
		REQUESTED {
			@Override
			public <T> void onNext(AbstractListenerWriteFlushProcessor<T> processor,
								   Publisher<? extends T> currentPublisher) {

				// 如果处理器成功将状态更改为 已收到
				if (processor.changeState(this, RECEIVED)) {
					// 创建一个写处理器
					Processor<? super T, Void> writeProcessor = processor.createWriteProcessor();

					// 将当前处理器的写处理器设置为创建的写处理器
					processor.currentWriteProcessor = (AbstractListenerWriteProcessor<?>) writeProcessor;

					// 将当前发布者订阅写处理器
					currentPublisher.subscribe(writeProcessor);

					// 将一个新的写结果订阅者订阅写处理器
					writeProcessor.subscribe(new WriteResultSubscriber(processor));
				}
			}

			@Override
			public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
				// 如果处理器成功将状态更改为 已完成
				if (processor.changeState(this, COMPLETED)) {
					// 发布结果完成信号给结果发布者
					processor.resultPublisher.publishComplete();
				} else {
					// 否则，调用当前状态对象的完成方法，处理完成事件
					processor.state.get().onComplete(processor);
				}
			}
		},

		/**
		 * 已收到
		 */
		RECEIVED {
			@Override
			public <T> void writeComplete(AbstractListenerWriteFlushProcessor<T> processor) {
				try {
					// 尝试执行处理器的刷新操作
					processor.flush();
				} catch (Throwable ex) {
					// 如果刷新操作抛出异常，处理器执行刷新失败的处理，并返回
					processor.flushingFailed(ex);
					return;
				}

				// 如果处理器成功将状态从当前状态更改为 已请求
				if (processor.changeState(this, REQUESTED)) {
					// 如果数据源已完成
					if (processor.sourceCompleted) {
						// 处理数据源完成事件
						handleSourceCompleted(processor);
					} else {
						// 否则，确保处理器有订阅对象
						Assert.state(processor.subscription != null, "No subscription");
						// 请求一个数据项
						processor.subscription.request(1);
					}
				}
			}

			@Override
			public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
				// 标记处理器的数据源已完成
				processor.sourceCompleted = true;

				// 如果处理器的当前状态为 已请求
				if (processor.state.get() == State.REQUESTED) {
					// 处理数据源完成事件
					handleSourceCompleted(processor);
				}
			}

			private <T> void handleSourceCompleted(AbstractListenerWriteFlushProcessor<T> processor) {
				// 如果仍有待刷新的操作
				if (processor.isFlushPending()) {
					// 确保最终刷新
					// 将请求状态更改为冲洗状态
					processor.changeState(State.REQUESTED, State.FLUSHING);
					// 刷新
					processor.flushIfPossible();
				} else if (processor.changeState(State.REQUESTED, State.COMPLETED)) {
					// 如果成功将状态从 已请求 更改为 已完成
					// 调用发布完成方法
					processor.resultPublisher.publishComplete();
				} else {
					// 如果状态未更改成功，则调用处理完成方法
					processor.state.get().onComplete(processor);
				}
			}
		},

		/**
		 * 冲洗
		 */
		FLUSHING {
			@Override
			public <T> void onFlushPossible(AbstractListenerWriteFlushProcessor<T> processor) {
				try {
					// 尝试执行处理器的刷新操作
					processor.flush();
				} catch (Throwable ex) {
					// 如果刷新操作抛出异常，处理器执行刷新失败的处理，并返回
					processor.flushingFailed(ex);
					return;
				}

				// 如果处理器成功将状态更改为 已完成
				if (processor.changeState(this, COMPLETED)) {
					// 发布结果完成信号给结果发布者
					processor.resultPublisher.publishComplete();
				} else {
					// 否则，调用当前状态对象的完成方法，处理完成事件
					processor.state.get().onComplete(processor);
				}
			}

			@Override
			public <T> void onNext(AbstractListenerWriteFlushProcessor<T> proc, Publisher<? extends T> pub) {
				// 忽略
			}

			@Override
			public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
				// 忽略
			}
		},

		/**
		 * 已完成
		 */
		COMPLETED {
			@Override
			public <T> void onNext(AbstractListenerWriteFlushProcessor<T> proc, Publisher<? extends T> pub) {
				// 忽略
			}

			@Override
			public <T> void onError(AbstractListenerWriteFlushProcessor<T> processor, Throwable t) {
				// 忽略
			}

			@Override
			public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
				// 忽略
			}
		};


		public <T> void onSubscribe(AbstractListenerWriteFlushProcessor<T> proc, Subscription subscription) {
			subscription.cancel();
		}

		public <T> void onNext(AbstractListenerWriteFlushProcessor<T> proc, Publisher<? extends T> pub) {
			throw new IllegalStateException(toString());
		}

		public <T> void onError(AbstractListenerWriteFlushProcessor<T> processor, Throwable ex) {
			// 如果处理器成功将状态更改为 已完成
			if (processor.changeState(this, COMPLETED)) {
				// 发布错误给结果发布者
				processor.resultPublisher.publishError(ex);
			} else {
				// 否则，调用当前状态对象的错误处理方法，处理异常
				processor.state.get().onError(processor, ex);
			}
		}

		public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
			throw new IllegalStateException(toString());
		}

		public <T> void writeComplete(AbstractListenerWriteFlushProcessor<T> processor) {
			throw new IllegalStateException(toString());
		}

		public <T> void onFlushPossible(AbstractListenerWriteFlushProcessor<T> processor) {
			// 忽略
		}


		/**
		 * 用于接收和委派来自当前Publisher（即当前刷新边界）的完成通知的订阅者。
		 */
		private static class WriteResultSubscriber implements Subscriber<Void> {
			/**
			 * 处理者
			 */
			private final AbstractListenerWriteFlushProcessor<?> processor;


			public WriteResultSubscriber(AbstractListenerWriteFlushProcessor<?> processor) {
				this.processor = processor;
			}

			@Override
			public void onSubscribe(Subscription subscription) {
				subscription.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(Void aVoid) {
			}

			@Override
			public void onError(Throwable ex) {
				// 如果日志记录器的追踪级别启用
				if (rsWriteFlushLogger.isTraceEnabled()) {
					// 记录当前写入发布者失败的信息
					rsWriteFlushLogger.trace(this.processor.getLogPrefix() + "current \"write\" Publisher failed: " + ex);
				}

				// 将当前处理器的写处理器设为null
				this.processor.currentWriteProcessor = null;

				// 取消处理器的操作
				this.processor.cancel();

				// 处理异常，调用处理器的错误处理方法
				this.processor.onError(ex);
			}

			@Override
			public void onComplete() {
				// 如果日志记录器的追踪级别启用
				if (rsWriteFlushLogger.isTraceEnabled()) {
					// 记录当前写入发布者完成的信息
					rsWriteFlushLogger.trace(
							this.processor.getLogPrefix() + "current \"write\" Publisher completed");
				}

				// 将当前处理器的写处理器设为null
				this.processor.currentWriteProcessor = null;

				// 调用当前状态对象的写完成方法，处理写操作完成事件
				this.processor.state.get().writeComplete(this.processor);
			}

			@Override
			public String toString() {
				return this.processor.getClass().getSimpleName() + "-WriteResultSubscriber";
			}
		}
	}

}
