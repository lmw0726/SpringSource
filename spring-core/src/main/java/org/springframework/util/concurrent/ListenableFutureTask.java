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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * {@link FutureTask}的扩展实现，同时实现了{@link ListenableFuture}接口。
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @param <T> 此Future的{@code get}方法返回的结果类型
 */
public class ListenableFutureTask<T> extends FutureTask<T> implements ListenableFuture<T> {

	private final ListenableFutureCallbackRegistry<T> callbacks = new ListenableFutureCallbackRegistry<>();


	/**
	 * 创建一个新的{@code ListenableFutureTask}，运行时将执行给定的{@link Callable}。
	 * @param callable 可调用任务
	 */
	public ListenableFutureTask(Callable<T> callable) {
		super(callable);
	}

	/**
	 * 创建一个{@code ListenableFutureTask}，运行时将执行给定的{@link Runnable}，
	 * 并安排在成功完成时{@link #get()}返回给定的结果。
	 * @param runnable 可运行任务
	 * @param result 成功完成时返回的结果
	 */
	public ListenableFutureTask(Runnable runnable, @Nullable T result) {
		super(runnable, result);
	}


	@Override
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		this.callbacks.addCallback(callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
		this.callbacks.addSuccessCallback(successCallback);
		this.callbacks.addFailureCallback(failureCallback);
	}

	@Override
	public CompletableFuture<T> completable() {
		CompletableFuture<T> completable = new DelegatingCompletableFuture<>(this);
		this.callbacks.addSuccessCallback(completable::complete);
		this.callbacks.addFailureCallback(completable::completeExceptionally);
		return completable;
	}


	@Override
	protected void done() {
		Throwable cause;
		try {
			T result = get();
			this.callbacks.success(result);
			return;
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return;
		}
		catch (ExecutionException ex) {
			cause = ex.getCause();
			if (cause == null) {
				cause = ex;
			}
		}
		catch (Throwable ex) {
			cause = ex;
		}
		this.callbacks.failure(cause);
	}

}
