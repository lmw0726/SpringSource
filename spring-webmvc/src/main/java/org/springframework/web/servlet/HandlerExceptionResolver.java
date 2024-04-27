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

package org.springframework.web.servlet;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 实现了能够解析在处理程序映射或执行过程中抛出的异常的对象的接口，典型情况下用于错误视图。
 * 实现者通常作为bean注册在应用程序上下文中。
 *
 * <p>错误视图类似于JSP错误页面，但可以与任何类型的异常一起使用，包括任何已检查异常，
 * 还可以为特定处理程序提供潜在的细粒度映射。
 *
 * @author Juergen Hoeller
 * @since 22.11.2003
 */
public interface HandlerExceptionResolver {

	/**
	 * 尝试解析在处理程序执行过程中抛出的给定异常，
	 * 如果适用，则返回表示特定错误页面的{@link ModelAndView}。
	 * <p>返回的{@code ModelAndView}可能会{@linkplain ModelAndView#isEmpty() empty}，
	 * 以指示异常已成功解析，
	 * 但不应渲染任何视图，例如通过设置状态代码。
	 *
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  执行的处理程序，如果在异常抛出时没有选择任何处理程序
	 *                 （例如，如果multipart解析失败），则为{@code null}
	 * @param ex       在处理程序执行期间抛出的异常
	 * @return 相应的{@code ModelAndView}以转发到，或者对于解析链中的默认处理，{@code null}
	 */
	@Nullable
	ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
								  @Nullable Object handler, Exception ex);

}
