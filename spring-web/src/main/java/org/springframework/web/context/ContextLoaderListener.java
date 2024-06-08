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

package org.springframework.web.context;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 引导监听器，用于启动和关闭 Spring 的根 {@link WebApplicationContext}。
 * 简单地委托给 {@link ContextLoader} 以及 {@link ContextCleanupListener}。
 *
 * <p>截至 Spring 3.1，{@code ContextLoaderListener} 支持通过 {@link #ContextLoaderListener(WebApplicationContext)}
 * 构造函数注入根 Web 应用程序上下文，允许在 Servlet 3.0+ 环境中进行编程配置。有关用法示例，请参阅 {@link org.springframework.web.WebApplicationInitializer}。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see #setContextInitializers
 * @see org.springframework.web.WebApplicationInitializer
 * @since 17.02.2003
 */
public class ContextLoaderListener extends ContextLoader implements ServletContextListener {

	/**
	 * 创建一个新的 {@code ContextLoaderListener}，它将根据 servlet context 参数 "contextClass" 和 "contextConfigLocation"
	 * 创建一个 Web 应用程序上下文。有关每个参数的默认值的详细信息，请参见 {@link ContextLoader} 超类文档。
	 * <p>通常在 {@code web.xml} 中将 {@code ContextLoaderListener} 声明为 {@code <listener>} 时使用此构造函数，
	 * 其中需要一个无参构造函数。
	 * <p>创建的应用程序上下文将在 ServletContext 中注册，并且 Spring 应用程序上下文将在此监听器的 {@link #contextDestroyed}
	 * 生命周期方法被调用时关闭。
	 *
	 * @see ContextLoader
	 * @see #ContextLoaderListener(WebApplicationContext)
	 * @see #contextInitialized(ServletContextEvent)
	 * @see #contextDestroyed(ServletContextEvent)
	 */
	public ContextLoaderListener() {
	}

	/**
	 * 创建一个新的 {@code ContextLoaderListener}，使用给定的应用程序上下文。此构造函数在 Servlet 3.0+ 环境中非常有用，
	 * 其中可以通过 {@link javax.servlet.ServletContext#addListener} API 基于实例注册监听器。
	 * <p>上下文可能已经 {@linkplain org.springframework.context.ConfigurableApplicationContext#refresh() 刷新}，
	 * 如果（a）它是 {@link ConfigurableWebApplicationContext} 的实现，并且（b）尚未刷新（推荐的方法），
	 * 那么将执行以下操作：
	 * <ul>
	 * <li>如果给定的上下文尚未分配 {@linkplain org.springframework.context.ConfigurableApplicationContext#setId id}，则将为其分配一个</li>
	 * <li>ServletContext 和 ServletConfig 对象将委托给应用程序上下文</li>
	 * <li>将调用 {@link #customizeContext}</li>
	 * <li>将应用 {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer org.springframework.context.ApplicationContextInitializer ApplicationContextInitializers} 的任何指定的
	 * {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer} 类。</li>
	 * <li>将调用 {@link org.springframework.context.ConfigurableApplicationContext#refresh refresh()}。</li>
	 * </ul>
	 * 如果上下文已经刷新或不实现 {@code ConfigurableWebApplicationContext}，则在用户根据特定需求执行了这些操作（或未执行）的情况下，
	 * 不会发生上述任何操作。
	 * <p>有关用法示例，请参阅 {@link org.springframework.web.WebApplicationInitializer}。
	 * <p>无论如何，给定的应用程序上下文将在 ServletContext 中注册为 {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}，
	 * 并且当此监听器的 {@link #contextDestroyed} 生命周期方法在 Spring 应用程序上下文上调用时，Spring 应用程序上下文将关闭。
	 *
	 * @param context 要管理的应用程序上下文
	 * @see #contextInitialized(ServletContextEvent)
	 * @see #contextDestroyed(ServletContextEvent)
	 */
	public ContextLoaderListener(WebApplicationContext context) {
		super(context);
	}


	/**
	 * 初始化根 Web 应用程序上下文。
	 */
	@Override
	public void contextInitialized(ServletContextEvent event) {
		// 初始化 WebApplicationContext
		initWebApplicationContext(event.getServletContext());
	}


	/**
	 * 关闭根 Web 应用程序上下文。
	 */
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		// 关闭 Web应用程序上下文
		closeWebApplicationContext(event.getServletContext());
		// 清理 Servlet上下文 中的属性
		ContextCleanupListener.cleanupAttributes(event.getServletContext());
	}

}
