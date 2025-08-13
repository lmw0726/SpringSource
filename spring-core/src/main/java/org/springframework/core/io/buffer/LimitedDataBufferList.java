/*
 * Copyright 2002-2021 the original author or authors.
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

import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * 自定义的 {@link List}，用于收集数据缓冲区，并对缓冲的总字节数强制限制。
 * 适用于声明式 API 中的“collect”或其他缓冲操作符，例如 {@link Flux}。
 *
 * <p>添加元素会增加字节计数，如果超过限制，将抛出 {@link DataBufferLimitException}。
 * 调用 {@link #clear()} 会重置计数。不支持 remove 和 set 操作。
 *
 * <p><strong>注意：</strong>该类不会自动释放其包含的缓冲区。
 * 通常建议使用诸如 {@link Flux#doOnDiscard} 之类的钩子，这些钩子也能处理取消和错误信号，
 * 或者可以使用 {@link #releaseAndClear()} 方法。
 *
 * @author Rossen Stoyanchev
 * @since 5.1.11
 */
@SuppressWarnings("serial")
public class LimitedDataBufferList extends ArrayList<DataBuffer> {

	private final int maxByteCount;

	private int byteCount;


	public LimitedDataBufferList(int maxByteCount) {
		this.maxByteCount = maxByteCount;
	}


	@Override
	public boolean add(DataBuffer buffer) {
		updateCount(buffer.readableByteCount());
		return super.add(buffer);
	}

	@Override
	public void add(int index, DataBuffer buffer) {
		super.add(index, buffer);
		updateCount(buffer.readableByteCount());
	}

	@Override
	public boolean addAll(Collection<? extends DataBuffer> collection) {
		boolean result = super.addAll(collection);
		collection.forEach(buffer -> updateCount(buffer.readableByteCount()));
		return result;
	}

	@Override
	public boolean addAll(int index, Collection<? extends DataBuffer> collection) {
		boolean result = super.addAll(index, collection);
		collection.forEach(buffer -> updateCount(buffer.readableByteCount()));
		return result;
	}

	private void updateCount(int bytesToAdd) {
		if (this.maxByteCount < 0) {
			return;
		}
		if (bytesToAdd > Integer.MAX_VALUE - this.byteCount) {
			raiseLimitException();
		}
		else {
			this.byteCount += bytesToAdd;
			if (this.byteCount > this.maxByteCount) {
				raiseLimitException();
			}
		}
	}

	private void raiseLimitException() {
		// 这里不要释放，通常会通过 doOnDiscard 进行释放……
		throw new DataBufferLimitException(
				"Exceeded limit on max bytes to buffer : " + this.maxByteCount);
	}

	@Override
	public DataBuffer remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void removeRange(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeIf(Predicate<? super DataBuffer> filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DataBuffer set(int index, DataBuffer element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		this.byteCount = 0;
		super.clear();
	}

	/**
	 * 快捷方法，释放所有数据缓冲区（调用 {@link DataBufferUtils#release release}），
	 * 然后执行 {@link #clear()} 清空操作。
	 */
	public void releaseAndClear() {
		forEach(buf -> {
			try {
				DataBufferUtils.release(buf);
			}
			catch (Throwable ex) {
				// 继续执行，忽略异常
			}
		});
		clear();
	}

}
