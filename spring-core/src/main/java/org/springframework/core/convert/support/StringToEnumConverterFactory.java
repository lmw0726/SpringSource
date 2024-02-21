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

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.lang.Nullable;

/**
 * 通过调用 {@link Enum#valueOf(Class, String)} 将 String 转换为 {@link java.lang.Enum}。
 *
 * @author Keith Donald
 * @author Stephane Nicoll
 * @since 3.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class StringToEnumConverterFactory implements ConverterFactory<String, Enum> {

	@Override
	public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
		return new StringToEnum(ConversionUtils.getEnumType(targetType));
	}


	private static class StringToEnum<T extends Enum> implements Converter<String, T> {
		/**
		 * 枚举类型
		 */
		private final Class<T> enumType;

		StringToEnum(Class<T> enumType) {
			this.enumType = enumType;
		}

		@Override
		@Nullable
		public T convert(String source) {
			if (source.isEmpty()) {
				// 它是一个空的枚举标识符: 将枚举值重置为null。
				return null;
			}
			return (T) Enum.valueOf(this.enumType, source.trim());
		}
	}

}
