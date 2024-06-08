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

package org.springframework.web.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Enumeration;

/**
 * Web应用程序监听器，用于清理ServletContext中剩余的可清理属性，
 * 即那些实现了{@link DisposableBean}且尚未被移除的属性。
 * 通常用于销毁“应用程序”范围内的对象，其生命周期意味着在Web应用程序关闭阶段的最后进行销毁。
 *
 * @author Juergen Hoeller
 * @see org.springframework.web.context.support.ServletContextScope
 * @see ContextLoaderListener
 * @since 3.0
 */
public class ContextCleanupListener implements ServletContextListener {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(ContextCleanupListener.class);


	@Override
	public void contextInitialized(ServletContextEvent event) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		cleanupAttributes(event.getServletContext());
	}


	/**
	 * 查找所有Spring内部的ServletContext属性，这些属性实现了{@link DisposableBean}，
	 * 并调用它们的destroy方法。
	 *
	 * @param servletContext 要检查的ServletContext
	 * @see DisposableBean#destroy()
	 */
	static void cleanupAttributes(ServletContext servletContext) {
		// 获取 Servlet上下文 中的所有属性名称
		Enumeration<String> attrNames = servletContext.getAttributeNames();
		// 遍历所有属性名称
		while (attrNames.hasMoreElements()) {
			String attrName = attrNames.nextElement();
			// 检查属性名称是否以 "org.springframework." 开头
			if (attrName.startsWith("org.springframework.")) {
				// 获取属性值
				Object attrValue = servletContext.getAttribute(attrName);
				// 检查属性值是否为 DisposableBean 实例
				if (attrValue instanceof DisposableBean) {
					try {
						// 调用 DisposableBean 的 destroy 方法
						((DisposableBean) attrValue).destroy();
					} catch (Throwable ex) {
						// 如果 destroy 方法调用失败，记录警告日志
						if (logger.isWarnEnabled()) {
							logger.warn("Invocation of destroy method failed on ServletContext " +
									"attribute with name '" + attrName + "'", ex);
						}
					}
				}
			}
		}
	}

}
