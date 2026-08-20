/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.lang.Nullable;

/**
 * 一个便捷的 {@link AsyncConfigurer} 实现，所有方法均使用默认值。
 * 提供了向后兼容的方式，无需直接实现 {@link AsyncConfigurer}。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class AsyncConfigurerSupport implements AsyncConfigurer {

	@Override
	public Executor getAsyncExecutor() {
		return null;
	}

	@Override
	@Nullable
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return null;
	}

}
