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

package org.springframework.web.filter;

import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 过滤器基类，旨在保证每个请求调度仅执行一次，无论在任何Servlet容器上。
 * 它提供了一个带有HttpServletRequest和HttpServletResponse参数的{@link #doFilterInternal}方法。
 *
 * <p>从Servlet 3.0开始，过滤器可以作为独立线程中发生的{@link javax.servlet.DispatcherType#REQUEST REQUEST}或{@link javax.servlet.DispatcherType#ASYNC ASYNC}调度的一部分来调用。
 * 可以在{@code web.xml}中配置过滤器是否应该参与异步调度。但是，在某些情况下，Servlet容器假定不同的默认配置。
 * 因此，子类可以覆盖{@link #shouldNotFilterAsyncDispatch()}方法，在静态声明它们是否确实应该在两种类型的调度期间（每次调度一次）中调用，以便提供线程初始化、日志记录、安全性等。
 * 此机制补充了而不是替换了在{@code web.xml}中配置具有调度程序类型的过滤器的需求。
 *
 * <p>子类可以使用{@link #isAsyncDispatch(HttpServletRequest)}来确定何时作为异步调度的一部分调用过滤器，并使用{@link #isAsyncStarted(HttpServletRequest)}来确定请求是否已经进入异步模式，因此当前调度不会是给定请求的最后一个。
 *
 * <p>另一个调度类型也在其自己的线程中发生，即{@link javax.servlet.DispatcherType#ERROR ERROR}。
 * 如果子类希望在错误调度期间静态声明它们是否应该被调用一次，则可以覆盖{@link #shouldNotFilterErrorDispatch()}。
 *
 * <p>{@link #getAlreadyFilteredAttributeName}方法确定如何标识已经过滤的请求。
 * 默认实现基于具体过滤器实例的配置名称。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 06.12.2003
 */
public abstract class OncePerRequestFilter extends GenericFilterBean {

	/**
	 * 附加到“已过滤”请求属性的过滤器名称的后缀。
	 *
	 * @see #getAlreadyFilteredAttributeName
	 */
	public static final String ALREADY_FILTERED_SUFFIX = ".FILTERED";


	/**
	 * 此{@code doFilter}实现存储了一个“已过滤”请求属性，如果属性已经存在，则在不进行再次过滤的情况下进行。
	 *
	 * @see #getAlreadyFilteredAttributeName
	 * @see #shouldNotFilter
	 * @see #doFilterInternal
	 */
	@Override
	public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 检查请求和响应是否是 HttpServletRequest 和 HttpServletResponse 的实例
		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			// 如果不是，则抛出 ServletException
			throw new ServletException("OncePerRequestFilter just supports HTTP requests");
		}

		// 将请求和响应转换为 HttpServletRequest 和 HttpServletResponse 实例
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		// 获取已经过滤属性的名称
		String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();
		// 检查是否已经在本次请求中过滤了该过滤器
		boolean hasAlreadyFilteredAttribute = request.getAttribute(alreadyFilteredAttributeName) != null;

		// 跳过该过滤器的情况或者不应该对该请求进行过滤
		if (skipDispatch(httpRequest) || shouldNotFilter(httpRequest)) {
			// 在不调用此过滤器的情况下继续...
			filterChain.doFilter(request, response);
		} else if (hasAlreadyFilteredAttribute) {
			// 如果已经在本次请求中过滤了该过滤器，且请求的调度类型为 ERROR
			if (DispatcherType.ERROR.equals(request.getDispatcherType())) {
				// 执行嵌套错误调度的过滤操作
				doFilterNestedErrorDispatch(httpRequest, httpResponse, filterChain);
				return;
			}

			// 在不调用此过滤器的情况下继续...
			filterChain.doFilter(request, response);
		} else {
			// 调用此过滤器...
			// 将已经过滤的属性设置为 true，表示该过滤器已经对此请求进行过滤
			request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
			try {
				// 调用过滤器的 doFilterInternal 方法进行实际的过滤操作
				doFilterInternal(httpRequest, httpResponse, filterChain);
			} finally {
				// 在过滤器操作完成后，删除已经过滤的属性，以便下次请求可以再次进行过滤
				request.removeAttribute(alreadyFilteredAttributeName);
			}
		}
	}

	private boolean skipDispatch(HttpServletRequest request) {
		// 如果请求是异步调度，并且不应该对异步调度进行过滤，则返回 true
		if (isAsyncDispatch(request) && shouldNotFilterAsyncDispatch()) {
			return true;
		}

		// 如果请求是错误调度，并且不应该对错误调度进行过滤，则返回 true
		if (request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE) != null && shouldNotFilterErrorDispatch()) {
			return true;
		}

		// 否则返回 false
		return false;
	}

	/**
	 * 在Servlet 3.0中引入的调度程序类型{@code javax.servlet.DispatcherType.ASYNC}意味着一个过滤器可以在单个请求的过程中在多个线程中调用。
	 * 如果过滤器当前正在异步调度中执行，则此方法返回{@code true}。
	 *
	 * @param request 当前请求
	 * @see WebAsyncManager#hasConcurrentResult()
	 * @since 3.2
	 */
	protected boolean isAsyncDispatch(HttpServletRequest request) {
		return DispatcherType.ASYNC.equals(request.getDispatcherType());
	}

	/**
	 * 请求处理是否处于异步模式，这意味着在当前线程退出后响应将不会被提交。
	 *
	 * @param request 当前请求
	 * @see WebAsyncManager#isConcurrentHandlingStarted()
	 * @since 3.2
	 */
	protected boolean isAsyncStarted(HttpServletRequest request) {
		return WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted();
	}

	/**
	 * 返回标识请求已经被过滤的请求属性的名称。
	 * <p>默认实现使用具体过滤器实例的配置名称，然后附加“.FILTERED”后缀。
	 * 如果过滤器未完全初始化，则回退到其类名。
	 *
	 * @see #getFilterName
	 * @see #ALREADY_FILTERED_SUFFIX
	 */
	protected String getAlreadyFilteredAttributeName() {
		String name = getFilterName();
		if (name == null) {
			// 如果过滤器名称为空，则获取类名
			name = getClass().getName();
		}
		// 附加过滤后缀
		return name + ALREADY_FILTERED_SUFFIX;
	}

	/**
	 * 子类可以覆盖此方法以进行自定义过滤控制，
	 * 返回{@code true}以避免过滤给定请求。
	 * <p>默认实现始终返回{@code false}。
	 *
	 * @param request 当前HTTP请求
	 * @return 给定请求是否<i>不应</i>被过滤
	 * @throws ServletException 如果发生错误
	 */
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return false;
	}

	/**
	 * Servlet 3.0中引入的调度程序类型{@code javax.servlet.DispatcherType.ASYNC}意味着一个过滤器可以在单个请求的过程中在多个线程中调用。
	 * 一些过滤器只需要过滤初始线程（例如请求包装），而其他过滤器可能需要在每个附加线程中至少调用一次，例如用于设置线程本地变量或在最后执行最终处理。
	 * <p>请注意，虽然过滤器可以通过{@code web.xml}或通过{@code ServletContext}的Java中映射以处理特定的调度程序类型，但是Servlet容器可能强制执行与调度程序类型相关的不同默认值。
	 * 此标志强制执行过滤器的设计意图。
	 * <p>默认返回值为“true”，这意味着过滤器在后续的异步调度期间不会被调用。
	 * 如果为“false”，则过滤器将在异步调度期间被调用，并具有在单个线程中请求期间仅被调用一次的相同保证。
	 *
	 * @since 3.2
	 */
	protected boolean shouldNotFilterAsyncDispatch() {
		return true;
	}

	/**
	 * 是否过滤错误调度，例如当Servlet容器处理并在{@code web.xml}中映射的错误调度时。
	 * 默认返回值为“true”，这意味着在错误调度情况下不会调用过滤器。
	 *
	 * @since 3.2
	 */
	protected boolean shouldNotFilterErrorDispatch() {
		return true;
	}


	/**
	 * 与{@code doFilter}的合同相同，但保证在单个请求线程中仅被调用一次。
	 * 详细信息请参见{@link #shouldNotFilterAsyncDispatch()}。
	 * <p>提供HttpServletRequest和HttpServletResponse参数，而不是默认的ServletRequest和ServletResponse参数。
	 */
	protected abstract void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException;

	/**
	 * 通常，在REQUEST调度完成后会发生ERROR调度，并且过滤器链会重新开始。
	 * 但是在某些服务器上，ERROR调度可能嵌套在REQUEST调度中，例如由于在响应上调用{@code sendError}的结果。
	 * 在这种情况下，我们仍然在过滤器链中，处于同一线程上，但请求和响应已切换到原始的、未包装的请求和响应。
	 * <p>子类可以使用此方法过滤这样的嵌套ERROR调度，并在请求或响应上重新应用包装。
	 * 如果有的话，{@code ThreadLocal}上下文应该仍然处于活动状态，因为我们仍然处于过滤器链的嵌套中。
	 *
	 * @since 5.1.9
	 */
	protected void doFilterNestedErrorDispatch(HttpServletRequest request, HttpServletResponse response,
											   FilterChain filterChain) throws ServletException, IOException {

		filterChain.doFilter(request, response);
	}

}
