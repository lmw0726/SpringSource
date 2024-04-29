/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.view.groovy;

import groovy.text.markup.MarkupTemplateEngine;

/**
 * 通过实现此接口的对象配置和管理 Groovy {@link MarkupTemplateEngine}，以便在 Web 环境中进行自动查找。
 * 由 {@link GroovyMarkupView} 检测并使用。
 *
 * @author Brian Clozel
 * @see GroovyMarkupConfigurer
 * @since 4.1
 */
public interface GroovyMarkupConfig {

	/**
	 * 返回当前 Web 应用程序上下文的 Groovy {@link MarkupTemplateEngine}。
	 * 可以是唯一的一个 servlet，也可以是共享的根上下文。
	 *
	 * @return Groovy MarkupTemplateEngine 引擎
	 */
	MarkupTemplateEngine getTemplateEngine();

}
