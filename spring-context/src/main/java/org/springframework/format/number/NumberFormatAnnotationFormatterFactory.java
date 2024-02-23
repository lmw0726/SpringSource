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

package org.springframework.format.number;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.annotation.NumberFormat.Style;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 格式化带有 {@link NumberFormat} 注解的字段。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @see NumberFormat
 * @since 3.0
 */
public class NumberFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
		implements AnnotationFormatterFactory<NumberFormat> {

	@Override
	public Set<Class<?>> getFieldTypes() {
		return NumberUtils.STANDARD_NUMBER_TYPES;
	}

	@Override
	public Printer<Number> getPrinter(NumberFormat annotation, Class<?> fieldType) {
		return configureFormatterFrom(annotation);
	}

	@Override
	public Parser<Number> getParser(NumberFormat annotation, Class<?> fieldType) {
		return configureFormatterFrom(annotation);
	}

	/**
	 * 根据注解配置格式化程序。
	 *
	 * @param annotation 注解
	 * @return 格式化程序
	 */
	private Formatter<Number> configureFormatterFrom(NumberFormat annotation) {
		// 解析嵌入值注解中的模式
		String pattern = resolveEmbeddedValue(annotation.pattern());
		// 如果模式长度大于0，返回数字样式格式化器，使用解析的模式
		if (StringUtils.hasLength(pattern)) {
			return new NumberStyleFormatter(pattern);
		} else {
			// 如果模式长度为0，根据样式创建相应的格式化器
			Style style = annotation.style();
			if (style == Style.CURRENCY) {
				// 如果是当前区域设置的货币格式，则创建货币样式格式化器
				return new CurrencyStyleFormatter();
			} else if (style == Style.PERCENT) {
				// 如果是当前区域设置的百分比格式，则创建百分比样式格式化器
				return new PercentStyleFormatter();
			} else {
				// 否则创建空的数字化样式格式化器
				return new NumberStyleFormatter();
			}
		}
	}

}
