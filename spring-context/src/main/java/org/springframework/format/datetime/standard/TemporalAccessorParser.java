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

import org.springframework.format.Parser;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * {@link Parser} 实现，用于 JSR-310 {@link java.time.temporal.TemporalAccessor}，
 * 使用 {@link java.time.format.DateTimeFormatter}（如果可用则使用上下文中的）。
 * <p>
 * 作者：Juergen Hoeller
 * 参见：DateTimeContextHolder#getFormatter
 * 参见：java.time.LocalDate#parse(CharSequence, java.time.format.DateTimeFormatter)
 * 参见：java.time.LocalTime#parse(CharSequence, java.time.format.DateTimeFormatter)
 * 参见：java.time.LocalDateTime#parse(CharSequence, java.time.format.DateTimeFormatter)
 * 参见：java.time.ZonedDateTime#parse(CharSequence, java.time.format.DateTimeFormatter)
 * 参见：java.time.OffsetDateTime#parse(CharSequence, java.time.format.DateTimeFormatter)
 * 参见：java.time.OffsetTime#parse(CharSequence, java.time.format.DateTimeFormatter)
 * 自 4.0 起
 */
public final class TemporalAccessorParser implements Parser<TemporalAccessor> {

	/**
	 * 时间访问器类型
	 */
	private final Class<? extends TemporalAccessor> temporalAccessorType;

	/**
	 * 日期时间格式化器
	 */
	private final DateTimeFormatter formatter;

	/**
	 * 回退模式
	 */
	@Nullable
	private final String[] fallbackPatterns;

	/**
	 * 数据源
	 */
	@Nullable
	private final Object source;


	/**
	 * 为给定的 TemporalAccessor 类型创建一个新的 TemporalAccessorParser。
	 *
	 * @param temporalAccessorType 具体的 TemporalAccessor 类（LocalDate、LocalTime、LocalDateTime、ZonedDateTime、OffsetDateTime、OffsetTime）
	 * @param formatter            基础的 DateTimeFormatter 实例
	 */
	public TemporalAccessorParser(Class<? extends TemporalAccessor> temporalAccessorType, DateTimeFormatter formatter) {
		this(temporalAccessorType, formatter, null, null);
	}

	TemporalAccessorParser(Class<? extends TemporalAccessor> temporalAccessorType, DateTimeFormatter formatter,
						   @Nullable String[] fallbackPatterns, @Nullable Object source) {
		this.temporalAccessorType = temporalAccessorType;
		this.formatter = formatter;
		this.fallbackPatterns = fallbackPatterns;
		this.source = source;
	}


	@Override
	public TemporalAccessor parse(String text, Locale locale) throws ParseException {
		try {
			// 尝试使用当前的 日期时间格式化器 解析文本
			return doParse(text, locale, this.formatter);
		} catch (DateTimeParseException ex) {
			// 如果解析失败，并且有提供备选模式
			if (!ObjectUtils.isEmpty(this.fallbackPatterns)) {
				// 遍历备选模式
				for (String pattern : this.fallbackPatterns) {
					try {
						// 使用备选模式创建严格的 DateTimeFormatter
						DateTimeFormatter fallbackFormatter = DateTimeFormatterUtils.createStrictDateTimeFormatter(pattern);
						// 使用备选模式重新解析文本
						return doParse(text, locale, fallbackFormatter);
					} catch (DateTimeParseException ignoredException) {
						// 忽略备选解析时的异常，因为下面的异常会包含来自 "source" 的信息，
						// 例如，@DateTimeFormat 注解的 toString() 方法的返回值。
					}
				}
			}
			// 如果设置了 source，抛出带有源信息的 DateTimeParseException
			if (this.source != null) {
				throw new DateTimeParseException(
						String.format("Unable to parse date time value \"%s\" using configuration from %s", text, this.source),
						text, ex.getErrorIndex(), ex);
			}
			// 否则，重新抛出原始异常
			throw ex;
		}
	}

	/**
	 * 使用指定的 Locale 和 DateTimeFormatter 解析文本，并返回对应的 TemporalAccessor。
	 *
	 * @param text      要解析的文本
	 * @param locale    区域设置
	 * @param formatter DateTimeFormatter 实例
	 * @return 解析后的 TemporalAccessor
	 * @throws DateTimeParseException 如果解析失败
	 */
	private TemporalAccessor doParse(String text, Locale locale, DateTimeFormatter formatter) throws DateTimeParseException {
		// 获取当前线程的 DateTimeFormatter，使用指定的 formatter 和 locale
		DateTimeFormatter formatterToUse = DateTimeContextHolder.getFormatter(formatter, locale);

		// 根据指定的 TemporalAccessor 类型解析文本
		if (LocalDate.class == this.temporalAccessorType) {
			// 解析为 LocalDate
			return LocalDate.parse(text, formatterToUse);
		} else if (LocalTime.class == this.temporalAccessorType) {
			// 解析为 LocalTime
			return LocalTime.parse(text, formatterToUse);
		} else if (LocalDateTime.class == this.temporalAccessorType) {
			// 解析为 LocalDateTime
			return LocalDateTime.parse(text, formatterToUse);
		} else if (ZonedDateTime.class == this.temporalAccessorType) {
			// 解析为 ZonedDateTime
			return ZonedDateTime.parse(text, formatterToUse);
		} else if (OffsetDateTime.class == this.temporalAccessorType) {
			// 解析为 OffsetDateTime
			return OffsetDateTime.parse(text, formatterToUse);
		} else if (OffsetTime.class == this.temporalAccessorType) {
			// 解析为 OffsetTime
			return OffsetTime.parse(text, formatterToUse);
		} else {
			// 不支持的 TemporalAccessor 类型，抛出异常
			throw new IllegalStateException("Unsupported TemporalAccessor type: " + this.temporalAccessorType);
		}
	}

}
