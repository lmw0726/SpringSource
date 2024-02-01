/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.context.support;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * 用于测试的静态{@link org.springframework.web.context.WebApplicationContext}实现。
 * 不适用于生产应用程序。
 *
 * <p>实现{@link org.springframework.web.context.ConfigurableWebApplicationContext}
 * 接口，允许直接替换{@link XmlWebApplicationContext}，尽管实际上不支持外部配置文件。
 *
 * <p>将资源路径解释为servlet上下文资源，即位于web应用程序根目录下的路径。
 * 绝对路径，例如位于web应用程序根目录之外的文件，可以通过"file:" URL访问，由
 * {@link org.springframework.core.io.DefaultResourceLoader}实现。
 *
 * <p>除了由{@link org.springframework.context.support.AbstractApplicationContext}检测到的特殊bean之外，
 * 此类还在上下文中检测到类型为{@link org.springframework.ui.context.ThemeSource}的bean，其特殊bean名称为"themeSource"。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.ui.context.ThemeSource
 */
public class StaticWebApplicationContext extends StaticApplicationContext
		implements ConfigurableWebApplicationContext, ThemeSource {

	/**
	 * Servlet上下文
	 */
	@Nullable
	private ServletContext servletContext;

	/**
	 * Servlet配置
	 */
	@Nullable
	private ServletConfig servletConfig;

	/**
	 * 命名空间
	 */
	@Nullable
	private String namespace;

	/**
	 * 主题资源
	 */
	@Nullable
	private ThemeSource themeSource;


	public StaticWebApplicationContext() {
		setDisplayName("Root WebApplicationContext");
	}


	/**
	 * 设置运行此WebApplicationContext的ServletContext。
	 */
	@Override
	public void setServletContext(@Nullable ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	@Nullable
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	public void setServletConfig(@Nullable ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
		if (servletConfig != null && this.servletContext == null) {
			this.servletContext = servletConfig.getServletContext();
		}
	}

	@Override
	@Nullable
	public ServletConfig getServletConfig() {
		return this.servletConfig;
	}

	@Override
	public void setNamespace(@Nullable String namespace) {
		this.namespace = namespace;
		if (namespace != null) {
			setDisplayName("WebApplicationContext for namespace '" + namespace + "'");
		}
	}

	@Override
	@Nullable
	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * {@link StaticWebApplicationContext}类不支持此方法。
	 *
	 * @param configLocation 配置位置
	 * @throws UnsupportedOperationException <b>总是</b>
	 */
	@Override
	public void setConfigLocation(String configLocation) {
		throw new UnsupportedOperationException("StaticWebApplicationContext does not support config locations");
	}

	/**
	 * {@link StaticWebApplicationContext}类不支持此方法。
	 *
	 * @param configLocations 配置位置
	 * @throws UnsupportedOperationException <b>总是</b>
	 */
	@Override
	public void setConfigLocations(String... configLocations) {
		throw new UnsupportedOperationException("StaticWebApplicationContext does not support config locations");
	}

	@Override
	public String[] getConfigLocations() {
		return null;
	}


	/**
	 * 注册请求/会话作用域、{@link ServletContextAwareProcessor}等。
	 *
	 * @param beanFactory 可配置的可列表的Bean工厂
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// 创建一个ServletContextAwareProcessor，并添加到BeanFactory的BeanPostProcessor列表中
		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext, this.servletConfig));

		// 忽略BeanFactory对ServletContextAware和ServletConfigAware接口的依赖
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
		beanFactory.ignoreDependencyInterface(ServletConfigAware.class);

		// 在BeanFactory中注册Web应用程序范围
		WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);

		// 在BeanFactory中注册与环境相关的Beans
		WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext, this.servletConfig);
	}

	/**
	 * 此实现支持位于ServletContext根目录下的文件路径。
	 *
	 * @param path 资源路径
	 * @see ServletContextResource
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		Assert.state(this.servletContext != null, "No ServletContext available");
		return new ServletContextResource(this.servletContext, path);
	}

	/**
	 * 此实现在未展开的WAR中支持模式匹配。
	 *
	 * @return ServletContextResourcePatternResolver
	 * @see ServletContextResourcePatternResolver
	 */
	@Override
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new ServletContextResourcePatternResolver(this);
	}

	/**
	 * 创建并返回一个新的{@link StandardServletEnvironment}。
	 *
	 * @return ConfigurableEnvironment
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * 初始化主题功能。
	 */
	@Override
	protected void onRefresh() {
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
	}

	@Override
	protected void initPropertySources() {
		WebApplicationContextUtils.initServletPropertySources(getEnvironment().getPropertySources(),
				this.servletContext, this.servletConfig);
	}

	@Override
	@Nullable
	public Theme getTheme(String themeName) {
		Assert.state(this.themeSource != null, "No ThemeSource available");
		return this.themeSource.getTheme(themeName);
	}

}
