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
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.text.NumberFormat;

/**
 * 针对任意 Number 子类（如 Short、Integer、Long、BigInteger、Float、Double、BigDecimal）的属性编辑器。
 * 可以使用指定的 NumberFormat 进行（基于地区的）解析和格式化，也可以使用默认的 {@code decode} / {@code valueOf} / {@code toString} 方法。
 *
 * <p>此编辑器并非系统级 PropertyEditor，而是用于自定义控制器代码中解析用户输入的数字字符串，
 * 并将其赋值给 Bean 的 Number 属性，同时在 UI 表单中进行渲染。
 *
 * <p>在 Web MVC 代码中，通常通过 {@code binder.registerCustomEditor} 注册此编辑器。
 *
 * @author Juergen Hoeller
 * @since 06.06.2003
 * @see Number
 * @see java.text.NumberFormat
 * @see org.springframework.validation.DataBinder#registerCustomEditor
 */
public class CustomNumberEditor extends PropertyEditorSupport {

	private final Class<? extends Number> numberClass;

	@Nullable
	private final NumberFormat numberFormat;

	private final boolean allowEmpty;


	/**
	 * 使用默认的 {@code valueOf} 方法解析文本，{@code toString} 方法格式化值。
	 * @param numberClass 要处理的 Number 子类
	 * @param allowEmpty 是否允许空字符串解析为 {@code null}
	 * @throws IllegalArgumentException 如果指定的 numberClass 无效
	 * @see org.springframework.util.NumberUtils#parseNumber(String, Class)
	 * @see Integer#valueOf
	 * @see Integer#toString
	 */
	public CustomNumberEditor(Class<? extends Number> numberClass, boolean allowEmpty) throws IllegalArgumentException {
		this(numberClass, null, allowEmpty);
	}

	/**
	 * 使用指定的 NumberFormat 解析和格式化数字。
	 * @param numberClass 要处理的 Number 子类
	 * @param numberFormat 用于解析和格式化的 NumberFormat
	 * @param allowEmpty 是否允许空字符串解析为 {@code null}
	 * @throws IllegalArgumentException 如果指定的 numberClass 无效
	 * @see org.springframework.util.NumberUtils#parseNumber(String, Class, java.text.NumberFormat)
	 * @see java.text.NumberFormat#parse
	 * @see java.text.NumberFormat#format
	 */
	public CustomNumberEditor(Class<? extends Number> numberClass,
			@Nullable NumberFormat numberFormat, boolean allowEmpty) throws IllegalArgumentException {

		if (!Number.class.isAssignableFrom(numberClass)) {
			throw new IllegalArgumentException("Property class must be a subclass of Number");
		}
		this.numberClass = numberClass;
		this.numberFormat = numberFormat;
		this.allowEmpty = allowEmpty;
	}


	/**
	 * 使用指定的 NumberFormat 或默认解析方法将文本解析为 Number。
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (this.allowEmpty && !StringUtils.hasText(text)) {
			// 空字符串视为 null
			setValue(null);
		}
		else if (this.numberFormat != null) {
			// 使用指定 NumberFormat 解析
			setValue(NumberUtils.parseNumber(text, this.numberClass, this.numberFormat));
		}
		else {
			// 使用默认解析方法解析
			setValue(NumberUtils.parseNumber(text, this.numberClass));
		}
	}

	/**
	 * 将 Number 值强制转换为目标类型（如果需要）。
	 */
	@Override
	public void setValue(@Nullable Object value) {
		if (value instanceof Number) {
			super.setValue(NumberUtils.convertNumberToTargetClass((Number) value, this.numberClass));
		}
		else {
			super.setValue(value);
		}
	}

	/**
	 * 使用指定的 NumberFormat 或 toString 方法将 Number 格式化为字符串。
	 */
	@Override
	public String getAsText() {
		Object value = getValue();
		if (value == null) {
			return "";
		}
		if (this.numberFormat != null) {
			// 使用 NumberFormat 格式化
			return this.numberFormat.format(value);
		}
		else {
			// 使用 toString 方法格式化
			return value.toString();
		}
	}

}
