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

package org.springframework.web.servlet.support;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * {@link org.springframework.web.WebApplicationInitializer WebApplicationInitializer}
 * 用于注册{@code DispatcherServlet}并使用基于Java的Spring配置。
 *
 * <p>实现需要实现以下方法：
 * <ul>
 * <li>{@link #getRootConfigClasses()} -- 用于“根”应用程序上下文（非Web基础设施）配置。
 * <li>{@link #getServletConfigClasses()} -- 用于{@code DispatcherServlet}应用程序上下文（Spring MVC基础设施）配置。
 * </ul>
 *
 * <p>如果不需要应用程序上下文层次结构，则应用程序可以通过{@link #getRootConfigClasses()}返回所有配置，并从{@link #getServletConfigClasses()}返回{@code null}。
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @since 3.2
 */
public abstract class AbstractAnnotationConfigDispatcherServletInitializer
		extends AbstractDispatcherServletInitializer {

	/**
	 * {@inheritDoc}
	 * <p>此实现创建一个{@link AnnotationConfigWebApplicationContext}，并将{@link #getRootConfigClasses()}返回的注解类提供给它。
	 * 如果{@link #getRootConfigClasses()}返回{@code null}，则返回{@code null}。
	 */
	@Override
	@Nullable
	protected WebApplicationContext createRootApplicationContext() {
		// 获取根配置类
		Class<?>[] configClasses = getRootConfigClasses();
		if (!ObjectUtils.isEmpty(configClasses)) {
			// 如果根配置类不为空，则创建 注解配置Web应用上下文
			AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
			// 注册根配置类
			context.register(configClasses);
			return context;
		} else {
			// 如果根配置类为空，则返回null
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>此实现创建一个{@link AnnotationConfigWebApplicationContext}，并将{@link #getServletConfigClasses()}返回的注解类提供给它。
	 */
	@Override
	protected WebApplicationContext createServletApplicationContext() {
		// 创建 注解配置Web应用上下文
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		// 获取Servlet配置类
		Class<?>[] configClasses = getServletConfigClasses();
		if (!ObjectUtils.isEmpty(configClasses)) {
			// 注册Servlet配置类
			context.register(configClasses);
		}
		return context;
	}

	/**
	 * 为{@linkplain #createRootApplicationContext() 根应用程序上下文}指定{@code @Configuration}和/或{@code @Component}类。
	 *
	 * @return 根应用程序上下文的配置，如果不希望创建和注册根上下文，则返回{@code null}
	 */
	@Nullable
	protected abstract Class<?>[] getRootConfigClasses();

	/**
	 * 为{@linkplain #createServletApplicationContext() Servlet应用程序上下文}指定{@code @Configuration}和/或{@code @Component}类。
	 *
	 * @return Servlet应用程序上下文的配置，如果所有配置都通过根配置类指定，则返回{@code null}
	 */
	@Nullable
	protected abstract Class<?>[] getServletConfigClasses();

}
