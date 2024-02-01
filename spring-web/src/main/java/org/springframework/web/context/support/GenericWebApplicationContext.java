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

package org.springframework.web.context.support;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * {@link GenericApplicationContext}的子类，适用于Web环境。
 *
 * <p>实现{@link ConfigurableWebApplicationContext}，但不适用于在{@code web.xml}中声明式设置。
 * 相反，它设计用于编程设置，例如用于构建嵌套上下文或在{@link org.springframework.web.WebApplicationInitializer WebApplicationInitializers}中使用。
 *
 * <p>将资源路径解释为servlet上下文资源，即位于web应用程序根目录下的路径。
 * 绝对路径 &mdash; 例如，对于web应用程序根目录之外的文件 &mdash; 可以通过{@code file:} URL访问，由
 * {@code AbstractApplicationContext}实现。
 *
 * <p>除了由{@link org.springframework.context.support.AbstractApplicationContext AbstractApplicationContext}检测到的特殊bean之外，
 * 此类还在上下文中检测到一个名为"themeSource"的{@link ThemeSource} bean。
 *
 * <p>如果要向{@code GenericWebApplicationContext}注册带有注释的<em>组件类</em>，可以使用
 * {@link org.springframework.context.annotation.AnnotatedBeanDefinitionReader
 * AnnotatedBeanDefinitionReader}，如以下示例所示。
 * 组件类特别包括
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * 类，还包括普通的{@link org.springframework.stereotype.Component @Component}
 * 类以及使用{@code javax.inject}注解的JSR-330兼容类。
 *
 * <pre class="code">
 * GenericWebApplicationContext context = new GenericWebApplicationContext();
 * AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(context);
 * reader.register(AppConfig.class, UserController.class, UserRepository.class);</pre>
 *
 * <p>如果您打算实现一个从配置文件读取bean定义的{@code WebApplicationContext}，可以考虑从
 * {@link AbstractRefreshableWebApplicationContext}派生，实现{@code loadBeanDefinitions}方法中读取bean定义。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 1.2
 */
public class GenericWebApplicationContext extends GenericApplicationContext
		implements ConfigurableWebApplicationContext, ThemeSource {
	/**
	 * Servlet上下文
	 */
	@Nullable
	private ServletContext servletContext;

	/**
	 * 主题资源
	 */
	@Nullable
	private ThemeSource themeSource;


	/**
	 * 创建一个新的{@code GenericWebApplicationContext}。
	 *
	 * @see #setServletContext
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericWebApplicationContext() {
		super();
	}

	/**
	 * 为给定的{@link ServletContext}创建一个新的{@code GenericWebApplicationContext}。
	 *
	 * @param servletContext 要运行的{@code ServletContext}
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericWebApplicationContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * 使用给定的{@link DefaultListableBeanFactory}创建一个新的{@code GenericWebApplicationContext}。
	 *
	 * @param beanFactory 用于此上下文的{@code DefaultListableBeanFactory}实例
	 * @see #setServletContext
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericWebApplicationContext(DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
	}

	/**
	 * 使用给定的{@link DefaultListableBeanFactory}和{@link ServletContext}创建一个新的{@code GenericWebApplicationContext}。
	 *
	 * @param beanFactory    用于此上下文的{@code DefaultListableBeanFactory}实例
	 * @param servletContext 要运行的{@code ServletContext}
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericWebApplicationContext(DefaultListableBeanFactory beanFactory, ServletContext servletContext) {
		super(beanFactory);
		this.servletContext = servletContext;
	}


	/**
	 * 设置此{@code WebApplicationContext}运行的{@link ServletContext}。
	 *
	 * @param servletContext 要设置的ServletContext对象
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
	public String getApplicationName() {
		return (this.servletContext != null ? this.servletContext.getContextPath() : "");
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
	 * 注册请求/会话作用域、环境bean、{@link ServletContextAwareProcessor}等。
	 *
	 * @param beanFactory 可配置的可列表的Bean工厂
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// 如果ServletContext不为null，则创建一个ServletContextAwareProcessor并添加到BeanFactory的BeanPostProcessor列表中
		if (this.servletContext != null) {
			beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext));
			beanFactory.ignoreDependencyInterface(ServletContextAware.class);
		}

		// 在BeanFactory中注册Web应用程序范围
		WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);

		// 在BeanFactory中注册与环境相关的Beans
		WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext);
	}

	/**
	 * 此实现支持位于{@link ServletContext}根目录下的文件路径。
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
	 * 初始化主题功能。
	 */
	@Override
	protected void onRefresh() {
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
	}

	/**
	 * {@inheritDoc}
	 * <p>替换与{@code Servlet}相关的属性源。
	 */
	@Override
	protected void initPropertySources() {
		// 获取当前ApplicationContext的环境
		ConfigurableEnvironment env = getEnvironment();

		// 如果环境实现了ConfigurableWebEnvironment接口，则调用initPropertySources方法初始化属性源
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(this.servletContext, null);
		}
	}

	@Override
	@Nullable
	public Theme getTheme(String themeName) {
		Assert.state(this.themeSource != null, "No ThemeSource available");
		return this.themeSource.getTheme(themeName);
	}


	// ---------------------------------------------------------------------
	// ConfigurableWebApplicationContext的伪实现
	// ---------------------------------------------------------------------

	@Override
	public void setServletConfig(@Nullable ServletConfig servletConfig) {
		// 没有操作
	}

	@Override
	@Nullable
	public ServletConfig getServletConfig() {
		throw new UnsupportedOperationException(
				"GenericWebApplicationContext does not support getServletConfig()");
	}

	@Override
	public void setNamespace(@Nullable String namespace) {
		// 没有操作
	}

	@Override
	@Nullable
	public String getNamespace() {
		throw new UnsupportedOperationException(
				"GenericWebApplicationContext does not support getNamespace()");
	}

	@Override
	public void setConfigLocation(String configLocation) {
		if (StringUtils.hasText(configLocation)) {
			throw new UnsupportedOperationException(
					"GenericWebApplicationContext does not support setConfigLocation(). " +
							"Do you still have a 'contextConfigLocation' init-param set?");
		}
	}

	@Override
	public void setConfigLocations(String... configLocations) {
		if (!ObjectUtils.isEmpty(configLocations)) {
			throw new UnsupportedOperationException(
					"GenericWebApplicationContext does not support setConfigLocations(). " +
							"Do you still have a 'contextConfigLocations' init-param set?");
		}
	}

	@Override
	public String[] getConfigLocations() {
		throw new UnsupportedOperationException(
				"GenericWebApplicationContext does not support getConfigLocations()");
	}

}
