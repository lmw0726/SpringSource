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

package org.springframework.web.util;

import java.net.URI;
import java.util.Map;

/**
 * 定义了使用变量扩展URI模板的方法。
 *
 * @author Rossen Stoyanchev
 * @see org.springframework.web.client.RestTemplate#setUriTemplateHandler(UriTemplateHandler)
 * @since 4.2
 */
public interface UriTemplateHandler {

	/**
	 * 使用URI变量的映射扩展给定的URI模板。
	 *
	 * @param uriTemplate  URI模板
	 * @param uriVariables 变量值
	 * @return 创建的URI实例
	 */
	URI expand(String uriTemplate, Map<String, ?> uriVariables);

	/**
	 * 使用URI变量的数组扩展给定的URI模板。
	 *
	 * @param uriTemplate  URI模板
	 * @param uriVariables 变量值
	 * @return 创建的URI实例
	 */
	URI expand(String uriTemplate, Object... uriVariables);

}
