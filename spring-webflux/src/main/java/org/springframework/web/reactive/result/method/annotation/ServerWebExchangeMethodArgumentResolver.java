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

package org.springframework.web.reactive.result.method.annotation;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 解析以下类型的与 ServerWebExchange 相关的方法参数值：
 * <ul>
 * <li>{@link ServerWebExchange}
 * <li>{@link ServerHttpRequest}
 * <li>{@link ServerHttpResponse}
 * <li>{@link HttpMethod}
 * <li>{@link Locale}
 * <li>{@link TimeZone}
 * <li>{@link ZoneId}
 * <li>{@link UriBuilder} 或 {@link UriComponentsBuilder} -- 用于构建相对于当前请求的 URL
 * </ul>
 *
 * <p>有关 {@code WebSession} 请参阅 {@link WebSessionMethodArgumentResolver}，
 * 有关 {@code Principal} 请参阅 {@link PrincipalMethodArgumentResolver}。
 *
 * @author Rossen Stoyanchev
 * @see WebSessionMethodArgumentResolver
 * @see PrincipalMethodArgumentResolver
 * @since 5.2
 */
public class ServerWebExchangeMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public ServerWebExchangeMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}

	/**
	 * 检查该参数解析器是否支持给定的参数类型
	 *
	 * @param parameter 方法参数
	 * @return 是否支持给定的参数类型
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return checkParameterTypeNoReactiveWrapper(parameter,
				// 检查参数类型是否符合以下条件之一：
				type ->
						// 是否为 ServerWebExchange 类型
						ServerWebExchange.class.isAssignableFrom(type) ||
								// 是否为 ServerHttpRequest 类型
								ServerHttpRequest.class.isAssignableFrom(type) ||
								// 是否为 ServerHttpResponse 类型
								ServerHttpResponse.class.isAssignableFrom(type) ||
								// 是否为 HttpMethod 类型
								HttpMethod.class == type ||
								// 是否为 Locale 类型
								Locale.class == type ||
								// 是否为 TimeZone 类型
								TimeZone.class == type ||
								// 是否为 ZoneId 类型
								ZoneId.class == type ||
								// 是否为 UriBuilder 类型
								UriBuilder.class == type ||
								// 是否为 UriComponentsBuilder 类型
								UriComponentsBuilder.class == type);
	}


	/**
	 * 解析方法参数值
	 *
	 * @param methodParameter 方法参数
	 * @param context         要使用的绑定上下文
	 * @param exchange        当前交换
	 * @return 解析的值，如果有的话
	 */
	@Override
	public Object resolveArgumentValue(
			MethodParameter methodParameter, BindingContext context, ServerWebExchange exchange) {
		// 获取参数类型
		Class<?> paramType = methodParameter.getParameterType();
		if (ServerWebExchange.class.isAssignableFrom(paramType)) {
			// 如果参数类型是 ServerWebExchange 类型，则返回当前的 exchange 对象
			return exchange;
		} else if (ServerHttpRequest.class.isAssignableFrom(paramType)) {
			// 如果参数类型是 ServerHttpRequest 类型，则返回当前 exchange 对象的请求部分
			return exchange.getRequest();
		} else if (ServerHttpResponse.class.isAssignableFrom(paramType)) {
			// 如果参数类型是 ServerHttpResponse 类型，则返回当前 exchange 对象的响应部分
			return exchange.getResponse();
		} else if (HttpMethod.class == paramType) {
			// 如果参数类型是 HttpMethod 类型，则返回当前 exchange 对象的请求方法
			return exchange.getRequest().getMethod();
		} else if (Locale.class == paramType) {
			// 如果参数类型是 Locale 类型，则返回当前 exchange 对象的区域设置
			return exchange.getLocaleContext().getLocale();
		} else if (TimeZone.class == paramType) {
			// 如果参数类型是 TimeZone 类型，则获取 exchange 对象的区域设置，并获取对应的 TimeZone
			LocaleContext localeContext = exchange.getLocaleContext();
			TimeZone timeZone = getTimeZone(localeContext);
			return (timeZone != null ? timeZone : TimeZone.getDefault());
		} else if (ZoneId.class == paramType) {
			// 如果参数类型是 ZoneId 类型，则获取 exchange 对象的区域设置，并获取对应的 TimeZone，并转换为 ZoneId
			LocaleContext localeContext = exchange.getLocaleContext();
			TimeZone timeZone = getTimeZone(localeContext);
			return (timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault());
		} else if (UriBuilder.class == paramType || UriComponentsBuilder.class == paramType) {
			// 如果参数类型是 UriBuilder 或 UriComponentsBuilder 类型，则构建 URI 并替换路径和查询部分
			URI uri = exchange.getRequest().getURI();
			String contextPath = exchange.getRequest().getPath().contextPath().value();
			return UriComponentsBuilder.fromUri(uri).replacePath(contextPath).replaceQuery(null);
		} else {
			// 不应该发生的情况...
			throw new IllegalArgumentException("Unknown parameter type: " +
					paramType + " in method: " + methodParameter.getMethod());
		}
	}

	/**
	 * 获取时区信息，可能返回空值
	 *
	 * @param localeContext 区域设置上下文
	 * @return 时区信息
	 */
	@Nullable
	private TimeZone getTimeZone(LocaleContext localeContext) {
		TimeZone timeZone = null;
		if (localeContext instanceof TimeZoneAwareLocaleContext) {
			// 如果 localeContext 是 TimeZoneAwareLocaleContext 的实例，则获取对应的时区信息
			timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
		}
		return timeZone; // 返回获取到的时区信息（可能为空）
	}


}
