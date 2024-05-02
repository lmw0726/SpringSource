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
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.support.BindStatus;

import javax.servlet.jsp.JspException;
import java.util.Collection;
import java.util.Map;

/**
 * {@code <select>} 标签用于渲染 HTML 的 'select' 元素。
 * 支持将选定的选项绑定到数据。
 *
 * <p>内部 '{@code option}' 标签可以使用 OptionWriter 类支持的一种方法来呈现。
 *
 * <p>还支持使用嵌套的 {@link OptionTag OptionTags} 或（通常是一个）嵌套的 {@link OptionsTag}。
 *
 * <p>
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th class="colFirst">属性</th>
 * <th class="colOne">是否必需？</th>
 * <th class="colOne">是否运行时表达式？</th>
 * <th class="colLast">描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="altColor">
 * <td><p>accesskey</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>cssClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>cssErrorClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性。当绑定字段存在错误时使用。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>cssStyle</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>dir</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>disabled</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性。将此属性的值设置为 'true' 将禁用 HTML 元素。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>启用/禁用呈现值的 HTML 转义。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>itemLabel</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>映射到 'option' 标签的内部文本的属性名称</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>items</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>用于生成内部 'option' 标签的 Collection、Map 或对象数组</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>itemValue</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>映射到 'option' 标签的 'value' 属性的属性名称</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>lang</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>multiple</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onblur</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onchange</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
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
 * <td><p>onfocus</p></td>
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
 * <td><p>数据绑定的属性路径</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>size</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>tabindex</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>title</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @see OptionTag
 * @since 2.0
 */
@SuppressWarnings("serial")
public class SelectTag extends AbstractHtmlInputElementTag {

	/**
	 * {@link javax.servlet.jsp.PageContext} 属性，将绑定值暴露给内部 {@link OptionTag OptionTags}。
	 */
	public static final String LIST_VALUE_PAGE_ATTRIBUTE =
			"org.springframework.web.servlet.tags.form.SelectTag.listValue";

	/**
	 * 标记对象，用于指示已指定但解析为 null 的项目。
	 * 允许区分 '已设置但为 null' 和 '根本未设置'。
	 */
	private static final Object EMPTY = new Object();


	/**
	 * 用于生成内部 '{@code option}' 标签的 {@link Collection}、{@link Map} 或对象数组。
	 */
	@Nullable
	private Object items;

	/**
	 * 映射到 '{@code option}' 标签的 '{@code value}' 属性的属性名称。
	 */
	@Nullable
	private String itemValue;

	/**
	 * 映射到 '{@code option}' 标签的内部文本的属性名称。
	 */
	@Nullable
	private String itemLabel;

	/**
	 * 在最终 '{@code select}' 元素上呈现的 HTML '{@code size}' 属性的值。
	 */
	@Nullable
	private String size;

	/**
	 * 指示 '{@code select}' 标签是否允许多选。
	 */
	@Nullable
	private Object multiple;

	/**
	 * 正在写入输出的 {@link TagWriter} 实例。
	 * <p>仅与嵌套的 {@link OptionTag OptionTags} 一起使用。
	 */
	@Nullable
	private TagWriter tagWriter;


	/**
	 * 设置用于生成内部 '{@code option}' 标签的 {@link Collection}、{@link Map} 或对象数组。
	 * <p>希望从数组、{@link Collection} 或 {@link Map} 渲染 '{@code option}' 标签时需要此属性。
	 * <p>通常是运行时表达式。
	 *
	 * @param items 组成此选择项的选项
	 */
	public void setItems(@Nullable Object items) {
		this.items = (items != null ? items : EMPTY);
	}

	/**
	 * 获取 '{@code items}' 属性的值。
	 * <p>可能是运行时表达式。
	 */
	@Nullable
	protected Object getItems() {
		return this.items;
	}

	/**
	 * 设置映射到 '{@code option}' 标签的 '{@code value}' 属性的属性名称。
	 * <p>希望从数组或 {@link Collection} 渲染 '{@code option}' 标签时需要此属性。
	 * <p>可能是运行时表达式。
	 */
	public void setItemValue(String itemValue) {
		this.itemValue = itemValue;
	}

	/**
	 * 获取 '{@code itemValue}' 属性的值。
	 * <p>可能是运行时表达式。
	 */
	@Nullable
	protected String getItemValue() {
		return this.itemValue;
	}

	/**
	 * 设置映射到 '{@code option}' 标签的标签（内部文本）的属性名称。
	 * <p>可能是运行时表达式。
	 */
	public void setItemLabel(String itemLabel) {
		this.itemLabel = itemLabel;
	}

	/**
	 * 获取 '{@code itemLabel}' 属性的值。
	 * <p>可能是运行时表达式。
	 */
	@Nullable
	protected String getItemLabel() {
		return this.itemLabel;
	}

	/**
	 * 设置在最终 '{@code select}' 元素上呈现的 HTML '{@code size}' 属性的值。
	 */
	public void setSize(String size) {
		this.size = size;
	}

	/**
	 * 获取 '{@code size}' 属性的值。
	 */
	@Nullable
	protected String getSize() {
		return this.size;
	}

	/**
	 * 设置在最终 '{@code select}' 元素上呈现的 HTML '{@code multiple}' 属性的值。
	 */
	public void setMultiple(Object multiple) {
		this.multiple = multiple;
	}

	/**
	 * 获取在最终 '{@code select}' 元素上呈现的 HTML '{@code multiple}' 属性的值。
	 */
	@Nullable
	protected Object getMultiple() {
		return this.multiple;
	}


	/**
	 * 将 HTML '{@code select}' 标签渲染到提供的 {@link TagWriter}。
	 * <p>如果设置了 {@link #setItems items} 属性，则渲染嵌套的 '{@code option}' 标签，
	 * 否则将绑定值暴露给嵌套的 {@link OptionTag OptionTags}。
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		// 开始 select 标签
		tagWriter.startTag("select");

		// 写入默认属性
		writeDefaultAttributes(tagWriter);

		// 如果是多选
		if (isMultiple()) {
			// 写入多选属性
			tagWriter.writeAttribute("multiple", "multiple");
		}

		// 写入可选的 size 属性
		tagWriter.writeOptionalAttributeValue("size", getDisplayString(evaluate("size", getSize())));

		// 获取 items
		Object items = getItems();
		// 如果 items 不为空
		if (items != null) {
			// Items 指定，但可能为空...
			if (items != EMPTY) {
				// 评估 items
				Object itemsObject = evaluate("items", items);
				// 如果 itemsObject 不为空
				if (itemsObject != null) {
					// 获取 select 的名称
					final String selectName = getName();
					// 获取值属性
					String valueProperty = (getItemValue() != null ?
							ObjectUtils.getDisplayString(evaluate("itemValue", getItemValue())) : null);
					// 获取标签属性
					String labelProperty = (getItemLabel() != null ?
							ObjectUtils.getDisplayString(evaluate("itemLabel", getItemLabel())) : null);
					// 创建 OptionWriter
					OptionWriter optionWriter =
							new OptionWriter(itemsObject, getBindStatus(), valueProperty, labelProperty, isHtmlEscape()) {
								@Override
								protected String processOptionValue(String resolvedValue) {
									// 处理选项值
									return processFieldValue(selectName, resolvedValue, "option");
								}
							};
					// 写入选项
					optionWriter.writeOptions(tagWriter);
				}
			}
			// 结束标签
			tagWriter.endTag(true);
			// 如果需要，写入隐藏标签
			writeHiddenTagIfNecessary(tagWriter);
			// 跳过正文
			return SKIP_BODY;
		} else {
			// 使用嵌套的 <form:option/> 标签，所以只需在 PageContext 中公开值...
			// 强制块级标签
			tagWriter.forceBlock();
			// 设置标签写入器
			this.tagWriter = tagWriter;
			// 在 PageContext 中设置列表值
			this.pageContext.setAttribute(LIST_VALUE_PAGE_ATTRIBUTE, getBindStatus());
			// 包含正文
			return EVAL_BODY_INCLUDE;
		}
	}

	/**
	 * 如果使用多选，则需要一个隐藏元素来确保在响应{@code null}提交时服务器端正确取消选择所有项目。
	 */
	private void writeHiddenTagIfNecessary(TagWriter tagWriter) throws JspException {
		// 如果是多选
		if (isMultiple()) {
			// 开始隐藏字段标签
			tagWriter.startTag("input");
			// 写入类型属性
			tagWriter.writeAttribute("type", "hidden");
			// 构建名称
			String name = WebDataBinder.DEFAULT_FIELD_MARKER_PREFIX + getName();
			// 写入名称属性
			tagWriter.writeAttribute("name", name);
			// 写入值属性，处理字段值
			tagWriter.writeAttribute("value", processFieldValue(name, "1", "hidden"));
			// 结束隐藏字段标签
			tagWriter.endTag();
		}
	}

	private boolean isMultiple() throws JspException {
		// 获取多选属性值
		Object multiple = getMultiple();
		// 如果多选属性不为空
		if (multiple != null) {
			// 将多选属性转换为字符串
			String stringValue = multiple.toString();
			// 如果字符串为 "multiple" 或解析为布尔值为 true，则返回 true
			return ("multiple".equalsIgnoreCase(stringValue) || Boolean.parseBoolean(stringValue));
		}
		// 否则，强制多选
		return forceMultiple();
	}

	/**
	 * 如果绑定值要求生成的 '{@code select}' 标签是多选的，则返回 '{@code true}'。
	 */
	private boolean forceMultiple() throws JspException {
		// 获取绑定状态
		BindStatus bindStatus = getBindStatus();
		// 获取值类型
		Class<?> valueType = bindStatus.getValueType();
		// 如果值类型不为空，并且类型需要多选
		if (valueType != null && typeRequiresMultiple(valueType)) {
			return true;
		} else if (bindStatus.getEditor() != null) {
			// 获取编辑器的值
			Object editorValue = bindStatus.getEditor().getValue();
			// 如果编辑器的值不为空，并且类型需要多选
			if (editorValue != null && typeRequiresMultiple(editorValue.getClass())) {
				return true;
			}
		}
		// 否则，返回 false
		return false;
	}

	/**
	 * 对于数组、{@link Collection} 和 {@link Map}，返回 '{@code true}'。
	 */
	private static boolean typeRequiresMultiple(Class<?> type) {
		return (type.isArray() || Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type));
	}

	/**
	 * 当使用嵌套的 {@link OptionTag options} 时，关闭可能已打开的任何块标签。
	 */
	@Override
	public int doEndTag() throws JspException {
		if (this.tagWriter != null) {
			this.tagWriter.endTag();
			writeHiddenTagIfNecessary(this.tagWriter);
		}
		return EVAL_PAGE;
	}

	/**
	 * 清除可能在使用嵌套的 {@link OptionTag options} 时遗留下来的 {@link TagWriter}。
	 */
	@Override
	public void doFinally() {
		// 调用父类的 doFinally 方法
		super.doFinally();
		// 清空标签写入器
		this.tagWriter = null;
		// 从页面上下文中移除列表值属性
		this.pageContext.removeAttribute(LIST_VALUE_PAGE_ATTRIBUTE);
	}

}
