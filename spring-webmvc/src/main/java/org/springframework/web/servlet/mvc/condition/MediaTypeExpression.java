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

package org.springframework.web.servlet.mvc.condition;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 媒体类型表达式（例如 "text/plain", "!text/plain"）的合同，
 * 这些表达式在 {@code @RequestMapping} 注解中的 "consumes" 和 "produces" 条件中定义。
 *
 * @author Rossen Stoyanchev
 * @see RequestMapping#consumes()
 * @see RequestMapping#produces()
 * @since 3.1
 */
public interface MediaTypeExpression {

	/**
	 * 获取表达式中的媒体类型。
	 *
	 * @return 媒体类型
	 */
	MediaType getMediaType();

	/**
	 * 检查表达式是否是否定的。
	 *
	 * @return 如果是否定的则返回 true，否则返回 false
	 */
	boolean isNegated();

}
