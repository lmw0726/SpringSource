/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.concurrent.ExecutionException;

/**
 * 抽象适配器类，将参数化为S类型的{@link ListenableFuture}适配为参数化为T类型的
 * {@code ListenableFuture}。所有方法都委托给adaptee，其中{@link #get()}、
 * {@link #get(long, java.util.concurrent.TimeUnit)}和
 * {@link ListenableFutureCallback#onSuccess(Object)}会调用{@link #adapt(Object)}
 * 方法来处理adaptee的结果。
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @param <T> 此{@code Future}的类型参数
 * @param <S> adaptee的{@code Future}的类型参数
 */
public abstract class ListenableFutureAdapter<T, S> extends FutureAdapter<T, S> implements ListenableFuture<T> {

	/**
	 * 使用给定的adaptee构造新的{@code ListenableFutureAdapter}。
	 * @param adaptee 要适配的future
	 */
	protected ListenableFutureAdapter(ListenableFuture<S> adaptee) {
		super(adaptee);
	}


	@Override
	public void addCallback(final ListenableFutureCallback<? super T> callback) {
		addCallback(callback, callback);
	}

	@Override
	public void addCallback(final SuccessCallback<? super T> successCallback, final FailureCallback failureCallback) {
		ListenableFuture<S> listenableAdaptee = (ListenableFuture<S>) getAdaptee();
		listenableAdaptee.addCallback(new ListenableFutureCallback<S>() {
			@Override
			public void onSuccess(@Nullable S result) {
				T adapted = null;
				if (result != null) {
					try {
						adapted = adaptInternal(result);
					}
					catch (ExecutionException ex) {
						Throwable cause = ex.getCause();
						onFailure(cause != null ? cause : ex);
						return;
					}
					catch (Throwable ex) {
						onFailure(ex);
						return;
					}
				}
				successCallback.onSuccess(adapted);
			}
			@Override
			public void onFailure(Throwable ex) {
				failureCallback.onFailure(ex);
			}
		});
	}

}
