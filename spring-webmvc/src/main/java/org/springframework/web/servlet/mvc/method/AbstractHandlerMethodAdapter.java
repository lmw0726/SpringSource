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

package org.springframework.web.servlet.mvc.method;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 支持{@link HandlerMethod}类型处理程序的{@link HandlerAdapter}实现的抽象基类。
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public abstract class AbstractHandlerMethodAdapter extends WebContentGenerator implements HandlerAdapter, Ordered {

	/**
	 * 排序值
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;


	public AbstractHandlerMethodAdapter() {
		// 默认情况下不限制HTTP方法
		super(false);
	}


	/**
	 * 指定此HandlerAdapter bean的排序值。
	 * <p>默认值为{@code Ordered.LOWEST_PRECEDENCE}，表示非排序。
	 *
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * 此实现期望处理程序是一个{@link HandlerMethod}。
	 *
	 * @param handler 要检查的处理程序实例
	 * @return 此适配器是否可以适配给定的处理程序
	 */
	@Override
	public final boolean supports(Object handler) {
		return (handler instanceof HandlerMethod && supportsInternal((HandlerMethod) handler));
	}

	/**
	 * 给定处理程序方法，返回此适配器是否可以支持它。
	 *
	 * @param handlerMethod 要检查的处理程序方法
	 * @return 此适配器是否可以适应给定方法
	 */
	protected abstract boolean supportsInternal(HandlerMethod handlerMethod);

	/**
	 * 此实现期望处理程序是一个{@link HandlerMethod}。
	 */
	@Override
	@Nullable
	public final ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return handleInternal(request, response, (HandlerMethod) handler);
	}

	/**
	 * 使用给定的处理程序方法处理请求。
	 *
	 * @param request       当前HTTP请求
	 * @param response      当前HTTP响应
	 * @param handlerMethod 要使用的处理程序方法。此对象必须先前已传递给{@link #supportsInternal(HandlerMethod)}此接口，
	 *                      该接口必须返回{@code true}。
	 * @return 一个包含视图名称和所需模型数据的ModelAndView对象，
	 * 如果请求已直接处理，则返回{@code null}
	 * @throws Exception 发生错误时
	 */
	@Nullable
	protected abstract ModelAndView handleInternal(HttpServletRequest request,
												   HttpServletResponse response, HandlerMethod handlerMethod) throws Exception;

	/**
	 * 此实现期望处理程序是一个{@link HandlerMethod}。
	 */
	@Override
	@SuppressWarnings("deprecation")
	public final long getLastModified(HttpServletRequest request, Object handler) {
		return getLastModifiedInternal(request, (HandlerMethod) handler);
	}

	/**
	 * 与{@link javax.servlet.http.HttpServlet#getLastModified(HttpServletRequest)}具有相同的契约。
	 *
	 * @param request       当前HTTP请求
	 * @param handlerMethod 要使用的处理程序方法
	 * @return 给定处理程序的lastModified值
	 * @deprecated 自5.3.9起，与{@link org.springframework.web.servlet.mvc.LastModified}一起。
	 */
	@Deprecated
	protected abstract long getLastModifiedInternal(HttpServletRequest request, HandlerMethod handlerMethod);

}
