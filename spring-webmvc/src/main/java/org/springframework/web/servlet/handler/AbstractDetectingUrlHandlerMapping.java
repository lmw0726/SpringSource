/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.handler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.util.ObjectUtils;

/**
 * {@link org.springframework.web.servlet.HandlerMapping} 接口的抽象实现，
 * 通过检查应用程序上下文中定义的所有 bean，来检测处理程序 bean 的 URL 映射。
 *
 * @author Juergen Hoeller
 * @see #determineUrlsForHandler
 * @since 2.5
 */
public abstract class AbstractDetectingUrlHandlerMapping extends AbstractUrlHandlerMapping {
	/**
	 * 是否检测祖先上下文中的处理程序
	 */
	private boolean detectHandlersInAncestorContexts = false;


	/**
	 * 设置是否在祖先 ApplicationContext 中检测处理程序 bean。
	 * <p>默认值为 "false"：只会检测当前 ApplicationContext 中的处理程序 bean，
	 * 即仅在定义此 HandlerMapping 的上下文中（通常是当前 DispatcherServlet 的上下文）。
	 * <p>将此标志打开以在祖先上下文中检测处理程序 bean（通常是 Spring 根 WebApplicationContext）。
	 */
	public void setDetectHandlersInAncestorContexts(boolean detectHandlersInAncestorContexts) {
		this.detectHandlersInAncestorContexts = detectHandlersInAncestorContexts;
	}


	/**
	 * 在初始化时调用 {@link #detectHandlers()} 方法以及超类的初始化。
	 */
	@Override
	public void initApplicationContext() throws ApplicationContextException {
		// 调用初始化应用上下文
		super.initApplicationContext();
		// 检测处理器
		detectHandlers();
	}

	/**
	 * 注册在当前 ApplicationContext 中找到的所有处理程序。
	 * <p>处理程序的实际 URL 确定由具体的 {@link #determineUrlsForHandler(String)} 实现决定。
	 * 无法确定此类 URL 的 bean 不被视为处理程序。
	 *
	 * @throws org.springframework.beans.BeansException 如果无法注册处理程序
	 * @see #determineUrlsForHandler(String)
	 */
	protected void detectHandlers() throws BeansException {
		// 获取应用程序上下文
		ApplicationContext applicationContext = obtainApplicationContext();
		// 如果检测祖先上下文中的处理程序，则从Object开始获取祖先上下文所有的bean名称，否则进获取当前上下文中的bean名称
		String[] beanNames = (this.detectHandlersInAncestorContexts ?
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(applicationContext, Object.class) :
				applicationContext.getBeanNamesForType(Object.class));

		// 获取任何我们可以确定 URL 的 bean 名称。
		for (String beanName : beanNames) {
			// 根据bean名称推断处理器的URL
			String[] urls = determineUrlsForHandler(beanName);
			if (!ObjectUtils.isEmpty(urls)) {
				// 找到 URL 路径：我们认为它是一个处理程序。
				// 注册处理器
				registerHandler(urls, beanName);
			}
		}

		if (mappingsLogger.isDebugEnabled()) {
			mappingsLogger.debug(formatMappingName() + " " + getHandlerMap());
		} else if ((logger.isDebugEnabled() && !getHandlerMap().isEmpty()) || logger.isTraceEnabled()) {
			logger.debug("Detected " + getHandlerMap().size() + " mappings in " + formatMappingName());
		}
	}


	/**
	 * 确定给定处理程序 bean 的 URL。
	 *
	 * @param beanName 候选 bean 的名称
	 * @return bean 的确定 URL，如果没有，则返回空数组
	 */
	protected abstract String[] determineUrlsForHandler(String beanName);

}
