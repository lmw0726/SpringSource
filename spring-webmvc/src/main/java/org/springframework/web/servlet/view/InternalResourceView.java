/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.view;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 用于封装同一Web应用程序中的JSP或其他资源的包装器。
 * 将模型对象作为请求属性公开，并使用{@link javax.servlet.RequestDispatcher}将请求转发到指定的资源URL。
 *
 * <p>此视图的URL应指定Web应用程序中的资源，适用于RequestDispatcher的{@code forward}或{@code include}方法。
 *
 * <p>如果在已包含的请求中操作或在已提交的响应中操作，此视图将退回到包含而不是转发。这可以通过在渲染视图之前调用{@code response.flushBuffer()}（将提交响应）来强制执行。
 *
 * <p>使用{@link InternalResourceViewResolver}的典型用法如下，从DispatcherServlet上下文定义的角度来看：
 *
 * <pre class="code">&lt;bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver"&gt;
 *   &lt;property name="prefix" value="/WEB-INF/jsp/"/&gt;
 *   &lt;property name="suffix" value=".jsp"/&gt;
 * &lt;/bean&gt;</pre>
 * <p>
 * 从处理程序返回的每个视图名称都将被翻译为JSP资源（例如：“myView” &rarr; “/WEB-INF/jsp/myView.jsp”），默认情况下使用此视图类。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see javax.servlet.RequestDispatcher#forward
 * @see javax.servlet.RequestDispatcher#include
 * @see javax.servlet.ServletResponse#flushBuffer
 * @see InternalResourceViewResolver
 * @see JstlView
 */
public class InternalResourceView extends AbstractUrlBasedView {

	/**
	 * 是否始终包含视图而不是转发到它。
	 */
	private boolean alwaysInclude = false;

	/**
	 * 防止循环分发
	 */
	private boolean preventDispatchLoop = false;


	/**
	 * 用作bean的构造函数。
	 *
	 * @see #setUrl
	 * @see #setAlwaysInclude
	 */
	public InternalResourceView() {
	}

	/**
	 * 使用给定的URL创建一个新的InternalResourceView。
	 *
	 * @param url 要转发到的URL
	 * @see #setAlwaysInclude
	 */
	public InternalResourceView(String url) {
		super(url);
	}

	/**
	 * 使用给定的URL创建一个新的InternalResourceView。
	 *
	 * @param url           要转发到的URL
	 * @param alwaysInclude 是否始终包含视图而不是转发到它
	 */
	public InternalResourceView(String url, boolean alwaysInclude) {
		super(url);
		this.alwaysInclude = alwaysInclude;
	}


	/**
	 * 指定是否始终包含视图而不是转发到它。
	 * <p>默认为“false”。将此标志切换为打开以强制使用Servlet包含，即使转发是可能的。
	 *
	 * @see javax.servlet.RequestDispatcher#forward
	 * @see javax.servlet.RequestDispatcher#include
	 * @see #useInclude(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void setAlwaysInclude(boolean alwaysInclude) {
		this.alwaysInclude = alwaysInclude;
	}

	/**
	 * 设置是否显式阻止分派回当前处理程序路径。
	 * <p>默认为“false”。将其设置为“true”可用于基于约定的视图，其中分派回当前处理程序路径是明确的错误。
	 */
	public void setPreventDispatchLoop(boolean preventDispatchLoop) {
		this.preventDispatchLoop = preventDispatchLoop;
	}

	/**
	 * InternalResourceView不严格要求ApplicationContext。
	 */
	@Override
	protected boolean isContextRequired() {
		return false;
	}


	/**
	 * 渲染给定模型的内部资源。这包括将模型设置为请求属性。
	 */
	@Override
	protected void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// 将模型对象作为请求属性公开。
		exposeModelAsRequestAttributes(model, request);

		// 如果有的话，将辅助程序作为请求属性公开。
		exposeHelpers(request);

		// 确定请求分派程序的路径。
		String dispatcherPath = prepareForRendering(request, response);

		// 获取用于目标资源（通常为JSP）的RequestDispatcher。
		RequestDispatcher rd = getRequestDispatcher(request, dispatcherPath);
		if (rd == null) {
			throw new ServletException("Could not get RequestDispatcher for [" + getUrl() +
					"]: Check that the corresponding file exists within your web application archive!");
		}

		// 如果已包含或响应已提交，则执行包含，否则执行转发。
		if (useInclude(request, response)) {
			response.setContentType(getContentType());
			if (logger.isDebugEnabled()) {
				logger.debug("Including [" + getUrl() + "]");
			}
			rd.include(request, response);
		} else {
			// 注意：转发的资源应确定内容类型。
			if (logger.isDebugEnabled()) {
				logger.debug("Forwarding to [" + getUrl() + "]");
			}
			rd.forward(request, response);
		}
	}

	/**
	 * 公开每个渲染操作特有的辅助程序。这是必要的，以防不同的渲染操作会覆盖彼此的上下文等。
	 * <p>由{@link #renderMergedOutputModel(Map, HttpServletRequest, HttpServletResponse)}调用。
	 * 默认实现为空。可以重写此方法以将自定义辅助程序作为请求属性添加。
	 *
	 * @param request 当前HTTP请求
	 * @throws Exception 如果添加属性时发生严重错误
	 * @see #renderMergedOutputModel
	 * @see JstlView#exposeHelpers
	 */
	protected void exposeHelpers(HttpServletRequest request) throws Exception {
	}

	/**
	 * 为渲染做准备，并确定要转发到（或包含的）请求分派程序路径。
	 * <p>此实现仅返回配置的URL。子类可以重写此方法以确定要渲染的资源，通常以不同的方式解释URL。
	 *
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @return 要使用的请求分派程序路径
	 * @throws Exception 如果准备失败
	 * @see #getUrl()
	 */
	protected String prepareForRendering(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		// 获取视图路径
		String path = getUrl();
		Assert.state(path != null, "'url' not set");

		// 如果不允许循环分发
		if (this.preventDispatchLoop) {
			// 获取请求的 URI
			String uri = request.getRequestURI();
			// 如果路径以 '/' 开头，检查 URI 是否与路径相同，否则检查 URI 是否与相对路径拼接后相同
			if (path.startsWith("/") ? uri.equals(path) : uri.equals(StringUtils.applyRelativePath(uri, path))) {
				// 抛出 ServletException，说明存在循环视图路径
				throw new ServletException("Circular view path [" + path + "]: would dispatch back " +
						"to the current handler URL [" + uri + "] again. Check your ViewResolver setup! " +
						"(Hint: This may be the result of an unspecified view, due to default view name generation.)");
			}
		}
		// 返回视图路径
		return path;
	}

	/**
	 * 获取用于转发/包含的RequestDispatcher。
	 * <p>默认实现简单地调用{@link HttpServletRequest#getRequestDispatcher(String)}。
	 * 可以在子类中重写。
	 *
	 * @param request 当前HTTP请求
	 * @param path    目标URL（从{@link #prepareForRendering}返回）
	 * @return 相应的RequestDispatcher
	 */
	@Nullable
	protected RequestDispatcher getRequestDispatcher(HttpServletRequest request, String path) {
		return request.getRequestDispatcher(path);
	}

	/**
	 * 确定是否使用RequestDispatcher的{@code include}或{@code forward}方法。
	 * <p>执行检查，看是否在请求中找到了包含URI属性，表明是一个包含请求，以及响应是否已经提交。
	 * 在这两种情况下，都将执行包含，因为不再可能进行转发。
	 *
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @return {@code true}表示包含，{@code false}表示转发
	 * @see javax.servlet.RequestDispatcher#forward
	 * @see javax.servlet.RequestDispatcher#include
	 * @see javax.servlet.ServletResponse#isCommitted
	 * @see org.springframework.web.util.WebUtils#isIncludeRequest
	 */
	protected boolean useInclude(HttpServletRequest request, HttpServletResponse response) {
		return (this.alwaysInclude || WebUtils.isIncludeRequest(request) || response.isCommitted());
	}

}
