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
import org.springframework.web.util.HtmlUtils;

import java.beans.PropertyEditor;

/**
 * 用于通过表单标签渲染值的包可见辅助类。支持两种格式化样式：普通样式和 {@link PropertyEditor} 感知样式。
 *
 * <p>普通样式简单地防止字符串 '{@code null}' 出现，将其替换为空字符串，并根据需要添加 HTML 转义。
 *
 * <p>{@link PropertyEditor} 感知样式将尝试使用提供的 {@link PropertyEditor} 在应用普通格式化规则之前，渲染任何非字符串值。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
abstract class ValueFormatter {

	/**
	 * 构建提供的 {@code Object} 的显示值，根据需要进行 HTML 转义。此版本不是 {@link PropertyEditor} 感知的。
	 *
	 * @see #getDisplayString(Object, java.beans.PropertyEditor, boolean)
	 */
	public static String getDisplayString(@Nullable Object value, boolean htmlEscape) {
		// 获取展示值
		String displayValue = ObjectUtils.getDisplayString(value);
		// 如果需要HTML转义，则进行转义
		return (htmlEscape ? HtmlUtils.htmlEscape(displayValue) : displayValue);
	}

	/**
	 * 构建提供的 {@code Object} 的显示值，根据需要进行 HTML 转义。如果提供的值不是 {@link String}，并且提供的
	 * {@link PropertyEditor} 不为空，则 {@link PropertyEditor} 用于获取显示值。
	 *
	 * @see #getDisplayString(Object, boolean)
	 */
	public static String getDisplayString(
			@Nullable Object value, @Nullable PropertyEditor propertyEditor, boolean htmlEscape) {

		if (propertyEditor != null && !(value instanceof String)) {
			// 如果属性编辑器存在，并且值不是字符串类型
			try {
				// 设置属性编辑器中的值为当前值
				propertyEditor.setValue(value);
				// 获取该值的文本形式
				String text = propertyEditor.getAsText();
				if (text != null) {
					// 如果文本值存在，则获取展示值
					return getDisplayString(text, htmlEscape);
				}
			} catch (Throwable ex) {
				// PropertyEditor 可能不支持此值... 传递。
			}
		}
		// 直接返回其展示值
		return getDisplayString(value, htmlEscape);
	}

}
