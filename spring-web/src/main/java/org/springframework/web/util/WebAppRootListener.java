/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 监听器，将系统属性设置为 Web 应用程序根目录。
 * 可以使用servlet上下文级别的“webAppRootKey”init参数（即web.xml中的context-param）定义系统属性的键，
 * 默认键是“webapp.root”。
 *
 * <p>可以用于支持使用系统属性进行替换的工具包（即System.getProperty值），
 * 例如 log4j 在日志文件位置中的 "${key}" 语法。
 *
 * <p>注意：此监听器应该放置在 web.xml 中的 ContextLoaderListener 之前，
 * 至少在用于 log4j 时是这样。Log4jConfigListener 隐式地设置系统属性，
 * 因此除此之外不需要此监听器。
 *
 * <p><b>警告</b>：一些容器，例如 Tomcat，<b>不会</b>将系统属性分开处理每个 Web 应用程序。
 * 您必须为每个 Web 应用程序使用唯一的“webAppRootKey”context-param，以避免冲突。
 * 其他容器如 Resin 则会隔离每个 Web 应用程序的系统属性：在这种情况下，
 * 您可以使用默认键（即没有“webAppRootKey”context-param）而不必担心。
 *
 * <p><b>警告</b>：包含 Web 应用程序的 WAR 文件需要展开才能设置 Web 应用程序根系统属性。
 * 当 WAR 文件被部署到 WebLogic 时，默认情况下不会发生这种情况。不要在这种环境中使用此监听器！
 *
 * @author Juergen Hoeller
 * @since 2003-04-18
 * @see WebUtils#setWebAppRootSystemProperty
 * @see System#getProperty
 */
public class WebAppRootListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		WebUtils.setWebAppRootSystemProperty(event.getServletContext());
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		WebUtils.removeWebAppRootSystemProperty(event.getServletContext());
	}

}
