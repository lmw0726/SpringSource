/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.format.number;

import org.springframework.lang.Nullable;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 使用NumberFormat的数字样式的通用数字格式化程序。
 *
 * <p>委托给{@link java.text.NumberFormat#getInstance(Locale)}。
 * 配置BigDecimal解析，以确保不会丢失精度。
 * 允许配置小数数字模式。
 * {@link #parse(String, Locale)}例程始终返回BigDecimal。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 4.2
 * @see #setPattern
 * @see #setLenient
 */
public class NumberStyleFormatter extends AbstractNumberFormatter {
	/**
	 * 用于格式化数字值的模式
	 */
	@Nullable
	private String pattern;


	/**
	 * 创建一个没有模式的新NumberStyleFormatter。
	 */
	public NumberStyleFormatter() {
	}

	/**
	 * 使用指定的模式创建一个新的NumberStyleFormatter。
	 * @param pattern 格式模式
	 * @see #setPattern
	 */
	public NumberStyleFormatter(String pattern) {
		this.pattern = pattern;
	}


	/**
	 * 指定用于格式化数字值的模式。
	 * 如果未指定，则使用默认的DecimalFormat模式。
	 * @see java.text.DecimalFormat#applyPattern(String)
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}


	@Override
	public NumberFormat getNumberFormat(Locale locale) {
		// 获取指定 locale 的通用数值格式化实例
		NumberFormat format = NumberFormat.getInstance(locale);

		// 如果格式化实例不是 DecimalFormat 类型，则抛出异常
		if (!(format instanceof DecimalFormat)) {
			if (this.pattern != null) {
				// 如果设置了模式，但格式化实例不是 DecimalFormat 类型，则抛出异常
				throw new IllegalStateException("Cannot support pattern for non-DecimalFormat: " + format);
			}
			// 返回格式化实例
			return format;
		}

		// 将格式化实例强制转换为 DecimalFormat 类型
		DecimalFormat decimalFormat = (DecimalFormat) format;

		// 设置解析时返回 BigDecimal 类型
		decimalFormat.setParseBigDecimal(true);

		// 如果设置了模式，则应用模式
		if (this.pattern != null) {
			decimalFormat.applyPattern(this.pattern);
		}

		// 返回设置完成的 DecimalFormat 对象
		return decimalFormat;
	}

}
