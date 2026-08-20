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

package org.springframework.scheduling.annotation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SuccessCallback;

/**
 * 一个直通的 {@code Future} 句柄，可用于声明了 {@code Future} 返回类型
 * 以进行异步执行的方法签名。
 *
 * <p>从 Spring 4.1 开始，该类实现了 {@link ListenableFuture}，而不仅仅是
 * 普通的 {@link java.util.concurrent.Future}，并在 {@code @Async} 处理中
 * 提供了相应的支持。
 *
 * <p>从 Spring 4.2 开始，该类还支持将执行异常传递回调用方。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 * @param <V> 值类型
 * @see Async
 * @see #forValue(Object)
 * @see #forExecutionException(Throwable)
 */
public class AsyncResult<V> implements ListenableFuture<V> {

	@Nullable
	private final V value;

	@Nullable
	private final Throwable executionException;


	/**
	 * 创建一个新的 AsyncResult 持有者。
	 * @param value 要传递的值
	 */
	public AsyncResult(@Nullable V value) {
		this(value, null);
	}

	/**
	 * 创建一个新的 AsyncResult 持有者。
	 * @param value 要传递的值
	 */
	private AsyncResult(@Nullable V value, @Nullable Throwable ex) {
		this.value = value;
		this.executionException = ex;
	}


	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	@Nullable
	public V get() throws ExecutionException {
		if (this.executionException != null) {
			throw (this.executionException instanceof ExecutionException ?
					(ExecutionException) this.executionException :
					new ExecutionException(this.executionException));
		}
		return this.value;
	}

	@Override
	@Nullable
	public V get(long timeout, TimeUnit unit) throws ExecutionException {
		return get();
	}

	@Override
	public void addCallback(ListenableFutureCallback<? super V> callback) {
		addCallback(callback, callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super V> successCallback, FailureCallback failureCallback) {
		try {
			if (this.executionException != null) {
				failureCallback.onFailure(exposedException(this.executionException));
			}
			else {
				successCallback.onSuccess(this.value);
			}
		}
		catch (Throwable ex) {
			// 忽略
		}
	}

	@Override
	public CompletableFuture<V> completable() {
		if (this.executionException != null) {
			CompletableFuture<V> completable = new CompletableFuture<>();
			completable.completeExceptionally(exposedException(this.executionException));
			return completable;
		}
		else {
			return CompletableFuture.completedFuture(this.value);
		}
	}


	/**
	 * 创建一个新的异步结果，使其通过 {@link Future#get()} 暴露给定的值。
	 * @param value 要暴露的值
	 * @since 4.2
	 * @see Future#get()
	 */
	public static <V> ListenableFuture<V> forValue(V value) {
		return new AsyncResult<>(value, null);
	}

	/**
	 * 创建一个新的异步结果，使其通过 {@link Future#get()} 将给定的异常
	 * 作为 {@link ExecutionException} 暴露出来。
	 * @param ex 要暴露的异常（可以是预先构建的 {@link ExecutionException}
	 * 或需要包装在 {@link ExecutionException} 中的原因）
	 * @since 4.2
	 * @see ExecutionException
	 */
	public static <V> ListenableFuture<V> forExecutionException(Throwable ex) {
		return new AsyncResult<>(null, ex);
	}

	/**
	 * 确定要暴露的异常：可以是给定 {@link ExecutionException} 的原因，
	 * 也可以是原始异常本身。
	 * @param original 传入 {@link #forExecutionException} 的原始异常
	 * @return 要暴露的异常
	 */
	private static Throwable exposedException(Throwable original) {
		if (original instanceof ExecutionException) {
			Throwable cause = original.getCause();
			if (cause != null) {
				return cause;
			}
		}
		return original;
	}

}
