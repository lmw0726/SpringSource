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

package org.springframework.beans.propertyeditors;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * {@code java.util.Date} 的属性编辑器，
 * 支持自定义的 {@code java.text.DateFormat}。
 *
 * <p>此编辑器不是用于系统级 PropertyEditor，而是作为自定义控制器中
 * 的本地化日期编辑器使用，将用户输入的字符串解析为 Bean 的 Date 属性，
 * 并在 UI 表单中进行渲染。
 *
 * <p>在 Web MVC 代码中，此编辑器通常通过 {@code binder.registerCustomEditor} 注册。
 *
 * @author Juergen Hoeller
 * @since 28.04.2003
 * @see java.util.Date
 * @see java.text.DateFormat
 * @see org.springframework.validation.DataBinder#registerCustomEditor
 */
public class CustomDateEditor extends PropertyEditorSupport {

	private final DateFormat dateFormat;

	private final boolean allowEmpty;

	private final int exactDateLength;


	/**
	 * 使用指定的 DateFormat 创建一个新的 CustomDateEditor 实例，用于日期的解析和渲染。
	 * <p>参数 "allowEmpty" 表示是否允许空字符串进行解析，即解析为空值 null。
	 * 否则，在解析空字符串时会抛出 IllegalArgumentException。
	 *
	 * @param dateFormat 用于解析和渲染的 DateFormat
	 * @param allowEmpty 是否允许空字符串
	 */
	public CustomDateEditor(DateFormat dateFormat, boolean allowEmpty) {
		this.dateFormat = dateFormat;
		this.allowEmpty = allowEmpty;
		this.exactDateLength = -1;
	}

	/**
	 * 使用指定的 DateFormat 创建一个新的 CustomDateEditor 实例，用于日期的解析和渲染。
	 * <p>参数 "allowEmpty" 表示是否允许空字符串进行解析，即解析为空值 null。
	 * 否则，在解析空字符串时会抛出 IllegalArgumentException。
	 * <p>参数 "exactDateLength" 表示如果字符串长度不完全匹配指定长度，则抛出 IllegalArgumentException。
	 * 这很有用，因为 SimpleDateFormat 即使使用 {@code setLenient(false)} 也不会严格限制年份长度。
	 * 如果未指定 "exactDateLength"，例如 "01/01/05" 会被解析为 "01/01/0005"。
	 * 即使指定了 "exactDateLength"，日期中的日或月前导零仍可能导致年份长度不一致，
	 * 因此这只是接近预期日期格式的另一种校验手段。
	 *
	 * @param dateFormat 用于解析和渲染的 DateFormat
	 * @param allowEmpty 是否允许空字符串
	 * @param exactDateLength 日期字符串的精确长度
	 */
	public CustomDateEditor(DateFormat dateFormat, boolean allowEmpty, int exactDateLength) {
		this.dateFormat = dateFormat;
		this.allowEmpty = allowEmpty;
		this.exactDateLength = exactDateLength;
	}


	/**
	 * 使用指定的 DateFormat 从给定文本解析 Date。
	 */
	@Override
	public void setAsText(@Nullable String text) throws IllegalArgumentException {
		if (this.allowEmpty && !StringUtils.hasText(text)) {
			// 将空字符串视为 null 值
			setValue(null);
		}
		else if (text != null && this.exactDateLength >= 0 && text.length() != this.exactDateLength) {
			throw new IllegalArgumentException(
					"Could not parse date: it is not exactly" + this.exactDateLength + "characters long");
		}
		else {
			try {
				setValue(this.dateFormat.parse(text));
			}
			catch (ParseException ex) {
				throw new IllegalArgumentException("Could not parse date: " + ex.getMessage(), ex);
			}
		}
	}

	/**
	 * 使用指定的 DateFormat 将 Date 格式化为字符串。
	 */
	@Override
	public String getAsText() {
		Date value = (Date) getValue();
		return (value != null ? this.dateFormat.format(value) : "");
	}

}
