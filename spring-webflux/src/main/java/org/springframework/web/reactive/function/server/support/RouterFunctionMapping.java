/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.function.server.support;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支持{@link RouterFunction RouterFunctions}的{@code HandlerMapping}实现。
 *
 * <p>如果在{@linkplain #RouterFunctionMapping(RouterFunction) 构造时}未提供{@link RouterFunction}，
 * 则该映射将检测应用程序上下文中的所有路由函数，并按{@linkplain org.springframework.core.annotation.Order 顺序}进行查询。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class RouterFunctionMapping extends AbstractHandlerMapping implements InitializingBean {

	/**
	 * 路由函数对象
	 */
	@Nullable
	private RouterFunction<?> routerFunction;

	/**
	 * HTTP消息读取器列表
	 */
	private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();


	/**
	 * 创建一个空的{@code RouterFunctionMapping}。
	 * <p>如果使用此构造函数，该映射将检测应用程序上下文中的所有{@link RouterFunction}实例。
	 */
	public RouterFunctionMapping() {
	}

	/**
	 * 创建一个具有给定{@link RouterFunction}的{@code RouterFunctionMapping}。
	 * <p>如果使用此构造函数，将不会发生应用程序上下文的检测。
	 *
	 * @param routerFunction 要用于映射的路由函数
	 */
	public RouterFunctionMapping(RouterFunction<?> routerFunction) {
		this.routerFunction = routerFunction;
	}


	/**
	 * 返回配置的{@link RouterFunction}。
	 * <p><strong>注意：</strong>当从ApplicationContext检测到路由函数时，在调用{@link #afterPropertiesSet()}之前，
	 * 此方法可能返回{@code null}。
	 *
	 * @return 路由函数或{@code null}
	 */
	@Nullable
	public RouterFunction<?> getRouterFunction() {
		return this.routerFunction;
	}

	/**
	 * 配置用于反序列化请求体的HTTP消息读取器。
	 * <p>默认情况下，它设置为{@link ServerCodecConfigurer}的默认值。
	 */
	public void setMessageReaders(List<HttpMessageReader<?>> messageReaders) {
		this.messageReaders = messageReaders;
	}

	/**
	 * 在设置了所有属性后，初始化RouterFunctionMapping实例。
	 *
	 * @throws Exception 可能抛出异常，包括在初始化RouterFunction时出现的异常
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		// 如果消息读取器为空，则使用ServerCodecConfigurer创建默认的消息读取器
		if (CollectionUtils.isEmpty(this.messageReaders)) {
			ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();
			this.messageReaders = codecConfigurer.getReaders();
		}

		// 如果RouterFunction为空，则初始化RouterFunctions
		if (this.routerFunction == null) {
			initRouterFunctions();
		}
		// 如果RouterFunction不为空，则修改RouterFunction的解析器为指定的PathPatternParser
		if (this.routerFunction != null) {
			RouterFunctions.changeParser(this.routerFunction, getPathPatternParser());
		}

	}

	/**
	 * 通过检测应用程序上下文中的路由函数来初始化路由函数。
	 */
	protected void initRouterFunctions() {
		// 获取应用程序上下文中的所有路由函数
		List<RouterFunction<?>> routerFunctions = routerFunctions();
		// 将所有路由函数合并成一个路由函数，以便于执行
		this.routerFunction = routerFunctions.stream().reduce(RouterFunction::andOther).orElse(null);
		// 记录日志，展示检测到的路由函数
		logRouterFunctions(routerFunctions);
	}

	/**
	 * 获取应用程序上下文中所有RouterFunction的列表。
	 *
	 * @return 返回包含应用程序上下文中所有RouterFunction的列表
	 */
	private List<RouterFunction<?>> routerFunctions() {
		return obtainApplicationContext()
				// 通过BeanProvider获取所有RouterFunction实例的有序流
				.getBeanProvider(RouterFunction.class)
				.orderedStream()
				// 将每个实例转换为RouterFunction类型并收集为列表
				.map(router -> (RouterFunction<?>) router)
				.collect(Collectors.toList());
	}

	/**
	 * 记录检测到的路由函数信息，根据日志级别输出相应的信息。
	 *
	 * @param routerFunctions 检测到的路由函数列表
	 */
	private void logRouterFunctions(List<RouterFunction<?>> routerFunctions) {
		if (mappingsLogger.isDebugEnabled()) {
			// 如果mappingsLogger的日志级别是DEBUG，则使用debug级别输出每个路由函数的信息
			routerFunctions.forEach(function -> mappingsLogger.debug("Mapped " + function));
		} else if (logger.isDebugEnabled()) {
			int total = routerFunctions.size();
			String message = total + " RouterFunction(s) in " + formatMappingName();
			if (logger.isTraceEnabled()) {
				if (total > 0) {
					// 如果logger的日志级别是TRACE，并且存在路由函数，则使用trace级别输出每个路由函数的信息
					routerFunctions.forEach(function -> logger.trace("Mapped " + function));
				} else {
					// 如果logger的日志级别是TRACE，并且不存在路由函数，则使用trace级别输出总体信息
					logger.trace(message);
				}
			} else if (total > 0) {
				// 如果logger的日志级别是DEBUG，并且存在路由函数，则使用debug级别输出总体信息
				logger.debug(message);
			}
		}
	}


	/**
	 * 从请求中获取处理程序。
	 *
	 * @param exchange 服务器请求-响应交换对象
	 * @return 返回一个包含处理程序的Mono，如果没有路由函数，则返回一个空的Mono
	 */
	@Override
	protected Mono<?> getHandlerInternal(ServerWebExchange exchange) {
		if (this.routerFunction != null) {
			// 如果存在路由函数，则创建一个ServerRequest对象，并使用路由函数进行路由
			ServerRequest request = ServerRequest.create(exchange, this.messageReaders);
			return this.routerFunction.route(request)
					.doOnNext(handler -> setAttributes(exchange.getAttributes(), request, handler));
		} else {
			// 如果没有路由函数，则返回一个空的Mono
			return Mono.empty();
		}
	}

	/**
	 * 设置请求的属性。
	 *
	 * @param attributes       请求的属性集合
	 * @param serverRequest    服务器请求对象
	 * @param handlerFunction  处理函数
	 */
	@SuppressWarnings("unchecked")
	private void setAttributes(
			Map<String, Object> attributes, ServerRequest serverRequest, HandlerFunction<?> handlerFunction) {
		// 设置默认请求属性
		attributes.put(RouterFunctions.REQUEST_ATTRIBUTE, serverRequest);
		attributes.put(BEST_MATCHING_HANDLER_ATTRIBUTE, handlerFunction);

		// 检查是否有匹配模式属性，并将其设置为最佳匹配模式属性
		PathPattern matchingPattern = (PathPattern) attributes.get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE);
		if (matchingPattern != null) {
			attributes.put(BEST_MATCHING_PATTERN_ATTRIBUTE, matchingPattern);
		}

		// 检查是否有URI模板变量属性，并将其设置为URI模板变量属性
		Map<String, String> uriVariables =
				(Map<String, String>) attributes.get(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		if (uriVariables != null) {
			attributes.put(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariables);
		}
	}

}
