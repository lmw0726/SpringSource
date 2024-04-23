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

package org.springframework.web.servlet.function.support;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.SpringProperties;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 实现了 {@link HandlerMapping} 接口，支持 {@link RouterFunction RouterFunctions}。
 *
 * <p>如果在构造时没有提供 {@link RouterFunction}，则此映射将检测应用程序上下文中的所有路由函数，并按
 * {@linkplain org.springframework.core.annotation.Order order} 调用它们。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 5.2
 */
public class RouterFunctionMapping extends AbstractHandlerMapping implements InitializingBean {

	/**
	 * 一个布尔标志，由 {@code spring.xml.ignore} 系统属性控制，指示 Spring 是否应忽略 XML，即不初始化与 XML 相关的基础设施。
	 * <p>默认为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");

	/**
	 * 路由函数
	 */
	@Nullable
	private RouterFunction<?> routerFunction;

	/**
	 * HTTP消息转换器列表
	 */
	private List<HttpMessageConverter<?>> messageConverters = Collections.emptyList();

	/**
	 * 设置是否在祖先 应用上下文 中检测处理器函数。
	 */
	private boolean detectHandlerFunctionsInAncestorContexts = false;


	/**
	 * 创建一个空的 {@code RouterFunctionMapping}。
	 * <p>如果使用此构造函数，则此映射将检测应用程序上下文中的所有 {@link RouterFunction} 实例。
	 */
	public RouterFunctionMapping() {
	}

	/**
	 * 使用给定的 {@link RouterFunction} 创建一个 {@code RouterFunctionMapping}。
	 * <p>如果使用此构造函数，则不会进行应用程序上下文检测。
	 *
	 * @param routerFunction 用于映射的路由函数
	 */
	public RouterFunctionMapping(RouterFunction<?> routerFunction) {
		this.routerFunction = routerFunction;
	}


	/**
	 * 设置要映射到的路由函数。
	 * <p>如果使用此属性，则不会进行应用程序上下文检测。
	 */
	public void setRouterFunction(@Nullable RouterFunction<?> routerFunction) {
		this.routerFunction = routerFunction;
	}

	/**
	 * 返回配置的 {@link RouterFunction}。
	 * <p><strong>注意：</strong>当从 ApplicationContext 检测到路由函数时，如果在 {@link #afterPropertiesSet()} 之前调用此方法，则可能返回 {@code null}。
	 *
	 * @return 路由函数或 {@code null}
	 */
	@Nullable
	public RouterFunction<?> getRouterFunction() {
		return this.routerFunction;
	}

	/**
	 * 设置要使用的消息体转换器。
	 * <p>这些转换器用于在 HTTP 请求和响应之间进行转换。
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * 设置是否在祖先 ApplicationContext 中检测处理器函数。
	 * <p>默认值为 "false"：仅在当前 ApplicationContext 中检测处理器函数，即仅在此 HandlerMapping 所在的上下文中检测（通常是当前 DispatcherServlet 的上下文）。
	 * <p>将此标志打开以在祖先上下文（通常是 Spring 根 WebApplicationContext）中检测处理器函数。
	 */
	public void setDetectHandlerFunctionsInAncestorContexts(boolean detectHandlerFunctionsInAncestorContexts) {
		this.detectHandlerFunctionsInAncestorContexts = detectHandlerFunctionsInAncestorContexts;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		// 如果路由函数为空，则初始化路由函数
		if (this.routerFunction == null) {
			initRouterFunctions();
		}

		// 如果消息转换器集合为空，则初始化消息转换器
		if (CollectionUtils.isEmpty(this.messageConverters)) {
			initMessageConverters();
		}

		// 如果路由函数不为空，则修改路由函数的路径模式解析器
		if (this.routerFunction != null) {
			// 获取路径模式解析器
			PathPatternParser patternParser = getPatternParser();
			if (patternParser == null) {
				// 如果路径模式解析器为空，则创建一个新的路径模式解析器
				patternParser = new PathPatternParser();
				// 设置模式解析器
				setPatternParser(patternParser);
			}
			//更改路由函数中的路径解析器
			RouterFunctions.changeParser(this.routerFunction, patternParser);
		}
	}

	/**
	 * 检测当前应用程序上下文中的所有 {@linkplain RouterFunction router functions}。
	 */
	private void initRouterFunctions() {
		// 获取应用程序上下文中的所有路由函数，并按顺序收集到列表中
		List<RouterFunction<?>> routerFunctions = obtainApplicationContext()
				.getBeanProvider(RouterFunction.class)
				.orderedStream()
				.map(router -> (RouterFunction<?>) router)
				.collect(Collectors.toList());

		// 获取父上下文
		ApplicationContext parentContext = obtainApplicationContext().getParent();
		// 如果存在父上下文且不需要在祖先上下文中检测处理程序函数
		if (parentContext != null && !this.detectHandlerFunctionsInAncestorContexts) {
			// 从父上下文中移除已存在的路由函数
			parentContext.getBeanProvider(RouterFunction.class)
					.stream()
					.forEach(routerFunctions::remove);
		}

		// 将所有路由函数组合成一个单一的路由函数
		this.routerFunction = routerFunctions.stream()
				.reduce(RouterFunction::andOther)
				.orElse(null);

		// 记录日志以显示已配置的路由函数
		logRouterFunctions(routerFunctions);
	}

	private void logRouterFunctions(List<RouterFunction<?>> routerFunctions) {
		// 如果 mappingsLogger 开启了 DEBUG 级别的日志记录
		if (mappingsLogger.isDebugEnabled()) {
			// 遍历路由函数，并记录调试级别的日志
			routerFunctions.forEach(function -> mappingsLogger.debug("Mapped " + function));
		} else if (logger.isDebugEnabled()) {
			// 获取路由函数的数量
			int total = routerFunctions.size();
			// 构建日志消息
			String message = total + " RouterFunction(s) in " + formatMappingName();
			if (logger.isTraceEnabled()) {
				// 如果日志级别是 TRACE
				if (total > 0) {
					// 如果有路由函数，记录 TRACE 级别的日志
					routerFunctions.forEach(function -> logger.trace("Mapped " + function));
				} else {
					// 否则，记录 TRACE 级别的消息
					logger.trace(message);
				}
			} else if (total > 0) {
				// 如果没有开启 TRACE 日志级别，但有路由函数，记录 DEBUG 级别的消息
				logger.debug(message);
			}
		}
	}

	/**
	 * 初始化一组默认的 {@linkplain HttpMessageConverter message converters}。
	 */
	private void initMessageConverters() {
		// 创建一个 HttpMessageConverter 列表
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>(4);
		// 添加 ByteArrayHttpMessageConverter 实例
		messageConverters.add(new ByteArrayHttpMessageConverter());
		// 添加 StringHttpMessageConverter 实例
		messageConverters.add(new StringHttpMessageConverter());

		// 如果不应该忽略 XML
		if (!shouldIgnoreXml) {
			try {
				// 尝试添加 SourceHttpMessageConverter 实例
				messageConverters.add(new SourceHttpMessageConverter<>());
			} catch (Error err) {
				// 当没有可用的 TransformerFactory 实现时，忽略异常
			}
		}

		// 添加 AllEncompassingFormHttpMessageConverter 实例
		messageConverters.add(new AllEncompassingFormHttpMessageConverter());

		// 将创建的消息转换器列表赋值给对象属性
		this.messageConverters = messageConverters;
	}


	@Override
	@Nullable
	protected Object getHandlerInternal(HttpServletRequest servletRequest) throws Exception {
		// 如果存在路由函数
		if (this.routerFunction != null) {
			// 创建 ServerRequest 对象
			ServerRequest request = ServerRequest.create(servletRequest, this.messageConverters);
			// 获得匹配的 HandlerFunction
			HandlerFunction<?> handlerFunction = this.routerFunction.route(request).orElse(null);
			// 设置请求属性
			setAttributes(servletRequest, request, handlerFunction);
			// 返回 HandlerFunction
			return handlerFunction;
		} else {
			// 如果路由函数为空，则返回 null
			return null;
		}
	}

	private void setAttributes(HttpServletRequest servletRequest, ServerRequest request,
							   @Nullable HandlerFunction<?> handlerFunction) {

		// 从 Servlet请求 中获取匹配的 路径模式
		PathPattern matchingPattern =
				(PathPattern) servletRequest.getAttribute(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE);
		// 如果存在匹配的 路径模式
		if (matchingPattern != null) {
			// 移除 Servlet请求 中的匹配的 路径模式 属性
			servletRequest.removeAttribute(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE);
			// 设置 Servlet请求 中的最佳匹配的 路径模式 属性
			servletRequest.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, matchingPattern.getPatternString());
		}
		// 设置 Servlet请求 中的最佳匹配的 处理器函数 属性
		servletRequest.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, handlerFunction);
		// 设置 Servlet请求 中的 RouterFunctions.REQUEST_ATTRIBUTE 属性
		servletRequest.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, request);
	}

}
