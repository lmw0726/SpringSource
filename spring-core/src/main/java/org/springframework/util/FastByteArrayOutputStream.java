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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * {@link java.io.ByteArrayOutputStream} 的高速替代实现。注意，这个类
 * <i>不继承</i> {@code ByteArrayOutputStream}，与它的兄弟类 {@link ResizableByteArrayOutputStream} 不同。
 *
 * <p>不同于 {@link java.io.ByteArrayOutputStream}，该实现由一个 {@link java.util.ArrayDeque} 管理多个 {@code byte[]} 缓冲区，
 * 而非单一不断扩展的 {@code byte[]}。
 * 它在扩容时不会复制已有缓冲区。
 *
 * <p>初始缓冲区只在第一次写操作时创建。
 * 使用 {@link #writeTo(OutputStream)} 方法导出内容时，也不会复制内部缓冲区。
 *
 * @author Craig Andrews
 * @author Juergen Hoeller
 * @since 4.2
 * @see #resize
 * @see ResizableByteArrayOutputStream
 */
public class FastByteArrayOutputStream extends OutputStream {

	private static final int DEFAULT_BLOCK_SIZE = 256;


	// 用于存储内容字节的缓冲区队列
	private final Deque<byte[]> buffers = new ArrayDeque<>();

	// 用于分配第一个 byte[] 的大小（字节数）
	private final int initialBlockSize;

	// 用于分配下一个 byte[] 的大小（字节数）
	private int nextBlockSize = 0;

	// 之前所有缓冲区中已存储的字节数（当前缓冲区的字节数由 index 表示）
	private int alreadyBufferedSize = 0;

	// 当前缓冲区（即 buffers.getLast()）中下一个写入位置的索引
	private int index = 0;

	// 流是否已关闭
	private boolean closed = false;


	/**
	 * 使用默认初始容量 256 字节创建新的 {@code FastByteArrayOutputStream}。
	 */
	public FastByteArrayOutputStream() {
		this(DEFAULT_BLOCK_SIZE);
	}

	/**
	 * 使用指定的初始容量创建新的 {@code FastByteArrayOutputStream}。
	 * @param initialBlockSize 初始缓冲区大小（字节）
	 */
	public FastByteArrayOutputStream(int initialBlockSize) {
		Assert.isTrue(initialBlockSize > 0, "Initial block size must be greater than 0");
		this.initialBlockSize = initialBlockSize;
		this.nextBlockSize = initialBlockSize;
	}


	// 重写的方法

	@Override
	public void write(int datum) throws IOException {
		if (this.closed) {
			throw new IOException("Stream closed");
		}
		else {
			if (this.buffers.peekLast() == null || this.buffers.getLast().length == this.index) {
				addBuffer(1);
			}
			// 存储字节
			this.buffers.getLast()[this.index++] = (byte) datum;
		}
	}

	@Override
	public void write(byte[] data, int offset, int length) throws IOException {
		if (offset < 0 || offset + length > data.length || length < 0) {
			throw new IndexOutOfBoundsException();
		}
		else if (this.closed) {
			throw new IOException("Stream closed");
		}
		else {
			if (this.buffers.peekLast() == null || this.buffers.getLast().length == this.index) {
				addBuffer(length);
			}
			if (this.index + length > this.buffers.getLast().length) {
				int pos = offset;
				do {
					if (this.index == this.buffers.getLast().length) {
						addBuffer(length);
					}
					int copyLength = this.buffers.getLast().length - this.index;
					if (length < copyLength) {
						copyLength = length;
					}
					System.arraycopy(data, pos, this.buffers.getLast(), this.index, copyLength);
					pos += copyLength;
					this.index += copyLength;
					length -= copyLength;
				}
				while (length > 0);
			}
			else {
				// copy in the sub-array
				System.arraycopy(data, offset, this.buffers.getLast(), this.index, length);
				this.index += length;
			}
		}
	}

	@Override
	public void close() {
		this.closed = true;
	}

	/**
	 * 将缓冲区内容转换为字符串，使用平台默认字符集解码字节。
	 * <p>新字符串的长度依赖于字符集，因此可能与缓冲区大小不相等。
	 * <p>该方法总是用平台默认字符集的默认替代字符串替换格式错误或不可映射的字符序列。
	 * 如果需要更精细的解码控制，应使用 {@linkplain java.nio.charset.CharsetDecoder} 类。
	 * @return 从缓冲区内容解码得到的字符串
	 */
	@Override
	public String toString() {
		return new String(toByteArrayUnsafe());
	}


	// 自定义方法

	/**
	 * 返回此 {@code FastByteArrayOutputStream} 中存储的字节数。
	 */
	public int size() {
		return (this.alreadyBufferedSize + this.index);
	}

	/**
	 * 将流中的数据转换为字节数组并返回该数组。
	 * <p>同时用该字节数组替换内部结构以节省内存：
	 * 如果字节数组已经生成，不妨直接使用它。
	 * 该方法还意味着如果连续两次调用本方法且之间无写操作，第二次调用将是无操作。
	 * <p>此方法为“不安全”的，因为它返回内部缓冲区，
	 * 调用者不应修改返回的缓冲区内容。
	 * @return 当前输出流的内容，字节数组形式。
	 * @see #size()
	 * @see #toByteArray()
	 */
	public byte[] toByteArrayUnsafe() {
		int totalSize = size();
		if (totalSize == 0) {
			return new byte[0];
		}
		resize(totalSize);
		return this.buffers.getFirst();
	}

	/**
	 * 创建一个新分配的字节数组。
	 * <p>其大小等于当前输出流的大小，并且缓冲区中有效内容已复制到该数组中。</p>
	 * @return 当前输出流的内容，以字节数组形式返回。
	 * @see #size()
	 * @see #toByteArrayUnsafe()
	 */
	public byte[] toByteArray() {
		byte[] bytesUnsafe = toByteArrayUnsafe();
		return bytesUnsafe.clone();
	}

	/**
	 * 重置此 {@code FastByteArrayOutputStream} 的内容。
	 * <p>丢弃输出流中当前累积的所有数据，输出流可重新使用。</p>
	 */
	public void reset() {
		this.buffers.clear();
		this.nextBlockSize = this.initialBlockSize;
		this.closed = false;
		this.index = 0;
		this.alreadyBufferedSize = 0;
	}

	/**
	 * 获取一个 {@link InputStream} 以读取此输出流中的数据。
	 * <p>注意，如果在输出流上调用任何方法（包括写方法、{@link #reset()}、
	 * {@link #toByteArray()} 和 {@link #toByteArrayUnsafe()}），则该输入流的行为未定义。</p>
	 * @return 此输出流内容的 {@link InputStream}
	 */
	public InputStream getInputStream() {
		return new FastByteArrayInputStream(this);
	}

	/**
	 * 将缓冲区内容写入给定的 {@link OutputStream}。
	 * @param out 要写入的输出流
	 * @throws IOException 发生 I/O 错误时抛出
	 */
	public void writeTo(OutputStream out) throws IOException {
		Iterator<byte[]> it = this.buffers.iterator();
		while (it.hasNext()) {
			byte[] bytes = it.next();
			if (it.hasNext()) {
				out.write(bytes, 0, bytes.length);
			}
			else {
				out.write(bytes, 0, this.index);
			}
		}
	}

	/**
	 * 调整内部缓冲区大小到指定容量。
	 * @param targetCapacity 目标缓冲区大小
	 * @throws IllegalArgumentException 如果指定容量小于当前已存储内容大小
	 * @see FastByteArrayOutputStream#size()
	 */
	public void resize(int targetCapacity) {
		Assert.isTrue(targetCapacity >= size(), "New capacity must not be smaller than current size");
		if (this.buffers.peekFirst() == null) {
			this.nextBlockSize = targetCapacity - size();
		}
		else if (size() == targetCapacity && this.buffers.getFirst().length == targetCapacity) {
			// 容量已满足目标，无需调整
		}
		else {
			int totalSize = size();
			byte[] data = new byte[targetCapacity];
			int pos = 0;
			Iterator<byte[]> it = this.buffers.iterator();
			while (it.hasNext()) {
				byte[] bytes = it.next();
				if (it.hasNext()) {
					System.arraycopy(bytes, 0, data, pos, bytes.length);
					pos += bytes.length;
				}
				else {
					System.arraycopy(bytes, 0, data, pos, this.index);
				}
			}
			this.buffers.clear();
			this.buffers.add(data);
			this.index = totalSize;
			this.alreadyBufferedSize = 0;
		}
	}

	/**
	 * 创建一个新的缓冲区并存入 ArrayDeque。
	 * <p>新增的缓冲区大小至少能存储 {@code minCapacity} 字节。
	 */
	private void addBuffer(int minCapacity) {
		if (this.buffers.peekLast() != null) {
			this.alreadyBufferedSize += this.index;
			this.index = 0;
		}
		if (this.nextBlockSize < minCapacity) {
			this.nextBlockSize = nextPowerOf2(minCapacity);
		}
		this.buffers.add(new byte[this.nextBlockSize]);
		this.nextBlockSize *= 2;  // 每次缓冲区大小翻倍
	}

	/**
	 * 获取大于等于给定数的下一个2的幂次方（例如119的下一个2的幂是128）。
	 */
	private static int nextPowerOf2(int val) {
		val--;
		val = (val >> 1) | val;
		val = (val >> 2) | val;
		val = (val >> 4) | val;
		val = (val >> 8) | val;
		val = (val >> 16) | val;
		val++;
		return val;
	}


	/**
	 * {@link java.io.InputStream} 的实现类，从给定的
	 * <code>FastByteArrayOutputStream</code> 中读取数据。
	 */
	private static final class FastByteArrayInputStream extends UpdateMessageDigestInputStream {

		private final FastByteArrayOutputStream fastByteArrayOutputStream;

		private final Iterator<byte[]> buffersIterator;

		@Nullable
		private byte[] currentBuffer;

		private int currentBufferLength = 0;

		private int nextIndexInCurrentBuffer = 0;

		private int totalBytesRead = 0;

		/**
		 * 创建一个新的由指定的 <code>FastByteArrayOutputStream</code> 支持的
		 * <code>FastByteArrayOutputStreamInputStream</code> 实例。
		 */
		public FastByteArrayInputStream(FastByteArrayOutputStream fastByteArrayOutputStream) {
			this.fastByteArrayOutputStream = fastByteArrayOutputStream;
			this.buffersIterator = fastByteArrayOutputStream.buffers.iterator();
			if (this.buffersIterator.hasNext()) {
				this.currentBuffer = this.buffersIterator.next();
				if (this.currentBuffer == fastByteArrayOutputStream.buffers.getLast()) {
					this.currentBufferLength = fastByteArrayOutputStream.index;
				}
				else {
					this.currentBufferLength = (this.currentBuffer != null ? this.currentBuffer.length : 0);
				}
			}
		}

		@Override
		public int read() {
			if (this.currentBuffer == null) {
				// 此流中没有任何数据...
				return -1;
			}
			else {
				if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
					this.totalBytesRead++;
					return this.currentBuffer[this.nextIndexInCurrentBuffer++] & 0xFF;
				}
				else {
					if (this.buffersIterator.hasNext()) {
						this.currentBuffer = this.buffersIterator.next();
						updateCurrentBufferLength();
						this.nextIndexInCurrentBuffer = 0;
					}
					else {
						this.currentBuffer = null;
					}
					return read();
				}
			}
		}

		@Override
		public int read(byte[] b) {
			return read(b, 0, b.length);
		}

		@Override
		public int read(byte[] b, int off, int len) {
			if (off < 0 || len < 0 || len > b.length - off) {
				throw new IndexOutOfBoundsException();
			}
			else if (len == 0) {
				return 0;
			}
			else {
				if (this.currentBuffer == null) {
					// 此流中没有任何数据...
					return -1;
				}
				else {
					if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
						int bytesToCopy = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
						System.arraycopy(this.currentBuffer, this.nextIndexInCurrentBuffer, b, off, bytesToCopy);
						this.totalBytesRead += bytesToCopy;
						this.nextIndexInCurrentBuffer += bytesToCopy;
						int remaining = read(b, off + bytesToCopy, len - bytesToCopy);
						return bytesToCopy + Math.max(remaining, 0);
					}
					else {
						if (this.buffersIterator.hasNext()) {
							this.currentBuffer = this.buffersIterator.next();
							updateCurrentBufferLength();
							this.nextIndexInCurrentBuffer = 0;
						}
						else {
							this.currentBuffer = null;
						}
						return read(b, off, len);
					}
				}
			}
		}

		@Override
		public long skip(long n) throws IOException {
			if (n > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("n exceeds maximum (" + Integer.MAX_VALUE + "): " + n);
			}
			else if (n == 0) {
				return 0;
			}
			else if (n < 0) {
				throw new IllegalArgumentException("n must be 0 or greater: " + n);
			}
			int len = (int) n;
			if (this.currentBuffer == null) {
				// 此流中没有任何数据...
				return 0;
			}
			else {
				if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
					int bytesToSkip = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
					this.totalBytesRead += bytesToSkip;
					this.nextIndexInCurrentBuffer += bytesToSkip;
					return (bytesToSkip + skip(len - bytesToSkip));
				}
				else {
					if (this.buffersIterator.hasNext()) {
						this.currentBuffer = this.buffersIterator.next();
						updateCurrentBufferLength();
						this.nextIndexInCurrentBuffer = 0;
					}
					else {
						this.currentBuffer = null;
					}
					return skip(len);
				}
			}
		}

		@Override
		public int available() {
			return (this.fastByteArrayOutputStream.size() - this.totalBytesRead);
		}

		/**
		 * 使用该流中剩余的字节更新消息摘要。
		 * @param messageDigest 需要更新的消息摘要
		 */
		@Override
		public void updateMessageDigest(MessageDigest messageDigest) {
			updateMessageDigest(messageDigest, available());
		}

		/**
		 * 使用该流中的下一个 len 个字节更新消息摘要。
		 * 避免创建新的字节数组，使用内部缓冲区以提升性能。
		 * @param messageDigest 需要更新的消息摘要
		 * @param len 从该流中读取多少字节用于更新消息摘要
		 */
		@Override
		public void updateMessageDigest(MessageDigest messageDigest, int len) {
			if (this.currentBuffer == null) {
				// 此流中没有任何数据...
				return;
			}
			else if (len == 0) {
				return;
			}
			else if (len < 0) {
				throw new IllegalArgumentException("len must be 0 or greater: " + len);
			}
			else {
				if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
					int bytesToCopy = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
					messageDigest.update(this.currentBuffer, this.nextIndexInCurrentBuffer, bytesToCopy);
					this.nextIndexInCurrentBuffer += bytesToCopy;
					updateMessageDigest(messageDigest, len - bytesToCopy);
				}
				else {
					if (this.buffersIterator.hasNext()) {
						this.currentBuffer = this.buffersIterator.next();
						updateCurrentBufferLength();
						this.nextIndexInCurrentBuffer = 0;
					}
					else {
						this.currentBuffer = null;
					}
					updateMessageDigest(messageDigest, len);
				}
			}
		}

		private void updateCurrentBufferLength() {
			if (this.currentBuffer == this.fastByteArrayOutputStream.buffers.getLast()) {
				this.currentBufferLength = this.fastByteArrayOutputStream.index;
			}
			else {
				this.currentBufferLength = (this.currentBuffer != null ? this.currentBuffer.length : 0);
			}
		}
	}

}
