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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import org.springframework.util.Assert;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * 基于 Netty {@link ByteBufAllocator} 实现的 {@code DataBufferFactory} 接口。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 * @see io.netty.buffer.PooledByteBufAllocator
 * @see io.netty.buffer.UnpooledByteBufAllocator
 */
public class NettyDataBufferFactory implements DataBufferFactory {

	private final ByteBufAllocator byteBufAllocator;


	/**
	 * 使用给定的 {@code ByteBufAllocator} 创建一个新的 {@code NettyDataBufferFactory}。
	 * @param byteBufAllocator 要使用的分配器
	 * @see io.netty.buffer.PooledByteBufAllocator
	 * @see io.netty.buffer.UnpooledByteBufAllocator
	 */
	public NettyDataBufferFactory(ByteBufAllocator byteBufAllocator) {
		Assert.notNull(byteBufAllocator, "ByteBufAllocator must not be null");
		this.byteBufAllocator = byteBufAllocator;
	}


	/**
	 * 返回此工厂使用的 {@code ByteBufAllocator}。
	 */
	public ByteBufAllocator getByteBufAllocator() {
		return this.byteBufAllocator;
	}

	@Override
	public NettyDataBuffer allocateBuffer() {
		ByteBuf byteBuf = this.byteBufAllocator.buffer();
		return new NettyDataBuffer(byteBuf, this);
	}

	@Override
	public NettyDataBuffer allocateBuffer(int initialCapacity) {
		ByteBuf byteBuf = this.byteBufAllocator.buffer(initialCapacity);
		return new NettyDataBuffer(byteBuf, this);
	}

	@Override
	public NettyDataBuffer wrap(ByteBuffer byteBuffer) {
		ByteBuf byteBuf = Unpooled.wrappedBuffer(byteBuffer);
		return new NettyDataBuffer(byteBuf, this);
	}

	@Override
	public DataBuffer wrap(byte[] bytes) {
		ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
		return new NettyDataBuffer(byteBuf, this);
	}

	/**
	 * 将给定的 Netty {@link ByteBuf} 包装为 {@code NettyDataBuffer}。
	 * @param byteBuf 要包装的 Netty 字节缓冲区
	 * @return 包装后的缓冲区
	 */
	public NettyDataBuffer wrap(ByteBuf byteBuf) {
		byteBuf.touch();
		return new NettyDataBuffer(byteBuf, this);
	}

	/**
	 * {@inheritDoc}
	 * <p>该实现使用 Netty 的 {@link CompositeByteBuf}。
	 */
	@Override
	public DataBuffer join(List<? extends DataBuffer> dataBuffers) {
		Assert.notEmpty(dataBuffers, "DataBuffer List must not be empty");
		int bufferCount = dataBuffers.size();
		if (bufferCount == 1) {
			return dataBuffers.get(0);
		}
		CompositeByteBuf composite = this.byteBufAllocator.compositeBuffer(bufferCount);
		for (DataBuffer dataBuffer : dataBuffers) {
			Assert.isInstanceOf(NettyDataBuffer.class, dataBuffer);
			composite.addComponent(true, ((NettyDataBuffer) dataBuffer).getNativeBuffer());
		}
		return new NettyDataBuffer(composite, this);
	}

	/**
	 * 将给定的 Netty {@link DataBuffer} 转换为 {@link ByteBuf}。
	 * <p>如果 {@code buffer} 是 {@link NettyDataBuffer}，则返回其
	 * {@linkplain NettyDataBuffer#getNativeBuffer() 原生缓冲区}；
	 * 否则返回 {@link Unpooled#wrappedBuffer(ByteBuffer)} 包装的缓冲区。
	 * @param buffer 要转换的 {@code DataBuffer}
	 * @return 对应的 Netty {@code ByteBuf}
	 */
	public static ByteBuf toByteBuf(DataBuffer buffer) {
		if (buffer instanceof NettyDataBuffer) {
			return ((NettyDataBuffer) buffer).getNativeBuffer();
		}
		else {
			return Unpooled.wrappedBuffer(buffer.asByteBuffer());
		}
	}


	@Override
	public String toString() {
		return "NettyDataBufferFactory (" + this.byteBufAllocator + ")";
	}

}
