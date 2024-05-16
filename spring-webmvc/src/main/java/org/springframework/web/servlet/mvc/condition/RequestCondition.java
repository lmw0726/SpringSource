/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

/**
 * 请求映射条件的合同。
 *
 * <p>请求条件可以通过 {@link #combine(Object)} 进行组合，通过 {@link #getMatchingCondition(HttpServletRequest)}
 * 匹配到请求，并通过 {@link #compareTo(Object, HttpServletRequest)} 彼此比较，以确定哪个条件更符合给定请求。
 *
 * @作者 Rossen Stoyanchev
 * @作者 Arjen Poutsma
 * @自 3.1 以来
 * @param <T> 此 RequestCondition 可以与之组合和比较的对象类型
 */
public interface RequestCondition<T> {

	/**
	 * 将此条件与另一个条件组合，例如来自类级别和方法级别的 {@code @RequestMapping} 注解的条件。
	 * @param other 要组合的条件。
	 * @return 一个请求条件实例，它是组合两个条件实例的结果。
	 */
	T combine(T other);

	/**
	 * 检查条件是否匹配请求，返回一个可能为当前请求创建的新实例。例如，具有多个 URL 模式的条件
	 * 可能仅返回与请求匹配的那些模式的新实例。
	 * <p>对于 CORS 预检请求，条件应匹配将要进行的实际请求（例如，URL 模式、查询参数和
	 * "Access-Control-Request-Method" 标头中的 HTTP 方法）。如果条件无法与预检请求匹配，
	 * 则应返回内容为空的实例，从而不会导致匹配失败。
	 * @param request 当前的 HTTP 请求
	 * @return 匹配情况下的条件实例，否则为 {@code null}。
	 */
	@Nullable
	T getMatchingCondition(HttpServletRequest request);

	/**
	 * 在特定请求的上下文中将此条件与另一个条件进行比较。此方法假定两个实例都是通过
	 * {@link #getMatchingCondition(HttpServletRequest)} 获得的，以确保它们仅包含与当前请求相关的内容。
	 * @param other 要比较的另一个条件
	 * @param request 当前的 HTTP 请求
	 * @return 比较结果。正数表示此条件优于其他条件，负数表示其他条件优于此条件，零表示两者相等。
	 */
	int compareTo(T other, HttpServletRequest request);

}
