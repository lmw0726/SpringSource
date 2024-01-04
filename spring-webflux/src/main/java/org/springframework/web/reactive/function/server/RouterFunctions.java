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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.Assert;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.*;

/**
 * <strong>Spring功能性Web框架的中央入口点。</strong>
 * 提供路由功能，比如使用可发现的构建器风格API{@linkplain #route() 创建}一个{@code RouterFunction}，
 * 使用{@code RequestPredicate}和{@code HandlerFunction} {@linkplain #route(RequestPredicate, HandlerFunction) 创建}一个{@code RouterFunction}，
 * 并在现有路由函数上进行进一步的{@linkplain #nest(RequestPredicate, RouterFunction) 子路由}。
 * <p>
 * 此外，这个类可以{@linkplain #toHttpHandler(RouterFunction) 将}一个{@code RouterFunction}转换为{@code HttpHandler}，
 * 可以在Servlet 3.1+、Reactor或Undertow中运行。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class RouterFunctions {

	private static final Log logger = LogFactory.getLog(RouterFunctions.class);

	/**
	 * {@link ServerWebExchange}属性的名称，包含{@link ServerRequest}。
	 */
	public static final String REQUEST_ATTRIBUTE = RouterFunctions.class.getName() + ".request";

	/**
	 * {@link ServerWebExchange}属性的名称，包含URI模板映射，将变量名称映射到值。
	 */
	public static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE =
			RouterFunctions.class.getName() + ".uriTemplateVariables";

	/**
	 * {@link ServerWebExchange#getAttributes() 属性}的名称，包含匹配模式，作为{@link org.springframework.web.util.pattern.PathPattern}。
	 */
	public static final String MATCHING_PATTERN_ATTRIBUTE =
			RouterFunctions.class.getName() + ".matchingPattern";


	/**
	 * 提供了一种可发现的方法通过构建器风格的接口来创建路由函数。
	 *
	 * @return 路由函数构建器
	 * @since 5.1
	 */
	public static Builder route() {
		return new RouterFunctionBuilder();
	}

	/**
	 * 如果给定的请求谓词适用，则路由到给定的处理函数。
	 * 例如，以下示例将GET请求路由到{@code userController}中的{@code listUsers}方法的"/user"：
	 * <pre class="code">
	 * RouterFunction&lt;ServerResponse&gt; route =
	 *     RouterFunctions.route(RequestPredicates.GET("/user"), userController::listUsers);
	 * </pre>
	 *
	 * @param predicate       要测试的谓词
	 * @param handlerFunction 如果谓词适用，则要路由到的处理函数
	 * @param <T>             处理函数返回的响应类型
	 * @return 如果{@code predicate}评估为{@code true}，则路由到{@code handlerFunction}的路由器函数
	 * @see RequestPredicates
	 */
	public static <T extends ServerResponse> RouterFunction<T> route(
			RequestPredicate predicate, HandlerFunction<T> handlerFunction) {

		return new DefaultRouterFunction<>(predicate, handlerFunction);
	}

	/**
	 * 如果给定的请求谓词适用，则路由到给定的路由器函数。此方法可用于创建<strong>嵌套路由</strong>，其中一组路由共享公共路径（前缀）、标头或其他请求谓词。
	 * 例如，以下示例首先创建一个组合路由，对于GET解析为{@code listUsers}，对于POST解析为{@code createUser}。
	 * 然后，该组合路由与"/user"路径谓词嵌套，以便"/user"的GET请求将列出用户，而"/user"的POST请求将创建新用户。
	 * <pre class="code">
	 * RouterFunction&lt;ServerResponse&gt; userRoutes =
	 *   RouterFunctions.route(RequestPredicates.method(HttpMethod.GET), this::listUsers)
	 *     .andRoute(RequestPredicates.method(HttpMethod.POST), this::createUser);
	 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
	 *   RouterFunctions.nest(RequestPredicates.path("/user"), userRoutes);
	 * </pre>
	 *
	 * @param predicate      要测试的谓词
	 * @param routerFunction 如果谓词适用，则委托的嵌套路由器函数
	 * @param <T>            处理函数返回的响应类型
	 * @return 如果{@code predicate}评估为{@code true}，则路由到{@code routerFunction}的路由器函数
	 * @see RequestPredicates
	 */
	public static <T extends ServerResponse> RouterFunction<T> nest(
			RequestPredicate predicate, RouterFunction<T> routerFunction) {

		return new DefaultNestedRouterFunction<>(predicate, routerFunction);
	}

	/**
	 * 将与给定模式匹配的请求路由到相对于给定根位置的资源。例如
	 * <pre class="code">
	 * Resource location = new FileSystemResource("public-resources/");
	 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources("/resources/**", location);
	 * </pre>
	 *
	 * @param pattern  要匹配的模式
	 * @param location 应解析资源的位置目录
	 * @return 路由到资源的路由器函数
	 * @see #resourceLookupFunction(String, Resource)
	 */
	public static RouterFunction<ServerResponse> resources(String pattern, Resource location) {
		return resources(resourceLookupFunction(pattern, location));
	}

	/**
	 * 返回{@link #resources(String, Resource)}使用的资源查找函数。返回的函数可以进行{@linkplain Function#andThen(Function) 组合}，
	 * 例如，当查找函数不匹配时返回默认资源：
	 * <pre class="code">
	 * Mono&lt;Resource&gt; defaultResource = Mono.just(new ClassPathResource("index.html"));
	 * Function&lt;ServerRequest, Mono&lt;Resource&gt;&gt; lookupFunction =
	 *   RouterFunctions.resourceLookupFunction("/resources/**", new FileSystemResource("public-resources/"))
	 *     .andThen(resourceMono -&gt; resourceMono.switchIfEmpty(defaultResource));
	 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources(lookupFunction);
	 * </pre>
	 *
	 * @param pattern  要匹配的模式
	 * @param location 应解析资源的位置目录
	 * @return 给定参数的默认资源查找函数。
	 */
	public static Function<ServerRequest, Mono<Resource>> resourceLookupFunction(String pattern, Resource location) {
		return new PathResourceLookupFunction(pattern, location);
	}

	/**
	 * 使用提供的查找函数路由到资源。如果查找函数为给定请求提供了{@link Resource}，则将使用处理GET、HEAD和OPTIONS请求的{@link HandlerFunction}公开它。
	 *
	 * @param lookupFunction 根据{@link ServerRequest}提供{@link Resource}的函数
	 * @return 路由到资源的路由器函数
	 */
	public static RouterFunction<ServerResponse> resources(Function<ServerRequest, Mono<Resource>> lookupFunction) {
		return new ResourcesRouterFunction(lookupFunction);
	}

	/**
	 * 将给定的{@linkplain RouterFunction 路由器函数}转换为{@link HttpHandler}。此转换使用{@linkplain HandlerStrategies#builder() 默认策略}。
	 * 返回的处理程序可以适应以下环境运行：
	 * - 在Servlet 3.1+中使用{@link org.springframework.http.server.reactive.ServletHttpHandlerAdapter}
	 * - 在Reactor中使用{@link org.springframework.http.server.reactive.ReactorHttpHandlerAdapter}
	 * - 在Undertow中使用{@link org.springframework.http.server.reactive.UndertowHttpHandlerAdapter}
	 * <p>注意，{@code HttpWebHandlerAdapter}还实现了{@link WebHandler}，允许通过{@link WebHttpHandlerBuilder}注册额外的过滤器和异常处理程序。
	 *
	 * @param routerFunction 要转换的路由器函数
	 * @return 使用给定路由器函数处理HTTP请求的http处理程序
	 */
	public static HttpHandler toHttpHandler(RouterFunction<?> routerFunction) {
		return toHttpHandler(routerFunction, HandlerStrategies.withDefaults());
	}

	/**
	 * 将给定的{@linkplain RouterFunction 路由器函数}使用给定策略转换为{@link HttpHandler}。
	 * 返回的{@code HttpHandler}可以适应以下环境运行：
	 * - 在Servlet 3.1+中使用{@link org.springframework.http.server.reactive.ServletHttpHandlerAdapter}
	 * - 在Reactor中使用{@link org.springframework.http.server.reactive.ReactorHttpHandlerAdapter}
	 * - 在Undertow中使用{@link org.springframework.http.server.reactive.UndertowHttpHandlerAdapter}
	 *
	 * @param routerFunction 要转换的路由器函数
	 * @param strategies     要使用的策略
	 * @return 使用给定路由器函数处理HTTP请求的http处理程序
	 */
	public static HttpHandler toHttpHandler(RouterFunction<?> routerFunction, HandlerStrategies strategies) {
		// 根据 routerFunction 和 strategies 创建一个 WebHandler
		WebHandler webHandler = toWebHandler(routerFunction, strategies);

		// 使用 WebHttpHandlerBuilder 创建一个 WebHandler
		return WebHttpHandlerBuilder.webHandler(webHandler)
				// 添加过滤器列表
				.filters(filters -> filters.addAll(strategies.webFilters()))
				// 添加异常处理器列表
				.exceptionHandlers(handlers -> handlers.addAll(strategies.exceptionHandlers()))
				// 设置区域设置上下文解析器
				.localeContextResolver(strategies.localeContextResolver())
				// 构建 WebHandler
				.build();
	}

	/**
	 * 将给定的{@linkplain RouterFunction 路由器函数}转换为{@link WebHandler}。
	 * 此转换使用{@linkplain HandlerStrategies#builder() 默认策略}。
	 *
	 * @param routerFunction 要转换的路由器函数
	 * @return 使用给定路由器函数处理Web请求的Web处理程序
	 */
	public static WebHandler toWebHandler(RouterFunction<?> routerFunction) {
		return toWebHandler(routerFunction, HandlerStrategies.withDefaults());
	}

	/**
	 * 将给定的{@linkplain RouterFunction 路由器函数}使用给定策略转换为{@link WebHandler}。
	 *
	 * @param routerFunction 要转换的路由器函数
	 * @param strategies     要使用的策略
	 * @return 使用给定路由器函数处理Web请求的Web处理程序
	 */
	public static WebHandler toWebHandler(RouterFunction<?> routerFunction, HandlerStrategies strategies) {
		Assert.notNull(routerFunction, "RouterFunction must not be null");
		Assert.notNull(strategies, "HandlerStrategies must not be null");

		return new RouterFunctionWebHandler(strategies, routerFunction);
	}

	/**
	 * 更改给定{@linkplain RouterFunction 路由器函数}上的{@link PathPatternParser}。此方法可用于更改{@code PathPatternParser}的属性，
	 * 例如更改{@linkplain PathPatternParser#setCaseSensitive(boolean) 大小写敏感性}。
	 *
	 * @param routerFunction 要更改解析器的路由器函数
	 * @param parser         要更改为的解析器。
	 * @param <T>            处理程序函数返回的响应类型
	 * @return 更改后的路由器函数
	 */
	public static <T extends ServerResponse> RouterFunction<T> changeParser(RouterFunction<T> routerFunction,
																			PathPatternParser parser) {

		Assert.notNull(routerFunction, "RouterFunction must not be null");
		Assert.notNull(parser, "Parser must not be null");

		ChangePathPatternParserVisitor visitor = new ChangePathPatternParserVisitor(parser);
		routerFunction.accept(visitor);
		return routerFunction;
	}


	/**
	 * 表示用于构建可发现的路由函数的构建器。
	 * 通过 {@link RouterFunctions#route()} 获取。
	 *
	 * @since 5.1
	 */
	public interface Builder {

		/**
		 * 添加处理 HTTP {@code GET} 请求的处理程序函数。
		 *
		 * @param handlerFunction 处理所有 {@code GET} 请求的处理程序函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder GET(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定模式的 HTTP {@code GET} 请求的处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param handlerFunction 处理所有匹配 {@code pattern} 的 {@code GET} 请求的处理程序函数
		 * @return 此构建器
		 */
		Builder GET(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定谓词的 HTTP {@code GET} 请求的处理程序函数。
		 *
		 * @param predicate       要匹配的谓词
		 * @param handlerFunction 处理所有匹配 {@code predicate} 的 {@code GET} 请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder GET(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定模式和谓词的 HTTP {@code GET} 请求的处理程序函数。
		 * <p>例如，以下示例将接受 JSON 的 "/user" 的 GET 请求路由到 {@code userController} 的 {@code listUsers} 方法：
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .GET("/user", RequestPredicates.accept(MediaType.APPLICATION_JSON), userController::listUsers)
		 *     .build();
		 * </pre>
		 *
		 * @param pattern         要匹配的模式
		 * @param predicate       要匹配的附加谓词
		 * @param handlerFunction 处理所有匹配 {@code pattern} 和谓词的 {@code GET} 请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 */
		Builder GET(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理 HTTP {@code HEAD} 请求的处理程序函数。
		 *
		 * @param handlerFunction 处理所有 {@code HEAD} 请求的处理程序函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder HEAD(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定模式的 HTTP {@code HEAD} 请求的处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param handlerFunction 处理所有匹配 {@code pattern} 的 {@code HEAD} 请求的处理程序函数
		 * @return 此构建器
		 */
		Builder HEAD(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定谓词的 HTTP {@code HEAD} 请求的处理程序函数。
		 *
		 * @param predicate       要匹配的谓词
		 * @param handlerFunction 处理所有匹配 {@code predicate} 的 {@code HEAD} 请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder HEAD(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定模式和谓词的 HTTP {@code HEAD} 请求的处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param predicate       额外的谓词匹配
		 * @param handlerFunction 处理所有匹配 {@code pattern} 的 {@code HEAD} 请求的处理程序函数
		 * @return 此构建器
		 */
		Builder HEAD(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理 HTTP {@code POST} 请求的处理程序函数。
		 *
		 * @param handlerFunction 处理所有 {@code POST} 请求的处理程序函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder POST(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定模式的 HTTP {@code POST} 请求的处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param handlerFunction 处理所有匹配 {@code pattern} 的 {@code POST} 请求的处理程序函数
		 * @return 此构建器
		 */
		Builder POST(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定谓词的 HTTP {@code POST} 请求的处理程序函数。
		 *
		 * @param predicate       要匹配的谓词
		 * @param handlerFunction 处理所有匹配 {@code predicate} 的 {@code POST} 请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder POST(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定模式和谓词的 HTTP {@code POST} 请求的处理程序函数。
		 * <p>例如，以下示例将包含 JSON 的 "/user" 的 {@code POST} 请求路由到 {@code userController} 中的 {@code addUser} 方法：
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .POST("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::addUser)
		 *     .build();
		 * </pre>
		 *
		 * @param pattern         要匹配的模式
		 * @param predicate       额外的谓词匹配
		 * @param handlerFunction 处理所有匹配 {@code pattern} 的 {@code POST} 请求的处理程序函数
		 * @return 此构建器
		 */
		Builder POST(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理 HTTP {@code PUT} 请求的处理程序函数。
		 *
		 * @param handlerFunction 处理所有 {@code PUT} 请求的处理程序函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder PUT(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定模式的 HTTP {@code PUT} 请求的处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param handlerFunction 处理所有匹配 {@code pattern} 的 {@code PUT} 请求的处理程序函数
		 * @return 此构建器
		 */
		Builder PUT(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定谓词的 HTTP {@code PUT} 请求的处理程序函数。
		 *
		 * @param predicate       要匹配的谓词
		 * @param handlerFunction 处理所有匹配 {@code predicate} 的 {@code PUT} 请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder PUT(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定模式和谓词的 HTTP {@code PUT} 请求的处理程序函数。
		 * <p>例如，以下示例将包含 JSON 的 "/user" 的 {@code PUT} 请求路由到 {@code userController} 中的 {@code editUser} 方法：
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .PUT("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::editUser)
		 *     .build();
		 * </pre>
		 *
		 * @param pattern         要匹配的模式
		 * @param predicate       额外的谓词匹配
		 * @param handlerFunction 处理所有匹配 {@code pattern} 的 {@code PUT} 请求的处理程序函数
		 * @return 此构建器
		 */
		Builder PUT(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理 HTTP {@code PATCH} 请求的处理程序函数。
		 *
		 * @param handlerFunction 处理所有 {@code PATCH} 请求的处理程序函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder PATCH(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定模式的 HTTP {@code PATCH} 请求的处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param handlerFunction 处理所有匹配 {@code pattern} 的 {@code PATCH} 请求的处理程序函数
		 * @return 此构建器
		 */
		Builder PATCH(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定谓词的 HTTP {@code PATCH} 请求的处理程序函数。
		 *
		 * @param predicate       要匹配的谓词
		 * @param handlerFunction 处理所有匹配 {@code predicate} 的 {@code PATCH} 请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder PATCH(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定模式和谓词的 HTTP {@code PATCH} 请求的处理程序函数。
		 * <p>例如，以下示例将包含 JSON 的 "/user" 的 {@code PATCH} 请求路由到 {@code userController} 中的 {@code editUser} 方法：
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .PATCH("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::editUser)
		 *     .build();
		 * </pre>
		 *
		 * @param pattern         要匹配的模式
		 * @param predicate       额外的谓词匹配
		 * @param handlerFunction 处理所有匹配 {@code pattern} 的 {@code PATCH} 请求的处理程序函数
		 * @return 此构建器
		 */
		Builder PATCH(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理 HTTP {@code DELETE} 请求的处理程序函数。
		 *
		 * @param handlerFunction 处理所有 {@code DELETE} 请求的处理程序函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder DELETE(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定模式的 HTTP {@code DELETE} 请求的处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param handlerFunction 处理所有匹配 {@code pattern} 的 {@code DELETE} 请求的处理程序函数
		 * @return 此构建器
		 */
		Builder DELETE(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定谓词的 HTTP {@code DELETE} 请求的处理程序函数。
		 *
		 * @param predicate       要匹配的谓词
		 * @param handlerFunction 处理所有匹配 {@code predicate} 的 {@code DELETE} 请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder DELETE(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定模式和谓词的 HTTP {@code DELETE} 请求的处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param predicate       额外的谓词匹配
		 * @param handlerFunction 处理所有匹配 {@code pattern} 的 {@code DELETE} 请求的处理程序函数
		 * @return 此构建器
		 */
		Builder DELETE(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理 HTTP {@code OPTIONS} 请求的处理程序函数。
		 *
		 * @param handlerFunction 处理所有 {@code OPTIONS} 请求的处理程序函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder OPTIONS(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定模式的 HTTP {@code OPTIONS} 请求的处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param handlerFunction 处理所有匹配 {@code pattern} 的 {@code OPTIONS} 请求的处理程序函数
		 * @return 此构建器
		 */
		Builder OPTIONS(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定谓词的 HTTP {@code OPTIONS} 请求的处理程序函数。
		 *
		 * @param predicate       要匹配的谓词
		 * @param handlerFunction 处理所有匹配 {@code predicate} 的 {@code OPTIONS} 请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder OPTIONS(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定模式和谓词的 HTTP {@code OPTIONS} 请求的处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param predicate       额外的谓词匹配
		 * @param handlerFunction 处理所有匹配 {@code pattern} 的 {@code OPTIONS} 请求的处理程序函数
		 * @return 此构建器
		 */
		Builder OPTIONS(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加处理所有匹配给定谓词的请求的处理程序函数。
		 *
		 * @param predicate       要匹配的请求谓词
		 * @param handlerFunction 处理所有匹配谓词的请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.2
		 */
		Builder route(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 将给定的路由函数添加到此构建器。可用于将外部定义的路由函数合并到此构建器中，
		 * 或与 {@link RouterFunctions#route(RequestPredicate, HandlerFunction)} 结合使用，
		 * 以允许更灵活的谓词匹配。
		 * <p>例如，以下示例将 {@code OrderController.routerFunction()} 返回的路由函数添加到 {@code userController} 中的 {@code changeUser} 方法。
		 *
		 * @param routerFunction 要添加的路由函数
		 * @return 此构建器
		 * @see RequestPredicates
		 */
		Builder add(RouterFunction<ServerResponse> routerFunction);

		/**
		 * 将匹配给定模式的请求路由到相对于给定根位置的资源。例如
		 *
		 * @param pattern  要匹配的模式
		 * @param location 资源应解析到的位置目录的相对位置
		 * @return 此构建器
		 */
		Builder resources(String pattern, Resource location);

		/**
		 * 使用提供的查找函数将请求路由到资源。如果查找函数为给定的请求提供了 {@link Resource}，
		 * 则将使用处理 GET、HEAD 和 OPTIONS 请求的 {@link HandlerFunction} 公开该资源。
		 *
		 * @param lookupFunction 给定 {@link ServerRequest} 后提供 {@link Resource} 的函数
		 * @return 此构建器
		 */
		Builder resources(Function<ServerRequest, Mono<Resource>> lookupFunction);

		/**
		 * 如果给定的请求谓词适用，则将请求路由到生成的路由函数。此方法可用于创建<strong>嵌套路由</strong>，
		 * 其中一组路由共享公共路径（前缀）、标题或其他请求谓词。
		 *
		 * @param predicate              要测试的谓词
		 * @param routerFunctionSupplier 如果谓词适用，则提供嵌套路由函数的供应商
		 * @return 此构建器
		 * @see RequestPredicates
		 */
		Builder nest(RequestPredicate predicate, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier);

		/**
		 * 如果给定的请求路径前缀模式适用，则将请求路由到生成的路由函数。此方法可用于创建<strong>嵌套路由</strong>，
		 * 其中一组路由共享公共路径前缀。
		 *
		 * @param pattern                要匹配的模式
		 * @param routerFunctionSupplier 如果模式匹配，则提供嵌套路由函数的供应商
		 * @return 此构建器
		 */
		Builder path(String pattern, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier);

		/**
		 * 将请求构建器的所有路由函数过滤器为给定的过滤器函数。过滤器函数通常用于解决交叉关注点，如日志记录、安全性等。
		 *
		 * @param filterFunction 用于过滤此构建器构建的所有路由函数的函数
		 * @return 此构建器
		 */
		Builder filter(HandlerFilterFunction<ServerResponse, ServerResponse> filterFunction);

		/**
		 * 对此构建器构建的所有路由函数的请求对象进行过滤。过滤器通常用于解决交叉关注点，如日志记录、安全性等。
		 *
		 * @param requestProcessor 变换请求的函数
		 * @return 此构建器
		 */
		Builder before(Function<ServerRequest, ServerRequest> requestProcessor);

		/**
		 * 用给定的响应处理函数过滤由该构建器创建的所有路由的响应对象。过滤器通常用于解决交叉关注点，如日志记录、安全性等。
		 * <p>例如，以下示例创建一个过滤器，在处理程序函数执行后记录响应。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .after((request, response) -&gt; {
		 *       log(response);
		 *       return response;
		 *     })
		 *     .build();
		 * </pre>
		 *
		 * @param responseProcessor 变换响应的函数
		 * @return 此构建器
		 */
		Builder after(BiFunction<ServerRequest, ServerResponse, ServerResponse> responseProcessor);

		/**
		 * 通过应用给定的响应提供程序函数，过滤所有与谓词匹配的异常。
		 * <p>例如，以下示例创建一个过滤器，在发生 {@code IllegalStateException} 时返回 500 响应状态。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .onError(e -&gt; e instanceof IllegalStateException,
		 *       (e, request) -&gt; ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
		 *     .build();
		 * </pre>
		 *
		 * @param predicate        要过滤的异常类型
		 * @param responseProvider 创建响应的函数
		 * @return 此构建器
		 */
		Builder onError(Predicate<? super Throwable> predicate,
						BiFunction<? super Throwable, ServerRequest, Mono<ServerResponse>> responseProvider);

		/**
		 * 通过应用给定的响应提供程序函数，过滤给定类型的所有异常。
		 * <p>例如，以下示例创建一个过滤器，在发生 {@code IllegalStateException} 时返回 500 响应状态。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .onError(IllegalStateException.class,
		 *       (e, request) -&gt; ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
		 *     .build();
		 * </pre>
		 *
		 * @param exceptionType    要过滤的异常类型
		 * @param responseProvider 创建响应的函数
		 * @return 此构建器
		 */
		<T extends Throwable> Builder onError(Class<T> exceptionType,
											  BiFunction<? super T, ServerRequest, Mono<ServerResponse>> responseProvider);

		/**
		 * 向使用此构建器构建的最后一个路由添加具有给定名称和值的属性。
		 *
		 * @param name  属性名称
		 * @param value 属性值
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder withAttribute(String name, Object value);

		/**
		 * 使用消费者操作给定的路由的最后一个路由的属性。
		 * <p>提供给消费者的映射是“活动的”，因此消费者可用于 {@linkplain Map#put(Object, Object) 覆盖}现有属性、
		 * {@linkplain Map#remove(Object) 删除}属性或使用任何其他{@link Map}方法。
		 *
		 * @param attributesConsumer 消费属性映射的函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder withAttributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * 构建 {@code RouterFunction}。所有创建的路由都会{@linkplain RouterFunction#and(RouterFunction) 合并}在一起，
		 * 并将过滤器（如果有）应用于结果。
		 *
		 * @return 构建的路由函数
		 */
		RouterFunction<ServerResponse> build();
	}


	/**
	 * 用于接收路由函数逻辑结构的通知的访问者接口。
	 */
	public interface Visitor {

		/**
		 * 接收嵌套路由函数开始的通知。
		 *
		 * @param predicate 适用于嵌套路由函数的谓词
		 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
		 */
		void startNested(RequestPredicate predicate);

		/**
		 * 接收嵌套路由函数结束的通知。
		 *
		 * @param predicate 适用于嵌套路由函数的谓词
		 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
		 */
		void endNested(RequestPredicate predicate);

		/**
		 * 接收到一个标准的带谓词的路由到处理函数的通知。
		 *
		 * @param predicate       适用于处理函数的谓词
		 * @param handlerFunction 处理函数
		 * @see RouterFunctions#route(RequestPredicate, HandlerFunction)
		 */
		void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction);

		/**
		 * 接收资源路由函数的通知。
		 *
		 * @param lookupFunction 资源的查找函数
		 * @see RouterFunctions#resources(Function)
		 */
		void resources(Function<ServerRequest, Mono<Resource>> lookupFunction);

		/**
		 * 接收带有属性的路由函数的通知。给定的属性适用于接下来的路由通知。
		 *
		 * @param attributes 适用于后续路由的属性
		 * @since 5.3
		 */
		void attributes(Map<String, Object> attributes);

		/**
		 * 接收未知路由函数的通知。这个方法用于那些不是通过各种{@link RouterFunctions}方法创建的路由函数。
		 *
		 * @param routerFunction 路由函数
		 */
		void unknown(RouterFunction<?> routerFunction);
	}


	/**
	 * 抽象的{@code RouterFunction}类，实现了基本的{@code RouterFunction}接口。
	 *
	 * @param <T> {@code ServerResponse}的子类，处理函数的响应类型
	 */
	abstract static class AbstractRouterFunction<T extends ServerResponse> implements RouterFunction<T> {

		/**
		 * 返回此路由函数的字符串表示形式。
		 *
		 * @return 此路由函数的字符串表示形式
		 */
		@Override
		public String toString() {
			// 使用ToStringVisitor生成此路由函数的字符串表示形式
			ToStringVisitor visitor = new ToStringVisitor();
			accept(visitor);
			return visitor.toString();
		}
	}


	/**
	 * 一个组合路由函数，首先调用一个函数，然后调用另一个函数（相同响应类型{@code T}），
	 * 如果此路由没有结果（{@linkplain Mono#empty()}）。
	 *
	 * @param <T> 服务器响应类型
	 */
	static final class SameComposedRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

		/**
		 * 第一个调用的路由函数
		 */
		private final RouterFunction<T> first;

		/**
		 * 第二个调用的路由函数
		 */
		private final RouterFunction<T> second;

		/**
		 * 构造函数，接收两个路由函数作为参数。
		 *
		 * @param first  第一个路由函数
		 * @param second 第二个路由函数
		 */
		public SameComposedRouterFunction(RouterFunction<T> first, RouterFunction<T> second) {
			this.first = first;
			this.second = second;
		}

		/**
		 * 路由请求的方法，首先尝试第一个路由函数，如果结果为空，则尝试第二个路由函数。
		 *
		 * @param request 请求
		 * @return 处理函数
		 */
		@Override
		public Mono<HandlerFunction<T>> route(ServerRequest request) {
			return Flux.concat(this.first.route(request), Mono.defer(() -> this.second.route(request)))
					.next();
		}

		/**
		 * 接受访问者的方法，依次通知两个路由函数的访问者方法。
		 *
		 * @param visitor 访问者
		 */
		@Override
		public void accept(Visitor visitor) {
			this.first.accept(visitor);
			this.second.accept(visitor);
		}
	}


	/**
	 * 一个组合路由函数，首先调用一个函数，然后调用另一个函数（不同响应类型），
	 * 如果此路由没有结果（{@linkplain Mono#empty()}）。
	 */
	static final class DifferentComposedRouterFunction extends AbstractRouterFunction<ServerResponse> {

		/**
		 * 第一个路由函数
		 */
		private final RouterFunction<?> first;

		/**
		 * 第二个路由函数
		 */
		private final RouterFunction<?> second;

		/**
		 * 构造函数，接收两个路由函数作为参数。
		 *
		 * @param first  第一个路由函数
		 * @param second 第二个路由函数
		 */
		public DifferentComposedRouterFunction(RouterFunction<?> first, RouterFunction<?> second) {
			this.first = first;
			this.second = second;
		}

		/**
		 * 路由请求的方法，首先尝试第一个路由函数，如果结果为空，则尝试第二个路由函数。
		 *
		 * @param request 请求
		 * @return 处理函数
		 */
		@Override
		public Mono<HandlerFunction<ServerResponse>> route(ServerRequest request) {
			return Flux.concat(this.first.route(request), Mono.defer(() -> this.second.route(request)))
					.next()
					.map(this::cast);
		}

		/**
		 * 将处理函数强制转换为指定响应类型的方法。
		 *
		 * @param handlerFunction 处理函数
		 * @param <T>             响应类型
		 * @return 转换后的处理函数
		 */
		@SuppressWarnings("unchecked")
		private <T extends ServerResponse> HandlerFunction<T> cast(HandlerFunction<?> handlerFunction) {
			return (HandlerFunction<T>) handlerFunction;
		}

		/**
		 * 接受访问者的方法，依次通知两个路由函数的访问者方法。
		 *
		 * @param visitor 访问者
		 */
		@Override
		public void accept(Visitor visitor) {
			this.first.accept(visitor);
			this.second.accept(visitor);
		}
	}


	/**
	 * 使用给定的{@linkplain HandlerFilterFunction 过滤函数}过滤指定的{@linkplain HandlerFunction 处理函数}。
	 *
	 * @param <T> 要过滤的{@linkplain HandlerFunction 处理函数}的类型
	 * @param <S> 函数的响应类型
	 */
	static final class FilteredRouterFunction<T extends ServerResponse, S extends ServerResponse>
			implements RouterFunction<S> {

		/**
		 * 声明一个 RouterFunction 类型的路由器函数字段
		 */
		private final RouterFunction<T> routerFunction;

		/**
		 * 声明一个 HandlerFilterFunction 类型的处理器过滤函数字段
		 */
		private final HandlerFilterFunction<T, S> filterFunction;


		public FilteredRouterFunction(
				RouterFunction<T> routerFunction,
				HandlerFilterFunction<T, S> filterFunction) {
			this.routerFunction = routerFunction;
			this.filterFunction = filterFunction;
		}

		/**
		 * 返回匹配给定请求的{@code HandlerFunction}的{@code Mono}描述，
		 * 应用{@code filterFunction}进行过滤。
		 *
		 * @param request 要路由的请求
		 * @return 描述匹配此请求的{@code HandlerFunction}的{@code Mono}，如果没有匹配，则为空的{@code Mono}
		 */
		@Override
		public Mono<HandlerFunction<S>> route(ServerRequest request) {
			return this.routerFunction.route(request).map(this.filterFunction::apply);
		}

		/**
		 * 接受给定的访问者，将其传递给内部的路由函数。
		 *
		 * @param visitor 要接受的访问者
		 */
		@Override
		public void accept(Visitor visitor) {
			this.routerFunction.accept(visitor);
		}

		/**
		 * 返回此路由函数的字符串表示形式，与内部路由函数的字符串表示形式相同。
		 *
		 * @return 此路由函数的字符串表示形式
		 */
		@Override
		public String toString() {
			return this.routerFunction.toString();
		}
	}


	/**
	 * 一个默认的路由函数，实现了路由函数的抽象类。
	 *
	 * @param <T> 服务器响应类型
	 */
	private static final class DefaultRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

		/**
		 * 请求断言
		 */
		private final RequestPredicate predicate;

		/**
		 * 处理函数
		 */
		private final HandlerFunction<T> handlerFunction;

		/**
		 * 构造函数，接收谓词和处理函数作为参数。
		 *
		 * @param predicate       谓词
		 * @param handlerFunction 处理函数
		 */
		public DefaultRouterFunction(RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
			Assert.notNull(predicate, "Predicate must not be null");
			Assert.notNull(handlerFunction, "HandlerFunction must not be null");
			this.predicate = predicate;
			this.handlerFunction = handlerFunction;
		}

		/**
		 * 路由请求的方法，根据请求是否符合谓词来决定是否执行处理函数。
		 *
		 * @param request 请求
		 * @return 处理函数
		 */
		@Override
		public Mono<HandlerFunction<T>> route(ServerRequest request) {
			if (this.predicate.test(request)) {
				if (logger.isTraceEnabled()) {
					String logPrefix = request.exchange().getLogPrefix();
					logger.trace(logPrefix + String.format("Matched %s", this.predicate));
				}
				return Mono.just(this.handlerFunction);
			} else {
				return Mono.empty();
			}
		}

		/**
		 * 接受访问者的方法，通知访问者匹配的谓词和处理函数。
		 *
		 * @param visitor 访问者
		 */
		@Override
		public void accept(Visitor visitor) {
			visitor.route(this.predicate, this.handlerFunction);
		}

	}

	/**
	 * 默认的嵌套路由函数，继承自抽象路由函数。
	 *
	 * @param <T> 服务器响应类型
	 */
	private static final class DefaultNestedRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

		/**
		 * 请求断言
		 */
		private final RequestPredicate predicate;

		/**
		 * 路由函数
		 */
		private final RouterFunction<T> routerFunction;

		/**
		 * 构造函数，接收谓词和路由函数作为参数。
		 *
		 * @param predicate      请求断言
		 * @param routerFunction 路由函数
		 */
		public DefaultNestedRouterFunction(RequestPredicate predicate, RouterFunction<T> routerFunction) {
			Assert.notNull(predicate, "Predicate must not be null");
			Assert.notNull(routerFunction, "RouterFunction must not be null");
			this.predicate = predicate;
			this.routerFunction = routerFunction;
		}

		/**
		 * 路由请求的方法。嵌套谓词，返回嵌套的请求，然后处理路由函数的路由方法，处理嵌套请求的路由结果并在必要时更新请求属性。
		 *
		 * @param serverRequest 服务器请求
		 * @return 处理函数
		 */
		@Override
		public Mono<HandlerFunction<T>> route(ServerRequest serverRequest) {
			// 根据断言进行嵌套处理
			return this.predicate.nest(serverRequest)
					.map(nestedRequest -> {
						if (logger.isTraceEnabled()) {
							String logPrefix = serverRequest.exchange().getLogPrefix();
							logger.trace(logPrefix + String.format("Matched nested %s", this.predicate));
						}
						// 处理嵌套请求
						return this.routerFunction.route(nestedRequest)
								.doOnNext(match -> {
									if (nestedRequest != serverRequest) {
										// 清除原始请求的属性
										serverRequest.attributes().clear();
										// 设置嵌套请求的属性
										serverRequest.attributes().putAll(nestedRequest.attributes());
									}
								});
					})
					// 如果没有嵌套请求则返回空的 Mono
					.orElseGet(Mono::empty);
		}

		/**
		 * 接受访问者的方法，通知访问者嵌套的开始和结束，然后处理路由函数的接受方法。
		 *
		 * @param visitor 访问者
		 */
		@Override
		public void accept(Visitor visitor) {
			// 调用 visitor 对象的方法开始访问嵌套路由函数，并传入当前的请求断言
			visitor.startNested(this.predicate);

			// 调用当前路由函数的 accept 方法，处理当前路由函数
			this.routerFunction.accept(visitor);

			// 调用 visitor 对象的方法结束嵌套访问，并传入当前的请求断言
			visitor.endNested(this.predicate);
		}

	}

	/**
	 * 表示资源路由函数的内部类，继承自AbstractRouterFunction<ServerResponse>。
	 */
	private static class ResourcesRouterFunction extends AbstractRouterFunction<ServerResponse> {

		/**
		 * 声明一个函数字段，接受 ServerRequest 参数并返回 Mono<Resource>
		 */
		private final Function<ServerRequest, Mono<Resource>> lookupFunction;


		/**
		 * 构造函数，接收一个查找资源的函数。
		 *
		 * @param lookupFunction 查找资源的函数
		 */
		public ResourcesRouterFunction(Function<ServerRequest, Mono<Resource>> lookupFunction) {
			Assert.notNull(lookupFunction, "Function must not be null");
			this.lookupFunction = lookupFunction;
		}

		/**
		 * 路由请求的方法，通过应用查找资源的函数创建资源处理函数。
		 *
		 * @param request 请求
		 * @return 处理函数
		 */
		@Override
		public Mono<HandlerFunction<ServerResponse>> route(ServerRequest request) {
			return this.lookupFunction.apply(request).map(ResourceHandlerFunction::new);
		}

		/**
		 * 接受访问者的方法，通知访问者这是一个资源路由函数，并传递查找资源的函数。
		 *
		 * @param visitor 访问者
		 */
		@Override
		public void accept(Visitor visitor) {
			visitor.resources(this.lookupFunction);
		}
	}


	/**
	 * 一个带属性的路由函数，继承自抽象路由函数。
	 *
	 * @param <T> 服务器响应类型
	 */
	static final class AttributesRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

		/**
		 * 路由函数
		 */
		private final RouterFunction<T> delegate;
		/**
		 * 属性
		 */
		private final Map<String, Object> attributes;

		/**
		 * 构造函数，接收委托的路由函数和属性作为参数。
		 *
		 * @param delegate   委托的路由函数
		 * @param attributes 属性
		 */
		public AttributesRouterFunction(RouterFunction<T> delegate, Map<String, Object> attributes) {
			this.delegate = delegate;
			this.attributes = initAttributes(attributes);
		}

		/**
		 * 初始化属性，将其设置为不可修改的。
		 *
		 * @param attributes 属性
		 * @return 不可修改的属性
		 */
		private static Map<String, Object> initAttributes(Map<String, Object> attributes) {
			if (attributes.isEmpty()) {
				return Collections.emptyMap();
			} else {
				return Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
			}
		}

		/**
		 * 路由请求的方法，调用委托路由函数的路由方法。
		 *
		 * @param request 请求
		 * @return 处理函数
		 */
		@Override
		public Mono<HandlerFunction<T>> route(ServerRequest request) {
			return this.delegate.route(request);
		}

		/**
		 * 接受访问者的方法，通知访问者属性信息，然后调用委托路由函数的接受方法。
		 *
		 * @param visitor 访问者
		 */
		@Override
		public void accept(Visitor visitor) {
			visitor.attributes(this.attributes);
			this.delegate.accept(visitor);
		}

		/**
		 * 返回具有指定属性的新路由函数。
		 *
		 * @param name  属性名
		 * @param value 属性值
		 * @return 具有指定属性的新路由函数
		 */
		@Override
		public RouterFunction<T> withAttribute(String name, Object value) {
			Assert.hasLength(name, "Name must not be empty");
			Assert.notNull(value, "Value must not be null");

			Map<String, Object> attributes = new LinkedHashMap<>(this.attributes);
			attributes.put(name, value);
			return new AttributesRouterFunction<>(this.delegate, attributes);
		}

		/**
		 * 使用消费者操作属性并返回新的路由函数。
		 *
		 * @param attributesConsumer 操作属性的消费者
		 * @return 新的路由函数
		 */
		@Override
		public RouterFunction<T> withAttributes(Consumer<Map<String, Object>> attributesConsumer) {
			Assert.notNull(attributesConsumer, "AttributesConsumer must not be null");

			Map<String, Object> attributes = new LinkedHashMap<>(this.attributes);
			attributesConsumer.accept(attributes);
			return new AttributesRouterFunction<>(this.delegate, attributes);
		}
	}


	/**
	 * 实现了 {@code ServerResponse.Context} 接口的 {@code HandlerStrategiesResponseContext} 类。
	 * 它接收 {@code HandlerStrategies} 对象并提供了消息写入器和视图解析器的列表。
	 */
	private static class HandlerStrategiesResponseContext implements ServerResponse.Context {

		/**
		 * 处理器策略
		 */
		private final HandlerStrategies strategies;

		/**
		 * 构造函数，接收 {@code HandlerStrategies} 对象。
		 *
		 * @param strategies {@code HandlerStrategies} 对象
		 */
		public HandlerStrategiesResponseContext(HandlerStrategies strategies) {
			this.strategies = strategies;
		}

		/**
		 * 获取消息写入器的列表。
		 *
		 * @return 消息写入器的列表
		 */
		@Override
		public List<HttpMessageWriter<?>> messageWriters() {
			return this.strategies.messageWriters();
		}

		/**
		 * 获取视图解析器的列表。
		 *
		 * @return 视图解析器的列表
		 */
		@Override
		public List<ViewResolver> viewResolvers() {
			return this.strategies.viewResolvers();
		}
	}


	/**
	 * 实现了 {@code WebHandler} 接口的 {@code RouterFunctionWebHandler} 类。它用于处理 {@code RouterFunction} 的 HTTP 请求。
	 */
	private static class RouterFunctionWebHandler implements WebHandler {

		/**
		 * 处理器策略
		 */
		private final HandlerStrategies strategies;

		/**
		 * 路由函数
		 */
		private final RouterFunction<?> routerFunction;

		/**
		 * 构造函数，接收 {@code HandlerStrategies} 和 {@code RouterFunction} 对象。
		 *
		 * @param strategies     {@code HandlerStrategies} 对象
		 * @param routerFunction {@code RouterFunction} 对象
		 */
		public RouterFunctionWebHandler(HandlerStrategies strategies, RouterFunction<?> routerFunction) {
			this.strategies = strategies;
			this.routerFunction = routerFunction;
		}

		/**
		 * 处理 {@code ServerWebExchange} 的方法。
		 *
		 * @param exchange HTTP 请求交换对象
		 * @return 一个表示异步 HTTP 响应的 {@code Mono<Void>}
		 */
		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			// 使用 Mono.defer 创建一个延迟执行的流程
			return Mono.defer(() -> {
				// 创建一个 DefaultServerRequest 对象，用于处理当前交换器和策略中的消息读取器
				ServerRequest request = new DefaultServerRequest(exchange, this.strategies.messageReaders());

				// 向请求添加属性
				addAttributes(exchange, request);

				// 使用路由函数处理请求
				return this.routerFunction.route(request)
						// 如果路由没有匹配到处理程序，则创建一个“未找到资源”的错误响应
						.switchIfEmpty(createNotFoundError())
						// 如果找到了处理程序，对其进行处理并尝试将其写入到服务器响应中
						.flatMap(handlerFunction -> wrapException(() -> handlerFunction.handle(request)))
						.flatMap(response -> wrapException(() -> response.writeTo(exchange,
								new HandlerStrategiesResponseContext(this.strategies))));
			});
		}

		/**
		 * 将请求对象添加到交换对象的属性中。
		 *
		 * @param exchange HTTP 请求交换对象
		 * @param request  HTTP 请求对象
		 */
		private void addAttributes(ServerWebExchange exchange, ServerRequest request) {
			Map<String, Object> attributes = exchange.getAttributes();
			attributes.put(REQUEST_ATTRIBUTE, request);
		}

		/**
		 * 创建表示未找到路由函数的错误 {@code Mono}。
		 *
		 * @param <R> 返回的类型
		 * @return 一个 {@code Mono}，表示未找到路由函数的错误
		 */
		private <R> Mono<R> createNotFoundError() {
			return Mono.defer(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
					"No matching router function")));
		}

		/**
		 * 包装执行函数，捕获异常并转换为 {@code Mono}。
		 *
		 * @param supplier 包含要执行的函数的供应商
		 * @param <T>      返回的类型
		 * @return 一个 {@code Mono}，表示执行函数的结果
		 */
		private static <T> Mono<T> wrapException(Supplier<Mono<T>> supplier) {
			try {
				return supplier.get();
			} catch (Throwable ex) {
				return Mono.error(ex);
			}
		}
	}
}
