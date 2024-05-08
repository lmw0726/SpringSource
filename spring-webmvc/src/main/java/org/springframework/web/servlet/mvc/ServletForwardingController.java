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

package org.springframework.web.servlet.mvc;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Spring 控制器实现，它将转发到一个命名的 servlet，即 web.xml 中的 "servlet-name" 而不是 URL 路径映射。
 * 目标 servlet 甚至不需要在 web.xml 中拥有 "servlet-mapping"：仅有一个 "servlet" 声明就足够了。
 *
 * <p>通过 Spring 的调度基础结构来调用现有的 servlet 是很有用的，例如应用 Spring HandlerInterceptors 到其请求。
 * 即使在不支持 Servlet 过滤器的最小 Servlet 容器中，这也将起作用。
 *
 * <p><b>示例：</b> web.xml，将所有 "/myservlet" 请求映射到 Spring 调度程序。
 * 还定义了一个自定义的 "myServlet"，但是 <i>没有</i> servlet 映射。
 *
 * <pre class="code">
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;myServlet&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;mypackage.TestServlet&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 *
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;myDispatcher&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;org.springframework.web.servlet.DispatcherServlet&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 *
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;myDispatcher&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;/myservlet&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;</pre>
 *
 * <b>示例：</b> myDispatcher-servlet.xml，将 "/myservlet" 转发到您的 servlet（由 servlet 名称标识）。
 * 所有这样的请求都将通过配置的 HandlerInterceptor 链（例如 OpenSessionInViewInterceptor）。
 * 从 servlet 的角度来看，一切都将像往常一样工作。
 *
 * <pre class="code">
 * &lt;bean id="urlMapping" class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping"&gt;
 *   &lt;property name="interceptors"&gt;
 *     &lt;list&gt;
 *       &lt;ref bean="openSessionInViewInterceptor"/&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 *   &lt;property name="mappings"&gt;
 *     &lt;props&gt;
 *       &lt;prop key="/myservlet"&gt;myServletForwardingController&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myServletForwardingController" class="org.springframework.web.servlet.mvc.ServletForwardingController"&gt;
 *   &lt;property name="servletName"&gt;&lt;value&gt;myServlet&lt;/value&gt;&lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @see ServletWrappingController
 * @see org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor
 * @see org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter
 * @since 1.1.1
 */
public class ServletForwardingController extends AbstractController implements BeanNameAware {

	/**
	 * Servlet名称
	 */
	@Nullable
	private String servletName;

	/**
	 * bean名称
	 */
	@Nullable
	private String beanName;


	public ServletForwardingController() {
		super(false);
	}


	/**
	 * 设置要转发到的 servlet 的名称，
	 * 即 web.xml 中目标 servlet 的 "servlet-name"。
	 * <p>默认是此控制器的 bean 名称。
	 */
	public void setServletName(String servletName) {
		this.servletName = servletName;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
		if (this.servletName == null) {
			// 如果Servlet名称为空，则设置Servlet名称为当前的名称
			this.servletName = name;
		}
	}


	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		// 获取 Servlet上下文
		ServletContext servletContext = getServletContext();
		Assert.state(servletContext != null, "No ServletContext");

		// 获取具有指定名称的 请求分发器
		RequestDispatcher rd = servletContext.getNamedDispatcher(this.servletName);
		if (rd == null) {
			// 如果找不到 请求分发器，则抛出异常
			throw new ServletException("No servlet with name '" + this.servletName + "' defined in web.xml");
		}

		// 根据条件选择是使用 include 还是 forward
		if (useInclude(request, response)) {
			// 使用 include 方法将请求和响应传递给指定的 servlet
			rd.include(request, response);
			if (logger.isTraceEnabled()) {
				logger.trace("Included servlet [" + this.servletName +
						"] in ServletForwardingController '" + this.beanName + "'");
			}
		} else {
			// 使用 forward 方法将请求和响应转发给指定的 servlet
			rd.forward(request, response);
			if (logger.isTraceEnabled()) {
				logger.trace("Forwarded to servlet [" + this.servletName +
						"] in ServletForwardingController '" + this.beanName + "'");
			}
		}

		// 由于已经进行了转发或包含操作，因此返回 null
		return null;
	}

	/**
	 * 确定是否使用 RequestDispatcher 的 {@code include} 或
	 * {@code forward} 方法。
	 * <p>执行检查是否在请求中找到包含 URI 属性，指示包含请求，以及响应是否已经提交。
	 * 在这两种情况下，将执行包含，因为转发不再可能。
	 *
	 * @param request  当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @return {@code true} 表示包含，{@code false} 表示转发
	 * @see javax.servlet.RequestDispatcher#forward
	 * @see javax.servlet.RequestDispatcher#include
	 * @see javax.servlet.ServletResponse#isCommitted
	 * @see org.springframework.web.util.WebUtils#isIncludeRequest
	 */
	protected boolean useInclude(HttpServletRequest request, HttpServletResponse response) {
		// 如果请求是 include 请求或响应已经提交，则返回 true，否则返回 false
		return (WebUtils.isIncludeRequest(request) || response.isCommitted());
	}

}
