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

package org.springframework.core.convert.converter;

/**
 * 用于向类型转换系统注册转换器。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface ConverterRegistry {

	/**
	 * 将普通转换器添加到此注册表中。
	 * 可转换的源/目标类型对是从转换器的参数化类型派生的。
	 *
	 * @throws IllegalArgumentException 如果无法解析参数化类型
	 */
	void addConverter(Converter<?, ?> converter);

	/**
	 * 将普通转换器添加到此注册表中。
	 * 明确指定可转换的源/目标类型对。
	 * <p>允许为多个不同的类型对重用转换器，而无需为每个类型对创建转换器类。
	 *
	 * @since 3.1
	 */
	<S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Converter<? super S, ? extends T> converter);

	/**
	 * 将通用转换器添加到此注册表中。
	 */
	void addConverter(GenericConverter converter);

	/**
	 * 将范围转换器工厂添加到此注册表中。
	 * 可转换的源/目标类型对是从ConverterFactory的参数化类型派生的。
	 *
	 * @throws IllegalArgumentException 如果无法解析参数化类型
	 */
	void addConverterFactory(ConverterFactory<?, ?> factory);

	/**
	 * 从{@code sourceType}到{@code targetType}中删除任何转换器。
	 *
	 * @param sourceType 源类型
	 * @param targetType 目标类型
	 */
	void removeConvertible(Class<?> sourceType, Class<?> targetType);

}
