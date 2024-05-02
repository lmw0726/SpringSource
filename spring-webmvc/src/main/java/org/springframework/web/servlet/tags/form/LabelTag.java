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

package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.jsp.JspException;

/**
 * {@code <label>} 标签在 HTML 'label' 标签中呈现表单字段标签。
 *
 * <p>请参阅随完整 Spring 发行版一起提供的 "formTags" 演示应用程序，了解此类的示例。
 *
 * <p>
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th class="colFirst">属性</th>
 * <th class="colOne">是否必需？</th>
 * <th class="colOne">运行时表达式？</th>
 * <th class="colLast">描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="altColor">
 * <td><p>cssClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>cssErrorClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性。仅在存在错误时使用。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>cssStyle</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>dir</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>for</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>启用/禁用呈现值的 HTML 转义。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>lang</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>ondblclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onkeydown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onkeypress</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onkeyup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmousedown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmousemove</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmouseout</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmouseover</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmouseup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>path</p></td>
 * <td><p>true</p></td>
 * <td><p>true</p></td>
 * <td><p>数据绑定的错误对象路径</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>tabindex</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>title</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class LabelTag extends AbstractHtmlElementTag {

	/**
	 * HTML '{@code label}' 标签。
	 */
	private static final String LABEL_TAG = "label";

	/**
	 * '{@code for}' 属性的名称。
	 */
	private static final String FOR_ATTRIBUTE = "for";


	/**
	 * 正在使用的 {@link TagWriter} 实例。
	 * <p>存储以便在 {@link #doEndTag()} 中关闭标签。
	 */
	@Nullable
	private TagWriter tagWriter;

	/**
	 * '{@code for}' 属性的值。
	 */
	@Nullable
	private String forId;


	/**
	 * 设置 '{@code for}' 属性的值。
	 * <p>默认为 {@link #getPath} 的值；可能是运行时表达式。
	 */
	public void setFor(String forId) {
		this.forId = forId;
	}

	/**
	 * 获取 '{@code id}' 属性的值。
	 * <p>可能是运行时表达式。
	 */
	@Nullable
	protected String getFor() {
		return this.forId;
	}


	/**
	 * 写入开头的 '{@code label}' 标签并强制块标记，以便正确写入正文内容。
	 *
	 * @return {@link javax.servlet.jsp.tagext.Tag#EVAL_BODY_INCLUDE}
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		// 开始标签标签
		tagWriter.startTag(LABEL_TAG);
		// 写入 for 属性
		tagWriter.writeAttribute(FOR_ATTRIBUTE, resolveFor());
		// 写入默认属性
		writeDefaultAttributes(tagWriter);
		// 强制标签闭合
		tagWriter.forceBlock();
		// 设置标签写入器
		this.tagWriter = tagWriter;
		// 返回评估正文的结果
		return EVAL_BODY_INCLUDE;
	}

	/**
	 * 重写 {@code #getName()}，始终返回 {@code null}，因为 '{@code name}' 属性不受 '{@code label}' 标签支持。
	 *
	 * @return HTML '{@code name}' 属性的值
	 */
	@Override
	@Nullable
	protected String getName() throws JspException {
		// 这也会抑制 'id' 属性（对于 <label/> 来说没问题）
		return null;
	}

	/**
	 * 确定此标签的 '{@code for}' 属性值，如果未指定则自动生成一个。
	 *
	 * @see #getFor()
	 * @see #autogenerateFor()
	 */
	protected String resolveFor() throws JspException {
		// 如果 forId 不为空且包含文本
		if (StringUtils.hasText(this.forId)) {
			// 返回评估后的 forId
			return getDisplayString(evaluate(FOR_ATTRIBUTE, this.forId));
		} else {
			// 否则自动生成 forId
			return autogenerateFor();
		}
	}

	/**
	 * 为此标签自动生成 '{@code for}' 属性值。
	 * <p>默认实现委托给 {@link #getPropertyPath()}，删除无效字符（例如 "[" 或 "]"）。
	 */
	protected String autogenerateFor() throws JspException {
		return StringUtils.deleteAny(getPropertyPath(), "[]");
	}

	/**
	 * 关闭 '{@code label}' 标签。
	 */
	@Override
	public int doEndTag() throws JspException {
		Assert.state(this.tagWriter != null, "No TagWriter set");
		this.tagWriter.endTag();
		return EVAL_PAGE;
	}

	/**
	 * 处理 {@link TagWriter} 实例。
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		this.tagWriter = null;
	}

}
