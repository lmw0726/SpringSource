/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.cache.interceptor;

import org.springframework.lang.Nullable;

/**
 * 抽象一次缓存操作的调用。
 *
 * <p>不提供传递受检异常（checked exception）的方式，但提供了一个特殊的异常，
 * 该异常用于包装底层调用过程中抛出的任何异常。
 * 调用方需要专门处理这一类型的异常。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@FunctionalInterface
public interface CacheOperationInvoker {

	/**
	 * 调用由本实例定义的缓存操作。将调用过程中抛出的任何异常
	 * 包装在一个 {@link ThrowableWrapper} 中。
	 * @return 操作的结果
	 * @throws ThrowableWrapper 如果在调用操作时发生错误
	 */
	@Nullable
	Object invoke() throws ThrowableWrapper;


	/**
	 * 包装调用 {@link #invoke()} 时抛出的任何异常。
	 */
	@SuppressWarnings("serial")
	class ThrowableWrapper extends RuntimeException {

		private final Throwable original;

		public ThrowableWrapper(Throwable original) {
			super(original.getMessage(), original);
			this.original = original;
		}

		public Throwable getOriginal() {
			return this.original;
		}
	}

}
