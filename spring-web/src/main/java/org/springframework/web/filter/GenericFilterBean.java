/*
 * Copyright 2002-2022 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.*;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.web.util.NestedServletException;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link javax.servlet.Filter} 的简单基本实现，将其配置参数（在 {@code web.xml} 中的 {@code filter} 标签内的
 * {@code init-param} 条目）视为 bean 属性。
 *
 * <p>对于任何类型的过滤器都是一个方便的超类。配置参数的类型转换是自动进行的，相应的 setter 方法会使用转换后的值被调用。
 * 子类也可以指定必需的属性。没有匹配的 bean 属性 setter 的参数将被简单地忽略。
 *
 * <p>此过滤器将实际的过滤操作留给子类，子类必须实现 {@link javax.servlet.Filter#doFilter} 方法。
 *
 * <p>此通用过滤器基类不依赖于 Spring {@link org.springframework.context.ApplicationContext} 概念。
 * 过滤器通常不加载自己的上下文，而是从 Spring 根应用程序上下文中访问服务 bean，可通过过滤器的
 * {@link #getServletContext() ServletContext} 获取（参见 {@link org.springframework.web.context.support.WebApplicationContextUtils}）。
 *
 * @author Juergen Hoeller
 * @see #addRequiredProperty
 * @see #initFilterBean
 * @see #doFilter
 * @since 06.12.2003
 */
public abstract class GenericFilterBean implements Filter, BeanNameAware, EnvironmentAware,
		EnvironmentCapable, ServletContextAware, InitializingBean, DisposableBean {

	/**
	 * 子类可用的日志记录器。
	 */
	protected final Log logger = LogFactory.getLog(getClass());
	/**
	 * bean名称
	 */
	@Nullable
	private String beanName;

	/**
	 * 环境
	 */
	@Nullable
	private Environment environment;

	/**
	 * Servlet上下文
	 */
	@Nullable
	private ServletContext servletContext;

	/**
	 * 过滤器配置
	 */
	@Nullable
	private FilterConfig filterConfig;

	/**
	 * 必需的属性集合
	 */
	private final Set<String> requiredProperties = new HashSet<>(4);


	/**
	 * 将在 Spring bean 工厂中定义的 bean 名称存储起来。
	 * <p>仅在作为 bean 进行初始化时相关，以便有一个名称作为回退，通常由 FilterConfig 实例提供的过滤器名称。
	 *
	 * @see org.springframework.beans.factory.BeanNameAware
	 * @see #getFilterName()
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 设置此过滤器运行的 {@code Environment}。
	 * <p>此处设置的任何环境都会覆盖默认提供的 {@link StandardServletEnvironment}。
	 * <p>此 {@code Environment} 对象仅用于解析传递给此过滤器的初始化参数中的资源路径中的占位符。
	 * 如果没有使用 init-params，则此 {@code Environment} 可以基本上被忽略。
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * 返回与此过滤器关联的 {@link Environment}。
	 * <p>如果未指定任何环境，则将通过 {@link #createEnvironment()} 初始化默认环境。
	 *
	 * @since 4.3.9
	 */
	@Override
	public Environment getEnvironment() {
		if (this.environment == null) {
			// 如果为指定环境，则创建环境
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * 创建并返回一个新的 {@link StandardServletEnvironment}。
	 * <p>子类可以重写此方法以配置环境或专门化返回的环境类型。
	 *
	 * @since 4.3.9
	 */
	protected Environment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * 存储 bean 工厂运行的 ServletContext。
	 * <p>仅在作为 bean 进行初始化时相关，以便将 ServletContext 作为回退到通常由 FilterConfig 实例提供的上下文。
	 *
	 * @see org.springframework.web.context.ServletContextAware
	 * @see #getServletContext()
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * 调用可能包含子类的自定义初始化的 {@code initFilterBean()} 方法。
	 * <p>仅在作为 bean 进行初始化时相关，其中不会调用标准的 {@code init(FilterConfig)} 方法。
	 *
	 * @see #initFilterBean()
	 * @see #init(javax.servlet.FilterConfig)
	 */
	@Override
	public void afterPropertiesSet() throws ServletException {
		initFilterBean();
	}

	/**
	 * 子类可以重写此方法以执行自定义的过滤器关闭操作。
	 * <p>注意：此方法将从标准过滤器销毁以及 Spring 应用程序上下文中的过滤器 bean 销毁中调用。
	 * <p>此默认实现为空。
	 */
	@Override
	public void destroy() {
	}


	/**
	 * 子类可以调用此方法来指定此属性（它必须与它们公开的 JavaBean 属性匹配）是必需的，
	 * 并且必须作为配置参数提供。这应该从子类的构造函数中调用。
	 * <p>此方法仅在传统初始化（由 FilterConfig 实例驱动）的情况下相关。
	 *
	 * @param property 必需属性的名称
	 */
	protected final void addRequiredProperty(String property) {
		this.requiredProperties.add(property);
	}

	/**
	 * 初始化此过滤器的标准方式。
	 * 将配置参数映射到此过滤器的 bean 属性，并调用子类初始化。
	 *
	 * @param filterConfig 此过滤器的配置
	 * @throws ServletException 如果 bean 属性无效（或缺少必需属性），或者如果子类初始化失败。
	 * @see #initFilterBean
	 */
	@Override
	public final void init(FilterConfig filterConfig) throws ServletException {
		Assert.notNull(filterConfig, "FilterConfig must not be null");
		// 设置过滤器配置
		this.filterConfig = filterConfig;

		// 从初始化参数设置 bean 属性。
		PropertyValues pvs = new FilterConfigPropertyValues(filterConfig, this.requiredProperties);
		if (!pvs.isEmpty()) {
			// 如果过滤器配置的bean属性不为空
			try {
				// 创建 Bean包装器 并使用该实例处理属性设置
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
				// 创建 资源加载器，用于加载资源文件
				ResourceLoader resourceLoader = new ServletContextResourceLoader(filterConfig.getServletContext());
				// 获取环境变量
				Environment env = this.environment;
				if (env == null) {
					// 如果环境变量为空，则创建一个标准的 Servlet 环境
					env = new StandardServletEnvironment();
				}
				// 注册一个自定义编辑器来处理 资源 类型的属性
				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, env));
				// 初始化 Bean包装器
				initBeanWrapper(bw);
				// 设置属性值
				bw.setPropertyValues(pvs, true);
			} catch (BeansException ex) {
				// 如果出现 BeansException，记录错误信息并抛出 NestedServletException
				String msg = "Failed to set bean properties on filter '" +
						filterConfig.getFilterName() + "': " + ex.getMessage();
				logger.error(msg, ex);
				throw new NestedServletException(msg, ex);
			}
		}

		// 让子类进行他们喜欢的任何初始化。
		initFilterBean();

		if (logger.isDebugEnabled()) {
			logger.debug("Filter '" + filterConfig.getFilterName() + "' configured for use");
		}
	}

	/**
	 * 初始化此 GenericFilterBean 的 BeanWrapper，可能使用自定义编辑器。
	 * <p>此默认实现为空。
	 *
	 * @param bw 要初始化的 BeanWrapper
	 * @throws BeansException 如果由 BeanWrapper 方法抛出
	 * @see org.springframework.beans.BeanWrapper#registerCustomEditor
	 */
	protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
	}

	/**
	 * 子类可以重写此方法以执行自定义初始化。
	 * 在调用此方法之前，将设置此过滤器的所有 bean 属性。
	 * <p>注意：此方法将从标准过滤器初始化以及 Spring 应用程序上下文中的过滤器 bean 初始化中调用。
	 * 在这两种情况下，都将可用过滤器名称和 ServletContext。
	 * <p>此默认实现为空。
	 *
	 * @throws ServletException 如果子类初始化失败
	 * @see #getFilterName()
	 * @see #getServletContext()
	 */
	protected void initFilterBean() throws ServletException {
	}

	/**
	 * 如果有的话，将此过滤器的 FilterConfig 设置为可用。
	 * 类似于 GenericServlet 的 {@code getServletConfig()}。
	 * <p>公共方法以类似于 WebLogic 6.1 中附带的 Servlet Filter 版本的 {@code getFilterConfig()} 方法的方式。
	 *
	 * @return FilterConfig 实例，如果没有可用则返回 {@code null}
	 * @see javax.servlet.GenericServlet#getServletConfig()
	 */
	@Nullable
	public FilterConfig getFilterConfig() {
		return this.filterConfig;
	}

	/**
	 * 将此过滤器的名称提供给子类。
	 * 类似于 GenericServlet 的 {@code getServletName()}。
	 * <p>默认情况下，使用 FilterConfig 的过滤器名称。
	 * 如果在 Spring 应用程序上下文中初始化为 bean，则会回退到在 bean 工厂中定义的 bean 名称。
	 *
	 * @return 过滤器名称，如果没有可用则返回 {@code null}
	 * @see javax.servlet.GenericServlet#getServletName()
	 * @see javax.servlet.FilterConfig#getFilterName()
	 * @see #setBeanName
	 */
	@Nullable
	protected String getFilterName() {
		return (this.filterConfig != null ? this.filterConfig.getFilterName() : this.beanName);
	}

	/**
	 * 将此过滤器的 ServletContext 提供给子类。
	 * 类似于 GenericServlet 的 {@code getServletContext()}。
	 * <p>默认情况下，使用 FilterConfig 的 ServletContext。
	 * 如果在 Spring 应用程序上下文中初始化为 bean，则会回退到 bean 工厂运行的 ServletContext。
	 *
	 * @return ServletContext 实例
	 * @throws IllegalStateException 如果没有可用的 ServletContext
	 * @see javax.servlet.GenericServlet#getServletContext()
	 * @see javax.servlet.FilterConfig#getServletContext()
	 * @see #setServletContext
	 */
	protected ServletContext getServletContext() {
		if (this.filterConfig != null) {
			// 如果过滤器配置不为空，则获取Servlet上下文
			return this.filterConfig.getServletContext();
		} else if (this.servletContext != null) {
			// 如果Servlet上下文不为空，获取Servlet上下文
			return this.servletContext;
		} else {
			// 否则抛出异常
			throw new IllegalStateException("No ServletContext");
		}
	}


	/**
	 * 从 FilterConfig 初始化参数创建的 PropertyValues 实现。
	 */
	@SuppressWarnings("serial")
	private static class FilterConfigPropertyValues extends MutablePropertyValues {

		/**
		 * 创建新的 FilterConfigPropertyValues。
		 *
		 * @param config             我们将从中获取 PropertyValues 的 FilterConfig
		 * @param requiredProperties 我们需要的属性名称集合，其中我们不能接受默认值
		 * @throws ServletException 如果缺少任何必需的属性
		 */
		public FilterConfigPropertyValues(FilterConfig config, Set<String> requiredProperties)
				throws ServletException {

			// 创建一个可能缺失的属性集合
			Set<String> missingProps = (!CollectionUtils.isEmpty(requiredProperties) ?
					new HashSet<>(requiredProperties) : null);

			// 获取过滤器配置的初始化参数名的枚举
			Enumeration<String> paramNames = config.getInitParameterNames();
			// 遍历初始化参数名枚举
			while (paramNames.hasMoreElements()) {
				// 获取初始化参数名
				String property = paramNames.nextElement();
				// 获取初始化参数的值
				Object value = config.getInitParameter(property);
				// 将初始化参数值添加到过滤器实例的属性中
				addPropertyValue(new PropertyValue(property, value));
				if (missingProps != null) {
					// 如果缺失的属性集合不为空，则从缺失的属性集合中移除该属性
					missingProps.remove(property);
				}
			}

			// 如果仍然缺少属性，则失败。
			if (!CollectionUtils.isEmpty(missingProps)) {
				// 如果缺少属性，抛出异常
				throw new ServletException(
						"Initialization from FilterConfig for filter '" + config.getFilterName() +
								"' failed; the following required properties were missing: " +
								StringUtils.collectionToDelimitedString(missingProps, ", "));
			}
		}
	}

}
