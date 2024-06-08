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

package org.springframework.web.context.support;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 简单的 HttpServlet，委托给在 Spring 的根 Web 应用程序上下文中定义的 {@link HttpRequestHandler} bean。
 * 目标 bean 的名称必须与 HttpRequestHandlerServlet 在 {@code web.xml} 中定义的 servlet-name 匹配。
 *
 * <p>这可以用于例如暴露单个 Spring 远程导出器，例如 {@link org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter}
 * 或 {@link org.springframework.remoting.caucho.HessianServiceExporter}，每个 HttpRequestHandlerServlet 定义一个导出器。
 * 这是在 DispatcherServlet 上下文中定义远程导出器作为 bean 的一个最小替代方法
 * （在那里提供了高级的映射和拦截功能）。
 *
 * @author Juergen Hoeller
 * @see org.springframework.web.HttpRequestHandler
 * @see org.springframework.web.servlet.DispatcherServlet
 * @since 2.0
 */
@SuppressWarnings("serial")
public class HttpRequestHandlerServlet extends HttpServlet {
	/**
	 * Http请求处理器
	 */
	@Nullable
	private HttpRequestHandler target;


	@Override
	public void init() throws ServletException {
		// 获取 Web应用程序上下文
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		// 获取对应 Servlet 名称的 Http请求处理器 对象
		this.target = wac.getBean(getServletName(), HttpRequestHandler.class);
	}


	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Assert.state(this.target != null, "No HttpRequestHandler available");

		// 设置当前线程的 区域设置上下文 为请求的 Locale
		LocaleContextHolder.setLocale(request.getLocale());
		try {
			// 尝试处理请求
			this.target.handleRequest(request, response);
		} catch (HttpRequestMethodNotSupportedException ex) {
			// 如果请求方法不支持
			String[] supportedMethods = ex.getSupportedMethods();
			if (supportedMethods != null) {
				// 设置 Allow 响应头
				response.setHeader("Allow", StringUtils.arrayToDelimitedString(supportedMethods, ", "));
			}
			// 发送方法不允许的错误响应
			response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, ex.getMessage());
		} finally {
			// 重置当前线程的 LocaleContext
			LocaleContextHolder.resetLocaleContext();
		}
	}

}
