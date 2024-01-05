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

package org.springframework.web.reactive.function.server;

import java.util.Optional;

/**
 * 表示在给定的{@link ServerRequest}上评估的函数。
 * 此函数的实例用于在常见的请求属性上进行评估，可以在{@link RequestPredicates}中找到。
 *
 * @author Arjen Poutsma
 * @see RequestPredicates
 * @see RouterFunctions#route(RequestPredicate, HandlerFunction)
 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
 * @since 5.0
 */
@FunctionalInterface
public interface RequestPredicate {

	/**
	 * 在给定的请求上评估此断言。
	 *
	 * @param request 要匹配的请求
	 * @return 如果请求与断言匹配，则返回{@code true}；否则返回{@code false}
	 */
	boolean test(ServerRequest request);

	/**
	 * 返回一个组合的请求断言，同时对此断言和{@code other}断言进行测试。
	 * 在评估组合断言时，如果此断言为{@code false}，则不对{@code other}断言进行评估。
	 *
	 * @param other 与此断言逻辑AND的断言
	 * @return 由此断言和{@code other}断言组合的断言
	 */
	default RequestPredicate and(RequestPredicate other) {
		return new RequestPredicates.AndRequestPredicate(this, other);
	}

	/**
	 * 返回表示此断言的逻辑否定的断言。
	 *
	 * @return 表示此断言的逻辑否定的断言
	 */
	default RequestPredicate negate() {
		return new RequestPredicates.NegateRequestPredicate(this);
	}

	/**
	 * 返回一个组合的请求断言，同时对此断言和{@code other}断言进行测试。
	 * 在评估组合断言时，如果此断言为{@code true}，则不对{@code other}断言进行评估。
	 *
	 * @param other 与此断言逻辑OR的断言
	 * @return 由此断言和{@code other}断言组合的断言
	 */
	default RequestPredicate or(RequestPredicate other) {
		return new RequestPredicates.OrRequestPredicate(this, other);
	}

	/**
	 * 将给定的请求转换为用于嵌套路由的请求。例如，基于路径的断言可以返回一个匹配后剩余路径的{@code ServerRequest}。
	 * <p>默认实现返回一个{@code Optional}，如果{@link #test(ServerRequest)}评估为{@code true}，则包装给定的请求；
	 * 如果评估为{@code false}，则返回{@link Optional#empty()}。
	 *
	 * @param request 要嵌套的请求
	 * @return 嵌套的请求
	 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
	 */
	default Optional<ServerRequest> nest(ServerRequest request) {
		return (test(request) ? Optional.of(request) : Optional.empty());
	}

	/**
	 * 接受给定的访问者。默认实现调用{@link RequestPredicates.Visitor#unknown(RequestPredicate)}；
	 * 预期组成此请求断言的组合{@code RequestPredicate}实现将对构成此请求断言的所有组件调用{@code accept}。
	 *
	 * @param visitor 要接受的访问者
	 */
	default void accept(RequestPredicates.Visitor visitor) {
		visitor.unknown(this);
	}

}
