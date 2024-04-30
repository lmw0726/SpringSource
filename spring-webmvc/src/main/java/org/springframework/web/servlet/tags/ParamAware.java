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

package org.springframework.web.servlet.tags;

/**
 * 允许实现标签利用嵌套的 {@code spring:param} 标签。
 *
 * @author Scott Andrews
 * @see ParamTag
 * @since 3.0
 */
public interface ParamAware {

	/**
	 * 回调钩子，用于让嵌套的 spring:param 标签将它们的值传递给父标签。
	 *
	 * @param param 嵌套的 {@code spring:param} 标签的结果
	 */
	void addParam(Param param);

}
