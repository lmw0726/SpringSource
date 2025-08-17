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

/**
 * Boolean/boolean 属性的属性编辑器。
 *
 * <p>此编辑器并非用于系统级 PropertyEditor，而是用于自定义控制器代码中的
 * 本地化 Boolean 编辑器，将 UI 提交的布尔字符串解析为 Bean 的布尔属性，
 * 并在 UI 表单中进行检查。
 *
 * <p>在 Web MVC 中，此编辑器通常通过 {@code binder.registerCustomEditor} 注册使用。
 *
 * @author Juergen Hoeller
 * @since 10.06.2003
 * @see org.springframework.validation.DataBinder#registerCustomEditor
 */
public class CustomBooleanEditor extends PropertyEditorSupport {

	/**
	 * {@code "true"} 的值。
	 */
	public static final String VALUE_TRUE = "true";

	/**
	 * {@code "false"} 的值。
	 */
	public static final String VALUE_FALSE = "false";

	/**
	 * {@code "on"} 的值。
	 */
	public static final String VALUE_ON = "on";

	/**
	 * {@code "off"} 的值。
	 */
	public static final String VALUE_OFF = "off";

	/**
	 * {@code "yes"} 的值。
	 */
	public static final String VALUE_YES = "yes";

	/**
	 * {@code "no"} 的值。
	 */
	public static final String VALUE_NO = "no";

	/**
	 * {@code "1"} 的值。
	 */
	public static final String VALUE_1 = "1";

	/**
	 * {@code "0"} 的值。
	 */
	public static final String VALUE_0 = "0";


	@Nullable
	private final String trueString;

	@Nullable
	private final String falseString;

	private final boolean allowEmpty;


	/**
	 * 创建一个新的 CustomBooleanEditor 实例，使用 "true"/"on"/"yes" 和 "false"/"off"/"no" 作为识别的字符串值。
	 * <p>"allowEmpty" 参数指示是否允许解析空字符串，即将其解释为 null 值。
	 * 否则，在这种情况下会抛出 IllegalArgumentException。
	 * @param allowEmpty 是否允许空字符串
	 */
	public CustomBooleanEditor(boolean allowEmpty) {
		this(null, null, allowEmpty);
	}

	/**
	 * 创建一个新的 CustomBooleanEditor 实例，允许自定义 true 和 false 的字符串值。
	 * <p>"allowEmpty" 参数指示是否允许解析空字符串，即将其解释为 null 值。
	 * 否则，在这种情况下会抛出 IllegalArgumentException。
	 * @param trueString 表示 true 的字符串值：
	 * 例如 "true" (VALUE_TRUE)、"on" (VALUE_ON)、"yes" (VALUE_YES) 或自定义值
	 * @param falseString 表示 false 的字符串值：
	 * 例如 "false" (VALUE_FALSE)、"off" (VALUE_OFF)、"no" (VALUE_NO) 或自定义值
	 * @param allowEmpty 是否允许空字符串
	 * @see #VALUE_TRUE
	 * @see #VALUE_FALSE
	 * @see #VALUE_ON
	 * @see #VALUE_OFF
	 * @see #VALUE_YES
	 * @see #VALUE_NO
	 */
	public CustomBooleanEditor(@Nullable String trueString, @Nullable String falseString, boolean allowEmpty) {
		this.trueString = trueString;
		this.falseString = falseString;
		this.allowEmpty = allowEmpty;
	}


	@Override
	public void setAsText(@Nullable String text) throws IllegalArgumentException {
		String input = (text != null ? text.trim() : null);
		if (this.allowEmpty && !StringUtils.hasLength(input)) {
			// 将空字符串视为null值。
			setValue(null);
		}
		else if (this.trueString != null && this.trueString.equalsIgnoreCase(input)) {
			setValue(Boolean.TRUE);
		}
		else if (this.falseString != null && this.falseString.equalsIgnoreCase(input)) {
			setValue(Boolean.FALSE);
		}
		else if (this.trueString == null &&
				(VALUE_TRUE.equalsIgnoreCase(input) || VALUE_ON.equalsIgnoreCase(input) ||
						VALUE_YES.equalsIgnoreCase(input) || VALUE_1.equals(input))) {
			setValue(Boolean.TRUE);
		}
		else if (this.falseString == null &&
				(VALUE_FALSE.equalsIgnoreCase(input) || VALUE_OFF.equalsIgnoreCase(input) ||
						VALUE_NO.equalsIgnoreCase(input) || VALUE_0.equals(input))) {
			setValue(Boolean.FALSE);
		}
		else {
			throw new IllegalArgumentException("Invalid boolean value [" + text + "]");
		}
	}

	@Override
	public String getAsText() {
		if (Boolean.TRUE.equals(getValue())) {
			return (this.trueString != null ? this.trueString : VALUE_TRUE);
		}
		else if (Boolean.FALSE.equals(getValue())) {
			return (this.falseString != null ? this.falseString : VALUE_FALSE);
		}
		else {
			return "";
		}
	}

}
