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

import java.util.Optional;

/**
 * 表示在给定的{@link ServerRequest}上进行评估的函数。
 * 在{@link RequestPredicates}中可以找到在常见请求属性上进行评估的此函数的实例。
 *
 * @author Arjen Poutsma
 * @see RequestPredicates
 * @see RouterFunctions#route(RequestPredicate, HandlerFunction)
 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
 * @since 5.2
 */
@FunctionalInterface
public interface RequestPredicate {

	/**
	 * 在给定的请求上评估此谓词。
	 *
	 * @param request 要匹配的请求
	 * @return 如果请求与谓词匹配，则为{@code true}；否则为{@code false}
	 */
	boolean test(ServerRequest request);

	/**
	 * 返回一个组合的请求谓词，它对此谓词和{@code other}谓词进行测试。
	 * 在评估组合谓词时，如果此谓词为{@code false}，则不会评估{@code other}谓词。
	 *
	 * @param other 将与此谓词逻辑AND的谓词
	 * @return 由此谓词和{@code other}谓词组成的谓词
	 */
	default RequestPredicate and(RequestPredicate other) {
		return new RequestPredicates.AndRequestPredicate(this, other);
	}

	/**
	 * 返回表示此谓词的逻辑否定的谓词。
	 *
	 * @return 表示此谓词的逻辑否定的谓词
	 */
	default RequestPredicate negate() {
		return new RequestPredicates.NegateRequestPredicate(this);
	}

	/**
	 * 返回一个组合的请求谓词，它对此谓词和{@code other}谓词进行测试。
	 * 在评估组合谓词时，如果此谓词为{@code true}，则不会评估{@code other}谓词。
	 *
	 * @param other 将与此谓词逻辑OR的谓词
	 * @return 由此谓词和{@code other}谓词组成的谓词
	 */
	default RequestPredicate or(RequestPredicate other) {
		return new RequestPredicates.OrRequestPredicate(this, other);
	}

	/**
	 * 将给定的请求转换为用于嵌套路由的请求。例如，
	 * 基于路径的谓词可以返回一个匹配后剩余路径的{@code ServerRequest}。
	 * <p>默认实现如果{@link #test(ServerRequest)}评估为{@code true}，则返回一个包装了给定请求的{@code Optional}；
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
	 * 组合的{@code RequestPredicate}实现应该对构成此请求谓词的所有组件调用{@code accept}。
	 *
	 * @param visitor 要接受的访问者
	 */
	default void accept(RequestPredicates.Visitor visitor) {
		visitor.unknown(this);
	}
}
