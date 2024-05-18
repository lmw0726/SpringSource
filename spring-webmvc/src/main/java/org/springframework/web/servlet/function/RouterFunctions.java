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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.*;

/**
 * <strong>Spring功能性Web框架的中央入口点。</strong>
 * 提供路由功能，例如通过可发现的构建器风格API{@linkplain #route() 创建} {@code RouterFunction}，
 * 通过给定的{@code RequestPredicate}和{@code HandlerFunction} {@linkplain #route(RequestPredicate, HandlerFunction)创建} {@code RouterFunction}，
 * 以及在现有路由功能上进行进一步的{@linkplain #nest(RequestPredicate, RouterFunction) 子路由}。
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
public abstract class RouterFunctions {

	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(RouterFunctions.class);

	/**
	 * 包含{@link ServerRequest}的请求属性的名称。
	 */
	public static final String REQUEST_ATTRIBUTE = RouterFunctions.class.getName() + ".request";

	/**
	 * 包含URI模板映射（将变量名称映射到值）的请求属性的名称。
	 */
	public static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE =
			RouterFunctions.class.getName() + ".uriTemplateVariables";

	/**
	 * 包含匹配模式的请求属性的名称，作为{@link org.springframework.web.util.pattern.PathPattern}。
	 */
	public static final String MATCHING_PATTERN_ATTRIBUTE =
			RouterFunctions.class.getName() + ".matchingPattern";


	/**
	 * 通过构建器风格的接口提供一种可发现的方式来创建路由函数。
	 *
	 * @return 路由函数构建器
	 */
	public static Builder route() {
		return new RouterFunctionBuilder();
	}

	/**
	 * 如果给定的请求谓词适用，则路由到给定的处理程序函数。
	 * <p>例如，以下示例将GET请求路由到{@code userController}中的{@code listUsers}方法：
	 * <pre class="code">
	 * RouterFunction&lt;ServerResponse&gt; route =
	 *     RouterFunctions.route(RequestPredicates.GET("/user"), userController::listUsers);
	 * </pre>
	 *
	 * @param predicate       要测试的谓词
	 * @param handlerFunction 如果谓词适用，则路由到的处理程序函数
	 * @param <T>             处理程序函数返回的响应类型
	 * @return 如果{@code predicate}求值为{@code true}，则路由到{@code handlerFunction}的路由函数
	 * @see RequestPredicates
	 */
	public static <T extends ServerResponse> RouterFunction<T> route(
			RequestPredicate predicate, HandlerFunction<T> handlerFunction) {

		return new DefaultRouterFunction<>(predicate, handlerFunction);
	}

	/**
	 * 如果给定的请求谓词适用，则路由到给定的路由函数。此方法可用于创建<strong>嵌套路由</strong>，其中一组路由共享共同的路径（前缀）、标头或其他请求谓词。
	 * <p>例如，以下示例首先创建一个复合路由，对于GET请求，解析为{@code listUsers}，对于POST请求，解析为{@code createUser}。
	 * 然后，此复合路由会嵌套在"/user"路径谓词中，因此"/user"的GET请求将列出用户，而"/user"的POST请求将创建新用户。
	 * <pre class="code">
	 * RouterFunction&lt;ServerResponse&gt; userRoutes =
	 *   RouterFunctions.route(RequestPredicates.method(HttpMethod.GET), this::listUsers)
	 *     .andRoute(RequestPredicates.method(HttpMethod.POST), this::createUser);
	 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
	 *   RouterFunctions.nest(RequestPredicates.path("/user"), userRoutes);
	 * </pre>
	 *
	 * @param predicate      要测试的谓词
	 * @param routerFunction 如果谓词适用，则委派到的嵌套路由函数
	 * @param <T>            处理程序函数返回的响应类型
	 * @return 如果{@code predicate}求值为{@code true}，则路由到{@code routerFunction}的路由函数
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
	 * @param location 应解析资源的位置目录的相对位置
	 * @return 一个路由到资源的路由函数
	 * @see #resourceLookupFunction(String, Resource)
	 */
	public static RouterFunction<ServerResponse> resources(String pattern, Resource location) {
		return resources(resourceLookupFunction(pattern, location));
	}

	/**
	 * 返回由{@link #resources(String, Resource)}使用的资源查找函数。
	 * 返回的函数可以{@linkplain Function#andThen(Function) 组合}，例如在查找函数不匹配时返回默认资源：
	 * <pre class="code">
	 * Optional&lt;Resource&gt; defaultResource = Optional.of(new ClassPathResource("index.html"));
	 * Function&lt;ServerRequest, Optional&lt;Resource&gt;&gt; lookupFunction =
	 *   RouterFunctions.resourceLookupFunction("/resources/**", new FileSystemResource("public-resources/"))
	 *     .andThen(resource -&gt; resource.or(() -&gt; defaultResource));
	 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources(lookupFunction);
	 * </pre>
	 *
	 * @param pattern  要匹配的模式
	 * @param location 应解析资源的位置目录的相对位置
	 * @return 给定参数的默认资源查找函数。
	 */
	public static Function<ServerRequest, Optional<Resource>> resourceLookupFunction(String pattern, Resource location) {
		return new PathResourceLookupFunction(pattern, location);
	}

	/**
	 * 使用提供的查找函数路由到资源。如果查找函数为给定的请求提供了{@link Resource}，则将使用处理GET、HEAD和OPTIONS请求的{@link HandlerFunction}公开它。
	 *
	 * @param lookupFunction 根据{@link ServerRequest}提供{@link Resource}的函数
	 * @return 一个路由到资源的路由函数
	 */
	public static RouterFunction<ServerResponse> resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
		return new ResourcesRouterFunction(lookupFunction);
	}

	/**
	 * 更改给定{@linkplain RouterFunction 路由函数}上的{@link PathPatternParser}。此方法可用于更改{@code PathPatternParser}属性的默认值，例如更改{@linkplain PathPatternParser#setCaseSensitive(boolean) 大小写敏感性}。
	 *
	 * @param routerFunction 要在其中更改解析器的路由函数
	 * @param parser         要更改为的解析器。
	 * @param <T>            处理程序函数返回的响应类型
	 * @return 更改后的路由函数
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
	 * 表示路由函数的可发现构建器。
	 * 通过{@link RouterFunctions#route()}获得。
	 */
	public interface Builder {

		/**
		 * 添加一个路由到处理HTTP {@code GET}请求的给定处理程序函数。
		 *
		 * @param handlerFunction 处理所有{@code GET}请求的处理程序函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder GET(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定模式的HTTP {@code GET}请求的给定处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param handlerFunction 处理所有匹配{@code pattern}的{@code GET}请求的处理程序函数
		 * @return 此构建器
		 */
		Builder GET(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定谓词的HTTP {@code GET}请求的给定处理程序函数。
		 *
		 * @param predicate       要匹配的谓词
		 * @param handlerFunction 处理所有匹配{@code predicate}的{@code GET}请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder GET(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定模式和谓词的HTTP {@code GET}请求的给定处理程序函数。
		 * <p>例如，以下示例将GET请求路由到"/user"的接受JSON的{@code listUsers}方法：
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .GET("/user", RequestPredicates.accept(MediaType.APPLICATION_JSON), userController::listUsers)
		 *     .build();
		 * </pre>
		 *
		 * @param pattern         要匹配的模式
		 * @param predicate       要匹配的附加谓词
		 * @param handlerFunction 处理所有匹配{@code pattern}和谓词的{@code GET}请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 */
		Builder GET(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理HTTP {@code HEAD}请求的给定处理程序函数。
		 *
		 * @param handlerFunction 处理所有{@code HEAD}请求的处理程序函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder HEAD(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定模式的HTTP {@code HEAD}请求的给定处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param handlerFunction 处理所有匹配{@code pattern}的{@code HEAD}请求的处理程序函数
		 * @return 此构建器
		 */
		Builder HEAD(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定谓词的HTTP {@code HEAD}请求的给定处理程序函数。
		 *
		 * @param predicate       要匹配的谓词
		 * @param handlerFunction 处理所有匹配{@code predicate}的{@code HEAD}请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder HEAD(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定模式和谓词的HTTP {@code HEAD}请求的给定处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param predicate       要匹配的附加谓词
		 * @param handlerFunction 处理所有匹配{@code pattern}的{@code HEAD}请求的处理程序函数
		 * @return 此构建器
		 */
		Builder HEAD(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理HTTP {@code POST}请求的给定处理程序函数。
		 *
		 * @param handlerFunction 处理所有{@code POST}请求的处理程序函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder POST(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定模式的HTTP {@code POST}请求的给定处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param handlerFunction 处理所有匹配{@code pattern}的{@code POST}请求的处理程序函数
		 * @return 此构建器
		 */
		Builder POST(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定谓词的HTTP {@code POST}请求的给定处理程序函数。
		 *
		 * @param predicate       要匹配的谓词
		 * @param handlerFunction 处理所有匹配{@code predicate}的{@code POST}请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder POST(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定模式和谓词的HTTP {@code POST}请求的给定处理程序函数。
		 * <p>例如，以下示例将包含JSON的"/user"的POST请求路由到{@code userController}中的{@code addUser}方法：
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .POST("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::addUser)
		 *     .build();
		 * </pre>
		 *
		 * @param pattern         要匹配的模式
		 * @param predicate       附加的要匹配的谓词
		 * @param handlerFunction 处理所有匹配{@code pattern}的{@code POST}请求的处理程序函数
		 * @return 此构建器
		 */
		Builder POST(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理HTTP {@code PUT}请求的给定处理程序函数。
		 *
		 * @param handlerFunction 处理所有{@code PUT}请求的处理程序函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder PUT(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定模式的HTTP {@code PUT}请求的给定处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param handlerFunction 处理所有匹配{@code pattern}的{@code PUT}请求的处理程序函数
		 * @return 此构建器
		 */
		Builder PUT(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定谓词的HTTP {@code PUT}请求的给定处理程序函数。
		 *
		 * @param predicate       要匹配的谓词
		 * @param handlerFunction 处理所有匹配{@code predicate}的{@code PUT}请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder PUT(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定模式和谓词的HTTP {@code PUT}请求的给定处理程序函数。
		 * <p>例如，以下示例将包含JSON的"/user"的PUT请求路由到{@code userController}中的{@code editUser}方法：
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .PUT("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::editUser)
		 *     .build();
		 * </pre>
		 *
		 * @param pattern         要匹配的模式
		 * @param predicate       附加的要匹配的谓词
		 * @param handlerFunction 处理所有匹配{@code pattern}的{@code PUT}请求的处理程序函数
		 * @return 此构建器
		 */
		Builder PUT(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理HTTP {@code PATCH}请求的给定处理程序函数。
		 *
		 * @param handlerFunction 处理所有{@code PATCH}请求的处理程序函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder PATCH(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定模式的HTTP {@code PATCH}请求的给定处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param handlerFunction 处理所有匹配{@code pattern}的{@code PATCH}请求的处理程序函数
		 * @return 此构建器
		 */
		Builder PATCH(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定谓词的HTTP {@code PATCH}请求的给定处理程序函数。
		 *
		 * @param predicate       要匹配的谓词
		 * @param handlerFunction 处理所有匹配{@code predicate}的{@code PATCH}请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder PATCH(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定模式和谓词的HTTP {@code PATCH}请求的给定处理程序函数。
		 * <p>例如，以下示例将包含JSON的"/user"的PATCH请求路由到{@code userController}中的{@code editUser}方法：
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .PATCH("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::editUser)
		 *     .build();
		 * </pre>
		 *
		 * @param pattern         要匹配的模式
		 * @param predicate       附加的要匹配的谓词
		 * @param handlerFunction 处理所有匹配{@code pattern}的{@code PATCH}请求的处理程序函数
		 * @return 此构建器
		 */
		Builder PATCH(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理HTTP {@code DELETE}请求的给定处理程序函数。
		 *
		 * @param handlerFunction 处理所有{@code DELETE}请求的处理程序函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder DELETE(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定模式的HTTP {@code DELETE}请求的给定处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param handlerFunction 处理所有匹配{@code pattern}的{@code DELETE}请求的处理程序函数
		 * @return 此构建器
		 */
		Builder DELETE(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定谓词的HTTP {@code DELETE}请求的给定处理程序函数。
		 *
		 * @param predicate       要匹配的谓词
		 * @param handlerFunction 处理所有匹配{@code predicate}的{@code DELETE}请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder DELETE(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定模式和谓词的HTTP {@code DELETE}请求的给定处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param predicate       附加的要匹配的谓词
		 * @param handlerFunction 处理所有匹配{@code pattern}的{@code DELETE}请求的处理程序函数
		 * @return 此构建器
		 */
		Builder DELETE(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理HTTP {@code OPTIONS}请求的给定处理程序函数。
		 *
		 * @param handlerFunction 处理所有{@code OPTIONS}请求的处理程序函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder OPTIONS(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定模式的HTTP {@code OPTIONS}请求的给定处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param handlerFunction 处理所有匹配{@code pattern}的{@code OPTIONS}请求的处理程序函数
		 * @return 此构建器
		 */
		Builder OPTIONS(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定谓词的HTTP {@code OPTIONS}请求的给定处理程序函数。
		 *
		 * @param predicate       要匹配的谓词
		 * @param handlerFunction 处理所有匹配{@code predicate}的{@code OPTIONS}请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder OPTIONS(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定模式和谓词的HTTP {@code OPTIONS}请求的给定处理程序函数。
		 *
		 * @param pattern         要匹配的模式
		 * @param predicate       附加的要匹配的谓词
		 * @param handlerFunction 处理所有匹配{@code pattern}的{@code OPTIONS}请求的处理程序函数
		 * @return 此构建器
		 */
		Builder OPTIONS(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由到处理所有匹配给定谓词的HTTP请求的给定处理程序函数。
		 *
		 * @param predicate       要匹配的请求谓词
		 * @param handlerFunction 处理所有匹配谓词的请求的处理程序函数
		 * @return 此构建器
		 * @see RequestPredicates
		 */
		Builder route(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 将给定的路由函数添加到此构建器中。可以将外部定义的路由函数合并到此构建器中，也可以与{@link RouterFunctions#route(RequestPredicate, HandlerFunction)}结合使用，以允许更灵活的谓词匹配。
		 * <p>例如，以下示例添加了从{@code OrderController.routerFunction()}返回的路由函数。
		 * 到{@code userController}中的{@code changeUser}方法：
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .GET("/users", userController::listUsers)
		 *     .add(orderController.routerFunction());
		 *     .build();
		 * </pre>
		 *
		 * @param routerFunction 要添加的路由函数
		 * @return 此构建器
		 * @see RequestPredicates
		 */
		Builder add(RouterFunction<ServerResponse> routerFunction);

		/**
		 * 将与给定模式匹配的请求路由到相对于给定根位置的资源。
		 * 例如
		 * <pre class="code">
		 * Resource location = new FileSystemResource("public-resources/");
		 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources("/resources/**", location);
		 * </pre>
		 *
		 * @param pattern  要匹配的模式
		 * @param location 应解析资源的位置目录的相对位置
		 * @return 此构建器
		 */
		Builder resources(String pattern, Resource location);

		/**
		 * 使用提供的查找函数路由到资源。如果查找函数为给定请求提供了一个{@link Resource}，它将使用一个处理GET、HEAD和OPTIONS请求的{@link HandlerFunction}来公开。
		 *
		 * @param lookupFunction 用于根据{@link ServerRequest}提供{@link Resource}的函数
		 * @return 此构建器
		 */
		Builder resources(Function<ServerRequest, Optional<Resource>> lookupFunction);

		/**
		 * 如果给定请求谓词适用，则路由到提供的路由函数。此方法可用于创建<strong>嵌套路由</strong>，其中一组路由共享公共路径（前缀）、标头或其他请求谓词。
		 * <p>例如，以下示例创建了一个具有“/user”路径谓词的嵌套路由，以便“/user”的GET请求将列出用户，
		 * 而“/user”的POST请求将创建新用户。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .nest(RequestPredicates.path("/user"), () -&gt;
		 *       RouterFunctions.route()
		 *         .GET(this::listUsers)
		 *         .POST(this::createUser)
		 *         .build())
		 *     .build();
		 * </pre>
		 *
		 * @param predicate              要测试的谓词
		 * @param routerFunctionSupplier 如果谓词适用，则委托给的嵌套路由函数的供应商
		 * @return 此构建器
		 * @see RequestPredicates
		 */
		Builder nest(RequestPredicate predicate, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier);

		/**
		 * 如果给定的请求谓词适用，则路由到构建的路由器函数。
		 * 此方法可用于创建<strong>嵌套路由</strong>，其中一组路由共享公共路径（前缀）、标头或其他请求谓词。
		 * <p>例如，以下示例创建了一个具有“/user”路径谓词的嵌套路由，因此对于“/user”的GET请求将列出用户，
		 * 并且对于“/user”的POST请求将创建新用户。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .nest(RequestPredicates.path("/user"), builder -&gt;
		 *       builder.GET(this::listUsers)
		 *              .POST(this::createUser))
		 *     .build();
		 * </pre>
		 *
		 * @param predicate       要测试的谓词
		 * @param builderConsumer 用于提供嵌套路由器函数的{@code Builder}的消费者
		 * @return 此构建器
		 * @see RequestPredicates
		 */
		Builder nest(RequestPredicate predicate, Consumer<Builder> builderConsumer);

		/**
		 * 如果给定的路径前缀模式适用，则路由到提供的路由器函数。
		 * 此方法可用于创建<strong>嵌套路由</strong>，其中一组路由共享公共路径前缀。
		 * 具体来说，此方法可用于将外部定义的路由器函数合并到路径前缀下。
		 * <p>例如，以下示例创建了一个具有“/user”路径前缀的嵌套路由，该路径前缀委托给{@code userController}中定义的路由器函数，
		 * 并具有一个“/order”路径前缀，该路径前缀委托给{@code orderController}。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .path("/user", userController::routerFunction)
		 *     .path("/order", orderController::routerFunction)
		 *     .build();
		 * </pre>
		 *
		 * @param pattern                要匹配的模式
		 * @param routerFunctionSupplier 用于提供嵌套路由器函数的供应商，以便在模式匹配时委托
		 * @return 此构建器
		 */
		Builder path(String pattern, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier);

		/**
		 * 如果给定路径前缀模式适用，则路由到构建的路由函数。
		 * 此方法可用于创建<strong>嵌套路由</strong>，其中一组路由共享公共路径前缀。
		 * <p>例如，以下示例创建了一个具有“/user”路径谓词的嵌套路由，以便“/user”的GET请求将列出用户，
		 * 而“/user”的POST请求将创建新用户。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .path("/user", builder -&gt;
		 *       builder.GET(this::listUsers)
		 *              .POST(this::createUser))
		 *     .build();
		 * </pre>
		 *
		 * @param pattern         要匹配的模式
		 * @param builderConsumer 提供嵌套路由函数的{@code Builder}的消费者
		 * @return 此构建器
		 */
		Builder path(String pattern, Consumer<Builder> builderConsumer);

		/**
		 * 使用给定的过滤器函数过滤此构建器创建的所有路由。过滤器函数通常用于解决横切关注点，例如日志记录、安全性等。
		 * <p>例如，以下示例创建了一个过滤器，如果请求不包含必要的身份验证标头，则返回401 Unauthorized响应。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .filter((request, next) -&gt; {
		 *       // 检查身份验证标头
		 *       if (isAuthenticated(request)) {
		 *         return next.handle(request);
		 *       }
		 *       else {
		 *         return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
		 *       }
		 *     })
		 *     .build();
		 * </pre>
		 *
		 * @param filterFunction 用于过滤此构建器构建的所有路由的函数
		 * @return 此构建器
		 */
		Builder filter(HandlerFilterFunction<ServerResponse, ServerResponse> filterFunction);

		/**
		 * 使用给定的请求处理函数过滤此构建器创建的所有路由的请求对象。过滤器通常用于解决横切关注点，例如日志记录、安全性等。
		 * <p>例如，以下示例创建了一个在处理程序函数执行之前记录请求的过滤器。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .before(request -&gt; {
		 *       log(request);
		 *       return request;
		 *     })
		 *     .build();
		 * </pre>
		 *
		 * @param requestProcessor 转换请求的函数
		 * @return 此构建器
		 */
		Builder before(Function<ServerRequest, ServerRequest> requestProcessor);

		/**
		 * 使用给定的响应处理函数过滤此构建器创建的所有路由的响应对象。过滤器通常用于解决横切关注点，例如日志记录、安全性等。
		 * <p>例如，以下示例创建了一个在处理程序函数执行后记录响应的过滤器。
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
		 * @param responseProcessor 转换响应的函数
		 * @return 此构建器
		 */
		Builder after(BiFunction<ServerRequest, ServerResponse, ServerResponse> responseProcessor);

		/**
		 * 使用给定的响应提供程序函数过滤与谓词匹配的所有异常。
		 * <p>例如，以下示例创建了一个过滤器，当发生{@code IllegalStateException}时返回500响应状态。
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
		Builder onError(Predicate<Throwable> predicate,
						BiFunction<Throwable, ServerRequest, ServerResponse> responseProvider);

		/**
		 * 使用给定类型的异常过滤所有异常，并应用给定的响应提供程序函数。
		 * <p>例如，以下示例创建了一个过滤器，当发生{@code IllegalStateException}时返回500响应状态。
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
		Builder onError(Class<? extends Throwable> exceptionType,
						BiFunction<Throwable, ServerRequest, ServerResponse> responseProvider);

		/**
		 * 将具有给定名称和值的属性添加到此构建器构建的最后一个路由中。
		 *
		 * @param name  属性名称
		 * @param value 属性值
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder withAttribute(String name, Object value);

		/**
		 * 使用给定的消费者函数处理最后一个路由构建的属性。
		 * <p>提供给消费者的映射是“活动的”，因此消费者可以用于{@linkplain Map#put(Object, Object)覆盖}现有属性、
		 * {@linkplain Map#remove(Object) 删除}属性，或使用任何其他{@link Map}方法。
		 *
		 * @param attributesConsumer 消费属性映射的函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder withAttributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * 构建{@code RouterFunction}。所有创建的路由都是{@linkplain RouterFunction#and(RouterFunction) 组合}在一起，并且应用了过滤器（如果有）。
		 *
		 * @return 构建的路由器函数
		 */
		RouterFunction<ServerResponse> build();
	}


	/**
	 * 从路由函数的逻辑结构接收通知。
	 */
	public interface Visitor {

		/**
		 * 接收嵌套路由函数开始的通知。
		 *
		 * @param predicate 应用于嵌套路由函数的谓词
		 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
		 */
		void startNested(RequestPredicate predicate);

		/**
		 * 接收嵌套路由函数结束的通知。
		 *
		 * @param predicate 应用于嵌套路由函数的谓词
		 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
		 */
		void endNested(RequestPredicate predicate);

		/**
		 * 接收到针对处理程序函数的标准谓词路由的通知。
		 *
		 * @param predicate       应用于处理程序函数的谓词
		 * @param handlerFunction 处理程序函数
		 * @see RouterFunctions#route(RequestPredicate, HandlerFunction)
		 */
		void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction);

		/**
		 * 接收到资源路由函数的通知。
		 *
		 * @param lookupFunction 资源的查找函数
		 * @see RouterFunctions#resources(Function)
		 */
		void resources(Function<ServerRequest, Optional<Resource>> lookupFunction);

		/**
		 * 接收到具有属性的路由函数的通知。给定的属性适用于在此通知之后的路由函数。
		 *
		 * @param attributes 适用于以下路由的属性
		 * @since 5.3
		 */
		void attributes(Map<String, Object> attributes);

		/**
		 * 接收到未知路由函数的通知。对于不是通过各种 {@link RouterFunctions} 方法创建的路由函数，将调用此方法。
		 *
		 * @param routerFunction 路由函数
		 */
		void unknown(RouterFunction<?> routerFunction);
	}


	abstract static class AbstractRouterFunction<T extends ServerResponse> implements RouterFunction<T> {

		@Override
		public String toString() {
			// 创建 ToStringVisitor 实例
			ToStringVisitor visitor = new ToStringVisitor();
			// 处理当前节点
			accept(visitor);
			// 返回 ToStringVisitor 的字符串表示形式
			return visitor.toString();
		}
	}


	/**
	 * 一个复合的路由函数，首先调用一个函数，然后如果此路由器函数没有结果，则调用另一个函数（相同响应类型{@code T}）。
	 *
	 * @param <T> 服务器响应类型
	 */
	static final class SameComposedRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {
		/**
		 * 第一个路由函数
		 */
		private final RouterFunction<T> first;

		/**
		 * 第二个路由函数
		 */
		private final RouterFunction<T> second;

		public SameComposedRouterFunction(RouterFunction<T> first, RouterFunction<T> second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public Optional<HandlerFunction<T>> route(ServerRequest request) {
			// 尝试在第一个路由函数中匹配请求
			Optional<HandlerFunction<T>> firstRoute = this.first.route(request);
			// 如果在第一个路由函数中找到了匹配的处理器函数，则返回该处理器函数
			if (firstRoute.isPresent()) {
				return firstRoute;
			} else {
				// 否则，尝试在第二个路由函数中匹配请求
				return this.second.route(request);
			}
		}

		@Override
		public void accept(Visitor visitor) {
			this.first.accept(visitor);
			this.second.accept(visitor);
		}
	}


	/**
	 * 一个复合的路由函数，首先调用一个函数，然后如果此路由器函数没有结果，则调用另一个函数（不同响应类型）。
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

		public DifferentComposedRouterFunction(RouterFunction<?> first, RouterFunction<?> second) {
			this.first = first;
			this.second = second;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Optional<HandlerFunction<ServerResponse>> route(ServerRequest request) {
			// 尝试在第一个路由函数中匹配请求
			Optional<? extends HandlerFunction<?>> firstRoute = this.first.route(request);
			// 如果在第一个路由函数中找到了匹配的处理器函数，则返回该处理器函数
			if (firstRoute.isPresent()) {
				return (Optional<HandlerFunction<ServerResponse>>) firstRoute;
			} else {
				// 否则，尝试在第二个路由函数中匹配请求
				Optional<? extends HandlerFunction<?>> secondRoute = this.second.route(request);
				return (Optional<HandlerFunction<ServerResponse>>) secondRoute;
			}
		}

		@Override
		public void accept(Visitor visitor) {
			this.first.accept(visitor);
			this.second.accept(visitor);
		}
	}


	/**
	 * 使用给定的 {@linkplain HandlerFilterFunction 过滤器函数} 过滤指定的 {@linkplain HandlerFunction 处理函数}。
	 *
	 * @param <T> 要过滤的 {@linkplain HandlerFunction 处理函数} 的类型
	 * @param <S> 函数的响应类型
	 */
	static final class FilteredRouterFunction<T extends ServerResponse, S extends ServerResponse>
			implements RouterFunction<S> {
		/**
		 * 路由函数
		 */
		private final RouterFunction<T> routerFunction;

		/**
		 * 过滤函数
		 */
		private final HandlerFilterFunction<T, S> filterFunction;

		public FilteredRouterFunction(
				RouterFunction<T> routerFunction,
				HandlerFilterFunction<T, S> filterFunction) {
			this.routerFunction = routerFunction;
			this.filterFunction = filterFunction;
		}

		@Override
		public Optional<HandlerFunction<S>> route(ServerRequest request) {
			return this.routerFunction.route(request).map(this.filterFunction::apply);
		}

		@Override
		public void accept(Visitor visitor) {
			this.routerFunction.accept(visitor);
		}

		@Override
		public String toString() {
			return this.routerFunction.toString();
		}
	}


	private static final class DefaultRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {
		/**
		 * 请求断言
		 */
		private final RequestPredicate predicate;

		/**
		 * 处理器函数
		 */
		private final HandlerFunction<T> handlerFunction;

		public DefaultRouterFunction(RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
			Assert.notNull(predicate, "Predicate must not be null");
			Assert.notNull(handlerFunction, "HandlerFunction must not be null");
			this.predicate = predicate;
			this.handlerFunction = handlerFunction;
		}

		@Override
		public Optional<HandlerFunction<T>> route(ServerRequest request) {
			// 检查请求是否与谓词匹配
			if (this.predicate.test(request)) {
				// 如果匹配，记录日志并返回处理器函数
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Predicate \"%s\" matches against \"%s\"", this.predicate, request));
				}
				return Optional.of(this.handlerFunction);
			} else {
				// 如果不匹配，则返回空Optional
				return Optional.empty();
			}
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.route(this.predicate, this.handlerFunction);
		}

	}


	private static final class DefaultNestedRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {
		/**
		 * 请求断言
		 */
		private final RequestPredicate predicate;

		/**
		 * 路由函数
		 */
		private final RouterFunction<T> routerFunction;

		public DefaultNestedRouterFunction(RequestPredicate predicate, RouterFunction<T> routerFunction) {
			Assert.notNull(predicate, "Predicate must not be null");
			Assert.notNull(routerFunction, "RouterFunction must not be null");
			this.predicate = predicate;
			this.routerFunction = routerFunction;
		}

		@Override
		public Optional<HandlerFunction<T>> route(ServerRequest serverRequest) {
			// 使用嵌套的服务器请求来评估谓词
			return this.predicate.nest(serverRequest)
					.map(nestedRequest -> {
						// 如果日志级别为跟踪，则记录嵌套谓词与请求的匹配情况
						if (logger.isTraceEnabled()) {
							logger.trace(
									String.format(
											"Nested predicate \"%s\" matches against \"%s\"",
											this.predicate, serverRequest));
						}
						// 获取嵌套请求的路由函数结果
						Optional<HandlerFunction<T>> result =
								this.routerFunction.route(nestedRequest);
						// 如果结果存在且嵌套请求与原始请求不同
						if (result.isPresent() && nestedRequest != serverRequest) {
							// 则清除原始请求的属性
							serverRequest.attributes().clear();
							// 将嵌套请求的属性添加到原始请求中
							serverRequest.attributes().putAll(nestedRequest.attributes());
						}
						return result;
					})
					// 如果嵌套请求为空，则返回空Optional
					.orElseGet(Optional::empty);
		}


		@Override
		public void accept(Visitor visitor) {
			// 开始嵌套访问
			visitor.startNested(this.predicate);
			// 接受路由函数的访问
			this.routerFunction.accept(visitor);
			// 结束嵌套访问
			visitor.endNested(this.predicate);
		}

	}


	private static class ResourcesRouterFunction extends AbstractRouterFunction<ServerResponse> {
		/**
		 * 要寻找的函数
		 */
		private final Function<ServerRequest, Optional<Resource>> lookupFunction;

		public ResourcesRouterFunction(Function<ServerRequest, Optional<Resource>> lookupFunction) {
			Assert.notNull(lookupFunction, "Function must not be null");
			this.lookupFunction = lookupFunction;
		}

		@Override
		public Optional<HandlerFunction<ServerResponse>> route(ServerRequest request) {
			return this.lookupFunction.apply(request).map(ResourceHandlerFunction::new);
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.resources(this.lookupFunction);
		}
	}


	static final class AttributesRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {
		/**
		 * 路由函数
		 */
		private final RouterFunction<T> delegate;

		/**
		 * 属性值
		 */
		private final Map<String, Object> attributes;

		public AttributesRouterFunction(RouterFunction<T> delegate, Map<String, Object> attributes) {
			this.delegate = delegate;
			this.attributes = initAttributes(attributes);
		}

		private static Map<String, Object> initAttributes(Map<String, Object> attributes) {
			if (attributes.isEmpty()) {
				// 如果属性为空，返回一个空的不可变映射
				return Collections.emptyMap();
			} else {
				// 如果属性不为空，返回一个不可修改的链接哈希映射
				return Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
			}
		}

		@Override
		public Optional<HandlerFunction<T>> route(ServerRequest request) {
			return this.delegate.route(request);
		}

		@Override
		public void accept(Visitor visitor) {
			// 设置属性值
			visitor.attributes(this.attributes);
			// 添加访问器
			this.delegate.accept(visitor);
		}

		@Override
		public RouterFunction<T> withAttribute(String name, Object value) {
			Assert.hasLength(name, "Name must not be empty");
			Assert.notNull(value, "Value must not be null");

			// 创建一个新的 LinkedHashMap 对象，将原始属性复制到其中
			Map<String, Object> attributes = new LinkedHashMap<>(this.attributes);
			// 将新的属性添加到 attributes 中
			attributes.put(name, value);
			// 返回一个包含新属性的新 AttributesRouterFunction 对象
			return new AttributesRouterFunction<>(this.delegate, attributes);
		}

		@Override
		public RouterFunction<T> withAttributes(Consumer<Map<String, Object>> attributesConsumer) {
			Assert.notNull(attributesConsumer, "AttributesConsumer must not be null");

			// 创建一个新的 LinkedHashMap 对象，将原始属性复制到其中
			Map<String, Object> attributes = new LinkedHashMap<>(this.attributes);
			// 将新的属性添加到 attributes 中
			attributesConsumer.accept(attributes);
			// 返回一个包含新属性的新 AttributesRouterFunction 对象
			return new AttributesRouterFunction<>(this.delegate, attributes);
		}
	}


}
