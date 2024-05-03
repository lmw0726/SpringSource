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
import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;

import javax.servlet.jsp.JspException;
import java.beans.PropertyEditor;

/**
 * 所有 JSP 表单标签的基类。提供用于空安全 EL 评估以及访问和使用 {@link TagWriter} 的实用方法。
 *
 * <p>子类应该实现 {@link #writeTagContent(TagWriter)} 方法来执行实际的标签渲染。
 *
 * <p>子类（或测试类）可以重写 {@link #createTagWriter()} 方法来将输出重定向到与当前 {@link javax.servlet.jsp.PageContext} 关联的 {@link javax.servlet.jsp.JspWriter} 之外的 {@link java.io.Writer}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractFormTag extends HtmlEscapingAwareTag {

	/**
	 * 评估提供的属性名称的提供的值。
	 * <p>默认实现只是返回给定的值。
	 */
	@Nullable
	protected Object evaluate(String attributeName, @Nullable Object value) throws JspException {
		return value;
	}

	/**
	 * 可选地将提供的值在提供的属性名称下写入提供的 {@link TagWriter}。
	 * 在这种情况下，首先对提供的值进行 {@link #evaluate 评估}，然后将 {@link ObjectUtils#getDisplayString 字符串表示} 写入作为属性值。
	 * 如果结果的 {@code String} 表示为 {@code null} 或空，则不写入属性。
	 *
	 * @see TagWriter#writeOptionalAttributeValue(String, String)
	 */
	protected final void writeOptionalAttribute(TagWriter tagWriter, String attributeName, @Nullable String value)
			throws JspException {

		if (value != null) {
			// 如果值不为空，则将其转换为字符串，并写入标签的可选属性值。
			tagWriter.writeOptionalAttributeValue(attributeName, getDisplayString(evaluate(attributeName, value)));
		}
	}

	/**
	 * 创建 {@link TagWriter}，所有输出将写入其中。默认情况下，{@link TagWriter} 将其输出写入与当前 {@link javax.servlet.jsp.PageContext} 关联的 {@link javax.servlet.jsp.JspWriter}。
	 * 子类可以选择将输出实际写入的 {@link java.io.Writer} 更改为其他内容。
	 */
	protected TagWriter createTagWriter() {
		return new TagWriter(this.pageContext);
	}

	/**
	 * 提供一个简单的模板方法，调用 {@link #createTagWriter()} 并将创建的 {@link TagWriter} 传递给 {@link #writeTagContent(TagWriter)} 方法。
	 *
	 * @return {@link #writeTagContent(TagWriter)} 返回的值
	 */
	@Override
	protected final int doStartTagInternal() throws Exception {
		return writeTagContent(createTagWriter());
	}

	/**
	 * 获取提供的 {@code Object} 的显示值，根据需要进行 HTML 转义。此版本不是 {@link PropertyEditor} -aware。
	 */
	protected String getDisplayString(@Nullable Object value) {
		return ValueFormatter.getDisplayString(value, isHtmlEscape());
	}

	/**
	 * 获取提供的 {@code Object} 的显示值，根据需要进行 HTML 转义。如果提供的值不是 {@link String} 并且提供的 {@link PropertyEditor} 不为 null，则使用 {@link PropertyEditor} 来获取显示值。
	 */
	protected String getDisplayString(@Nullable Object value, @Nullable PropertyEditor propertyEditor) {
		return ValueFormatter.getDisplayString(value, propertyEditor, isHtmlEscape());
	}

	/**
	 * 在没有明确指定默认值的情况下重写为 {@code true}。
	 */
	@Override
	protected boolean isDefaultHtmlEscape() {
		Boolean defaultHtmlEscape = getRequestContext().getDefaultHtmlEscape();
		return (defaultHtmlEscape == null || defaultHtmlEscape.booleanValue());
	}


	/**
	 * 子类应实现此方法以执行标签内容渲染。
	 *
	 * @return 根据 {@link javax.servlet.jsp.tagext.Tag#doStartTag()} 的有效标签渲染指令。
	 */
	protected abstract int writeTagContent(TagWriter tagWriter) throws JspException;

}
