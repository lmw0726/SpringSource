/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.core.convert.TypeDescriptor;

/**
 * 允许 {@link Converter}、{@link GenericConverter} 或 {@link ConverterFactory}
 * 根据 {@code source} 和 {@code target} {@link TypeDescriptor} 的属性有条件地执行。
 *
 * <p>通常用于根据字段或类级别的特征（例如注解或方法）有选择地匹配自定义转换逻辑。例如，当从 String 字段转换为 Date 字段时，
 * 如果目标字段也已用 {@code @DateTimeFormat} 注解，实现可能会返回 {@code true}。
 *
 * <p>另一个例子是，当从 String 字段转换为 Account 字段时，如果目标 Account 类定义了一个 {@code public static findAccount(String)} 方法，
 * 实现可能会返回 {@code true}。
 *
 * @author Phillip Webb
 * @author Keith Donald
 * @see Converter
 * @see GenericConverter
 * @see ConverterFactory
 * @see ConditionalGenericConverter
 * @since 3.2
 */
public interface ConditionalConverter {

	/**
	 * 是否应该选择当前正在考虑的从 {@code sourceType} 到 {@code targetType} 的转换？
	 *
	 * @param sourceType 我们正在转换的字段的类型描述符
	 * @param targetType 我们正在转换为的字段的类型描述符
	 * @return 如果应该进行转换，则为true，否则为false
	 */
	boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType);

}
