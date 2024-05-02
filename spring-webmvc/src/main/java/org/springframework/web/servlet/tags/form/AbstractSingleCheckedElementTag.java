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

import javax.servlet.jsp.JspException;

/**
 * 抽象基类，提供用于实现数据绑定感知的 JSP 标签的常用方法，用于渲染一个带有类型为 '{@code checkbox}' 或 '{@code radio}' 的单个 HTML '{@code input}' 元素。
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
@SuppressWarnings("serial")
public abstract class AbstractSingleCheckedElementTag extends AbstractCheckedElementTag {

	/**
	 * '{@code value}' 属性的值。
	 */
	@Nullable
	private Object value;

	/**
	 * '{@code label}' 属性的值。
	 */
	@Nullable
	private Object label;


	/**
	 * 设置 '{@code value}' 属性的值。
	 * 可能是运行时表达式。
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 获取 '{@code value}' 属性的值。
	 */
	@Nullable
	protected Object getValue() {
		return this.value;
	}

	/**
	 * 设置 '{@code label}' 属性的值。
	 * 可能是运行时表达式。
	 */
	public void setLabel(Object label) {
		this.label = label;
	}

	/**
	 * 获取 '{@code label}' 属性的值。
	 */
	@Nullable
	protected Object getLabel() {
		return this.label;
	}


	/**
	 * 使用配置的 {@link #setValue(Object) value} 渲染 '{@code input(radio)}' 元素。
	 * 如果值与 {@link #getValue 绑定值} 匹配，则标记元素为已选中。
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		// 开始输入标签
		tagWriter.startTag("input");

		// 解析并设置id属性
		String id = resolveId();
		writeOptionalAttribute(tagWriter, "id", id);

		// 设置name属性
		writeOptionalAttribute(tagWriter, "name", getName());

		// 写入可选属性
		writeOptionalAttributes(tagWriter);

		// 写入标签的详细内容
		writeTagDetails(tagWriter);

		// 结束输入标签
		tagWriter.endTag();

		// 解析并设置标签的label属性
		Object resolvedLabel = evaluate("label", getLabel());
		if (resolvedLabel != null) {
			// 确保id属性已设置
			Assert.state(id != null, "Label id is required");
			// 开始label标签
			tagWriter.startTag("label");
			// 设置for属性
			tagWriter.writeAttribute("for", id);
			// 添加标签文本
			tagWriter.appendValue(convertToDisplayString(resolvedLabel));
			// 结束label标签
			tagWriter.endTag();
		}

		// 跳过标签主体
		return SKIP_BODY;
	}

	/**
	 * 为给定的主标签写入详细信息：
	 * 即特殊属性和标签的值。
	 */
	protected abstract void writeTagDetails(TagWriter tagWriter) throws JspException;

}
