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

package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.Nullable;

import java.util.Set;

/**
 * 一个常见的 {@link org.springframework.core.convert.ConversionService} 配置的工厂。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 */
public final class ConversionServiceFactory {

	private ConversionServiceFactory() {
	}


	/**
	 * 使用给定的目标 ConverterRegistry 注册给定的转换器对象。
	 *
	 * @param converters 转换器对象集合：实现 {@link Converter}、{@link ConverterFactory} 或 {@link GenericConverter}
	 * @param registry 目标注册表
	 */
	public static void registerConverters(@Nullable Set<?> converters, ConverterRegistry registry) {
		if (converters == null) {
			return;
		}
		// 遍历转换器列表
		for (Object converter : converters) {
			// 判断转换器类型并添加到注册表中
			if (converter instanceof GenericConverter) {
				// 如果是GenericConverter类型，则添加到注册表中
				registry.addConverter((GenericConverter) converter);
			} else if (converter instanceof Converter<?, ?>) {
				// 如果是Converter类型，则添加到注册表中
				registry.addConverter((Converter<?, ?>) converter);
			} else if (converter instanceof ConverterFactory<?, ?>) {
				// 如果是ConverterFactory类型，则添加到注册表中
				registry.addConverterFactory((ConverterFactory<?, ?>) converter);
			} else {
				// 如果转换器不是上述类型之一，则抛出异常
				throw new IllegalArgumentException("Each converter object must implement one of the " +
						"Converter, ConverterFactory, or GenericConverter interfaces");
			}
		}
	}

}
