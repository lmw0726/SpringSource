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

package org.springframework.web.servlet;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * MVC框架SPI，允许对核心MVC工作流进行参数化。
 *
 * <p>必须为每种处理程序类型实现的接口，以处理请求。此接口用于允许 {@link DispatcherServlet} 无限扩展。
 * {@code DispatcherServlet} 通过此接口访问所有安装的处理程序，这意味着它不包含特定于任何处理程序类型的代码。
 *
 * <p>请注意，处理程序可以是类型为 {@code Object} 的。这是为了使其他框架的处理程序能够在不进行自定义编码的情况下与此框架集成，
 * 以及允许不遵循任何特定Java接口的基于注解的处理程序对象。
 *
 * <p>此接口不适用于应用程序开发人员。它提供给想要开发自己的Web工作流的处理程序。
 *
 * <p>注意: {@code HandlerAdapter} 实现者可以实现 {@link org.springframework.core.Ordered} 接口，
 * 以能够指定排序顺序（因此也是优先级），以便 {@code DispatcherServlet} 应用。
 * 非有序实例将被视为最低优先级。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter
 * @see org.springframework.web.servlet.handler.SimpleServletHandlerAdapter
 */
public interface HandlerAdapter {

	/**
	 * 给定一个处理程序实例，返回此 {@code HandlerAdapter} 是否可以支持它。
	 * 典型的HandlerAdapters将基于处理程序类型进行决策。HandlerAdapters通常只支持一个处理程序类型。
	 * <p>典型的实现:
	 * <p>{@code
	 * return (handler instanceof MyHandler);
	 * }
	 *
	 * @param handler 要检查的处理程序对象
	 * @return 是否可以使用给定处理程序的对象
	 */
	boolean supports(Object handler);

	/**
	 * 使用给定的处理程序处理此请求。所需的工作流可能有很大的变化。
	 *
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  要使用的处理程序。此对象必须先前已经通过此接口的 {@code supports} 方法传递，
	 *                 该方法必须返回 {@code true}。
	 * @return ModelAndView对象，其中包含视图的名称和所需的模型数据；如果请求已直接处理，则为{@code null}
	 * @throws Exception 如果发生错误
	 */
	@Nullable
	ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;

	/**
	 * 与HttpServlet的 {@code getLastModified} 方法具有相同的契约。如果处理程序类中没有支持，则可以简单地返回-1。
	 *
	 * @param request 当前HTTP请求
	 * @param handler 要使用的处理程序
	 * @return 给定处理程序的lastModified值
	 * @deprecated 自5.3.9开始，与{@link org.springframework.web.servlet.mvc.LastModified}一起。
	 */
	@Deprecated
	long getLastModified(HttpServletRequest request, Object handler);

}
