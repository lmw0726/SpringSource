/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.convert.support;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.NumberUtils;

/**
 * 将任何 JDK 标准数字实现转换为另一个 JDK 标准数字实现。
 *
 * <p>支持的 Number 类包括 Byte、Short、Integer、Float、Double、Long、BigInteger、BigDecimal。此类
 * 委托给 {@link NumberUtils#convertNumberToTargetClass(Number, Class)} 来执行转换。
 *
 * @author Keith Donald
 * @see java.lang.Byte
 * @see java.lang.Short
 * @see java.lang.Integer
 * @see java.lang.Long
 * @see java.math.BigInteger
 * @see java.lang.Float
 * @see java.lang.Double
 * @see java.math.BigDecimal
 * @see NumberUtils
 * @since 3.0
 */
final class NumberToNumberConverterFactory implements ConverterFactory<Number, Number>, ConditionalConverter {

	@Override
	public <T extends Number> Converter<Number, T> getConverter(Class<T> targetType) {
		return new NumberToNumber<>(targetType);
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return !sourceType.equals(targetType);
	}


	private static final class NumberToNumber<T extends Number> implements Converter<Number, T> {
		/**
		 * 目标类型
		 */
		private final Class<T> targetType;

		NumberToNumber(Class<T> targetType) {
			this.targetType = targetType;
		}

		@Override
		public T convert(Number source) {
			return NumberUtils.convertNumberToTargetClass(source, this.targetType);
		}
	}

}
