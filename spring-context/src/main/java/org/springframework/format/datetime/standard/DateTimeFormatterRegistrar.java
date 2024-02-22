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

package org.springframework.format.datetime.standard;

import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.EnumMap;
import java.util.Map;

/**
 * 配置用于与Spring一起使用的JSR-310 <code>java.time</code>格式化系统。
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @see #setDateStyle
 * @see #setTimeStyle
 * @see #setDateTimeStyle
 * @see #setUseIsoFormat
 * @see org.springframework.format.FormatterRegistrar#registerFormatters
 * @see org.springframework.format.datetime.DateFormatterRegistrar
 * @see org.springframework.format.datetime.joda.DateTimeFormatterFactoryBean
 * @since 4.0
 */
public class DateTimeFormatterRegistrar implements FormatterRegistrar {

	private enum Type {DATE, TIME, DATE_TIME}


	/**
	 * 用户定义的格式化程序。
	 */
	private final Map<Type, DateTimeFormatter> formatters = new EnumMap<>(Type.class);

	/**
	 * 当未指定特定格式化程序时使用的工厂。
	 */
	private final Map<Type, DateTimeFormatterFactory> factories = new EnumMap<>(Type.class);


	public DateTimeFormatterRegistrar() {
		for (Type type : Type.values()) {
			this.factories.put(type, new DateTimeFormatterFactory());
		}
	}


	/**
	 * 设置是否应用标准的 ISO 格式化到所有的日期/时间类型。
	 * 默认值为 "false"（否）。
	 * <p>如果设置为 "true"，则 "dateStyle"、"timeStyle" 和 "dateTimeStyle" 属性将被有效地忽略。
	 */
	public void setUseIsoFormat(boolean useIsoFormat) {
		this.factories.get(Type.DATE).setIso(useIsoFormat ? ISO.DATE : ISO.NONE);
		this.factories.get(Type.TIME).setIso(useIsoFormat ? ISO.TIME : ISO.NONE);
		this.factories.get(Type.DATE_TIME).setIso(useIsoFormat ? ISO.DATE_TIME : ISO.NONE);
	}

	/**
	 * 设置 {@link java.time.LocalDate} 对象的默认格式样式。
	 * 默认值为 {@link java.time.format.FormatStyle#SHORT}。
	 */
	public void setDateStyle(FormatStyle dateStyle) {
		this.factories.get(Type.DATE).setDateStyle(dateStyle);
	}

	/**
	 * 设置 {@link java.time.LocalTime} 对象的默认格式样式。
	 * 默认值为 {@link java.time.format.FormatStyle#SHORT}。
	 */
	public void setTimeStyle(FormatStyle timeStyle) {
		this.factories.get(Type.TIME).setTimeStyle(timeStyle);
	}

	/**
	 * 设置 {@link java.time.LocalDateTime} 对象的默认格式样式。
	 * 默认值为 {@link java.time.format.FormatStyle#SHORT}。
	 */
	public void setDateTimeStyle(FormatStyle dateTimeStyle) {
		this.factories.get(Type.DATE_TIME).setDateTimeStyle(dateTimeStyle);
	}

	/**
	 * 设置用于表示日期值的对象的格式化程序。
	 * <p>此格式化程序将用于 {@link LocalDate} 类型。
	 * 当指定此属性时，将忽略 {@link #setDateStyle dateStyle} 和 {@link #setUseIsoFormat useIsoFormat} 属性。
	 *
	 * @param formatter 要使用的格式化程序
	 * @see #setTimeFormatter
	 * @see #setDateTimeFormatter
	 */
	public void setDateFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.DATE, formatter);
	}

	/**
	 * 设置用于表示时间值的对象的格式化程序。
	 * <p>此格式化程序将用于 {@link LocalTime} 和 {@link OffsetTime} 类型。
	 * 当指定此属性时，将忽略 {@link #setTimeStyle timeStyle} 和 {@link #setUseIsoFormat useIsoFormat} 属性。
	 *
	 * @param formatter 要使用的格式化程序
	 * @see #setDateFormatter
	 * @see #setDateTimeFormatter
	 */
	public void setTimeFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.TIME, formatter);
	}

	/**
	 * 设置用于表示日期和时间值的对象的格式化程序。
	 * <p>此格式化程序将用于 {@link LocalDateTime}、{@link ZonedDateTime} 和 {@link OffsetDateTime} 类型。
	 * 当指定此属性时，将忽略 {@link #setDateTimeStyle dateTimeStyle} 和 {@link #setUseIsoFormat useIsoFormat} 属性。
	 *
	 * @param formatter 要使用的格式化程序
	 * @see #setDateFormatter
	 * @see #setTimeFormatter
	 */
	public void setDateTimeFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.DATE_TIME, formatter);
	}


	@Override
	public void registerFormatters(FormatterRegistry registry) {
		// 注册日期时间转换器
		DateTimeConverters.registerConverters(registry);

		// 获取日期、时间和日期时间的格式化器
		DateTimeFormatter df = getFormatter(Type.DATE);
		DateTimeFormatter tf = getFormatter(Type.TIME);
		DateTimeFormatter dtf = getFormatter(Type.DATE_TIME);

		// 使用效率高的 ISO_LOCAL_* 变体进行打印，因为它们的速度是其他变体的两倍...

		// 注册 LocalDate 的格式化器
		registry.addFormatterForFieldType(LocalDate.class,
				new TemporalAccessorPrinter(
						df == DateTimeFormatter.ISO_DATE ? DateTimeFormatter.ISO_LOCAL_DATE : df),
				new TemporalAccessorParser(LocalDate.class, df));

		// 注册 LocalTime 的格式化器
		registry.addFormatterForFieldType(LocalTime.class,
				new TemporalAccessorPrinter(
						tf == DateTimeFormatter.ISO_TIME ? DateTimeFormatter.ISO_LOCAL_TIME : tf),
				new TemporalAccessorParser(LocalTime.class, tf));

		// 注册 LocalDateTime 的格式化器
		registry.addFormatterForFieldType(LocalDateTime.class,
				new TemporalAccessorPrinter(
						dtf == DateTimeFormatter.ISO_DATE_TIME ? DateTimeFormatter.ISO_LOCAL_DATE_TIME : dtf),
				new TemporalAccessorParser(LocalDateTime.class, dtf));

		// 注册 ZonedDateTime 的格式化器
		registry.addFormatterForFieldType(ZonedDateTime.class,
				new TemporalAccessorPrinter(dtf),
				new TemporalAccessorParser(ZonedDateTime.class, dtf));

		// 注册 OffsetDateTime 的格式化器
		registry.addFormatterForFieldType(OffsetDateTime.class,
				new TemporalAccessorPrinter(dtf),
				new TemporalAccessorParser(OffsetDateTime.class, dtf));

		// 注册 OffsetTime 的格式化器
		registry.addFormatterForFieldType(OffsetTime.class,
				new TemporalAccessorPrinter(tf),
				new TemporalAccessorParser(OffsetTime.class, tf));

		// 注册 Instant 的格式化器
		registry.addFormatterForFieldType(Instant.class, new InstantFormatter());

		// 注册 Period 的格式化器
		registry.addFormatterForFieldType(Period.class, new PeriodFormatter());

		// 注册 Duration 的格式化器
		registry.addFormatterForFieldType(Duration.class, new DurationFormatter());

		// 注册 Year 的格式化器
		registry.addFormatterForFieldType(Year.class, new YearFormatter());

		// 注册 Month 的格式化器
		registry.addFormatterForFieldType(Month.class, new MonthFormatter());

		// 注册 YearMonth 的格式化器
		registry.addFormatterForFieldType(YearMonth.class, new YearMonthFormatter());

		// 注册 MonthDay 的格式化器
		registry.addFormatterForFieldType(MonthDay.class, new MonthDayFormatter());

		// 注册 JSR-310 的日期时间格式化注解工厂
		registry.addFormatterForFieldAnnotation(new Jsr310DateTimeFormatAnnotationFormatterFactory());
	}

	private DateTimeFormatter getFormatter(Type type) {
		// 获取指定类型的日期时间格式化器
		DateTimeFormatter formatter = this.formatters.get(type);
		if (formatter != null) {
			return formatter;
		}

		// 获取后备的日期时间格式化器
		DateTimeFormatter fallbackFormatter = getFallbackFormatter(type);

		// 使用工厂创建日期时间格式化器
		return this.factories.get(type).createDateTimeFormatter(fallbackFormatter);
	}

	private DateTimeFormatter getFallbackFormatter(Type type) {
		switch (type) {
			case DATE:
				return DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
			case TIME:
				return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
			default:
				return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
		}
	}

}
