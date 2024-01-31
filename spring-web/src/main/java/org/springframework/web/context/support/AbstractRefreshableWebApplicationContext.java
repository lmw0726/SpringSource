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

package org.springframework.web.context.support;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * {@link org.springframework.context.support.AbstractRefreshableApplicationContext}
 * 的子类，实现了 {@link org.springframework.web.context.ConfigurableWebApplicationContext}
 * 接口，用于 Web 环境。提供一个 "configLocations" 属性，通过 ConfigurableWebApplicationContext
 * 接口在 Web 应用启动时进行填充。
 *
 * <p>这个类与 AbstractRefreshableApplicationContext 一样容易被子类化：
 * 你只需要实现 {@link #loadBeanDefinitions} 方法；有关详细信息，请参见超类的 javadoc。
 * 注意，实现应该从由 {@link #getConfigLocations} 方法返回的位置指定的文件加载 bean 定义。
 *
 * <p>将资源路径解释为 servlet 上下文资源，即 Web 应用程序根目录下的路径。绝对路径，
 * 例如在 Web 应用程序根目录之外的文件，可以通过 "file:" URL 访问，如
 * {@link org.springframework.core.io.DefaultResourceLoader} 实现。
 *
 * <p>除了由 {@link org.springframework.context.support.AbstractApplicationContext}
 * 检测到的特殊 bean 之外，此类还在上下文中检测到一个类型为
 * {@link org.springframework.ui.context.ThemeSource} 的 bean，其特殊 bean 名称为 "themeSource"。
 *
 * <p><b>这是要为不同的 bean 定义格式进行子类化的 Web 上下文。</b> 可以将这样的上下文实现指定为
 * {@link org.springframework.web.context.ContextLoader} 的 "contextClass" 上下文参数
 * 或作为 {@link org.springframework.web.servlet.FrameworkServlet} 的 "contextClass"
 * init-param，替换默认的 {@link XmlWebApplicationContext}。然后，它将自动接收
 * "contextConfigLocation" 上下文参数或 init-param。
 *
 * <p>请注意，WebApplicationContext 实现通常应该根据通过
 * {@link ConfigurableWebApplicationContext} 接口接收到的配置进行自我配置。
 * 相比之下，独立的应用程序上下文可能允许在自定义启动代码中进行配置
 * （例如，{@link org.springframework.context.support.GenericApplicationContext}）。
 *
 * @author Juergen Hoeller
 * @since 1.1.3
 * @see #loadBeanDefinitions
 * @see org.springframework.web.context.ConfigurableWebApplicationContext#setConfigLocations
 * @see org.springframework.ui.context.ThemeSource
 * @see XmlWebApplicationContext
 */
public abstract class AbstractRefreshableWebApplicationContext extends AbstractRefreshableConfigApplicationContext
		implements ConfigurableWebApplicationContext, ThemeSource {

	/** 运行此上下文的 Servlet 上下文。 */
	@Nullable
	private ServletContext servletContext;

	/** 运行此上下文的 Servlet 配置，如果有的话。 */
	@Nullable
	private ServletConfig servletConfig;

	/** 此上下文的命名空间，如果是根则为 {@code null}。 */
	@Nullable
	private String namespace;

	/** 用于此 ApplicationContext 的 ThemeSource。 */
	@Nullable
	private ThemeSource themeSource;


	public AbstractRefreshableWebApplicationContext() {
		setDisplayName("Root WebApplicationContext");
	}


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
		// 设置Servlet配置对象
		this.servletConfig = servletConfig;

		// 如果Servlet配置对象不为null且当前ServletContext为null，设置当前ServletContext为Servlet配置对象的ServletContext。
		if (servletConfig != null && this.servletContext == null) {
			setServletContext(servletConfig.getServletContext());
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

	@Override
	public String[] getConfigLocations() {
		return super.getConfigLocations();
	}

	@Override
	public String getApplicationName() {
		return (this.servletContext != null ? this.servletContext.getContextPath() : "");
	}

	/**
	 * 创建并返回一个新的 {@link StandardServletEnvironment}。
	 * 子类可以覆盖此方法以配置环境或特化返回的环境类型。
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * 注册请求/会话作用域，{@link ServletContextAwareProcessor} 等。
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// 添加Servlet上下文感知处理器，传入当前的ServletContext和Servlet配置对象
		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext, this.servletConfig));

		// 忽略对ServletContextAware和ServletConfigAware接口的依赖
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
		beanFactory.ignoreDependencyInterface(ServletConfigAware.class);

		// 注册Web应用程序作用域和环境相关的bean
		WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
		WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext, this.servletConfig);
	}

	/**
	 * 该实现支持 ServletContext 根目录下的文件路径。
	 * @see ServletContextResource
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		Assert.state(this.servletContext != null, "No ServletContext available");
		return new ServletContextResource(this.servletContext, path);
	}

	/**
	 * 该实现支持在未展开的 WAR 文件中进行模式匹配。
	 * @see ServletContextResourcePatternResolver
	 */
	@Override
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new ServletContextResourcePatternResolver(this);
	}

	/**
	 * 初始化主题能力。
	 */
	@Override
	protected void onRefresh() {
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
	}

	/**
	 * {@inheritDoc}
	 * <p>替换与 Servlet 相关的属性源。
	 */
	@Override
	protected void initPropertySources() {
		// 获取当前的环境对象
		ConfigurableEnvironment env = getEnvironment();

		// 如果环境对象是可配置的Web环境
		if (env instanceof ConfigurableWebEnvironment) {
			// 调用initPropertySources方法，传入当前的ServletContext和Servlet配置对象
			((ConfigurableWebEnvironment) env).initPropertySources(this.servletContext, this.servletConfig);
		}
	}

	@Override
	@Nullable
	public Theme getTheme(String themeName) {
		Assert.state(this.themeSource != null, "No ThemeSource available");
		return this.themeSource.getTheme(themeName);
	}

}
