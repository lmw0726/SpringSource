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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.lang.Nullable;
import org.springframework.web.WebApplicationInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * {@link WebApplicationInitializer}实现的方便基类，用于在servlet上下文中注册{@link ContextLoaderListener}。
 *
 * <p>子类需要实现的唯一方法是{@link #createRootApplicationContext()}，
 * 它从{@link #registerContextLoaderListener(ServletContext)}中调用。
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.2
 */
public abstract class AbstractContextLoaderInitializer implements WebApplicationInitializer {

	/**
	 * 可用于子类的记录器。
	 */
	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		registerContextLoaderListener(servletContext);
	}

	/**
	 * 对给定的servlet上下文注册一个{@link ContextLoaderListener}。
	 * {@code ContextLoaderListener}将使用从{@link #createRootApplicationContext()}模板方法返回的应用程序上下文初始化。
	 *
	 * @param servletContext 要注册监听器的servlet上下文
	 */
	protected void registerContextLoaderListener(ServletContext servletContext) {
		// 创建根应用程序上下文
		WebApplicationContext rootAppContext = createRootApplicationContext();
		if (rootAppContext != null) {
			// 如果根应用程序上下文不为空，则创建ContextLoaderListener监听器
			ContextLoaderListener listener = new ContextLoaderListener(rootAppContext);
			// 设置根应用程序上下文的初始化器
			listener.setContextInitializers(getRootApplicationContextInitializers());
			// 将ContextLoaderListener监听器添加到ServletContext中
			servletContext.addListener(listener);
		} else {
			// 否则记录调试日志
			logger.debug("No ContextLoaderListener registered, as " +
					"createRootApplicationContext() did not return an application context");
		}
	}

	/**
	 * 创建要提供给{@code ContextLoaderListener}的“根”应用程序上下文。
	 * <p>返回的上下文将委托给{@link ContextLoaderListener#ContextLoaderListener(WebApplicationContext)}，
	 * 并将作为任何{@code DispatcherServlet}应用程序上下文的父上下文建立。
	 * 因此，它通常包含中间层服务、数据源等。
	 *
	 * @return 根应用程序上下文，如果不需要根上下文，则返回{@code null}
	 * @see org.springframework.web.servlet.support.AbstractDispatcherServletInitializer
	 */
	@Nullable
	protected abstract WebApplicationContext createRootApplicationContext();

	/**
	 * 指定要应用于{@code ContextLoaderListener}创建的根应用程序上下文的应用程序上下文初始化程序。
	 *
	 * @see #createRootApplicationContext()
	 * @see ContextLoaderListener#setContextInitializers
	 * @since 4.2
	 */
	@Nullable
	protected ApplicationContextInitializer<?>[] getRootApplicationContextInitializers() {
		return null;
	}

}
