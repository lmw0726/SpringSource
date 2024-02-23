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
import org.joda.time.format.DateTimeFormatter;
import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 使用 Joda-Time 格式化带有 {@link DateTimeFormat} 注解的字段。
 *
 * <p><b>注意:</b> 自 Spring 4.0 起，Spring 的 Joda-Time 支持需要 Joda-Time 2.x。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @see DateTimeFormat
 * @since 3.0
 * @deprecated 自 5.3 起，推荐使用标准的 JSR-310 支持
 */
@Deprecated
public class JodaDateTimeFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
		implements AnnotationFormatterFactory<DateTimeFormat> {
	/**
	 * 允许注解的字段类型
	 */
	private static final Set<Class<?>> FIELD_TYPES;

	static {
		// 创建可以用 @DateTimeFormat 注释的字段类型集合。
		// 注意：3 个 ReadablePartial 具体类型被显式注册，因为对于每个这些类型，都存在 addFormatterForFieldType 规则
		// （如果我们不这样做，那么对于 LocalDate、LocalTime 和 LocalDateTime，将使用默认的 byType 规则，这不是我们想要的）
		Set<Class<?>> fieldTypes = new HashSet<>(8);
		// 添加 ReadableInstant 类型
		fieldTypes.add(ReadableInstant.class);
		// 添加 LocalDate 类型
		fieldTypes.add(LocalDate.class);
		// 添加 LocalTime 类型
		fieldTypes.add(LocalTime.class);
		// 添加 LocalDateTime 类型
		fieldTypes.add(LocalDateTime.class);
		// 添加 Date 类型
		fieldTypes.add(Date.class);
		// 添加 Calendar 类型
		fieldTypes.add(Calendar.class);
		// 添加 Long 类型
		fieldTypes.add(Long.class);
		// 将字段类型集合设置为不可修改的
		FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
	}


	@Override
	public final Set<Class<?>> getFieldTypes() {
		return FIELD_TYPES;
	}

	@Override
	public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatter formatter = getFormatter(annotation, fieldType);
		if (ReadablePartial.class.isAssignableFrom(fieldType)) {
			// 如果字段类型是 ReadablePartial 或其子类，则返回一个使用指定注解和字段类型的日期时间格式化程序
			return new ReadablePartialPrinter(formatter);
		} else if (ReadableInstant.class.isAssignableFrom(fieldType) || Calendar.class.isAssignableFrom(fieldType)) {
			// 如果字段类型是 ReadableInstant 或 Calendar 或其子类，则返回一个使用指定注解和字段类型的日期时间格式化程序
			// 假设注册了 Calendar->ReadableInstant 的转换器
			return new ReadableInstantPrinter(formatter);
		} else {
			// 否则，返回一个使用指定注解和字段类型的日期时间格式化程序
			// 假设注册了 Date->Long 的转换器
			return new MillisecondInstantPrinter(formatter);
		}
	}

	@Override
	public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
		if (LocalDate.class == fieldType) {
			// 如果字段类型是 LocalDate，则返回一个使用指定注解和字段类型的日期时间格式化程序
			return new LocalDateParser(getFormatter(annotation, fieldType));
		} else if (LocalTime.class == fieldType) {
			// 如果字段类型是 LocalTime，则返回一个使用指定注解和字段类型的日期时间格式化程序
			return new LocalTimeParser(getFormatter(annotation, fieldType));
		} else if (LocalDateTime.class == fieldType) {
			// 如果字段类型是 LocalDateTime，则返回一个使用指定注解和字段类型的日期时间格式化程序
			return new LocalDateTimeParser(getFormatter(annotation, fieldType));
		} else {
			// 否则，返回一个使用指定注解和字段类型的日期时间格式化程序
			return new DateTimeParser(getFormatter(annotation, fieldType));
		}
	}

	/**
	 * 用于创建 {@link DateTimeFormatter} 的工厂方法。
	 *
	 * @param annotation 字段的格式注解
	 * @param fieldType  字段的类型
	 * @return 一个 {@link DateTimeFormatter} 实例
	 * @since 3.2
	 */
	protected DateTimeFormatter getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatterFactory factory = new DateTimeFormatterFactory();
		// 解析嵌入值的样式
		String style = resolveEmbeddedValue(annotation.style());
		if (StringUtils.hasLength(style)) {
			// 如果样式非空，则设置样式
			factory.setStyle(style);
		}
		// 设置是否使用 ISO 格式
		factory.setIso(annotation.iso());
		// 解析嵌入值的模式
		String pattern = resolveEmbeddedValue(annotation.pattern());
		if (StringUtils.hasLength(pattern)) {
			// 如果模式非空，则设置模式
			factory.setPattern(pattern);
		}
		// 创建并返回日期时间格式工厂的日期时间格式化程序
		return factory.createDateTimeFormatter();
	}

}
