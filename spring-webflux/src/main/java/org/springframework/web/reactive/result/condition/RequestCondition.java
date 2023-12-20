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

package org.springframework.web.reactive.result.condition;

import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

/**
 * 请求映射条件的契约。
 *
 * <p>请求条件可以通过 {@link #combine(Object)} 进行组合，通过 {@link #getMatchingCondition(ServerWebExchange)} 匹配到
 * 请求，并通过 {@link #compareTo(Object, ServerWebExchange)} 进行比较，以确定哪个条件更适合给定的请求。
 *
 * @param <T> 可以与此 RequestCondition 组合和比较的对象类型
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface RequestCondition<T> {

	/**
	 * 将此条件与另一个条件进行组合，例如从类型级别和方法级别的 {@code @RequestMapping} 注解中获取的条件。
	 *
	 * @param other 要与之组合的条件。
	 * @return 一个请求条件实例，是将两个条件实例组合而成的结果。
	 */
	T combine(T other);

	/**
	 * 检查条件是否与请求匹配，返回一个可能为当前请求创建的新实例。例如，具有多个 URL 模式的条件可能只返回具有与请求匹配的那些模式的新实例。
	 * <p>对于CORS预检请求，条件应该匹配到实际请求（例如，来自 "Access-Control-Request-Method" 头的 URL 模式、查询参数和 HTTP 方法）。
	 * 如果条件无法与预检请求匹配，它应该返回一个具有空内容的实例，从而不会导致匹配失败。
	 *
	 * @return 如果匹配则为条件实例，否则为 {@code null}。
	 */
	@Nullable
	T getMatchingCondition(ServerWebExchange exchange);

	/**
	 * 在特定请求的上下文中比较此条件与另一个条件。此方法假定这两个实例都是通过 {@link #getMatchingCondition(ServerWebExchange)}
	 * 获取的，以确保它们只包含与当前请求相关的内容。
	 */
	int compareTo(T other, ServerWebExchange exchange);

}
