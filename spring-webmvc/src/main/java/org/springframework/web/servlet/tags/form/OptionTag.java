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
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.util.TagUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;

/**
 * {@code <option>} 标签渲染单个 HTML 'option'。根据绑定值设置 'selected'。
 *
 * <p><b>必须嵌套在 {@link SelectTag} 内部。</b>
 *
 * <p>通过将 '{@code option}' 标记为 'selected'，提供完全支持数据绑定，
 * 如果 {@link #setValue value} 与外部 {@link SelectTag} 绑定的值匹配。
 *
 * <p>{@link #setValue value} 属性是必需的，对应于渲染的 '{@code option}' 的 '{@code value}' 属性。
 *
 * <p>可指定可选的 {@link #setLabel label} 属性，其值对应于渲染的 '{@code option}' 标签的内部文本。
 * 如果未指定 {@link #setLabel label}，则在渲染内部文本时将使用 {@link #setValue value} 属性。
 *
 * <p>
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th class="colFirst">属性</th>
 * <th class="colOne">是否必需?</th>
 * <th class="colOne">运行时表达式?</th>
 * <th class="colLast">描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="altColor">
 * <td><p>cssClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>cssErrorClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性。当绑定字段存在错误时使用。</p></td>
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
 * <td><p>disabled</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性。将此属性的值设置为 'true' 将禁用 HTML 元素。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>启用/禁用渲染值的 HTML 转义。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>label</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>lang</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>ondblclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onkeydown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onkeypress</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onkeyup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmousedown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmousemove</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmouseout</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmouseover</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmouseup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
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
 * <tr class="rowColor">
 * <td><p>value</p></td>
 * <td><p>true</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class OptionTag extends AbstractHtmlElementBodyTag implements BodyTag {

	/**
	 * 用于暴露此标签值的 JSP 变量的名称。
	 */
	public static final String VALUE_VARIABLE_NAME = "value";

	/**
	 * 用于暴露此标签显示值的 JSP 变量的名称。
	 */
	public static final String DISPLAY_VALUE_VARIABLE_NAME = "displayValue";

	/**
	 * '{@code selected}' 属性的名称。
	 */
	private static final String SELECTED_ATTRIBUTE = "selected";

	/**
	 * '{@code value}' 属性的名称。
	 */
	private static final String VALUE_ATTRIBUTE = VALUE_VARIABLE_NAME;

	/**
	 * '{@code disabled}' 属性的名称。
	 */
	private static final String DISABLED_ATTRIBUTE = "disabled";


	/**
	 * 渲染的 HTML {@code <option>} 标签的 'value' 属性。
	 */
	@Nullable
	private Object value;

	/**
	 * 渲染的 HTML {@code <option>} 标签的文本内容。
	 */
	@Nullable
	private String label;

	/**
	 * 旧的值
	 */
	@Nullable
	private Object oldValue;

	/**
	 * 旧的展示值
	 */
	@Nullable
	private Object oldDisplayValue;

	/**
	 * 是否禁止选中
	 */
	private boolean disabled;


	/**
	 * 设置渲染的 HTML {@code <option>} 标签的 'value' 属性。
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 获取渲染的 HTML {@code <option>} 标签的 'value' 属性。
	 */
	@Nullable
	protected Object getValue() {
		return this.value;
	}

	/**
	 * 设置 '{@code disabled}' 属性的值。
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * 获取 '{@code disabled}' 属性的值。
	 */
	protected boolean isDisabled() {
		return this.disabled;
	}

	/**
	 * 设置渲染的 HTML {@code <option>} 标签的文本内容。
	 * <p>可能是运行时表达式。
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * 获取渲染的 HTML {@code <option>} 标签的文本内容。
	 */
	@Nullable
	protected String getLabel() {
		return this.label;
	}


	@Override
	protected void renderDefaultContent(TagWriter tagWriter) throws JspException {
		// 获取值变量
		Object value = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
		// 获取标签值
		String label = getLabelValue(value);
		// 渲染选项
		renderOption(value, label, tagWriter);
	}

	@Override
	protected void renderFromBodyContent(BodyContent bodyContent, TagWriter tagWriter) throws JspException {
		// 获取值变量
		Object value = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
		// 获取标签值
		String label = bodyContent.getString();
		// 渲染选项
		renderOption(value, label, tagWriter);
	}

	/**
	 * 在继续之前确保我们处于 '{@code select}' 标签下。
	 */
	@Override
	protected void onWriteTagContent() {
		assertUnderSelectTag();
	}

	@Override
	protected void exposeAttributes() throws JspException {
		// 解析值
		Object value = resolveValue();
		// 保存旧值变量，并设置新的值变量
		this.oldValue = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
		this.pageContext.setAttribute(VALUE_VARIABLE_NAME, value);
		// 保存旧显示值变量，并设置新的显示值变量
		this.oldDisplayValue = this.pageContext.getAttribute(DISPLAY_VALUE_VARIABLE_NAME);
		this.pageContext.setAttribute(DISPLAY_VALUE_VARIABLE_NAME, getDisplayString(value, getBindStatus().getEditor()));
	}

	@Override
	protected BindStatus getBindStatus() {
		return (BindStatus) this.pageContext.getAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE);
	}

	@Override
	protected void removeAttributes() {
		// 如果旧值不为空
		if (this.oldValue != null) {
			// 将旧值设置回页面上下文
			this.pageContext.setAttribute(VALUE_ATTRIBUTE, this.oldValue);
			// 将旧值设为 null
			this.oldValue = null;
		} else {
			// 否则移除值变量
			this.pageContext.removeAttribute(VALUE_VARIABLE_NAME);
		}

		// 如果旧显示值不为空
		if (this.oldDisplayValue != null) {
			// 将旧显示值设置回页面上下文
			this.pageContext.setAttribute(DISPLAY_VALUE_VARIABLE_NAME, this.oldDisplayValue);
			// 将旧显示值设为 null
			this.oldDisplayValue = null;
		} else {
			// 否则移除显示值变量
			this.pageContext.removeAttribute(DISPLAY_VALUE_VARIABLE_NAME);
		}
	}

	private void renderOption(Object value, String label, TagWriter tagWriter) throws JspException {
		// 开始 option 标签
		tagWriter.startTag("option");
		// 写入可选的 id 属性
		writeOptionalAttribute(tagWriter, "id", resolveId());
		// 写入可选的属性
		writeOptionalAttributes(tagWriter);
		// 获取渲染值并处理为字符串
		String renderedValue = getDisplayString(value, getBindStatus().getEditor());
		// 处理字段值并设置为 option 标签的 value 属性
		renderedValue = processFieldValue(getSelectTag().getName(), renderedValue, "option");
		tagWriter.writeAttribute(VALUE_ATTRIBUTE, renderedValue);
		// 如果值被选中
		if (isSelected(value)) {
			// 写入选中属性
			tagWriter.writeAttribute(SELECTED_ATTRIBUTE, SELECTED_ATTRIBUTE);
		}
		// 如果字段被禁用
		if (isDisabled()) {
			// 写入禁用属性
			tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
		}
		// 追加标签值
		tagWriter.appendValue(label);
		// 结束 option 标签
		tagWriter.endTag();
	}

	@Override
	protected String autogenerateId() throws JspException {
		return null;
	}

	/**
	 * 返回此 '{@code option}' 元素的标签值。
	 * <p>如果设置了 {@link #setLabel label} 属性，则使用该属性的解析值，
	 * 否则使用 {@code resolvedValue} 参数的值。
	 *
	 * @param resolvedValue 解析后的值
	 * @return 此 '{@code option}' 元素的标签值
	 * @throws JspException 如果无法获取标签值
	 */
	private String getLabelValue(Object resolvedValue) throws JspException {
		// 获取标签
		String label = getLabel();
		// 如果标签为空
		Object labelObj = (label == null ? resolvedValue : evaluate("label", label));
		// 返回标签的显示字符串
		return getDisplayString(labelObj, getBindStatus().getEditor());
	}

	private void assertUnderSelectTag() {
		TagUtils.assertHasAncestorOfType(this, SelectTag.class, "option", "select");
	}

	private SelectTag getSelectTag() {
		return (SelectTag) findAncestorWithClass(this, SelectTag.class);
	}

	private boolean isSelected(Object resolvedValue) {
		return SelectedValueComparator.isSelected(getBindStatus(), resolvedValue);
	}

	@Nullable
	private Object resolveValue() throws JspException {
		return evaluate(VALUE_VARIABLE_NAME, getValue());
	}

}
