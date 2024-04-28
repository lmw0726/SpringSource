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
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HandlerInterceptor接口允许自定义处理器执行链。
 * 应用程序可以为某些处理器组注册任意数量的现有或自定义拦截器，
 * 以添加通用预处理行为，而无需修改每个处理器实现。
 *
 * <p>在适当的HandlerAdapter触发处理器本身的执行之前，
 * HandlerInterceptor会被调用。此机制可用于许多预处理方面，
 * 例如授权检查，或通用处理器行为，如更改语言或主题。
 * 它的主要目的是将重复的处理器代码分解出来。
 *
 * <p>在异步处理场景中，处理器可能在主线程退出渲染或调用{@code postHandle}和{@code afterCompletion}回调时，
 * 在单独的线程中执行。当并发处理器执行完成时，请求将被分派回来，以便继续渲染模型并调用此契约的所有方法。
 * 有关更多选项和详细信息，请参阅{@code org.springframework.web.servlet.AsyncHandlerInterceptor}。
 *
 * <p>通常，拦截器链是根据每个HandlerMapping bean定义的，共享其粒度。
 * 为了能够将特定的拦截器链应用于一组处理器，需要通过一个HandlerMapping bean将所需的处理器映射。
 * 拦截器本身是在应用程序上下文中作为bean定义的，通过其"interceptors"属性（在XML中：&lt;list&gt; &lt;ref&gt;）进行引用。
 *
 * <p>HandlerInterceptor基本上类似于Servlet过滤器，但与后者不同的是，它只允许自定义预处理，
 * 并具有禁止执行处理器本身的选项以及自定义后处理。过滤器功能更强大，例如它们允许交换传递给链的请求和响应对象。
 * 请注意，过滤器在web.xml中配置，而HandlerInterceptor在应用程序上下文中配置。
 *
 * <p>作为基本指南，细粒度的处理器相关预处理任务是HandlerInterceptor实现的候选对象，
 * 特别是分解出的通用处理器代码和授权检查。另一方面，过滤器非常适合于处理请求内容和视图内容，
 * 如多部分表单和GZIP压缩。这通常在需要将过滤器映射到某些内容类型（例如图像）或所有请求时显示出来。
 *
 * @author Juergen Hoeller
 * @see HandlerExecutionChain#getInterceptors
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#setInterceptors
 * @see org.springframework.web.servlet.handler.UserRoleAuthorizationInterceptor
 * @see org.springframework.web.servlet.i18n.LocaleChangeInterceptor
 * @see org.springframework.web.servlet.theme.ThemeChangeInterceptor
 * @see javax.servlet.Filter
 * @since 20.06.2003
 */
public interface HandlerInterceptor {

	/**
	 * 在处理器执行之前调用拦截点。
	 * 在HandlerMapping确定适当的处理器对象之后，但在HandlerAdapter调用处理器之前调用。
	 * <p>DispatcherServlet使用执行链处理处理器，执行链由任意数量的拦截器组成，处理器本身位于链的末尾。
	 * 通过此方法，每个拦截器都可以决定中止执行链，通常发送HTTP错误或编写自定义响应。
	 * <p><strong>注意：</strong>异步请求处理有特殊注意事项。
	 * 有关更多详细信息，请参阅{@link org.springframework.web.servlet.AsyncHandlerInterceptor}。
	 * <p>默认实现返回{@code true}。
	 *
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  要执行的选定处理器，用于类型和/或实例评估
	 * @return 如果执行链应继续执行下一个拦截器或处理器本身，则返回{@code true}。
	 * 否则，DispatcherServlet假定此拦截器已自行处理响应。
	 * @throws Exception 出现错误时抛出异常
	 */
	default boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return true;
	}

	/**
	 * 成功执行处理器后的拦截点。
	 * 在HandlerAdapter实际调用处理器之后，但在DispatcherServlet呈现视图之前调用。
	 * 可以通过给定的ModelAndView向视图提供额外的模型对象。
	 * <p>DispatcherServlet使用执行链处理处理器，执行链由任意数量的拦截器组成，处理器本身位于链的末尾。
	 * 通过此方法，每个拦截器都可以对执行进行后处理，按照执行链的逆序应用。
	 * <p><strong>注意：</strong>异步请求处理有特殊注意事项。
	 * 有关更多详细信息，请参阅{@link org.springframework.web.servlet.AsyncHandlerInterceptor}。
	 * <p>默认实现为空。
	 *
	 * @param request      当前HTTP请求
	 * @param response     当前HTTP响应
	 * @param handler      启动异步执行的处理器（或{@link HandlerMethod}），用于类型和/或实例检查
	 * @param modelAndView 处理器返回的{@code ModelAndView}（也可以为{@code null}）
	 * @throws Exception 出现错误时抛出异常
	 */
	default void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
							@Nullable ModelAndView modelAndView) throws Exception {
	}

	/**
	 * 请求处理完成后的回调，即在呈现视图之后。
	 * 无论处理器执行的结果如何，都将调用此方法，从而允许适当的资源清理。
	 * <p>注意：只有在拦截器的{@code preHandle}方法成功完成并返回{@code true}时才会调用！
	 * <p>与{@code postHandle}方法一样，该方法将在执行链中的每个拦截器上按逆序调用，
	 * 因此第一个拦截器将是最后被调用的。
	 * <p><strong>注意：</strong>异步请求处理有特殊注意事项。
	 * 有关更多详细信息，请参阅{@link org.springframework.web.servlet.AsyncHandlerInterceptor}。
	 * <p>默认实现为空。
	 *
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  启动异步执行的处理器（或{@link HandlerMethod}），用于类型和/或实例检查
	 * @param ex       处理器执行过程中抛出的任何异常；这不包括通过异常解析器处理的异常
	 * @throws Exception 出现错误时抛出异常
	 */
	default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
								 @Nullable Exception ex) throws Exception {
	}

}
