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

package org.springframework.core.convert.converter;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Set;

/**
 * 用于在两个或多个类型之间进行转换的通用转换器接口。
 * <p> 这是转换器 SPI 接口中最灵活的接口，但也是最复杂的接口。
 * 它是灵活的，因为 GenericConverter 可能支持在多个源/目标类型对之间进行转换（参见 getConvertibleTypes()）。
 * 此外，GenericConverter 实现在类型转换过程中可以访问源/目标 TypeDescriptor 的字段上下文。
 * 这允许解析源和目标字段元数据，如注解和泛型信息，这些信息可以用来影响转换逻辑。
 * <p> 当简单的 Converter 或 ConverterFactory 接口足够时，通常不应使用此接口。
 * <p> 实现类还可以实现 ConditionalConverter 接口。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @see TypeDescriptor
 * @see Converter
 * @see ConverterFactory
 * @see ConditionalConverter
 * @since 3.0
 */
public interface GenericConverter {

	/**
	 * 返回此转换器可以在之间转换的源类型和目标类型。
	 * <p> 每个条目都是可转换的源到目标类型对。
	 * <p> 对于 {@link ConditionalConverter 条件转换器}，此方法可能返回 {@code null} 以指示应考虑所有源到目标对。
	 */
	@Nullable
	Set<ConvertiblePair> getConvertibleTypes();

	/**
	 * 通过{@code TypeDescriptor}将源对象转为目标类型描述
	 *
	 * @param source     要转换的源对象 (可能是 {@code null})
	 * @param sourceType 我们正在转换的字段的类型描述符
	 * @param targetType 我们正在转换为的字段的类型描述符
	 * @return 转换后的对象
	 */
	@Nullable
	Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType);


	/**
	 * 源类型和目标类型的持有者。
	 */
	final class ConvertiblePair {
		/**
		 * 源类型
		 */
		private final Class<?> sourceType;
		/**
		 * 目标类型
		 */
		private final Class<?> targetType;

		/**
		 * 创建一个新的源类型到目标类型的对应关系。
		 *
		 * @param sourceType 源类型
		 * @param targetType 目标类型
		 */
		public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
			Assert.notNull(sourceType, "Source type must not be null");
			Assert.notNull(targetType, "Target type must not be null");
			this.sourceType = sourceType;
			this.targetType = targetType;
		}

		public Class<?> getSourceType() {
			return this.sourceType;
		}

		public Class<?> getTargetType() {
			return this.targetType;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || other.getClass() != ConvertiblePair.class) {
				return false;
			}
			ConvertiblePair otherPair = (ConvertiblePair) other;
			return (this.sourceType == otherPair.sourceType && this.targetType == otherPair.targetType);
		}

		@Override
		public int hashCode() {
			return (this.sourceType.hashCode() * 31 + this.targetType.hashCode());
		}

		@Override
		public String toString() {
			return (this.sourceType.getName() + " -> " + this.targetType.getName());
		}
	}

}
