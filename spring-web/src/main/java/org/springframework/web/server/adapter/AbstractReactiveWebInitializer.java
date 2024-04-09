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

package org.springframework.web.server.adapter;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;
import org.springframework.util.Assert;
import org.springframework.web.WebApplicationInitializer;

import javax.servlet.*;

/**
 * {@link org.springframework.web.WebApplicationInitializer}的基类，
 * 在Servlet容器上安装Spring Reactive Web应用程序。
 *
 * <p>Spring配置加载并传递给{@link WebHttpHandlerBuilder#applicationContext WebHttpHandlerBuilder}，
 * 后者扫描上下文寻找特定的bean并创建一个响应式的{@link HttpHandler}。
 * 结果处理器通过{@link ServletHttpHandlerAdapter}安装为Servlet。
 *
 * @author Rossen Stoyanchev
 * @since 5.0.2
 */
public abstract class AbstractReactiveWebInitializer implements WebApplicationInitializer {

	/**
	 * 要使用的默认Servlet名称。请参见{@link #getServletName}。
	 */
	public static final String DEFAULT_SERVLET_NAME = "http-handler-adapter";


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		String servletName = getServletName();
		Assert.hasLength(servletName, "getServletName() must not return null or empty");

		// 创建应用程序上下文
		ApplicationContext applicationContext = createApplicationContext();
		Assert.notNull(applicationContext, "createApplicationContext() must not return null");

		// 刷新应用程序上下文
		refreshApplicationContext(applicationContext);
		// 注册应用程序上下文的关闭监听器
		registerCloseListener(servletContext, applicationContext);

		// 创建HttpHandler
		HttpHandler httpHandler = WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		// 创建ServletHttpHandlerAdapter
		ServletHttpHandlerAdapter servlet = new ServletHttpHandlerAdapter(httpHandler);

		// 添加Servlet到Servlet上下文中
		ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, servlet);
		if (registration == null) {
			// 如果注册失败，则抛出IllegalStateException异常
			throw new IllegalStateException("Failed to register servlet with name '" + servletName + "'. " +
					"Check if there is another servlet registered under the same name.");
		}

		// 设置加载顺序为1
		registration.setLoadOnStartup(1);
		// 添加映射路径
		registration.addMapping(getServletMapping());
		// 设置异步支持
		registration.setAsyncSupported(true);
	}

	/**
	 * 返回用于注册{@link ServletHttpHandlerAdapter}的名称。
	 * <p>默认情况下为{@link #DEFAULT_SERVLET_NAME}。
	 */
	protected String getServletName() {
		return DEFAULT_SERVLET_NAME;
	}

	/**
	 * 返回包含应用程序bean的Spring配置，包括通过{@link WebHttpHandlerBuilder#applicationContext}检测到的bean。
	 */
	protected ApplicationContext createApplicationContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		Class<?>[] configClasses = getConfigClasses();
		Assert.notEmpty(configClasses, "No Spring configuration provided through getConfigClasses()");
		context.register(configClasses);
		return context;
	}

	/**
	 * 指定{@link org.springframework.context.annotation.Configuration @Configuration}和/
	 * 或{@link org.springframework.stereotype.Component @Component}类，构成应用程序配置。
	 * 配置类将传递给{@linkplain #createApplicationContext()}。
	 */
	protected abstract Class<?>[] getConfigClasses();

	/**
	 * 刷新给定的应用程序上下文（如果需要）。
	 */
	protected void refreshApplicationContext(ApplicationContext context) {
		if (context instanceof ConfigurableApplicationContext) {
			ConfigurableApplicationContext cac = (ConfigurableApplicationContext) context;
			// 如果应用程序上下文未激活，则刷新它
			if (!cac.isActive()) {
				cac.refresh();
			}
		}
	}

	/**
	 * 注册一个{@link ServletContextListener}，当销毁servlet上下文时关闭给定的应用程序上下文。
	 * @param servletContext 要监听的servlet上下文
	 * @param applicationContext 当{@code servletContext}被销毁时要关闭的应用程序上下文
	 */
	protected void registerCloseListener(ServletContext servletContext, ApplicationContext applicationContext) {
		if (applicationContext instanceof ConfigurableApplicationContext) {
			ConfigurableApplicationContext cac = (ConfigurableApplicationContext) applicationContext;
			// 创建Servlet上下文销毁监听器
			ServletContextDestroyedListener listener = new ServletContextDestroyedListener(cac);
			// 添加 Servlet上下文销毁监听器 到 Servlet上下文中
			servletContext.addListener(listener);
		}
	}

	/**
	 * 返回要使用的Servlet映射。仅支持默认的Servlet映射“/”和基于路径的Servlet映射，例如“/api/*”。
	 * <p>默认情况下，此设置为“/”。
	 */
	protected String getServletMapping() {
		return "/";
	}


	private static class ServletContextDestroyedListener implements ServletContextListener {

		/**
		 * 可配置的应用上下文
		 */
		private final ConfigurableApplicationContext applicationContext;

		public ServletContextDestroyedListener(ConfigurableApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Override
		public void contextInitialized(ServletContextEvent sce) {
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			this.applicationContext.close();
		}
	}

}
