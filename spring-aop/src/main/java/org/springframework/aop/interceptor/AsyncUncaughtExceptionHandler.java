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

package org.springframework.aop.interceptor;

import java.lang.reflect.Method;

/**
 * 用于处理从异步方法抛出的未捕获异常的策略。
 *
 * <p>异步方法通常返回 {@link java.util.concurrent.Future} 实例，
 * 该实例提供对底层异常的访问。当方法不提供该返回类型时，
 * 可以使用此处理器来管理此类未捕获的异常。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@FunctionalInterface
public interface AsyncUncaughtExceptionHandler {

	/**
	 * 处理从异步方法抛出的给定未捕获异常。
	 * @param ex 从异步方法抛出的异常
	 * @param method 异步方法
	 * @param params 用于调用方法的参数
	 */
	void handleUncaughtException(Throwable ex, Method method, Object... params);

}
