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
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.support.BindStatus;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import java.beans.PropertyEditor;

/**
 * {@code <bind>} 标签支持对特定 bean 或 bean 属性的绑定错误进行评估。
 * 将一个类型为 {@link org.springframework.web.servlet.support.BindStatus} 的 "status" 变量暴露给 Java 表达式和 JSP EL 表达式。
 *
 * <p>可以用于绑定模型中的任何 bean 或 bean 属性。指定的路径决定了标签是否公开 bean 本身的状态（显示对象级错误）、特定 bean 属性的状态（显示字段错误），
 * 或一组匹配的 bean 属性（显示所有对应的字段错误）。
 *
 * <p>使用此标签绑定的 {@link org.springframework.validation.Errors} 对象会暴露给协作的其他标签，以及适用于该错误对象的 bean 属性。
 * 嵌套标签（例如 {@link TransformTag}）可以访问那些公开的属性。
 *
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th class="colFirst">属性</th>
 * <th class="colOne">是否必需</th>
 * <th class="colOne">是否运行时表达式</th>
 * <th class="colLast">描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="altColor">
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>设置此标签的 HTML 转义，作为布尔值。覆盖当前页面的默认 HTML 转义设置。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>ignoreNestedPath</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>设置是否忽略任何嵌套路径。默认为不忽略。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>path</p></td>
 * <td><p>true</p></td>
 * <td><p>true</p></td>
 * <td><p>要绑定状态信息的 bean 或 bean 属性的路径。例如，account.name、company.address.zipCode 或只是 employee。
 * 状态对象将被导出到页面范围，特定于此 bean 或 bean 属性。</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setPath
 */
@SuppressWarnings("serial")
public class BindTag extends HtmlEscapingAwareTag implements EditorAwareTag {

	/**
	 * 在此标签范围内公开的变量名称："status"。
	 */
	public static final String STATUS_VARIABLE_NAME = "status";

	/**
	 * 此标签应用的路径
	 */
	private String path = "";

	/**
	 * 是否忽略任何嵌套路径。
	 */
	private boolean ignoreNestedPath = false;

	/**
	 * 绑定状态
	 */
	@Nullable
	private BindStatus status;

	/**
	 * 先前页面的状态
	 */
	@Nullable
	private Object previousPageStatus;

	/**
	 * 先前请求状态
	 */
	@Nullable
	private Object previousRequestStatus;


	/**
	 * 设置此标签应用的路径。可以是一个 bean（例如 "person"）以获取全局错误，
	 * 或者是一个 bean 属性（例如 "person.name"）以获取字段错误
	 * （还支持嵌套字段和 "person.na*" 映射）。
	 * "person.*" 将返回指定 bean 的所有错误，包括全局错误和字段错误。
	 *
	 * @see org.springframework.validation.Errors#getGlobalErrors
	 * @see org.springframework.validation.Errors#getFieldErrors
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * 返回此标签适用于的路径。
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * 设置是否忽略任何嵌套路径。
	 * 默认为不忽略。
	 */
	public void setIgnoreNestedPath(boolean ignoreNestedPath) {
		this.ignoreNestedPath = ignoreNestedPath;
	}

	/**
	 * 返回是否忽略任何嵌套路径。
	 */
	public boolean isIgnoreNestedPath() {
		return this.ignoreNestedPath;
	}


	@Override
	protected final int doStartTagInternal() throws Exception {
		// 获取解析后的路径
		String resolvedPath = getPath();
		// 如果不忽略嵌套路径
		if (!isIgnoreNestedPath()) {
			// 获取嵌套路径
			String nestedPath = (String) this.pageContext.getAttribute(
					NestedPathTag.NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
			// 仅在路径尚未是绝对路径时进行前置
			if (nestedPath != null && !resolvedPath.startsWith(nestedPath) &&
					!resolvedPath.equals(nestedPath.substring(0, nestedPath.length() - 1))) {
				resolvedPath = nestedPath + resolvedPath;
			}
		}

		try {
			// 创建绑定状态对象
			this.status = new BindStatus(getRequestContext(), resolvedPath, isHtmlEscape());
		} catch (IllegalStateException ex) {
			// 抛出 JSP 标签异常
			throw new JspTagException(ex.getMessage());
		}

		// 保存先前的状态值，以便在此标签结束时重新暴露。
		this.previousPageStatus = this.pageContext.getAttribute(STATUS_VARIABLE_NAME, PageContext.PAGE_SCOPE);
		this.previousRequestStatus = this.pageContext.getAttribute(STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);

		// 将此标签的状态对象作为 PageContext 属性公开，使其可用于 JSP EL。
		this.pageContext.removeAttribute(STATUS_VARIABLE_NAME, PageContext.PAGE_SCOPE);
		this.pageContext.setAttribute(STATUS_VARIABLE_NAME, this.status, PageContext.REQUEST_SCOPE);

		// 返回评估正文的结果
		return EVAL_BODY_INCLUDE;
	}

	@Override
	public int doEndTag() {
		// 重置先前的状态值。
		if (this.previousPageStatus != null) {
			// 恢复先前的页面范围状态值
			this.pageContext.setAttribute(STATUS_VARIABLE_NAME, this.previousPageStatus, PageContext.PAGE_SCOPE);
		}
		if (this.previousRequestStatus != null) {
			// 恢复先前的请求范围状态值
			this.pageContext.setAttribute(STATUS_VARIABLE_NAME, this.previousRequestStatus, PageContext.REQUEST_SCOPE);
		} else {
			// 如果先前的请求范围状态值为空，则在请求范围中移除状态属性
			this.pageContext.removeAttribute(STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}
		// 返回页面的评估
		return EVAL_PAGE;
	}


	/**
	 * 返回当前的 BindStatus。
	 */
	private BindStatus getStatus() {
		Assert.state(this.status != null, "No current BindStatus");
		return this.status;
	}

	/**
	 * 检索此标签当前绑定到的属性，如果绑定到对象而不是特定属性，则返回 {@code null}。
	 * 用于协作嵌套标签。
	 *
	 * @return 此标签当前绑定到的属性，如果没有则为 {@code null}
	 */
	@Nullable
	public final String getProperty() {
		return getStatus().getExpression();
	}

	/**
	 * 检索此标签当前绑定到的 Errors 实例。
	 * 用于协作嵌套标签。
	 *
	 * @return 当前的 Errors 实例，如果没有则为 {@code null}
	 */
	@Nullable
	public final Errors getErrors() {
		return getStatus().getErrors();
	}

	@Override
	@Nullable
	public final PropertyEditor getEditor() {
		return getStatus().getEditor();
	}


	@Override
	public void doFinally() {
		super.doFinally();
		this.status = null;
		this.previousPageStatus = null;
		this.previousRequestStatus = null;
	}

}
