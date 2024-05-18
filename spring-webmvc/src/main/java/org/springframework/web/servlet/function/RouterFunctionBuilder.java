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

package org.springframework.web.servlet.function;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * {@link RouterFunctions.Builder} 的默认实现。
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
class RouterFunctionBuilder implements RouterFunctions.Builder {

	/**
	 * 存储路由函数的列表。
	 */
	private final List<RouterFunction<ServerResponse>> routerFunctions = new ArrayList<>();

	/**
	 * 存储过滤器函数的列表。
	 */
	private final List<HandlerFilterFunction<ServerResponse, ServerResponse>> filterFunctions = new ArrayList<>();

	/**
	 * 存储错误处理器函数的列表。
	 */
	private final List<HandlerFilterFunction<ServerResponse, ServerResponse>> errorHandlers = new ArrayList<>();


	@Override
	public RouterFunctions.Builder add(RouterFunction<ServerResponse> routerFunction) {
		Assert.notNull(routerFunction, "RouterFunction must not be null");
		this.routerFunctions.add(routerFunction);
		return this;
	}

	private RouterFunctions.Builder add(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction) {
		this.routerFunctions.add(RouterFunctions.route(predicate, handlerFunction));
		return this;
	}

	// GET

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

	// HEAD

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

	// POST

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

	// PUT

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

	// PATCH

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

	// DELETE

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

	// OPTIONS

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

	// other

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
	public RouterFunctions.Builder resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
		return add(RouterFunctions.resources(lookupFunction));
	}

	@Override
	public RouterFunctions.Builder nest(RequestPredicate predicate,
										Consumer<RouterFunctions.Builder> builderConsumer) {

		// 断言消费者不为空
		Assert.notNull(builderConsumer, "Consumer must not be null");

		// 创建一个嵌套的路由构建器
		RouterFunctionBuilder nestedBuilder = new RouterFunctionBuilder();
		// 接受消费者对嵌套构建器的操作
		builderConsumer.accept(nestedBuilder);
		// 构建嵌套的路由函数
		RouterFunction<ServerResponse> nestedRoute = nestedBuilder.build();
		// 将嵌套的路由函数添加到路由函数列表中
		this.routerFunctions.add(RouterFunctions.nest(predicate, nestedRoute));
		return this;
	}

	@Override
	public RouterFunctions.Builder nest(RequestPredicate predicate,
										Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier) {

		// 断言路由函数供应者不为空
		Assert.notNull(routerFunctionSupplier, "RouterFunction Supplier must not be null");

		// 获取嵌套的路由函数
		RouterFunction<ServerResponse> nestedRoute = routerFunctionSupplier.get();
		// 将嵌套的路由函数添加到路由函数列表中
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

	@Override
	public RouterFunctions.Builder before(Function<ServerRequest, ServerRequest> requestProcessor) {
		Assert.notNull(requestProcessor, "RequestProcessor must not be null");
		return filter(HandlerFilterFunction.ofRequestProcessor(requestProcessor));
	}

	@Override
	public RouterFunctions.Builder after(
			BiFunction<ServerRequest, ServerResponse, ServerResponse> responseProcessor) {

		Assert.notNull(responseProcessor, "ResponseProcessor must not be null");
		return filter(HandlerFilterFunction.ofResponseProcessor(responseProcessor));
	}

	@Override
	public RouterFunctions.Builder onError(Predicate<Throwable> predicate,
										   BiFunction<Throwable, ServerRequest, ServerResponse> responseProvider) {

		Assert.notNull(predicate, "Predicate must not be null");
		Assert.notNull(responseProvider, "ResponseProvider must not be null");

		this.errorHandlers.add(0, HandlerFilterFunction.ofErrorHandler(predicate, responseProvider));
		return this;
	}

	@Override
	public RouterFunctions.Builder onError(Class<? extends Throwable> exceptionType,
										   BiFunction<Throwable, ServerRequest, ServerResponse> responseProvider) {
		Assert.notNull(exceptionType, "ExceptionType must not be null");
		Assert.notNull(responseProvider, "ResponseProvider must not be null");

		return onError(exceptionType::isInstance, responseProvider);
	}

	@Override
	public RouterFunctions.Builder withAttribute(String name, Object value) {
		// 断言名称不为空
		Assert.hasLength(name, "Name must not be empty");
		// 断言值不为空
		Assert.notNull(value, "Value must not be null");

		// 如果路由函数列表为空，则抛出异常
		if (this.routerFunctions.isEmpty()) {
			throw new IllegalStateException("attributes can only be called after any other method (GET, path, etc.)");
		}

		// 获取最后一个路由函数的索引
		int lastIdx = this.routerFunctions.size() - 1;
		// 获取具有属性的路由函数
		RouterFunction<ServerResponse> attributed = this.routerFunctions.get(lastIdx)
				.withAttribute(name, value);
		// 替换最后一个路由函数为具有属性的路由函数
		this.routerFunctions.set(lastIdx, attributed);
		return this;
	}

	@Override
	public RouterFunctions.Builder withAttributes(Consumer<Map<String, Object>> attributesConsumer) {
		// 断言属性消费者不为空
		Assert.notNull(attributesConsumer, "AttributesConsumer must not be null");

		// 如果路由函数列表为空，则抛出异常
		if (this.routerFunctions.isEmpty()) {
			throw new IllegalStateException("attributes can only be called after any other method (GET, path, etc.)");
		}

		// 获取最后一个路由函数的索引
		int lastIdx = this.routerFunctions.size() - 1;
		// 获取具有属性的路由函数
		RouterFunction<ServerResponse> attributed = this.routerFunctions.get(lastIdx)
				.withAttributes(attributesConsumer);
		// 替换最后一个路由函数为具有属性的路由函数
		this.routerFunctions.set(lastIdx, attributed);
		return this;
	}

	@Override
	public RouterFunction<ServerResponse> build() {
		// 如果路由函数列表为空，则抛出异常
		if (this.routerFunctions.isEmpty()) {
			throw new IllegalStateException("No routes registered. Register a route with GET(), POST(), etc.");
		}

		// 构建路由函数
		RouterFunction<ServerResponse> result = new BuiltRouterFunction(this.routerFunctions);

		// 如果过滤器函数列表和错误处理函数列表都为空，则返回结果
		if (this.filterFunctions.isEmpty() && this.errorHandlers.isEmpty()) {
			return result;
		} else {
			// 合并过滤器函数和错误处理函数，然后返回结果
			HandlerFilterFunction<ServerResponse, ServerResponse> filter =
					Stream.concat(this.filterFunctions.stream(), this.errorHandlers.stream())
							.reduce(HandlerFilterFunction::andThen)
							.orElseThrow(IllegalStateException::new);

			return result.filter(filter);
		}
	}


	/**
	 * 由 {@link #build()} 返回的路由函数，简单地遍历注册的路由。
	 */
	private static class BuiltRouterFunction extends RouterFunctions.AbstractRouterFunction<ServerResponse> {
		/**
		 * 存储路由函数的列表
		 */
		private final List<RouterFunction<ServerResponse>> routerFunctions;

		public BuiltRouterFunction(List<RouterFunction<ServerResponse>> routerFunctions) {
			Assert.notEmpty(routerFunctions, "RouterFunctions must not be empty");
			this.routerFunctions = new ArrayList<>(routerFunctions);
		}

		@Override
		public Optional<HandlerFunction<ServerResponse>> route(ServerRequest request) {
			// 遍历路由函数列表
			for (RouterFunction<ServerResponse> routerFunction : this.routerFunctions) {
				// 尝试匹配当前请求
				Optional<HandlerFunction<ServerResponse>> result = routerFunction.route(request);
				// 如果匹配成功，则返回结果
				if (result.isPresent()) {
					return result;
				}
			}
			// 如果没有匹配的结果，则返回空
			return Optional.empty();
		}

		@Override
		public void accept(RouterFunctions.Visitor visitor) {
			this.routerFunctions.forEach(routerFunction -> routerFunction.accept(visitor));
		}
	}


}
