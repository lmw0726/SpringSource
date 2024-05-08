/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Spring 控制器实现，它包装了一个它内部管理的 servlet 实例。这样包装的 servlet 在此控制器之外是不可知的；
 * 其整个生命周期都在这里进行管理（与 ServletForwardingController 相反）。
 *
 * <p>通过 Spring 的调度基础结构来调用现有的 servlet 是很有用的，例如应用 Spring HandlerInterceptors 到其请求。
 *
 * <p>请注意，Struts 有一个特殊要求，即它会解析 {@code web.xml} 来查找其 servlet 映射。
 * 因此，您需要在此控制器上指定 DispatcherServlet 的 servlet 名称为 "servletName"，
 * 这样 Struts 才能找到 DispatcherServlet 的映射（认为它是指向 ActionServlet）。
 *
 * <p><b>示例：</b> DispatcherServlet XML 上下文，将 "*.do" 转发给由 ServletWrappingController 包装的 Struts
 * ActionServlet。所有这样的请求都将通过配置的 HandlerInterceptor 链（例如 OpenSessionInViewInterceptor）。
 * 从 Struts 的角度来看，一切都和往常一样工作。
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
 *       &lt;prop key="*.do"&gt;strutsWrappingController&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="strutsWrappingController" class="org.springframework.web.servlet.mvc.ServletWrappingController"&gt;
 *   &lt;property name="servletClass"&gt;
 *     &lt;value&gt;org.apache.struts.action.ActionServlet&lt;/value&gt;
 *   &lt;/property&gt;
 *   &lt;property name="servletName"&gt;
 *     &lt;value&gt;action&lt;/value&gt;
 *   &lt;/property&gt;
 *   &lt;property name="initParameters"&gt;
 *     &lt;props&gt;
 *       &lt;prop key="config"&gt;/WEB-INF/struts-config.xml&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @see ServletForwardingController
 * @since 1.1.1
 */
public class ServletWrappingController extends AbstractController
		implements BeanNameAware, InitializingBean, DisposableBean {

	/**
	 * Servlet类型
	 */
	@Nullable
	private Class<? extends Servlet> servletClass;

	/**
	 * Servlet名称
	 */
	@Nullable
	private String servletName;

	/**
	 * 初始化参数
	 */
	private Properties initParameters = new Properties();

	/**
	 * bean名称
	 */
	@Nullable
	private String beanName;

	/**
	 * Servlet实例
	 */
	@Nullable
	private Servlet servletInstance;


	public ServletWrappingController() {
		super(false);
	}


	/**
	 * 设置要包装的 servlet 的类。
	 * 必须实现 {@code javax.servlet.Servlet}。
	 *
	 * @see javax.servlet.Servlet
	 */
	public void setServletClass(Class<? extends Servlet> servletClass) {
		this.servletClass = servletClass;
	}

	/**
	 * 设置要包装的 servlet 的名称。
	 * 默认是此控制器的 bean 名称。
	 */
	public void setServletName(String servletName) {
		this.servletName = servletName;
	}

	/**
	 * 指定要包装的 servlet 的初始化参数，作为名称-值对。
	 */
	public void setInitParameters(Properties initParameters) {
		this.initParameters = initParameters;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	/**
	 * 初始化包装的 Servlet 实例。
	 *
	 * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		// 检查是否设置了 Servlet类型
		if (this.servletClass == null) {
			throw new IllegalArgumentException("'servletClass' is required");
		}

		// 如果未设置 Servlet名称，则使用 bean名称
		if (this.servletName == null) {
			this.servletName = this.beanName;
		}

		// 使用反射创建 Servlet 实例，
		this.servletInstance = ReflectionUtils.accessibleConstructor(this.servletClass).newInstance();
		// 调用Servlet初始化方法
		this.servletInstance.init(new DelegatingServletConfig());
	}


	/**
	 * 调用包装的 Servlet 实例。
	 *
	 * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
	 */
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		Assert.state(this.servletInstance != null, "No Servlet instance");
		// 调用Servlet的服务方法
		this.servletInstance.service(request, response);
		return null;
	}


	/**
	 * 销毁包装的 Servlet 实例。
	 *
	 * @see javax.servlet.Servlet#destroy()
	 */
	@Override
	public void destroy() {
		if (this.servletInstance != null) {
			// Servlet实例存在，则调用它的销毁方法
			this.servletInstance.destroy();
		}
	}


	/**
	 * ServletConfig 接口的内部实现，传递给包装的 servlet。委托给 ServletWrappingController 字段和方法，
	 * 以提供初始化参数和其他环境信息。
	 */
	private class DelegatingServletConfig implements ServletConfig {

		@Override
		@Nullable
		public String getServletName() {
			return servletName;
		}

		@Override
		@Nullable
		public ServletContext getServletContext() {
			return ServletWrappingController.this.getServletContext();
		}

		@Override
		public String getInitParameter(String paramName) {
			return initParameters.getProperty(paramName);
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public Enumeration<String> getInitParameterNames() {
			return (Enumeration) initParameters.keys();
		}
	}

}
