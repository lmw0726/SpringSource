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
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.util.TagUtils;

import javax.servlet.jsp.JspException;

/**
 * {@code <options>} 标签呈现一系列 HTML 'option' 标签。
 * 根据绑定的值设置 'selected' 属性。
 *
 * <p><i>必须</i>在 {@link SelectTag 'select'} 标签内使用。
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
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>cssErrorClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性。当绑定字段有错误时使用。</p></td>
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
 * <td><p>启用/禁用呈现值的 HTML 转义。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>itemLabel</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>映射到 'option' 标签内部文本的属性名称</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>items</p></td>
 * <td><p>true</p></td>
 * <td><p>true</p></td>
 * <td><p>用于生成内部 'option' 标签的集合、映射或对象数组</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>itemValue</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>映射到 'option' 标签的 'value' 属性的属性名称</p></td>
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
 * </tbody>
 * </table>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Scott Andrews
 * @since 2.0
 */
@SuppressWarnings("serial")
public class OptionsTag extends AbstractHtmlElementTag {

	/**
	 * 用于生成内部 '{@code option}' 标签的 {@link java.util.Collection}、{@link java.util.Map} 或对象数组。
	 */
	@Nullable
	private Object items;

	/**
	 * 映射到 '{@code option}' 标签的 '{@code value}' 属性的属性名称。
	 */
	@Nullable
	private String itemValue;

	/**
	 * 映射到 '{@code option}' 标签的标签（内部文本）的属性名称。
	 */
	@Nullable
	private String itemLabel;

	/**
	 * 是否禁用
	 */
	private boolean disabled;


	/**
	 * 设置用于生成内部 '{@code option}' 标签的 {@link java.util.Collection}、{@link java.util.Map} 或对象数组。
	 * <p>希望从数组、{@link java.util.Collection} 或 {@link java.util.Map} 渲染 '{@code option}' 标签时需要。
	 * <p>通常是运行时表达式。
	 */
	public void setItems(Object items) {
		this.items = items;
	}

	/**
	 * 获取用于生成内部 '{@code option}' 标签的 {@link java.util.Collection}、{@link java.util.Map} 或对象数组。
	 * <p>通常是运行时表达式。
	 */
	@Nullable
	protected Object getItems() {
		return this.items;
	}

	/**
	 * 设置映射到 '{@code option}' 标签的 '{@code value}' 属性的属性名称。
	 * <p>希望从数组或 {@link java.util.Collection} 渲染 '{@code option}' 标签时需要。
	 */
	public void setItemValue(String itemValue) {
		Assert.hasText(itemValue, "'itemValue' must not be empty");
		this.itemValue = itemValue;
	}

	/**
	 * 返回映射到 '{@code option}' 标签的 '{@code value}' 属性的属性名称。
	 */
	@Nullable
	protected String getItemValue() {
		return this.itemValue;
	}

	/**
	 * 设置映射到 '{@code option}' 标签的标签（内部文本）的属性名称。
	 */
	public void setItemLabel(String itemLabel) {
		Assert.hasText(itemLabel, "'itemLabel' must not be empty");
		this.itemLabel = itemLabel;
	}

	/**
	 * 获取映射到 '{@code option}' 标签的标签（内部文本）的属性名称。
	 */
	@Nullable
	protected String getItemLabel() {
		return this.itemLabel;
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


	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		// 获取 SelectTag 对象
		SelectTag selectTag = getSelectTag();
		// 获取 items 对象
		Object items = getItems();
		// 初始化 itemsObject
		Object itemsObject = null;
		// 如果 items 不为空
		if (items != null) {
			// 如果 items 是字符串，则评估并赋值给 itemsObject
			itemsObject = (items instanceof String ? evaluate("items", items) : items);
		} else {
			// 否则，获取 selectTag 的绑定状态值类型
			Class<?> selectTagBoundType = selectTag.getBindStatus().getValueType();
			// 如果绑定状态值类型不为空且是枚举类型
			if (selectTagBoundType != null && selectTagBoundType.isEnum()) {
				// 获取枚举类型的所有常量并赋值给 itemsObject
				itemsObject = selectTagBoundType.getEnumConstants();
			}
		}
		// 如果 itemsObject 不为空
		if (itemsObject != null) {
			// 获取 select 标签的 name 属性值
			String selectName = selectTag.getName();
			// 获取 itemValue 和 itemLabel 属性值
			String itemValue = getItemValue();
			String itemLabel = getItemLabel();
			// 获取 valueProperty 和 labelProperty
			String valueProperty = (itemValue != null ? ObjectUtils.getDisplayString(evaluate("itemValue", itemValue)) : null);
			String labelProperty = (itemLabel != null ? ObjectUtils.getDisplayString(evaluate("itemLabel", itemLabel)) : null);
			// 创建 OptionsWriter 对象
			OptionsWriter optionWriter = new OptionsWriter(selectName, itemsObject, valueProperty, labelProperty);
			// 写入选项
			optionWriter.writeOptions(tagWriter);
		}
		// 返回跳过正文
		return SKIP_BODY;
	}

	/**
	 * 附加计数器到指定的 id，因为我们处理多个 HTML 元素。
	 */
	@Override
	protected String resolveId() throws JspException {
		// 评估 ID 属性
		Object id = evaluate("id", getId());
		// 如果 ID 不为空
		if (id != null) {
			// 将 ID 转换为字符串
			String idString = id.toString();
			// 如果 ID 字符串不为空，获取生成下一个ID，否则返回null。
			return (StringUtils.hasText(idString) ? TagIdGenerator.nextId(idString, this.pageContext) : null);
		}
		// 否则返回空
		return null;
	}

	private SelectTag getSelectTag() {
		TagUtils.assertHasAncestorOfType(this, SelectTag.class, "options", "select");
		return (SelectTag) findAncestorWithClass(this, SelectTag.class);
	}

	@Override
	protected BindStatus getBindStatus() {
		return (BindStatus) this.pageContext.getAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE);
	}


	/**
	 * 用于多个选项呈现的 OptionWriter 的内部类。
	 */
	private class OptionsWriter extends OptionWriter {

		/**
		 * 选中的选项名称
		 */
		@Nullable
		private final String selectName;

		public OptionsWriter(@Nullable String selectName, Object optionSource,
							 @Nullable String valueProperty, @Nullable String labelProperty) {

			super(optionSource, getBindStatus(), valueProperty, labelProperty, isHtmlEscape());
			this.selectName = selectName;
		}

		@Override
		protected boolean isOptionDisabled() throws JspException {
			return isDisabled();
		}

		@Override
		protected void writeCommonAttributes(TagWriter tagWriter) throws JspException {
			writeOptionalAttribute(tagWriter, "id", resolveId());
			writeOptionalAttributes(tagWriter);
		}

		@Override
		protected String processOptionValue(String value) {
			return processFieldValue(this.selectName, value, "option");
		}
	}

}
