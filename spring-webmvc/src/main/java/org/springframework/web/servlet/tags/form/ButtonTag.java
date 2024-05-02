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
import org.springframework.web.servlet.support.RequestDataValueProcessor;

import javax.servlet.jsp.JspException;

/**
 * {@code <button>} 标签在 HTML 'button' 标签中呈现表单字段标签。
 * 如果应用程序依赖于 {@link RequestDataValueProcessor}，则提供此标签以完整性。
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
 * <td><p>disabled</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性。将此属性的值设置为 'true' 将禁用 HTML 元素。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>name</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML button 标签的 name 属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>value</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML button 标签的 value 属性</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@SuppressWarnings("serial")
public class ButtonTag extends AbstractHtmlElementTag {

	/**
	 * '{@code disabled}' 属性的名称。
	 */
	public static final String DISABLED_ATTRIBUTE = "disabled";

	/**
	 * 标签写入器
	 */
	@Nullable
	private TagWriter tagWriter;

	/**
	 * 按钮名称
	 */
	@Nullable
	private String name;

	/**
	 * 按钮值
	 */
	@Nullable
	private String value;

	/**
	 * 是否禁止点击
	 */
	private boolean disabled;


	/**
	 * 设置 '{@code name}' 属性的值。
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 获取 '{@code name}' 属性的值。
	 */
	@Override
	@Nullable
	public String getName() {
		return this.name;
	}

	/**
	 * 设置 '{@code value}' 属性的值。
	 */
	public void setValue(@Nullable String value) {
		this.value = value;
	}

	/**
	 * 获取 '{@code value}' 属性的值。
	 */
	@Nullable
	public String getValue() {
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
	public boolean isDisabled() {
		return this.disabled;
	}


	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		// 开始按钮标签
		tagWriter.startTag("button");
		// 写入默认属性
		writeDefaultAttributes(tagWriter);
		// 写入 type 属性
		tagWriter.writeAttribute("type", getType());
		// 写入值
		writeValue(tagWriter);
		// 如果按钮被禁用
		if (isDisabled()) {
			// 写入 disabled 属性
			tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
		}
		// 强制标签闭合
		tagWriter.forceBlock();
		// 设置标签写入器
		this.tagWriter = tagWriter;
		// 返回评估正文的结果
		return EVAL_BODY_INCLUDE;
	}

	/**
	 * 将 '{@code value}' 属性写入提供的 {@link TagWriter}。
	 * 子类可以选择重写此实现以控制何时写入值。
	 */
	protected void writeValue(TagWriter tagWriter) throws JspException {
		String valueToUse = (getValue() != null ? getValue() : getDefaultValue());
		tagWriter.writeAttribute("value", processFieldValue(getName(), valueToUse, getType()));
	}

	/**
	 * 返回默认值。
	 *
	 * @return 如果未提供默认值，则返回默认值
	 */
	protected String getDefaultValue() {
		return "Submit";
	}

	/**
	 * 获取 '{@code type}' 属性的值。子类可以重写此方法以更改呈现的 '{@code input}' 元素的类型。默认值为 '{@code submit}'。
	 */
	protected String getType() {
		return "submit";
	}

	/**
	 * 关闭 '{@code button}' 块标签。
	 */
	@Override
	public int doEndTag() throws JspException {
		Assert.state(this.tagWriter != null, "No TagWriter set");
		this.tagWriter.endTag();
		return EVAL_PAGE;
	}

}
