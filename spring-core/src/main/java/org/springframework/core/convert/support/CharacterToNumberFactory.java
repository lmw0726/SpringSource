/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.NumberUtils;

/**
 * 将 Character 转换为任何 JDK 标准数字实现。
 *
 * <p>支持的 Number 类包括 Byte、Short、Integer、Float、Double、Long、BigInteger、BigDecimal。此类
 * 委托给 {@link NumberUtils#convertNumberToTargetClass(Number, Class)} 来执行转换。
 *
 * @author Keith Donald
 * @since 3.0
 * @see java.lang.Byte
 * @see java.lang.Short
 * @see java.lang.Integer
 * @see java.lang.Long
 * @see java.math.BigInteger
 * @see java.lang.Float
 * @see java.lang.Double
 * @see java.math.BigDecimal
 * @see NumberUtils
 */
final class CharacterToNumberFactory implements ConverterFactory<Character, Number> {

	@Override
	public <T extends Number> Converter<Character, T> getConverter(Class<T> targetType) {
		return new CharacterToNumber<>(targetType);
	}

	private static final class CharacterToNumber<T extends Number> implements Converter<Character, T> {
		/**
		 * 目标类型
		 */
		private final Class<T> targetType;

		public CharacterToNumber(Class<T> targetType) {
			this.targetType = targetType;
		}

		@Override
		public T convert(Character source) {
			// 将Number转换为目标类型
			return NumberUtils.convertNumberToTargetClass((short) source.charValue(), this.targetType);
		}
	}

}
