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

package org.springframework.core.convert;

import org.springframework.lang.Nullable;

/**
 * 类型转换的服务接口。这是进入转换系统的入口点。调用{@link #convert(Object, Class)}使用此系统执行线程安全的类型转换。
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @since 3.0
 */
public interface ConversionService {

	/**
	 * 如果{@code sourceType}的对象可以转换为{@code targetType}，则返回{@code true}。
	 * <p>如果此方法返回{@code true}，则意味着{@link #convert(Object, Class)}能够将{@code sourceType}的实例转换为{@code targetType}。
	 * <p>关于集合、数组和映射类型的特别说明：
	 * 对于集合、数组和映射类型之间的转换，即使调用convert可能仍会生成{@link ConversionException}（如果底层元素不可转换），此方法也将返回{@code true}。
	 * 调用者在使用集合和映射时应处理此特殊情况。
	 *
	 * @param sourceType 要转换的源类型（如果源为{@code null}，则可以为{@code null}）
	 * @param targetType 要转换为的目标类型（必需）
	 * @return 如果可以执行转换，则为{@code true}；否则为{@code false}
	 * @throws IllegalArgumentException 如果{@code targetType}为{@code null}
	 */
	boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType);

	/**
	 * 如果{@code sourceType}的对象可以转换为{@code targetType}，则返回{@code true}。
	 * TypeDescriptors提供有关转换将发生的源和目标位置的附加上下文，通常是对象字段或属性位置。
	 * <p>如果此方法返回{@code true}，则意味着{@link #convert(Object, TypeDescriptor, TypeDescriptor)}能够将{@code sourceType}的实例转换为{@code targetType}。
	 * <p>关于集合、数组和映射类型的特别说明：
	 * 对于集合、数组和映射类型之间的转换，即使调用convert可能仍会生成{@link ConversionException}（如果底层元素不可转换），此方法也将返回{@code true}。
	 * 调用者在使用集合和映射时应处理此特殊情况。
	 *
	 * @param sourceType 关于要转换的源类型的上下文（如果源为{@code null}，则可以为{@code null}）
	 * @param targetType 要转换为的目标类型的上下文（必需）
	 * @return 如果可以在源和目标类型之间执行转换，则为{@code true}；否则为{@code false}
	 * @throws IllegalArgumentException 如果{@code targetType}为{@code null}
	 */
	boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

	/**
	 * 将给定的 {@code source} 转换为指定的 {@code targetType}。
	 *
	 * @param source     要转换的源对象 (可能是 {@code null})
	 * @param targetType 要转换为 (必需) 的目标类型
	 * @return 转换后的对象，targetType的一个实例
	 * @throws ConversionException      如果发生转换异常
	 * @throws IllegalArgumentException 如果目标类型为 {@code null}
	 */
	@Nullable
	<T> T convert(@Nullable Object source, Class<T> targetType);

	/**
	 * 将给定的{@code source}转换为指定的{@code targetType}。
	 * TypeDescriptors提供有关转换将发生的源和目标位置的附加上下文，通常是对象字段或属性位置。
	 *
	 * @param source     要转换的源对象（可以为{@code null}）
	 * @param sourceType 关于要转换的源类型的上下文（如果源为{@code null}，则可以为{@code null}）
	 * @param targetType 要转换为的目标类型的上下文（必需）
	 * @return 转换后的对象，一个{@link TypeDescriptor#getObjectType() targetType}的实例
	 * @throws ConversionException      如果发生转换异常
	 * @throws IllegalArgumentException 如果targetType为{@code null}，或者{@code sourceType}为{@code null}但源不为{@code null}
	 */
	@Nullable
	Object convert(@Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

}
