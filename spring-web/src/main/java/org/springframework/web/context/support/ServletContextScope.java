/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.ServletContext;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ServletContext 的 {@link Scope} 包装器，即全局 Web 应用程序属性。
 *
 * <p>这与传统的 Spring 单例不同之处在于它公开了 ServletContext 中的属性。这些属性将在整个应用程序关闭时被销毁，可能比包含的 Spring ApplicationContext 的关闭早或晚。
 *
 * <p>相关的销毁机制依赖于在 {@code web.xml} 中注册的 {@link org.springframework.web.context.ContextCleanupListener}。注意 {@link org.springframework.web.context.ContextLoaderListener} 包含了 ContextCleanupListener 的功能。
 *
 * <p>此作用域以键 {@link org.springframework.web.context.WebApplicationContext#SCOPE_APPLICATION "application"} 的形式注册为默认作用域。
 *
 * @author Juergen Hoeller
 * @see org.springframework.web.context.ContextCleanupListener
 * @since 3.0
 */
public class ServletContextScope implements Scope, DisposableBean {

	/**
	 * Servlet上下文
	 */
	private final ServletContext servletContext;

	/**
	 * 销毁回调，key为bean名称，value为回调方法
	 */
	private final Map<String, Runnable> destructionCallbacks = new LinkedHashMap<>();


	/**
	 * 为给定的 ServletContext 创建一个新的 Scope 包装器。
	 *
	 * @param servletContext 要包装的 ServletContext
	 */
	public ServletContextScope(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		this.servletContext = servletContext;
	}


	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		// 获取 Servlet 上下文中指定名称的属性
		Object scopedObject = this.servletContext.getAttribute(name);

		// 如果属性值为空
		if (scopedObject == null) {
			// 通过对象工厂获取对象
			scopedObject = objectFactory.getObject();
			// 将对象设置到 Servlet 上下文中
			this.servletContext.setAttribute(name, scopedObject);
		}

		// 返回获取到的属性值
		return scopedObject;
	}

	@Override
	@Nullable
	public Object remove(String name) {
		// 获取 Servlet 上下文中指定名称的属性
		Object scopedObject = this.servletContext.getAttribute(name);

		// 如果属性值不为空
		if (scopedObject != null) {
			// 在同步块中移除销毁回调
			synchronized (this.destructionCallbacks) {
				this.destructionCallbacks.remove(name);
			}
			// 从 Servlet 上下文中移除属性
			this.servletContext.removeAttribute(name);
			// 返回获取到的属性值
			return scopedObject;
		} else {
			// 如果属性值为空，则返回 null
			return null;
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		synchronized (this.destructionCallbacks) {
			this.destructionCallbacks.put(name, callback);
		}
	}

	@Override
	@Nullable
	public Object resolveContextualObject(String key) {
		return null;
	}

	@Override
	@Nullable
	public String getConversationId() {
		return null;
	}


	/**
	 * 调用所有已注册的销毁回调。在 ServletContext 关闭时调用。
	 *
	 * @see org.springframework.web.context.ContextCleanupListener
	 */
	@Override
	public void destroy() {
		// 在销毁回调列表上进行同步
		synchronized (this.destructionCallbacks) {
			// 遍历销毁回调列表，并执行每个回调
			for (Runnable runnable : this.destructionCallbacks.values()) {
				runnable.run();
			}
			// 清空销毁回调列表
			this.destructionCallbacks.clear();
		}
	}

}
