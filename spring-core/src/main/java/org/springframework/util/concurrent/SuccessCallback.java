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

/**
 * 用于{@link ListenableFuture}的成功回调接口。
 *
 * @author Sebastien Deleuze
 * @since 4.1
 * @param <T> 结果类型
 */
@FunctionalInterface
public interface SuccessCallback<T> {

	/**
	 * 当{@link ListenableFuture}成功完成时调用。
	 * <p>注意：此方法抛出的异常将被忽略。
	 * @param result 结果(可为null)
	 */
	void onSuccess(@Nullable T result);

}
