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

package org.springframework.format.datetime;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 使用 {@link DateFormatter} 格式化带有 {@link DateTimeFormat} 注解的字段。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.2
 * @see org.springframework.format.datetime.joda.JodaDateTimeFormatAnnotationFormatterFactory
 */
public class DateTimeFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
		implements AnnotationFormatterFactory<DateTimeFormat> {
	/**
	 * 允许注解的字段类型
	 */
	private static final Set<Class<?>> FIELD_TYPES;

	static {
		Set<Class<?>> fieldTypes = new HashSet<>(4);
		fieldTypes.add(Date.class);
		fieldTypes.add(Calendar.class);
		fieldTypes.add(Long.class);
		FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
	}


	@Override
	public Set<Class<?>> getFieldTypes() {
		return FIELD_TYPES;
	}

	@Override
	public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
		return getFormatter(annotation, fieldType);
	}

	@Override
	public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
		return getFormatter(annotation, fieldType);
	}

	protected Formatter<Date> getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
		DateFormatter formatter = new DateFormatter();
		// 设置来源注解
		formatter.setSource(annotation);
		// 设置是否使用 ISO 格式
		formatter.setIso(annotation.iso());

		String style = resolveEmbeddedValue(annotation.style());
		if (StringUtils.hasLength(style)) {
			// 如果样式非空，则设置样式模式
			formatter.setStylePattern(style);
		}

		String pattern = resolveEmbeddedValue(annotation.pattern());
		if (StringUtils.hasLength(pattern)) {
			// 如果模式非空，则设置模式
			formatter.setPattern(pattern);
		}

		List<String> resolvedFallbackPatterns = new ArrayList<>();
		for (String fallbackPattern : annotation.fallbackPatterns()) {
			String resolvedFallbackPattern = resolveEmbeddedValue(fallbackPattern);
			if (StringUtils.hasLength(resolvedFallbackPattern)) {
				// 如果回退模式非空，则将其添加到已解析回退模式列表中
				resolvedFallbackPatterns.add(resolvedFallbackPattern);
			}
		}
		if (!resolvedFallbackPatterns.isEmpty()) {
			// 如果已解析的回退模式列表不为空，则设置回退模式
			formatter.setFallbackPatterns(resolvedFallbackPatterns.toArray(new String[0]));
		}

		return formatter;
	}

}
