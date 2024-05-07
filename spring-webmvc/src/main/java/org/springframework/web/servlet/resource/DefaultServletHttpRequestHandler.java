/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.resource;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用于使用Servlet容器的“默认”Servlet提供静态文件的{@link HttpRequestHandler}。
 *
 * <p>此处理程序旨在与“/*”映射一起使用，当{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
 * 映射到“/”时，覆盖Servlet容器对静态资源的默认处理。
 * 将此处理程序映射到链中的最后位置通常是有意义的，以便仅在无法匹配到更具体的映射（即，到控制器）时执行。
 *
 * <p>请求通过通过通过{@link RequestDispatcher}转发处理，通过{@link #setDefaultServletName “defaultServletName”属性指定的名称获取。
 * 在大多数情况下，不需要显式设置{@code defaultServletName}，因为处理程序在初始化时会检查是否存在默认Servlet，例如Tomcat、Jetty、Resin、WebLogic和WebSphere等常见容器。
 * 但是，当运行在不知道默认Servlet名称的容器中，或者通过服务器配置自定义了默认Servlet的名称时，需要显式设置{@code defaultServletName}。
 *
 * @author Jeremy Grelle
 * @author Juergen Hoeller
 * @since 3.0.4
 */
public class DefaultServletHttpRequestHandler implements HttpRequestHandler, ServletContextAware {

	/**
	 * 用于Tomcat、Jetty、JBoss和GlassFish的默认Servlet名称。
	 */
	private static final String COMMON_DEFAULT_SERVLET_NAME = "default";

	/**
	 * 用于Google App Engine的默认Servlet名称。
	 */
	private static final String GAE_DEFAULT_SERVLET_NAME = "_ah_default";

	/**
	 * 用于Resin的默认Servlet名称。
	 */
	private static final String RESIN_DEFAULT_SERVLET_NAME = "resin-file";

	/**
	 * 用于WebLogic的默认Servlet名称。
	 */
	private static final String WEBLOGIC_DEFAULT_SERVLET_NAME = "FileServlet";

	/**
	 * 用于WebSphere的默认Servlet名称。
	 */
	private static final String WEBSPHERE_DEFAULT_SERVLET_NAME = "SimpleFileServlet";


	/**
	 * 默认的Servlet名称
	 */
	@Nullable
	private String defaultServletName;

	/**
	 * Servlet上下文
	 */
	@Nullable
	private ServletContext servletContext;


	/**
	 * 设置用于静态资源请求转发到的默认Servlet的名称。
	 */
	public void setDefaultServletName(String defaultServletName) {
		this.defaultServletName = defaultServletName;
	}

	/**
	 * 如果尚未显式设置{@code defaultServletName}属性，则尝试使用已知的常见容器特定名称查找默认Servlet。
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		// 设置ServletContext并初始化默认的Servlet名称
		this.servletContext = servletContext;
		if (!StringUtils.hasText(this.defaultServletName)) {
			// 如果默认Servlet名称未设置，则尝试查找常见的默认Servlet名称并设置
			if (this.servletContext.getNamedDispatcher(COMMON_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = COMMON_DEFAULT_SERVLET_NAME;
			} else if (this.servletContext.getNamedDispatcher(GAE_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = GAE_DEFAULT_SERVLET_NAME;
			} else if (this.servletContext.getNamedDispatcher(RESIN_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = RESIN_DEFAULT_SERVLET_NAME;
			} else if (this.servletContext.getNamedDispatcher(WEBLOGIC_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = WEBLOGIC_DEFAULT_SERVLET_NAME;
			} else if (this.servletContext.getNamedDispatcher(WEBSPHERE_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = WEBSPHERE_DEFAULT_SERVLET_NAME;
			} else {
				// 如果无法找到常见的默认Servlet名称，则抛出异常
				throw new IllegalStateException("Unable to locate the default servlet for serving static content. " +
						"Please set the 'defaultServletName' property explicitly.");
			}
		}
	}


	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Assert.state(this.servletContext != null, "No ServletContext set");
		// 根据默认的Servlet名称获取请求分发器
		RequestDispatcher rd = this.servletContext.getNamedDispatcher(this.defaultServletName);
		if (rd == null) {
			// 如果请求分发器为空，则抛出异常
			throw new IllegalStateException("A RequestDispatcher could not be located for the default servlet '" +
					this.defaultServletName + "'");
		}
		// 分发请求
		rd.forward(request, response);
	}

}
