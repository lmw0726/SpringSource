/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.util;

import java.io.ByteArrayOutputStream;

/**
 * {@link java.io.ByteArrayOutputStream} 的扩展：
 * <ul>
 * <li>提供了公开的 {@link org.springframework.util.ResizableByteArrayOutputStream#grow(int)}
 * 和 {@link org.springframework.util.ResizableByteArrayOutputStream#resize(int)} 方法，
 * 以便更好地控制内部缓冲区的大小</li>
 * <li>默认具有更大的初始容量（256）</li>
 * </ul>
 *
 * <p>从 4.2 版本开始，该类已被 {@link FastByteArrayOutputStream} 取代，
 * 用于 Spring 内部不需要赋值为 {@link ByteArrayOutputStream} 的场景
 * （因为 {@link FastByteArrayOutputStream} 在缓冲区大小调整管理上更高效，
 * 但不继承标准的 {@link ByteArrayOutputStream}）。
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 4.0.3
 * @see #resize
 * @see FastByteArrayOutputStream
 */
public class ResizableByteArrayOutputStream extends ByteArrayOutputStream {

	private static final int DEFAULT_INITIAL_CAPACITY = 256;


	/**
	 * 创建一个具有默认初始容量 256 字节的
	 * <code>ResizableByteArrayOutputStream</code> 实例。
	 */
	public ResizableByteArrayOutputStream() {
		super(DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * 创建一个具有指定初始容量的
	 * <code>ResizableByteArrayOutputStream</code> 实例。
	 * @param initialCapacity 初始缓冲区大小（字节）
	 */
	public ResizableByteArrayOutputStream(int initialCapacity) {
		super(initialCapacity);
	}


	/**
	 * 调整内部缓冲区大小到指定容量。
	 * @param targetCapacity 目标缓冲区大小
	 * @throws IllegalArgumentException 如果给定容量小于当前缓冲区内实际内容的大小
	 * @see ResizableByteArrayOutputStream#size()
	 */
	public synchronized void resize(int targetCapacity) {
		Assert.isTrue(targetCapacity >= this.count, "New capacity must not be smaller than current size");
		byte[] resizedBuffer = new byte[targetCapacity];
		System.arraycopy(this.buf, 0, resizedBuffer, 0, this.count);
		this.buf = resizedBuffer;
	}

	/**
	 * 扩展内部缓冲区大小。
	 * @param additionalCapacity 要增加的字节数
	 * @see ResizableByteArrayOutputStream#size()
	 */
	public synchronized void grow(int additionalCapacity) {
		Assert.isTrue(additionalCapacity >= 0, "Additional capacity must be 0 or higher");
		if (this.count + additionalCapacity > this.buf.length) {
			int newCapacity = Math.max(this.buf.length * 2, this.count + additionalCapacity);
			resize(newCapacity);
		}
	}

	/**
	 * 返回当前流内部缓冲区的容量大小。
	 */
	public synchronized int capacity() {
		return this.buf.length;
	}

}
