/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.util.concurrent;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.concurrent.*;

/**
 * 一个可通过{@link #set(Object)}或{@link #setException(Throwable)}设置值的
 * {@link ListenableFuture}。它也可以被取消。
 *
 * <p>灵感来自{@code com.google.common.util.concurrent.SettableFuture}。
 *
 * @author Mattias Severson
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.1
 * @param <T> 此Future的{@code get}方法返回的结果类型
 */
public class SettableListenableFuture<T> implements ListenableFuture<T> {

	private static final Callable<Object> DUMMY_CALLABLE = () -> {
		throw new IllegalStateException("Should never be called");
	};


	private final SettableTask<T> settableTask = new SettableTask<>();


	/**
	 * 设置此future的值。如果值设置成功则返回{@code true}，
	 * 如果future已被设置或取消则返回{@code false}。
	 * @param value 要设置的值
	 * @return 如果值设置成功返回{@code true}，否则返回{@code false}
	 */
	public boolean set(@Nullable T value) {
		return this.settableTask.setResultValue(value);
	}

	/**
	 * 设置此future的异常。如果异常设置成功则返回{@code true}，
	 * 如果future已被设置或取消则返回{@code false}。
	 * @param exception 要设置的异常
	 * @return 如果异常设置成功返回{@code true}，否则返回{@code false}
	 * @throws IllegalArgumentException 如果异常参数为null
	 */
	public boolean setException(Throwable exception) {
		Assert.notNull(exception, "Exception must not be null");
		return this.settableTask.setExceptionResult(exception);
	}


	@Override
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		this.settableTask.addCallback(callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
		this.settableTask.addCallback(successCallback, failureCallback);
	}

	@Override
	public CompletableFuture<T> completable() {
		return this.settableTask.completable();
	}


	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		boolean cancelled = this.settableTask.cancel(mayInterruptIfRunning);
		if (cancelled && mayInterruptIfRunning) {
			interruptTask();
		}
		return cancelled;
	}

	@Override
	public boolean isCancelled() {
		return this.settableTask.isCancelled();
	}

	@Override
	public boolean isDone() {
		return this.settableTask.isDone();
	}

	/**
	 * 获取结果值。
	 * <p>如果值已通过{@link #set(Object)}设置，则返回该值；
	 * 如果已通过{@link #setException(Throwable)}设置异常，则抛出
	 * {@link java.util.concurrent.ExecutionException}；
	 * 如果future已被取消，则抛出
	 * {@link java.util.concurrent.CancellationException}。
	 * @return 与此future关联的值
	 */
	@Override
	public T get() throws InterruptedException, ExecutionException {
		return this.settableTask.get();
	}

	/**
	 * 获取结果值。
	 * <p>如果值已通过{@link #set(Object)}设置，则返回该值；
	 * 如果已通过{@link #setException(Throwable)}设置异常，则抛出
	 * {@link java.util.concurrent.ExecutionException}；
	 * 如果future已被取消，则抛出
	 * {@link java.util.concurrent.CancellationException}。
	 * @param timeout 最大等待时间
	 * @param unit 时间单位
	 * @return 与此future关联的值
	 */
	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return this.settableTask.get(timeout, unit);
	}

	/**
	 * 子类可以重写此方法以实现future计算的中断。
	 * 此方法会在成功调用{@link #cancel(boolean) cancel(true)}时自动调用。
	 * <p>默认实现为空。
	 */
	protected void interruptTask() {
	}


	private static class SettableTask<T> extends ListenableFutureTask<T> {

		@Nullable
		private volatile Thread completingThread;

		@SuppressWarnings("unchecked")
		public SettableTask() {
			super((Callable<T>) DUMMY_CALLABLE);
		}

		public boolean setResultValue(@Nullable T value) {
			set(value);
			return checkCompletingThread();
		}

		public boolean setExceptionResult(Throwable exception) {
			setException(exception);
			return checkCompletingThread();
		}

		@Override
		protected void done() {
			if (!isCancelled()) {
				// 由set/setException隐式调用：存储当前线程用于判断
				// 给定的结果是否实际触发了完成状态（因为FutureTask.set/setException
				// 不幸地没有暴露这个信息）
				this.completingThread = Thread.currentThread();
			}
			super.done();
		}

		private boolean checkCompletingThread() {
			boolean check = (this.completingThread == Thread.currentThread());
			if (check) {
				this.completingThread = null;  // 只有第一个匹配项才是真正的计数
			}
			return check;
		}
	}

}
