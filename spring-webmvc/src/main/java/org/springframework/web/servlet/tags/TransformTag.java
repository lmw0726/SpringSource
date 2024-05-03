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
import org.springframework.web.util.TagUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.beans.PropertyEditor;
import java.io.IOException;

/**
 * {@code <transform>} 标签为 {@code spring:bind} 标签（或 Spring 表单标签库中的数据绑定表单元素标签）内的控制器和其他对象提供转换。
 *
 * <p>BindTag 有一个 PropertyEditor，用于将 bean 的属性转换为可在 HTML 表单中使用的字符串。此标签使用该 PropertyEditor 来转换传递到此标签的对象。
 *
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th>属性</th>
 * <th>是否必需？</th>
 * <th>运行时表达式？</th>
 * <th>描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>设置此标签的 HTML 转义，作为布尔值。覆盖当前页面的默认 HTML 转义设置。</td>
 * </tr>
 * <tr>
 * <td>scope</td>
 * <td>false</td>
 * <td>true</td>
 * <td>将结果导出到变量时要使用的范围。仅当也设置了 var 时才使用此属性。可能的值包括 page、request、session 和 application。</td>
 * </tr>
 * <tr>
 * <td>value</td>
 * <td>true</td>
 * <td>true</td>
 * <td>要转换的值。这是您要转换的实际对象（例如日期）。使用当前由 'spring:bind' 标签使用的 PropertyEditor。</td>
 * </tr>
 * <tr>
 * <td>var</td>
 * <td>false</td>
 * <td>true</td>
 * <td>用于将结果绑定到页面、请求、会话或应用程序范围时要使用的字符串。如果未指定，则将结果输出到 writer（即通常直接输出到 JSP）。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @see BindTag
 * @since 20.09.2003
 */
@SuppressWarnings("serial")
public class TransformTag extends HtmlEscapingAwareTag {

	/**
	 * 要使用适当的属性编辑器进行转换的值。
	 */
	@Nullable
	private Object value;

	/**
	 * 要将结果放入的变量。
	 */
	@Nullable
	private String var;

	/**
	 * 结果将放入的变量的范围。
	 */
	private String scope = TagUtils.SCOPE_PAGE;


	/**
	 * 设置要转换的值，使用封闭的 BindTag 中的适当 PropertyEditor。
	 * <p>值可以是要转换的纯值（JSP 中的硬编码字符串值或 JSP 表达式），
	 * 也可以是要评估的 JSP EL 表达式（转换表达式的结果）。
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 设置 PageContext 属性名称，用于公开一个包含转换结果的变量。
	 *
	 * @see #setScope
	 * @see javax.servlet.jsp.PageContext#setAttribute
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * 设置要导出变量的范围。
	 * 默认为 SCOPE_PAGE（"page"）。
	 *
	 * @see #setVar
	 * @see org.springframework.web.util.TagUtils#SCOPE_PAGE
	 * @see javax.servlet.jsp.PageContext#setAttribute
	 */
	public void setScope(String scope) {
		this.scope = scope;
	}


	@Override
	protected final int doStartTagInternal() throws JspException {
		if (this.value != null) {
			// 查找包含的 EditorAwareTag（例如 BindTag），如果适用。
			EditorAwareTag tag = (EditorAwareTag) TagSupport.findAncestorWithClass(this, EditorAwareTag.class);
			if (tag == null) {
				// 如果找不到适用的 EditorAwareTag，则抛出异常。
				throw new JspException("TransformTag can only be used within EditorAwareTag (e.g. BindTag)");
			}

			String result = null;
			// 获取属性编辑器
			PropertyEditor editor = tag.getEditor();
			if (editor != null) {
				// 如果找到编辑器，设置编辑器的值。
				editor.setValue(this.value);
				// 获取编辑器的文本值。
				result = editor.getAsText();
			} else {
				// 否则，只需执行 toString。
				result = this.value.toString();
			}
			// 对结果进行 HTML 转义。
			result = htmlEscape(result);
			if (this.var != null) {
				// 如果有 var 属性，将结果存在页面上下文中
				this.pageContext.setAttribute(this.var, result, TagUtils.getScope(this.scope));
			} else {
				try {
					// 否则，将结果直接打印到页面输出流中。
					this.pageContext.getOut().print(result);
				} catch (IOException ex) {
					// 处理 IO 异常。
					throw new JspException(ex);
				}
			}
		}

		return SKIP_BODY;
	}

}
