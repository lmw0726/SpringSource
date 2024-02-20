/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;

/**
 * 定义类型转换方法的接口。通常（但不一定）与 {@link PropertyEditorRegistry} 接口一起实现。
 *
 * <p><b>注意：</b>由于 TypeConverter 实现通常基于 {@link java.beans.PropertyEditor PropertyEditors}，
 * 而 PropertyEditors 不是线程安全的，因此 TypeConverters 本身也不被视为线程安全。
 *
 * @author Juergen Hoeller
 * @see SimpleTypeConverter
 * @see BeanWrapperImpl
 * @since 2.0
 */
public interface TypeConverter {

	/**
	 * 将值转换为指定类型（如果必要，从 String 转换）。
	 * <p>从 String 到任何类型的转换通常将使用 PropertyEditor 类的 {@code setAsText} 方法，
	 * 或者在 ConversionService 中使用 Spring 转换器。
	 *
	 * @param value        要转换的值
	 * @param requiredType 我们必须转换到的类型（如果不知道，则为 {@code null}，例如在集合元素的情况下）
	 * @return 新值，可能是类型转换的结果
	 * @throws TypeMismatchException 如果类型转换失败
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 */
	@Nullable
	<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException;

	/**
	 * 将值转换为指定类型（如果必要，从 String 转换）。
	 * <p>从 String 到任何类型的转换通常将使用 PropertyEditor 类的 {@code setAsText} 方法，
	 * 或者在 ConversionService 中使用 Spring 转换器。
	 *
	 * @param value        要转换的值
	 * @param requiredType 我们必须转换到的类型（如果不知道，则为 {@code null}，例如在集合元素的情况下）
	 * @param methodParam  目标转换的方法参数（用于分析泛型类型；可能为 {@code null}）
	 * @return 新值，可能是类型转换的结果
	 * @throws TypeMismatchException 如果类型转换失败
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 */
	@Nullable
	<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
							 @Nullable MethodParameter methodParam) throws TypeMismatchException;

	/**
	 * 将值转换为指定类型（如果必要，从 String 转换）。
	 * <p>从 String 到任何类型的转换通常将使用 PropertyEditor 类的 {@code setAsText} 方法，
	 * 或者在 ConversionService 中使用 Spring 转换器。
	 *
	 * @param value        要转换的值
	 * @param requiredType 我们必须转换到的类型（如果不知道，则为 {@code null}，例如在集合元素的情况下）
	 * @param field        目标转换的反射字段（用于分析泛型类型；可能为 {@code null}）
	 * @return 新值，可能是类型转换的结果
	 * @throws TypeMismatchException 如果类型转换失败
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 */
	@Nullable
	<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
			throws TypeMismatchException;

	/**
	 * 将值转换为指定类型（如果必要，从 String 转换）。
	 * <p>从 String 到任何类型的转换通常将使用 PropertyEditor 类的 {@code setAsText} 方法，
	 * 或者在 ConversionService 中使用 Spring 转换器。
	 *
	 * @param value          要转换的值
	 * @param requiredType   我们必须转换到的类型（如果不知道，则为 {@code null}，例如在集合元素的情况下）
	 * @param typeDescriptor 要使用的类型描述符（可能为 {@code null}）
	 * @return 新值，可能是类型转换的结果
	 * @throws TypeMismatchException 如果类型转换失败
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 * @since 5.1.4
	 */
	@Nullable
	default <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
									 @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

		throw new UnsupportedOperationException("TypeDescriptor resolution not supported");
	}

}
