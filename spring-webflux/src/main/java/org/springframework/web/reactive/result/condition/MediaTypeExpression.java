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

package org.springframework.web.reactive.result.condition;

import org.springframework.http.MediaType;

/**
 * 媒体类型表达式的契约（例如 "text/plain"、"!text/plain"），在 {@code @RequestMapping} 注解中
 * 用于 "consumes" 和 "produces" 条件中定义。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface MediaTypeExpression {

	/**
	 * 获取媒体类型。
	 *
	 * @return 媒体类型
	 */
	MediaType getMediaType();

	/**
	 * 检查是否为否定表达式。
	 *
	 * @return 如果是否定表达式，则返回 true；否则返回 false
	 */
	boolean isNegated();

}
