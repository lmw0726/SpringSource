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

package org.springframework.web.servlet;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;

/**
 * 将传入的 {@link javax.servlet.http.HttpServletRequest}
 * 转换为逻辑视图名称的策略接口，当没有显式提供视图名称时使用。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public interface RequestToViewNameTranslator {

	/**
	 * 将给定的 {@link HttpServletRequest} 转换为视图名称。
	 *
	 * @param request 提供从中解析视图名称的上下文的传入 {@link HttpServletRequest}
	 * @return 视图名称，如果没有找到默认视图，则返回 {@code null}
	 * @throws Exception 如果视图名称转换失败
	 */
	@Nullable
	String getViewName(HttpServletRequest request) throws Exception;

}
