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

package org.springframework.web.servlet.function;

import org.springframework.util.Assert;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 表示过滤 {@linkplain HandlerFunction 处理函数} 的函数。
 *
 * @param <T> 要过滤的 {@linkplain HandlerFunction 处理函数} 的类型
 * @param <R> 函数响应的类型
 * @author Arjen Poutsma
 * @see RouterFunction#filter(HandlerFilterFunction)
 * @since 5.2
 */
@FunctionalInterface
public interface HandlerFilterFunction<T extends ServerResponse, R extends ServerResponse> {

	/**
	 * 将此过滤器应用于给定的处理函数。给定的 {@linkplain HandlerFunction 处理函数} 表示链中的下一个实体，
	 * 可以通过 {@linkplain HandlerFunction#handle(ServerRequest) 调用} 以继续到该实体，或者不调用以阻塞该链。
	 *
	 * @param request 请求
	 * @param next    链中的下一个处理程序或过滤器函数
	 * @return 过滤后的响应
	 * @throws Exception 如果过滤过程中发生异常
	 */
	R filter(ServerRequest request, HandlerFunction<T> next) throws Exception;

	/**
	 * 返回一个组合的过滤器函数，首先应用此过滤器，然后应用 {@code after} 过滤器。
	 *
	 * @param after 在应用此过滤器后要应用的过滤器
	 * @return 一个组合的过滤器，首先应用此函数，然后应用 {@code after} 函数
	 */
	default HandlerFilterFunction<T, R> andThen(HandlerFilterFunction<T, T> after) {
		Assert.notNull(after, "HandlerFilterFunction must not be null");
		// 返回一个过滤器函数
		return (request, next) -> {
			// 创建一个新的处理程序函数，该函数在原始处理程序函数后面执行过滤器
			HandlerFunction<T> nextHandler = handlerRequest -> after.filter(handlerRequest, next);
			// 调用当前过滤器的 filter 方法，传入请求和新的处理程序函数
			return filter(request, nextHandler);
		};
	}

	/**
	 * 将此过滤器应用于给定的处理函数，从而产生一个经过过滤的处理函数。
	 *
	 * @param handler 要过滤的处理函数
	 * @return 过滤后的处理函数
	 */
	default HandlerFunction<R> apply(HandlerFunction<T> handler) {
		Assert.notNull(handler, "HandlerFunction must not be null");
		return request -> this.filter(request, handler);
	}

	/**
	 * 将给定的请求处理函数适配为仅在 {@code ServerRequest} 上操作的过滤器函数。
	 *
	 * @param requestProcessor 请求处理函数
	 * @return 请求处理函数的过滤器适配
	 */
	static <T extends ServerResponse> HandlerFilterFunction<T, T>
	ofRequestProcessor(Function<ServerRequest, ServerRequest> requestProcessor) {

		Assert.notNull(requestProcessor, "Function must not be null");
		return (request, next) -> next.handle(requestProcessor.apply(request));
	}

	/**
	 * 将给定的响应处理函数适配为仅在 {@code ServerResponse} 上操作的过滤器函数。
	 *
	 * @param responseProcessor 响应处理函数
	 * @return 响应处理函数的过滤器适配
	 */
	static <T extends ServerResponse, R extends ServerResponse> HandlerFilterFunction<T, R>
	ofResponseProcessor(BiFunction<ServerRequest, T, R> responseProcessor) {

		Assert.notNull(responseProcessor, "Function must not be null");
		return (request, next) -> responseProcessor.apply(request, next.handle(request));
	}

	/**
	 * 将给定的谓词和响应提供函数适配为在给定异常上返回 {@code ServerResponse} 的过滤器函数。
	 *
	 * @param predicate    要匹配异常的谓词
	 * @param errorHandler 响应提供函数
	 * @return 错误处理程序的过滤器适配
	 */
	static <T extends ServerResponse> HandlerFilterFunction<T, T>
	ofErrorHandler(Predicate<Throwable> predicate, BiFunction<Throwable, ServerRequest, T> errorHandler) {

		Assert.notNull(predicate, "Predicate must not be null");
		Assert.notNull(errorHandler, "ErrorHandler must not be null");

		// 返回一个处理程序函数
		return (request, next) -> {
			try {
				// 调用下一个处理程序函数处理请求
				T t = next.handle(request);
				// 如果处理程序函数返回的对象是 ErrorHandlingServerResponse 类型
				if (t instanceof ErrorHandlingServerResponse) {
					// 将当前错误处理程序添加到 ErrorHandlingServerResponse 对象中
					((ErrorHandlingServerResponse) t).addErrorHandler(predicate, errorHandler);
				}
				// 返回处理结果
				return t;
			} catch (Throwable throwable) {
				// 如果捕获到异常
				// 如果异常满足条件
				if (predicate.test(throwable)) {
					// 调用错误处理函数处理异常
					return errorHandler.apply(throwable, request);
				} else {
					// 如果异常不满足条件，则重新抛出异常
					throw throwable;
				}
			}
		};
	}

}
