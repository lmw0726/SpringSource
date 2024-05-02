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
import java.util.Map;

/**
 * {@code <input>} 标签使用绑定值呈现 HTML 'input' 标签，类型为 'text'。
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
 * <td><p>accesskey</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>alt</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>autocomplete</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>常见可选属性</p></td>
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
 * <td><p>HTML 可选属性。当绑定字段有错误时使用。</p></td>
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
 * <td><p>启用/禁用渲染值的 HTML 转义。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>lang</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>maxlength</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onblur</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onchange</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
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
 * <td><p>onfocus</p></td>
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
 * <td><p>onselect</p></td>
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
 * <td><p>readonly</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性。将此属性的值设置为 'true' 将使 HTML 元素为只读。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>size</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
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
 * @author Rossen Stoyanchev
 * @since 2.0
 */
@SuppressWarnings("serial")
public class InputTag extends AbstractHtmlInputElementTag {

	public static final String SIZE_ATTRIBUTE = "size";

	public static final String MAXLENGTH_ATTRIBUTE = "maxlength";

	public static final String ALT_ATTRIBUTE = "alt";

	public static final String ONSELECT_ATTRIBUTE = "onselect";

	public static final String AUTOCOMPLETE_ATTRIBUTE = "autocomplete";

	/**
	 *
	 */
	@Nullable
	private String size;

	/**
	 * 最长长度
	 */
	@Nullable
	private String maxlength;

	/**
	 * 备选的文本
	 */
	@Nullable
	private String alt;

	/**
	 * 选中事件
	 */
	@Nullable
	private String onselect;

	/**
	 * 是否自动完成
	 */
	@Nullable
	private String autocomplete;


	/**
	 * 设置 '{@code size}' 属性的值。
	 * 可以是运行时表达式。
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
	 * 设置 '{@code maxlength}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setMaxlength(String maxlength) {
		this.maxlength = maxlength;
	}

	/**
	 * 获取 '{@code maxlength}' 属性的值。
	 */
	@Nullable
	protected String getMaxlength() {
		return this.maxlength;
	}

	/**
	 * 设置 '{@code alt}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setAlt(String alt) {
		this.alt = alt;
	}

	/**
	 * 获取 '{@code alt}' 属性的值。
	 */
	@Nullable
	protected String getAlt() {
		return this.alt;
	}

	/**
	 * 设置 '{@code onselect}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnselect(String onselect) {
		this.onselect = onselect;
	}

	/**
	 * 获取 '{@code onselect}' 属性的值。
	 */
	@Nullable
	protected String getOnselect() {
		return this.onselect;
	}

	/**
	 * 设置 '{@code autocomplete}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setAutocomplete(String autocomplete) {
		this.autocomplete = autocomplete;
	}

	/**
	 * 获取 '{@code autocomplete}' 属性的值。
	 */
	@Nullable
	protected String getAutocomplete() {
		return this.autocomplete;
	}


	/**
	 * 将 '{@code input}' 标签写入提供的 {@link TagWriter}。
	 * 使用 {@link #getType()} 返回的值确定渲染哪种类型的 '{@code input}' 元素。
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		// 开始 input 标签
		tagWriter.startTag("input");

		// 写入默认属性
		writeDefaultAttributes(tagWriter);

		// 获取动态属性
		Map<String, Object> attributes = getDynamicAttributes();
		// 如果属性为空或不包含 "type"
		if (attributes == null || !attributes.containsKey("type")) {
			// 写入 type 属性
			tagWriter.writeAttribute("type", getType());
		}

		// 写入值
		writeValue(tagWriter);

		// 写入可选的大小属性
		writeOptionalAttribute(tagWriter, SIZE_ATTRIBUTE, getSize());
		// 写入可选的最大长度属性
		writeOptionalAttribute(tagWriter, MAXLENGTH_ATTRIBUTE, getMaxlength());
		// 写入可选的 alt 属性
		writeOptionalAttribute(tagWriter, ALT_ATTRIBUTE, getAlt());
		// 写入可选的 onselect 属性
		writeOptionalAttribute(tagWriter, ONSELECT_ATTRIBUTE, getOnselect());
		// 写入可选的 autocomplete 属性
		writeOptionalAttribute(tagWriter, AUTOCOMPLETE_ATTRIBUTE, getAutocomplete());

		// 结束 input 标签
		tagWriter.endTag();

		// 返回跳过正文
		return SKIP_BODY;
	}

	/**
	 * 将 '{@code value}' 属性写入提供的 {@link TagWriter}。
	 * 子类可以选择重写此实现以控制何时确切地写入值。
	 */
	protected void writeValue(TagWriter tagWriter) throws JspException {
		// 获取绑定值的显示字符串
		String value = getDisplayString(getBoundValue(), getPropertyEditor());
		// 初始化类型变量
		String type = null;
		// 获取动态属性
		Map<String, Object> attributes = getDynamicAttributes();
		// 如果属性不为空
		if (attributes != null) {
			// 获取类型属性
			type = (String) attributes.get("type");
		}
		// 如果类型为空
		if (type == null) {
			// 获取默认类型
			type = getType();
		}
		// 写入值属性，处理字段值
		tagWriter.writeAttribute("value", processFieldValue(getName(), value, type));
	}

	/**
	 * 将 {@code type="checkbox"} 和 {@code type="radio"} 标记为非法的动态属性。
	 */
	@Override
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		return !("type".equals(localName) && ("checkbox".equals(value) || "radio".equals(value)));
	}

	/**
	 * 获取 '{@code type}' 属性的值。子类可以重写此方法以更改要呈现的 '{@code input}' 元素的类型。默认值为 '{@code text}'。
	 */
	protected String getType() {
		return "text";
	}

}
