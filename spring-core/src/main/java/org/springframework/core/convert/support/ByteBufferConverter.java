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

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 直接将 {@link ByteBuffer} 转换为 {@code byte[]}，以及直接将 {@code byte[]} 转换为 {@link ByteBuffer}，并间接地
 * 通过 {@code byte[]} 转换为 {@link ConversionService} 支持的任何类型。
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 4.0
 */
final class ByteBufferConverter implements ConditionalGenericConverter {

	/**
	 * {@code ByteBuffer} 类型描述符。
	 */
	private static final TypeDescriptor BYTE_BUFFER_TYPE = TypeDescriptor.valueOf(ByteBuffer.class);

	/**
	 * {@code byte[]} 类型描述符。
	 */
	private static final TypeDescriptor BYTE_ARRAY_TYPE = TypeDescriptor.valueOf(byte[].class);

	/**
	 * 可转换的类型对集合。
	 */
	private static final Set<ConvertiblePair> CONVERTIBLE_PAIRS;

	static {
		Set<ConvertiblePair> convertiblePairs = new HashSet<>(4);
		convertiblePairs.add(new ConvertiblePair(ByteBuffer.class, byte[].class));
		convertiblePairs.add(new ConvertiblePair(byte[].class, ByteBuffer.class));
		convertiblePairs.add(new ConvertiblePair(ByteBuffer.class, Object.class));
		convertiblePairs.add(new ConvertiblePair(Object.class, ByteBuffer.class));
		CONVERTIBLE_PAIRS = Collections.unmodifiableSet(convertiblePairs);
	}

	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public ByteBufferConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return CONVERTIBLE_PAIRS;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 检查目标类型是否可赋值给ByteBuffer类型
		boolean byteBufferTarget = targetType.isAssignableTo(BYTE_BUFFER_TYPE);

		// 如果源类型可赋值给ByteBuffer类型
		if (sourceType.isAssignableTo(BYTE_BUFFER_TYPE)) {
			// 返回目标类型也可赋值给ByteBuffer类型，或者检查从ByteBuffer到目标类型的匹配情况
			return (byteBufferTarget || matchesFromByteBuffer(targetType));
		}

		// 如果目标类型可赋值给ByteBuffer类型
		return (byteBufferTarget && matchesToByteBuffer(sourceType));
	}

	private boolean matchesFromByteBuffer(TypeDescriptor targetType) {
		return (targetType.isAssignableTo(BYTE_ARRAY_TYPE) ||
				this.conversionService.canConvert(BYTE_ARRAY_TYPE, targetType));
	}

	private boolean matchesToByteBuffer(TypeDescriptor sourceType) {
		return (sourceType.isAssignableTo(BYTE_ARRAY_TYPE) ||
				this.conversionService.canConvert(sourceType, BYTE_ARRAY_TYPE));
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 检查目标类型是否可赋值给ByteBuffer类型
		boolean byteBufferTarget = targetType.isAssignableTo(BYTE_BUFFER_TYPE);

		// 如果源是ByteBuffer类型
		if (source instanceof ByteBuffer) {
			ByteBuffer buffer = (ByteBuffer) source;
			// 如果目标类型也是ByteBuffer类型，则返回ByteBuffer的副本，否则转换为目标类型
			return (byteBufferTarget ? buffer.duplicate() : convertFromByteBuffer(buffer, targetType));
		}

		// 如果目标类型是ByteBuffer类型，则将源转换为ByteBuffer类型
		if (byteBufferTarget) {
			return convertToByteBuffer(source, sourceType);
		}

		// 不应该发生的情况
		throw new IllegalStateException("Unexpected source/target types");
	}

	@Nullable
	private Object convertFromByteBuffer(ByteBuffer source, TypeDescriptor targetType) {
		// 创建与源ByteBuffer大小相同的字节数组
		byte[] bytes = new byte[source.remaining()];
		// 从源ByteBuffer中读取字节到字节数组中
		source.get(bytes);

		// 如果目标类型可赋值给字节数组类型，则直接返回字节数组，否则使用转换服务进行转换
		if (targetType.isAssignableTo(BYTE_ARRAY_TYPE)) {
			return bytes;
		}
		return this.conversionService.convert(bytes, BYTE_ARRAY_TYPE, targetType);
	}

	private Object convertToByteBuffer(@Nullable Object source, TypeDescriptor sourceType) {
		// 将源对象转换为字节数组，如果源对象已经是字节数组，则直接使用，否则通过转换服务进行转换
		byte[] bytes = (byte[]) (source instanceof byte[] ? source :
				this.conversionService.convert(source, sourceType, BYTE_ARRAY_TYPE));

		// 如果转换后的字节数组为空，则创建一个空的ByteBuffer并返回
		if (bytes == null) {
			return ByteBuffer.wrap(new byte[0]);
		}

		// 创建一个ByteBuffer并将字节数组放入其中
		ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
		byteBuffer.put(bytes);

		// 将ByteBuffer的位置设为0并返回
		// 在JDK 8上运行的JDK 9 plus上进行编译所需的额外强制转换，因为否则将选择在JDK 8上不可用的重写ByteBuffer返回rewind方法。
		return ((Buffer) byteBuffer).rewind();
	}

}
