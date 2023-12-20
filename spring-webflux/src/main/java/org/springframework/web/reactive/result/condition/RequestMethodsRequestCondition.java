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

package org.springframework.web.reactive.result.condition;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.*;

/**
 * 一个逻辑或 (' || ') 请求条件，用于匹配一个请求与一组 {@link RequestMethod RequestMethods}。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class RequestMethodsRequestCondition extends AbstractRequestCondition<RequestMethodsRequestCondition> {

	/**
	 * 按照 HTTP 方法缓存，用于从 getMatchingCondition 方法中返回准备好的实例。
	 */
	private static final Map<HttpMethod, RequestMethodsRequestCondition> requestMethodConditionCache;


	// 静态块，用于初始化请求方法条件缓存
	static {
		// 创建一个哈希映射来存储请求方法条件
		requestMethodConditionCache = CollectionUtils.newHashMap(RequestMethod.values().length);

		// 遍历所有的请求方法
		for (RequestMethod method : RequestMethod.values()) {
			// 将请求方法与对应的请求方法请求条件放入缓存中
			requestMethodConditionCache.put(
					HttpMethod.valueOf(method.name()), new RequestMethodsRequestCondition(method));
		}
	}


	/**
	 * 请求方法集合
	 */
	private final Set<RequestMethod> methods;


	/**
	 * 使用给定的请求方法创建一个新实例。
	 *
	 * @param requestMethods 0个或更多个 HTTP 请求方法；如果为0，该条件将匹配所有请求
	 */
	public RequestMethodsRequestCondition(RequestMethod... requestMethods) {
		this.methods = (ObjectUtils.isEmpty(requestMethods) ?
				Collections.emptySet() : new LinkedHashSet<>(Arrays.asList(requestMethods)));
	}

	/**
	 * 用于在合并条件时内部使用的私有构造函数。
	 */
	private RequestMethodsRequestCondition(Set<RequestMethod> requestMethods) {
		this.methods = requestMethods;
	}

	/**
	 * 返回此条件中包含的所有 {@link RequestMethod RequestMethods}。
	 */
	public Set<RequestMethod> getMethods() {
		return this.methods;
	}

	@Override
	protected Collection<RequestMethod> getContent() {
		return this.methods;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 返回一个新实例，其中包含从“this”和“other”实例中的 HTTP 请求方法的并集。
	 */
	@Override
	public RequestMethodsRequestCondition combine(RequestMethodsRequestCondition other) {
		// 如果“this”和“other”都为空
		if (isEmpty() && other.isEmpty()) {
			return this;
		} else if (other.isEmpty()) {
			return this;
		} else if (isEmpty()) {
			return other;
		}

		// 创建一个新的 LinkedHashSet，包含“this”实例中的方法
		Set<RequestMethod> set = new LinkedHashSet<>(this.methods);
		// 将“other”实例中的方法添加到新集合中
		set.addAll(other.methods);

		// 返回一个新的 RequestMethodsRequestCondition 实例，包含合并后的方法集合
		return new RequestMethodsRequestCondition(set);
	}

	/**
	 * 检查任何 HTTP 请求方法是否与给定请求匹配，并返回仅包含匹配 HTTP 请求方法的实例。
	 *
	 * @param exchange 当前交换对象
	 * @return 如果条件为空（除非请求方法是 HTTP OPTIONS），则返回相同实例；
	 * 如果是匹配的请求方法，则返回包含匹配请求方法的新条件实例；
	 * 如果没有匹配或条件为空且请求方法是 OPTIONS，则返回{@code null}
	 */
	@Override
	@Nullable
	public RequestMethodsRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		// 如果是预检请求
		if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
			// 匹配预检请求中的方法
			return matchPreFlight(exchange.getRequest());
		}

		// 如果条件中的方法为空
		if (getMethods().isEmpty()) {
			// 如果请求方法是 OPTIONS，则返回null，否则返回当前实例
			if (RequestMethod.OPTIONS.name().equals(exchange.getRequest().getMethodValue())) {
				// 我们透明地处理 OPTIONS，如果没有明确声明，则不进行匹配
				return null;
			}
			return this;
		}

		// 匹配请求中的方法
		return matchRequestMethod(exchange.getRequest().getMethod());
	}


	/**
	 * 在预检请求（pre-flight request）中匹配即将发生的实际请求。
	 * 因此，空条件是一个匹配，否则尝试匹配“Access-Control-Request-Method”头中的HTTP方法。
	 *
	 * @param request 服务器 HTTP 请求
	 * @return 如果条件中的方法为空，则返回当前实例；
	 * 否则尝试匹配“Access-Control-Request-Method”头中的HTTP方法，
	 * 匹配成功返回对应的 RequestMethodsRequestCondition，否则返回{@code null}
	 */
	@Nullable
	private RequestMethodsRequestCondition matchPreFlight(ServerHttpRequest request) {
		// 如果条件中的方法为空
		if (getMethods().isEmpty()) {
			// 返回当前实例
			return this;
		}

		// 获取预检请求中的期望 HTTP 方法
		HttpMethod expectedMethod = request.getHeaders().getAccessControlRequestMethod();

		// 如果期望的方法不为null，尝试匹配对应的 RequestMethodsRequestCondition
		if (expectedMethod == null) {
			return null;
		}
		return matchRequestMethod(expectedMethod);
	}

	/**
	 * 匹配 HTTP 请求方法。
	 *
	 * @param httpMethod HTTP 请求方法
	 * @return 如果匹配成功，返回对应的 RequestMethodsRequestCondition；否则返回{@code null}
	 */
	@Nullable
	private RequestMethodsRequestCondition matchRequestMethod(@Nullable HttpMethod httpMethod) {
		// 如果 HTTP 请求方法为null，返回null
		if (httpMethod == null) {
			return null;
		}

		// 转换 HTTP 请求方法为 RequestMethod
		RequestMethod requestMethod = RequestMethod.valueOf(httpMethod.name());

		// 如果条件中包含该请求方法
		if (getMethods().contains(requestMethod)) {
			// 返回对应的 RequestMethodsRequestCondition
			return requestMethodConditionCache.get(httpMethod);
		}

		// 如果请求方法为 HEAD 且条件中包含 GET 请求方法
		if (requestMethod.equals(RequestMethod.HEAD) && getMethods().contains(RequestMethod.GET)) {
			// 返回对应 GET 请求方法的 RequestMethodsRequestCondition
			return requestMethodConditionCache.get(HttpMethod.GET);
		}

		// 匹配失败，返回null
		return null;
	}

	/**
	 * 返回：
	 * <ul>
	 * <li>如果两个条件包含相同数量的HTTP请求方法，则返回0
	 * <li>如果“this”实例有一个HTTP请求方法而“other”没有，则返回小于0
	 * <li>如果“other”有一个HTTP请求方法而“this”没有，则返回大于0
	 * </ul>
	 * <p>假设两个实例都是通过{@link #getMatchingCondition(ServerWebExchange)}获取的，
	 * 因此每个实例仅包含匹配的HTTP请求方法或为空。
	 */
	@Override
	public int compareTo(RequestMethodsRequestCondition other, ServerWebExchange exchange) {
		// 如果两个实例包含的HTTP请求方法数量不同
		if (other.methods.size() != this.methods.size()) {
			return other.methods.size() - this.methods.size();
		}
		// 如果两个实例都包含一个HTTP请求方法
		else if (this.methods.size() == 1) {
			// 如果“this”实例包含HEAD而“other”实例包含GET
			if (this.methods.contains(RequestMethod.HEAD) && other.methods.contains(RequestMethod.GET)) {
				return -1;
			}
			// 如果“this”实例包含GET而“other”实例包含HEAD
			else if (this.methods.contains(RequestMethod.GET) && other.methods.contains(RequestMethod.HEAD)) {
				return 1;
			}
		}

		// 默认情况返回0，表示相同数量的HTTP请求方法
		return 0;
	}

}
