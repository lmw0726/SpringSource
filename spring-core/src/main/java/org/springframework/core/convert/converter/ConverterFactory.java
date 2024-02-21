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

package org.springframework.core.convert.converter;

/**
 * 一种“范围”转换器的工厂，可以将对象从 S 转换为 R 的子类型。
 *
 * <p>实现还可以实现 {@link ConditionalConverter}。
 *
 * @author Keith Donald
 * @since 3.0
 * @param <S> 此工厂创建的转换器可以从中转换的源类型
 * @param <R> 转换器创建的工厂可以转换为的目标范围（或基本）类型；例如，数字子类型的一组 {@link Number}。
 * @see ConditionalConverter
 */
public interface ConverterFactory<S, R> {

	/**
	 * 获取将从 S 转换为目标类型 T 的转换器，其中 T 也是 R 的实例。
	 * @param <T> 目标类型
	 * @param targetType 要转换为的目标类型
	 * @return 从 S 到 T 的转换器
	 */
	<T extends R> Converter<S, T> getConverter(Class<T> targetType);

}
