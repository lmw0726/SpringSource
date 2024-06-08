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

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletContext;
import java.io.File;

/**
 * {@link WebApplicationContext} 中运行的应用程序对象的方便超类。
 * 提供了 {@code getWebApplicationContext()}、{@code getServletContext()} 和 {@code getTempDir()} 访问器。
 *
 * <p>注意: 通常建议为实际所需的回调使用单独的回调接口。
 * 此广泛的基类主要用于框架内部使用，通常需要 {@link ServletContext} 访问等。
 *
 * @author Juergen Hoeller
 * @see SpringBeanAutowiringSupport
 * @since 28.08.2003
 */
public abstract class WebApplicationObjectSupport extends ApplicationObjectSupport implements ServletContextAware {
	/**
	 * Servlet上下文
	 */
	@Nullable
	private ServletContext servletContext;


	@Override
	public final void setServletContext(ServletContext servletContext) {
		// 如果传入的 servlet 上下文与当前 servlet 上下文不同
		if (servletContext != this.servletContext) {
			// 更新当前 servlet 上下文
			this.servletContext = servletContext;
			// 初始化 servlet 上下文
			initServletContext(servletContext);
		}
	}

	/**
	 * 覆盖基类行为以强制在 ApplicationContext 中运行。
	 * 如果未在上下文中运行，所有访问器都将抛出 IllegalStateException。
	 *
	 * @see #getApplicationContext()
	 * @see #getMessageSourceAccessor()
	 * @see #getWebApplicationContext()
	 * @see #getServletContext()
	 * @see #getTempDir()
	 */
	@Override
	protected boolean isContextRequired() {
		return true;
	}

	/**
	 * 如果给定的 ApplicationContext 是 {@link WebApplicationContext}，则调用 {@link #initServletContext(javax.servlet.ServletContext)}。
	 */
	@Override
	protected void initApplicationContext(ApplicationContext context) {
		// 调用父类的 initApplicationContext 方法
		super.initApplicationContext(context);

		// 如果当前 servlet 上下文为空，并且上下文是 Web应用上下文 类型
		if (this.servletContext == null && context instanceof WebApplicationContext) {
			// 获取 Web应用上下文 中的 servlet 上下文
			this.servletContext = ((WebApplicationContext) context).getServletContext();
			// 如果获取到的 servlet 上下文不为空，则初始化 servlet 上下文
			if (this.servletContext != null) {
				initServletContext(this.servletContext);
			}
		}
	}

	/**
	 * 子类可以根据此应用程序对象运行的 ServletContext 进行自定义初始化。
	 * 默认实现为空。由 {@link #initApplicationContext(org.springframework.context.ApplicationContext)} 和 {@link #setServletContext(javax.servlet.ServletContext)} 调用。
	 */
	protected void initServletContext(ServletContext servletContext) {
	}

	/**
	 * 作为 WebApplicationContext 返回当前应用程序上下文。
	 * <p><b>注意:</b> 仅在实际需要访问 WebApplicationContext 特定功能时使用此方法。
	 * 最好使用 {@code getApplicationContext()} 或 {@code getServletContext()}，
	 * 以便能够在非 WebApplicationContext 环境中运行。
	 *
	 * @throws IllegalStateException 如果未在 WebApplicationContext 中运行
	 * @see #getApplicationContext()
	 */
	@Nullable
	protected final WebApplicationContext getWebApplicationContext() throws IllegalStateException {
		// 获取应用程序上下文
		ApplicationContext ctx = getApplicationContext();

		if (ctx instanceof WebApplicationContext) {
			// 如果应用程序上下文是 Web应用上下文 类型，则返回该上下文
			return (WebApplicationContext) getApplicationContext();
		} else if (isContextRequired()) {
			// 如果需要上下文，但上下文不是 Web应用上下文 类型，则抛出异常
			throw new IllegalStateException("WebApplicationObjectSupport instance [" + this +
					"] does not run in a WebApplicationContext but in: " + ctx);
		} else {
			// 如果不需要上下文，则返回 null
			return null;
		}
	}

	/**
	 * 返回当前的 ServletContext。
	 *
	 * @throws IllegalStateException 如果未在所需的 ServletContext 中运行
	 * @see #isContextRequired()
	 */
	@Nullable
	protected final ServletContext getServletContext() throws IllegalStateException {
		// 如果当前 servlet 上下文不为空，则返回该上下文
		if (this.servletContext != null) {
			return this.servletContext;
		}

		ServletContext servletContext = null;
		WebApplicationContext wac = getWebApplicationContext();
		if (wac != null) {
			// 尝试获取 servlet 上下文
			servletContext = wac.getServletContext();
		}

		// 如果未能获取到 servlet 上下文，并且需要上下文，则抛出异常
		if (servletContext == null && isContextRequired()) {
			throw new IllegalStateException("WebApplicationObjectSupport instance [" + this +
					"] does not run within a ServletContext. Make sure the object is fully configured!");
		}

		// 返回 servlet 上下文
		return servletContext;
	}

	/**
	 * 返回当前 Web 应用程序的临时目录，由 Servlet 容器提供。
	 *
	 * @return 表示临时目录的 File
	 * @throws IllegalStateException 如果未在 ServletContext 中运行
	 * @see org.springframework.web.util.WebUtils#getTempDir(javax.servlet.ServletContext)
	 */
	protected final File getTempDir() throws IllegalStateException {
		// 获取 servlet 上下文
		ServletContext servletContext = getServletContext();
		Assert.state(servletContext != null, "ServletContext is required");
		// 返回临时目录
		return WebUtils.getTempDir(servletContext);
	}

}
