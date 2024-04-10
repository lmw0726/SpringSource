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

package org.springframework.web.servlet.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.Conventions;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.AbstractContextLoaderInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FrameworkServlet;

import javax.servlet.*;
import javax.servlet.FilterRegistration.Dynamic;
import java.util.EnumSet;

/**
 * 基类{@link org.springframework.web.WebApplicationInitializer}的实现，用于在servlet上下文中注册{@link DispatcherServlet}。
 *
 * <p>大多数应用程序应考虑扩展Spring Java配置子类{@link AbstractAnnotationConfigDispatcherServletInitializer}。
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.2
 */
public abstract class AbstractDispatcherServletInitializer extends AbstractContextLoaderInitializer {

	/**
	 * 默认的servlet名称。可以通过覆盖{@link #getServletName}进行自定义。
	 */
	public static final String DEFAULT_SERVLET_NAME = "dispatcher";


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		// 调用父类启动的逻辑
		super.onStartup(servletContext);
		// 注册 DispacherServlt
		registerDispatcherServlet(servletContext);
	}

	/**
	 * 在给定的servlet上下文中注册{@link DispatcherServlet}。
	 * <p>此方法将使用{@link #getServletName()}返回的名称创建一个{@code DispatcherServlet}，
	 * 使用从{@link #createServletApplicationContext()}返回的应用程序上下文进行初始化，
	 * 并将其映射到从{@link #getServletMappings()}返回的模式。
	 * <p>可以通过覆盖{@link #customizeRegistration(ServletRegistration.Dynamic)}或{@link #createDispatcherServlet(WebApplicationContext)}来实现进一步的定制。
	 * @param servletContext 要注册servlet的上下文
	 */
	protected void registerDispatcherServlet(ServletContext servletContext) {
		String servletName = getServletName();
		Assert.hasLength(servletName, "getServletName() must not return null or empty");

		// 创建Servlet应用程序上下文
		WebApplicationContext servletAppContext = createServletApplicationContext();
		Assert.notNull(servletAppContext, "createServletApplicationContext() must not return null");

		// 创建DispatcherServlet
		FrameworkServlet dispatcherServlet = createDispatcherServlet(servletAppContext);
		Assert.notNull(dispatcherServlet, "createDispatcherServlet(WebApplicationContext) must not return null");
		// 设置Servlet应用程序上下文的初始化器
		dispatcherServlet.setContextInitializers(getServletApplicationContextInitializers());

		// 添加Servlet到Servlet上下文中
		ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, dispatcherServlet);
		if (registration == null) {
			// 如果注册失败，则抛出IllegalStateException异常
			throw new IllegalStateException("Failed to register servlet with name '" + servletName + "'. " +
					"Check if there is another servlet registered under the same name.");
		}

		// 设置加载顺序为1
		registration.setLoadOnStartup(1);
		// 添加映射路径
		registration.addMapping(getServletMappings());
		// 设置异步支持
		registration.setAsyncSupported(isAsyncSupported());

		// 获取Servlet过滤器
		Filter[] filters = getServletFilters();
		if (!ObjectUtils.isEmpty(filters)) {
			// 遍历过滤器数组，注册到Servlet上下文中
			for (Filter filter : filters) {
				registerServletFilter(servletContext, filter);
			}
		}

		// 定制注册信息
		customizeRegistration(registration);
	}

	/**
	 * 返回DispatcherServlet将被注册的名称。默认为DEFAULT_SERVLET_NAME。
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected String getServletName() {
		return DEFAULT_SERVLET_NAME;
	}

	/**
	 * 创建一个servlet应用程序上下文，以提供给DispatcherServlet。
	 * <p>返回的上下文将被委托给Spring的DispatcherServlet。因此，它通常包含控制器、视图解析器、区域解析器和其他与Web相关的bean。
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected abstract WebApplicationContext createServletApplicationContext();

	/**
	 * 使用指定的WebApplicationContext创建一个DispatcherServlet（或其他类型的FrameworkServlet派生的调度程序）。
	 * <p>注意：从4.2.3开始，这允许任何FrameworkServlet子类。以前，它坚持返回一个DispatcherServlet或其子类。
	 */
	protected FrameworkServlet createDispatcherServlet(WebApplicationContext servletAppContext) {
		return new DispatcherServlet(servletAppContext);
	}

	/**
	 * 指定要应用于创建DispatcherServlet的servlet特定应用程序上下文的应用程序上下文初始化器。
	 * @since 4.2
	 * @see #createServletApplicationContext()
	 * @see DispatcherServlet#setContextInitializers
	 * @see #getRootApplicationContextInitializers()
	 */
	@Nullable
	protected ApplicationContextInitializer<?>[] getServletApplicationContextInitializers() {
		return null;
	}

	/**
	 * 指定DispatcherServlet的Servlet映射（或多个映射）——例如"/"、"/app"等。
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected abstract String[] getServletMappings();

	/**
	 * 指定要添加并映射到DispatcherServlet的过滤器。
	 * @return 一个过滤器数组或null
	 * @see #registerServletFilter(ServletContext, Filter)
	 */
	@Nullable
	protected Filter[] getServletFilters() {
		return null;
	}

	/**
	 * 将给定的过滤器添加到ServletContext，并按照以下方式映射到DispatcherServlet：
	 * <ul>
	 * <li>根据其具体类型选择默认过滤器名称
	 * <li>根据{@link #isAsyncSupported() asyncSupported}的返回值设置{@code asyncSupported}标志
	 * <li>创建一个使用调度程序类型{@code REQUEST}、{@code FORWARD}、{@code INCLUDE}的过滤器映射，根据{@link #isAsyncSupported() asyncSupported}的返回值条件性地创建{@code ASYNC}
	 * </ul>
	 * <p>如果上述默认值不合适或不足够，请重写此方法，并直接使用ServletContext注册过滤器。
	 * @param servletContext 要向其注册过滤器的ServletContext
	 * @param filter 要注册的过滤器
	 * @return 过滤器注册
	 */
	protected FilterRegistration.Dynamic registerServletFilter(ServletContext servletContext, Filter filter) {
		String filterName = Conventions.getVariableName(filter);
		// 添加过滤器到Servlet上下文中
		Dynamic registration = servletContext.addFilter(filterName, filter);

		if (registration == null) {
			int counter = 0;
			while (registration == null) {
				if (counter == 100) {
					// 如果注册失败达到100次，则抛出IllegalStateException异常
					throw new IllegalStateException("Failed to register filter with name '" + filterName + "'. " +
							"Check if there is another filter registered under the same name.");
				}
				// 使用带计数器的名称尝试再次注册
				registration = servletContext.addFilter(filterName + "#" + counter, filter);
				counter++;
			}
		}

		// 设置异步支持
		registration.setAsyncSupported(isAsyncSupported());
		// 为DispatcherServlet名称添加过滤器映射
		registration.addMappingForServletNames(getDispatcherTypes(), false, getServletName());
		// 返回注册的过滤器
		return registration;
	}

	private EnumSet<DispatcherType> getDispatcherTypes() {
		return (isAsyncSupported() ?
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ASYNC) :
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE));
	}

	/**
	 * 控制DispatcherServlet和通过{@link #getServletFilters()}添加的所有过滤器的{@code asyncSupported}标志的单个位置。
	 * <p>默认值为"true"。
	 */
	protected boolean isAsyncSupported() {
		return true;
	}

	/**
	 * 在{@link #registerDispatcherServlet(ServletContext)}完成后，可选择执行进一步的注册自定义。
	 * @param registration 要定制的DispatcherServlet注册
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected void customizeRegistration(ServletRegistration.Dynamic registration) {
	}

}
