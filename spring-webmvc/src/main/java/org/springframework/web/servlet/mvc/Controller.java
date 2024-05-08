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

package org.springframework.web.servlet.mvc;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * BaseController 接口，表示一个组件，接收 HttpServletRequest 和 HttpServletResponse
 * 实例，就像一个 HttpServlet 一样，但能够参与 MVC 工作流程。控制器可与 Struts 的 Action 相比较。
 *
 * <p>Controller 接口的任何实现应该是一个可重用、线程安全的类，能够在应用程序的整个生命周期内处理多个 HTTP 请求。为了能够
 * 方便地配置控制器，鼓励控制器实现为（通常也是）JavaBeans。
 *
 * <h3><a name="workflow">工作流程</a></h3>
 *
 * <p>{@code DispatcherServlet} 在接收请求并解析区域设置、主题等内容后，会尝试解析控制器，使用
 * {@link org.springframework.web.servlet.HandlerMapping HandlerMapping}。
 * 当找到控制器来处理请求时，会调用已定位控制器的 {@link #handleRequest(HttpServletRequest, HttpServletResponse) handleRequest}
 * 方法；然后，已定位的控制器负责处理实际请求，如果适用，则返回适当的
 * {@link org.springframework.web.servlet.ModelAndView ModelAndView}。
 * 所以实际上，这个方法是 {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
 * 的主要入口点，它将请求委托给控制器。
 *
 * <p>因此，任何 {@code Controller} 接口的直接实现都只处理 HttpServletRequest，并应返回 ModelAndView，
 * 以供 DispatcherServlet 进一步解释。任何额外的功能，例如可选验证、表单处理等，应通过扩展
 * {@link org.springframework.web.servlet.mvc.AbstractController AbstractController} 或其子类来获取。
 *
 * <h3>设计和测试说明</h3>
 *
 * <p>Controller 接口明确设计为在 HttpServletRequest 和 HttpServletResponse 对象上操作，就像一个 HttpServlet 一样。
 * 与 WebWork、JSF 或 Tapestry 不同，它不旨在将自身与 Servlet API 解耦。相反，完整的 Servlet API 功能可用，
 * 允许控制器是通用的：控制器不仅能够处理 web 用户界面请求，还能够处理远程协议或根据需要生成报告。
 *
 * <p>控制器可以通过将 HttpServletRequest 和 HttpServletResponse 对象作为参数传递给
 * {@link #handleRequest(HttpServletRequest, HttpServletResponse) handleRequest} 方法来轻松测试。
 * 作为方便，Spring 提供了一组适用于测试任何类型的 Web 组件的 Servlet API 模拟对象，但特别适用于测试 Spring Web 控制器。
 * 与 Struts Action 不同，无需模拟 ActionServlet 或任何其他基础结构；模拟 HttpServletRequest 和 HttpServletResponse 就足够了。
 *
 * <p>如果控制器需要了解特定环境引用，它们可以选择实现特定的感知接口，就像 Spring（Web）应用程序上下文中的任何其他 bean 一样，例如：
 * <ul>
 * <li>{@code org.springframework.context.ApplicationContextAware}</li>
 * <li>{@code org.springframework.context.ResourceLoaderAware}</li>
 * <li>{@code org.springframework.web.context.ServletContextAware}</li>
 * </ul>
 *
 * <p>这些环境引用可以通过在相应感知接口中定义的相应 setter 在测试环境中轻松传递。通常建议尽量保持依赖关系尽可能少：
 * 例如，如果您只需要资源加载，只需实现 ResourceLoaderAware。或者，从 WebApplicationObjectSupport 基类派生，
 * 通过方便的访问器给您所有这些引用，但需要在初始化时提供 ApplicationContext 引用。
 *
 * <p>控制器可以在 {@link org.springframework.web.context.request.WebRequest} 上使用 {@code checkNotModified} 方法来支持 HTTP 缓存。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SimpleControllerHandlerAdapter
 * @see AbstractController
 * @see org.springframework.mock.web.MockHttpServletRequest
 * @see org.springframework.mock.web.MockHttpServletResponse
 * @see org.springframework.context.ApplicationContextAware
 * @see org.springframework.context.ResourceLoaderAware
 * @see org.springframework.web.context.ServletContextAware
 * @see org.springframework.web.context.support.WebApplicationObjectSupport
 */
@FunctionalInterface
public interface Controller {

	/**
	 * 处理请求并返回 ModelAndView 对象，DispatcherServlet 将对其进行呈现。返回 {@code null} 不是错误：它表示此对象已完成请求处理本身，
	 * 因此没有 ModelAndView 可呈现。
	 * @param request 当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @return 要呈现的 ModelAndView，如果直接处理则为 {@code null}
	 * @throws Exception 如果出现错误
	 */
	@Nullable
	ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception;

}
