/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.core.io;

/**
 * 从封闭“上下文”中加载资源的扩展接口，例如从 {@link javax.servlet.ServletContext} 加载，
 * 也可以从普通的类路径路径或相对文件系统路径加载（在未显式指定前缀的情况下，
 * 路径相对于本地 {@link ResourceLoader} 的上下文进行解析）。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.web.context.support.ServletContextResource
 */
public interface ContextResource extends Resource {

	/**
	 * 返回封闭“上下文”中的路径。
	 * <p>通常这是相对于上下文特定根目录的路径，
	 * 例如 ServletContext 根目录或 PortletContext 根目录。
	 */
	String getPathWithinContext();

}
