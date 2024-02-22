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

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.format.datetime.DateFormatterRegistrar;

import java.time.*;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * 安装所需的较低级别类型转换器，以将 JSR-310 支持集成到 Spring 的字段格式化系统中。
 *
 * <p>注意：{@link DateTimeFormatterRegistrar} 安装这些转换器，但不依赖于它们进行格式化。 它们只是注册为不同 JSR-310 值类型之间的自定义转换场景以及 {@link java.util.Calendar} 和 JSR-310 值类型之间的转换。
 *
 * @author Juergen Hoeller
 * @since 4.0.1
 */
final class DateTimeConverters {

	private DateTimeConverters() {
	}


	/**
	 * 将转换器安装到转换器注册表中。
	 *
	 * @param registry 转换器注册表
	 */
	public static void registerConverters(ConverterRegistry registry) {
		// 添加日期转换器
		DateFormatterRegistrar.addDateConverters(registry);

		registry.addConverter(new LocalDateTimeToLocalDateConverter());
		registry.addConverter(new LocalDateTimeToLocalTimeConverter());
		registry.addConverter(new ZonedDateTimeToLocalDateConverter());
		registry.addConverter(new ZonedDateTimeToLocalTimeConverter());
		registry.addConverter(new ZonedDateTimeToLocalDateTimeConverter());
		registry.addConverter(new ZonedDateTimeToOffsetDateTimeConverter());
		registry.addConverter(new ZonedDateTimeToInstantConverter());
		registry.addConverter(new OffsetDateTimeToLocalDateConverter());
		registry.addConverter(new OffsetDateTimeToLocalTimeConverter());
		registry.addConverter(new OffsetDateTimeToLocalDateTimeConverter());
		registry.addConverter(new OffsetDateTimeToZonedDateTimeConverter());
		registry.addConverter(new OffsetDateTimeToInstantConverter());
		registry.addConverter(new CalendarToZonedDateTimeConverter());
		registry.addConverter(new CalendarToOffsetDateTimeConverter());
		registry.addConverter(new CalendarToLocalDateConverter());
		registry.addConverter(new CalendarToLocalTimeConverter());
		registry.addConverter(new CalendarToLocalDateTimeConverter());
		registry.addConverter(new CalendarToInstantConverter());
		registry.addConverter(new LongToInstantConverter());
		registry.addConverter(new InstantToLongConverter());
	}

	private static ZonedDateTime calendarToZonedDateTime(Calendar source) {
		// 如果源对象是GregorianCalendar类型，则转换为ZonedDateTime类型
		if (source instanceof GregorianCalendar) {
			return ((GregorianCalendar) source).toZonedDateTime();
		} else {
			// 否则，使用源对象的时间戳和时区信息创建ZonedDateTime对象
			return ZonedDateTime.ofInstant(Instant.ofEpochMilli(source.getTimeInMillis()),
					source.getTimeZone().toZoneId());
		}
	}


	private static class LocalDateTimeToLocalDateConverter implements Converter<LocalDateTime, LocalDate> {

		@Override
		public LocalDate convert(LocalDateTime source) {
			return source.toLocalDate();
		}
	}


	private static class LocalDateTimeToLocalTimeConverter implements Converter<LocalDateTime, LocalTime> {

		@Override
		public LocalTime convert(LocalDateTime source) {
			return source.toLocalTime();
		}
	}


	private static class ZonedDateTimeToLocalDateConverter implements Converter<ZonedDateTime, LocalDate> {

		@Override
		public LocalDate convert(ZonedDateTime source) {
			return source.toLocalDate();
		}
	}


	private static class ZonedDateTimeToLocalTimeConverter implements Converter<ZonedDateTime, LocalTime> {

		@Override
		public LocalTime convert(ZonedDateTime source) {
			return source.toLocalTime();
		}
	}


	private static class ZonedDateTimeToLocalDateTimeConverter implements Converter<ZonedDateTime, LocalDateTime> {

		@Override
		public LocalDateTime convert(ZonedDateTime source) {
			return source.toLocalDateTime();
		}
	}

	private static class ZonedDateTimeToOffsetDateTimeConverter implements Converter<ZonedDateTime, OffsetDateTime> {

		@Override
		public OffsetDateTime convert(ZonedDateTime source) {
			return source.toOffsetDateTime();
		}
	}


	private static class ZonedDateTimeToInstantConverter implements Converter<ZonedDateTime, Instant> {

		@Override
		public Instant convert(ZonedDateTime source) {
			return source.toInstant();
		}
	}


	private static class OffsetDateTimeToLocalDateConverter implements Converter<OffsetDateTime, LocalDate> {

		@Override
		public LocalDate convert(OffsetDateTime source) {
			return source.toLocalDate();
		}
	}


	private static class OffsetDateTimeToLocalTimeConverter implements Converter<OffsetDateTime, LocalTime> {

		@Override
		public LocalTime convert(OffsetDateTime source) {
			return source.toLocalTime();
		}
	}


	private static class OffsetDateTimeToLocalDateTimeConverter implements Converter<OffsetDateTime, LocalDateTime> {

		@Override
		public LocalDateTime convert(OffsetDateTime source) {
			return source.toLocalDateTime();
		}
	}


	private static class OffsetDateTimeToZonedDateTimeConverter implements Converter<OffsetDateTime, ZonedDateTime> {

		@Override
		public ZonedDateTime convert(OffsetDateTime source) {
			return source.toZonedDateTime();
		}
	}


	private static class OffsetDateTimeToInstantConverter implements Converter<OffsetDateTime, Instant> {

		@Override
		public Instant convert(OffsetDateTime source) {
			return source.toInstant();
		}
	}


	private static class CalendarToZonedDateTimeConverter implements Converter<Calendar, ZonedDateTime> {

		@Override
		public ZonedDateTime convert(Calendar source) {
			return calendarToZonedDateTime(source);
		}
	}


	private static class CalendarToOffsetDateTimeConverter implements Converter<Calendar, OffsetDateTime> {

		@Override
		public OffsetDateTime convert(Calendar source) {
			return calendarToZonedDateTime(source).toOffsetDateTime();
		}
	}


	private static class CalendarToLocalDateConverter implements Converter<Calendar, LocalDate> {

		@Override
		public LocalDate convert(Calendar source) {
			return calendarToZonedDateTime(source).toLocalDate();
		}
	}


	private static class CalendarToLocalTimeConverter implements Converter<Calendar, LocalTime> {

		@Override
		public LocalTime convert(Calendar source) {
			return calendarToZonedDateTime(source).toLocalTime();
		}
	}


	private static class CalendarToLocalDateTimeConverter implements Converter<Calendar, LocalDateTime> {

		@Override
		public LocalDateTime convert(Calendar source) {
			return calendarToZonedDateTime(source).toLocalDateTime();
		}
	}


	private static class CalendarToInstantConverter implements Converter<Calendar, Instant> {

		@Override
		public Instant convert(Calendar source) {
			return calendarToZonedDateTime(source).toInstant();
		}
	}


	private static class LongToInstantConverter implements Converter<Long, Instant> {

		@Override
		public Instant convert(Long source) {
			return Instant.ofEpochMilli(source);
		}
	}


	private static class InstantToLongConverter implements Converter<Instant, Long> {

		@Override
		public Long convert(Instant source) {
			return source.toEpochMilli();
		}
	}

}
