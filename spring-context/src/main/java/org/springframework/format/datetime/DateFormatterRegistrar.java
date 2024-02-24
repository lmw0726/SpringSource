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

package org.springframework.format.datetime;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Calendar;
import java.util.Date;

/**
 * 配置基本的日期格式化以供Spring使用，主要用于{@link org.springframework.format.annotation.DateTimeFormat}声明。
 * 适用于{@link Date}、{@link Calendar}和{@code long}类型的字段。
 *
 * <p>设计用于直接实例化，但也通过静态的{@link #addDateConverters(ConverterRegistry)}实用方法公开，以供针对任何
 * {@code ConverterRegistry}实例进行临时使用。
 *
 * @author Phillip Webb
 * @see org.springframework.format.datetime.standard.DateTimeFormatterRegistrar
 * @see org.springframework.format.datetime.joda.JodaTimeFormatterRegistrar
 * @see FormatterRegistrar#registerFormatters
 * @since 3.2
 */
public class DateFormatterRegistrar implements FormatterRegistrar {

	/**
	 * 日期格式化器
	 */
	@Nullable
	private DateFormatter dateFormatter;


	/**
	 * 设置要注册的全局日期格式化程序。
	 * <p>如果未指定，将不会注册用于非注解{@link Date}和{@link Calendar}字段的通用格式化程序。
	 *
	 * @param dateFormatter DateFormatter，不能为空
	 */
	public void setFormatter(DateFormatter dateFormatter) {
		Assert.notNull(dateFormatter, "DateFormatter must not be null");
		this.dateFormatter = dateFormatter;
	}


	@Override
	public void registerFormatters(FormatterRegistry registry) {
		// 将日期转换器添加到指定的注册表中。
		addDateConverters(registry);
		// 为了保持向后兼容性，只有在指定了用户定义的格式化器时才注册 Date/Calendar 类型（参见 SPR-10105）
		if (this.dateFormatter != null) {
			// 如果存在日期格式化器，则将其添加到注册表中
			registry.addFormatter(this.dateFormatter);
			// 同时为 Calendar 类型字段注册日期格式化器
			registry.addFormatterForFieldType(Calendar.class, this.dateFormatter);
		}
		// 向注册表中添加字段注解的日期时间格式化器工厂
		registry.addFormatterForFieldAnnotation(new DateTimeFormatAnnotationFormatterFactory());
	}

	/**
	 * 将日期转换器添加到指定的注册表中。
	 *
	 * @param converterRegistry 要添加到的转换器注册表
	 */
	public static void addDateConverters(ConverterRegistry converterRegistry) {
		converterRegistry.addConverter(new DateToLongConverter());
		converterRegistry.addConverter(new DateToCalendarConverter());
		converterRegistry.addConverter(new CalendarToDateConverter());
		converterRegistry.addConverter(new CalendarToLongConverter());
		converterRegistry.addConverter(new LongToDateConverter());
		converterRegistry.addConverter(new LongToCalendarConverter());
	}


	private static class DateToLongConverter implements Converter<Date, Long> {

		@Override
		public Long convert(Date source) {
			return source.getTime();
		}
	}


	private static class DateToCalendarConverter implements Converter<Date, Calendar> {

		@Override
		public Calendar convert(Date source) {
			// 创建一个 Calendar 实例并设置其时间为源日期时间
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(source);
			return calendar;
		}
	}


	private static class CalendarToDateConverter implements Converter<Calendar, Date> {

		@Override
		public Date convert(Calendar source) {
			return source.getTime();
		}
	}


	private static class CalendarToLongConverter implements Converter<Calendar, Long> {

		@Override
		public Long convert(Calendar source) {
			return source.getTimeInMillis();
		}
	}


	private static class LongToDateConverter implements Converter<Long, Date> {

		@Override
		public Date convert(Long source) {
			return new Date(source);
		}
	}


	private static class LongToCalendarConverter implements Converter<Long, Calendar> {

		@Override
		public Calendar convert(Long source) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(source);
			return calendar;
		}
	}

}
