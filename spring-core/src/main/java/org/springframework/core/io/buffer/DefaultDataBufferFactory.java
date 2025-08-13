/*
 * Copyright 2002-2020 the original author or authors.
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

import java.nio.ByteBuffer;
import java.util.List;

/**
 * {@code DataBufferFactory} 接口的默认实现。允许在构造时指定默认初始容量，
 * 以及优先使用基于堆的缓冲区还是直接缓冲区。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class DefaultDataBufferFactory implements DataBufferFactory {

	/**
	 * 未指定时的默认容量。
	 * @see #DefaultDataBufferFactory()
	 * @see #DefaultDataBufferFactory(boolean)
	 */
	public static final int DEFAULT_INITIAL_CAPACITY = 256;

	/**
	 * 基于默认构造函数的共享实例。
	 * @since 5.3
	 */
	public static final DefaultDataBufferFactory sharedInstance = new DefaultDataBufferFactory();


	private final boolean preferDirect;

	private final int defaultInitialCapacity;


	/**
	 * 创建一个使用默认设置的 {@code DefaultDataBufferFactory}。
	 * @see #sharedInstance
	 */
	public DefaultDataBufferFactory() {
		this(false);
	}

	/**
	 * 创建一个 {@code DefaultDataBufferFactory}，指明 {@link #allocateBuffer()} 和
	 * {@link #allocateBuffer(int)} 是否应创建直接缓冲区。
	 * @param preferDirect 如果优先使用直接缓冲区则为 {@code true}，否则为 {@code false}
	 */
	public DefaultDataBufferFactory(boolean preferDirect) {
		this(preferDirect, DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * 创建一个 {@code DefaultDataBufferFactory}，指明 {@link #allocateBuffer()} 和
	 * {@link #allocateBuffer(int)} 是否应创建直接缓冲区，以及 {@link #allocateBuffer()} 使用的容量。
	 * @param preferDirect 如果优先使用直接缓冲区则为 {@code true}，否则为 {@code false}
	 */
	public DefaultDataBufferFactory(boolean preferDirect, int defaultInitialCapacity) {
		Assert.isTrue(defaultInitialCapacity > 0, "'defaultInitialCapacity' should be larger than 0");
		this.preferDirect = preferDirect;
		this.defaultInitialCapacity = defaultInitialCapacity;
	}


	@Override
	public DefaultDataBuffer allocateBuffer() {
		return allocateBuffer(this.defaultInitialCapacity);
	}

	@Override
	public DefaultDataBuffer allocateBuffer(int initialCapacity) {
		ByteBuffer byteBuffer = (this.preferDirect ?
				ByteBuffer.allocateDirect(initialCapacity) :
				ByteBuffer.allocate(initialCapacity));
		return DefaultDataBuffer.fromEmptyByteBuffer(this, byteBuffer);
	}

	@Override
	public DefaultDataBuffer wrap(ByteBuffer byteBuffer) {
		return DefaultDataBuffer.fromFilledByteBuffer(this, byteBuffer.slice());
	}

	@Override
	public DefaultDataBuffer wrap(byte[] bytes) {
		return DefaultDataBuffer.fromFilledByteBuffer(this, ByteBuffer.wrap(bytes));
	}

	/**
	 * {@inheritDoc}
	 * <p>此实现创建一个单独的 {@link DefaultDataBuffer} 来包含 {@code dataBuffers} 中的数据。
	 */
	@Override
	public DefaultDataBuffer join(List<? extends DataBuffer> dataBuffers) {
		Assert.notEmpty(dataBuffers, "DataBuffer List must not be empty");
		int capacity = dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum();
		DefaultDataBuffer result = allocateBuffer(capacity);
		dataBuffers.forEach(result::write);
		dataBuffers.forEach(DataBufferUtils::release);
		return result;
	}


	@Override
	public String toString() {
		return "DefaultDataBufferFactory (preferDirect=" + this.preferDirect + ")";
	}

}
