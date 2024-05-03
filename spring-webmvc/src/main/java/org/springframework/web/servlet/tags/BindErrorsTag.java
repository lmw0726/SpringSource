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

package org.springframework.web.servlet.tags;

import org.springframework.lang.Nullable;
import org.springframework.validation.Errors;

import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * 这个 {@code <hasBindErrors>} 标签在绑定错误时提供一个 {@link Errors} 实例。
 * HTML 转义标志参与页面范围或应用程序范围的设置（例如由 HtmlEscapeTag 或 web.xml 中的 "defaultHtmlEscape" context-param 设置）。
 *
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th>属性</th>
 * <th>是否必需</th>
 * <th>运行时表达式？</th>
 * <th>描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>设置此标签的 HTML 转义，作为布尔值。
 * 覆盖当前页面的默认 HTML 转义设置。</td>
 * </tr>
 * <tr>
 * <td>name</td>
 * <td>true</td>
 * <td>true</td>
 * <td>请求中需要检查错误的 bean 的名称。
 * 如果对于此 bean 可用错误，则它们将绑定到 'errors' 键下。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see BindTag
 * @see org.springframework.validation.Errors
 */
@SuppressWarnings("serial")
public class BindErrorsTag extends HtmlEscapingAwareTag {

	/**
	 * 包含 {@link Errors} 的页面上下文属性。
	 */
	public static final String ERRORS_VARIABLE_NAME = "errors";

	/**
	 * bean 名称
	 */
	private String name = "";

	/**
	 * 错误
	 */
	@Nullable
	private Errors errors;


	/**
	 * 设置此标签应检查的 bean 的名称。
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 返回此标签检查的 bean 的名称。
	 */
	public String getName() {
		return this.name;
	}


	@Override
	protected final int doStartTagInternal() throws ServletException, JspException {
		// 获取当前标签的错误信息。
		this.errors = getRequestContext().getErrors(this.name, isHtmlEscape());
		if (this.errors != null && this.errors.hasErrors()) {
			// 如果存在错误信息且有错误发生，则将错误信息存储到请求范围中。
			this.pageContext.setAttribute(ERRORS_VARIABLE_NAME, this.errors, PageContext.REQUEST_SCOPE);
			// 返回包含标签体内容页面。
			return EVAL_BODY_INCLUDE;
		} else {
			// 否则，跳过标签体内容。
			return SKIP_BODY;
		}
	}

	@Override
	public int doEndTag() {
		this.pageContext.removeAttribute(ERRORS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		return EVAL_PAGE;
	}

	/**
	 * 检索此标签当前绑定到的 Errors 实例。
	 * <p>用于配合嵌套标签。
	 */
	@Nullable
	public final Errors getErrors() {
		return this.errors;
	}


	@Override
	public void doFinally() {
		super.doFinally();
		this.errors = null;
	}

}
