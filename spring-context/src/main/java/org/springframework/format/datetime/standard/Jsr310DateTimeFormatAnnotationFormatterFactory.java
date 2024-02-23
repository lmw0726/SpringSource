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

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;

/**
 * 使用 JDK 8 中的 JSR-310 <code>java.time</code> 包格式化带有 {@link DateTimeFormat} 注解的字段。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see org.springframework.format.annotation.DateTimeFormat
 * @since 4.0
 */
public class Jsr310DateTimeFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
		implements AnnotationFormatterFactory<DateTimeFormat> {
	/**
	 * 字段类型
	 */
	private static final Set<Class<?>> FIELD_TYPES;

	static {
		// 创建可以使用 @DateTimeFormat 注解的字段类型集合。
		Set<Class<?>> fieldTypes = new HashSet<>(8);
		fieldTypes.add(LocalDate.class);
		fieldTypes.add(LocalTime.class);
		fieldTypes.add(LocalDateTime.class);
		fieldTypes.add(ZonedDateTime.class);
		fieldTypes.add(OffsetDateTime.class);
		fieldTypes.add(OffsetTime.class);
		FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
	}


	@Override
	public final Set<Class<?>> getFieldTypes() {
		return FIELD_TYPES;
	}

	@Override
	public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
		// 根据注解和字段类型获取日期时间格式化器
		DateTimeFormatter formatter = getFormatter(annotation, fieldType);

		// 对于ISO_LOCAL_*变体进行高效打印，因为它们的速度是其他方法的两倍
		// 如果格式化器是ISO_DATE
		if (formatter == DateTimeFormatter.ISO_DATE) {
			// 如果字段类型是本地的
			if (isLocal(fieldType)) {
				// 使用ISO_LOCAL_DATE
				formatter = DateTimeFormatter.ISO_LOCAL_DATE;
			}
		} else if (formatter == DateTimeFormatter.ISO_TIME) {
			// 如果格式化器是ISO_TIME
			// 如果字段类型是本地的
			if (isLocal(fieldType)) {
				// 使用ISO_LOCAL_TIME
				formatter = DateTimeFormatter.ISO_LOCAL_TIME;
			}
		} else if (formatter == DateTimeFormatter.ISO_DATE_TIME) {
			// 如果格式化器是ISO_DATE_TIME
			// 如果字段类型是本地的
			if (isLocal(fieldType)) {
				// 使用ISO_LOCAL_DATE_TIME
				formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
			}
		}

		// 返回基于日期时间格式化器的TemporalAccessorPrinter
		return new TemporalAccessorPrinter(formatter);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
		// 存储解析后的回退模式的列表
		List<String> resolvedFallbackPatterns = new ArrayList<>();
		// 遍历注解中的回退模式数组
		for (String fallbackPattern : annotation.fallbackPatterns()) {
			// 解析嵌入值后的回退模式
			String resolvedFallbackPattern = resolveEmbeddedValue(fallbackPattern);
			// 如果回退模式长度不为零
			if (StringUtils.hasLength(resolvedFallbackPattern)) {
				// 将解析后的回退模式添加到列表中
				resolvedFallbackPatterns.add(resolvedFallbackPattern);
			}
		}

		// 获取字段的格式化器
		DateTimeFormatter formatter = getFormatter(annotation, fieldType);
		// 创建并返回一个TemporalAccessorParser对象，用于解析时间对象
		return new TemporalAccessorParser((Class<? extends TemporalAccessor>) fieldType,
				formatter, resolvedFallbackPatterns.toArray(new String[0]), annotation);
	}

	/**
	 * 用于创建 {@link DateTimeFormatter} 的工厂方法。
	 *
	 * @param annotation 字段的格式注解
	 * @param fieldType 字段的声明类型
	 * @return 一个 {@link DateTimeFormatter} 实例
	 */
	protected DateTimeFormatter getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
		// 创建一个DateTimeFormatterFactory对象
		DateTimeFormatterFactory factory = new DateTimeFormatterFactory();
		// 解析嵌入值后的样式
		String style = resolveEmbeddedValue(annotation.style());
		// 如果样式长度不为零
		if (StringUtils.hasLength(style)) {
			// 设置样式模式
			factory.setStylePattern(style);
		}
		// 设置ISO标准
		factory.setIso(annotation.iso());
		// 解析嵌入值后的模式
		String pattern = resolveEmbeddedValue(annotation.pattern());
		// 如果模式长度不为零
		if (StringUtils.hasLength(pattern)) {
			// 设置模式
			factory.setPattern(pattern);
		}
		// 创建并返回一个DateTimeFormatter对象
		return factory.createDateTimeFormatter();
	}

	private boolean isLocal(Class<?> fieldType) {
		return fieldType.getSimpleName().startsWith("Local");
	}

}
