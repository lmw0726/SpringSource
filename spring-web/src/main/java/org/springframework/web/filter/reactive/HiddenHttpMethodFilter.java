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

package org.springframework.web.filter.reactive;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 响应式的 {@link WebFilter}，将发布的方法参数转换为 HTTP 方法，可通过 {@link ServerHttpRequest#getMethod()} 检索。
 * 由于浏览器当前仅支持 GET 和 POST，一种常见的技术是使用带有附加隐藏表单字段（{@code _method}）的普通 POST 来传递“真正的”HTTP 方法。
 * 此过滤器读取该参数，并使用 {@link ServerWebExchange#mutate()} 更改 {@link ServerHttpRequest#getMethod()} 返回值。
 *
 * <p>请求参数的名称默认为 {@code _method}，但可以通过 {@link #setMethodParamName(String) methodParamName} 属性进行调整。
 *
 * @author Greg Turnquist
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HiddenHttpMethodFilter implements WebFilter {
	/**
	 * 允许的方法列表
	 */
	private static final List<HttpMethod> ALLOWED_METHODS =
			Collections.unmodifiableList(Arrays.asList(HttpMethod.PUT,
					HttpMethod.DELETE, HttpMethod.PATCH));

	/**
	 * 默认的表单参数名称，用于指定要使用的 HTTP 方法。
	 */
	public static final String DEFAULT_METHOD_PARAMETER_NAME = "_method";

	/**
	 * 方法参数名称
	 */
	private String methodParamName = DEFAULT_METHOD_PARAMETER_NAME;


	/**
	 * 设置要使用的 HTTP 方法的表单参数的名称。
	 * <p>默认情况下，此设置为 {@code "_method"}。
	 */
	public void setMethodParamName(String methodParamName) {
		Assert.hasText(methodParamName, "'methodParamName' must not be empty");
		this.methodParamName = methodParamName;
	}


	/**
	 * 根据 {@code methodParamName} 将 HTTP POST 转换为另一种方法。
	 *
	 * @param exchange 当前的服务器交换
	 * @param chain    提供一种委派给下一个过滤器的方式
	 * @return {@code Mono<Void>}，指示请求处理何时完成
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

		if (exchange.getRequest().getMethod() != HttpMethod.POST) {
			// 如果请求方法不是POST，则直接传递给下一个过滤器链处理
			return chain.filter(exchange);
		}

		// 对POST请求，从表单数据中获取指定参数值
		return exchange.getFormData()
				.map(formData -> {
					// 从表单数据中获取指定参数的值
					String method = formData.getFirst(this.methodParamName);
					// 如果参数值不为空，则根据该参数值对交换对象进行映射，否则不进行映射
					return StringUtils.hasLength(method) ? mapExchange(exchange, method) : exchange;
				})
				.flatMap(chain::filter);
	}

	private ServerWebExchange mapExchange(ServerWebExchange exchange, String methodParamValue) {
		// 解析Http方法
		HttpMethod httpMethod = HttpMethod.resolve(methodParamValue.toUpperCase(Locale.ENGLISH));
		Assert.notNull(httpMethod, () -> "HttpMethod '" + methodParamValue + "' not supported");

		// 如果解析出的 Http方法 在允许的方法列表中，则将请求方法设置为解析出的方法
		if (ALLOWED_METHODS.contains(httpMethod)) {
			return exchange.mutate().request(builder -> builder.method(httpMethod)).build();
		} else {
			// 否则不做修改
			return exchange;
		}
	}

}
