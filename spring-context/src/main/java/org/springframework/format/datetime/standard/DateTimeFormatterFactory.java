/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.format.datetime.standard;

import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.TimeZone;

/**
 * 创建一个 JSR-310 {@link java.time.format.DateTimeFormatter} 的工厂。
 *
 * <p>将使用定义的 {@link #setPattern pattern}、{@link #setIso ISO} 和 <code>xxxStyle</code> 方法创建格式化程序（按顺序考虑）。
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @see #createDateTimeFormatter()
 * @see #createDateTimeFormatter(DateTimeFormatter)
 * @see #setPattern
 * @see #setIso
 * @see #setDateStyle
 * @see #setTimeStyle
 * @see #setDateTimeStyle
 * @see DateTimeFormatterFactoryBean
 * @since 4.0
 */
public class DateTimeFormatterFactory {

	/**
	 * 模式字符串。
	 */
	@Nullable
	private String pattern;

	/**
	 * ISO 格式。
	 */
	@Nullable
	private ISO iso;

	/**
	 * 日期样式。
	 */
	@Nullable
	private FormatStyle dateStyle;

	/**
	 * 时间样式。
	 */
	@Nullable
	private FormatStyle timeStyle;

	/**
	 * 时区。
	 */
	@Nullable
	private TimeZone timeZone;


	/**
	 * 创建一个新的 {@code DateTimeFormatterFactory} 实例。
	 */
	public DateTimeFormatterFactory() {
	}

	/**
	 * 使用指定的模式创建一个新的 {@code DateTimeFormatterFactory} 实例。
	 *
	 * @param pattern 用于格式化日期值的模式
	 */
	public DateTimeFormatterFactory(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * 设置用于格式化日期值的模式。
	 *
	 * @param pattern 格式化模式
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * 设置用于格式化日期值的 ISO 格式。
	 *
	 * @param iso ISO 格式
	 */
	public void setIso(ISO iso) {
		this.iso = iso;
	}

	/**
	 * 设置日期类型的样式。
	 *
	 * @param dateStyle 日期类型的样式
	 */
	public void setDateStyle(FormatStyle dateStyle) {
		this.dateStyle = dateStyle;
	}

	/**
	 * 设置时间类型的样式。
	 *
	 * @param timeStyle 时间类型的样式
	 */
	public void setTimeStyle(FormatStyle timeStyle) {
		this.timeStyle = timeStyle;
	}

	/**
	 * 设置日期和时间类型的样式。
	 *
	 * @param dateTimeStyle 日期和时间类型的样式
	 */
	public void setDateTimeStyle(FormatStyle dateTimeStyle) {
		this.dateStyle = dateTimeStyle;
		this.timeStyle = dateTimeStyle;
	}

	/**
	 * 设置用于格式化日期值的两个字符，以 Joda-Time 风格为准。
	 * <p>第一个字符用于日期样式；第二个字符用于时间样式。支持的字符有：
	 * <ul>
	 * <li>'S' = 小</li>
	 * <li>'M' = 中等</li>
	 * <li>'L' = 长</li>
	 * <li>'F' = 完整</li>
	 * <li>'-' = 省略</li>
	 * </ul>
	 * <p>此方法模仿了 Joda-Time 支持的样式。请注意，JSR-310 本地支持{@link java.time.format.FormatStyle}，
	 * 用于{@link #setDateStyle}、{@link #setTimeStyle} 和 {@link #setDateTimeStyle}。
	 *
	 * @param style 从集合 {"S", "M", "L", "F", "-"} 中选择的两个字符
	 */
	public void setStylePattern(String style) {
		Assert.isTrue(style.length() == 2, "Style pattern must consist of two characters");
		this.dateStyle = convertStyleCharacter(style.charAt(0));
		this.timeStyle = convertStyleCharacter(style.charAt(1));
	}

	@Nullable
	private FormatStyle convertStyleCharacter(char c) {
		// 根据输入的字符返回相应的格式样式枚举值
		switch (c) {
			case 'S':
				// 如果字符为 'S'，返回 SHORT 格式样式
				return FormatStyle.SHORT;
			case 'M':
				// 如果字符为 'M'，返回 MEDIUM 格式样式
				return FormatStyle.MEDIUM;
			case 'L':
				// 如果字符为 'L'，返回 LONG 格式样式
				return FormatStyle.LONG;
			case 'F':
				// 如果字符为 'F'，返回 FULL 格式样式
				return FormatStyle.FULL;
			case '-':
				// 如果字符为 '-'，返回 null
				return null;
			default:
				// 如果字符不是 'S', 'M', 'L', 'F', 或 '-'，抛出异常
				throw new IllegalArgumentException("Invalid style character '" + c + "'");
		}

	}

	/**
	 * 设置用于将日期值标准化的{@code TimeZone}（如果有）。
	 *
	 * @param timeZone 时间区域
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}


	/**
	 * 使用此工厂创建一个新的{@code DateTimeFormatter}。
	 * <p>如果未定义特定的模式或样式，
	 * 将使用{@link FormatStyle#MEDIUM 中等日期时间格式}。
	 *
	 * @return 一个新的日期时间格式化程序
	 * @see #createDateTimeFormatter(DateTimeFormatter)
	 */
	public DateTimeFormatter createDateTimeFormatter() {
		return createDateTimeFormatter(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
	}

	/**
	 * 使用此工厂创建一个新的{@code DateTimeFormatter}。
	 * <p>如果未定义特定的模式或样式，
	 * 将使用提供的{@code fallbackFormatter}。
	 *
	 * @param fallbackFormatter 当未设置特定的工厂属性时要使用的备用格式化程序
	 * @return 一个新的日期时间格式化程序
	 */
	public DateTimeFormatter createDateTimeFormatter(DateTimeFormatter fallbackFormatter) {
		// 创建一个 DateTimeFormatter 对象，并初始化为 null
		DateTimeFormatter dateTimeFormatter = null;

		// 如果指定了日期时间模式（pattern）
		if (StringUtils.hasLength(this.pattern)) {
			// 使用指定的模式创建严格的 DateTimeFormatter
			dateTimeFormatter = DateTimeFormatterUtils.createStrictDateTimeFormatter(this.pattern);
		} else if (this.iso != null && this.iso != ISO.NONE) {
			// 如果指定了 ISO 格式
			switch (this.iso) {
				case DATE:
					// 使用 ISO_DATE 创建 DateTimeFormatter
					dateTimeFormatter = DateTimeFormatter.ISO_DATE;
					break;
				case TIME:
					// 使用 ISO_TIME 创建 DateTimeFormatter
					dateTimeFormatter = DateTimeFormatter.ISO_TIME;
					break;
				case DATE_TIME:
					// 使用 ISO_DATE_TIME 创建 DateTimeFormatter
					dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
					break;
				default:
					// 抛出异常，不支持的 ISO 格式
					throw new IllegalStateException("Unsupported ISO format: " + this.iso);
			}
		} else if (this.dateStyle != null && this.timeStyle != null) {
			// 如果指定了日期和时间样式
			// 使用指定的日期和时间样式创建 DateTimeFormatter
			dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(this.dateStyle, this.timeStyle);
		} else if (this.dateStyle != null) {
			// 如果只指定了日期样式
			// 使用指定的日期样式创建 DateTimeFormatter
			dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(this.dateStyle);
		} else if (this.timeStyle != null) {
			// 如果只指定了时间样式
			// 使用指定的时间样式创建 DateTimeFormatter
			dateTimeFormatter = DateTimeFormatter.ofLocalizedTime(this.timeStyle);
		}

		if (dateTimeFormatter != null && this.timeZone != null) {
			// 如果 DateTimeFormatter 对象不为 null，并且指定了时区
			// 将 DateTimeFormatter 的时区设置为指定的时区
			dateTimeFormatter = dateTimeFormatter.withZone(this.timeZone.toZoneId());
		}

		// 返回 DateTimeFormatter 对象，如果为 null 则返回 fallbackFormatter
		return (dateTimeFormatter != null ? dateTimeFormatter : fallbackFormatter);
	}

}
