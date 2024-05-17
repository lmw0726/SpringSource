/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.function;

import org.reactivestreams.Publisher;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * {@link AsyncServerResponse} 的默认实现。
 *
 * @author Arjen Poutsma
 * @since 5.3.2
 */
final class DefaultAsyncServerResponse extends ErrorHandlingServerResponse implements AsyncServerResponse {
	/**
	 * 是否存在响应式流
	 */
	static final boolean reactiveStreamsPresent = ClassUtils.isPresent(
			"org.reactivestreams.Publisher", DefaultAsyncServerResponse.class.getClassLoader());

	/**
	 * 服务响应Future
	 */
	private final CompletableFuture<ServerResponse> futureResponse;

	/**
	 * 过期时间
	 */
	@Nullable
	private final Duration timeout;


	private DefaultAsyncServerResponse(CompletableFuture<ServerResponse> futureResponse, @Nullable Duration timeout) {
		this.futureResponse = futureResponse;
		this.timeout = timeout;
	}

	@Override
	public ServerResponse block() {
		try {
			// 如果设置了超时时间
			if (this.timeout != null) {
				// 在指定的时间内获取 Future 对象的结果
				return this.futureResponse.get(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
			} else {
				// 获取 Future 对象的结果
				return this.futureResponse.get();
			}
		} catch (InterruptedException | ExecutionException | TimeoutException ex) {
			// 如果捕获到中断、执行异常或超时异常，则抛出 IllegalStateException 异常
			throw new IllegalStateException("Failed to get future response", ex);
		}
	}

	@Override
	public HttpStatus statusCode() {
		return delegate(ServerResponse::statusCode);
	}

	@Override
	public int rawStatusCode() {
		return delegate(ServerResponse::rawStatusCode);
	}

	@Override
	public HttpHeaders headers() {
		return delegate(ServerResponse::headers);
	}

	@Override
	public MultiValueMap<String, Cookie> cookies() {
		return delegate(ServerResponse::cookies);
	}

	private <R> R delegate(Function<ServerResponse, R> function) {
		ServerResponse response = this.futureResponse.getNow(null);
		if (response != null) {
			// 如果响应已经就绪，则将响应传递给函数处理
			return function.apply(response);
		} else {
			// 如果响应尚未完成，则抛出 IllegalStateException 异常
			throw new IllegalStateException("Future ServerResponse has not yet completed");
		}
	}

	@Nullable
	@Override
	public ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response, Context context)
			throws ServletException, IOException {

		writeAsync(request, response, createDeferredResult(request));
		return null;
	}

	static void writeAsync(HttpServletRequest request, HttpServletResponse response, DeferredResult<?> deferredResult)
			throws ServletException, IOException {

		// 获取 WebAsyncManager 实例
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		// 创建 AsyncWebRequest 实例
		AsyncWebRequest asyncWebRequest = WebAsyncUtils.createAsyncWebRequest(request, response);
		// 设置 AsyncWebRequest 到 WebAsyncManager 中
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		try {
			// 启动延迟结果处理
			asyncManager.startDeferredResultProcessing(deferredResult);
		} catch (IOException | ServletException ex) {
			// 捕获可能的异常并抛出
			throw ex;
		} catch (Exception ex) {
			// 捕获其他异常，并将其作为 ServletException 抛出
			throw new ServletException("Async processing failed", ex);
		}

	}

	private DeferredResult<ServerResponse> createDeferredResult(HttpServletRequest request) {
		// 创建一个 DeferredResult 实例
		DeferredResult<ServerResponse> result;
		if (this.timeout != null) {
			// 如果超时时间不为 null，则使用该超时时间创建 DeferredResult
			result = new DeferredResult<>(this.timeout.toMillis());
		} else {
			// 否则，创建一个没有超时时间的 DeferredResult
			result = new DeferredResult<>();
		}
		// 处理 futureResponse 的结果
		this.futureResponse.handle((value, ex) -> {
			if (ex != null) {
				// 如果发生异常
				if (ex instanceof CompletionException && ex.getCause() != null) {
					// 如果是 CompletionException 异常，且其 cause 不为 null，则将 cause 设置为异常
					ex = ex.getCause();
				}
				// 根据异常生成错误响应或错误结果
				ServerResponse errorResponse = errorResponse(ex, request);
				if (errorResponse != null) {
					// 如果能够生成错误响应，则设置该响应到 DeferredResult 中
					result.setResult(errorResponse);
				} else {
					// 否则，设置异常到 DeferredResult 中
					result.setErrorResult(ex);
				}
			} else {
				// 如果没有异常，则将结果设置到 DeferredResult 中
				result.setResult(value);
			}
			return null;
		});
		return result;
	}

	@SuppressWarnings({"unchecked"})
	public static AsyncServerResponse create(Object o, @Nullable Duration timeout) {
		Assert.notNull(o, "Argument to async must not be null");

		// 检查对象是否为 CompletableFuture 类型
		if (o instanceof CompletableFuture) {
			// 如果是 CompletableFuture 类型，将其转换为 CompletableFuture<ServerResponse>
			CompletableFuture<ServerResponse> futureResponse = (CompletableFuture<ServerResponse>) o;
			// 返回一个 DefaultAsyncServerResponse 实例，该实例基于 CompletableFuture 和超时时间创建
			return new DefaultAsyncServerResponse(futureResponse, timeout);
		} else if (reactiveStreamsPresent) {
			// 如果支持响应式流
			ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();
			// 获取对象的响应式适配器
			ReactiveAdapter publisherAdapter = registry.getAdapter(o.getClass());
			if (publisherAdapter != null) {
				// 如果存在适配器
				// 将对象转换为 Publisher<ServerResponse>
				Publisher<ServerResponse> publisher = publisherAdapter.toPublisher(o);
				// 获取 CompletableFuture 的适配器
				ReactiveAdapter futureAdapter = registry.getAdapter(CompletableFuture.class);
				if (futureAdapter != null) {
					// 如果存在 CompletableFuture 的适配器
					// 将 Publisher<ServerResponse> 转换为 CompletableFuture<ServerResponse>
					CompletableFuture<ServerResponse> futureResponse =
							(CompletableFuture<ServerResponse>) futureAdapter.fromPublisher(publisher);
					// 返回一个 DefaultAsyncServerResponse 实例，该实例基于 CompletableFuture 和超时时间创建
					return new DefaultAsyncServerResponse(futureResponse, timeout);
				}
			}
		}
		// 如果不是 CompletableFuture 类型且不支持响应式流，则抛出异常
		throw new IllegalArgumentException("Asynchronous type not supported: " + o.getClass());
	}


}
