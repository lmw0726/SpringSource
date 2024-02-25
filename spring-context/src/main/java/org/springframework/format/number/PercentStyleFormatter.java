/*
 * Copyright 2002-2015 the original author or authors.
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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 百分比样式的数字值格式化程序。
 *
 * <p>委托给{@link java.text.NumberFormat#getPercentInstance(Locale)}。
 * 配置BigDecimal解析，以确保不会丢失精度。
 * {@link #parse(String, Locale)}例程始终返回BigDecimal。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 4.2
 * @see #setLenient
 */
public class PercentStyleFormatter extends AbstractNumberFormatter {

	@Override
	protected NumberFormat getNumberFormat(Locale locale) {
		// 获取指定 locale 的百分比格式化实例
		NumberFormat format = NumberFormat.getPercentInstance(locale);

		// 如果格式化实例是 DecimalFormat 类型，则设置解析时返回 BigDecimal 类型
		if (format instanceof DecimalFormat) {
			((DecimalFormat) format).setParseBigDecimal(true);
		}

		// 返回设置完成的 NumberFormat 实例
		return format;
	}

}
