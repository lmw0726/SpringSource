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

import org.springframework.beans.PropertyAccessor;
import org.springframework.lang.Nullable;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

/**
 * <p>{@code <nestedPath>} 标签支持和辅助模型中的嵌套 bean 或 bean 属性。
 * 在请求范围内导出一个类型为 String 的 "nestedPath" 变量，对当前页面和包含的页面（如果有）可见。
 *
 * <p>BindTag 将自动检测当前的嵌套路径，并自动将其前置到自身路径之前，形成对 bean 或 bean 属性的完整路径。
 *
 * <p>此标签还将前置当前设置的任何现有嵌套路径。因此，您可以嵌套多个 nested-path 标签。
 *
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th>属性</th>
 * <th>是否必需</th>
 * <th>运行时表达式</th>
 * <th>描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>path</td>
 * <td>true</td>
 * <td>true</td>
 * <td>设置此标签应用的路径。例如，'customer' 以允许绑定路径 'address.street' 而不是 'customer.address.street'。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
@SuppressWarnings("serial")
public class NestedPathTag extends TagSupport implements TryCatchFinally {

	/**
	 * 此标签作用域内的公开变量的名称: "nestedPath"。
	 */
	public static final String NESTED_PATH_VARIABLE_NAME = "nestedPath";

	/**
	 * 当前路径
	 */
	@Nullable
	private String path;

	/**
	 * 缓存先前的嵌套路径，以便可以重置它。
	 */
	@Nullable
	private String previousNestedPath;


	/**
	 * 设置此标签应用的路径。
	 * <p>例如，"customer" 以允许绑定路径 "address.street" 而不是 "customer.address.street"。
	 *
	 * @see BindTag#setPath
	 */
	public void setPath(@Nullable String path) {
		// 如果路径为空，则将其设置为空字符串。
		if (path == null) {
			path = "";
		}

		// 如果路径的长度大于零且不以 PropertyAccessor.NESTED_PROPERTY_SEPARATOR 结尾
		if (path.length() > 0 && !path.endsWith(PropertyAccessor.NESTED_PROPERTY_SEPARATOR)) {
			// 则添加 PropertyAccessor.NESTED_PROPERTY_SEPARATOR 到路径末尾。
			path += PropertyAccessor.NESTED_PROPERTY_SEPARATOR;
		}

		// 将路径设置为已处理的路径。
		this.path = path;
	}

	/**
	 * 返回此标签应用的路径。
	 */
	@Nullable
	public String getPath() {
		return this.path;
	}


	@Override
	public int doStartTag() throws JspException {
		// 保存先前的 nestedPath 值，构建并暴露当前的 nestedPath 值。
		// 使用请求范围将 nestedPath 暴露给包含的页面。
		// 获取之前的嵌套路径。
		this.previousNestedPath = (String) this.pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);

		// 如果之前的嵌套路径不为 null，则将当前路径与之前的嵌套路径相结合，否则将当前路径作为新的嵌套路径。
		String nestedPath = (this.previousNestedPath != null ? this.previousNestedPath + getPath() : getPath());

		// 将新的嵌套路径设置到请求范围的页面上下文属性中。
		this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, nestedPath, PageContext.REQUEST_SCOPE);

		// 返回 包含标签体内容的页面。
		return EVAL_BODY_INCLUDE;
	}

	/**
	 * 重置任何先前的 nestedPath 值。
	 */
	@Override
	public int doEndTag() {
		if (this.previousNestedPath != null) {
			// 暴露先前的 nestedPath 值。
			this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, this.previousNestedPath, PageContext.REQUEST_SCOPE);
		} else {
			// 删除暴露的 nestedPath 值。
			this.pageContext.removeAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}

		return EVAL_PAGE;
	}

	@Override
	public void doCatch(Throwable throwable) throws Throwable {
		throw throwable;
	}

	@Override
	public void doFinally() {
		this.previousNestedPath = null;
	}

}
