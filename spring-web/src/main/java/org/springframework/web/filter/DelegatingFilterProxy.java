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

package org.springframework.web.filter;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import java.io.IOException;

/**
 * 代理标准 Servlet Filter，委托给实现 Filter 接口的 Spring 管理的 bean。
 * 在 {@code web.xml} 中支持一个名为 "targetBeanName" 的 filter init-param，
 * 指定 Spring 应用程序上下文中的目标 bean 的名称。
 *
 * <p>{@code web.xml} 通常会包含一个 {@code DelegatingFilterProxy} 定义，
 * 指定的 {@code filter-name} 对应于 Spring 根应用程序上下文中的一个 bean 名称。
 * 所有对过滤器代理的调用都将委托给 Spring 上下文中的那个 bean，
 * 该 bean 必须实现标准的 Servlet Filter 接口。
 *
 * <p>此方法对于需要复杂设置的 Filter 实现特别有用，允许将完整的 Spring bean 定义机制应用于 Filter 实例。
 * 或者，考虑将标准的 Filter 设置与从 Spring 根应用程序上下文中查找服务 bean 结合使用。
 *
 * <p><b>注意:</b> Servlet Filter 接口定义的生命周期方法默认情况下 <i>不会</i> 委托给目标 bean，
 * 依赖于 Spring 应用程序上下文来管理该 bean 的生命周期。
 * 指定 "targetFilterLifecycle" filter init-param 为 "true" 将强制在目标 bean 上调用 {@code Filter.init} 和 {@code Filter.destroy} 生命周期方法，
 * 让 servlet 容器管理过滤器生命周期。
 *
 * <p>从 Spring 3.1 开始，{@code DelegatingFilterProxy} 已更新为在使用 Servlet 容器的基于实例的过滤器注册方法时，
 * 可选地接受构造函数参数，通常与 Spring 的 {@link org.springframework.web.WebApplicationInitializer} SPI 结合使用。
 * 这些构造函数允许直接提供委托 Filter bean，或者提供应用程序上下文和要提取的 bean 名称，避免了从 ServletContext 查找应用程序上下文的需要。
 *
 * <p>该类最初受到 Spring Security 的 {@code FilterToBeanProxy} 类的启发，由 Ben Alex 编写。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @see #setTargetBeanName
 * @see #setTargetFilterLifecycle
 * @see javax.servlet.Filter#doFilter
 * @see javax.servlet.Filter#init
 * @see javax.servlet.Filter#destroy
 * @see #DelegatingFilterProxy(Filter)
 * @see #DelegatingFilterProxy(String)
 * @see #DelegatingFilterProxy(String, WebApplicationContext)
 * @see javax.servlet.ServletContext#addFilter(String, Filter)
 * @see org.springframework.web.WebApplicationInitializer
 * @since 1.2
 */
public class DelegatingFilterProxy extends GenericFilterBean {
	/**
	 * 上下文属性名称
	 */
	@Nullable
	private String contextAttribute;

	/**
	 * Web应用上下文
	 */
	@Nullable
	private WebApplicationContext webApplicationContext;

	/**
	 * 目标bean名称
	 */
	@Nullable
	private String targetBeanName;

	/**
	 * 目标过滤器是否调用生命周期的方法
	 */
	private boolean targetFilterLifecycle = false;

	/**
	 * 代理的过滤器
	 */
	@Nullable
	private volatile Filter delegate;

	/**
	 * 代理监控器
	 */
	private final Object delegateMonitor = new Object();


	/**
	 * 创建一个新的 {@code DelegatingFilterProxy}。用于传统的 {@code web.xml}。
	 *
	 * @see #setTargetBeanName(String)
	 */
	public DelegatingFilterProxy() {
	}

	/**
	 * 创建一个新的 {@code DelegatingFilterProxy}，带有给定的 {@link Filter} 委托。
	 * 完全绕过与 Spring 应用程序上下文交互的需要，指定 {@linkplain #setTargetBeanName 目标 bean 名称} 等。
	 * <p>用于实例化注册过滤器。
	 *
	 * @param delegate 该代理将委托给并管理生命周期的 {@code Filter} 实例 (不能为 {@code null})。
	 * @see #doFilter(ServletRequest, ServletResponse, FilterChain)
	 * @see #invokeDelegate(Filter, ServletRequest, ServletResponse, FilterChain)
	 * @see #destroy()
	 * @see #setEnvironment(org.springframework.core.env.Environment)
	 */
	public DelegatingFilterProxy(Filter delegate) {
		Assert.notNull(delegate, "Delegate Filter must not be null");
		this.delegate = delegate;
	}

	/**
	 * 创建一个新的 {@code DelegatingFilterProxy}，将从 {@code ServletContext} 中的 Spring {@code WebApplicationContext} 中找到的命名目标 bean。
	 * 用于实例化注册过滤器。
	 * <p>目标 bean 必须实现标准的 Servlet Filter 接口。
	 *
	 * @param targetBeanName 要在 Spring 应用程序上下文中查找的目标过滤器 bean 的名称 (不能为 {@code null})。
	 * @see #findWebApplicationContext()
	 * @see #setEnvironment(org.springframework.core.env.Environment)
	 */
	public DelegatingFilterProxy(String targetBeanName) {
		this(targetBeanName, null);
	}

	/**
	 * 创建一个新的 {@code DelegatingFilterProxy}，将从给定的 Spring {@code WebApplicationContext} 中检索命名目标 bean。
	 * 用于实例化注册过滤器。
	 * <p>目标 bean 必须实现标准的 Servlet Filter 接口。
	 * <p>当传入时，给定的 {@code WebApplicationContext} 可能已经刷新，也可能没有。
	 * 如果它尚未刷新，并且上下文实现了 {@link ConfigurableApplicationContext}，那么在检索命名的目标 bean 之前，将尝试执行 {@link ConfigurableApplicationContext#refresh() refresh()}。
	 * <p>该代理的 {@code Environment} 将继承自给定的 {@code WebApplicationContext}。
	 *
	 * @param targetBeanName 在 Spring 应用程序上下文中的目标过滤器 bean 的名称 (不能为 {@code null})。
	 * @param wac            将检索目标过滤器的应用程序上下文；如果为 {@code null}，则会从 {@code ServletContext} 中查找应用程序上下文作为后备。
	 * @see #findWebApplicationContext()
	 * @see #setEnvironment(org.springframework.core.env.Environment)
	 */
	public DelegatingFilterProxy(String targetBeanName, @Nullable WebApplicationContext wac) {
		Assert.hasText(targetBeanName, "Target Filter bean name must not be null or empty");
		// 设置目标bean名称
		this.setTargetBeanName(targetBeanName);
		this.webApplicationContext = wac;
		if (wac != null) {
			// 如果Web上下文不为空，则从Web上下文中获取并设置环境
			this.setEnvironment(wac.getEnvironment());
		}
	}

	/**
	 * 设置应该用于检索 {@link WebApplicationContext} 的 ServletContext 属性的名称，
	 * 该 ServletContext 属性用于加载代理 {@link Filter} bean。
	 */
	public void setContextAttribute(@Nullable String contextAttribute) {
		this.contextAttribute = contextAttribute;
	}

	/**
	 * 返回应该用于检索 {@link WebApplicationContext} 的 ServletContext 属性的名称，
	 * 该 ServletContext 属性用于加载代理 {@link Filter} bean。
	 */
	@Nullable
	public String getContextAttribute() {
		return this.contextAttribute;
	}

	/**
	 * 设置 Spring 应用程序上下文中目标 bean 的名称。
	 * 目标 bean 必须实现标准的 Servlet Filter 接口。
	 * <p>默认情况下，将使用 {@code web.xml} 中为 DelegatingFilterProxy 指定的 {@code filter-name}。
	 */
	public void setTargetBeanName(@Nullable String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	/**
	 * 返回 Spring 应用程序上下文中目标 bean 的名称。
	 */
	@Nullable
	protected String getTargetBeanName() {
		return this.targetBeanName;
	}

	/**
	 * 设置是否在目标 bean 上调用 {@code Filter.init} 和 {@code Filter.destroy} 生命周期方法。
	 * <p>默认值为 "false"；目标 bean 通常依赖于 Spring 应用程序上下文来管理其生命周期。
	 * 将此标志设置为 "true" 表示 servlet 容器将控制目标 Filter 的生命周期，而此代理将委托相应的调用。
	 */
	public void setTargetFilterLifecycle(boolean targetFilterLifecycle) {
		this.targetFilterLifecycle = targetFilterLifecycle;
	}

	/**
	 * 返回是否在目标 bean 上调用 {@code Filter.init} 和 {@code Filter.destroy} 生命周期方法。
	 */
	protected boolean isTargetFilterLifecycle() {
		return this.targetFilterLifecycle;
	}


	@Override
	protected void initFilterBean() throws ServletException {
		// 使用委托监视器对象进行同步，确保线程安全
		synchronized (this.delegateMonitor) {
			// 如果委托对象为 null
			if (this.delegate == null) {
				// 如果未指定目标 bean 名称，则使用过滤器的名称作为目标 bean 名称
				if (this.targetBeanName == null) {
					this.targetBeanName = getFilterName();
				}
				// 获取 Spring 根应用程序上下文并尽早初始化委托对象，如果可能的话。
				// 如果根应用程序上下文在此过滤器代理之后启动，我们将不得不使用延迟初始化。
				WebApplicationContext wac = findWebApplicationContext();
				// 如果找到了 Web应用上下文
				if (wac != null) {
					// 初始化委托对象
					this.delegate = initDelegate(wac);
				}
			}
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 如果需要的话，延迟初始化委托对象。
		Filter delegateToUse = this.delegate;
		if (delegateToUse == null) {
			// 使用委托监视器对象进行同步
			synchronized (this.delegateMonitor) {
				// 再次检查委托对象是否为 null，确保线程安全
				delegateToUse = this.delegate;
				if (delegateToUse == null) {
					// 获取 Spring 根应用程序上下文
					WebApplicationContext wac = findWebApplicationContext();
					// 如果未找到 Web应用上下文，则抛出异常
					if (wac == null) {
						throw new IllegalStateException("No WebApplicationContext found: " +
								"no ContextLoaderListener or DispatcherServlet registered?");
					}
					// 初始化委托对象
					delegateToUse = initDelegate(wac);
					// 将委托对象赋值给实例变量
					this.delegate = delegateToUse;
				}
			}
		}

		// 让委托对象执行实际的 doFilter 操作。
		invokeDelegate(delegateToUse, request, response, filterChain);
	}

	@Override
	public void destroy() {
		Filter delegateToUse = this.delegate;
		if (delegateToUse != null) {
			// 如果要使用的代理过滤器不为空，则销毁代理过滤器
			destroyDelegate(delegateToUse);
		}
	}


	/**
	 * 返回在构造时传入的 {@code WebApplicationContext}（如果可用）。
	 * 否则，尝试从具有已配置名称的 {@code ServletContext} 属性中检索 {@code WebApplicationContext}。
	 * 否则，在已知的 "root" 应用程序上下文属性下查找 {@code WebApplicationContext}。
	 * 在此过滤器初始化（或调用）之前，{@code WebApplicationContext} 必须已经加载并存储在 {@code ServletContext} 中。
	 * <p>子类可以重写此方法以提供不同的 {@code WebApplicationContext} 检索策略。
	 *
	 * @return 此代理的 {@code WebApplicationContext}，如果找不到，则为 {@code null}
	 * @see #DelegatingFilterProxy(String, WebApplicationContext)
	 * @see #getContextAttribute()
	 * @see WebApplicationContextUtils#getWebApplicationContext(javax.servlet.ServletContext)
	 * @see WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	@Nullable
	protected WebApplicationContext findWebApplicationContext() {
		// 如果已经注入了 Web应用上下文
		if (this.webApplicationContext != null) {
			// 用户在构造函数中已经注入了上下文 -> 使用它...
			if (this.webApplicationContext instanceof ConfigurableApplicationContext) {
				// 如果上下文是 ConfigurableApplicationContext 类型的实例
				ConfigurableApplicationContext cac = (ConfigurableApplicationContext) this.webApplicationContext;
				// 如果上下文尚未激活
				if (!cac.isActive()) {
					// 上下文尚未刷新 -> 在返回之前刷新它...
					cac.refresh();
				}
			}
			// 返回注入的 Web应用上下文
			return this.webApplicationContext;
		}

		// 如果没有注入 Web应用上下文，则尝试从 Servlet上下文 中获取
		String attrName = getContextAttribute();
		if (attrName != null) {
			// 如果指定了上下文属性名称，则使用其获取 Web应用上下文
			return WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
		} else {
			// 否则尝试使用默认的方式查找 Web应用上下文
			return WebApplicationContextUtils.findWebApplicationContext(getServletContext());
		}
	}

	/**
	 * 初始化在给定 Spring 应用程序上下文中定义的 Filter 代理。
	 * <p>默认实现从应用程序上下文中获取 bean，并在 Filter 代理的 FilterConfig 上调用标准的 {@code Filter.init} 方法。
	 *
	 * @param wac 根应用程序上下文
	 * @return 初始化的代理 Filter
	 * @throws ServletException 如果 Filter 抛出异常
	 * @see #getTargetBeanName()
	 * @see #isTargetFilterLifecycle()
	 * @see #getFilterConfig()
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	protected Filter initDelegate(WebApplicationContext wac) throws ServletException {
		// 获取目标 bean 的名称
		String targetBeanName = getTargetBeanName();
		// 断言目标 bean 名称不为空
		Assert.state(targetBeanName != null, "No target bean name set");
		// 从 Web应用上下文 中获取目标 bean 的实例
		Filter delegate = wac.getBean(targetBeanName, Filter.class);
		// 如果需要目标过滤器的生命周期管理
		if (isTargetFilterLifecycle()) {
			// 调用目标过滤器的 init 方法进行初始化
			delegate.init(getFilterConfig());
		}
		// 返回目标过滤器的实例
		return delegate;
	}

	/**
	 * 实际调用给定请求和响应的代理 Filter。
	 *
	 * @param delegate    代理 Filter
	 * @param request     当前 HTTP 请求
	 * @param response    当前 HTTP 响应
	 * @param filterChain 当前 FilterChain
	 * @throws ServletException 如果 Filter 抛出异常
	 * @throws IOException      如果 Filter 抛出异常
	 */
	protected void invokeDelegate(
			Filter delegate, ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		delegate.doFilter(request, response, filterChain);
	}

	/**
	 * 销毁 Filter 代理。
	 * 默认实现简单地调用 {@code Filter.destroy}。
	 *
	 * @param delegate 代理 Filter（永远不会是 {@code null}）
	 * @see #isTargetFilterLifecycle()
	 * @see javax.servlet.Filter#destroy()
	 */
	protected void destroyDelegate(Filter delegate) {
		// 如果需要目标过滤器的生命周期管理
		if (isTargetFilterLifecycle()) {
			// 调用目标过滤器的 destroy 方法进行销毁
			delegate.destroy();
		}
	}

}
