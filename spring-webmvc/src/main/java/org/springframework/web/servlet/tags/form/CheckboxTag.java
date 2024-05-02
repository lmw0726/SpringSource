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

import org.springframework.web.bind.WebDataBinder;

import javax.servlet.jsp.JspException;
import java.util.Collection;

/**
 * {@code <checkbox>} 标签用于渲染带有类型为 'checkbox' 的 HTML 'input' 标签。
 * 可根据 {@link #getValue 绑定值} 的类型采用三种不同的方法之一。
 *
 * <h3>方法一</h3>
 * 当绑定值的类型为 {@link Boolean} 时，如果绑定值为 {@code true}，则 '{@code input(checkbox)}'
 * 将标记为 'checked'。 '{@code value}' 属性对应于 {@link #setValue(Object) value} 属性的解析值。
 * <h3>方法二</h3>
 * 当绑定值的类型为 {@link Collection} 时，如果配置的 {@link #setValue(Object) value} 存在于
 * 绑定的 {@link Collection} 中，则 '{@code input(checkbox)}' 将标记为 'checked'。
 * <h3>方法三</h3>
 * 对于任何其他绑定值类型，如果配置的 {@link #setValue(Object) value} 等于绑定值，则 '{@code input(checkbox)}'
 * 将标记为 'checked'。
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
 * <td><p>启用/禁用渲染值的 HTML 转义。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>label</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>作为标签一部分显示的值</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>lang</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
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
 * <td><p>path</p></td>
 * <td><p>true</p></td>
 * <td><p>true</p></td>
 * <td><p>数据绑定的属性路径</p></td>
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
 * <tr class="altColor">
 * <td><p>value</p></td>
 * <td><p>false</p></td>
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
public class CheckboxTag extends AbstractSingleCheckedElementTag {

	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		// 调用父类的写标签内容方法
		super.writeTagContent(tagWriter);

		// 如果字段不被禁用，则写入“字段存在”标记
		if (!isDisabled()) {
			// 开始写入隐藏字段
			tagWriter.startTag("input");
			// 设置输入类型为隐藏
			tagWriter.writeAttribute("type", "hidden");
			// 设置隐藏字段的名称
			String name = WebDataBinder.DEFAULT_FIELD_MARKER_PREFIX + getName();
			tagWriter.writeAttribute("name", name);
			// 设置隐藏字段的值为“on”
			tagWriter.writeAttribute("value", processFieldValue(name, "on", "hidden"));
			// 结束隐藏字段标签
			tagWriter.endTag();
		}

		// 返回SKIP_BODY，表示不处理标签体内容
		return SKIP_BODY;
	}

	@Override
	protected void writeTagDetails(TagWriter tagWriter) throws JspException {
		// 设置输入类型
		tagWriter.writeAttribute("type", getInputType());

		// 获取绑定的值和值的类型
		Object boundValue = getBoundValue();
		Class<?> valueType = getBindStatus().getValueType();

		// 如果值的类型是布尔类型或其包装类
		if (Boolean.class == valueType || boolean.class == valueType) {
			// 将值转换为布尔类型
			Boolean booleanValue = (boundValue instanceof String ? Boolean.valueOf((String) boundValue) : (Boolean) boundValue);
			// 渲染布尔值
			renderFromBoolean(booleanValue, tagWriter);
		} else {
			// 否则，获取标签的值
			Object value = getValue();
			// 如果值为null，则抛出异常
			if (value == null) {
				throw new IllegalArgumentException("Attribute 'value' is required when binding to non-boolean values");
			}
			// 解析值
			Object resolvedValue = (value instanceof String ? evaluate("value", value) : value);
			// 根据解析后的值渲染标签
			renderFromValue(resolvedValue, tagWriter);
		}
	}

	@Override
	protected String getInputType() {
		return "checkbox";
	}

}
