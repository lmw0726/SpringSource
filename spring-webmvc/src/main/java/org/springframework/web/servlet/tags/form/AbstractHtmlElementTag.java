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
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据绑定感知 JSP 标签的基础类，用于呈现 HTML 元素。提供了一组属性，对应于一组在元素之间通用的 HTML 属性。
 *
 * <p>此外，此基础类允许在标签输出的一部分中呈现非标准属性。如果需要，这些属性可通过 {@link AbstractHtmlElementTag#getDynamicAttributes() dynamicAttributes} 映射在子类中访问。
 *
 * @author Rob Harrop
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractHtmlElementTag extends AbstractDataBoundFormElementTag implements DynamicAttributes {

	public static final String CLASS_ATTRIBUTE = "class";

	public static final String STYLE_ATTRIBUTE = "style";

	public static final String LANG_ATTRIBUTE = "lang";

	public static final String TITLE_ATTRIBUTE = "title";

	public static final String DIR_ATTRIBUTE = "dir";

	public static final String TABINDEX_ATTRIBUTE = "tabindex";

	public static final String ONCLICK_ATTRIBUTE = "onclick";

	public static final String ONDBLCLICK_ATTRIBUTE = "ondblclick";

	public static final String ONMOUSEDOWN_ATTRIBUTE = "onmousedown";

	public static final String ONMOUSEUP_ATTRIBUTE = "onmouseup";

	public static final String ONMOUSEOVER_ATTRIBUTE = "onmouseover";

	public static final String ONMOUSEMOVE_ATTRIBUTE = "onmousemove";

	public static final String ONMOUSEOUT_ATTRIBUTE = "onmouseout";

	public static final String ONKEYPRESS_ATTRIBUTE = "onkeypress";

	public static final String ONKEYUP_ATTRIBUTE = "onkeyup";

	public static final String ONKEYDOWN_ATTRIBUTE = "onkeydown";


	/**
	 * css class属性值
	 */
	@Nullable
	private String cssClass;

	/**
	 * 当与特定标签绑定的字段存在错误时使用的 CSS 类。
	 */
	@Nullable
	private String cssErrorClass;

	/**
	 * CSS '{@code style}' 属性的值。
	 */
	@Nullable
	private String cssStyle;

	/**
	 * '{@code lang}' 属性的值。
	 */
	@Nullable
	private String lang;

	/**
	 * '{@code title}' 属性的值。
	 */
	@Nullable
	private String title;

	/**
	 * '{@code dir}' 属性的值。
	 */
	@Nullable
	private String dir;

	/**
	 * '{@code tabindex}' 属性的值。
	 */
	@Nullable
	private String tabindex;

	/**
	 * '{@code onclick}' 属性的值。
	 */
	@Nullable
	private String onclick;

	/**
	 * '{@code ondblclick}' 属性的值。
	 */
	@Nullable
	private String ondblclick;

	/**
	 * '{@code onmousedown}' 属性的值。
	 */
	@Nullable
	private String onmousedown;

	/**
	 * '{@code onmouseup}' 属性的值。
	 */
	@Nullable
	private String onmouseup;

	/**
	 * '{@code onmouseover}' 属性的值。
	 */
	@Nullable
	private String onmouseover;

	/**
	 * '{@code onmousemove}' 属性的值。
	 */
	@Nullable
	private String onmousemove;

	/**
	 * '{@code onmouseout}' 属性的值。
	 */
	@Nullable
	private String onmouseout;

	/**
	 * '{@code onkeypress}' 属性的值。
	 */
	@Nullable
	private String onkeypress;

	/**
	 * '{@code onkeyup}' 属性的值。
	 */
	@Nullable
	private String onkeyup;

	/**
	 * '{@code onkeydown}' 属性的值。
	 */
	@Nullable
	private String onkeydown;

	/**
	 * 动态属性映射。
	 */
	@Nullable
	private Map<String, Object> dynamicAttributes;


	/**
	 * 设置 '{@code class}' 属性的值。可能是运行时表达式。
	 */
	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}

	/**
	 * 获取 '{@code class}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getCssClass() {
		return this.cssClass;
	}

	/**
	 * 当与特定标签绑定的字段存在错误时使用的 CSS 类。可能是运行时表达式。
	 */
	public void setCssErrorClass(String cssErrorClass) {
		this.cssErrorClass = cssErrorClass;
	}

	/**
	 * 当与特定标签绑定的字段存在错误时使用的 CSS 类。可能是运行时表达式。
	 */
	@Nullable
	protected String getCssErrorClass() {
		return this.cssErrorClass;
	}

	/**
	 * 设置 '{@code style}' 属性的值。可能是运行时表达式。
	 */
	public void setCssStyle(String cssStyle) {
		this.cssStyle = cssStyle;
	}

	/**
	 * 获取 '{@code style}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getCssStyle() {
		return this.cssStyle;
	}

	/**
	 * 设置 '{@code lang}' 属性的值。可能是运行时表达式。
	 */
	public void setLang(String lang) {
		this.lang = lang;
	}

	/**
	 * 获取 '{@code lang}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getLang() {
		return this.lang;
	}

	/**
	 * 设置 '{@code title}' 属性的值。可能是运行时表达式。
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * 获取 '{@code title}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getTitle() {
		return this.title;
	}

	/**
	 * 设置 '{@code dir}' 属性的值。可能是运行时表达式。
	 */
	public void setDir(String dir) {
		this.dir = dir;
	}

	/**
	 * 获取 '{@code dir}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getDir() {
		return this.dir;
	}

	/**
	 * 设置 '{@code tabindex}' 属性的值。可能是运行时表达式。
	 */
	public void setTabindex(String tabindex) {
		this.tabindex = tabindex;
	}

	/**
	 * 获取 '{@code tabindex}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getTabindex() {
		return this.tabindex;
	}

	/**
	 * 设置 '{@code onclick}' 属性的值。可能是运行时表达式。
	 */
	public void setOnclick(String onclick) {
		this.onclick = onclick;
	}

	/**
	 * 获取 '{@code onclick}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getOnclick() {
		return this.onclick;
	}

	/**
	 * 设置 '{@code ondblclick}' 属性的值。可能是运行时表达式。
	 */
	public void setOndblclick(String ondblclick) {
		this.ondblclick = ondblclick;
	}

	/**
	 * 获取 '{@code ondblclick}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getOndblclick() {
		return this.ondblclick;
	}

	/**
	 * 设置 '{@code onmousedown}' 属性的值。可能是运行时表达式。
	 */
	public void setOnmousedown(String onmousedown) {
		this.onmousedown = onmousedown;
	}

	/**
	 * 获取 '{@code onmousedown}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getOnmousedown() {
		return this.onmousedown;
	}

	/**
	 * 设置 '{@code onmouseup}' 属性的值。可能是运行时表达式。
	 */
	public void setOnmouseup(String onmouseup) {
		this.onmouseup = onmouseup;
	}

	/**
	 * 获取 '{@code onmouseup}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getOnmouseup() {
		return this.onmouseup;
	}

	/**
	 * 设置 '{@code onmouseover}' 属性的值。可能是运行时表达式。
	 */
	public void setOnmouseover(String onmouseover) {
		this.onmouseover = onmouseover;
	}

	/**
	 * 获取 '{@code onmouseover}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getOnmouseover() {
		return this.onmouseover;
	}

	/**
	 * 设置 '{@code onmousemove}' 属性的值。可能是运行时表达式。
	 */
	public void setOnmousemove(String onmousemove) {
		this.onmousemove = onmousemove;
	}

	/**
	 * 获取 '{@code onmousemove}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getOnmousemove() {
		return this.onmousemove;
	}

	/**
	 * 设置 '{@code onmouseout}' 属性的值。可能是运行时表达式。
	 */
	public void setOnmouseout(String onmouseout) {
		this.onmouseout = onmouseout;
	}

	/**
	 * 获取 '{@code onmouseout}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getOnmouseout() {
		return this.onmouseout;
	}

	/**
	 * 设置 '{@code onkeypress}' 属性的值。可能是运行时表达式。
	 */
	public void setOnkeypress(String onkeypress) {
		this.onkeypress = onkeypress;
	}

	/**
	 * 获取 '{@code onkeypress}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getOnkeypress() {
		return this.onkeypress;
	}

	/**
	 * 设置 '{@code onkeyup}' 属性的值。可能是运行时表达式。
	 */
	public void setOnkeyup(String onkeyup) {
		this.onkeyup = onkeyup;
	}

	/**
	 * 获取 '{@code onkeyup}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getOnkeyup() {
		return this.onkeyup;
	}

	/**
	 * 设置 '{@code onkeydown}' 属性的值。可能是运行时表达式。
	 */
	public void setOnkeydown(String onkeydown) {
		this.onkeydown = onkeydown;
	}

	/**
	 * 获取 '{@code onkeydown}' 属性的值。可能是运行时表达式。
	 */
	@Nullable
	protected String getOnkeydown() {
		return this.onkeydown;
	}

	/**
	 * 获取动态属性映射。
	 */
	@Nullable
	protected Map<String, Object> getDynamicAttributes() {
		return this.dynamicAttributes;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDynamicAttribute(String uri, String localName, Object value) throws JspException {
		if (this.dynamicAttributes == null) {
			// 如果动态属性映射为空，设置为一个空的HashMap
			this.dynamicAttributes = new HashMap<>();
		}
		if (!isValidDynamicAttribute(localName, value)) {
			// 如果不是有效的动态属性，则抛出异常
			throw new IllegalArgumentException(
					"Attribute " + localName + "=\"" + value + "\" is not allowed");
		}
		// 将名称和值添加动态属性映射中
		this.dynamicAttributes.put(localName, value);
	}

	/**
	 * 给定的名称-值对是否是有效的动态属性。
	 */
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		return true;
	}

	/**
	 * 将配置的默认属性写入提供的 {@link TagWriter}。
	 * 当子类想要将基础属性集写入输出时，应调用此方法。
	 */
	@Override
	protected void writeDefaultAttributes(TagWriter tagWriter) throws JspException {
		// 调用父类的方法
		super.writeDefaultAttributes(tagWriter);
		// 写入可选属性
		writeOptionalAttributes(tagWriter);
	}

	/**
	 * 将配置的可选属性写入提供的 {@link TagWriter}。
	 * 将渲染的可选属性集包括任何非标准动态属性。
	 * {@link #writeDefaultAttributes(TagWriter)} 调用此方法。
	 */
	protected void writeOptionalAttributes(TagWriter tagWriter) throws JspException {
		// 写入可选的 CLASS 属性值
		tagWriter.writeOptionalAttributeValue(CLASS_ATTRIBUTE, resolveCssClass());
		// 写入可选的 STYLE 属性值
		tagWriter.writeOptionalAttributeValue(STYLE_ATTRIBUTE, ObjectUtils.getDisplayString(evaluate("cssStyle", getCssStyle())));
		// 写入可选的 LANG 属性
		writeOptionalAttribute(tagWriter, LANG_ATTRIBUTE, getLang());
		// 写入可选的 TITLE 属性
		writeOptionalAttribute(tagWriter, TITLE_ATTRIBUTE, getTitle());
		// 写入可选的 DIR 属性
		writeOptionalAttribute(tagWriter, DIR_ATTRIBUTE, getDir());
		// 写入可选的 TABINDEX 属性
		writeOptionalAttribute(tagWriter, TABINDEX_ATTRIBUTE, getTabindex());
		// 写入可选的 ONCLICK 属性
		writeOptionalAttribute(tagWriter, ONCLICK_ATTRIBUTE, getOnclick());
		// 写入可选的 ONDBLCLICK 属性
		writeOptionalAttribute(tagWriter, ONDBLCLICK_ATTRIBUTE, getOndblclick());
		// 写入可选的 ONMOUSEDOWN 属性
		writeOptionalAttribute(tagWriter, ONMOUSEDOWN_ATTRIBUTE, getOnmousedown());
		// 写入可选的 ONMOUSEUP 属性
		writeOptionalAttribute(tagWriter, ONMOUSEUP_ATTRIBUTE, getOnmouseup());
		// 写入可选的 ONMOUSEOVER 属性
		writeOptionalAttribute(tagWriter, ONMOUSEOVER_ATTRIBUTE, getOnmouseover());
		// 写入可选的 ONMOUSEMOVE 属性
		writeOptionalAttribute(tagWriter, ONMOUSEMOVE_ATTRIBUTE, getOnmousemove());
		// 写入可选的 ONMOUSEOUT 属性
		writeOptionalAttribute(tagWriter, ONMOUSEOUT_ATTRIBUTE, getOnmouseout());
		// 写入可选的 ONKEYPRESS 属性
		writeOptionalAttribute(tagWriter, ONKEYPRESS_ATTRIBUTE, getOnkeypress());
		// 写入可选的 ONKEYUP 属性
		writeOptionalAttribute(tagWriter, ONKEYUP_ATTRIBUTE, getOnkeyup());
		// 写入可选的 ONKEYDOWN 属性
		writeOptionalAttribute(tagWriter, ONKEYDOWN_ATTRIBUTE, getOnkeydown());

		// 如果动态属性不为空
		if (!CollectionUtils.isEmpty(this.dynamicAttributes)) {
			// 遍历动态属性集合
			for (Map.Entry<String, Object> entry : this.dynamicAttributes.entrySet()) {
				// 写入动态属性值
				tagWriter.writeOptionalAttributeValue(entry.getKey(), getDisplayString(entry.getValue()));
			}
		}
	}

	/**
	 * 根据当前 {@link org.springframework.web.servlet.support.BindStatus} 对象的状态获取要使用的适当 CSS 类。
	 */
	protected String resolveCssClass() throws JspException {
		// 如果绑定状态为错误且 CSS 错误类不为空
		if (getBindStatus().isError() && StringUtils.hasText(getCssErrorClass())) {
			// 返回评估后的 CSS 错误类
			return ObjectUtils.getDisplayString(evaluate("cssErrorClass", getCssErrorClass()));
		} else {
			// 否则返回评估后的 CSS 类
			return ObjectUtils.getDisplayString(evaluate("cssClass", getCssClass()));
		}
	}

}
