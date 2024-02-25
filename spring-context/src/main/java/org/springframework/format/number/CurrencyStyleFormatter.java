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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

/**
 * 用于以货币样式格式化数字值的BigDecimal格式化程序。
 *
 * <p>委托给{@link java.text.NumberFormat#getCurrencyInstance(Locale)}。
 * 配置BigDecimal解析，以确保没有精度损失。
 * 可以对解析后的值应用指定的{@link java.math.RoundingMode}。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 4.2
 * @see #setLenient
 * @see #setRoundingMode
 */
public class CurrencyStyleFormatter extends AbstractNumberFormatter {
	/**
	 * 所需的小数位数，默认为2位小数
	 */
	private int fractionDigits = 2;
	/**
	 * 用于十进制解析的舍入模式。
	 */
	@Nullable
	private RoundingMode roundingMode;
	/**
	 * 指定货币
	 */
	@Nullable
	private Currency currency;
	/**
	 * 格式化数字值的模式
	 */
	@Nullable
	private String pattern;


	/**
	 * 指定所需的小数位数。
	 * 默认为2。
	 */
	public void setFractionDigits(int fractionDigits) {
		this.fractionDigits = fractionDigits;
	}

	/**
	 * 指定用于十进制解析的舍入模式。
	 * 默认为{@link java.math.RoundingMode#UNNECESSARY}。
	 */
	public void setRoundingMode(RoundingMode roundingMode) {
		this.roundingMode = roundingMode;
	}

	/**
	 * 指定货币，如果已知。
	 */
	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	/**
	 * 指定用于格式化数字值的模式。
	 * 如果未指定，则使用默认的DecimalFormat模式。
	 *
	 * @see java.text.DecimalFormat#applyPattern(String)
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}


	@Override
	public BigDecimal parse(String text, Locale locale) throws ParseException {
		// 使用父类的 parse 方法解析文本为 BigDecimal 对象
		BigDecimal decimal = (BigDecimal) super.parse(text, locale);

		// 如果设置了舍入模式
		if (this.roundingMode != null) {
			// 使用指定的舍入模式和小数位数对 BigDecimal 进行舍入
			decimal = decimal.setScale(this.fractionDigits, this.roundingMode);
		} else {
			// 使用指定的小数位数对 BigDecimal 进行舍入，默认采用的是舍入模式 ROUND_HALF_UP
			decimal = decimal.setScale(this.fractionDigits);
		}

		// 返回舍入后的 BigDecimal 对象
		return decimal;
	}

	@Override
	protected NumberFormat getNumberFormat(Locale locale) {
		// 获取指定 locale 的货币格式化实例
		DecimalFormat format = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);

		// 设置解析时返回 BigDecimal 类型
		format.setParseBigDecimal(true);

		// 设置最大和最小小数位数
		format.setMaximumFractionDigits(this.fractionDigits);
		format.setMinimumFractionDigits(this.fractionDigits);

		// 如果设置了舍入模式，则应用舍入模式
		if (this.roundingMode != null) {
			format.setRoundingMode(this.roundingMode);
		}

		// 如果设置了货币单位，则设置货币单位
		if (this.currency != null) {
			format.setCurrency(this.currency);
		}

		// 如果设置了模式，则应用模式
		if (this.pattern != null) {
			format.applyPattern(this.pattern);
		}

		// 返回设置完成的 DecimalFormat 对象
		return format;
	}

}
