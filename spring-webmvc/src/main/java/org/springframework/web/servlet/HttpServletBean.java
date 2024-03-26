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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link javax.servlet.http.HttpServlet} 的简单扩展，它将其配置参数（{@code web.xml} 中 {@code servlet} 标签内的 {@code init-param} 条目）视为 bean 属性。
 *
 * <p>对于任何类型的 servlet 都是一个方便的超类。配置参数的类型转换是自动进行的，对应的 setter 方法将以转换后的值调用。
 * 子类也可以指定必需的属性。没有匹配的 bean 属性 setter 的参数将被忽略。
 *
 * <p>此 servlet 将请求处理委托给子类，继承了 HttpServlet 的默认行为（{@code doGet}、{@code doPost} 等）。
 *
 * <p>此通用 servlet 基类不依赖于 Spring 的 {@link org.springframework.context.ApplicationContext} 概念。
 * 简单的 servlet 通常不会加载自己的上下文，而是通过过滤器的 {@link #getServletContext() ServletContext}
 * 访问 Spring 根应用程序上下文，可以通过 {@link org.springframework.web.context.support.WebApplicationContextUtils} 进行访问。
 *
 * <p>{@link FrameworkServlet} 类是一个更具体的 servlet 基类，它加载自己的应用程序上下文。
 * FrameworkServlet 作为 Spring 的全功能 {@link DispatcherServlet} 的直接基类。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #addRequiredProperty
 * @see #initServletBean
 * @see #doGet
 * @see #doPost
 */
@SuppressWarnings("serial")
public abstract class HttpServletBean extends HttpServlet implements EnvironmentCapable, EnvironmentAware {


	/**
	 * 可用于子类的记录器
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 可配置的环境
	 */
	@Nullable
	private ConfigurableEnvironment environment;

	/**
	 * 必填的属性集合
	 */
	private final Set<String> requiredProperties = new HashSet<>(4);


	/**
	 * 子类可以调用此方法指定此属性（必须与它们公开的 JavaBean 属性匹配）是强制的，并且必须作为配置参数提供。这应该从子类的构造函数中调用。
	 * <p>此方法仅在由 ServletConfig 实例驱动的传统初始化情况下相关。
	 *
	 * @param property 必需属性的名称
	 */
	protected final void addRequiredProperty(String property) {
		this.requiredProperties.add(property);
	}

	/**
	 * 设置此 servlet 运行的 {@code Environment}。
	 * <p>这里设置的任何环境都会覆盖默认提供的 {@link StandardServletEnvironment}。
	 *
	 * @throws IllegalArgumentException 如果环境不可分配给 {@code ConfigurableEnvironment}
	 */
	@Override
	public void setEnvironment(Environment environment) {
		Assert.isInstanceOf(ConfigurableEnvironment.class, environment, "ConfigurableEnvironment required");
		this.environment = (ConfigurableEnvironment) environment;
	}

	/**
	 * 返回与此 servlet 关联的 {@link Environment}。
	 * <p>如果未指定任何环境，则将通过 {@link #createEnvironment()} 初始化默认环境。
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * 创建并返回一个新的 {@link StandardServletEnvironment}。
	 * <p>子类可以重写此方法以配置环境或特化返回的环境类型。
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * 将配置参数映射到此 servlet 的 bean 属性，并调用子类初始化。
	 *
	 * @throws ServletException 如果 bean 属性无效（或缺少必需属性），或者如果子类初始化失败。
	 */
	@Override
	public final void init() throws ServletException {

		// 从初始化参数设置 Bean 的属性。
		PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
		if (!pvs.isEmpty()) {
			try {
				// 创建 BeanWrapper 并设置自定义编辑器
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
				ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
				// 初始化 BeanWrapper 并设置属性值
				initBeanWrapper(bw);
				bw.setPropertyValues(pvs, true);
			} catch (BeansException ex) {
				// 如果设置 Bean 属性失败，则记录错误日志并抛出异常
				if (logger.isErrorEnabled()) {
					logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
				}
				throw ex;
			}
		}

		// 让子类执行他们喜欢的任何初始化。
		initServletBean();
	}

	/**
	 * 初始化此 HttpServletBean 的 BeanWrapper，可能使用自定义编辑器。
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
	 * 在调用此方法之前，此 servlet 的所有 bean 属性都将被设置。
	 * <p>此默认实现为空。
	 *
	 * @throws ServletException 如果子类初始化失败
	 */
	protected void initServletBean() throws ServletException {
	}

	/**
	 * 重写的方法，当尚未设置 ServletConfig 时简单返回 {@code null}。
	 *
	 * @see #getServletConfig()
	 */
	@Override
	@Nullable
	public String getServletName() {
		return (getServletConfig() != null ? getServletConfig().getServletName() : null);
	}


	/**
	 * 从ServletConfig init参数创建的PropertyValues实现。
	 */
	private static class ServletConfigPropertyValues extends MutablePropertyValues {

		/**
		 * 创建新的ServletConfigPropertyValues
		 *
		 * @param config             我们要从ServletConfig取PropertyValues
		 * @param requiredProperties 我们需要的一组属性名称，我们不能接受默认值
		 * @throws ServletException 如果缺少任何必需的属性
		 */
		public ServletConfigPropertyValues(ServletConfig config, Set<String> requiredProperties)
				throws ServletException {

			// 如果 必填属性 不为空，则创建一个缺失属性的集合
			Set<String> missingProps = (!CollectionUtils.isEmpty(requiredProperties) ?
					new HashSet<>(requiredProperties) : null);

			// 获取初始化参数的名称枚举，并遍历其中的每个参数
			Enumeration<String> paramNames = config.getInitParameterNames();
			while (paramNames.hasMoreElements()) {
				String property = paramNames.nextElement();
				Object value = config.getInitParameter(property);
				// 添加属性值，并在缺失属性集合中移除该属性
				addPropertyValue(new PropertyValue(property, value));
				if (missingProps != null) {
					missingProps.remove(property);
				}
			}

			// 如果仍然存在缺失的属性，则抛出异常
			if (!CollectionUtils.isEmpty(missingProps)) {
				throw new ServletException(
						"Initialization from ServletConfig for servlet '" + config.getServletName() +
								"' failed; the following required properties were missing: " +
								StringUtils.collectionToDelimitedString(missingProps, ", "));
			}
		}
	}

}
