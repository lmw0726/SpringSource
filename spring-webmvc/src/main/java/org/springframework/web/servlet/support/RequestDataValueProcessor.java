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

package org.springframework.web.servlet.support;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 用于检查和可能修改请求数据值的契约，例如 URL 查询参数或表单字段值，在它们被视图渲染之前或重定向之前。
 *
 * <p>实现可以使用此契约作为解决方案的一部分，以提供数据完整性、保密性、防止跨站请求伪造 (CSRF) 等保护措施，或用于其他任务，例如自动向所有表单和 URL 添加隐藏字段。
 *
 * <p>支持此契约的视图技术可以通过 {@link RequestContext#getRequestDataValueProcessor()} 获取一个实例来委托。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public interface RequestDataValueProcessor {

	/**
	 * 在渲染新表单操作时调用。
	 *
	 * @param request    当前请求
	 * @param action     表单操作
	 * @param httpMethod 表单的 HTTP 方法
	 * @return 要使用的操作，可能已修改
	 */
	String processAction(HttpServletRequest request, String action, String httpMethod);

	/**
	 * 在渲染表单字段值时调用。
	 *
	 * @param request 当前请求
	 * @param name    表单字段名称（如果有）
	 * @param value   表单字段值
	 * @param type    表单字段类型（"text"、"hidden" 等）
	 * @return 要使用的表单字段值，可能已修改
	 */
	String processFormFieldValue(HttpServletRequest request, @Nullable String name, String value, String type);

	/**
	 * 在所有表单字段都已渲染之后调用。
	 *
	 * @param request 当前请求
	 * @return 要添加的额外隐藏表单字段，如果没有，则为 {@code null}
	 */
	@Nullable
	Map<String, String> getExtraHiddenFields(HttpServletRequest request);

	/**
	 * 在要渲染或重定向到 URL 之前调用。
	 *
	 * @param request 当前请求
	 * @param url     URL 值
	 * @return 要使用的 URL，可能已修改
	 */
	String processUrl(HttpServletRequest request, String url);

}
