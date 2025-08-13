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

package org.springframework.core.io.buffer;

import org.springframework.util.Assert;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.function.IntPredicate;

/**
 * 对字节缓冲区的基础抽象。
 *
 * <p>{@code DataBuffer} 拥有独立的 {@linkplain #readPosition() 读} 和
 * {@linkplain #writePosition() 写} 位置，区别于 {@code ByteBuffer} 的单一
 * {@linkplain ByteBuffer#position() 位置}。因此，{@code DataBuffer} 在写入后读取时
 * 不需要 {@linkplain ByteBuffer#flip() flip} 操作。通常，以下不变式适用于读写位置和容量：
 *
 * <blockquote>
 *     <tt>0</tt> <tt>&lt;=</tt>
 *     <i>readPosition</i> <tt>&lt;=</tt>
 *     <i>writePosition</i> <tt>&lt;=</tt>
 *     <i>capacity</i>
 * </blockquote>
 *
 * <p>{@linkplain #capacity() 容量} 会按需扩展，类似于 {@code StringBuilder}。
 *
 * <p>{@code DataBuffer} 抽象的主要目的是提供一个对 {@link ByteBuffer} 的便捷封装，
 * 类似于 Netty 的 {@link io.netty.buffer.ByteBuf}，但同样适用于非 Netty 平台（如 Servlet 容器）。
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 * @see DataBufferFactory
 */
public interface DataBuffer {

	/**
	 * 返回创建该缓冲区的 {@link DataBufferFactory}。
	 * @return 创建该缓冲区的工厂
	 */
	DataBufferFactory factory();

	/**
	 * 返回缓冲区中第一个匹配给定谓词的字节的索引。
	 * @param predicate 用于匹配的谓词
	 * @param fromIndex 开始搜索的索引
	 * @return 第一个匹配字节的索引，找不到则返回 {@code -1}
	 */
	int indexOf(IntPredicate predicate, int fromIndex);

	/**
	 * 返回缓冲区中最后一个匹配给定谓词的字节的索引。
	 * @param predicate 用于匹配的谓词
	 * @param fromIndex 开始搜索的索引
	 * @return 最后一个匹配字节的索引，找不到则返回 {@code -1}
	 */
	int lastIndexOf(IntPredicate predicate, int fromIndex);

	/**
	 * 返回该数据缓冲区中可读取的字节数。
	 * @return 可读取的字节数
	 */
	int readableByteCount();

	/**
	 * 返回该数据缓冲区中可写入的字节数。
	 * @return 可写入的字节数
	 * @since 5.0.1
	 */
	int writableByteCount();

	/**
	 * 返回该缓冲区的容量（可容纳的字节数）。
	 * @return 容量
	 * @since 5.0.1
	 */
	int capacity();

	/**
	 * 设置该缓冲区的容量。
	 * <p>如果新容量小于当前容量，缓冲区内容将被截断。
	 * 如果新容量大于当前容量，缓冲区将被扩展。
	 * @param capacity 新容量
	 * @return 当前缓冲区
	 */
	DataBuffer capacity(int capacity);

	/**
	 * 确保当前缓冲区有足够的 {@link #writableByteCount()} 写入指定容量。
	 * 如果不足，则会扩容。
	 * @param capacity 需要确保的可写容量
	 * @return 当前缓冲区
	 * @since 5.1.4
	 */
	default DataBuffer ensureCapacity(int capacity) {
		return this;
	}

	/**
	 * 返回该缓冲区当前读取位置。
	 * @return 读取位置
	 * @since 5.0.1
	 */
	int readPosition();

	/**
	 * 设置该缓冲区的读取位置。
	 * @param readPosition 新的读取位置
	 * @return 当前缓冲区
	 * @throws IndexOutOfBoundsException 如果读取位置小于0或大于写入位置
	 * @since 5.0.1
	 */
	DataBuffer readPosition(int readPosition);

	/**
	 * 返回该缓冲区当前写入位置。
	 * @return 写入位置
	 * @since 5.0.1
	 */
	int writePosition();

	/**
	 * 设置该缓冲区的写入位置。
	 * @param writePosition 新的写入位置
	 * @return 当前缓冲区
	 * @throws IndexOutOfBoundsException 如果写入位置小于读取位置或大于容量
	 * @since 5.0.1
	 */
	DataBuffer writePosition(int writePosition);

	/**
	 * 读取指定索引处的单个字节。
	 * @param index 读取的索引
	 * @return 该索引处的字节
	 * @throws IndexOutOfBoundsException 如果索引越界
	 * @since 5.0.4
	 */
	byte getByte(int index);

	/**
	 * 从当前读取位置读取单个字节。
	 * @return 当前读取位置的字节
	 */
	byte read();

	/**
	 * 从当前读取位置读取数据到目标数组中。
	 * @param destination 写入的目标数组
	 * @return 当前缓冲区
	 */
	DataBuffer read(byte[] destination);

	/**
	 * 从当前读取位置读取最多 {@code length} 个字节到目标数组中，写入从 offset 开始。
	 * @param destination 写入的目标数组
	 * @param offset 目标数组的起始写入位置
	 * @param length 最大写入字节数
	 * @return 当前缓冲区
	 */
	DataBuffer read(byte[] destination, int offset, int length);

	/**
	 * 在当前写入位置写入单个字节。
	 * @param b 要写入的字节
	 * @return 当前缓冲区
	 */
	DataBuffer write(byte b);

	/**
	 * 在当前写入位置写入给定的字节数组。
	 * @param source 要写入的字节数组
	 * @return 当前缓冲区
	 */
	DataBuffer write(byte[] source);

	/**
	 * 在当前写入位置写入给定字节数组的部分内容，从 offset 开始，写入 length 个字节。
	 * @param source 要写入的字节数组
	 * @param offset 开始写入的偏移量
	 * @param length 最大写入字节数
	 * @return 当前缓冲区
	 */
	DataBuffer write(byte[] source, int offset, int length);

	/**
	 * 在当前写入位置写入一个或多个 {@code DataBuffer} 的内容。
	 * 调用者负责 {@linkplain DataBufferUtils#release(DataBuffer) 释放} 传入的缓冲区。
	 * @param buffers 要写入的缓冲区数组
	 * @return 当前缓冲区
	 */
	DataBuffer write(DataBuffer... buffers);

	/**
	 * 在当前写入位置写入一个或多个 {@link ByteBuffer} 的内容。
	 * @param buffers 要写入的字节缓冲区数组
	 * @return 当前缓冲区
	 */
	DataBuffer write(ByteBuffer... buffers);

	/**
	 * 使用给定的 {@code Charset} 编码写入指定的 {@code CharSequence}，
	 * 从当前写入位置开始。
	 * @param charSequence 要写入的字符序列
	 * @param charset 用于编码的字符集
	 * @return 当前缓冲区
	 * @since 5.1.4
	 */
	default DataBuffer write(CharSequence charSequence, Charset charset) {
		Assert.notNull(charSequence, "CharSequence must not be null");
		Assert.notNull(charset, "Charset must not be null");
		if (charSequence.length() != 0) {
			CharsetEncoder charsetEncoder = charset.newEncoder()
					.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE);
			CharBuffer inBuffer = CharBuffer.wrap(charSequence);
			int estimatedSize = (int) (inBuffer.remaining() * charsetEncoder.averageBytesPerChar());
			ByteBuffer outBuffer = ensureCapacity(estimatedSize)
					.asByteBuffer(writePosition(), writableByteCount());
			while (true) {
				CoderResult cr = (inBuffer.hasRemaining() ?
						charsetEncoder.encode(inBuffer, outBuffer, true) : CoderResult.UNDERFLOW);
				if (cr.isUnderflow()) {
					cr = charsetEncoder.flush(outBuffer);
				}
				if (cr.isUnderflow()) {
					break;
				}
				if (cr.isOverflow()) {
					writePosition(writePosition() + outBuffer.position());
					int maximumSize = (int) (inBuffer.remaining() * charsetEncoder.maxBytesPerChar());
					ensureCapacity(maximumSize);
					outBuffer = asByteBuffer(writePosition(), writableByteCount());
				}
			}
			writePosition(writePosition() + outBuffer.position());
		}
		return this;
	}

	/**
	 * 创建一个新的 {@code DataBuffer}，内容是当前缓冲区内容的共享子序列。
	 * 当前缓冲区与新缓冲区共享数据，但新缓冲区位置的变更不会影响当前缓冲区的读写位置。
	 * <p><strong>注意：</strong>此方法不会调用 {@link DataBufferUtils#retain(DataBuffer)}，
	 * 因此引用计数不会增加。
	 * @param index 起始索引
	 * @param length 子序列长度
	 * @return 指定的子序列缓冲区
	 */
	DataBuffer slice(int index, int length);

	/**
	 * 创建一个新的 {@code DataBuffer}，内容是当前缓冲区内容的共享且已保留引用的子序列。
	 * 当前缓冲区与新缓冲区共享数据，但新缓冲区位置的变更不会影响当前缓冲区的读写位置。
	 * <p><strong>注意：</strong>与 {@link #slice(int, int)} 不同，该方法会调用
	 * {@link DataBufferUtils#retain(DataBuffer)}（或等效方法）增加引用计数。
	 * @param index 起始索引
	 * @param length 子序列长度
	 * @return 指定且已保留的子序列缓冲区
	 * @since 5.2
	 */
	default DataBuffer retainedSlice(int index, int length) {
		return DataBufferUtils.retain(slice(index, length));
	}

	/**
	 * 将当前缓冲区的字节以 {@link ByteBuffer} 形式暴露。
	 * 当前缓冲区与返回的 {@code ByteBuffer} 共享数据；
	 * 但返回缓冲区位置的变更不会影响当前缓冲区的读写位置。
	 * @return 该数据缓冲区对应的字节缓冲区
	 */
	ByteBuffer asByteBuffer();

	/**
	 * 将当前缓冲区指定部分的字节以 {@link ByteBuffer} 形式暴露。
	 * 当前缓冲区与返回的 {@code ByteBuffer} 共享数据；
	 * 但返回缓冲区位置的变更不会影响当前缓冲区的读写位置。
	 * @param index 起始索引
	 * @param length 字节长度
	 * @return 该数据缓冲区对应的字节缓冲区
	 * @since 5.0.1
	 */
	ByteBuffer asByteBuffer(int index, int length);

	/**
	 * 将当前缓冲区的数据以 {@link InputStream} 形式暴露。
	 * 返回的输入流与当前缓冲区共享数据及读取位置，
	 * 关闭输入流时不会释放底层缓冲区。
	 * @return 该数据缓冲区对应的输入流
	 * @see #asInputStream(boolean)
	 */
	InputStream asInputStream();

	/**
	 * 将当前缓冲区的数据以 {@link InputStream} 形式暴露。
	 * 返回的输入流与当前缓冲区共享数据及读取位置。
	 * @param releaseOnClose 关闭输入流时是否释放底层缓冲区
	 * @return 该数据缓冲区对应的输入流
	 * @since 5.0.4
	 */
	InputStream asInputStream(boolean releaseOnClose);

	/**
	 * 将当前缓冲区的数据以 {@link OutputStream} 形式暴露。
	 * 返回的输出流与当前缓冲区共享数据及写入位置。
	 * @return 该数据缓冲区对应的输出流
	 */
	OutputStream asOutputStream();

	/**
	 * 使用指定字符集将缓冲区数据转换为字符串。
	 * 默认实现委托调用 {@code toString(readPosition(), readableByteCount(), charset)}。
	 * @param charset 使用的字符集
	 * @return 缓冲区所有数据的字符串表示
	 * @since 5.2
	 */
	default String toString(Charset charset) {
		Assert.notNull(charset, "Charset must not be null");
		return toString(readPosition(), readableByteCount(), charset);
	}

	/**
	 * 使用指定字符集返回缓冲区部分数据的字符串表示。
	 * @param index 起始索引
	 * @param length 字节长度
	 * @param charset 使用的字符集
	 * @return 部分数据的字符串表示
	 * @since 5.2
	 */
	String toString(int index, int length, Charset charset);

}
