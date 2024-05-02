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
 * 用于渲染 HTML 表单输入元素的支持数据绑定的 JSP 标签的基类。
 *
 * <p>提供了一组属性，这些属性对应于表单输入元素通用的 HTML 属性集。
 *
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractHtmlInputElementTag extends AbstractHtmlElementTag {

	/**
	 * '{@code onfocus}' 属性的名称。
	 */
	public static final String ONFOCUS_ATTRIBUTE = "onfocus";

	/**
	 * '{@code onblur}' 属性的名称。
	 */
	public static final String ONBLUR_ATTRIBUTE = "onblur";

	/**
	 * '{@code onchange}' 属性的名称。
	 */
	public static final String ONCHANGE_ATTRIBUTE = "onchange";

	/**
	 * '{@code accesskey}' 属性的名称。
	 */
	public static final String ACCESSKEY_ATTRIBUTE = "accesskey";

	/**
	 * '{@code disabled}' 属性的名称。
	 */
	public static final String DISABLED_ATTRIBUTE = "disabled";

	/**
	 * '{@code readonly}' 属性的名称。
	 */
	public static final String READONLY_ATTRIBUTE = "readonly";

	/**
	 *  聚焦事件
	 */
	@Nullable
	private String onfocus;

	/**
	 *离开输入框事件
	 */
	@Nullable
	private String onblur;

	/**
	 *元素值改变事件
	 */
	@Nullable
	private String onchange;

	/**
	 * 快捷键
	 */
	@Nullable
	private String accesskey;

	/**
	 * 是否禁用
	 */
	private boolean disabled;

	/**
	 * 是否只读
	 */
	private boolean readonly;


	/**
	 * 设置 '{@code onfocus}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnfocus(String onfocus) {
		this.onfocus = onfocus;
	}

	/**
	 * 获取 '{@code onfocus}' 属性的值。
	 */
	@Nullable
	protected String getOnfocus() {
		return this.onfocus;
	}

	/**
	 * 设置 '{@code onblur}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnblur(String onblur) {
		this.onblur = onblur;
	}

	/**
	 * 获取 '{@code onblur}' 属性的值。
	 */
	@Nullable
	protected String getOnblur() {
		return this.onblur;
	}

	/**
	 * 设置 '{@code onchange}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnchange(String onchange) {
		this.onchange = onchange;
	}

	/**
	 * 获取 '{@code onchange}' 属性的值。
	 */
	@Nullable
	protected String getOnchange() {
		return this.onchange;
	}

	/**
	 * 设置 '{@code accesskey}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setAccesskey(String accesskey) {
		this.accesskey = accesskey;
	}

	/**
	 * 获取 '{@code accesskey}' 属性的值。
	 */
	@Nullable
	protected String getAccesskey() {
		return this.accesskey;
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
	 * 设置 '{@code readonly}' 属性的值。
	 */
	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	/**
	 * 获取 '{@code readonly}' 属性的值。
	 */
	protected boolean isReadonly() {
		return this.readonly;
	}


	/**
	 * 添加由此基类定义的特定于输入的可选属性。
	 */
	@Override
	protected void writeOptionalAttributes(TagWriter tagWriter) throws JspException {
		// 调用父类的写入可选属性方法
		super.writeOptionalAttributes(tagWriter);

		// 写入可选的 onfocus 属性
		writeOptionalAttribute(tagWriter, ONFOCUS_ATTRIBUTE, getOnfocus());
		// 写入可选的 onblur 属性
		writeOptionalAttribute(tagWriter, ONBLUR_ATTRIBUTE, getOnblur());
		// 写入可选的 onchange 属性
		writeOptionalAttribute(tagWriter, ONCHANGE_ATTRIBUTE, getOnchange());
		// 写入可选的 accesskey 属性
		writeOptionalAttribute(tagWriter, ACCESSKEY_ATTRIBUTE, getAccesskey());
		// 如果字段被禁用
		if (isDisabled()) {
			// 写入禁用属性
			tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
		}
		// 如果字段为只读
		if (isReadonly()) {
			// 写入可选的 readonly 属性
			writeOptionalAttribute(tagWriter, READONLY_ATTRIBUTE, "readonly");
		}
	}

}
