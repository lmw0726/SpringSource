/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.server.handler;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * {@link WebFilterChain} 的默认实现。
 *
 * <p>该类的每个实例代表链中的一个链接。公共构造函数 {@link #DefaultWebFilterChain(WebHandler, List)}
 * 初始化完整的链并表示其第一个链接。
 *
 * <p>此类是不可变的且线程安全的。它可以创建一次并重复使用以同时处理请求。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultWebFilterChain implements WebFilterChain {
	/**
	 * 所有的Web拦截器
	 */
	private final List<WebFilter> allFilters;

	/**
	 * Web请求处理器
	 */
	private final WebHandler handler;

	/**
	 * 当前的Web拦截器
	 */
	@Nullable
	private final WebFilter currentFilter;

	/**
	 * 默认的Web拦截器链
	 */
	@Nullable
	private final DefaultWebFilterChain chain;


	/**
	 * 公共构造函数，带有过滤器列表和要使用的目标处理程序。
	 *
	 * @param handler 目标处理程序
	 * @param filters 处理程序之前的过滤器
	 * @since 5.1
	 */
	public DefaultWebFilterChain(WebHandler handler, List<WebFilter> filters) {
		Assert.notNull(handler, "WebHandler is required");
		this.allFilters = Collections.unmodifiableList(filters);
		this.handler = handler;
		// 初始化拦截器链
		DefaultWebFilterChain chain = initChain(filters, handler);
		this.currentFilter = chain.currentFilter;
		this.chain = chain.chain;
	}

	private static DefaultWebFilterChain initChain(List<WebFilter> filters, WebHandler handler) {
		// 创建默认的Web过滤器链，初始时尾部的后续处理器和上下文都为null
		DefaultWebFilterChain chain = new DefaultWebFilterChain(filters, handler, null, null);
		// 使用迭代器逆序遍历过滤器列表
		ListIterator<? extends WebFilter> iterator = filters.listIterator(filters.size());
		while (iterator.hasPrevious()) {
			// 将当前过滤器和链的引用创建一个新的Web过滤器链，并将该链作为下一个过滤器链的前一个链
			chain = new DefaultWebFilterChain(filters, handler, iterator.previous(), chain);
		}
		// 返回最终的Web过滤器链
		return chain;
	}

	/**
	 * 代表链中一个链接的私有构造函数。
	 */
	private DefaultWebFilterChain(List<WebFilter> allFilters, WebHandler handler,
								  @Nullable WebFilter currentFilter, @Nullable DefaultWebFilterChain chain) {

		this.allFilters = allFilters;
		this.currentFilter = currentFilter;
		this.handler = handler;
		this.chain = chain;
	}

	/**
	 * 公共构造函数，带有过滤器列表和要使用的目标处理程序。
	 *
	 * @param handler 目标处理程序
	 * @param filters 处理程序之前的过滤器
	 * @deprecated 自 5.1 版本起，此构造函数已弃用，改为使用 {@link #DefaultWebFilterChain(WebHandler, List)}。
	 */
	@Deprecated
	public DefaultWebFilterChain(WebHandler handler, WebFilter... filters) {
		this(handler, Arrays.asList(filters));
	}


	public List<WebFilter> getFilters() {
		return this.allFilters;
	}

	public WebHandler getHandler() {
		return this.handler;
	}


	@Override
	public Mono<Void> filter(ServerWebExchange exchange) {
		return Mono.defer(() ->
				// 如果当前过滤器和链都不为空，则调用当前过滤器处理请求
				// 否则调用处理器处理请求
				this.currentFilter != null && this.chain != null ?
						invokeFilter(this.currentFilter, this.chain, exchange) :
						this.handler.handle(exchange));
	}

	private Mono<Void> invokeFilter(WebFilter current, DefaultWebFilterChain chain, ServerWebExchange exchange) {
		String currentName = current.getClass().getName();
		// 对当前过滤器进行过滤操作，并添加检查点信息
		return current.filter(exchange, chain).checkpoint(currentName + " [DefaultWebFilterChain]");
	}

}
