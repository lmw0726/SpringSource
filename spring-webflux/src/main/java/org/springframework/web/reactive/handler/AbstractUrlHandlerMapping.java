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

import org.springframework.beans.BeansException;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.BiPredicate;

/**
 * URL映射的抽象基类，用于{@link org.springframework.web.reactive.HandlerMapping}的实现。
 *
 * <p>支持直接匹配，例如注册的"/test"匹配"/test"，以及各种路径模式匹配，
 * 例如注册的"/t*"模式匹配"/test"和"/team"，"/test/*"匹配"/test"下的所有路径，
 * "/test/**"匹配"/test"下的所有路径及其子路径。有关详细信息，请参阅
 * {@link org.springframework.web.util.pattern.PathPattern} 的javadoc。
 *
 * <p>将搜索所有路径模式，以找到当前请求路径的最具体匹配。
 * 最具体的模式定义为具有最长路径模式、捕获变量和通配符最少的路径模式。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractUrlHandlerMapping extends AbstractHandlerMapping {

	/**
	 * 是否采用延迟初始化处理程序，默认为false
	 */
	private boolean lazyInitHandlers = false;

	/**
	 * 路径模式与处理程序映射关系的集合
	 */
	private final Map<PathPattern, Object> handlerMap = new LinkedHashMap<>();

	/**
	 * 处理程序断言，判断处理程序与ServerWebExchange是否匹配
	 */
	@Nullable
	private BiPredicate<Object, ServerWebExchange> handlerPredicate;


	/**
	 * 设置是否懒初始化处理程序。仅适用于单例处理程序，因为原型始终会延迟初始化。
	 * 默认值为 "false"，因为急切初始化通过直接引用控制器对象可以实现更高效率。
	 * <p>如果要允许控制器进行懒初始化，请将它们设置为 "lazy-init" 并将此标志设置为 true。
	 * 仅将它们设置为 "lazy-init" 不起作用，因为在这种情况下，它们是通过处理程序映射中的引用初始化的。
	 */
	public void setLazyInitHandlers(boolean lazyInitHandlers) {
		this.lazyInitHandlers = lazyInitHandlers;
	}

	/**
	 * 返回已注册的路径模式和处理程序的只读视图，这些处理程序可能是实际的处理程序实例，
	 * 也可能是懒初始化处理程序的bean名称。
	 */
	public final Map<PathPattern, Object> getHandlerMap() {
		return Collections.unmodifiableMap(this.handlerMap);
	}

	/**
	 * 配置用于通过URL路径匹配的处理程序的扩展匹配的断言。这允许通过检查请求的其他属性来进一步缩小映射。
	 * 如果断言返回 "false"，它将导致无匹配，从而允许另一个
	 * {@link org.springframework.web.reactive.HandlerMapping} 进行匹配，
	 * 或导致404（NOT_FOUND）响应。
	 *
	 * @param handlerPredicate 用于将候选处理程序与当前交换匹配的双重断言。
	 * @see org.springframework.web.reactive.socket.server.support.WebSocketUpgradeHandlerPredicate
	 * @since 5.3.5
	 */
	public void setHandlerPredicate(BiPredicate<Object, ServerWebExchange> handlerPredicate) {
		this.handlerPredicate = (this.handlerPredicate != null ?
				this.handlerPredicate.and(handlerPredicate) : handlerPredicate);
	}

	/**
	 * 获取处理程序的内部方法。根据给定的服务器Web交换对象获取处理程序。
	 *
	 * @param exchange 服务器Web交换对象，包含当前请求的信息
	 * @return 一个 {@code Mono} 对象，包含处理程序对象，如果处理程序对象为空，则返回一个空的 {@code Mono}
	 */
	@Override
	public Mono<Object> getHandlerInternal(ServerWebExchange exchange) {
		// 获取当前请求的路径
		PathContainer lookupPath = exchange.getRequest().getPath().pathWithinApplication();
		Object handler;
		try {
			// 查找处理程序
			handler = lookupHandler(lookupPath, exchange);
		} catch (Exception ex) {
			// 如果出现异常，返回一个带有异常的 Mono
			return Mono.error(ex);
		}
		// 返回一个包含处理程序对象的 Mono，如果处理程序对象为空，则返回一个空的 Mono
		return Mono.justOrEmpty(handler);
	}

	/**
	 * 查找给定URL查找路径的处理程序实例。
	 * <p>支持直接匹配，例如注册的"/test"匹配"/test"，
	 * 以及各种路径模式匹配，例如注册的"/t*"匹配"/test"和"/team"。
	 * 有关详细信息，请参阅PathPattern类。
	 *
	 * @param lookupPath 要查找处理程序的URL路径
	 * @param exchange   当前交换对象
	 * @return 关联的处理程序实例；如果未找到，则返回{@code null}
	 * @throws Exception 查找处理程序时可能抛出的异常
	 * @see org.springframework.web.util.pattern.PathPattern
	 */
	@Nullable
	protected Object lookupHandler(PathContainer lookupPath, ServerWebExchange exchange) throws Exception {
		// 匹配结果列表
		List<PathPattern> matches = null;

		// 遍历处理程序映射中的每个路径模式
		for (PathPattern pattern : this.handlerMap.keySet()) {
			// 如果当前路径模式与查找路径匹配
			if (pattern.matches(lookupPath)) {
				// 初始化或添加到匹配结果列表
				matches = (matches != null ? matches : new ArrayList<>());
				matches.add(pattern);
			}
		}

		// 如果没有匹配结果，则返回null
		if (matches == null) {
			return null;
		}

		// 如果有多个匹配结果，根据特定性排序
		if (matches.size() > 1) {
			matches.sort(PathPattern.SPECIFICITY_COMPARATOR);

			// 如果跟踪日志启用，记录匹配的模式
			if (logger.isTraceEnabled()) {
				logger.debug(exchange.getLogPrefix() + "Matching patterns " + matches);
			}
		}

		// 获取最佳匹配的路径模式
		PathPattern pattern = matches.get(0);

		// 从路径模式中提取在模式内的路径
		PathContainer pathWithinMapping = pattern.extractPathWithinPattern(lookupPath);

		// 进行路径匹配和提取
		PathPattern.PathMatchInfo matchInfo = pattern.matchAndExtract(lookupPath);
		Assert.notNull(matchInfo, "Expected a match");

		// 获取与路径模式匹配的处理程序
		Object handler = this.handlerMap.get(pattern);

		// 处理程序是Bean名称还是解析好的处理程序？
		if (handler instanceof String) {
			// 如果是Bean名称，获取相应的Bean实例
			String handlerName = (String) handler;
			handler = obtainApplicationContext().getBean(handlerName);
		}

		// 如果存在处理程序断言且不满足条件，返回null
		if (this.handlerPredicate != null && !this.handlerPredicate.test(handler, exchange)) {
			return null;
		}

		// 验证处理程序
		validateHandler(handler, exchange);

		// 将最佳匹配的处理程序和相关属性放入交换的属性中
		exchange.getAttributes().put(BEST_MATCHING_HANDLER_ATTRIBUTE, handler);
		exchange.getAttributes().put(BEST_MATCHING_PATTERN_ATTRIBUTE, pattern);
		exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathWithinMapping);
		exchange.getAttributes().put(URI_TEMPLATE_VARIABLES_ATTRIBUTE, matchInfo.getUriVariables());

		// 返回最佳匹配的处理程序
		return handler;
	}

	/**
	 * 验证给定处理程序与当前请求是否匹配。
	 * <p>默认实现为空。可以在子类中重写，例如，为了强制执行在URL映射中表达的特定前提条件。
	 *
	 * @param handler  要验证的处理程序对象
	 * @param exchange 当前交换对象
	 */
	@SuppressWarnings("UnusedParameters")
	protected void validateHandler(Object handler, ServerWebExchange exchange) {
	}

	/**
	 * 为给定的URL路径注册指定的处理程序。
	 *
	 * @param urlPaths 应将bean映射到的URL
	 * @param beanName 处理程序bean的名称
	 * @throws BeansException        如果无法注册处理程序
	 * @throws IllegalStateException 如果注册了冲突的处理程序
	 */
	protected void registerHandler(String[] urlPaths, String beanName) throws BeansException, IllegalStateException {
		Assert.notNull(urlPaths, "URL path array must not be null");
		for (String urlPath : urlPaths) {
			registerHandler(urlPath, beanName);
		}
	}

	/**
	 * 为给定的URL路径注册指定的处理程序。
	 *
	 * @param urlPath 应将bean映射到的URL
	 * @param handler 处理程序实例或处理程序bean名称字符串
	 *                （bean名称将自动解析为相应的处理程序bean）
	 * @throws BeansException        如果无法注册处理程序
	 * @throws IllegalStateException 如果已注册冲突的处理程序
	 */
	protected void registerHandler(String urlPath, Object handler) throws BeansException, IllegalStateException {
		Assert.notNull(urlPath, "URL path must not be null");
		Assert.notNull(handler, "Handler object must not be null");
		Object resolvedHandler = handler;

		// 在URL路径前添加斜杠
		urlPath = prependLeadingSlash(urlPath);
		// 解析路径模式
		PathPattern pattern = getPathPatternParser().parse(urlPath);
		if (this.handlerMap.containsKey(pattern)) {
			Object existingHandler = this.handlerMap.get(pattern);
			if (existingHandler != null && existingHandler != resolvedHandler) {
				// 如果该路径模式存在处理器，且该处理器不是当前注册的处理程序
				throw new IllegalStateException(
						"Cannot map " + getHandlerDescription(handler) + " to [" + urlPath + "]: " +
								"there is already " + getHandlerDescription(existingHandler) + " mapped.");
			}
		}

		// 如果引用的是单例处理程序名称，则急切解析处理程序。
		if (!this.lazyInitHandlers && handler instanceof String) {
			// 如果不是懒加载处理器并且当前处理器是字符串类型
			String handlerName = (String) handler;
			if (obtainApplicationContext().isSingleton(handlerName)) {
				// 如果当前处理器名称是单例，则获取该名称对应的处理器实例
				resolvedHandler = obtainApplicationContext().getBean(handlerName);
			}
		}

		// 注册已解析的处理程序
		this.handlerMap.put(pattern, resolvedHandler);
		if (logger.isTraceEnabled()) {
			logger.trace("Mapped [" + urlPath + "] onto " + getHandlerDescription(handler));
		}
	}

	/**
	 * 获取处理程序的描述信息。
	 *
	 * @param handler 处理程序对象
	 * @return 处理程序的描述信息，如果处理程序是字符串类型，则返回带引号的字符串；否则返回处理程序的字符串表示形式
	 */
	private String getHandlerDescription(Object handler) {
		return (handler instanceof String ? "'" + handler + "'" : handler.toString());
	}

	/**
	 * 在模式前添加斜杠。
	 *
	 * @param pattern 要处理的模式字符串
	 * @return 添加斜杠后的模式字符串
	 */
	private static String prependLeadingSlash(String pattern) {
		if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
			return "/" + pattern;
		} else {
			return pattern;
		}
	}

}
