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

package org.springframework.web.reactive.result.view.freemarker;

import freemarker.template.Configuration;

/**
 * 在 Web 环境中配置和管理 FreeMarker Configuration 对象的对象所需实现的接口。
 * 被 {@link FreeMarkerView} 检测并使用。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface FreeMarkerConfig {

	/**
	 * 返回当前 Web 应用程序上下文的 FreeMarker Configuration 对象。
	 * <p>FreeMarker Configuration 对象可用于设置 FreeMarker 属性和共享对象，并允许检索模板。
	 *
	 * @return FreeMarker Configuration
	 */
	Configuration getConfiguration();

}
