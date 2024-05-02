/*
 * Copyright 2002-2013 the original author or authors.
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

import javax.servlet.jsp.JspException;

/**
 * {@code <hidden>} 标签使用绑定的值渲染 HTML 'input' 标签，类型为 'hidden'。
 *
 * <p>示例（绑定到表单后备对象的 'name' 属性）：
 * <pre class="code">
 * &lt;form:hidden path=&quot;name&quot;/&gt;
 * </pre>
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
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>启用/禁用渲染值的 HTML 转义。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>path</p></td>
 * <td><p>true</p></td>
 * <td><p>true</p></td>
 * <td><p>数据绑定的属性路径</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 2.0
 */
@SuppressWarnings("serial")
public class HiddenInputTag extends AbstractHtmlElementTag {

	/**
	 * '{@code disabled}' 属性的名称。
	 */
	public static final String DISABLED_ATTRIBUTE = "disabled";

	/**
	 * 是否禁止输入
	 */
	private boolean disabled;


	/**
	 * 设置 '{@code disabled}' 属性的值。
	 * 可能是运行时表达式。
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


	/**
	 * 将 "type" 标记为非法动态属性。
	 */
	@Override
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		return !"type".equals(localName);
	}

	/**
	 * 将 HTML '{@code input}' 标签写入提供的 {@link TagWriter}，包括数据绑定的值。
	 *
	 * @see #writeDefaultAttributes(TagWriter)
	 * @see #getBoundValue()
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		// 开始 input 标签
		tagWriter.startTag("input");
		// 写入默认属性
		writeDefaultAttributes(tagWriter);
		// 写入 type 属性
		tagWriter.writeAttribute("type", "hidden");
		// 如果字段被禁用
		if (isDisabled()) {
			// 写入 disabled 属性
			tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
		}
		// 获取绑定值并处理为字符串
		String value = getDisplayString(getBoundValue(), getPropertyEditor());
		// 写入 value 属性
		tagWriter.writeAttribute("value", processFieldValue(getName(), value, "hidden"));
		// 结束 input 标签
		tagWriter.endTag();
		// 返回跳过正文
		return SKIP_BODY;
	}

}
