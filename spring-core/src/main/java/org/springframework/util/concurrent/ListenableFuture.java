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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * 扩展{@link Future}，使其能够接受完成回调。
 * 如果在添加回调时future已经完成，则立即触发回调。
 *
 * <p>灵感来自{@code com.google.common.util.concurrent.ListenableFuture}。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 4.0
 * @param <T> 此Future的{@code get}方法返回的结果类型
 */
public interface ListenableFuture<T> extends Future<T> {

	/**
	 * 注册给定的{@code ListenableFutureCallback}回调。
	 * @param callback 要注册的回调
	 */
	void addCallback(ListenableFutureCallback<? super T> callback);

	/**
	 * 支持Java 8 lambda表达式的替代方法，包含成功和失败回调。
	 * @param successCallback 成功回调
	 * @param failureCallback 失败回调
	 * @since 4.1
	 */
	void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback);


	/**
	 * 将此{@link ListenableFuture}暴露为JDK {@link CompletableFuture}。
	 * @since 5.0
	 */
	default CompletableFuture<T> completable() {
		CompletableFuture<T> completable = new DelegatingCompletableFuture<>(this);
		addCallback(completable::complete, completable::completeExceptionally);
		return completable;
	}

}
