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

package org.springframework.web.servlet.handler;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 适配器，用于将Servlet接口与通用的DispatcherServlet结合使用。
 * 调用Servlet的{@code service}方法来处理请求。
 *
 * <p>不明确支持最后修改检查：这通常由Servlet实现本身处理（通常派生自HttpServlet基类）。
 *
 * <p>此适配器默认未激活；需要在DispatcherServlet上下文中定义为bean。
 * 然后，它将自动应用于实现了Servlet接口的映射处理程序bean。
 *
 * <p>请注意，作为bean定义的Servlet实例将不会接收初始化和销毁回调，
 * 除非在DispatcherServlet上下文中定义了特殊的后处理器，
 * 例如SimpleServletPostProcessor。
 *
 * <p><b>或者，考虑使用Spring的ServletWrappingController包装Servlet。
 * </b>这对于现有的Servlet类特别合适，
 * 允许指定Servlet初始化参数等。
 *
 * @author Juergen Hoeller
 * @see javax.servlet.Servlet
 * @see javax.servlet.http.HttpServlet
 * @see SimpleServletPostProcessor
 * @see org.springframework.web.servlet.mvc.ServletWrappingController
 * @since 1.1.5
 */
public class SimpleServletHandlerAdapter implements HandlerAdapter {

	@Override
	public boolean supports(Object handler) {
		return (handler instanceof Servlet);
	}

	@Override
	@Nullable
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		((Servlet) handler).service(request, response);
		return null;
	}

	@Override
	@SuppressWarnings("deprecation")
	public long getLastModified(HttpServletRequest request, Object handler) {
		return -1;
	}

}
