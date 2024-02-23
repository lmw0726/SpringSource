/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.format.number.money;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.annotation.NumberFormat.Style;
import org.springframework.format.number.CurrencyStyleFormatter;
import org.springframework.format.number.NumberStyleFormatter;
import org.springframework.format.number.PercentStyleFormatter;
import org.springframework.util.StringUtils;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.text.ParseException;
import java.util.Collections;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;

/**
 * 格式化使用 Spring 的常见 {@link NumberFormat} 注解注释的 {@link javax.money.MonetaryAmount} 字段。
 *
 * @author Juergen Hoeller
 * @see NumberFormat
 * @since 4.2
 */
public class Jsr354NumberFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
		implements AnnotationFormatterFactory<NumberFormat> {
	/**
	 * 货币代码模式
	 */
	private static final String CURRENCY_CODE_PATTERN = "\u00A4\u00A4";


	@Override
	public Set<Class<?>> getFieldTypes() {
		return Collections.singleton(MonetaryAmount.class);
	}

	@Override
	public Printer<MonetaryAmount> getPrinter(NumberFormat annotation, Class<?> fieldType) {
		return configureFormatterFrom(annotation);
	}

	@Override
	public Parser<MonetaryAmount> getParser(NumberFormat annotation, Class<?> fieldType) {
		return configureFormatterFrom(annotation);
	}


	/**
	 * 从 {@link NumberFormat} 注解配置 Formatter<MonetaryAmount>。
	 *
	 * @param annotation 注解对象
	 * @return Formatter<MonetaryAmount> 格式化器对象
	 */
	private Formatter<MonetaryAmount> configureFormatterFrom(NumberFormat annotation) {
		// 解析嵌入值注解中的模式
		String pattern = resolveEmbeddedValue(annotation.pattern());
		// 如果模式长度大于0，返回使用模式的模式装饰格式化器
		if (StringUtils.hasLength(pattern)) {
			return new PatternDecoratingFormatter(pattern);
		} else {
			// 如果模式长度为0，根据样式创建相应的装饰格式化器
			Style style = annotation.style();
			if (style == Style.NUMBER) {
				// 如果是通用数字格式，返回数字样式格式化器的装饰器
				return new NumberDecoratingFormatter(new NumberStyleFormatter());
			} else if (style == Style.PERCENT) {
				// 如果是百分比格式，返回百分比样式格式化器的装饰器
				return new NumberDecoratingFormatter(new PercentStyleFormatter());
			} else {
				// 否则返回货币样式格式化器的装饰器
				return new NumberDecoratingFormatter(new CurrencyStyleFormatter());
			}
		}
	}


	private static class NumberDecoratingFormatter implements Formatter<MonetaryAmount> {
		/**
		 * 数字格式化器
		 */
		private final Formatter<Number> numberFormatter;

		public NumberDecoratingFormatter(Formatter<Number> numberFormatter) {
			this.numberFormatter = numberFormatter;
		}

		@Override
		public String print(MonetaryAmount object, Locale locale) {
			return this.numberFormatter.print(object.getNumber(), locale);
		}

		@Override
		public MonetaryAmount parse(String text, Locale locale) throws ParseException {
			// 使用给定的地区获取货币单位
			CurrencyUnit currencyUnit = Monetary.getCurrency(locale);
			// 使用数字格式化器解析文本并获取数字值
			Number numberValue = this.numberFormatter.parse(text, locale);
			// 创建并返回一个MonetaryAmount对象，设置数字值和货币单位
			return Monetary.getDefaultAmountFactory().setNumber(numberValue).setCurrency(currencyUnit).create();
		}
	}


	private static class PatternDecoratingFormatter implements Formatter<MonetaryAmount> {
		/**
		 * 模式
		 */
		private final String pattern;

		public PatternDecoratingFormatter(String pattern) {
			this.pattern = pattern;
		}

		@Override
		public String print(MonetaryAmount object, Locale locale) {
			// 创建一个货币样式格式化器对象
			CurrencyStyleFormatter formatter = new CurrencyStyleFormatter();
			// 获取货币代码对应的货币对象，并设置货币单位
			formatter.setCurrency(Currency.getInstance(object.getCurrency().getCurrencyCode()));
			// 设置格式化模式为指定的模式
			formatter.setPattern(this.pattern);
			// 使用格式化器将给定的数字值格式化为货币字符串，并返回结果
			return formatter.print(object.getNumber(), locale);
		}

		@Override
		public MonetaryAmount parse(String text, Locale locale) throws ParseException {
			// 创建一个货币样式格式化器对象
			CurrencyStyleFormatter formatter = new CurrencyStyleFormatter();
			// 确定货币对象
			Currency currency = determineCurrency(text, locale);
			// 获取货币代码对应的货币单位
			CurrencyUnit currencyUnit = Monetary.getCurrency(currency.getCurrencyCode());
			// 设置格式化器的货币对象
			formatter.setCurrency(currency);
			// 设置格式化模式为指定的模式
			formatter.setPattern(this.pattern);
			// 使用格式化器解析文本，并返回解析后的数字值
			Number numberValue = formatter.parse(text, locale);
			// 创建并返回货币对象的金额
			return Monetary.getDefaultAmountFactory().setNumber(numberValue).setCurrency(currencyUnit).create();
		}

		/**
		 * 推断货币
		 *
		 * @param text   文本字符串
		 * @param locale 当前用户语言环境
		 * @return 货币对象
		 */
		private Currency determineCurrency(String text, Locale locale) {
			try {
				if (text.length() < 3) {
					// 文本长度小于3，不可能包含货币代码 -> 尝试使用区域设置，可能会在解析时失败。
					return Currency.getInstance(locale);
				} else if (this.pattern.startsWith(CURRENCY_CODE_PATTERN)) {
					// 根据模式的起始位置获取货币代码
					return Currency.getInstance(text.substring(0, 3));
				} else if (this.pattern.endsWith(CURRENCY_CODE_PATTERN)) {
					// 根据模式的末尾位置获取货币代码
					return Currency.getInstance(text.substring(text.length() - 3));
				} else {
					// 没有货币代码的模式...
					return Currency.getInstance(locale);
				}
			} catch (IllegalArgumentException ex) {
				// 捕获IllegalArgumentException异常，抛出带有详细信息的新异常
				throw new IllegalArgumentException("Cannot determine currency for number value [" + text + "]", ex);
			}
		}
	}

}
