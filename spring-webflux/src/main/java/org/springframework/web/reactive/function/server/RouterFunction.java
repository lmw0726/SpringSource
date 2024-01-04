/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 表示将路由到{@linkplain HandlerFunction 处理函数}的函数。
 * 作者是 Arjen Poutsma。
 * 自版本5.0开始使用。
 *
 * @param <T> 要路由到的{@linkplain HandlerFunction 处理函数}的类型
 * @see RouterFunctions
 */
@FunctionalInterface
public interface RouterFunction<T extends ServerResponse> {

	/**
	 * 返回与给定请求匹配的{@linkplain HandlerFunction 处理函数}。
	 *
	 * @param request 要路由的请求
	 * @return 一个{@code Mono}描述匹配此请求的{@code HandlerFunction}，
	 * 如果没有匹配则返回空的{@code Mono}
	 */
	Mono<HandlerFunction<T>> route(ServerRequest request);

	/**
	 * 返回一个组合的路由函数，首先调用此函数，然后调用{@code other}函数
	 * （相同响应类型{@code T}的函数），如果此路由没有结果{@linkplain Mono#empty()}。
	 *
	 * @param other 当此函数没有结果时应用的类型为{@code T}的函数
	 * @return 一个组合的函数，首先使用此函数路由，然后使用
	 * {@code other}函数，如果此函数没有结果
	 * @see #andOther(RouterFunction)
	 */
	default RouterFunction<T> and(RouterFunction<T> other) {
		return new RouterFunctions.SameComposedRouterFunction<>(this, other);
	}

	/**
	 * 返回一个组合的路由函数，首先调用此函数，然后调用{@code other}函数
	 * （具有不同响应类型）如果此路由没有结果{@linkplain Mono#empty()}。
	 *
	 * @param other 当此函数没有结果时应用的函数
	 * @return 一个组合的函数，首先使用此函数路由，然后使用
	 * {@code other}函数，如果此函数没有结果
	 * @see #and(RouterFunction)
	 */
	default RouterFunction<?> andOther(RouterFunction<?> other) {
		return new RouterFunctions.DifferentComposedRouterFunction(this, other);
	}

	/**
	 * 返回一个组合的路由函数，如果此路由不匹配并且给定的请求谓词适用，则路由到给定的处理函数。
	 * 此方法是{@link #and(RouterFunction)}和{@link RouterFunctions#route(RequestPredicate, HandlerFunction)}
	 * 的便利组合。
	 *
	 * @param predicate       测试此路由是否不匹配的谓词
	 * @param handlerFunction 如果此路由不匹配且谓词适用，则路由到的处理函数
	 * @return 一个组合的函数，如果此路由不匹配并且{@code predicate}适用，则路由到{@code handlerFunction}
	 */
	default RouterFunction<T> andRoute(RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
		return and(RouterFunctions.route(predicate, handlerFunction));
	}

	/**
	 * 返回一个组合的路由函数，如果此路由不匹配并且给定的请求谓词适用，则路由到给定的路由函数。
	 * 此方法是{@link #and(RouterFunction)}和{@link RouterFunctions#nest(RequestPredicate, RouterFunction)}
	 * 的便利组合。
	 *
	 * @param predicate      测试此路由是否不匹配的谓词
	 * @param routerFunction 如果此路由不匹配且谓词适用，则路由到的路由函数
	 * @return 一个组合的函数，如果此路由不匹配并且{@code predicate}适用，则路由到{@code routerFunction}
	 */
	default RouterFunction<T> andNest(RequestPredicate predicate, RouterFunction<T> routerFunction) {
		return and(RouterFunctions.nest(predicate, routerFunction));
	}

	/**
	 * 使用给定的{@linkplain HandlerFilterFunction 过滤函数}过滤此函数路由的所有{@linkplain HandlerFunction 处理函数}。
	 *
	 * @param <S>            过滤器返回类型
	 * @param filterFunction 要应用的过滤器
	 * @return 经过过滤的路由函数
	 */
	default <S extends ServerResponse> RouterFunction<S> filter(HandlerFilterFunction<T, S> filterFunction) {
		return new RouterFunctions.FilteredRouterFunction<>(this, filterFunction);
	}

	/**
	 * 接受给定的访问者。默认实现调用{@link RouterFunctions.Visitor#unknown(RouterFunction)}；
	 * 组合的{@code RouterFunction}实现应对构成此路由函数的所有组件调用{@code accept}。
	 *
	 * @param visitor 要接受的访问者
	 */
	default void accept(RouterFunctions.Visitor visitor) {
		visitor.unknown(this);
	}

	/**
	 * 使用给定的属性返回一个新的路由函数。
	 *
	 * @param name  属性名称
	 * @param value 属性值
	 * @return 具有指定属性的函数
	 * @since 5.3
	 */
	default RouterFunction<T> withAttribute(String name, Object value) {
		Assert.hasLength(name, "Name must not be empty");
		Assert.notNull(value, "Value must not be null");

		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put(name, value);
		return new RouterFunctions.AttributesRouterFunction<>(this, attributes);
	}

	/**
	 * 使用给定的消费者操作属性并返回新的路由函数。
	 * <p>提供给消费者的映射是“活动的”，因此消费者可以用于{@linkplain Map#put(Object, Object) 覆盖}现有属性，
	 * {@linkplain Map#remove(Object) 删除}属性，或使用任何其他{@link Map}方法。
	 *
	 * @param attributesConsumer 消费属性映射的函数
	 * @return 具有操作属性后的新路由函数
	 * @since 5.3
	 */
	default RouterFunction<T> withAttributes(Consumer<Map<String, Object>> attributesConsumer) {
		Assert.notNull(attributesConsumer, "AttributesConsumer must not be null");

		Map<String, Object> attributes = new LinkedHashMap<>();
		attributesConsumer.accept(attributes);
		return new RouterFunctions.AttributesRouterFunction<>(this, attributes);
	}


}
