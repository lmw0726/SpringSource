/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.TilesException;
import org.apache.tiles.preparer.ViewPreparer;
import org.apache.tiles.preparer.factory.PreparerFactory;
import org.apache.tiles.request.Request;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Tiles {@link org.apache.tiles.preparer.factory.PreparerFactory}接口的抽象实现，
 * 获取当前的Spring WebApplicationContext并委托给{@link #getPreparer(String, org.springframework.web.context.WebApplicationContext)}。
 *
 * @author Juergen Hoeller
 * @see #getPreparer(String, org.springframework.web.context.WebApplicationContext)
 * @see SimpleSpringPreparerFactory
 * @see SpringBeanPreparerFactory
 * @since 3.2
 */
public abstract class AbstractSpringPreparerFactory implements PreparerFactory {

	@Override
	public ViewPreparer getPreparer(String name, Request context) {
		// 从上下文中获取request相关的Web应用上下文
		WebApplicationContext webApplicationContext = (WebApplicationContext) context.getContext("request").get(
				DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		// 如果 Web应用上下文 为null
		if (webApplicationContext == null) {
			// 从上下文中获取application相关的 Web应用上下文
			webApplicationContext = (WebApplicationContext) context.getContext("application").get(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			// 如果 Web应用上下文 仍为null
			if (webApplicationContext == null) {
				// 抛出IllegalStateException异常
				throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
			}
		}
		// 根据名称从获取到的 Web应用上下文 中获取或创建ViewPreparer，并返回
		return getPreparer(name, webApplicationContext);
	}

	/**
	 * 根据给定的准备器名称和Spring WebApplicationContext获取准备器实例。
	 *
	 * @param name    准备器的名称
	 * @param context 当前的Spring WebApplicationContext
	 * @return 准备器实例
	 * @throws TilesException 如果失败
	 */
	protected abstract ViewPreparer getPreparer(String name, WebApplicationContext context) throws TilesException;

}
