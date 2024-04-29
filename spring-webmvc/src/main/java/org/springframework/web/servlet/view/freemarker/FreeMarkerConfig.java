/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.view.freemarker;

import freemarker.ext.jsp.TaglibFactory;
import freemarker.template.Configuration;

/**
 * 由在 Web 环境中配置和管理 FreeMarker Configuration 对象的对象实现的接口。
 * {@link FreeMarkerView} 检测并使用它。
 *
 * @author Darren Davison
 * @author Rob Harrop
 * @see FreeMarkerConfigurer
 * @see FreeMarkerView
 * @since 03.03.2004
 */
public interface FreeMarkerConfig {

	/**
	 * 返回当前 Web 应用程序上下文的 FreeMarker {@link Configuration} 对象。
	 * <p>FreeMarker Configuration 对象可用于设置 FreeMarker 属性和共享对象，
	 * 并允许检索模板。
	 *
	 * @return FreeMarker Configuration
	 */
	Configuration getConfiguration();

	/**
	 * 返回用于在 FreeMarker 模板中访问 JSP 标签的 {@link TaglibFactory}。
	 */
	TaglibFactory getTaglibFactory();

}
