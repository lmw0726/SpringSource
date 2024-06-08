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

package org.springframework.web.context;

import org.springframework.beans.factory.Aware;

import javax.servlet.ServletConfig;

/**
 * 由任何希望被通知其运行的{@link ServletConfig}（通常由{@link WebApplicationContext}确定）的对象实现的接口。
 *
 * <p>注意：只有在实际运行在特定于Servlet的WebApplicationContext中时才满足。否则，不会设置ServletConfig。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see ServletContextAware
 * @since 2.0
 */
public interface ServletConfigAware extends Aware {

	/**
	 * 设置此对象运行的{@link ServletConfig}。
	 * <p>在填充常规bean属性之后但在init回调（如InitializingBean的{@code afterPropertiesSet}或自定义init方法）之前调用。
	 * 在ApplicationContextAware的{@code setApplicationContext}之后调用。
	 *
	 * @param servletConfig 此对象使用的{@link ServletConfig}
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 */
	void setServletConfig(ServletConfig servletConfig);

}
