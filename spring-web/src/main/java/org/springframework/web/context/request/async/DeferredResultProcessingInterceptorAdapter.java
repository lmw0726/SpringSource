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

package org.springframework.web.context.request.async;

import org.springframework.web.context.request.NativeWebRequest;

/**
 * 用于 {@link DeferredResultProcessingInterceptor} 接口的抽象适配器类，
 * 简化了各个方法的实现。
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 * @since 5.0 起弃用，其中 DeferredResultProcessingInterceptor 已有默认方法
 */
@Deprecated
public abstract class DeferredResultProcessingInterceptorAdapter implements DeferredResultProcessingInterceptor {

	/**
	 * 该实现为空。
	 */
	@Override
	public <T> void beforeConcurrentHandling(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {
	}

	/**
	 * 该实现为空。
	 */
	@Override
	public <T> void preProcess(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
	}

	/**
	 * 该实现为空。
	 */
	@Override
	public <T> void postProcess(NativeWebRequest request, DeferredResult<T> deferredResult,
								Object concurrentResult) throws Exception {
	}

	/**
	 * 该实现默认返回 {@code true}，允许其他拦截器有机会处理超时。
	 */
	@Override
	public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
		return true;
	}

	/**
	 * 该实现默认返回 {@code true}，允许其他拦截器有机会处理错误。
	 */
	@Override
	public <T> boolean handleError(NativeWebRequest request, DeferredResult<T> deferredResult, Throwable t)
			throws Exception {
		return true;
	}

	/**
	 * 该实现为空。
	 */
	@Override
	public <T> void afterCompletion(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
	}

}
