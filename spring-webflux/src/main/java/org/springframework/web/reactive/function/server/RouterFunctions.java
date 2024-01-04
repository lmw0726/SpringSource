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
 * <strong>Central entry point to Spring's functional web framework.</strong>
 * Exposes routing functionality, such as to {@linkplain #route() create} a
 * {@code RouterFunction} using a discoverable builder-style API, to
 * {@linkplain #route(RequestPredicate, HandlerFunction) create} a {@code RouterFunction}
 * given a {@code RequestPredicate} and {@code HandlerFunction}, and to do further
 * {@linkplain #nest(RequestPredicate, RouterFunction) subrouting} on an existing routing
 * function.
 *
 * <p>Additionally, this class can {@linkplain #toHttpHandler(RouterFunction) transform} a
 * {@code RouterFunction} into an {@code HttpHandler}, which can be run in Servlet 3.1+,
 * Reactor, or Undertow.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class RouterFunctions {

	private static final Log logger = LogFactory.getLog(RouterFunctions.class);

	/**
	 * Name of the {@link ServerWebExchange} attribute that contains the {@link ServerRequest}.
	 */
	public static final String REQUEST_ATTRIBUTE = RouterFunctions.class.getName() + ".request";

	/**
	 * Name of the {@link ServerWebExchange} attribute that contains the URI
	 * templates map, mapping variable names to values.
	 */
	public static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE =
			RouterFunctions.class.getName() + ".uriTemplateVariables";

	/**
	 * Name of the {@link ServerWebExchange#getAttributes() attribute} that
	 * contains the matching pattern, as a {@link org.springframework.web.util.pattern.PathPattern}.
	 */
	public static final String MATCHING_PATTERN_ATTRIBUTE =
			RouterFunctions.class.getName() + ".matchingPattern";


	/**
	 * Offers a discoverable way to create router functions through a builder-style interface.
	 *
	 * @return a router function builder
	 * @since 5.1
	 */
	public static Builder route() {
		return new RouterFunctionBuilder();
	}

	/**
	 * Route to the given handler function if the given request predicate applies.
	 * <p>For instance, the following example routes GET requests for "/user" to the
	 * {@code listUsers} method in {@code userController}:
	 * <pre class="code">
	 * RouterFunction&lt;ServerResponse&gt; route =
	 *     RouterFunctions.route(RequestPredicates.GET("/user"), userController::listUsers);
	 * </pre>
	 *
	 * @param predicate       the predicate to test
	 * @param handlerFunction the handler function to route to if the predicate applies
	 * @param <T>             the type of response returned by the handler function
	 * @return a router function that routes to {@code handlerFunction} if
	 * {@code predicate} evaluates to {@code true}
	 * @see RequestPredicates
	 */
	public static <T extends ServerResponse> RouterFunction<T> route(
			RequestPredicate predicate, HandlerFunction<T> handlerFunction) {

		return new DefaultRouterFunction<>(predicate, handlerFunction);
	}

	/**
	 * Route to the given router function if the given request predicate applies. This method can be
	 * used to create <strong>nested routes</strong>, where a group of routes share a common path
	 * (prefix), header, or other request predicate.
	 * <p>For instance, the following example first creates a composed route that resolves to
	 * {@code listUsers} for a GET, and {@code createUser} for a POST. This composed route then gets
	 * nested with a "/user" path predicate, so that GET requests for "/user" will list users,
	 * and POST request for "/user" will create a new user.
	 * <pre class="code">
	 * RouterFunction&lt;ServerResponse&gt; userRoutes =
	 *   RouterFunctions.route(RequestPredicates.method(HttpMethod.GET), this::listUsers)
	 *     .andRoute(RequestPredicates.method(HttpMethod.POST), this::createUser);
	 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
	 *   RouterFunctions.nest(RequestPredicates.path("/user"), userRoutes);
	 * </pre>
	 *
	 * @param predicate      the predicate to test
	 * @param routerFunction the nested router function to delegate to if the predicate applies
	 * @param <T>            the type of response returned by the handler function
	 * @return a router function that routes to {@code routerFunction} if
	 * {@code predicate} evaluates to {@code true}
	 * @see RequestPredicates
	 */
	public static <T extends ServerResponse> RouterFunction<T> nest(
			RequestPredicate predicate, RouterFunction<T> routerFunction) {

		return new DefaultNestedRouterFunction<>(predicate, routerFunction);
	}

	/**
	 * Route requests that match the given pattern to resources relative to the given root location.
	 * For instance
	 * <pre class="code">
	 * Resource location = new FileSystemResource("public-resources/");
	 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources("/resources/**", location);
	 * </pre>
	 *
	 * @param pattern  the pattern to match
	 * @param location the location directory relative to which resources should be resolved
	 * @return a router function that routes to resources
	 * @see #resourceLookupFunction(String, Resource)
	 */
	public static RouterFunction<ServerResponse> resources(String pattern, Resource location) {
		return resources(resourceLookupFunction(pattern, location));
	}

	/**
	 * Returns the resource lookup function used by {@link #resources(String, Resource)}.
	 * The returned function can be {@linkplain Function#andThen(Function) composed} on, for
	 * instance to return a default resource when the lookup function does not match:
	 * <pre class="code">
	 * Mono&lt;Resource&gt; defaultResource = Mono.just(new ClassPathResource("index.html"));
	 * Function&lt;ServerRequest, Mono&lt;Resource&gt;&gt; lookupFunction =
	 *   RouterFunctions.resourceLookupFunction("/resources/**", new FileSystemResource("public-resources/"))
	 *     .andThen(resourceMono -&gt; resourceMono.switchIfEmpty(defaultResource));
	 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources(lookupFunction);
	 * </pre>
	 *
	 * @param pattern  the pattern to match
	 * @param location the location directory relative to which resources should be resolved
	 * @return the default resource lookup function for the given parameters.
	 */
	public static Function<ServerRequest, Mono<Resource>> resourceLookupFunction(String pattern, Resource location) {
		return new PathResourceLookupFunction(pattern, location);
	}

	/**
	 * Route to resources using the provided lookup function. If the lookup function provides a
	 * {@link Resource} for the given request, it will be it will be exposed using a
	 * {@link HandlerFunction} that handles GET, HEAD, and OPTIONS requests.
	 *
	 * @param lookupFunction the function to provide a {@link Resource} given the {@link ServerRequest}
	 * @return a router function that routes to resources
	 */
	public static RouterFunction<ServerResponse> resources(Function<ServerRequest, Mono<Resource>> lookupFunction) {
		return new ResourcesRouterFunction(lookupFunction);
	}

	/**
	 * Convert the given {@linkplain RouterFunction router function} into a {@link HttpHandler}.
	 * This conversion uses {@linkplain HandlerStrategies#builder() default strategies}.
	 * <p>The returned handler can be adapted to run in
	 * <ul>
	 * <li>Servlet 3.1+ using the
	 * {@link org.springframework.http.server.reactive.ServletHttpHandlerAdapter},</li>
	 * <li>Reactor using the
	 * {@link org.springframework.http.server.reactive.ReactorHttpHandlerAdapter},</li>
	 * <li>Undertow using the
	 * {@link org.springframework.http.server.reactive.UndertowHttpHandlerAdapter}.</li>
	 * </ul>
	 * <p>Note that {@code HttpWebHandlerAdapter} also implements {@link WebHandler}, allowing
	 * for additional filter and exception handler registration through
	 * {@link WebHttpHandlerBuilder}.
	 *
	 * @param routerFunction the router function to convert
	 * @return an http handler that handles HTTP request using the given router function
	 */
	public static HttpHandler toHttpHandler(RouterFunction<?> routerFunction) {
		return toHttpHandler(routerFunction, HandlerStrategies.withDefaults());
	}

	/**
	 * Convert the given {@linkplain RouterFunction router function} into a {@link HttpHandler},
	 * using the given strategies.
	 * <p>The returned {@code HttpHandler} can be adapted to run in
	 * <ul>
	 * <li>Servlet 3.1+ using the
	 * {@link org.springframework.http.server.reactive.ServletHttpHandlerAdapter},</li>
	 * <li>Reactor using the
	 * {@link org.springframework.http.server.reactive.ReactorHttpHandlerAdapter},</li>
	 * <li>Undertow using the
	 * {@link org.springframework.http.server.reactive.UndertowHttpHandlerAdapter}.</li>
	 * </ul>
	 *
	 * @param routerFunction the router function to convert
	 * @param strategies     the strategies to use
	 * @return an http handler that handles HTTP request using the given router function
	 */
	public static HttpHandler toHttpHandler(RouterFunction<?> routerFunction, HandlerStrategies strategies) {
		WebHandler webHandler = toWebHandler(routerFunction, strategies);
		return WebHttpHandlerBuilder.webHandler(webHandler)
				.filters(filters -> filters.addAll(strategies.webFilters()))
				.exceptionHandlers(handlers -> handlers.addAll(strategies.exceptionHandlers()))
				.localeContextResolver(strategies.localeContextResolver())
				.build();
	}

	/**
	 * Convert the given {@linkplain RouterFunction router function} into a {@link WebHandler}.
	 * This conversion uses {@linkplain HandlerStrategies#builder() default strategies}.
	 *
	 * @param routerFunction the router function to convert
	 * @return a web handler that handles web request using the given router function
	 */
	public static WebHandler toWebHandler(RouterFunction<?> routerFunction) {
		return toWebHandler(routerFunction, HandlerStrategies.withDefaults());
	}

	/**
	 * Convert the given {@linkplain RouterFunction router function} into a {@link WebHandler},
	 * using the given strategies.
	 *
	 * @param routerFunction the router function to convert
	 * @param strategies     the strategies to use
	 * @return a web handler that handles web request using the given router function
	 */
	public static WebHandler toWebHandler(RouterFunction<?> routerFunction, HandlerStrategies strategies) {
		Assert.notNull(routerFunction, "RouterFunction must not be null");
		Assert.notNull(strategies, "HandlerStrategies must not be null");

		return new RouterFunctionWebHandler(strategies, routerFunction);
	}

	/**
	 * Changes the {@link PathPatternParser} on the given {@linkplain RouterFunction router function}. This method
	 * can be used to change the {@code PathPatternParser} properties from the defaults, for instance to change
	 * {@linkplain PathPatternParser#setCaseSensitive(boolean) case sensitivity}.
	 *
	 * @param routerFunction the router function to change the parser in
	 * @param parser         the parser to change to.
	 * @param <T>            the type of response returned by the handler function
	 * @return the change router function
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
	 * Represents a discoverable builder for router functions.
	 * Obtained via {@link RouterFunctions#route()}.
	 *
	 * @since 5.1
	 */
	public interface Builder {

		/**
		 * Adds a route to the given handler function that handles HTTP {@code GET} requests.
		 *
		 * @param handlerFunction the handler function to handle all {@code GET} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder GET(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code GET} requests
		 * that match the given pattern.
		 *
		 * @param pattern         the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code GET} requests that
		 *                        match {@code pattern}
		 * @return this builder
		 */
		Builder GET(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code GET} requests
		 * that match the given predicate.
		 *
		 * @param predicate       predicate to match
		 * @param handlerFunction the handler function to handle all {@code GET} requests that
		 *                        match {@code predicate}
		 * @return this builder
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder GET(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code GET} requests
		 * that match the given pattern and predicate.
		 * <p>For instance, the following example routes GET requests for "/user" that accept JSON
		 * to the {@code listUsers} method in {@code userController}:
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .GET("/user", RequestPredicates.accept(MediaType.APPLICATION_JSON), userController::listUsers)
		 *     .build();
		 * </pre>
		 *
		 * @param pattern         the pattern to match to
		 * @param predicate       additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code GET} requests that
		 *                        match {@code pattern} and the predicate
		 * @return this builder
		 * @see RequestPredicates
		 */
		Builder GET(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles HTTP {@code HEAD} requests.
		 *
		 * @param handlerFunction the handler function to handle all {@code HEAD} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder HEAD(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code HEAD} requests
		 * that match the given pattern.
		 *
		 * @param pattern         the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code HEAD} requests that
		 *                        match {@code pattern}
		 * @return this builder
		 */
		Builder HEAD(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code HEAD} requests
		 * that match the given predicate.
		 *
		 * @param predicate       predicate to match
		 * @param handlerFunction the handler function to handle all {@code HEAD} requests that
		 *                        match {@code predicate}
		 * @return this builder
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder HEAD(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code HEAD} requests
		 * that match the given pattern and predicate.
		 *
		 * @param pattern         the pattern to match to
		 * @param predicate       additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code HEAD} requests that
		 *                        match {@code pattern}
		 * @return this builder
		 */
		Builder HEAD(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles HTTP {@code POST} requests.
		 *
		 * @param handlerFunction the handler function to handle all {@code POST} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder POST(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code POST} requests
		 * that match the given pattern.
		 *
		 * @param pattern         the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code POST} requests that
		 *                        match {@code pattern}
		 * @return this builder
		 */
		Builder POST(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code POST} requests
		 * that match the given predicate.
		 *
		 * @param predicate       predicate to match
		 * @param handlerFunction the handler function to handle all {@code POST} requests that
		 *                        match {@code predicate}
		 * @return this builder
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder POST(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code POST} requests
		 * that match the given pattern and predicate.
		 * <p>For instance, the following example routes POST requests for "/user" that contain JSON
		 * to the {@code addUser} method in {@code userController}:
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .POST("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::addUser)
		 *     .build();
		 * </pre>
		 *
		 * @param pattern         the pattern to match to
		 * @param predicate       additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code POST} requests that
		 *                        match {@code pattern}
		 * @return this builder
		 */
		Builder POST(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles HTTP {@code PUT} requests.
		 *
		 * @param handlerFunction the handler function to handle all {@code PUT} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder PUT(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code PUT} requests
		 * that match the given pattern.
		 *
		 * @param pattern         the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code PUT} requests that
		 *                        match {@code pattern}
		 * @return this builder
		 */
		Builder PUT(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code PUT} requests
		 * that match the given predicate.
		 *
		 * @param predicate       predicate to match
		 * @param handlerFunction the handler function to handle all {@code PUT} requests that
		 *                        match {@code predicate}
		 * @return this builder
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder PUT(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code PUT} requests
		 * that match the given pattern and predicate.
		 * <p>For instance, the following example routes PUT requests for "/user" that contain JSON
		 * to the {@code editUser} method in {@code userController}:
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .PUT("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::editUser)
		 *     .build();
		 * </pre>
		 *
		 * @param pattern         the pattern to match to
		 * @param predicate       additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code PUT} requests that
		 *                        match {@code pattern}
		 * @return this builder
		 */
		Builder PUT(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles HTTP {@code PATCH} requests.
		 *
		 * @param handlerFunction the handler function to handle all {@code PATCH} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder PATCH(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code PATCH} requests
		 * that match the given pattern.
		 *
		 * @param pattern         the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code PATCH} requests that
		 *                        match {@code pattern}
		 * @return this builder
		 */
		Builder PATCH(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code PATCH} requests
		 * that match the given predicate.
		 *
		 * @param predicate       predicate to match
		 * @param handlerFunction the handler function to handle all {@code PATCH} requests that
		 *                        match {@code predicate}
		 * @return this builder
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder PATCH(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code PATCH} requests
		 * that match the given pattern and predicate.
		 * <p>For instance, the following example routes PATCH requests for "/user" that contain JSON
		 * to the {@code editUser} method in {@code userController}:
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .PATCH("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::editUser)
		 *     .build();
		 * </pre>
		 *
		 * @param pattern         the pattern to match to
		 * @param predicate       additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code PATCH} requests that
		 *                        match {@code pattern}
		 * @return this builder
		 */
		Builder PATCH(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles HTTP {@code DELETE} requests.
		 *
		 * @param handlerFunction the handler function to handle all {@code DELETE} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder DELETE(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code DELETE} requests
		 * that match the given pattern.
		 *
		 * @param pattern         the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code DELETE} requests that
		 *                        match {@code pattern}
		 * @return this builder
		 */
		Builder DELETE(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code DELETE} requests
		 * that match the given predicate.
		 *
		 * @param predicate       predicate to match
		 * @param handlerFunction the handler function to handle all {@code DELETE} requests that
		 *                        match {@code predicate}
		 * @return this builder
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder DELETE(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code DELETE} requests
		 * that match the given pattern and predicate.
		 *
		 * @param pattern         the pattern to match to
		 * @param predicate       additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code DELETE} requests that
		 *                        match {@code pattern}
		 * @return this builder
		 */
		Builder DELETE(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles HTTP {@code OPTIONS} requests.
		 *
		 * @param handlerFunction the handler function to handle all {@code OPTIONS} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder OPTIONS(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code OPTIONS} requests
		 * that match the given pattern.
		 *
		 * @param pattern         the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code OPTIONS} requests that
		 *                        match {@code pattern}
		 * @return this builder
		 */
		Builder OPTIONS(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code OPTIONS} requests
		 * that match the given predicate.
		 *
		 * @param predicate       predicate to match
		 * @param handlerFunction the handler function to handle all {@code OPTIONS} requests that
		 *                        match {@code predicate}
		 * @return this builder
		 * @see RequestPredicates
		 * @since 5.3
		 */
		Builder OPTIONS(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code OPTIONS} requests
		 * that match the given pattern and predicate.
		 *
		 * @param pattern         the pattern to match to
		 * @param predicate       additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code OPTIONS} requests that
		 *                        match {@code pattern}
		 * @return this builder
		 */
		Builder OPTIONS(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all requests that match the
		 * given predicate.
		 *
		 * @param predicate       the request predicate to match
		 * @param handlerFunction the handler function to handle all requests that match the predicate
		 * @return this builder
		 * @see RequestPredicates
		 * @since 5.2
		 */
		Builder route(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds the given route to this builder. Can be used to merge externally defined router
		 * functions into this builder, or can be combined with
		 * {@link RouterFunctions#route(RequestPredicate, HandlerFunction)}
		 * to allow for more flexible predicate matching.
		 * <p>For instance, the following example adds the router function returned from
		 * {@code OrderController.routerFunction()}.
		 * to the {@code changeUser} method in {@code userController}:
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .GET("/users", userController::listUsers)
		 *     .add(orderController.routerFunction());
		 *     .build();
		 * </pre>
		 *
		 * @param routerFunction the router function to be added
		 * @return this builder
		 * @see RequestPredicates
		 */
		Builder add(RouterFunction<ServerResponse> routerFunction);

		/**
		 * Route requests that match the given pattern to resources relative to the given root location.
		 * For instance
		 * <pre class="code">
		 * Resource location = new FileSystemResource("public-resources/");
		 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources("/resources/**", location);
		 * </pre>
		 *
		 * @param pattern  the pattern to match
		 * @param location the location directory relative to which resources should be resolved
		 * @return this builder
		 */
		Builder resources(String pattern, Resource location);

		/**
		 * Route to resources using the provided lookup function. If the lookup function provides a
		 * {@link Resource} for the given request, it will be it will be exposed using a
		 * {@link HandlerFunction} that handles GET, HEAD, and OPTIONS requests.
		 *
		 * @param lookupFunction the function to provide a {@link Resource} given the {@link ServerRequest}
		 * @return this builder
		 */
		Builder resources(Function<ServerRequest, Mono<Resource>> lookupFunction);

		/**
		 * Route to the supplied router function if the given request predicate applies. This method
		 * can be used to create <strong>nested routes</strong>, where a group of routes share a
		 * common path (prefix), header, or other request predicate.
		 * <p>For instance, the following example creates a nested route with a "/user" path
		 * predicate, so that GET requests for "/user" will list users,
		 * and POST request for "/user" will create a new user.
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
		 * @param predicate              the predicate to test
		 * @param routerFunctionSupplier supplier for the nested router function to delegate to if
		 *                               the predicate applies
		 * @return this builder
		 * @see RequestPredicates
		 */
		Builder nest(RequestPredicate predicate, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier);

		/**
		 * Route to a built router function if the given request predicate applies.
		 * This method can be used to create <strong>nested routes</strong>, where a group of routes
		 * share a common path (prefix), header, or other request predicate.
		 * <p>For instance, the following example creates a nested route with a "/user" path
		 * predicate, so that GET requests for "/user" will list users,
		 * and POST request for "/user" will create a new user.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .nest(RequestPredicates.path("/user"), builder -&gt;
		 *       builder.GET(this::listUsers)
		 *              .POST(this::createUser))
		 *     .build();
		 * </pre>
		 *
		 * @param predicate       the predicate to test
		 * @param builderConsumer consumer for a {@code Builder} that provides the nested router
		 *                        function
		 * @return this builder
		 * @see RequestPredicates
		 */
		Builder nest(RequestPredicate predicate, Consumer<Builder> builderConsumer);

		/**
		 * Route to the supplied router function if the given path prefix pattern applies. This method
		 * can be used to create <strong>nested routes</strong>, where a group of routes share a
		 * common path prefix. Specifically, this method can be used to merge externally defined
		 * router functions under a path prefix.
		 * <p>For instance, the following example creates a nested route with a "/user" path
		 * predicate that delegates to the router function defined in {@code userController},
		 * and with a "/order" path that delegates to {@code orderController}.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .path("/user", userController::routerFunction)
		 *     .path("/order", orderController::routerFunction)
		 *     .build();
		 * </pre>
		 *
		 * @param pattern                the pattern to match to
		 * @param routerFunctionSupplier supplier for the nested router function to delegate to if
		 *                               the pattern matches
		 * @return this builder
		 */
		Builder path(String pattern, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier);

		/**
		 * Route to a built router function if the given path prefix pattern applies.
		 * This method can be used to create <strong>nested routes</strong>, where a group of routes
		 * share a common path prefix.
		 * <p>For instance, the following example creates a nested route with a "/user" path
		 * predicate, so that GET requests for "/user" will list users,
		 * and POST request for "/user" will create a new user.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .path("/user", builder -&gt;
		 *       builder.GET(this::listUsers)
		 *              .POST(this::createUser))
		 *     .build();
		 * </pre>
		 *
		 * @param pattern         the pattern to match to
		 * @param builderConsumer consumer for a {@code Builder} that provides the nested router
		 *                        function
		 * @return this builder
		 */
		Builder path(String pattern, Consumer<Builder> builderConsumer);

		/**
		 * Filters all routes created by this builder with the given filter function. Filter
		 * functions are typically used to address cross-cutting concerns, such as logging,
		 * security, etc.
		 * <p>For instance, the following example creates a filter that returns a 401 Unauthorized
		 * response if the request does not contain the necessary authentication headers.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .filter((request, next) -&gt; {
		 *       // check for authentication headers
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
		 * @param filterFunction the function to filter all routes built by this builder
		 * @return this builder
		 */
		Builder filter(HandlerFilterFunction<ServerResponse, ServerResponse> filterFunction);

		/**
		 * Filter the request object for all routes created by this builder with the given request
		 * processing function. Filters are typically used to address cross-cutting concerns, such
		 * as logging, security, etc.
		 * <p>For instance, the following example creates a filter that logs the request before
		 * the handler function executes.
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
		 * @param requestProcessor a function that transforms the request
		 * @return this builder
		 */
		Builder before(Function<ServerRequest, ServerRequest> requestProcessor);

		/**
		 * Filter the response object for all routes created by this builder with the given response
		 * processing function. Filters are typically used to address cross-cutting concerns, such
		 * as logging, security, etc.
		 * <p>For instance, the following example creates a filter that logs the response after
		 * the handler function executes.
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
		 * @param responseProcessor a function that transforms the response
		 * @return this builder
		 */
		Builder after(BiFunction<ServerRequest, ServerResponse, ServerResponse> responseProcessor);

		/**
		 * Filters all exceptions that match the predicate by applying the given response provider
		 * function.
		 * <p>For instance, the following example creates a filter that returns a 500 response
		 * status when an {@code IllegalStateException} occurs.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .onError(e -&gt; e instanceof IllegalStateException,
		 *       (e, request) -&gt; ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
		 *     .build();
		 * </pre>
		 *
		 * @param predicate        the type of exception to filter
		 * @param responseProvider a function that creates a response
		 * @return this builder
		 */
		Builder onError(Predicate<? super Throwable> predicate,
						BiFunction<? super Throwable, ServerRequest, Mono<ServerResponse>> responseProvider);

		/**
		 * Filters all exceptions of the given type by applying the given response provider
		 * function.
		 * <p>For instance, the following example creates a filter that returns a 500 response
		 * status when an {@code IllegalStateException} occurs.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .onError(IllegalStateException.class,
		 *       (e, request) -&gt; ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
		 *     .build();
		 * </pre>
		 *
		 * @param exceptionType    the type of exception to filter
		 * @param responseProvider a function that creates a response
		 * @return this builder
		 */
		<T extends Throwable> Builder onError(Class<T> exceptionType,
											  BiFunction<? super T, ServerRequest, Mono<ServerResponse>> responseProvider);

		/**
		 * Add an attribute with the given name and value to the last route built with this builder.
		 *
		 * @param name  the attribute name
		 * @param value the attribute value
		 * @return this builder
		 * @since 5.3
		 */
		Builder withAttribute(String name, Object value);

		/**
		 * Manipulate the attributes of the last route built with the given consumer.
		 * <p>The map provided to the consumer is "live", so that the consumer can be used
		 * to {@linkplain Map#put(Object, Object) overwrite} existing attributes,
		 * {@linkplain Map#remove(Object) remove} attributes, or use any of the other
		 * {@link Map} methods.
		 *
		 * @param attributesConsumer a function that consumes the attributes map
		 * @return this builder
		 * @since 5.3
		 */
		Builder withAttributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * Builds the {@code RouterFunction}. All created routes are
		 * {@linkplain RouterFunction#and(RouterFunction) composed} with one another, and filters
		 * (if any) are applied to the result.
		 *
		 * @return the built router function
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


	private static class HandlerStrategiesResponseContext implements ServerResponse.Context {

		private final HandlerStrategies strategies;

		public HandlerStrategiesResponseContext(HandlerStrategies strategies) {
			this.strategies = strategies;
		}

		@Override
		public List<HttpMessageWriter<?>> messageWriters() {
			return this.strategies.messageWriters();
		}

		@Override
		public List<ViewResolver> viewResolvers() {
			return this.strategies.viewResolvers();
		}
	}


	private static class RouterFunctionWebHandler implements WebHandler {

		private final HandlerStrategies strategies;

		private final RouterFunction<?> routerFunction;

		public RouterFunctionWebHandler(HandlerStrategies strategies, RouterFunction<?> routerFunction) {
			this.strategies = strategies;
			this.routerFunction = routerFunction;
		}

		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			return Mono.defer(() -> {
				ServerRequest request = new DefaultServerRequest(exchange, this.strategies.messageReaders());
				addAttributes(exchange, request);
				return this.routerFunction.route(request)
						.switchIfEmpty(createNotFoundError())
						.flatMap(handlerFunction -> wrapException(() -> handlerFunction.handle(request)))
						.flatMap(response -> wrapException(() -> response.writeTo(exchange,
								new HandlerStrategiesResponseContext(this.strategies))));
			});
		}

		private void addAttributes(ServerWebExchange exchange, ServerRequest request) {
			Map<String, Object> attributes = exchange.getAttributes();
			attributes.put(REQUEST_ATTRIBUTE, request);
		}

		private <R> Mono<R> createNotFoundError() {
			return Mono.defer(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
					"No matching router function")));
		}

		private static <T> Mono<T> wrapException(Supplier<Mono<T>> supplier) {
			try {
				return supplier.get();
			} catch (Throwable ex) {
				return Mono.error(ex);
			}
		}
	}
}
