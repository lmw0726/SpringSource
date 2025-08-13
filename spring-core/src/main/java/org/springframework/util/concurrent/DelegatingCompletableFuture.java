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

import org.springframework.util.Assert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * {@link CompletableFuture}的扩展实现，允许在取消{@link CompletableFuture}本身时，
 * 同时取消其委托对象。
 *
 * @author Juergen Hoeller
 * @since 5.0
 * @param <T> 此Future的{@code get}方法返回的结果类型
 */
class DelegatingCompletableFuture<T> extends CompletableFuture<T> {

	private final Future<T> delegate;


	public DelegatingCompletableFuture(Future<T> delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}


	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		boolean result = this.delegate.cancel(mayInterruptIfRunning);
		super.cancel(mayInterruptIfRunning);
		return result;
	}

}
