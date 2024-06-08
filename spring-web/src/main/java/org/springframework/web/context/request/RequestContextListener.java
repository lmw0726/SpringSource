/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.context.request;

import org.springframework.context.i18n.LocaleContextHolder;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

/**
 * Servlet 监听器，通过 {@link org.springframework.context.i18n.LocaleContextHolder} 和
 * {@link RequestContextHolder} 将请求暴露给当前线程。要在 {@code web.xml} 中注册为监听器。
 *
 * <p>另外，Spring 的 {@link org.springframework.web.filter.RequestContextFilter} 和
 * Spring 的 {@link org.springframework.web.servlet.DispatcherServlet} 也会向当前线程暴露
 * 相同的请求上下文。与此监听器相比，那里提供了更多的高级选项（例如 "threadContextInheritable"）。
 *
 * <p>这个监听器主要用于与第三方的 Servlet（例如 JSF FacesServlet）一起使用。
 * 在 Spring 自己的 Web 支持中，DispatcherServlet 的处理完全足够了。
 *
 * @author Juergen Hoeller
 * @see javax.servlet.ServletRequestListener
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @see RequestContextHolder
 * @see org.springframework.web.filter.RequestContextFilter
 * @see org.springframework.web.servlet.DispatcherServlet
 * @since 2.0
 */
public class RequestContextListener implements ServletRequestListener {
	/**
	 * 请求属性属性名称
	 */
	private static final String REQUEST_ATTRIBUTES_ATTRIBUTE =
			RequestContextListener.class.getName() + ".REQUEST_ATTRIBUTES";


	@Override
	public void requestInitialized(ServletRequestEvent requestEvent) {
		// 检查请求事件中的 Servlet 请求对象是否是 HttpServletRequest 的实例
		if (!(requestEvent.getServletRequest() instanceof HttpServletRequest)) {
			// 如果不是则抛出异常
			throw new IllegalArgumentException(
					"Request is not an HttpServletRequest: " + requestEvent.getServletRequest());
		}
		// 将 Servlet 请求对象转换为 HttpServlet请求
		HttpServletRequest request = (HttpServletRequest) requestEvent.getServletRequest();
		// 创建 Servlet 请求属性对象
		ServletRequestAttributes attributes = new ServletRequestAttributes(request);
		// 将 Servlet 请求属性对象设置为请求属性
		request.setAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE, attributes);
		// 设置当前线程的区域上下文为请求的区域设置
		LocaleContextHolder.setLocale(request.getLocale());
		// 将 Servlet 请求属性对象设置为当前请求的属性
		RequestContextHolder.setRequestAttributes(attributes);
	}

	@Override
	public void requestDestroyed(ServletRequestEvent requestEvent) {
		// 初始化 Servlet 请求属性对象为 null
		ServletRequestAttributes attributes = null;
		// 获取请求事件中的 Servlet 请求对象的 请求属性
		Object reqAttr = requestEvent.getServletRequest().getAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE);
		// 如果请求属性对象是 Servlet请求属性 的实例
		if (reqAttr instanceof ServletRequestAttributes) {
			// 将请求属性对象转换为 Servlet 请求属性对象
			attributes = (ServletRequestAttributes) reqAttr;
		}
		// 获取当前线程的请求属性对象
		RequestAttributes threadAttributes = RequestContextHolder.getRequestAttributes();
		// 如果当前线程的请求属性对象不为空
		if (threadAttributes != null) {
			// 在原始请求线程内...
			// 重置区域上下文的区域设置
			LocaleContextHolder.resetLocaleContext();
			// 重置请求属性
			RequestContextHolder.resetRequestAttributes();
			// 如果 Servlet 请求属性对象为空，且当前线程的请求属性对象是 Servlet请求属性 的实例
			if (attributes == null && threadAttributes instanceof ServletRequestAttributes) {
				// 将当前线程的请求属性对象转换为 Servlet 请求属性对象
				attributes = (ServletRequestAttributes) threadAttributes;
			}
		}
		// 如果 Servlet 请求属性对象不为空
		if (attributes != null) {
			// 标记请求已完成
			attributes.requestCompleted();
		}
	}

}
