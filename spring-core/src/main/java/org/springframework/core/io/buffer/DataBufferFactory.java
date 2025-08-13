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

package org.springframework.core.io.buffer;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * {@link DataBuffer 数据缓冲区} 的工厂接口，支持分配和包装数据缓冲区。
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @see DataBuffer
 */
public interface DataBufferFactory {

	/**
	 * 分配一个默认初始容量的数据缓冲区。
	 * 根据底层实现和配置，可能是基于堆内存的缓冲区或直接缓冲区。
	 * @return 分配的缓冲区
	 */
	DataBuffer allocateBuffer();

	/**
	 * 分配指定初始容量的数据缓冲区。
	 * 根据底层实现和配置，可能是基于堆内存的缓冲区或直接缓冲区。
	 * @param initialCapacity 要分配的缓冲区初始容量
	 * @return 分配的缓冲区
	 */
	DataBuffer allocateBuffer(int initialCapacity);

	/**
	 * 将给定的 {@link ByteBuffer} 包装为 {@code DataBuffer}。
	 * 与 {@linkplain #allocateBuffer(int) 分配} 不同，包装操作不会使用新的内存。
	 * @param byteBuffer 要包装的 NIO 字节缓冲区
	 * @return 包装后的缓冲区
	 */
	DataBuffer wrap(ByteBuffer byteBuffer);

	/**
	 * 将给定的字节数组包装为 {@code DataBuffer}。
	 * 与 {@linkplain #allocateBuffer(int) 分配} 不同，包装操作不会使用新的内存。
	 * @param bytes 要包装的字节数组
	 * @return 包装后的缓冲区
	 */
	DataBuffer wrap(byte[] bytes);

	/**
	 * 返回一个由传入的 {@code dataBuffers} 组成的新 {@code DataBuffer}，相当于将它们合并。
	 * 具体实现可能返回一个包含所有数据的单一缓冲区，或一个真正的组合缓冲区，包含对各个缓冲区的引用。
	 * <p>注意，传入的缓冲区无需显式释放，因为它们会作为返回的组合缓冲区的一部分被释放。
	 * @param dataBuffers 要组合的多个数据缓冲区列表
	 * @return 由传入缓冲区组成的新缓冲区
	 * @since 5.0.3
	 */
	DataBuffer join(List<? extends DataBuffer> dataBuffers);

}
