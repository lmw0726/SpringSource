/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.jsf;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.WebUtils;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

/**
 * 用于检索给定JSF {@link FacesContext}的Spring根{@link WebApplicationContext}的便捷方法。
 * 这对于从自定义基于JSF的代码访问Spring应用程序上下文非常有用。
 * <p>
 * 类似于Servlet环境中Spring的WebApplicationContextUtils。
 *
 * @author Juergen Hoeller
 * @see org.springframework.web.context.ContextLoader
 * @see org.springframework.web.context.support.WebApplicationContextUtils
 * @since 1.1
 */
public abstract class FacesContextUtils {

	/**
	 * 查找此Web应用程序的根{@link WebApplicationContext}，通常通过{@link org.springframework.web.context.ContextLoaderListener}加载。
	 * <p>将重新抛出在根上下文启动时发生的异常，以区分启动失败的上下文和没有上下文。
	 *
	 * @param fc 要查找Web应用程序上下文的FacesContext
	 * @return 此Web应用程序的根WebApplicationContext，如果没有则为{@code null}
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	@Nullable
	public static WebApplicationContext getWebApplicationContext(FacesContext fc) {
		// 确保 Faces上下文 不为 null
		Assert.notNull(fc, "FacesContext must not be null");
		// 从外部上下文中获取应用程序映射中的属性
		Object attr = fc.getExternalContext().getApplicationMap().get(
				WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (attr == null) {
			// 如果属性为 null，则返回 null
			return null;
		}
		// 如果属性是 RuntimeException 类型，则抛出该异常
		if (attr instanceof RuntimeException) {
			throw (RuntimeException) attr;
		}
		// 如果属性是 Error 类型，则抛出该错误
		if (attr instanceof Error) {
			throw (Error) attr;
		}
		// 如果属性不是 Web应用上下文 类型，则抛出异常
		if (!(attr instanceof WebApplicationContext)) {
			throw new IllegalStateException("Root context attribute is not of type WebApplicationContext: " + attr);
		}
		// 将属性强制转换为 Web应用上下文 并返回
		return (WebApplicationContext) attr;
	}

	/**
	 * 查找此Web应用程序的根{@link WebApplicationContext}，通常通过{@link org.springframework.web.context.ContextLoaderListener}加载。
	 * <p>将重新抛出在根上下文启动时发生的异常，以区分启动失败的上下文和没有上下文。
	 *
	 * @param fc 要查找Web应用程序上下文的FacesContext
	 * @return 此Web应用程序的根WebApplicationContext
	 * @throws IllegalStateException 如果找不到根WebApplicationContext
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	public static WebApplicationContext getRequiredWebApplicationContext(FacesContext fc) throws IllegalStateException {
		// 从 Faces上下文 获取 Web应用程序上下文
		WebApplicationContext wac = getWebApplicationContext(fc);
		if (wac == null) {
			// 如果获取不到，抛出异常
			throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
		}
		return wac;
	}

	/**
	 * 返回给定会话的最佳可用互斥体：即，用于给定会话同步的对象。
	 * <p>如果可用，返回会话互斥体属性；通常，这意味着需要在{@code web.xml}中定义{@code HttpSessionMutexListener}。
	 * 如果未找到互斥属性，则返回会话引用本身。
	 * <p>会话互斥体在会话的整个生命周期中保证是相同的对象，可在{@code SESSION_MUTEX_ATTRIBUTE}常量定义的键下获得。
	 * 它用作在当前会话上同步锁定的安全引用。
	 * <p>在许多情况下，会话引用本身也是一个安全的互斥体，因为对于同一个活动逻辑会话，它将始终是相同的对象引用。
	 * 但是，这并不保证在不同的servlet容器中；唯一100%安全的方法是会话互斥体。
	 *
	 * @param fc 要为其查找会话互斥体的FacesContext
	 * @return 互斥对象（永远不会为{@code null}）
	 * @see org.springframework.web.util.WebUtils#SESSION_MUTEX_ATTRIBUTE
	 * @see org.springframework.web.util.HttpSessionMutexListener
	 */
	@Nullable
	public static Object getSessionMutex(FacesContext fc) {
		Assert.notNull(fc, "FacesContext must not be null");
		// 从 Faces上下文 获取外部上下文
		ExternalContext ec = fc.getExternalContext();
		// 获取会话锁对象
		Object mutex = ec.getSessionMap().get(WebUtils.SESSION_MUTEX_ATTRIBUTE);
		if (mutex == null) {
			// 如果会话锁对象为 null，则创建会话锁
			mutex = ec.getSession(true);
		}
		return mutex;
	}

}
