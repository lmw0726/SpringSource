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

package org.springframework.web.reactive.handler;

import org.apache.commons.logging.Log;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.Ordered;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.*;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 抽象基类，用于实现 {@link org.springframework.web.reactive.HandlerMapping} 的处理器映射。
 * 实现了路径匹配、CORS（跨域资源共享）配置等功能。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractHandlerMapping extends ApplicationObjectSupport implements HandlerMapping, Ordered, BeanNameAware {
	/**
	 * 一个用于空操作的 WebHandler
	 */
	private static final WebHandler NO_OP_HANDLER = exchange -> Mono.empty();

	/**
	 * 专门用于请求映射的“隐藏”日志记录器。
	 */
	protected final Log mappingsLogger = LogDelegateFactory.getHiddenLog(HandlerMapping.class.getName() + ".Mappings");

	/**
	 * 路径模式解析器
	 */
	private final PathPatternParser patternParser;

	/**
	 * 跨域配置源
	 */
	@Nullable
	private CorsConfigurationSource corsConfigurationSource;

	/**
	 * 跨域处理器，默认为 DefaultCorsProcessor
	 */
	private CorsProcessor corsProcessor = new DefaultCorsProcessor();

	/**
	 * 默认值：与非 Ordered 相同
	 * 用于排序的值，默认为最低优先级
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;

	/**
	 * Bean 的名称
	 */
	@Nullable
	private String beanName;


	public AbstractHandlerMapping() {
		this.patternParser = new PathPatternParser();
	}


	/**
	 * 设置是否使用区分大小写的匹配。
	 * <p>默认情况下，与底层模式解析器中的设置相同。
	 * 在使用的基础模式解析器上设置相同属性的快捷方法。
	 * 有关更多详细信息，请参见: <ul> <li >{@link #getPathPatternParser()} -- 基础模式解析器
	 * <li >{@link PathPatternParser#setCaseSensitive(boolean)} -- 区分大小写的斜杠选项，包括其默认值。
	 *
	 * @param caseSensitiveMatch 是否使用区分大小写的匹配
	 **/
	public void setUseCaseSensitiveMatch(boolean caseSensitiveMatch) {
		this.patternParser.setCaseSensitive(caseSensitiveMatch);
	}

	/**
	 * 用于设置正在使用的底层模式解析器相同属性的快捷方法。更多详细信息请参见：
	 * <ul>
	 * <li>{@link #getPathPatternParser()} -- 正在使用的底层模式解析器
	 * <li>{@link PathPatternParser#setMatchOptionalTrailingSeparator(boolean)} --
	 * 包括其默认值在内的可选尾随斜杠选项。
	 * </ul>
	 *
	 * @param trailingSlashMatch 是否使用可选尾随斜杠
	 */
	public void setUseTrailingSlashMatch(boolean trailingSlashMatch) {
		this.patternParser.setMatchOptionalTrailingSeparator(trailingSlashMatch);
	}

	/**
	 * 返回用于 {@link #setCorsConfigurations(Map) 设置 CORS 配置检查} 的 {@link PathPatternParser} 实例。
	 * 子类还可以将此模式解析器用于其自己的请求映射目的。
	 *
	 * @return 路径模式解析器
	 */
	public PathPatternParser getPathPatternParser() {
		return this.patternParser;
	}

	/**
	 * 设置基于 URL 模式的“全局”CORS配置。
	 * 默认情况下，将第一个匹配的 URL 模式与处理程序级别的 CORS 配置（如果有）结合起来。
	 *
	 * @param corsConfigurations 跨域配置
	 * @see #setCorsConfigurationSource(CorsConfigurationSource)
	 */
	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
		Assert.notNull(corsConfigurations, "corsConfigurations must not be null");
		if (!corsConfigurations.isEmpty()) {
			//如果没有指定跨域配置，则使用基础的跨域配置源
			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(this.patternParser);
			source.setCorsConfigurations(corsConfigurations);
			this.corsConfigurationSource = source;
		} else {
			this.corsConfigurationSource = null;
		}
	}

	/**
	 * 设置基于 URL 模式的“全局”CORS配置。
	 * 默认情况下，将第一个匹配的 URL 模式与处理程序级别的 CORS 配置（如果有）结合起来。
	 *
	 * @param corsConfigurationSource 跨域配置源
	 * @see #setCorsConfigurationSource(CorsConfigurationSource)
	 * @since 5.1
	 */
	public void setCorsConfigurationSource(CorsConfigurationSource corsConfigurationSource) {
		Assert.notNull(corsConfigurationSource, "corsConfigurationSource must not be null");
		this.corsConfigurationSource = corsConfigurationSource;
	}

	/**
	 * 配置要用于应用匹配的{@link CorsProcessor}，以应用匹配的{@link CorsConfiguration}。
	 * <p>默认情况下，使用 {@link DefaultCorsProcessor} 的实例。
	 */
	public void setCorsProcessor(CorsProcessor corsProcessor) {
		Assert.notNull(corsProcessor, "CorsProcessor must not be null");
		this.corsProcessor = corsProcessor;
	}

	/**
	 * 返回配置的 {@link CorsProcessor}。
	 */
	public CorsProcessor getCorsProcessor() {
		return this.corsProcessor;
	}

	/**
	 * 为此 HandlerMapping bean 指定的顺序值。
	 * <p>默认值是 {@code Ordered.LOWEST_PRECEDENCE}，表示非排序。
	 *
	 * @param order 顺序值
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	protected String formatMappingName() {
		return this.beanName != null ? "'" + this.beanName + "'" : "<unknown>";
	}


	@Override
	public Mono<Object> getHandler(ServerWebExchange exchange) {
		// 获取处理程序并进行映射操作
		return getHandlerInternal(exchange).map(handler -> {
			// 如果日志级别为调试，则记录请求映射信息
			if (logger.isDebugEnabled()) {
				logger.debug(exchange.getLogPrefix() + "Mapped to " + handler);
			}
			ServerHttpRequest request = exchange.getRequest();
			// 如果处理程序有跨域配置源或者是预检请求
			if (hasCorsConfigurationSource(handler) || CorsUtils.isPreFlightRequest(request)) {
				// 获取跨域配置信息
				CorsConfiguration config = (this.corsConfigurationSource != null ?
						this.corsConfigurationSource.getCorsConfiguration(exchange) : null);
				CorsConfiguration handlerConfig = getCorsConfiguration(handler, exchange);
				// 合并处理程序的跨域配置信息
				config = (config != null ? config.combine(handlerConfig) : handlerConfig);
				// 验证允许凭证的有效性
				if (config != null) {
					config.validateAllowCredentials();
				}
				// 如果跨域处理器处理失败或者是预检请求，返回一个 NO_OP_HANDLER
				if (!this.corsProcessor.process(config, exchange) || CorsUtils.isPreFlightRequest(request)) {
					return NO_OP_HANDLER;
				}
			}
			// 返回处理程序
			return handler;
		});
	}

	/**
	 * 查找给定请求的处理程序，如果没有找到特定的处理程序，
	 * 则返回一个空的 {@code Mono}。此方法由 {@link #getHandler} 调用。
	 * <p>在 CORS 预检请求中，此方法不应返回预检请求的匹配项，
	 * 而应基于 URL 路径、来自 "Access-Control-Request-Method" 标头的 HTTP 方法
	 * 以及来自 "Access-Control-Request-Headers" 标头的标头，返回预期的实际请求。
	 *
	 * @param exchange 当前交换
	 * @return 如果有匹配的处理程序，则为 {@code Mono}
	 */
	protected abstract Mono<?> getHandlerInternal(ServerWebExchange exchange);


	/**
	 * 如果此处理程序存在 {@link CorsConfigurationSource}，则返回 {@code true}。
	 *
	 * @param handler 处理程序对象
	 * @return 如果存在 {@link CorsConfigurationSource}，则为 {@code true}
	 * @since 5.2
	 */
	protected boolean hasCorsConfigurationSource(Object handler) {
		return (handler instanceof CorsConfigurationSource || this.corsConfigurationSource != null);
	}

	/**
	 * 获取给定处理程序的 CORS 配置。
	 *
	 * @param handler 用于检查的处理程序（绝不为 {@code null}）
	 * @param exchange 当前交换
	 * @return 处理程序的 CORS 配置；如果不存在，则为 {@code null}
	 */
	@Nullable
	protected CorsConfiguration getCorsConfiguration(Object handler, ServerWebExchange exchange) {
		if (handler instanceof CorsConfigurationSource) {
			return ((CorsConfigurationSource) handler).getCorsConfiguration(exchange);
		}
		return null;
	}

}
