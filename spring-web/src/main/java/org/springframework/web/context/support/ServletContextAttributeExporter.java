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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.util.Map;

/**
 * 导出器，将 Spring 定义的对象公开为 ServletContext 属性。通常，将使用 bean 引用将 Spring 定义的 bean
 * 导出为 ServletContext 属性。
 *
 * <p>将 Spring 定义的 bean 提供给完全不了解 Spring，而只了解 Servlet API 的代码非常有用。客户端代码可以
 * 使用普通的 ServletContext 属性查找来访问这些对象，尽管它们是在 Spring 应用程序上下文中定义的。
 *
 * <p>或者，考虑使用 WebApplicationContextUtils 类通过 WebApplicationContext 接口访问 Spring 定义的
 * bean。当然，这会使客户端代码了解 Spring API。
 *
 * @author Juergen Hoeller
 * @see javax.servlet.ServletContext#getAttribute
 * @see WebApplicationContextUtils#getWebApplicationContext
 * @since 1.1.4
 */
public class ServletContextAttributeExporter implements ServletContextAware {
	/**
	 * 日志记录器
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 属性值
	 */
	@Nullable
	private Map<String, Object> attributes;


	/**
	 * 设置要公开为键值对的 ServletContext 属性。每个键都将被视为 ServletContext 属性键，
	 * 并且每个值将用作相应的属性值。
	 * <p>通常，您将为值使用 bean 引用，以将 Spring 定义的 bean 导出为 ServletContext 属性。
	 * 当然，也可以定义普通值以进行导出。
	 */
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		// 如果属性集合不为空
		if (this.attributes != null) {
			// 遍历属性集合中的每个条目
			for (Map.Entry<String, Object> entry : this.attributes.entrySet()) {
				// 获取属性名称和值
				String attributeName = entry.getKey();
				// 如果日志级别为调试，并且 servlet 上下文中存在同名属性，则记录替换信息
				if (logger.isDebugEnabled()) {
					if (servletContext.getAttribute(attributeName) != null) {
						logger.debug("Replacing existing ServletContext attribute with name '" + attributeName + "'");
					}
				}
				// 将属性设置到 servlet 上下文中
				servletContext.setAttribute(attributeName, entry.getValue());
				// 如果日志级别为跟踪，则记录导出信息
				if (logger.isTraceEnabled()) {
					logger.trace("Exported ServletContext attribute with name '" + attributeName + "'");
				}
			}
		}
	}

}
