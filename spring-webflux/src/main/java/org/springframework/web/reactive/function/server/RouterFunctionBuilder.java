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

package org.springframework.web.reactive.function.server;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * {@link RouterFunctions.Builder} 的默认实现。
 *
 * @author Arjen Poutsma
 * @since 5.1
 */
class RouterFunctionBuilder implements RouterFunctions.Builder {

	/**
	 * 路由函数的列表，用于存储多个RouterFunction<ServerResponse>对象。
	 */
	private final List<RouterFunction<ServerResponse>> routerFunctions = new ArrayList<>();

	/**
	 * 过滤器函数的列表，用于存储多个HandlerFilterFunction<ServerResponse, ServerResponse>对象。
	 */
	private final List<HandlerFilterFunction<ServerResponse, ServerResponse>> filterFunctions = new ArrayList<>();

	/**
	 * 错误处理器函数的列表，用于存储多个HandlerFilterFunction<ServerResponse, ServerResponse>对象，处理错误情况。
	 */
	private final List<HandlerFilterFunction<ServerResponse, ServerResponse>> errorHandlers = new ArrayList<>();


	@Override
	public RouterFunctions.Builder add(RouterFunction<ServerResponse> routerFunction) {
		Assert.notNull(routerFunction, "RouterFunction must not be null");
		this.routerFunctions.add(routerFunction);
		return this;
	}

	/**
	 * 添加请求断言和处理函数构建一个 RouterFunction，并将其加入到路由函数列表中。
	 *
	 * @param predicate       请求断言，用于匹配请求
	 * @param handlerFunction 处理函数，用于处理匹配到的请求
	 * @return 返回修改后的 RouterFunctions.Builder 对象
	 */
	private RouterFunctions.Builder add(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction) {
		// 创建一个 RouterFunction 并添加到路由函数列表中
		this.routerFunctions.add(RouterFunctions.route(predicate, handlerFunction));
		return this;
	}

	// GET请求

	@Override
	public RouterFunctions.Builder GET(HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.GET), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder GET(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.GET).and(predicate), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder GET(String pattern, HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.GET(pattern), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder GET(String pattern, RequestPredicate predicate,
									   HandlerFunction<ServerResponse> handlerFunction) {

		return add(RequestPredicates.GET(pattern).and(predicate), handlerFunction);
	}

	// HEAD请求

	@Override
	public RouterFunctions.Builder HEAD(HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.HEAD), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder HEAD(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.HEAD).and(predicate), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder HEAD(String pattern, HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.HEAD(pattern), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder HEAD(String pattern, RequestPredicate predicate,
										HandlerFunction<ServerResponse> handlerFunction) {

		return add(RequestPredicates.HEAD(pattern).and(predicate), handlerFunction);
	}

	// POST请求

	@Override
	public RouterFunctions.Builder POST(HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.POST), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder POST(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.POST).and(predicate), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder POST(String pattern, HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.POST(pattern), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder POST(String pattern, RequestPredicate predicate,
										HandlerFunction<ServerResponse> handlerFunction) {

		return add(RequestPredicates.POST(pattern).and(predicate), handlerFunction);
	}

	// PUT请求

	@Override
	public RouterFunctions.Builder PUT(HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.PUT), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder PUT(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.PUT).and(predicate), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder PUT(String pattern, HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.PUT(pattern), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder PUT(String pattern, RequestPredicate predicate,
									   HandlerFunction<ServerResponse> handlerFunction) {

		return add(RequestPredicates.PUT(pattern).and(predicate), handlerFunction);
	}

	// PATCH请求

	@Override
	public RouterFunctions.Builder PATCH(HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.PATCH), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder PATCH(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.PATCH).and(predicate), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder PATCH(String pattern, HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.PATCH(pattern), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder PATCH(String pattern, RequestPredicate predicate,
										 HandlerFunction<ServerResponse> handlerFunction) {

		return add(RequestPredicates.PATCH(pattern).and(predicate), handlerFunction);
	}

	// DELETE请求

	@Override
	public RouterFunctions.Builder DELETE(HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.DELETE), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder DELETE(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.DELETE).and(predicate), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder DELETE(String pattern, HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.DELETE(pattern), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder DELETE(String pattern, RequestPredicate predicate,
										  HandlerFunction<ServerResponse> handlerFunction) {

		return add(RequestPredicates.DELETE(pattern).and(predicate), handlerFunction);
	}

	// OPTIONS请求

	@Override
	public RouterFunctions.Builder OPTIONS(HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.OPTIONS), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder OPTIONS(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.OPTIONS).and(predicate), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder OPTIONS(String pattern, HandlerFunction<ServerResponse> handlerFunction) {
		return add(RequestPredicates.OPTIONS(pattern), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder OPTIONS(String pattern, RequestPredicate predicate,
										   HandlerFunction<ServerResponse> handlerFunction) {

		return add(RequestPredicates.OPTIONS(pattern).and(predicate), handlerFunction);
	}

	// 其他请求

	/**
	 * 根据请求断言和处理函数构建一个路由，并将其添加到路由函数列表中。
	 *
	 * @param predicate       请求断言，用于匹配请求
	 * @param handlerFunction 处理函数，用于处理匹配到的请求
	 * @return 返回修改后的 RouterFunctions.Builder 对象
	 */
	@Override
	public RouterFunctions.Builder route(RequestPredicate predicate,
										 HandlerFunction<ServerResponse> handlerFunction) {
		return add(RouterFunctions.route(predicate, handlerFunction));
	}

	@Override
	public RouterFunctions.Builder resources(String pattern, Resource location) {
		return add(RouterFunctions.resources(pattern, location));
	}

	@Override
	public RouterFunctions.Builder resources(Function<ServerRequest, Mono<Resource>> lookupFunction) {
		return add(RouterFunctions.resources(lookupFunction));
	}

	/**
	 * 创建一个嵌套的路由，并将其添加到路由函数列表中。
	 *
	 * @param predicate       请求断言，用于匹配请求以确定是否应用嵌套路由
	 * @param builderConsumer 用于配置嵌套路由的 Consumer 函数
	 * @return 返回修改后的 RouterFunctions.Builder 对象
	 */
	@Override
	public RouterFunctions.Builder nest(RequestPredicate predicate, Consumer<RouterFunctions.Builder> builderConsumer) {
		Assert.notNull(builderConsumer, "Consumer must not be null");

		// 创建一个嵌套路由的 Builder
		RouterFunctionBuilder nestedBuilder = new RouterFunctionBuilder();
		// 传入Consumer函数以配置嵌套路由
		builderConsumer.accept(nestedBuilder);
		// 构建嵌套路由
		RouterFunction<ServerResponse> nestedRoute = nestedBuilder.build();
		// 添加嵌套路由到路由函数列表中
		this.routerFunctions.add(RouterFunctions.nest(predicate, nestedRoute));
		return this;
	}

	/**
	 * 创建一个嵌套的路由，并将其添加到路由函数列表中。
	 *
	 * @param predicate                请求断言，用于匹配请求以确定是否应用嵌套路由
	 * @param routerFunctionSupplier   用于提供嵌套路由的 Supplier 函数
	 * @return 返回修改后的 RouterFunctions.Builder 对象
	 */
	@Override
	public RouterFunctions.Builder nest(RequestPredicate predicate, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier) {
		Assert.notNull(routerFunctionSupplier, "RouterFunction Supplier must not be null");

		// 从 Supplier 获取嵌套路由
		RouterFunction<ServerResponse> nestedRoute = routerFunctionSupplier.get();
		// 将嵌套路由添加到路由函数列表中
		this.routerFunctions.add(RouterFunctions.nest(predicate, nestedRoute));
		return this;
	}

	@Override
	public RouterFunctions.Builder path(String pattern,
										Consumer<RouterFunctions.Builder> builderConsumer) {

		return nest(RequestPredicates.path(pattern), builderConsumer);
	}

	@Override
	public RouterFunctions.Builder path(String pattern,
										Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier) {

		return nest(RequestPredicates.path(pattern), routerFunctionSupplier);
	}

	@Override
	public RouterFunctions.Builder filter(HandlerFilterFunction<ServerResponse, ServerResponse> filterFunction) {
		Assert.notNull(filterFunction, "HandlerFilterFunction must not be null");

		this.filterFunctions.add(filterFunction);
		return this;
	}

	/**
	 * 在处理请求之前对请求进行处理。
	 *
	 * @param requestProcessor 请求处理函数，用于在处理请求之前对请求进行处理
	 * @return 返回修改后的 RouterFunctions.Builder 对象
	 */
	@Override
	public RouterFunctions.Builder before(Function<ServerRequest, ServerRequest> requestProcessor) {
		Assert.notNull(requestProcessor, "RequestProcessor must not be null");
		return filter((request, next) -> next.handle(requestProcessor.apply(request)));
	}

	/**
	 * 在处理请求后对响应进行处理。
	 *
	 * @param responseProcessor 响应处理函数，用于在处理请求后对响应进行处理
	 * @return 返回修改后的 RouterFunctions.Builder 对象
	 */
	@Override
	public RouterFunctions.Builder after(
			BiFunction<ServerRequest, ServerResponse, ServerResponse> responseProcessor) {

		Assert.notNull(responseProcessor, "ResponseProcessor must not be null");
		return filter((request, next) -> next.handle(request)
				.map(serverResponse -> responseProcessor.apply(request, serverResponse)));
	}

	/**
	 * 在处理过程中出现错误时进行错误处理。
	 *
	 * @param predicate         错误条件断言，用于匹配错误类型
	 * @param responseProvider  响应提供函数，用于根据错误信息提供新的 ServerResponse
	 * @return 返回修改后的 RouterFunctions.Builder 对象
	 */
	@Override
	public RouterFunctions.Builder onError(Predicate<? super Throwable> predicate,
										   BiFunction<? super Throwable, ServerRequest, Mono<ServerResponse>> responseProvider) {

		Assert.notNull(predicate, "Predicate must not be null");
		Assert.notNull(responseProvider, "ResponseProvider must not be null");

		// 在错误处理器列表的头部添加一个处理函数
		this.errorHandlers.add(0, (request, next) -> next.handle(request)
				// 如果出现错误，根据错误信息提供新的响应
				.onErrorResume(predicate, t -> responseProvider.apply(t, request)));
		return this;
	}

	/**
	 * 在处理过程中出现指定类型的异常时进行错误处理。
	 *
	 * @param exceptionType     异常类型，用于匹配需要处理的异常
	 * @param responseProvider  响应提供函数，用于根据异常信息提供新的 ServerResponse
	 * @param <T>               异常类型的限定
	 * @return 返回修改后的 RouterFunctions.Builder 对象
	 */
	@Override
	public <T extends Throwable> RouterFunctions.Builder onError(Class<T> exceptionType,
																 BiFunction<? super T, ServerRequest, Mono<ServerResponse>> responseProvider) {

		Assert.notNull(exceptionType, "ExceptionType must not be null");
		Assert.notNull(responseProvider, "ResponseProvider must not be null");

		// 在错误处理器列表的头部添加一个处理函数
		this.errorHandlers.add(0, (request, next) -> next.handle(request)
				// 如果出现指定类型的异常，根据异常信息提供新的响应
				.onErrorResume(exceptionType, t -> responseProvider.apply(t, request)));
		return this;
	}

	/**
	 * 添加属性到路由函数。
	 *
	 * @param name   属性名，不能为空
	 * @param value  属性值，不能为空
	 * @return 返回修改后的 RouterFunctions.Builder 对象
	 * @throws IllegalStateException 如果方法调用顺序不正确，即未先调用其他方法（例如GET、path等），则抛出异常
	 */
	@Override
	public RouterFunctions.Builder withAttribute(String name, Object value) {
		Assert.hasLength(name, "Name must not be empty");
		Assert.notNull(value, "Value must not be null");

		if (this.routerFunctions.isEmpty()) {
			throw new IllegalStateException("attributes can only be called after any other method (GET, path, etc.)");
		}
		int lastIdx = this.routerFunctions.size() - 1;
		// 从路由函数列表中获取最后一个路由函数，并添加属性
		RouterFunction<ServerResponse> attributed = this.routerFunctions.get(lastIdx)
				.withAttribute(name, value);
		// 将带有属性的路由函数替换最后一个路由函数
		this.routerFunctions.set(lastIdx, attributed);
		return this;
	}

	/**
	 * 使用属性消费者函数为路由函数添加属性。
	 *
	 * @param attributesConsumer 属性消费者函数，不能为空
	 * @return 返回修改后的 RouterFunctions.Builder 对象
	 * @throws IllegalStateException 如果方法调用顺序不正确，即未先调用其他方法（例如GET、path等），则抛出异常
	 */
	@Override
	public RouterFunctions.Builder withAttributes(Consumer<Map<String, Object>> attributesConsumer) {
		Assert.notNull(attributesConsumer, "AttributesConsumer must not be null");

		if (this.routerFunctions.isEmpty()) {
			throw new IllegalStateException("attributes can only be called after any other method (GET, path, etc.)");
		}
		int lastIdx = this.routerFunctions.size() - 1;
		// 从路由函数列表中获取最后一个路由函数，并使用属性消费者函数添加属性
		RouterFunction<ServerResponse> attributed = this.routerFunctions.get(lastIdx)
				.withAttributes(attributesConsumer);
		// 将带有属性的路由函数替换最后一个路由函数
		this.routerFunctions.set(lastIdx, attributed);
		return this;
	}

	/**
	 * 构建并返回一个RouterFunction实例。
	 *
	 * @return 返回构建完成的RouterFunction<ServerResponse>对象
	 * @throws IllegalStateException 如果未注册任何路由，则抛出异常
	 */
	@Override
	public RouterFunction<ServerResponse> build() {
		// 检查是否有注册路由，若没有则抛出异常
		if (this.routerFunctions.isEmpty()) {
			throw new IllegalStateException("No routes registered. Register a route with GET(), POST(), etc.");
		}

		// 将已注册的路由函数转换为 BuiltRouterFunction 对象
		RouterFunction<ServerResponse> result = new BuiltRouterFunction(this.routerFunctions);

		if (this.filterFunctions.isEmpty() && this.errorHandlers.isEmpty()) {
			// 如果没有过滤器函数和错误处理器函数，则直接返回路由函数
			return result;
		} else {
			// 将过滤器函数和错误处理器函数组合成一个 HandlerFilterFunction 并应用到路由函数上
			HandlerFilterFunction<ServerResponse, ServerResponse> filter =
					Stream.concat(this.filterFunctions.stream(), this.errorHandlers.stream())
							.reduce(HandlerFilterFunction::andThen)
							.orElseThrow(IllegalStateException::new);

			return result.filter(filter);
		}
	}


	/**
	 * {@link #build()} 返回的路由函数，简单地迭代注册的路由。
	 */
	private static class BuiltRouterFunction extends RouterFunctions.AbstractRouterFunction<ServerResponse> {

		/**
		 * 声明一个 RouterFunction<ServerResponse> 类型的列表字段
		 */
		private final List<RouterFunction<ServerResponse>> routerFunctions;

		/**
		 * 构造函数，接收一个路由函数列表。
		 *
		 * @param routerFunctions 路由函数列表
		 */
		public BuiltRouterFunction(List<RouterFunction<ServerResponse>> routerFunctions) {
			Assert.notEmpty(routerFunctions, "RouterFunctions must not be empty");
			this.routerFunctions = new ArrayList<>(routerFunctions);
		}

		/**
		 * 路由请求的方法，将注册的路由函数按顺序迭代，并返回匹配的第一个结果。
		 *
		 * @param request 请求
		 * @return 处理函数
		 */
		@Override
		public Mono<HandlerFunction<ServerResponse>> route(ServerRequest request) {
			return Flux.fromIterable(this.routerFunctions)
					.concatMap(routerFunction -> routerFunction.route(request))
					.next();
		}

		/**
		 * 接受访问者的方法，遍历注册的路由函数并通知访问者。
		 *
		 * @param visitor 访问者
		 */
		@Override
		public void accept(RouterFunctions.Visitor visitor) {
			this.routerFunctions.forEach(routerFunction -> routerFunction.accept(visitor));
		}
	}

}
