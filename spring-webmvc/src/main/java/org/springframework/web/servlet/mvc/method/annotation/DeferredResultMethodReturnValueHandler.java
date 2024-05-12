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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

/**
 * 处理类型为 {@link DeferredResult}、{@link ListenableFuture} 和 {@link CompletionStage} 的返回值。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class DeferredResultMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// 获取返回类型
		Class<?> type = returnType.getParameterType();

		// 检查返回类型是否为 DeferredResult、ListenableFuture 或 CompletionStage 类型（接口）及其子类
		return (DeferredResult.class.isAssignableFrom(type) ||
				ListenableFuture.class.isAssignableFrom(type) ||
				CompletionStage.class.isAssignableFrom(type));
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		// 如果返回值为空，则标记请求已处理并返回
		if (returnValue == null) {
			mavContainer.setRequestHandled(true);
			return;
		}

		// 声明一个 DeferredResult 对象
		DeferredResult<?> result;

		if (returnValue instanceof DeferredResult) {
			// 如果返回值是 DeferredResult 类型，直接使用
			result = (DeferredResult<?>) returnValue;
		} else if (returnValue instanceof ListenableFuture) {
			// 如果返回值是 ListenableFuture 类型，进行适配，ListenableFuture转为DeferredResult
			result = adaptListenableFuture((ListenableFuture<?>) returnValue);
		} else if (returnValue instanceof CompletionStage) {
			// 如果返回值是 CompletionStage 类型，进行适配，CompletionStage转为DeferredResult
			result = adaptCompletionStage((CompletionStage<?>) returnValue);
		} else {
			// 不应该发生的情况...
			throw new IllegalStateException("Unexpected return value type: " + returnValue);
		}

		// 启动 DeferredResult 异步处理
		WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(result, mavContainer);
	}

	private DeferredResult<Object> adaptListenableFuture(ListenableFuture<?> future) {
		// 创建一个 DeferredResult 对象
		DeferredResult<Object> result = new DeferredResult<>();

		// 添加 ListenableFuture 的回调函数
		future.addCallback(new ListenableFutureCallback<Object>() {
			@Override
			public void onSuccess(@Nullable Object value) {
				// 当 ListenableFuture 成功时，设置 DeferredResult 的结果
				result.setResult(value);
			}

			@Override
			public void onFailure(Throwable ex) {
				// 当 ListenableFuture 失败时，设置 DeferredResult 的错误结果
				result.setErrorResult(ex);
			}
		});

		// 返回设置了回调函数的 DeferredResult 对象
		return result;
	}

	private DeferredResult<Object> adaptCompletionStage(CompletionStage<?> future) {
		// 创建一个 DeferredResult 对象
		DeferredResult<Object> result = new DeferredResult<>();

		// 使用 handle 方法处理 ListenableFuture 的结果或异常
		future.handle((BiFunction<Object, Throwable, Object>) (value, ex) -> {
			// 如果有异常
			if (ex != null) {
				// 如果异常是 CompletionException，则获取其原因
				if (ex instanceof CompletionException && ex.getCause() != null) {
					ex = ex.getCause();
				}
				// 设置 DeferredResult 的错误结果
				result.setErrorResult(ex);
			} else {
				// 设置 DeferredResult 的结果
				result.setResult(value);
			}
			return null;
		});

		// 返回设置了处理函数的 DeferredResult 对象
		return result;
	}

}
