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

package org.springframework.web.filter;

import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 一个通用的复合 Servlet {@link Filter}，它只是将其行为委托给用户提供的一系列过滤器，
 * 从而实现了 {@link FilterChain} 的功能，但方便地只使用 {@link Filter} 实例。
 *
 * <p>这对于需要依赖注入的过滤器非常有用，因此可以在 Spring 应用程序上下文中进行设置。
 * 通常，这个复合过滤器将与 {@link DelegatingFilterProxy} 一起使用，这样它就可以在 Spring 中声明，
 * 但应用于 Servlet 上下文。
 *
 * @author Dave Syer
 * @since 3.1
 */
public class CompositeFilter implements Filter {
	/**
	 * 过滤器列表
	 */
	private List<? extends Filter> filters = new ArrayList<>();


	public void setFilters(List<? extends Filter> filters) {
		this.filters = new ArrayList<>(filters);
	}


	/**
	 * 初始化所有的过滤器，按顺序调用每个过滤器的 init 方法。
	 *
	 * @see Filter#init(FilterConfig)
	 */
	@Override
	public void init(FilterConfig config) throws ServletException {
		// 遍历所有的过滤器
		for (Filter filter : this.filters) {
			// 逐个调用每一个过滤器的初始化方法
			filter.init(config);
		}
	}

	/**
	 * 从提供的委托过滤器列表（{@link #setFilters}）中形成一个临时链，
	 * 并按顺序执行它们。每个过滤器将请求委托给列表中的下一个过滤器，
	 * 尽管这是一个 {@link Filter}，但它实现了 {@link FilterChain} 的正常行为。
	 *
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		new VirtualFilterChain(chain, this.filters).doFilter(request, response);
	}

	/**
	 * 清理提供的所有过滤器，按相反的顺序调用每个过滤器的 destroy 方法。
	 *
	 * @see Filter#init(FilterConfig)
	 */
	@Override
	public void destroy() {
		// 倒序遍历过滤器列表
		for (int i = this.filters.size(); i-- > 0; ) {
			Filter filter = this.filters.get(i);
			// 逐个调用每一个过滤器的销毁方法
			filter.destroy();
		}
	}


	private static class VirtualFilterChain implements FilterChain {
		/**
		 * 过滤器链
		 */
		private final FilterChain originalChain;

		/**
		 * 额外的过滤器列表
		 */
		private final List<? extends Filter> additionalFilters;

		/**
		 * 当前位置
		 */
		private int currentPosition = 0;

		public VirtualFilterChain(FilterChain chain, List<? extends Filter> additionalFilters) {
			this.originalChain = chain;
			this.additionalFilters = additionalFilters;
		}

		@Override
		public void doFilter(final ServletRequest request, final ServletResponse response)
				throws IOException, ServletException {

			// 如果 当前位置 达到 最后一个过滤器
			if (this.currentPosition == this.additionalFilters.size()) {
				// 调用原始的过滤链的 doFilter 方法，继续处理请求和响应
				this.originalChain.doFilter(request, response);
			} else {
				// 当前位置+1
				this.currentPosition++;
				// 获取下一个要执行的过滤器
				Filter nextFilter = this.additionalFilters.get(this.currentPosition - 1);
				// 调用下一个过滤器的 doFilter 方法，继续处理请求和响应，并传递当前过滤链
				nextFilter.doFilter(request, response, this);
			}
		}
	}

}
