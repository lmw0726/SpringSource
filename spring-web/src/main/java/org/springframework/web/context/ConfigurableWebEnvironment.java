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

package org.springframework.web.context;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.lang.Nullable;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * {@link ConfigurableEnvironment}的特化版本，允许在{@link ServletContext}和（可选）{@link ServletConfig}最早可用时初始化与Servlet相关的
 * {@link org.springframework.core.env.PropertySource}对象。
 *
 * @author Chris Beams
 * @since 3.1.2
 * @see ConfigurableWebApplicationContext#getEnvironment()
 */
public interface ConfigurableWebEnvironment extends ConfigurableEnvironment {

	/**
	 * 使用给定的参数，将任何充当占位符的{@linkplain
	 * org.springframework.core.env.PropertySource.StubPropertySource stub property source}实例替换为真正的servlet上下文/配置属性源。
	 *
	 * @param servletContext {@link ServletContext}（不能为空）
	 * @param servletConfig {@link ServletConfig}（如果不可用，则为{@code null}）
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources(
	 * org.springframework.core.env.MutablePropertySources, ServletContext, ServletConfig)
	 */
	void initPropertySources(@Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig);

}
