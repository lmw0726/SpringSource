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

import javax.servlet.jsp.JspException;

/**
 * 抽象基类，提供用于实现数据绑定感知的 JSP 标签的通用方法，用于渲染 HTML '{@code input}'
 * 元素，类型为 '{@code checkbox}' 或 '{@code radio}'。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 2.5
 */
@SuppressWarnings("serial")
public abstract class AbstractCheckedElementTag extends AbstractHtmlInputElementTag {

	/**
	 * 使用提供的值渲染 '{@code input(checkbox)}'，如果提供的值与绑定值匹配，则将 '{@code input}' 元素标记为 'checked'。
	 */
	protected void renderFromValue(@Nullable Object value, TagWriter tagWriter) throws JspException {
		renderFromValue(value, value, tagWriter);
	}

	/**
	 * 使用提供的值渲染 '{@code input(checkbox)}'，如果提供的值与绑定值匹配，则将 '{@code input}' 元素标记为 'checked'。
	 */
	protected void renderFromValue(@Nullable Object item, @Nullable Object value, TagWriter tagWriter)
			throws JspException {

		// 将值转换为显示字符串
		String displayValue = convertToDisplayString(value);
		// 写入值属性，处理字段值
		tagWriter.writeAttribute("value", processFieldValue(getName(), displayValue, getInputType()));
		// 如果选项被选中，或者（值不等于选项且选项被选中）
		if (isOptionSelected(value) || (value != item && isOptionSelected(item))) {
			// 写入选中属性
			tagWriter.writeAttribute("checked", "checked");
		}
	}

	/**
	 * 通过委托给 {@link SelectedValueComparator#isSelected} 来确定提供的值是否与所选值匹配。
	 */
	private boolean isOptionSelected(@Nullable Object value) throws JspException {
		return SelectedValueComparator.isSelected(getBindStatus(), value);
	}

	/**
	 * 使用提供的布尔值渲染 '{@code input(checkbox)}'，如果布尔值为 {@code true}，则将 '{@code input}' 元素标记为 'checked'。
	 */
	protected void renderFromBoolean(Boolean boundValue, TagWriter tagWriter) throws JspException {
		// 写入值属性，处理字段值为 "true"
		tagWriter.writeAttribute("value", processFieldValue(getName(), "true", getInputType()));
		// 如果绑定的值为 true
		if (boundValue) {
			// 写入选中属性
			tagWriter.writeAttribute("checked", "checked");
		}
	}

	/**
	 * 在当前 PageContext 中为绑定的名称生成唯一 ID。
	 */
	@Override
	@Nullable
	protected String autogenerateId() throws JspException {
		String id = super.autogenerateId();
		return (id != null ? TagIdGenerator.nextId(id, this.pageContext) : null);
	}


	/**
	 * 将 '{@code input}' 元素写入提供的 {@link TagWriter}，如果适用，则标记为 'checked'。
	 */
	@Override
	protected abstract int writeTagContent(TagWriter tagWriter) throws JspException;

	/**
	 * 将 "type" 标记为非法的动态属性。
	 */
	@Override
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		return !"type".equals(localName);
	}

	/**
	 * 返回要生成的 HTML 输入元素的类型："checkbox" 或 "radio"。
	 */
	protected abstract String getInputType();

}
