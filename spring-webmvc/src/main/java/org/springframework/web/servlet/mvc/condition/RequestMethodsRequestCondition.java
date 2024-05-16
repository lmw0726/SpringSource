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

package org.springframework.web.servlet.mvc.condition;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsUtils;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 一个逻辑或 (' || ') 请求条件，用于匹配一组 {@link RequestMethod RequestMethods} 的请求。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class RequestMethodsRequestCondition extends AbstractRequestCondition<RequestMethodsRequestCondition> {

	/**
	 * 按HTTP方法进行缓存，从 getMatchingCondition 返回准备好的实例。
	 */
	private static final Map<String, RequestMethodsRequestCondition> requestMethodConditionCache;

	static {
		// 创建一个新的哈希映射用于缓存请求方法条件
		requestMethodConditionCache = CollectionUtils.newHashMap(RequestMethod.values().length);
		// 遍历所有请求方法枚举值
		for (RequestMethod method : RequestMethod.values()) {
			// 将每个请求方法及其对应的请求方法条件添加到缓存中
			requestMethodConditionCache.put(method.name(), new RequestMethodsRequestCondition(method));
		}
	}

	/**
	 * 请求方法集合
	 */
	private final Set<RequestMethod> methods;


	/**
	 * 创建一个具有给定请求方法的新实例。
	 *
	 * @param requestMethods 0 或多个 HTTP 请求方法；如果为 0，则该条件将匹配每个请求
	 */
	public RequestMethodsRequestCondition(RequestMethod... requestMethods) {
		this.methods = (ObjectUtils.isEmpty(requestMethods) ?
				Collections.emptySet() : new LinkedHashSet<>(Arrays.asList(requestMethods)));
	}

	/**
	 * 内部使用的私有构造函数，用于组合条件。
	 */
	private RequestMethodsRequestCondition(Set<RequestMethod> methods) {
		this.methods = methods;
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
	 * 返回一个具有来自 "this" 和 "other" 实例的 HTTP 请求方法的联合的新实例。
	 */
	@Override
	public RequestMethodsRequestCondition combine(RequestMethodsRequestCondition other) {
		// 如果当前对象和另一个对象都为空
		if (isEmpty() && other.isEmpty()) {
			// 返回当前对象
			return this;
		} else if (other.isEmpty()) {
			// 如果另一个对象为空，则返回当前对象
			return this;
		} else if (isEmpty()) {
			// 如果当前对象为空，则返回另一个对象
			return other;
		}
		// 否则，合并两个对象的请求方法集合
		Set<RequestMethod> set = new LinkedHashSet<>(this.methods);
		set.addAll(other.methods);
		// 创建一个新的 RequestMethodsRequestCondition 对象
		return new RequestMethodsRequestCondition(set);
	}

	/**
	 * 检查任何 HTTP 请求方法是否与给定请求匹配，并返回一个仅包含匹配的 HTTP 请求方法的实例。
	 *
	 * @param request 当前请求
	 * @return 如果条件为空（除非请求方法是 HTTP OPTIONS），则返回相同的实例，如果条件为空且请求方法是 OPTIONS，则返回 null，否则返回匹配的请求方法的新条件实例
	 */
	@Override
	@Nullable
	public RequestMethodsRequestCondition getMatchingCondition(HttpServletRequest request) {
		// 如果是预检请求
		if (CorsUtils.isPreFlightRequest(request)) {
			// 匹配预检请求
			return matchPreFlight(request);
		}

		// 如果请求的方法集合为空
		if (getMethods().isEmpty()) {
			// 如果请求方法是 OPTIONS，并且不是由错误调度器引发的
			if (RequestMethod.OPTIONS.name().equals(request.getMethod()) &&
					!DispatcherType.ERROR.equals(request.getDispatcherType())) {
				// 我们透明地处理 OPTIONS，因此如果没有显式声明，则不匹配
				return null;
			}
			// 返回当前对象
			return this;
		}

		// 否则，匹配请求方法
		return matchRequestMethod(request.getMethod());
	}

	/**
	 * 在预检请求上匹配将要的实际请求。因此，空条件是匹配的，否则尝试匹配 "Access-Control-Request-Method" 头中的 HTTP 方法。
	 */
	@Nullable
	private RequestMethodsRequestCondition matchPreFlight(HttpServletRequest request) {
		// 如果请求方法集合为空
		if (getMethods().isEmpty()) {
			// 返回当前对象
			return this;
		}
		// 从请求头中获取预期的请求方法
		String expectedMethod = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
		// 匹配预期的请求方法
		return matchRequestMethod(expectedMethod);
	}

	@Nullable
	private RequestMethodsRequestCondition matchRequestMethod(String httpMethodValue) {
		// 定义请求方法变量
		RequestMethod requestMethod;
		try {
			// 尝试将字符串转换为 RequestMethod 枚举值
			requestMethod = RequestMethod.valueOf(httpMethodValue);
			// 如果请求方法包含在当前对象的请求方法集合中
			if (getMethods().contains(requestMethod)) {
				// 从缓存中获取请求方法条件并返回
				return requestMethodConditionCache.get(httpMethodValue);
			}
			// 如果请求方法是 HEAD，并且当前对象的请求方法集合包含 GET
			if (requestMethod.equals(RequestMethod.HEAD) && getMethods().contains(RequestMethod.GET)) {
				// 从缓存中获取 GET 请求方法条件并返回
				return requestMethodConditionCache.get(HttpMethod.GET.name());
			}
		} catch (IllegalArgumentException ex) {
			/// 自定义请求方法
		}
		// 返回空，表示未匹配到请求方法条件
		return null;
	}

	/**
	 * 返回：
	 * <ul>
	 * <li>如果两个条件包含相同数量的 HTTP 请求方法，则返回 0
	 * <li>如果 "this" 实例有 HTTP 请求方法但 "other" 没有，则返回小于 0
	 * <li>如果 "other" 有 HTTP 请求方法但 "this" 没有，则返回大于 0
	 * </ul>
	 * <p>假设两个实例都是通过 {@link #getMatchingCondition(HttpServletRequest)} 获取的，
	 * 因此每个实例仅包含匹配的 HTTP 请求方法或以其他方式为空。
	 */
	@Override
	public int compareTo(RequestMethodsRequestCondition other, HttpServletRequest request) {
		// 如果另一个对象的请求方法数量不等于当前对象的请求方法数量
		if (other.methods.size() != this.methods.size()) {
			// 返回请求方法数量的差值
			return other.methods.size() - this.methods.size();
		} else if (this.methods.size() == 1) {
			// 如果请求方法数量都为 1
			// 如果当前对象的请求方法包含 HEAD 并且另一个对象的请求方法包含 GET
			if (this.methods.contains(RequestMethod.HEAD) && other.methods.contains(RequestMethod.GET)) {
				// 返回 -1，表示当前对象的优先级较低
				return -1;
			} else if (this.methods.contains(RequestMethod.GET) && other.methods.contains(RequestMethod.HEAD)) {
				// 如果当前对象的请求方法包含 GET 并且另一个对象的请求方法包含 HEAD
				// 返回 1，表示当前对象的优先级较高
				return 1;
			}
		}
		// 否则，返回 0，表示优先级相同
		return 0;
	}

}
