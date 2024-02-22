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

package org.springframework.format.datetime.joda;

import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

/**
 * 为了与Spring一起使用，配置Joda-Time的格式化系统。
 *
 * <p><b>注意:</b> 从Spring 4.0开始，Spring对Joda-Time的支持需要Joda-Time 2.x。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.1
 * @see #setDateStyle
 * @see #setTimeStyle
 * @see #setDateTimeStyle
 * @see #setUseIsoFormat
 * @see FormatterRegistrar#registerFormatters
 * @see org.springframework.format.datetime.DateFormatterRegistrar
 * @see DateTimeFormatterFactoryBean
 * @deprecated 自5.3起，不推荐使用，推荐使用标准的JSR-310支持
 */
@Deprecated
public class JodaTimeFormatterRegistrar implements FormatterRegistrar {

	private enum Type {DATE, TIME, DATE_TIME}


	/**
	 * 用户自定义格式化程序。
	 */
	private final Map<Type, DateTimeFormatter> formatters = new EnumMap<>(Type.class);

	/**
	 * 在未指定特定格式化程序时使用的工厂。
	 */
	private final Map<Type, DateTimeFormatterFactory> factories;


	public JodaTimeFormatterRegistrar() {
		this.factories = new EnumMap<>(Type.class);
		for (Type type : Type.values()) {
			this.factories.put(type, new DateTimeFormatterFactory());
		}
	}


	/**
	 * 设置是否对所有日期/时间类型应用标准 ISO 格式化。
	 * 默认值为“false”（不是）。
	 * <p>如果设置为“true”，则“dateStyle”、“timeStyle”和“dateTimeStyle”属性将被忽略。
	 */
	public void setUseIsoFormat(boolean useIsoFormat) {
		this.factories.get(Type.DATE).setIso(useIsoFormat ? ISO.DATE : ISO.NONE);
		this.factories.get(Type.TIME).setIso(useIsoFormat ? ISO.TIME : ISO.NONE);
		this.factories.get(Type.DATE_TIME).setIso(useIsoFormat ? ISO.DATE_TIME : ISO.NONE);
	}

	/**
	 * 设置 Joda {@link LocalDate} 对象的默认格式样式。
	 * 默认值为 {@link DateTimeFormat#shortDate()}。
	 */
	public void setDateStyle(String dateStyle) {
		this.factories.get(Type.DATE).setStyle(dateStyle + "-");
	}

	/**
	 * 设置 Joda {@link LocalTime} 对象的默认格式样式。
	 * 默认值为 {@link DateTimeFormat#shortTime()}。
	 */
	public void setTimeStyle(String timeStyle) {
		this.factories.get(Type.TIME).setStyle("-" + timeStyle);
	}

	/**
	 * 设置 Joda {@link LocalDateTime} 和 {@link DateTime} 对象，
	 * 以及 JDK {@link Date} 和 {@link Calendar} 对象的默认格式样式。
	 * 默认值为 {@link DateTimeFormat#shortDateTime()}。
	 */
	public void setDateTimeStyle(String dateTimeStyle) {
		this.factories.get(Type.DATE_TIME).setStyle(dateTimeStyle);
	}

	/**
	 * 设置将用于表示日期值的对象的格式化程序。
	 * <p>此格式化程序将用于 {@link LocalDate} 类型。
	 * 当指定此参数时，{@link #setDateStyle(String) dateStyle} 和
	 * {@link #setUseIsoFormat(boolean) useIsoFormat} 属性将被忽略。
	 *
	 * @param formatter 要使用的格式化程序
	 * @since 3.2
	 * @see #setTimeFormatter
	 * @see #setDateTimeFormatter
	 */
	public void setDateFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.DATE, formatter);
	}

	/**
	 * 设置将用于表示时间值的对象的格式化程序。
	 * <p>此格式化程序将用于 {@link LocalTime} 类型。
	 * 当指定此参数时，{@link #setTimeStyle(String) timeStyle} 和
	 * {@link #setUseIsoFormat(boolean) useIsoFormat} 属性将被忽略。
	 * @param formatter 要使用的格式化程序
	 * @since 3.2
	 * @see #setDateFormatter
	 * @see #setDateTimeFormatter
	 */
	public void setTimeFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.TIME, formatter);
	}

	/**
	 * 设置将用于表示日期和时间值的对象的格式化程序。
	 * <p>此格式化程序将用于 {@link LocalDateTime}、{@link ReadableInstant}、
	 * {@link Date} 和 {@link Calendar} 类型。
	 * 当指定此参数时，{@link #setDateTimeStyle(String) dateTimeStyle} 和
	 * {@link #setUseIsoFormat(boolean) useIsoFormat} 属性将被忽略。
	 * @param formatter 要使用的格式化程序
	 * @since 3.2
	 * @see #setDateFormatter
	 * @see #setTimeFormatter
	 */
	public void setDateTimeFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.DATE_TIME, formatter);
	}


	@Override
	public void registerFormatters(FormatterRegistry registry) {
		// 注册JodaTime的转换器
		JodaTimeConverters.registerConverters(registry);

		// 获取各种格式的日期时间格式化器
		DateTimeFormatter dateFormatter = getFormatter(Type.DATE);
		DateTimeFormatter timeFormatter = getFormatter(Type.TIME);
		DateTimeFormatter dateTimeFormatter = getFormatter(Type.DATE_TIME);

		// 注册局部可读打印器和解析器以及对应的字段类型
		addFormatterForFields(registry,
				new ReadablePartialPrinter(dateFormatter),
				new LocalDateParser(dateFormatter),
				LocalDate.class);

		addFormatterForFields(registry,
				new ReadablePartialPrinter(timeFormatter),
				new LocalTimeParser(timeFormatter),
				LocalTime.class);

		addFormatterForFields(registry,
				new ReadablePartialPrinter(dateTimeFormatter),
				new LocalDateTimeParser(dateTimeFormatter),
				LocalDateTime.class);

		addFormatterForFields(registry,
				new ReadableInstantPrinter(dateTimeFormatter),
				new DateTimeParser(dateTimeFormatter),
				ReadableInstant.class);

		// 为字段类型为Period的字段注册格式化器
		registry.addFormatterForFieldType(Period.class, new PeriodFormatter());
		// 为字段类型为Duration的字段注册格式化器
		registry.addFormatterForFieldType(Duration.class, new DurationFormatter());
		// 为字段类型为YearMonth的字段注册格式化器
		registry.addFormatterForFieldType(YearMonth.class, new YearMonthFormatter());
		// 为字段类型为MonthDay的字段注册格式化器
		registry.addFormatterForFieldType(MonthDay.class, new MonthDayFormatter());

		// 如果指定了DATE_TIME类型的格式化器，则为Date和Calendar类型注册格式化器以保持向后兼容性
		if (this.formatters.containsKey(Type.DATE_TIME)) {
			addFormatterForFields(registry,
					new ReadableInstantPrinter(dateTimeFormatter),
					new DateTimeParser(dateTimeFormatter),
					Date.class, Calendar.class);
		}

		// 为字段上的JodaDateTimeFormatAnnotationFormatterFactory注解注册格式化器
		registry.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory());
	}

	private DateTimeFormatter getFormatter(Type type) {
		// 获取给定类型对应的DateTimeFormatter对象
		DateTimeFormatter formatter = this.formatters.get(type);
		// 如果找到对应的formatter，则直接返回
		if (formatter != null) {
			return formatter;
		}
		// 否则，获取对应类型的备用formatter
		DateTimeFormatter fallbackFormatter = getFallbackFormatter(type);
		// 使用备用formatter创建DateTimeFormatter对象
		return this.factories.get(type).createDateTimeFormatter(fallbackFormatter);
	}

	private DateTimeFormatter getFallbackFormatter(Type type) {
		switch (type) {
			case DATE: return DateTimeFormat.shortDate();
			case TIME: return DateTimeFormat.shortTime();
			default: return DateTimeFormat.shortDateTime();
		}
	}

	private void addFormatterForFields(FormatterRegistry registry, Printer<?> printer,
			Parser<?> parser, Class<?>... fieldTypes) {

		// 遍历字段类型集合，为每种字段类型添加格式化器
		for (Class<?> fieldType : fieldTypes) {
			registry.addFormatterForFieldType(fieldType, printer, parser);
		}
	}

}
